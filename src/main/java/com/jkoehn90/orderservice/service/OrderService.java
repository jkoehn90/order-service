package com.jkoehn90.orderservice.service;

import com.jkoehn90.orderservice.dto.*;
import com.jkoehn90.orderservice.entity.Order;
import com.jkoehn90.orderservice.entity.OrderItem;
import com.jkoehn90.orderservice.entity.OrderStatus;
import com.jkoehn90.orderservice.kafka.OrderEventProducer;
import com.jkoehn90.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final WebClient.Builder webClientBuilder;

    // Place a new order
    public OrderResponse placeOrder(OrderRequest request) {

        // 1. Build order items by fetching price from Product Service for each item
        List<OrderItem> orderItems = request.getItems().stream()
                .map(itemRequest -> {
                    // Call Product Service to get product details
                    Map productDetails = webClientBuilder.build()
                            .get()
                            .uri("http://product-service/products/" + itemRequest.getProductId())
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

                    Double price = ((Number) productDetails.get("price")).doubleValue();

                    return OrderItem.builder()
                            .productId(itemRequest.getProductId())
                            .quantity(itemRequest.getQuantity())
                            .price(price)
                            .build();
                })
                .collect(Collectors.toList());

        // 2. Calculate total amount
        Double totalAmount = orderItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        // 3. Build and save the order
        Order order = Order.builder()
                .userId(request.getUserId())
                .items(orderItems)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .createdAt(LocalDateTime.now())
                .build();

        // 4. Set the order reference on each item
        orderItems.forEach(item -> item.setOrder(order));

        Order saved = orderRepository.save(order);

        // 5. Publish Kafka event
        orderEventProducer.sendOrderPlacedEvent(saved.getId(), saved.getUserId(), saved.getTotalAmount());

        return mapToResponse(saved);
    }

    // Get all orders for a user
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get a single order by ID
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    // Update order status
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        order.setStatus(status);
        Order updated = orderRepository.save(order);
        return mapToResponse(updated);
    }

    // Map Order entity to OrderResponse DTO
    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .items(itemResponses)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
