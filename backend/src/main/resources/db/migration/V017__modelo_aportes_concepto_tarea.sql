-- =============================================================================
-- V017: Modelo de aportes por concepto y tarea (reemplaza los flags de tipo_tarea)
-- =============================================================================

-- 1. Catalogo de conceptos
CREATE TABLE concepto_aporte (
                                 id      BIGSERIAL    PRIMARY KEY,
                                 codigo  VARCHAR(30)  NOT NULL UNIQUE,
                                 nombre  VARCHAR(100) NOT NULL,
                                 grupo   VARCHAR(10)  NOT NULL,
                                 activo  BOOLEAN      NOT NULL DEFAULT TRUE,
                                 CONSTRAINT chk_concepto_grupo CHECK (grupo IN ('CIEC', 'CAJA'))
);

-- 2. Seeds de conceptos
INSERT INTO concepto_aporte (codigo, nombre, grupo) VALUES
                                                        ('REGISTRO_OBRA',          'Registro de Obra',                 'CIEC'),
                                                        ('REGISTRO_OBRA_DIFERIDO', 'Registro de Obra Diferido',        'CIEC'),
                                                        ('ARANCEL_ADMIN',          'Arancel administrativo',           'CIEC'),
                                                        ('CAJA_PROFESIONAL',       'Aporte a la Caja (profesional)',   'CAJA'),
                                                        ('CAJA_COMITENTE',         'Aporte a la Caja (comitente)',     'CAJA');

-- 3. parametro_aporte: agregar concepto_id y migrar los datos existentes
ALTER TABLE parametro_aporte ADD COLUMN concepto_id BIGINT;

UPDATE parametro_aporte p SET concepto_id = c.id
    FROM concepto_aporte c
WHERE c.codigo = CASE p.concepto
    WHEN 'ROD'              THEN 'REGISTRO_OBRA'
    WHEN 'ARANCEL_ADMIN'    THEN 'ARANCEL_ADMIN'
    WHEN 'CAJA_PROFESIONAL' THEN 'CAJA_PROFESIONAL'
    WHEN 'CAJA_COMITENTE'   THEN 'CAJA_COMITENTE'
END;

-- 4. Cerrar la migracion de la columna: NOT NULL, FK, y recien ahi dropear la vieja
ALTER TABLE parametro_aporte ALTER COLUMN concepto_id SET NOT NULL;
ALTER TABLE parametro_aporte ADD CONSTRAINT fk_parametro_concepto
    FOREIGN KEY (concepto_id) REFERENCES concepto_aporte (id);
ALTER TABLE parametro_aporte DROP COLUMN concepto;

-- 5. AHORA si: nuevo parametro para el diferido (2%), ya sin la columna vieja
INSERT INTO parametro_aporte (concepto_id, tipo_valor, valor, base_calculo, vigencia_desde)
SELECT c.id, 'PORCENTAJE', 2.0000, 'HONORARIOS', DATE '2026-01-01'
FROM concepto_aporte c WHERE c.codigo = 'REGISTRO_OBRA_DIFERIDO';

-- 6. tipo_tarea_aporte: que conceptos aplican a cada tarea
CREATE TABLE tipo_tarea_aporte (
                                   id            BIGSERIAL   PRIMARY KEY,
                                   tipo_tarea_id BIGINT      NOT NULL REFERENCES tipo_tarea (id),
                                   concepto_id   BIGINT      NOT NULL REFERENCES concepto_aporte (id),
                                   base_calculo  VARCHAR(15) NOT NULL,
                                   activo        BOOLEAN     NOT NULL DEFAULT TRUE,
                                   CONSTRAINT chk_tta_base CHECK (base_calculo IN ('HONORARIOS', 'MONTO_OBRA')),
                                   CONSTRAINT uq_tta_tarea_concepto UNIQUE (tipo_tarea_id, concepto_id)
);

-- 7. Filas de PR-DT-RT
INSERT INTO tipo_tarea_aporte (tipo_tarea_id, concepto_id, base_calculo)
SELECT tt.id, c.id, 'HONORARIOS'
FROM tipo_tarea tt
         JOIN concepto_aporte c ON c.codigo IN
                                   ('REGISTRO_OBRA', 'ARANCEL_ADMIN', 'CAJA_PROFESIONAL', 'CAJA_COMITENTE')
WHERE tt.codigo = 'PR-DT-RT';

-- 8. Eliminar los flags de tipo_tarea
ALTER TABLE tipo_tarea
DROP COLUMN aplica_rod,
    DROP COLUMN aplica_arancel_admin,
    DROP COLUMN aplica_caja;