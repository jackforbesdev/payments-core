# Payments Core API (v1)

A minimal payments API that models a real payment lifecycle:

- **Authorise** a payment
- **Get** a payment by ID
- **Capture** funds (partial/full)
- **Refund** captured funds (partial/full)
- **Idempotency** for write operations
- **Optimistic locking** to prevent lost updates
- **Flyway migrations**
- **Continuous Integration testing** using **Testcontainers & GitHub Actions**

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA (Hibernate)
- PostgreSQL
- Flyway
- Testcontainers
- JUnit 5 + MockMvc
- GitHub Actions

---

## Core Concepts

### Payment amounts

- `amount`: authorised amount
- `capturedAmount`: total captured so far
- `refundedAmount`: total refunded so far

### Idempotency

Write endpoints require:

- `X-Client-Id`
- `Idempotency-Key`

Behaviour:

- Same `(X-Client-Id, Idempotency-Key)` + same request -> **replay previous result**
- Same key reused with a different request -> **409 Conflict** with code `IDEMPOTENCY_KEY_REUSED`

### Optimistic locking

Payments use an optimistic version field (`@Version`), so concurrent updates can't overrwrite eachother.

---

## API

Base path: `/payments`

### Authorise

`POST /payments/authorise`

Request:

```json
{ "amount": 1234, "currency": "GBP" }
```

Response: `201 Created` (PaymentResponse)

```json
{
  "id": "uuid",
  "amount": 1234,
  "currency": "GBP",
  "state": "AUTHORISED",
  "capturedAmount": 0,
  "refundedAmount": 0,
  "createdAt": "2026-02-16T18:39:29.000Z",
  "updatedAt": "2026-02-16T18:39:29.000Z"
}
```

### Get payment

`GET /payments/{id}`

Response: `200 OK` (PaymentResponse)  
Missing payment: `404` with `application/problem+json`

### Capture

`POST /payments/{id}/capture`

Headers:

- `X-Client-Id: clientA`
- `Idempotency-Key: <uuid>`

Request:

```json
{ "amount": 100 }
```

Success: `200 OK` (PaymentResponse)

Errors:

- `400` `INVALID_INPUT` (e.g. amount <= 0)
- `409` `INVALID_TRANSITION` (e.g. exceeds authorised amount, wrong state)
- `409` `IDEMPOTENCY_KEY_REUSED` (same key, different request)

### Refund

`POST /payments/{id}/refund`

Headers:

- `X-Client-Id: clientA`
- `Idempotency-Key: <uuid>`

Request:

```json
{ "amount": 50 }
```

Success: `200 OK` (PaymentResponse)

Errors:

- `400` `INVALID_INPUT` (e.g. amount <= 0)
- `409` `INVALID_TRANSITION` (e.g. refund exceeds captured, wrong state)
- `409` `IDEMPOTENCY_KEY_REUSED` (same key, different request)

---

## Error Format

Errors are returned as `application/problem+json` using Spring’s `ProblemDetail`.

Fields:

- `title`
- `status`
- `detail`
- `path`
- `code`
- `errors`

Example:

```json
{
  "title": "Invalid state transition",
  "status": 409,
  "detail": "Capture would exceed authorised amount",
  "code": "INVALID_TRANSITION",
  "path": "/payments/<id>/capture"
}
```

---

## Running Locally

### Prerequisites

- Java 21
- Docker (required for tests via Testcontainers; optional for docker-compose DB)
- Maven wrapper included (`./mvnw`)

### Run tests

```bash
./mvnw test
```

### Run the application

```bash
./mvnw spring-boot:run
```

---

## Example Commands

Authorise:

```bash
curl -i -X POST http://localhost:8080/payments/authorise \
  -H "Content-Type: application/json" \
  -d '{"amount":1234,"currency":"GBP"}'
```

Capture:

```bash
curl -i -X POST http://localhost:8080/payments/<PAYMENT_ID>/capture \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: clientA" \
  -H "Idempotency-Key: '"$(uuidgen)"'" \
  -d '{"amount":100}'
```

Refund:

```bash
curl -i -X POST http://localhost:8080/payments/<PAYMENT_ID>/refund \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: clientA" \
  -H "Idempotency-Key: '"$(uuidgen)"'" \
  -d '{"amount":50}'
```

Get:

```bash
curl -i http://localhost:8080/payments/<PAYMENT_ID>
```

---

## Database Migrations

Flyway migrations are in:

```
src/main/resources/db/migration
```

They run automatically on app startup.

---

## GitHub Actions

GitHub Actions runs:

- `./mvnw test`
