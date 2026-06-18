-- =============================================================================
-- V022 — Estado EN_PROCESO reemplaza a COMPLETO en el ciclo del expediente
-- =============================================================================
-- El ciclo de vida queda en: BORRADOR -> EN_PROCESO (generar).
--   - BORRADOR:   en armado en el wizard (progreso parcial).
--   - EN_PROCESO: generado; el profesional está en el armado documental.
--
-- No hay estado terminal de "entregado/completo": la entrega y el visado formal
-- ocurren en otro sistema. Esta app es solo la capa de prevalidación y armado.
--
-- "Generar" (POST /expedientes/{id}/completar) deja el expediente en EN_PROCESO.
-- =============================================================================

-- Los COMPLETO existentes fueron generados con la semántica vieja (no hubo nunca
-- un cierre real), así que en el modelo nuevo son EN_PROCESO.
UPDATE expediente SET estado = 'EN_PROCESO' WHERE estado = 'COMPLETO';

-- Reemplazar el CHECK por el conjunto final de estados.
ALTER TABLE expediente DROP CONSTRAINT chk_expediente_estado;
ALTER TABLE expediente ADD CONSTRAINT chk_expediente_estado
    CHECK (estado IN ('BORRADOR', 'EN_PROCESO'));
