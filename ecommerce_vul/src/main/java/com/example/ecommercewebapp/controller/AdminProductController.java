package com.example.ecommercewebapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

/**
 * VULNERABLE ON PURPOSE - admin "add product" page.
 *
 * - File upload has no extension/content-type allowlist and no filename
 *   sanitization: the browser-supplied filename is used as-is, so a name
 *   containing "../" escapes the uploads directory (path traversal /
 *   arbitrary file write), and files like .html or .svg are stored and
 *   served back with script content intact (stored XSS via upload).
 * - The image URL field is stored and rendered as-is with no validation,
 *   and is also fetchable through /image-proxy (SSRF).
 * - This page is only "protected" by AdminAccessFilter's forgeable cookie
 *   check, same as /products, /users, /orders.
 */
@Controller
public class AdminProductController {

    private static final String UPLOAD_DIR = "uploads";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/admin/products/new")
    public String newProductForm() {
        return "admin-product-form";
    }

    @PostMapping("/admin/products/new")
    public String createProduct(@RequestParam String productName,
                                 @RequestParam String productPrice,
                                 @RequestParam String noOfQuantity,
                                 @RequestParam(required = false) String voucherDiscount,
                                 @RequestParam(required = false) String expirationDate,
                                 @RequestParam(required = false) String imageUrl,
                                 @RequestParam(required = false) MultipartFile imageFile,
                                 Model model) throws Exception {

        String finalImageUrl = imageUrl;
        BigDecimal price = new BigDecimal(productPrice.trim());
        Integer quantity = Integer.valueOf(noOfQuantity.trim());
        BigDecimal discount = (voucherDiscount == null || voucherDiscount.isBlank())
                ? BigDecimal.ZERO : new BigDecimal(voucherDiscount.trim());

        if (imageFile != null && !imageFile.isEmpty()) {
            Path uploadDir = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadDir);

            // No sanitization: "../../foo.html" in the filename escapes the
            // uploads directory; no extension allowlist either.
            String rawName = imageFile.getOriginalFilename();
            Path target = Paths.get(UPLOAD_DIR, rawName);
            Files.createDirectories(target.getParent());
            imageFile.transferTo(target.toFile());

            finalImageUrl = "/uploads/" + rawName;
        }

        LocalDate exp = null;
        if (expirationDate != null && !expirationDate.isBlank()) {
            exp = LocalDate.parse(expirationDate);
        }

        jdbcTemplate.update(
                "INSERT INTO products (id, product_name, expiration_date, voucher_discount, " +
                        "no_of_quantity, product_price, image_url) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), productName, exp, discount, quantity, price, finalImageUrl);

        return "redirect:/products";
    }
}
