package com.example.demo;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name="orders")
public class orderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private String productName;

    private double price;        // ✅ lowercase, double not Long

    private String image;

    private String customerName;

    private String email;

    private String address;

    private String mobile_No;

    private int quantity;        // ✅ fixed from "setQuantity"

    private String status;       // ✅ add this — "confirmed/shipped/delivered"

    private LocalDateTime orderDate;

    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
    }
}