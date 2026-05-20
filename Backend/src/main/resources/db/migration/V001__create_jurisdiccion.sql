-- ============================================================
-- V001: Catálogos de jurisdicción (Provincia y Regional)
-- ============================================================
-- Provincia: la jurisdicción donde operan los profesionales y
--   donde se encuentran las obras. Una provincia tiene N regionales.
--   Las reglas de visado son provinciales.
--
-- Regional: las sedes del CIEC dentro de una provincia. Es donde
--   un profesional está matriculado y donde se presenta cada obra.
--   En el MVP solo cargamos Córdoba y sus regionales.
--
-- Catálogo: estas tablas NO llevan created_at/updated_at porque
--   son datos estables administrados por el equipo, no por usuarios.
-- ============================================================

-- ------------------------------------------------------------
-- Tabla: provincia
-- ------------------------------------------------------------
CREATE TABLE provincia (
                           id      BIGSERIAL    PRIMARY KEY,
                           nombre  VARCHAR(100) NOT NULL UNIQUE,
                           codigo  VARCHAR(3)   NOT NULL UNIQUE
);

COMMENT ON TABLE  provincia        IS 'Catálogo de provincias argentinas. En el MVP solo se carga Córdoba.';
COMMENT ON COLUMN provincia.codigo IS 'Código ISO 3166-2:AR de 2-3 letras (ej: CBA, BA, SF).';

-- ------------------------------------------------------------
-- Tabla: regional
-- ------------------------------------------------------------
CREATE TABLE regional (
                          id            BIGSERIAL    PRIMARY KEY,
                          nombre        VARCHAR(100) NOT NULL,
                          provincia_id  BIGINT       NOT NULL REFERENCES provincia(id),
                          CONSTRAINT uq_regional_nombre_provincia UNIQUE (nombre, provincia_id)
);

CREATE INDEX idx_regional_provincia ON regional(provincia_id);

COMMENT ON TABLE  regional              IS 'Sedes del CIEC dentro de una provincia. Un profesional está matriculado en una.';
COMMENT ON COLUMN regional.provincia_id IS 'FK a provincia. Un nombre de regional puede repetirse entre provincias.';

-- ------------------------------------------------------------
-- Datos semilla: Provincia Córdoba
-- ------------------------------------------------------------
INSERT INTO provincia (nombre, codigo) VALUES
    ('Córdoba', 'CBA');

-- ------------------------------------------------------------
-- Datos semilla: Regionales del CIEC en Córdoba
-- ------------------------------------------------------------
-- Fuente: estructura institucional del CIEC. El expediente de
-- referencia (Zarranz) ingresa por la regional "San Francisco".
-- ------------------------------------------------------------
INSERT INTO regional (nombre, provincia_id) VALUES
                                                ('Córdoba Capital', (SELECT id FROM provincia WHERE codigo = 'CBA')),
                                                ('Río Cuarto',      (SELECT id FROM provincia WHERE codigo = 'CBA')),
                                                ('San Francisco',   (SELECT id FROM provincia WHERE codigo = 'CBA')),
                                                ('Villa María',     (SELECT id FROM provincia WHERE codigo = 'CBA')),
                                                ('Bell Ville',      (SELECT id FROM provincia WHERE codigo = 'CBA')),
                                                ('Marcos Juárez',   (SELECT id FROM provincia WHERE codigo = 'CBA')),
                                                ('Cruz del Eje',    (SELECT id FROM provincia WHERE codigo = 'CBA'));