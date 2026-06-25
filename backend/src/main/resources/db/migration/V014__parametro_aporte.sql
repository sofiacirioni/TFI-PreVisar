-- =============================================================================
-- V014: Parametros de aporte (valores configurables con vigencia)
-- -----------------------------------------------------------------------------
-- Guarda CUANTO vale cada concepto de aporte. El "que conceptos aplican" lo
-- definen los flags de tipo_tarea (V013); aca viven los importes/porcentajes.
-- Cada concepto puede tener historia: un valor con vigencia_hasta = NULL es el
-- vigente. Esto permite recalcular un expediente viejo con los valores que
-- regian en su momento, y actualizar montos (ej. el arancel) sin tocar codigo.
-- =============================================================================

CREATE TABLE parametro_aporte (
                                  id             BIGSERIAL     PRIMARY KEY,
                                  concepto       VARCHAR(30)   NOT NULL,
                                  tipo_valor     VARCHAR(15)   NOT NULL,             -- PORCENTAJE | FIJO
                                  valor          NUMERIC(15,4) NOT NULL,             -- porcentaje (5.0000 = 5%) o monto fijo en $
                                  base_calculo   VARCHAR(15),                        -- solo aplica si tipo_valor = PORCENTAJE
                                  vigencia_desde DATE          NOT NULL,
                                  vigencia_hasta DATE,                               -- NULL = vigente
                                  activo         BOOLEAN       NOT NULL DEFAULT TRUE,

                                  CONSTRAINT chk_parametro_concepto
                                      CHECK (concepto IN ('ROD', 'ARANCEL_ADMIN', 'CAJA_PROFESIONAL', 'CAJA_COMITENTE')),
                                  CONSTRAINT chk_parametro_tipo_valor
                                      CHECK (tipo_valor IN ('PORCENTAJE', 'FIJO')),
                                  CONSTRAINT chk_parametro_base
                                      CHECK (base_calculo IS NULL OR base_calculo IN ('HONORARIOS', 'MONTO_OBRA')),
                                  CONSTRAINT chk_parametro_base_requerida
                                      CHECK (tipo_valor <> 'PORCENTAJE' OR base_calculo IS NOT NULL),
                                  CONSTRAINT chk_parametro_valor_no_negativo
                                      CHECK (valor >= 0),
                                  CONSTRAINT chk_parametro_vigencia
                                      CHECK (vigencia_hasta IS NULL OR vigencia_hasta >= vigencia_desde)
);

-- A lo sumo un valor vigente (sin fecha de fin) y activo por concepto.
CREATE UNIQUE INDEX uq_parametro_vigente
    ON parametro_aporte (concepto)
    WHERE vigencia_hasta IS NULL AND activo;

-- -----------------------------------------------------------------------------
-- Seeds (valores segun instructivo CIEC / ejemplo Feb 2026)
-- NOTA: el arancel administrativo cambia seguido.
-- -----------------------------------------------------------------------------
INSERT INTO parametro_aporte (concepto, tipo_valor, valor, base_calculo, vigencia_desde) VALUES
                                                                                             ('ROD',              'PORCENTAJE',     5.0000, 'HONORARIOS', DATE '2026-01-01'),
                                                                                             ('CAJA_PROFESIONAL', 'PORCENTAJE',     9.0000, 'HONORARIOS', DATE '2026-01-01'),
                                                                                             ('CAJA_COMITENTE',   'PORCENTAJE',     9.0000, 'HONORARIOS', DATE '2026-01-01'),
                                                                                             ('ARANCEL_ADMIN',    'FIJO',       19000.0000, NULL,         DATE '2026-01-01');