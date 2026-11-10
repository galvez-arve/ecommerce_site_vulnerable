package com.example.ecommercewebapp.repository;

import com.example.ecommercewebapp.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByProductNameContainingIgnoreCase(String productName, Pageable pageable);
}