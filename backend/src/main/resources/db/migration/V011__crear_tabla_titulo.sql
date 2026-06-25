-- ============================================
-- V011: Tabla titulo (catálogo de títulos de ingeniería)
-- ============================================

CREATE TABLE titulo (
                        id BIGSERIAL PRIMARY KEY,
                        nombre VARCHAR(150) NOT NULL UNIQUE,
                        permite_texto_libre BOOLEAN NOT NULL DEFAULT false,
                        activo BOOLEAN NOT NULL DEFAULT true
);

COMMENT ON TABLE titulo IS
    'Catálogo de títulos universitarios de ingeniería habilitados.';
COMMENT ON COLUMN titulo.permite_texto_libre IS
    'Si true, cuando un profesional selecciona este título debe completar titulo_otro_descripcion (caso Otro).';

-- ============================================
-- Seeds: títulos según Res. Ministerial 1232/2001 + especialidades CIEC
-- ============================================

INSERT INTO titulo (nombre, permite_texto_libre, activo) VALUES
                                                                    ('Ingeniero Aeronáutico',             false, true),
                                                                    ('Ingeniero Agrónomo',                false, true),
                                                                    ('Ingeniero Ambiental',               false, true),
                                                                    ('Ingeniero Biomédico',               false, true),
                                                                    ('Bioingeniero',                      false, true),
                                                                    ('Ingeniero Civil',                   false, true),
                                                                    ('Ingeniero Electricista',            false, true),
                                                                    ('Ingeniero Electricista Electrónico',false, true),
                                                                    ('Ingeniero Electromecánico',         false, true),
                                                                    ('Ingeniero Electrónico',             false, true),
                                                                    ('Ingeniero en Alimentos',            false, true),
                                                                    ('Ingeniero en Computación',          false, true),
                                                                    ('Ingeniero en Materiales',           false, true),
                                                                    ('Ingeniero en Minas',                false, true),
                                                                    ('Ingeniero en Petróleo',             false, true),
                                                                    ('Ingeniero en Sistemas',             false, true),
                                                                    ('Ingeniero en Telecomunicaciones',   false, true),
                                                                    ('Ingeniero Industrial',              false, true),
                                                                    ('Ingeniero Informático',             false, true),
                                                                    ('Ingeniero Laboral',                 false, true),
                                                                    ('Ingeniero Mecánico',                false, true),
                                                                    ('Ingeniero Mecánico Electricista',   false, true),
                                                                    ('Ingeniero Nuclear',                 false, true),
                                                                    ('Ingeniero Químico',                 false, true),
                                                                    ('Especialista en Higiene y Seguridad en el Trabajo',                 false, true),
                                                                    ('Otro',                              true,  true);