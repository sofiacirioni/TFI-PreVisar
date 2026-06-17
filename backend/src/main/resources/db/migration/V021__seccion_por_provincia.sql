-- =============================================================================
-- V021 — La estructura documental pasa a ser propia de cada provincia
-- =============================================================================
-- SCRUM-12x: el revisor puede crear/modificar/eliminar secciones y documentos
-- requeridos para un (provincia, tipo_tarea). Cada provincia es dueña de su
-- estructura; la competencia del revisor es provincial (ver RolRevisor).
--
-- Decisiones de modelado:
--   - provincia_id va SOLO en `seccion`. `documento_requerido` hereda el scope
--     a través de su sección, así que no necesita su propia columna.
--   - La estructura ya sembrada en V020 (sin provincia) se asigna a Córdoba.
--   - Unicidad por código pasa a ser PARCIAL (WHERE activo): así, al dar de baja
--     lógica una sección/documento, su código queda libre para recrearse.
-- =============================================================================

-- ----------------------------------------------------------------------------
-- seccion: agregar provincia_id
-- ----------------------------------------------------------------------------
ALTER TABLE seccion ADD COLUMN provincia_id BIGINT REFERENCES provincia (id);

-- Backfill: la estructura existente queda como la de Córdoba (único MVP).
UPDATE seccion
SET provincia_id = (SELECT id FROM provincia WHERE codigo = 'CBA')
WHERE provincia_id IS NULL;

ALTER TABLE seccion ALTER COLUMN provincia_id SET NOT NULL;

CREATE INDEX idx_seccion_provincia ON seccion (provincia_id);

-- Reemplazar la unicidad global por una por provincia, y parcial (solo activas).
ALTER TABLE seccion DROP CONSTRAINT uq_seccion_tipo_tarea_codigo;

CREATE UNIQUE INDEX uq_seccion_provincia_tipo_tarea_codigo
    ON seccion (provincia_id, tipo_tarea_id, codigo)
    WHERE activo;

-- ----------------------------------------------------------------------------
-- documento_requerido: unicidad parcial (permite recrear un código dado de baja)
-- ----------------------------------------------------------------------------
ALTER TABLE documento_requerido DROP CONSTRAINT uq_documento_seccion_codigo;

CREATE UNIQUE INDEX uq_documento_seccion_codigo
    ON documento_requerido (seccion_id, codigo)
    WHERE activo;
