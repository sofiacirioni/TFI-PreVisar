-- ============================================================
-- V005: RolRevisor
-- ============================================================
-- Asocia un Profesional con la Provincia donde puede revisar
-- expedientes. Si existe un registro en esta tabla para un
-- profesional, ESE profesional es revisor.
--
-- Decisiones:
--   - 1:1 con Profesional (profesional_id UNIQUE): un profesional
--     puede tener 0 o 1 RolRevisor. No queremos profesionales
--     que revisen en N provincias en el MVP.
--   - No se relaciona con Regional, se relaciona con Provincia.
--     Un revisor de la regional San Francisco puede revisar
--     expedientes de cualquier regional de Córdoba.
-- ============================================================

CREATE TABLE rol_revisor (
                             id              BIGSERIAL PRIMARY KEY,
                             profesional_id  BIGINT    NOT NULL UNIQUE REFERENCES profesional(id),
                             provincia_id    BIGINT    NOT NULL REFERENCES provincia(id),
                             created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rol_revisor_provincia ON rol_revisor(provincia_id);

COMMENT ON TABLE  rol_revisor                IS 'Rol secundario que habilita a un Profesional a revisar expedientes en una Provincia.';
COMMENT ON COLUMN rol_revisor.profesional_id IS 'FK única a profesional. Un profesional tiene 0 o 1 RolRevisor.';
COMMENT ON COLUMN rol_revisor.provincia_id   IS 'FK a provincia. El revisor solo puede aprobar expedientes de obras en esta provincia.';