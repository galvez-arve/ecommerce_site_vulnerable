package com.example.ecommercewebapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "voucher_discount", nullable = false)
    private BigDecimal voucherDiscount;

    @Column(name = "no_of_quantity", nullable = false)
    private Integer noOfQuantity;

    @Column(name = "product_price", nullable = false)
    private BigDecimal productPrice;

    @Column(name = "image_url")
    private String imageUrl;

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // ============================================
    // GETTERS AND SETTERS (Required for Thymeleaf!)
    // ============================================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public BigDecimal getVoucherDiscount() {
        return voucherDiscount;
    }

    public void setVoucherDiscount(BigDecimal voucherDiscount) {
        this.voucherDiscount = voucherDiscount;
    }

    public Integer getNoOfQuantity() {
        return noOfQuantity;
    }

    public void setNoOfQuantity(Integer noOfQuantity) {
        this.noOfQuantity = noOfQuantity;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }
}