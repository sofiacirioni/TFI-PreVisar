-- =============================================================================
-- V019: La base de calculo vive solo en parametro_aporte
-- -----------------------------------------------------------------------------
-- base_calculo estaba duplicada en tipo_tarea_aporte y parametro_aporte. La base
-- es una propiedad de COMO se aplica una tasa (solo importa para PORCENTAJE), no
-- de que tarea la usa, y no varia por tarea/especialidad. Su unico hogar correcto
-- es parametro_aporte (que ya conoce tipo_valor y tiene los CHECK asociados).
-- El calculador ahora lee parametro_aporte.base_calculo.
-- =============================================================================

ALTER TABLE tipo_tarea_aporte DROP CONSTRAINT IF EXISTS chk_tta_base;
ALTER TABLE tipo_tarea_aporte DROP COLUMN base_calculo;
