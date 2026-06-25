-- ============================================
-- V012: Reemplazar profesional.titulo (texto libre) por FK a titulo + opcional texto libre
-- ============================================

-- 1) Eliminar columna vieja
-- ATENCIÓN: esto elimina los valores de título de todos los profesionales existentes.
ALTER TABLE profesional DROP COLUMN titulo;

-- 2) Agregar FK al catálogo de títulos y campo opcional de texto libre
ALTER TABLE profesional
    ADD COLUMN titulo_id BIGINT NOT NULL,
    ADD COLUMN titulo_otro_descripcion VARCHAR(150) NULL,
    ADD CONSTRAINT fk_profesional_titulo
        FOREIGN KEY (titulo_id) REFERENCES titulo(id);

COMMENT ON COLUMN profesional.titulo_id IS
    'FK al catalogo titulo. Reemplaza el campo de texto libre anterior.';
COMMENT ON COLUMN profesional.titulo_otro_descripcion IS
    'Texto libre del titulo solo si titulo_id apunta a una opcion con permite_texto_libre=true (ej. "Otro"). La consistencia se valida en el backend.';