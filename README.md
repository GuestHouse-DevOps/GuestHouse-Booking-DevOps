# GuestHouse Booking System

A web application for managing guest house reservations, built with Spring Boot, Thymeleaf, and Docker.

## Microservices
This application is part of an interconnected microservices architecture and works together with the **Customer Service**. It communicates with the Customer Service via REST to fetch customer details and validate bookings.

## Features
* **Customers:** Register, update, and delete customers.
* **Rooms:** Manage single and double rooms with optional extra beds.
* **Bookings:** Create and manage reservations with automatic double-booking prevention.
* **Search:** Find available rooms by date range and guest capacity.

## Tech Stack
* Java 21
* Spring Boot 4.0.6
* Spring Data JPA & Hibernate
* MySQL
* Thymeleaf & Bootstrap 5
* Docker

## Repository Structure Note for Docker Compose
For `docker compose up --build` to locate all service directories correctly using the relative build contexts, ensure that both repositories (`GuestHouse-Booking-System`, `GuestHouse-Customer-Service`) and your infrastructure repository (`GuestHouse-Infrastructure`) are placed within the same parent folder like this:

```text
📁 parent-folder/
├── 📁 GuestHouse-Infrastructure/  (contains docker-compose.yml)
├── 📁 GuestHouse-Booking-System/
└── 📁 GuestHouse-Customer-Service/

## Change History
Every create, update and delete of a booking, room or customer writes a row to the `audit_event` table with the entity type, entity id, action and time. To see one booking's history:

```sql
SELECT action, occurred_at FROM audit_event
WHERE entity_type = 'BOOKING' AND entity_id = 42
ORDER BY occurred_at DESC;
```

Customer events are written by this service after Customer Service confirms the change.
## Running Tests
Tests start their own MySQL 8.0 container through Testcontainers, so Docker must be running. No database or environment variables are needed.

```bash
./mvnw test
```

Use JDK 21. Lombok fails to compile on newer JDKs.

## Health Check

The service exposes `/actuator/health` through Spring Boot Actuator.

Railway is connected to this endpoint and continuously checks that the service is running.

The response lists each component's status. Besides the database, the
`customerService` component calls Customer Service's `/api/customers`.
If Customer Service is unreachable or returns an error, the whole
endpoint reports `DOWN`.

Test locally:

```text
http://localhost:8080/actuator/health
```

In production:

```text
https://guesthouse-booking-system-production-87cb.up.railway.app/actuator/health
```
