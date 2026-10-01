# GuestHouse Booking System

A web application for managing guest house reservations, built with Spring Boot, Thymeleaf, and Docker.

Production: https://guesthouse-booking-system-production-87cb.up.railway.app

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

## Workflow

### Branches

`main` is the only long-lived branch and always matches production. All work happens on short-lived branches cut from `main`, named after the kind of change:

* `feat/...` for new features
* `fix/...` for bug fixes
* `chore/...` for configuration and cleanup
* `docs/...` for documentation
* `test/...` for tests

A ruleset on GitHub protects `main`. Nobody can push to it directly, force-push, or delete it.

### From branch to production

1. Create a branch from `main` and commit your change.
2. Push the branch and open a pull request against `main`. Fill in the PR template.
3. GitHub Actions runs `.github/workflows/ci.yml`, which runs the tests and builds the JAR. CodeQL scans the code in parallel.
4. Another group member reviews the PR. Merging requires one approval and a green `Test and Build` check, and the branch must be up to date with `main`. A new push dismisses earlier approvals.
5. Merge the PR. CI runs again on `main`, then pushes a Docker image to Docker Hub tagged with the commit SHA and `latest`.
6. Railway watches `main` and deploys the merge commit to both the `staging` and `production` environments. The deployment shows up under Deployments on the GitHub repository.
7. Check that the service is up at https://guesthouse-booking-system-production-87cb.up.railway.app/actuator/health.

To roll back, revert the merge commit in a new PR. It goes through the same steps.

## Repository Structure Note for Docker Compose
For `docker compose up --build` to locate all service directories correctly using the relative build contexts, ensure that both repositories (`GuestHouse-Booking-System`, `GuestHouse-Customer-Service`) and your infrastructure repository (`GuestHouse-Infrastructure`) are placed within the same parent folder like this:

```text
📁 parent-folder/
├── 📁 GuestHouse-Infrastructure/  (contains docker-compose.yml)
├── 📁 GuestHouse-Booking-System/
└── 📁 GuestHouse-Customer-Service/
```

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
