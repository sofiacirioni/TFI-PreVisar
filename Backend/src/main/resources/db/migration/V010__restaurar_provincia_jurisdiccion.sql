-- ============================================================
-- V010: Restaurar jurisdicción provincial
-- ============================================================
-- Tras validación con el cliente real, se confirma:
--
-- 1. La jurisdicción del revisor SÍ es provincial. Un revisor solo
--    puede aprobar expedientes de obras ubicadas en su misma provincia.
--    Lo que NO es estructural es la asignación a regional específica
--    dentro de la provincia (eso es decisión interna del CIEC y volátil).
--
-- 2. Las reglas/normativas de presentación son provinciales. Cada
--    provincia puede exigir documentos y validaciones distintas. Esto
--    YA está reflejado en el modelo del catálogo (TipoTarea está
--    asociado a Provincia desde el diseño original).
--
-- Esta migración corrige el alcance de V008, que había eliminado
-- provincia_id de rol_revisor erróneamente:
--   - Restaura provincia_id en rol_revisor.
--   - Agrega provincia_id en obra (para validar autorización del revisor).
--
-- Lo que NO se restaura: regional_id en obra. La regional donde se
-- presenta el expediente sigue siendo decisión interna del CIEC.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Restaurar provincia_id en rol_revisor
-- ------------------------------------------------------------
-- Si ya hay registros en rol_revisor sin provincia, hay que decidir
-- qué valor poner. En este punto del desarrollo no hay datos productivos,
-- así que la columna se agrega NOT NULL sin default y, si hubiera filas
-- existentes, fallaría la migración (lo cual sería un alerta correcto).
--
-- Para entornos con datos: se podría agregar primero como nullable,
-- popular los valores, y después hacer NOT NULL. Acá no hace falta.
-- ------------------------------------------------------------
ALTER TABLE rol_revisor
    ADD COLUMN provincia_id BIGINT NOT NULL REFERENCES provincia(id);

CREATE INDEX idx_rol_revisor_provincia ON rol_revisor(provincia_id);

COMMENT ON COLUMN rol_revisor.provincia_id IS
    'Provincia donde el profesional puede revisar expedientes. La asignación a regional específica dentro de la provincia es decisión interna del CIEC y NO se modela acá.';

-- ------------------------------------------------------------
-- 2. Agregar provincia_id en obra
-- ------------------------------------------------------------
-- La obra debe declarar en qué provincia está ubicada para que la
-- validación de autorización del revisor funcione:
--   revisor.provincia == obra.provincia → puede revisar.
--
-- Se agrega NOT NULL porque toda obra tiene una ubicación. Si hubiera
-- obras pre-existentes, asignar la provincia correspondiente antes
-- de hacer NOT NULL. En este entorno no hay datos productivos.
-- ------------------------------------------------------------
ALTER TABLE obra
    ADD COLUMN provincia_id BIGINT NOT NULL REFERENCES provincia(id);

CREATE INDEX idx_obra_provincia ON obra(provincia_id);

COMMENT ON COLUMN obra.provincia_id IS
    'Provincia donde se ubica físicamente la obra. Determina qué revisor (por jurisdicción provincial) puede validar el expediente asociado.';