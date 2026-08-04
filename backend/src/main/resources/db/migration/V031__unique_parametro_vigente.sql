-- =============================================================================
-- V031 — Restaura la unicidad "un solo valor vigente por concepto"
-- =============================================================================
-- La V014 creó este índice sobre parametro_aporte.concepto (VARCHAR). La V017
-- reemplazó esa columna por concepto_id (FK) y, al hacer DROP COLUMN, Postgres
-- se llevó el índice con ella. Nunca se recreó, así que desde entonces nada
-- impide que convivan dos filas vigentes del mismo concepto.
--
-- Por qué importa: AporteCalculatorServiceImpl y ParametroAporteServiceImpl leen
-- el valor con ParametroAporteRepository.findVigente(), que devuelve Optional.
-- Con dos filas vigentes Spring Data lanza IncorrectResultSizeDataAccessException
-- y se caen tanto el cálculo de aportes como el cobro del arancel. Es un
-- invariante que tiene que sostener la base, no solo el orden de las operaciones
-- del service.
--
-- Parcial (WHERE): la restricción aplica solo a la fila vigente. El histórico
-- —las filas ya cerradas con vigencia_hasta— puede tener tantas por concepto
-- como haga falta.
-- =============================================================================

CREATE UNIQUE INDEX uq_parametro_vigente
    ON parametro_aporte (concepto_id)
    WHERE vigencia_hasta IS NULL AND activo;

COMMENT ON INDEX uq_parametro_vigente IS
    'Un único parámetro vigente por concepto. El histórico cerrado no se restringe.';
