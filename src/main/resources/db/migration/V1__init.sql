
CREATE TYPE payment_state AS ENUM (

    'AUTHORISED',
    'PARTIALLY_CAPTURED',
    'CAPTURED',
    'PARTIALLY_REFUNDED',
    'REFUNDED',
    'VOIDED'
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    amount BIGINT NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    state payment_state NOT NULL,

    captured_amount BIGINT NOT NULL DEFAULT 0 CHECK (captured_amount >= 0),
    refunded_amount BIGINT NOT NULL DEFAULT 0 CHECK (refunded_amount >= 0),

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    version INT NOT NULL,

    CHECK (refunded_amount <= captured_amount),
    CHECK (captured_amount <= amount)
);

CREATE TABLE idempotency_keys (
    client_id TEXT NOT NULL,
    idem_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,

    response_status INT NOT NULL CHECK (response_status BETWEEN 100 AND 599),
    response_body JSONB NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (client_id, idem_key)
);

CREATE INDEX idx_idempotency_created_at ON idempotency_keys (created_at);