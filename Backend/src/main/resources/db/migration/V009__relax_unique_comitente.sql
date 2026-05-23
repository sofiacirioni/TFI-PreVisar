-- ============================================================
-- V009: Relajar UNIQUE de comitente a índice parcial
-- ============================================================
-- El constraint UNIQUE original (profesional_id, dni_cuit) impide tener
-- un comitente soft-deleted y otro activo con el mismo CUIT dentro de
-- la misma cartera profesional.
--
-- Necesitamos permitir esa coexistencia: el histórico (soft-deleted)
-- preserva referencias de expedientes pasados, mientras que el activo
-- representa el cliente actual.
--
-- Solución: reemplazar el constraint por un índice único parcial que
-- solo aplica a los registros NO eliminados.
--
-- Postgres soporta índices parciales (cláusula WHERE en el índice).
-- ============================================================

-- Eliminar el constraint UNIQUE original
ALTER TABLE comitente
DROP CONSTRAINT uq_comitente_profesional_doc;

-- Crear un índice único parcial: solo cuenta como duplicado entre activos
CREATE UNIQUE INDEX uq_comitente_profesional_doc_activos
    ON comitente (profesional_id, dni_cuit)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX uq_comitente_profesional_doc_activos IS
    'Garantiza unicidad de DNI/CUIT por profesional, considerando solo comitentes activos (no soft-deleted).';