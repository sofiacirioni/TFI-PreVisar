-- V024 — Documentos generables por el sistema (dato, no if por código en el front).

-- Un slot es "generable" si la app puede producir su PDF pre-completado
-- (contrato, carátula). El front muestra el botón "Generar" según este flag.
ALTER TABLE documento_requerido
    ADD COLUMN generable BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE documento_requerido SET generable = TRUE
WHERE codigo IN ('CONTRATO_LOCACION', 'CARATULA');
