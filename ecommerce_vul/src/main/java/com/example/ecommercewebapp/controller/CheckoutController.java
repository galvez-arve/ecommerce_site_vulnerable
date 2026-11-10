package com.example.ecommercewebapp.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * VULNERABLE ON PURPOSE - cart and checkout flow.
 *
 * - /cart/add and /checkout trust client-submitted price, quantity and
 *   voucher discount instead of re-pricing server-side from the products
 *   table -> business-logic / price-tampering flaw (negative totals,
 *   >100% discounts, free items).
 * - /checkout accepts an optional userId straight from the request and
 *   uses it with no ownership/auth check -> you can place (and later view)
 *   an order "as" any existing user id -> broken access control / IDOR.
 * - orderStatus is taken directly from the request too -> mass assignment
 *   (an order can be submitted pre-marked "Delivered").
 * - shippingNotes is stored and later rendered with th:utext on the
 *   confirmation page and in the admin orders view -> stored XSS.
 * - No CSRF token on the POST endpoints.
 */
@Controller
public class CheckoutController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cart(HttpSession session) {
        List<Map<String, Object>> cart = (List<Map<String, Object>>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam String productId,
                             @RequestParam String productName,
                             @RequestParam BigDecimal price,          // client-supplied, not re-checked
                             @RequestParam(defaultValue = "1") Integer quantity,
                             @RequestParam(defaultValue = "0") BigDecimal discount, // client-supplied
                             HttpServletRequest request) {
        Map<String, Object> item = new HashMap<>();
        item.put("productId", productId);
        item.put("productName", productName);
        item.put("price", price);
        item.put("quantity", quantity);
        item.put("discount", discount);
        cart(request.getSession(true)).add(item);
        return "redirect:/checkout";
    }

    @GetMapping("/checkout")
    public String checkoutPage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(true);
        // Checkout requires an active login - browsing and adding to cart
        // do not. Note this only checks that *some* session userId is set;
        // it does not confirm the session is still valid server-side in
        // any stronger way, and placeOrder() below still accepts a
        // different userId in the request than the one in session (IDOR).
        if (session.getAttribute("userId") == null) {
            return "redirect:/login?next=/checkout";
        }
        List<Map<String, Object>> items = cart(session);
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            BigDecimal price = (BigDecimal) item.get("price");
            BigDecimal discount = (BigDecimal) item.get("discount");
            int qty = (Integer) item.get("quantity");
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));
            BigDecimal discounted = lineTotal.subtract(lineTotal.multiply(discount).divide(BigDecimal.valueOf(100)));
            total = total.add(discounted);
        }
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@RequestParam(required = false) String userId,
                              @RequestParam String shippingName,
                              @RequestParam(required = false) String shippingNotes,
                              @RequestParam(required = false) String orderStatus, // mass assignment
                              @RequestParam(required = false) String overrideTotal, // price tampering
                              HttpServletRequest request,
                              Model model) {
        HttpSession session = request.getSession(true);
        if (session.getAttribute("userId") == null) {
            return "redirect:/login?next=/checkout";
        }
        List<Map<String, Object>> items = cart(session);

        if (items.isEmpty()) {
            return "redirect:/checkout";
        }

        // Fall back to the logged-in session user, but happily accept a
        // different one if the request supplies it - no check that the
        // caller is allowed to order "as" that user.
        String effectiveUserId = (userId != null && !userId.isBlank())
                ? userId
                : (String) session.getAttribute("userId");

        if (effectiveUserId == null) {
            model.addAttribute("error", "No user context. Pass userId or log in.");
            return "checkout";
        }

        List<String> createdOrderIds = new ArrayList<>();

        for (Map<String, Object> item : items) {
            BigDecimal price = (BigDecimal) item.get("price");
            BigDecimal discount = (BigDecimal) item.get("discount");
            int qty = (Integer) item.get("quantity");
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));
            BigDecimal discounted = lineTotal.subtract(lineTotal.multiply(discount).divide(BigDecimal.valueOf(100)));

            // overrideTotal lets the request simply dictate the final price,
            // bypassing the computed amount entirely if present.
            BigDecimal finalTotal = discounted;
            if (overrideTotal != null && !overrideTotal.isBlank()) {
                try {
                    finalTotal = new BigDecimal(overrideTotal.trim());
                } catch (NumberFormatException ignored) {
                    // fall back to the computed total on garbage input
                }
            }

            String status = (orderStatus != null && !orderStatus.isBlank()) ? orderStatus : "Pending";

            UUID orderId = UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO orders (id, user_id, product_id, name, product_name, products_bought, " +
                            "voucher_discount, product_price, quantity, total_price, order_date, order_status, shipping_notes) " +
                            "VALUES (?, ?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, ?, now(), ?, ?)",
                    orderId, effectiveUserId, item.get("productId"), shippingName, item.get("productName"),
                    qty, discount, price, qty, finalTotal, status, shippingNotes);
            createdOrderIds.add(orderId.toString());
        }

        session.removeAttribute("cart");
        model.addAttribute("orderIds", createdOrderIds);
        model.addAttribute("shippingNotes", shippingNotes);
        return "order-confirmation";
    }

    // IDOR: any order, any user, no ownership check.
    @GetMapping("/order/{id}")
    public String viewOrder(@PathVariable String id, Model model) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT o.id, o.product_name, o.quantity, o.total_price, o.order_status, " +
                        "o.shipping_notes, o.order_date, u.name, u.lastname, u.email_address " +
                        "FROM orders o JOIN users u ON u.id = o.user_id WHERE o.id = ?::uuid", id);
        if (rows.isEmpty()) {
            model.addAttribute("error", "Order not found");
            return "order-confirmation";
        }
        model.addAttribute("order", rows.get(0));
        return "order-confirmation";
    }
}
