package com.example.ecommercewebapp.service;

import com.example.ecommercewebapp.dto.JointProbabilityDto;
import com.example.ecommercewebapp.model.Order;
import com.example.ecommercewebapp.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Computes joint probability P(product ∩ city) using all orders.
     * City is roughly extracted from the user's address (first part before comma).
     */
    public List<JointProbabilityDto> computeProductCityJointProbability() {
        // Fetch all orders with user and product details (your existing method)
        List<Order> allOrders = orderRepository.findAllWithDetails();
        long totalOrders = allOrders.size();
        if (totalOrders == 0) {
            return Collections.emptyList();
        }

        // Count co-occurrences of (productName, city)
        Map<String, Long> pairCount = new HashMap<>();

        for (Order order : allOrders) {
            String productName = order.getProduct().getProductName();
            String address = order.getUser().getAddress();
            String city = extractCity(address);
            String key = productName + "|" + city;
            pairCount.put(key, pairCount.getOrDefault(key, 0L) + 1);
        }

        // Convert to DTO list and calculate joint probability
        List<JointProbabilityDto> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : pairCount.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String product = parts[0];
            String city = parts[1];
            Long count = entry.getValue();
            double prob = count / (double) totalOrders;

            result.add(new JointProbabilityDto(product, city, count, prob));
        }

        // Sort descending by probability (most probable first)
        result.sort((a, b) -> Double.compare(b.getJointProbability(), a.getJointProbability()));
        return result;
    }

    /**
     * Simple heuristic to extract city from address.
     * You can replace this with a more robust logic (e.g., using a geocoding library).
     */
    private String extractCity(String address) {
        if (address == null || address.isBlank()) {
            return "Unknown";
        }
        // Example: address = "Manila, Philippines" -> "Manila"
        String[] parts = address.split(",");
        if (parts.length > 0) {
            return parts[0].trim();
        }
        // Fallback: first word
        String[] words = address.split(" ");
        return words[0];
    }
}