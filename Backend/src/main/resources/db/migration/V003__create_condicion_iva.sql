-- ============================================================
-- V003: Catálogo de Condición frente al IVA (AFIP)
-- ============================================================
-- Categorías AFIP de un contribuyente. Cada Profesional declara
-- una condición. Se modela como tabla (no enum Java) porque las
-- categorías AFIP pueden cambiar (ej: 2023 se eliminó "Responsable
-- Monotributo con IVA"). Tener tabla evita recompilar la app.
-- ============================================================

CREATE TABLE condicion_iva (
                               id          BIGSERIAL    PRIMARY KEY,
                               codigo      VARCHAR(30)  NOT NULL UNIQUE,
                               descripcion VARCHAR(100) NOT NULL,
                               activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE  condicion_iva             IS 'Catálogo de categorías AFIP. Las categorías que dejan de existir se desactivan, no se borran.';
COMMENT ON COLUMN condicion_iva.codigo      IS 'Código corto, usado en código Java y referencias (ej: RESPONSABLE_INSCRIPTO).';
COMMENT ON COLUMN condicion_iva.descripcion IS 'Descripción humana para mostrar en UI.';
COMMENT ON COLUMN condicion_iva.activo      IS 'Si es false, NO se muestra en dropdowns. Mantiene integridad referencial.';

-- ------------------------------------------------------------
-- Datos semilla: categorías AFIP vigentes (2026)
-- ------------------------------------------------------------
INSERT INTO condicion_iva (codigo, descripcion) VALUES
                                                    ('RESPONSABLE_INSCRIPTO', 'Responsable Inscripto'),
                                                    ('MONOTRIBUTO',           'Monotributo'),
                                                    ('EXENTO',                'Exento'),
                                                    ('CONSUMIDOR_FINAL',      'Consumidor Final'),
                                                    ('NO_RESPONSABLE',        'No Responsable');