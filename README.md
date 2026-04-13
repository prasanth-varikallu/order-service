# Order Service

The **Order Service** handles the order placement process for the Retail Management microservices demo. It provides a RESTful interface to accept customer orders and coordinates with other services via asynchronous events to ensure a responsive and reliable user experience.

## Features
- **Order Processing**: Accepts customer orders and initiates the reservation flow.
- **Reactive Stack**: Built with Spring Boot WebFlux and R2DBC for a fully non-blocking data access layer.
- **Asynchronous Flow**:
  - Accepts an order request and immediately returns a processing status to the user.
  - Publishes `OrderCreated` events to Kafka to initiate stock reservation in the Inventory Service.
  - Consumes outcomes from the Inventory Service to finalize or cancel orders.
- **Strong Persistence**: Uses H2 database with R2DBC for transactional order storage.
- **High Observability**: Integrated with Zipkin for distributed tracing across the order lifecycle.

## Tech Stack
- **Java 25**
- **Spring Boot 3.5.x** (WebFlux)
- **Spring Data R2DBC** (H2)
- **Spring Kafka**
- **Micrometer Tracing + Zipkin**

## Configuration
- **Port**: 8080 (internal)
- **Database**: `r2dbc:h2:mem:///orderdb`
- **Kafka**: `kafka:9092`
- **Zipkin**: `http://zipkin:9411/api/v2/spans`

## Running the Service

### Prerequisites
- Java 25
- Maven
- Kafka running (or use Docker Compose)

### Locally
```bash
./mvnw spring-boot:run
```

### Docker
```bash
docker build -t order-service .
docker run -p 8080:8080 order-service
```
