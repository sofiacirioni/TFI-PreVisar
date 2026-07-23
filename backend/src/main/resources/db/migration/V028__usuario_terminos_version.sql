-- V028: versión de Términos y Condiciones aceptada al registrarse.
-- Registra QUÉ versión de los T&C aceptó cada usuario (requisito de la política de
-- privacidad: dejar traza de la aceptación). El CUÁNDO ya lo da usuario.created_at,
-- que coincide con el momento del registro/aceptación.
-- Nullable: los usuarios previos a esta columna no tienen versión registrada.
ALTER TABLE usuario ADD COLUMN terminos_version VARCHAR(20);
