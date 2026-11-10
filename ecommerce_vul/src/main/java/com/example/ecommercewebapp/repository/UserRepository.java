package com.example.ecommercewebapp.repository;

import com.example.ecommercewebapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    
    Page<User> findByNameContainingIgnoreCaseOrLastnameContainingIgnoreCase(
            String name, String lastname, Pageable pageable);

    User findByUsername(String username);
}