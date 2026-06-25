-- ============================================================
-- V006: Comitente
-- ============================================================
-- Cliente del profesional. Cada profesional tiene su propia
-- cartera privada de comitentes. Dos profesionales que atienden
-- al mismo CUIT/DNI tienen registros distintos en esta tabla.
--
-- La unicidad del DNI/CUIT es POR PROFESIONAL (constraint compuesto):
-- el mismo profesional no puede tener dos registros con el mismo
-- documento, pero el mismo CUIT puede aparecer en carteras de
-- profesionales distintos.
--
-- Soft delete (deleted_at): cuando un profesional "borra" un
-- comitente, no se elimina realmente. Esto preserva la integridad
-- referencial de expedientes históricos asociados a ese comitente.
-- ============================================================

CREATE TABLE comitente (
                           id                  BIGSERIAL    PRIMARY KEY,
                           profesional_id      BIGINT       NOT NULL REFERENCES profesional(id),
                           tipo_persona        VARCHAR(10)  NOT NULL,
                           nombre_razon_social VARCHAR(200) NOT NULL,
                           dni_cuit            VARCHAR(13)  NOT NULL,
                           domicilio           VARCHAR(255) NOT NULL,
                           email               VARCHAR(255),
                           telefono            VARCHAR(30),
                           created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           deleted_at          TIMESTAMP,

                           CONSTRAINT chk_comitente_tipo_persona  CHECK (tipo_persona IN ('FISICA', 'JURIDICA')),
                           CONSTRAINT uq_comitente_profesional_doc UNIQUE (profesional_id, dni_cuit)
);

CREATE INDEX idx_comitente_profesional ON comitente(profesional_id);
CREATE INDEX idx_comitente_dni_cuit    ON comitente(dni_cuit);

COMMENT ON TABLE  comitente                     IS 'Cartera privada de clientes de cada profesional. Soft delete con deleted_at.';
COMMENT ON COLUMN comitente.profesional_id      IS 'Dueño del registro. Aísla las carteras entre profesionales.';
COMMENT ON COLUMN comitente.tipo_persona        IS 'FISICA (DNI) o JURIDICA (CUIT). Gestionado como enum en Java.';
COMMENT ON COLUMN comitente.nombre_razon_social IS 'Nombre completo (persona física) o razón social (persona jurídica).';
COMMENT ON COLUMN comitente.dni_cuit            IS 'DNI si tipo_persona=FISICA, CUIT si JURIDICA. Único por profesional, no globalmente.';
COMMENT ON COLUMN comitente.deleted_at          IS 'Soft delete. Si está seteado, el comitente está "eliminado" pero conserva referencias históricas.';