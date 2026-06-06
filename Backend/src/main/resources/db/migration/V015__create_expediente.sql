-- =============================================================================
-- V015: Expediente (entidad principal)
-- -----------------------------------------------------------------------------
-- Un expediente = una presentacion = una tarea sobre una obra. La obra es el
-- ancla de larga vida; un profesional puede tener 1..N expedientes por obra.
--
-- IMPORTANTE: casi todos los campos de negocio son NULLABLE a proposito. El
-- wizard guarda progreso parcial (pasos no bloqueantes), asi que un BORRADOR
-- puede existir con la mayoria de los datos vacios. La completitud se valida
-- en backend recien al pasar a estado COMPLETO, no a nivel de columna.
-- =============================================================================

CREATE TABLE expediente (
                            id                       BIGSERIAL     PRIMARY KEY,
                            profesional_id           BIGINT        NOT NULL REFERENCES profesional (id),
                            obra_id                  BIGINT                 REFERENCES obra (id),
                            tipo_tarea_id            BIGINT                 REFERENCES tipo_tarea (id),
                            estado                   VARCHAR(15)   NOT NULL DEFAULT 'BORRADOR',

    -- Datos economicos
                            honorarios_referenciales NUMERIC(15,2),                    -- lo ingresa el profesional (el MVP no lo calcula)

    -- Aportes calculados (snapshot al momento del calculo)
                            aporte_rod               NUMERIC(15,2),
                            aporte_arancel_admin     NUMERIC(15,2),
                            aporte_caja_profesional  NUMERIC(15,2),
                            aporte_caja_comitente    NUMERIC(15,2),

                            activo                   BOOLEAN       NOT NULL DEFAULT TRUE,   -- soft delete (consistente con comitente)
                            created_at               TIMESTAMP     NOT NULL DEFAULT now(),
                            updated_at               TIMESTAMP     NOT NULL DEFAULT now(),

                            CONSTRAINT chk_expediente_estado
                                CHECK (estado IN ('BORRADOR', 'COMPLETO')),
                            CONSTRAINT chk_expediente_honorarios_no_negativo
                                CHECK (honorarios_referenciales IS NULL OR honorarios_referenciales >= 0)
);

-- La lista "Mis expedientes" filtra siempre por profesional y activo.
CREATE INDEX idx_expediente_profesional
    ON expediente (profesional_id)
    WHERE activo;