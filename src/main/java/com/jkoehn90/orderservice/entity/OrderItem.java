package com.jkoehn90.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;                    // Reference back to the parent order

    @Column(nullable = false)
    private Long productId;                 // References the product in Product Service

    @Column(nullable = false)
    private Integer quantity;               // How many of this product were ordered

    @Column(nullable = false)
    private Double price;                   // Price at time of order
}
