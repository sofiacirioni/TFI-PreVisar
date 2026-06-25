-- =============================================================================
-- V020 — Estructura configurable de secciones y documentos del expediente
-- =============================================================================
-- SCRUM-123 / SCRUM-124 (tablas) + SCRUM-126 (seeds PR-DT-RT).
--
-- Modelo (ADR-006 "reglas como dato"): la estructura del armado documental se
-- declara en BD, no en código. La cadena es:
--   tipo_tarea -> seccion -> documento_requerido  (-> regla, en Sprint 12)
--
-- Convenciones (mismas que los catálogos especialidad / tipo_tarea):
--   - id BIGSERIAL, FKs BIGINT, activo BOOLEAN para baja lógica.
--   - codigo: clave estable legible (las validaciones/reglas referencian el
--     codigo, no el nombre, que puede cambiar de redacción).
-- =============================================================================

-- ----------------------------------------------------------------------------
-- Tabla: seccion  (agrupador de documentos dentro de un tipo de tarea)
-- ----------------------------------------------------------------------------
CREATE TABLE seccion (
                         id            BIGSERIAL    PRIMARY KEY,
                         tipo_tarea_id BIGINT       NOT NULL REFERENCES tipo_tarea (id),
                         codigo        VARCHAR(40)  NOT NULL,
                         nombre        VARCHAR(120) NOT NULL,
                         orden         INT          NOT NULL,
                         activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    -- el codigo de seccion es único dentro de cada tipo de tarea
                         CONSTRAINT uq_seccion_tipo_tarea_codigo UNIQUE (tipo_tarea_id, codigo)
);

CREATE INDEX idx_seccion_tipo_tarea ON seccion (tipo_tarea_id);

-- ----------------------------------------------------------------------------
-- Tabla: documento_requerido  (cada documento esperado dentro de una sección)
-- ----------------------------------------------------------------------------
CREATE TABLE documento_requerido (
                                     id          BIGSERIAL    PRIMARY KEY,
                                     seccion_id  BIGINT       NOT NULL REFERENCES seccion (id),
                                     codigo      VARCHAR(40)  NOT NULL,
                                     nombre      VARCHAR(160) NOT NULL,
                                     obligatorio BOOLEAN      NOT NULL DEFAULT TRUE,
                                     orden       INT          NOT NULL,
                                     activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    -- el codigo de documento es único dentro de cada sección
                                     CONSTRAINT uq_documento_seccion_codigo UNIQUE (seccion_id, codigo)
);

CREATE INDEX idx_documento_seccion ON documento_requerido (seccion_id);

-- =============================================================================
-- SEEDS — PR-DT-RT (Proyecto, Dirección Técnica y Representación Técnica)
-- =============================================================================
-- Validado contra el expediente real aprobado 104146 (Zarranz / Los Varta S.A.).
-- Se resuelve el tipo_tarea por codigo para no acoplar a un id concreto.
-- ⚠ Verificar que 'PR-DT-RT' coincida con el codigo sembrado en V013.

-- --- Secciones -------------------------------------------------------------
INSERT INTO seccion (tipo_tarea_id, codigo, nombre, orden)
SELECT tt.id, v.codigo, v.nombre, v.orden
FROM tipo_tarea tt
         CROSS JOIN (VALUES
                         ('ADMINISTRATIVO', 'Contenido administrativo', 1),
                         ('TECNICO',        'Contenido técnico',        2)
) AS v(codigo, nombre, orden)
WHERE tt.codigo = 'PR-DT-RT';

-- --- Documentos de la sección administrativa -------------------------------
INSERT INTO documento_requerido (seccion_id, codigo, nombre, obligatorio, orden)
SELECT s.id, v.codigo, v.nombre, v.obligatorio, v.orden
FROM seccion s
         JOIN tipo_tarea tt ON tt.id = s.tipo_tarea_id
         CROSS JOIN (VALUES
                         ('CARATULA',             'Carátula del expediente',                              TRUE,  1),
                         ('CONTRATO_LOCACION',    'Contrato de locación (firmado por ambas partes)',      TRUE,  2),
                         ('PLANILLA_HONORARIOS',  'Planilla oficial de honorarios y aportes',             TRUE,  3),
                         ('COMPUTO_MATERIALES',   'Cómputo y listado de materiales',                      TRUE,  4),
                         ('COMPROBANTE_CIEC',     'Comprobante de aporte al CIEC (registro de obra + arancel)', TRUE, 5),
                         ('BOLETA_CAJA',          'Boleta de aporte a Caja de Previsión (Ley 8470)',      TRUE,  6),
                         ('COMPROBANTE_DEPOSITO', 'Comprobante de depósito / pago',                       TRUE,  7),
                         ('BOLETA_ERSEP',         'Boleta de aporte a ERSEP',                             FALSE, 8),
                         ('COMPROBANTE_ERSEP',    'Comprobante de pago de ERSEP',                         FALSE, 9)
) AS v(codigo, nombre, obligatorio, orden)
WHERE tt.codigo = 'PR-DT-RT' AND s.codigo = 'ADMINISTRATIVO';

-- --- Documentos de la sección técnica --------------------------------------
INSERT INTO documento_requerido (seccion_id, codigo, nombre, obligatorio, orden)
SELECT s.id, v.codigo, v.nombre, v.obligatorio, v.orden
FROM seccion s
         JOIN tipo_tarea tt ON tt.id = s.tipo_tarea_id
         CROSS JOIN (VALUES
                         ('MEMORIA_TECNICA', 'Memoria técnica',      TRUE, 1),
                         ('PLANIMETRIA',     'Planimetría / planos', TRUE, 2)
) AS v(codigo, nombre, obligatorio, orden)
WHERE tt.codigo = 'PR-DT-RT' AND s.codigo = 'TECNICO';