# GuestHouse Booking System

A web application for managing guest house reservations,
built with Spring Boot, Thymeleaf, and Docker.

**Production:** https://guesthouse-booking-system-production-87cb.up.railway.app
**Staging:** https://guesthouse-booking-devops-staging.up.railway.app

## Team

- Maruf Mulk (marufmulk)
- Joakim Samuelsson (joakim-epp)

## Microservices

This application is part of an interconnected microservices
architecture and works together with the Customer Service.
It communicates with the Customer Service via REST to fetch
customer details and validate bookings.

## Features

- **Customers:** Register, update, and delete customers.
- **Rooms:** Manage single and double rooms with optional extra beds.
- **Bookings:** Create and manage reservations with automatic
  double-booking prevention.
- **Search:** Find available rooms by date range and guest capacity.

## Tech Stack

- Java 21
- Spring Boot 4.0.6
- Spring Data JPA & Hibernate
- MySQL
- Thymeleaf & Bootstrap 5
- Docker

## Workflow

### Branches

`main` is the only long-lived branch and always matches
production. All work happens on short-lived branches cut
from `main`, named after the kind of change:

- `feat/...` for new features
- `fix/...` for bug fixes
- `chore/...` for configuration and cleanup
- `docs/...` for documentation
- `test/...` for tests

A ruleset on GitHub protects `main`. Nobody can push to it
directly, force-push, or delete it.

**Why feature branches over trunk-based development**

We are two people working asynchronously and not always
available at the same time. Feature branches let each person
work independently and open a PR when ready, without blocking
the other.

Without branch protection and mandatory code review, the
merge conflict in `CustomerServiceHealthIndicator.java` would
likely have caused one person to silently overwrite the
other's changes without noticing. Code review also caught
that timeout values were hardcoded, which we fixed before
the code reached production.

### From branch to production

1. Create a branch from `main` and commit your change.
2. Push the branch and open a pull request against `main`.
   Fill in the PR template.
3. GitHub Actions runs `.github/workflows/ci.yml`, which
   runs the tests and builds the JAR. CodeQL scans the code
   in parallel.
4. Another group member reviews the PR. Merging requires one
   approval and a green `Test and Build` check, and the
   branch must be up to date with `main`. A new push
   dismisses earlier approvals.
5. Merge the PR. CI runs again on `main`, then pushes a
   Docker image to Docker Hub tagged with the commit SHA
   and `latest`.
6. Railway deploys the merge commit automatically to
   **staging**. Verify that staging looks correct before
   deploying to production.
7. Trigger production deploy manually via
   **GitHub Actions → Run workflow → deploy_to_production: true**.
8. Check that the service is up at the health endpoint.

### Environments

| Environment | URL | Deploy trigger |
|---|---|---|
| Staging | https://guesthouse-booking-devops-staging.up.railway.app | Automatic on merge to main |
| Production | https://guesthouse-booking-system-production-87cb.up.railway.app | Manual via workflow_dispatch |

Both environments use the same Docker image. Only environment
variables differ between them.

## Rollback

Railway builds production from `main`, so a rollback on
Railway means changing `main`. Every image CI pushes is also
on Docker Hub, tagged with its commit SHA, so you can run
any earlier version as a container.

**Option 1: revert the last commit**

Revert the latest commit on `main` and push the revert
through a pull request, since nobody can push to `main`
directly:

```bash
git switch main
git pull
git switch -c fix/revert-bad-release
git revert HEAD          # for a merge commit: git revert -m 1 HEAD
git push -u origin fix/revert-bad-release
```

Open a PR against `main`. Once it is merged, CI builds a
new image and Railway deploys the reverted code to staging
and production.

**Option 2: run a specific version from Docker Hub**

Images live at
https://hub.docker.com/r/marufmulk/guesthouse-booking-service/tags.
Each tag is the full SHA of a commit on `main`, and `latest`
points to the newest one. Find the SHA of the last good
release:

```bash
git log --oneline main
```

Then pull and run that image:

```bash
docker pull marufmulk/guesthouse-booking-service:<commit-sha>
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/<database> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  -e CUSTOMER_SERVICE_URL=<customer-service-url> \
  marufmulk/guesthouse-booking-service:<commit-sha>
```

The container needs a MySQL database and a running Customer
Service, so pass their addresses as above.

To use it with Docker Compose, set
`image: marufmulk/guesthouse-booking-service:<commit-sha>`
for the booking service in
`GuestHouse-Infrastructure/docker-compose.yml`.

This does not change what Railway runs. Production only
moves back when `main` does, through option 1.

## Merge Conflict

### How it happened

The conflict occurred in `CustomerServiceHealthIndicator.java`
when a PR was being merged into a newer version of `main`
where the same health code had already been updated.

### How we resolved it

We kept the new structure from `main` and added the
timeout configuration, Docker mapping and `latencyMs` from
the incoming branch on top of it. Tests were updated and
re-run to verify that everything still worked.

The resolved PR can be seen here:
https://github.com/GuestHouse-DevOps/GuestHouse-Booking-DevOps/pull/49/changes/b67806a0f882ca4cffd17d5be187be9e5deadcbf

### What triggered the code review that led to the conflict

During code review, feedback was given that the timeout
values for Customer Service were hardcoded in `RestClientConfig`.
We updated them so that connect timeout and read timeout are
controlled via `CUSTOMER_SERVICE_CONNECT_TIMEOUT` and
`CUSTOMER_SERVICE_READ_TIMEOUT` in Railway Variables, with
defaults of 2 and 10 seconds respectively. This prevents
the health check from blocking for an unreasonable amount of
time if Customer Service stalls instead of refusing the
connection outright. These changes were part of the branch
that caused the conflict when merging.

## Repository Structure Note for Docker Compose

For `docker compose up --build` to locate all service
directories correctly using the relative build contexts,
ensure that both repositories (`GuestHouse-Booking-System`,
`GuestHouse-Customer-Service`) and your infrastructure
repository (`GuestHouse-Infrastructure`) are placed within
the same parent folder like this:

```
📁 parent-folder/
├── 📁 GuestHouse-Infrastructure/  (contains docker-compose.yml)
├── 📁 GuestHouse-Booking-System/
└── 📁 GuestHouse-Customer-Service/
```

## Change History

Every create, update and delete of a booking, room or
customer writes a row to the `audit_event` table with the
entity type, entity id, action and time. To see one
booking's history:

```sql
SELECT action, occurred_at FROM audit_event
WHERE entity_type = 'BOOKING' AND entity_id = 42
ORDER BY occurred_at DESC;
```

Customer events are written by this service after Customer
Service confirms the change.

## Running Tests

Tests start their own MySQL 8.0 container through
Testcontainers, so Docker must be running. No database or
environment variables are needed.

```bash
./mvnw test
```

Use JDK 21. Lombok fails to compile on newer JDKs.

## Health Check

The service exposes `/actuator/health` through Spring Boot
Actuator.

Railway is connected to `/actuator/health/liveness` which
only includes the database and ping. This means that if
Customer Service is unreachable, Railway does not restart
the booking service container unnecessarily. The full
`/actuator/health` endpoint still shows the customer service
status.

The `customerService` component calls Customer Service's
`/api/customers`. If Customer Service is unreachable or
returns an error, that component reports `DOWN` but the
overall liveness status remains `UP` as long as the database
is healthy.

The `customerService` details include `responseTimeMs`, the
elapsed check time in milliseconds, for both successful and
failed checks. Timeout values are configurable via
`CUSTOMER_SERVICE_CONNECT_TIMEOUT` and
`CUSTOMER_SERVICE_READ_TIMEOUT` in Railway Variables without
needing to rebuild the application.

INFO is used for normal operation, for example when a health
check starts and succeeds. WARN is used when Customer Service
cannot be reached or the check fails. Sensitive information
such as passwords and personal data is never logged.

Test locally:
```
http://localhost:8080/actuator/health
```

In production:
```
https://guesthouse-booking-system-production-87cb.up.railway.app/actuator/health
```