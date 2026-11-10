package com.example.ecommercewebapp.rest;

import com.example.ecommercewebapp.model.User;
import com.example.ecommercewebapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    @Autowired
    private UserRepository userRepository;

    // Existing endpoint: paginated list of users with search
    @GetMapping
    public Map<String, Object> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, 
                Sort.by("name").ascending().and(Sort.by("lastname").ascending()));
        Page<User> userPage;

        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.findByNameContainingIgnoreCaseOrLastnameContainingIgnoreCase(
                    search.trim(), search.trim(), pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());
        response.put("currentPage", userPage.getNumber());
        response.put("totalPages", userPage.getTotalPages());
        response.put("totalItems", userPage.getTotalElements());
        return response;
    }

    // 👇 New endpoint: fetch a single user by ID
    @GetMapping("/{id}")
    public Map<String, Object> getUserById(@PathVariable UUID id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId().toString());
            userMap.put("name", user.getName());
            userMap.put("lastname", user.getLastname());
            userMap.put("fullName", user.getName() + " " + user.getLastname());
            userMap.put("address", user.getAddress());
            userMap.put("contactNumber", user.getContactNumber());
            // Add location or other fields if present in your User entity
            userMap.put("emailAddress", user.getemailAddress());
            return userMap;
        } else {
            throw new RuntimeException("User not found with id: " + id);
        }
    }
}