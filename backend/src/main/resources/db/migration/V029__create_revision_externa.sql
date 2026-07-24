-- ============================================================
-- V029: RevisionExterna
-- ============================================================
-- Expediente técnico completo (PDF único) que un revisor sube
-- para obtener un resumen general generado por IA.
--
-- El estado vive acá (no en un tracker en memoria) para que el
-- historial sobreviva un reinicio: el front consulta el detalle
-- hasta que deje de estar EN_PROGRESO.
-- ============================================================

CREATE TABLE revision_externa (
    id             BIGSERIAL    PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL REFERENCES usuario (id),
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_relativa  VARCHAR(500) NOT NULL,
    estado         VARCHAR(30)  NOT NULL,   -- EN_PROGRESO / COMPLETADO / ERROR
    resultado      JSONB,                   -- resumen crudo de Gemini; NULL mientras corre o si falla
    detalle        TEXT,                    -- motivo del ERROR, legible para el revisor
    created_at     TIMESTAMP    NOT NULL DEFAULT now()
);

-- El historial se lista por usuario, más nuevo primero.
CREATE INDEX idx_revision_externa_usuario ON revision_externa (usuario_id, created_at DESC);

COMMENT ON TABLE  revision_externa            IS 'Expediente completo (PDF) que un revisor analiza con IA para obtener un resumen general.';
COMMENT ON COLUMN revision_externa.usuario_id IS 'Dueño de la revisión (revisor que subió el PDF). El historial es por usuario.';
COMMENT ON COLUMN revision_externa.estado     IS 'EN_PROGRESO mientras la IA analiza; COMPLETADO con resumen; ERROR con motivo en detalle.';
