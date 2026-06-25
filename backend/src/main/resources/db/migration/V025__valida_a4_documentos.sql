-- V025 — Slots que se esperan en A4
ALTER TABLE documento_requerido
    ADD COLUMN valida_a4 BOOLEAN NOT NULL DEFAULT TRUE;

-- Los planos son de gran formato (A1/A3): no se valida A4.
UPDATE documento_requerido SET valida_a4 = FALSE WHERE codigo = 'PLANIMETRIA';