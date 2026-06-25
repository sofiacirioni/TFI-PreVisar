-- ============================================================
-- V004: Profesional
-- ============================================================
-- Datos personales y profesionales del matriculado del CIEC.
-- Cada Profesional pertenece a un Usuario (composición, no
-- herencia). El Usuario maneja autenticación, el Profesional
-- maneja los datos del negocio.
--
-- Relaciones:
--   usuario_id      → usuario      (1:1, único)
--   regional_id     → regional     (N:1, dónde está matriculado)
--   condicion_iva_id → condicion_iva (N:1, categoría AFIP)
-- ============================================================

CREATE TABLE profesional (
                             id                   BIGSERIAL    PRIMARY KEY,
                             usuario_id           BIGINT       NOT NULL UNIQUE REFERENCES usuario(id),
                             nombre               VARCHAR(100) NOT NULL,
                             apellido             VARCHAR(100) NOT NULL,
                             dni                  VARCHAR(20)  NOT NULL,
                             cuit                 VARCHAR(13)  NOT NULL UNIQUE,
                             matricula            VARCHAR(20)  NOT NULL UNIQUE,
                             titulo               VARCHAR(150) NOT NULL,
                             domicilio            VARCHAR(255) NOT NULL,
                             telefono             VARCHAR(30),
                             regional_id          BIGINT       NOT NULL REFERENCES regional(id),
                             condicion_iva_id     BIGINT       NOT NULL REFERENCES condicion_iva(id),
                             afiliado_caja_8470   BOOLEAN      NOT NULL DEFAULT FALSE,
                             created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profesional_cuit      ON profesional(cuit);
CREATE INDEX idx_profesional_matricula ON profesional(matricula);
CREATE INDEX idx_profesional_regional  ON profesional(regional_id);

COMMENT ON TABLE  profesional                    IS 'Datos del matriculado CIEC. Cada uno tiene un Usuario asociado (1:1).';
COMMENT ON COLUMN profesional.usuario_id         IS 'FK única a usuario. UNIQUE asegura 1:1 (un Usuario = un Profesional).';
COMMENT ON COLUMN profesional.cuit               IS 'CUIT con formato XX-XXXXXXXX-X. Único globalmente.';
COMMENT ON COLUMN profesional.matricula          IS 'Número de matrícula del CIEC. En el expediente de referencia: 17372068.';
COMMENT ON COLUMN profesional.titulo             IS 'Título profesional. Ej: "Ing. Electricista Electrónico".';
COMMENT ON COLUMN profesional.afiliado_caja_8470 IS 'Si está afiliado a Caja Ley 8470. Obligatorio salvo mayores de 60 o relación de dependencia.';