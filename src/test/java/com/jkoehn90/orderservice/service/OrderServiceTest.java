package com.jkoehn90.orderservice.service;

import com.jkoehn90.orderservice.dto.*;
import com.jkoehn90.orderservice.entity.Order;
import com.jkoehn90.orderservice.entity.OrderItem;
import com.jkoehn90.orderservice.entity.OrderStatus;
import com.jkoehn90.orderservice.kafka.OrderEventProducer;
import com.jkoehn90.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private OrderService orderService;

    private Order mockOrder;
    private OrderItem mockOrderItem;
    private OrderRequest orderRequest;
    private OrderItemRequest orderItemRequest;

    @BeforeEach
    void setUp() {
        mockOrderItem = OrderItem.builder()
                .id(1L)
                .productId(1L)
                .quantity(2)
                .price(799.99)
                .build();

        mockOrder = Order.builder()
                .id(1L)
                .userId(1L)
                .items(List.of(mockOrderItem))
                .status(OrderStatus.PENDING)
                .totalAmount(1599.98)
                .createdAt(LocalDateTime.now())
                .build();

        mockOrderItem.setOrder(mockOrder);

        orderItemRequest = OrderItemRequest.builder()
                .productId(1L)
                .quantity(2)
                .build();

        orderRequest = OrderRequest.builder()
                .userId(1L)
                .items(List.of(orderItemRequest))
                .build();
    }

    // ─── Place Order Tests ────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void placeOrder_ShouldReturnOrderResponse_WhenValidRequest() {
        // Arrange — mock WebClient chain to return product details
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("price", 799.99)));

        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        doNothing().when(orderEventProducer)
                .sendOrderPlacedEvent(anyLong(), anyLong(), anyDouble());

        // Act
        OrderResponse response = orderService.placeOrder(orderRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(1599.98, response.getTotalAmount());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventProducer, times(1))
                .sendOrderPlacedEvent(anyLong(), anyLong(), anyDouble());
    }

    @Test
    @SuppressWarnings("unchecked")
    void placeOrder_ShouldPublishKafkaEvent_AfterSaving() {
        // Arrange
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("price", 799.99)));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        doNothing().when(orderEventProducer)
                .sendOrderPlacedEvent(anyLong(), anyLong(), anyDouble());

        // Act
        orderService.placeOrder(orderRequest);

        // Assert — Kafka event must be published exactly once
        verify(orderEventProducer, times(1))
                .sendOrderPlacedEvent(1L, 1L, 1599.98);
    }

    // ─── Get Orders By User Tests ─────────────────────────────────────────────

    @Test
    void getOrdersByUser_ShouldReturnOrders_WhenUserHasOrders() {
        // Arrange
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(mockOrder));

        // Act
        List<OrderResponse> responses = orderService.getOrdersByUser(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getUserId());
        assertEquals(OrderStatus.PENDING, responses.get(0).getStatus());
    }

    @Test
    void getOrdersByUser_ShouldReturnEmptyList_WhenUserHasNoOrders() {
        // Arrange
        when(orderRepository.findByUserId(anyLong())).thenReturn(List.of());

        // Act
        List<OrderResponse> responses = orderService.getOrdersByUser(99L);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    // ─── Get Order By ID Tests ────────────────────────────────────────────────

    @Test
    void getOrderById_ShouldReturnOrder_WhenOrderExists() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        // Act
        OrderResponse response = orderService.getOrderById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1599.98, response.getTotalAmount());
    }

    @Test
    void getOrderById_ShouldThrowException_WhenOrderNotFound() {
        // Arrange
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.getOrderById(99L));

        assertEquals("Order not found with id: 99", exception.getMessage());
    }

    // ─── Update Order Status Tests ────────────────────────────────────────────

    @Test
    void updateOrderStatus_ShouldReturnUpdatedOrder_WhenOrderExists() {
        // Arrange
        Order confirmedOrder = Order.builder()
                .id(1L)
                .userId(1L)
                .items(List.of(mockOrderItem))
                .status(OrderStatus.CONFIRMED)
                .totalAmount(1599.98)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(confirmedOrder);

        // Act
        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        // Assert
        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_ShouldThrowException_WhenOrderNotFound() {
        // Arrange
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.updateOrderStatus(99L, OrderStatus.CONFIRMED));

        assertEquals("Order not found with id: 99", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }
}
