package com.example.ecommercewebapp.rest;

import com.example.ecommercewebapp.model.Order;
import com.example.ecommercewebapp.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/orders")
public class OrderRestController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping
    public Map<String, Object> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Order> orderPage;

        if (search != null && !search.trim().isEmpty()) {
            orderPage = orderRepository.searchOrders(search.trim(), pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        // Convert each Order entity to a simplified map for the frontend
        List<Map<String, Object>> ordersList = orderPage.getContent().stream()
                .map(order -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", order.getId().toString());
                    // Concatenate user's first and last name – adjust to your actual fields
                    map.put("userId", order.getUser().getId().toString());
                    String customerName = order.getUser().getName() + " " + order.getUser().getLastname();
                    map.put("customerName", customerName);
                    map.put("productName", order.getProduct().getProductName());
                    map.put("quantity", order.getQuantity());
                    map.put("totalPrice", order.getTotalPrice());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
                    map.put("orderDate", order.getOrderDate().format(formatter));
                    map.put("orderStatus", order.getOrderStatus());
                    // Shipping notes go straight from the DB to JSON, no
                    // sanitization - the admin orders page renders them with
                    // innerHTML, so this is the stored-XSS payload path.
                    map.put("shippingNotes", order.getShippingNotes());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("orders", ordersList);
        response.put("currentPage", orderPage.getNumber());
        response.put("totalPages", orderPage.getTotalPages());
        response.put("totalItems", orderPage.getTotalElements());
        return response;
    }
    private static final Logger logger = LoggerFactory.getLogger(OrderRestController.class);
    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updateOrderStatus(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        logger.info("Received PATCH for order {} with new status: {}", id, newStatus);

        if (newStatus == null || newStatus.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
        }

        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            logger.warn("Order {} not found", id);
            return ResponseEntity.notFound().build();
        }

        Order order = orderOpt.get();
        logger.info("Order before update: id={}, status={}", order.getId(), order.getOrderStatus());
        order.setOrderStatus(newStatus);
        Order savedOrder = orderRepository.saveAndFlush(order); // Force immediate flush
        logger.info("Order after update: id={}, status={}", savedOrder.getId(), savedOrder.getOrderStatus());

        return ResponseEntity.ok(Map.of("id", id.toString(), "status", newStatus));
    }
}