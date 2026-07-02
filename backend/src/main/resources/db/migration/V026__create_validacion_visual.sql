-- V026 — Resultado de la validación visual (nivel 3). Se persiste por costo (ADR-022).
CREATE TABLE validacion_visual (
                                   id                   BIGSERIAL   PRIMARY KEY,
                                   documento_cargado_id BIGINT      NOT NULL REFERENCES documento_cargado (id),
                                   hash_documento       VARCHAR(64) NOT NULL,
                                   resultado            JSONB       NOT NULL,
                                   created_at           TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX idx_validacion_visual_doc ON validacion_visual (documento_cargado_id);
-- Un resultado por documento+contenido: si el archivo cambia (nuevo hash), se recalcula.
CREATE UNIQUE INDEX uq_validacion_visual_doc_hash
    ON validacion_visual (documento_cargado_id, hash_documento);