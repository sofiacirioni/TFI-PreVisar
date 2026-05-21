-- ============================================================
-- V007: Obra
-- ============================================================
-- Lugar físico donde se ejecuta la tarea profesional. Asociada a:
--   - un Comitente (el cliente)
--   - una Regional del CIEC (donde se presenta el expediente)
--
-- La regional de la obra es la que determina qué revisor puede
-- verla: revisor.provincia == obra.regional.provincia.
--
-- Datos catastrales: circunscripción, sección, manzana, parcela.
-- En el expediente de Zarranz aparecen como "Des. Cat: 01-01-072-019".
-- Los modelamos como columnas separadas para poder validar formato
-- y armar el string consolidado al generar el PDF.
--
-- Soft delete por la misma razón que Comitente: preservar
-- expedientes históricos.
-- ============================================================

CREATE TABLE obra (
                      id                  BIGSERIAL    PRIMARY KEY,
                      comitente_id        BIGINT       NOT NULL REFERENCES comitente(id),
                      designacion         VARCHAR(255) NOT NULL,
                      calle               VARCHAR(150) NOT NULL,
                      numero              VARCHAR(20)  NOT NULL,
                      barrio              VARCHAR(100),
                      localidad           VARCHAR(100) NOT NULL,
                      codigo_postal       VARCHAR(10)  NOT NULL,
                      circunscripcion     VARCHAR(10),
                      seccion             VARCHAR(10),
                      manzana             VARCHAR(10),
                      parcela             VARCHAR(10),
                      created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      deleted_at          TIMESTAMP
);

CREATE INDEX idx_obra_comitente ON obra(comitente_id);

COMMENT ON TABLE  obra                 IS 'Ubicación física de la tarea profesional. Asociada a un comitente y una regional CIEC.';
COMMENT ON COLUMN obra.designacion     IS 'Descripción de la obra. Ej: "Proyecto Inst. Eléctrica BT edificio MYKONOS".';
COMMENT ON COLUMN obra.circunscripcion IS 'Datos catastrales. Parte 1 de "Des. Cat: 01-01-072-019".';
COMMENT ON COLUMN obra.seccion         IS 'Datos catastrales. Parte 2 de la nomenclatura.';
COMMENT ON COLUMN obra.manzana         IS 'Datos catastrales. Parte 3 de la nomenclatura.';
COMMENT ON COLUMN obra.parcela         IS 'Datos catastrales. Parte 4 de la nomenclatura.';
COMMENT ON COLUMN obra.deleted_at      IS 'Soft delete. Preserva referencias desde expedientes históricos.';