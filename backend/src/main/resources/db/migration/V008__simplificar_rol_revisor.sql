-- V008__simplificar_rol_revisor.sql
ALTER TABLE rol_revisor
DROP CONSTRAINT rol_revisor_provincia_id_fkey;

DROP INDEX idx_rol_revisor_provincia;

ALTER TABLE rol_revisor
DROP COLUMN provincia_id;