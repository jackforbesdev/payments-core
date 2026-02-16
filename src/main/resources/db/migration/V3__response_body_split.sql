ALTER TABLE idempotency_keys
  ADD COLUMN payment_id UUID NOT NULL;

ALTER TABLE idempotency_keys
DROP COLUMN response_body;