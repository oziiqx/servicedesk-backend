# ServiceDesk

Backend for a generic services business (bookings, staff scheduling, dynamic pricing, invoicing).
Built as a portfolio project to demonstrate production-grade Spring Boot: layered architecture,
JWT security with role-based access, non-trivial domain logic, RFC 7807 error handling, and a
test suite that runs against a real PostgreSQL instance.

> Status: work in progress. The domain is delivered module by module — see the checklist below.

## Build status

| Module | State |
|---|---|
| `identity` — accounts, JWT auth, refresh-token rotation, RBAC | done |
| `common` — RFC 7807 error handling, auditing, OpenAPI | done |
| `clients` — client profiles | scaffolded |
| `catalog` — service categories & offerings, ADMIN-managed | done |
| `resources` — bookable resources (rooms / stations / …) | done |
| `staff` — employees, weekly hours, time off, skill assignment | done |
| `scheduling` — appointments, availability, overlap protection | done |
| `pricing` — loyalty tiers, time-based rules, quote calculation | done |
| `billing` — invoices issued on completion, payments | done |

## Tech stack

- Java 21, Spring Boot 3.3
- Spring Web, Spring Data JPA, Spring Security (stateless JWT)
- PostgreSQL 16, Flyway migrations
- MapStruct, Lombok (used sparingly — never `@Data` on entities)
- JUnit 5, Mockito, AssertJ, Testcontainers
- Docker / Docker Compose
- springdoc-openapi (Swagger UI at `/swagger-ui.html`)

## Architecture

Package-by-feature; each feature keeps its own layers:

```
web  ->  service  ->  repository  ->  domain
 |          |
 DTOs    commands / results (records)
```

- Controllers are thin: validation, mapping, delegation.
- Business rules and transaction boundaries live in services.
- Domain entities carry behaviour (state transitions, invariants), not just fields.
- Cross-cutting concerns (error handling, auditing, security) sit in `common` and `identity`.

### Error handling

Every error response is an [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) `application/problem+json`
document produced by a single `@RestControllerAdvice`. Domain exceptions extend `ApplicationException`
and carry their own HTTP status and problem type; validation failures are returned with a structured
`errors` array.

### Authentication

- `POST /api/v1/auth/register` creates a `CLIENT` account and profile.
- `POST /api/v1/auth/login` returns a short-lived access token (JWT, HS256) and an opaque refresh token.
- Refresh tokens are stored **hashed**, single-use, and rotated on every `POST /api/v1/auth/refresh`.
  Re-using an already-rotated token revokes the whole token family (reuse-detection).
- `POST /api/v1/auth/logout` revokes the presented refresh token.

### Pricing

Each booking is priced through an `AppointmentPricer`:

```
net       = Σ(service base price × quantity)   (snapshot on the appointment items)
surcharge = net × Σ(matching PricingRule surcharge %)   e.g. +15% on weekends
subtotal  = net + surcharge
discount% = loyalty tier % + rule discounts, capped at servicedesk.pricing.max-total-discount-percentage
total     = subtotal − subtotal × discount%
```

The loyalty tier is resolved from the client's completed-appointment count and 12-month spend
(`LoyaltyTierResolver`); the tier name is snapshotted onto the appointment. Tiers and rules are
seeded (`V7`) and managed under `/api/v1/admin/loyalty-tiers` and `/api/v1/admin/pricing-rules`.

### Billing

Moving an appointment to `COMPLETED` publishes an `AppointmentCompleted` event; the `billing`
module listens for it and issues an invoice (`net` = appointment total, plus
`servicedesk.billing.invoice-tax-rate` VAT) in the same transaction — `scheduling` has no
compile-time dependency on `billing`. Invoice numbers come from a database sequence
(`INV-<year>-<seq>`). An invoice flips to `PAID` once recorded payments cover the gross amount
(`POST /api/v1/invoices/{id}/payments`, admin only).

## Data model

```mermaid
erDiagram
    users ||--o{ user_roles : has
    roles ||--o{ user_roles : grants
    users ||--o| clients : "profile"
    users ||--o| employees : "profile"
    users ||--o{ refresh_tokens : owns

    clients   ||--o{ appointments : books
    employees ||--o{ appointments : "performs"
    resources ||--o{ appointments : "reserved for"

    employees ||--o{ employee_skills : "has"
    services  ||--o{ employee_skills : "required for"
    employees ||--o{ employee_working_hours : defines
    employees ||--o{ employee_time_off : requests

    service_categories ||--o{ services : contains
    services      ||--o{ appointment_items : "referenced by"
    appointments  ||--o{ appointment_items : includes

    appointments ||--o| invoices : "billed as"
    invoices     ||--o{ payments : "settled by"
```

Concurrency strategy for bookings (`scheduling` module):

1. Pessimistic lock (`SELECT ... FOR UPDATE`) on the employee and resource rows during the
   booking critical section, so concurrent bookings for the same provider serialize and the
   loser gets a friendly `409`.
2. PostgreSQL `EXCLUDE USING gist` constraint on `appointments` (needs the `btree_gist`
   extension) as the backstop — overlapping `tstzrange` values for the same employee or
   resource are rejected at the database level and translated to `409 slot-unavailable`.
3. Optimistic locking (`@Version`) for reschedule and status-change flows.

`ConcurrentBookingIntegrationTest` starts two threads on the same slot and asserts exactly one
booking is created.

## Running the app

### With Docker Compose

```bash
cp .env.example .env          # adjust the JWT secret
docker compose up --build
```

API on `http://localhost:8080`, Swagger UI on `http://localhost:8080/swagger-ui.html`.

### Locally against a database

```bash
docker compose up -d db
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Build and test

```bash
./mvnw verify
```

Unit tests (service layer, Mockito + AssertJ) run everywhere. Integration tests
(`@SpringBootTest` + MockMvc + Testcontainers PostgreSQL) run only when a Docker daemon is
available and are skipped otherwise.

## Example flow

```bash
# register
curl -s localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' -d '{
  "email": "ada@example.com", "password": "Sup3rSecret",
  "firstName": "Ada", "lastName": "Kowalska", "marketingConsent": true
}'

# login
TOKENS=$(curl -s localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{
  "email": "ada@example.com", "password": "Sup3rSecret"
}')
ACCESS=$(echo "$TOKENS" | jq -r .accessToken)

# call a protected endpoint
curl -s localhost:8080/api/v1/me -H "Authorization: Bearer $ACCESS"
```
