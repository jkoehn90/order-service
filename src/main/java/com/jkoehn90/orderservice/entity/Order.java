package com.jkoehn90.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;                    // References the user who placed the order

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;          // List of items in the order

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;             // Current status of the order

    @Column(nullable = false)
    private Double totalAmount;             // Total cost of the order

    @Column(nullable = false)
    private LocalDateTime createdAt;        // When the order was placed
}
