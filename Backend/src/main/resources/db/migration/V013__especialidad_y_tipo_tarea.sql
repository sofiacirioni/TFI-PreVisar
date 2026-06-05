-- =============================================================================
-- V013: Catalogo de especialidad y tipo de tarea
-- -----------------------------------------------------------------------------
-- Fundacion del esquema configurable (HU 0). La especialidad clasifica el
-- expediente (MVP: Electrica). El tipo de tarea es lo que efectivamente se
-- colegia y define, mediante flags, que aportes incurre -- en lugar de
-- hardcodear esa logica en el codigo. El subtipo del MVP es PR-DT-RT.
-- =============================================================================

CREATE TABLE especialidad (
                              id      BIGSERIAL    PRIMARY KEY,
                              codigo  VARCHAR(20)  NOT NULL UNIQUE,
                              nombre  VARCHAR(100) NOT NULL,
                              activo  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE tipo_tarea (
                            id                   BIGSERIAL    PRIMARY KEY,
                            especialidad_id      BIGINT       NOT NULL REFERENCES especialidad (id),
                            codigo               VARCHAR(20)  NOT NULL,
                            nombre               VARCHAR(150) NOT NULL,
                            aplica_rod           BOOLEAN      NOT NULL DEFAULT FALSE,  -- Aporte por Registro de Obra (5%)
                            aplica_arancel_admin BOOLEAN      NOT NULL DEFAULT FALSE,  -- Arancel administrativo (monto fijo)
                            aplica_caja          BOOLEAN      NOT NULL DEFAULT FALSE,  -- Caja Ley 8470 (9% profesional + 9% comitente)
                            orden                INTEGER      NOT NULL DEFAULT 0,
                            activo               BOOLEAN      NOT NULL DEFAULT TRUE,
                            CONSTRAINT uq_tipo_tarea_codigo UNIQUE (especialidad_id, codigo)
);

-- -----------------------------------------------------------------------------
-- Seeds
-- -----------------------------------------------------------------------------
INSERT INTO especialidad (codigo, nombre) VALUES ('ELEC', 'Electrica');

-- Se referencia la especialidad por codigo (no por id) para no asumir el valor
-- que genero la secuencia.
INSERT INTO tipo_tarea (especialidad_id, codigo, nombre,
                        aplica_rod, aplica_arancel_admin, aplica_caja, orden)
SELECT e.id, 'PR-DT-RT',
       'Proyecto, Direccion Tecnica y Representacion Tecnica',
       TRUE, TRUE, TRUE, 1
FROM especialidad e
WHERE e.codigo = 'ELEC';