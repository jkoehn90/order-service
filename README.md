# Order Service — Microservices System

Part of the **Microservices System** — a distributed backend built with Java, Spring Boot, and Spring Cloud.

## Overview

The Order Service handles **order placement and management** for the Microservices System. It communicates synchronously with the Product Service via `WebClient` to validate and price products, and publishes asynchronous events to Apache Kafka when orders are placed, enabling an event-driven architecture.

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Core language |
| Spring Boot 3.5.x | Application framework |
| Spring Data JPA | Database ORM |
| PostgreSQL | Relational database |
| Apache Kafka | Asynchronous event publishing |
| Spring WebFlux (WebClient) | Inter-service HTTP communication |
| Lombok | Boilerplate reduction |
| Netflix Eureka Client | Service discovery |

## Architecture Role

```
Client Request
      │
      ▼
API Gateway (port 8080)
      │
      ▼ routes /orders/**
Order Service (port 8083)
      │              │
      ▼              ▼
PostgreSQL      Product Service     Kafka
(orderdb)    (price lookup via    (publishes
              WebClient)         order-placed
                                   event)
```

## API Endpoints

All endpoints require a valid JWT token passed via the `Authorization: Bearer <token>` header.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/orders` | Place a new order |
| `GET` | `/orders/user/{userId}` | Get all orders for a user |
| `GET` | `/orders/{id}` | Get order by ID |
| `PATCH` | `/orders/{id}/status?status=` | Update order status |

### Place Order Request
```json
{
    "userId": 1,
    "items": [
        {
            "productId": 1,
            "quantity": 2
        }
    ]
}
```

### Order Response
```json
{
    "id": 1,
    "userId": 1,
    "items": [
        {
            "productId": 1,
            "quantity": 2,
            "price": 799.99
        }
    ],
    "status": "PENDING",
    "totalAmount": 1599.98,
    "createdAt": "2026-04-28T21:43:06"
}
```

### Order Statuses
`PENDING` → `CONFIRMED` → `SHIPPED` → `DELIVERED` → `CANCELLED`

## Kafka Events

When an order is placed, the Order Service publishes an event to the `order-placed` Kafka topic:

```json
{
    "orderId": 1,
    "userId": 1,
    "totalAmount": 1599.98
}
```

Any downstream service (e.g. Notification Service, Payment Service) can subscribe to this topic to react to new orders without tight coupling.

## Project Structure

```
src/main/java/com/yourname/orderservice/
├── controller/
│   └── OrderController.java
├── service/
│   └── OrderService.java
├── repository/
│   └── OrderRepository.java
├── entity/
│   ├── Order.java
│   ├── OrderItem.java
│   └── OrderStatus.java
├── dto/
│   ├── OrderRequest.java
│   ├── OrderResponse.java
│   ├── OrderItemRequest.java
│   └── OrderItemResponse.java
├── kafka/
│   └── OrderEventProducer.java
└── config/
    └── WebClientConfig.java
```

## Getting Started

### Prerequisites
- Java 17+
- Maven
- PostgreSQL (or Docker)
- Apache Kafka + Zookeeper (or Docker)
- Eureka Server running on port `8761`
- Product Service running on port `8082`

### Database Setup (Docker)
```bash
docker run --name orderdb-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=yourpassword \
  -e POSTGRES_DB=orderdb \
  -p 5434:5432 \
  -d postgres:15
```

### Kafka Setup (Docker)
```bash
docker network create kafka-network

docker run --name zookeeper --network kafka-network -e ZOOKEEPER_CLIENT_PORT=2181 -e ZOOKEEPER_TICK_TIME=2000 -p 2181:2181 -d confluentinc/cp-zookeeper:7.4.0

docker run --name kafka --network kafka-network -e KAFKA_BROKER_ID=1 -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -p 9092:9092 -d confluentinc/cp-kafka:7.4.0
```

### Running Locally
```bash
mvn spring-boot:run
```

## Related Services

| Service | Port | Repo |
|---|---|---|
| Eureka Server | 8761 | [eureka-server](../eureka-server) |
| API Gateway | 8080 | [api-gateway](../api-gateway) |
| User Service | 8081 | [user-service](../user-service) |
| Product Service | 8082 | [product-service](../product-service) |