-- =============================================================================
-- V018: Quitar columnas de aporte persistidas del expediente
-- -----------------------------------------------------------------------------
-- Los aportes son un calculo determinístico (honorarios + parametros vigentes),
-- no un dato del expediente. Se calculan bajo demanda via endpoint de consulta,
-- no se persisten. El expediente conserva solo honorarios_referenciales (input
-- del profesional).
-- =============================================================================

ALTER TABLE expediente
DROP COLUMN aporte_rod,
    DROP COLUMN aporte_arancel_admin,
    DROP COLUMN aporte_caja_profesional,
    DROP COLUMN aporte_caja_comitente;