package com.jkoehn90.orderservice.dto;

import com.jkoehn90.orderservice.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long userId;
    private List<OrderItemResponse> items;
    private OrderStatus status;
    private Double totalAmount;
    private LocalDateTime createdAt;
}
