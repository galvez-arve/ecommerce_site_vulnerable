package com.example.ecommercewebapp.controller;

import com.example.ecommercewebapp.model.Order;
import com.example.ecommercewebapp.model.Product;
import com.example.ecommercewebapp.model.User;
import com.example.ecommercewebapp.repository.OrderRepository;
import com.example.ecommercewebapp.repository.ProductRepository;
import com.example.ecommercewebapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class WebController {

    @Autowired
    private UserRepository userRepository;
   

    @Autowired
    private ProductRepository productRepository;
   
    @Autowired
    private OrderRepository orderRepository;

    // Public entry point: the storefront, not the admin dashboard.
    @GetMapping("/")
    public String home() {
        return "redirect:/catalog";
    }

    // The old dashboard now lives at /dashboard and is gated by
    // AdminAccessFilter the same (weak, cookie-based) way as /products,
    // /users, /orders and /analytics.
    @GetMapping("/dashboard")
    public String dashboard() {
        return "index";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "users";
    }

    @GetMapping("/products")
    public String listProducts(Model model) {
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        return "products";
    }

    @GetMapping("/orders")
    public String listOrders(Model model) {
        List<Order> orders = orderRepository.findAllWithDetails();
        model.addAttribute("orders", orders);
        return "orders";
    }
    @GetMapping("/analytics")
    public String analyticsPage() {
        return "analytics";   // corresponds to analytics.html in templates
    }
}