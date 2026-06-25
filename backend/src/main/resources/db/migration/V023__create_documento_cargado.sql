-- V023 — Soporte de subida de documentos

-- Cardinalidad por slot (ADR-006: es dato).
ALTER TABLE documento_requerido
    ADD COLUMN permite_multiples BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE documento_requerido SET permite_multiples = TRUE
WHERE codigo IN ('COMPROBANTE_CIEC','BOLETA_CAJA','COMPROBANTE_DEPOSITO',
                 'BOLETA_ERSEP','COMPROBANTE_ERSEP','PLANIMETRIA');

-- Documentos cargados (ADR-008: archivo en filesystem, path en BD).
CREATE TABLE documento_cargado (
                                   id                      BIGSERIAL    PRIMARY KEY,
                                   expediente_id           BIGINT       NOT NULL REFERENCES expediente (id),
                                   documento_requerido_id  BIGINT       NOT NULL REFERENCES documento_requerido (id),
                                   nombre_original         VARCHAR(255) NOT NULL,
                                   ruta_relativa           VARCHAR(512) NOT NULL,
                                   tipo_mime               VARCHAR(120) NOT NULL,
                                   tamano_bytes            BIGINT       NOT NULL,
                                   activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
                                   created_at              TIMESTAMP    NOT NULL DEFAULT now(),
                                   updated_at              TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_documento_cargado_expediente ON documento_cargado (expediente_id);
CREATE INDEX idx_documento_cargado_slot ON documento_cargado (documento_requerido_id);