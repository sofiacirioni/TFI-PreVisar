-- V030 — Campos editables del contrato de locación, persistidos en el expediente.
--
-- Antes vivían solo en la pantalla de armado (memoria del navegador): se perdían al
-- recargar y, peor, el compilado generaba el contrato en blanco porque el backend nunca
-- los veía. Son datos del expediente, no del PDF: el PDF se sigue generando on-demand
-- (es estado derivado y no se guarda en disco).

ALTER TABLE expediente
    ADD COLUMN honorarios_pactados      NUMERIC(15, 2),
    ADD COLUMN documentacion_confeccion VARCHAR(1000),
    ADD COLUMN tareas_especiales        VARCHAR(1000),
    ADD COLUMN forma_pago               VARCHAR(500),
    ADD COLUMN plazo_entrega            VARCHAR(255),
    ADD COLUMN gastos_especiales        VARCHAR(500);

COMMENT ON COLUMN expediente.honorarios_pactados IS
    'Honorarios efectivamente convenidos con el comitente. En NULL el contrato usa los referenciales.';
