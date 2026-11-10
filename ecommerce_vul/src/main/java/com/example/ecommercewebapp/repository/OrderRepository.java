package com.example.ecommercewebapp.repository;

import com.example.ecommercewebapp.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Method used by WebController for the HTML page
    @Query("SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.product")
    List<Order> findAllWithDetails();

    // Method used by OrderRestController for search and pagination
    @Query("SELECT o FROM Order o " +
           "JOIN o.user u " +
           "JOIN o.product p " +
           "WHERE LOWER(CONCAT(u.name, ' ', u.lastname)) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Order> searchOrders(@Param("search") String search, Pageable pageable);
}