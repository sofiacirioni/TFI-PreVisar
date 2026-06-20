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

-- 1. Soltar el CHECK viejo (permitía BORRADOR/COMPLETO) para poder migrar los datos.
ALTER TABLE expediente DROP CONSTRAINT IF EXISTS chk_expediente_estado;

-- 2. Migrar los expedientes existentes: COMPLETO deja de existir como estado.
UPDATE expediente SET estado = 'EN_PROCESO' WHERE estado = 'COMPLETO';

-- 3. Re-crear el CHECK con el modelo binario.
ALTER TABLE expediente ADD CONSTRAINT chk_expediente_estado
    CHECK (estado IN ('BORRADOR', 'EN_PROCESO'));