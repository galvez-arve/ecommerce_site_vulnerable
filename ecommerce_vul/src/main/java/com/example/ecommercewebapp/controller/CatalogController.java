package com.example.ecommercewebapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * VULNERABLE ON PURPOSE - public-facing shop pages.
 *
 * - /catalog/product uses a raw string-concatenated SQL query instead of
 *   the UUID-typed JPA repository, so it accepts arbitrary input and is
 *   injectable (e.g. id=' UNION SELECT ... -- ).
 * - DB errors are shown to the visitor (information disclosure, helps
 *   blind/error-based injection).
 * - The "no results" search message is rendered with th:utext, so it is
 *   reflected XSS.
 */
@Controller
public class CatalogController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/catalog")
    public String catalog(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("searchTerm", q);
        return "catalog";
    }

    @GetMapping("/catalog/product")
    public String productDetail(@RequestParam String id, Model model) {
        String sql = "SELECT id, product_name, expiration_date, voucher_discount, " +
                "no_of_quantity, product_price, image_url FROM products WHERE id = '" + id + "'";

        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            model.addAttribute("error", "Database error: " + e.getMessage());
            model.addAttribute("debugSql", sql);
            return "product";
        }

        if (rows.isEmpty()) {
            model.addAttribute("notFoundMessage", "No product found for id: " + id);
            return "product";
        }

        model.addAttribute("product", rows.get(0));
        // If the injection returns extra columns/rows (e.g. via UNION SELECT),
        // surface them too so the technique is visibly rewarded.
        if (rows.size() > 1) {
            model.addAttribute("extraRows", rows.subList(1, rows.size()));
        }
        return "product";
    }
}
