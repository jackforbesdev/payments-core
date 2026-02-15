
Payments Core (v0)

Fields:

- id (uuid)
- amount (int - smallest unit of currency)
- currency (String - ISO 4217)
- state (AUTHORISED/PARTIALLY_CAPTURED/CAPTURED/PARTIALLY_REFUNDED/REFUNDED/VOIDED)
- capturedAmount
- refundedAmount
- timestamps


Endpoints:

1. POST /payments/authorise

- request: amount, currency
- response: payment, representation
- rules: amount > 0

2. POST /payments/{id}/capture

- request: amount
- headers: Idempotency-Key: <string>
- rules:
	- only if state is AUTHORISED or PARTIALLY_CAPTURED
	- capturedAmount must not exceed amount

3. POST /payments/{id}/refund

- request: amount
- headers: Idempotency-key: <string>
- rules:
	- only if capturedAmount > refundedAmount
	- refundedAmount must not exceed capturedAmount

4. GET /payments/{id}

Invariants:
- 0 <= refundedAmount <= capturedAmount <= amount

Idempotency:
- capture and refund require Idempotency-Key
- same key + same request -> replay response
- same key + different request -> 409


Errors:

- invalid transition -> 409
- idempotency mistmatch -> 409
- not found -> 404
- invalid input -> 400
