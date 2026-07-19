-- V027 — Registro de pagos de Mercado Pago
CREATE TABLE pago (
    id                BIGSERIAL    PRIMARY KEY,
    expediente_id     BIGINT       NOT NULL REFERENCES expediente (id),
    mp_payment_id     VARCHAR(64)  NOT NULL,
    mp_preference_id  VARCHAR(128),
    estado            VARCHAR(30)  NOT NULL,          -- APROBADO / PENDIENTE / RECHAZADO
    monto             NUMERIC(15,2),
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_pago_mp_payment ON pago (mp_payment_id);   -- idempotencia
CREATE INDEX idx_pago_expediente ON pago (expediente_id);