-- ============================================================
-- V002: Usuario
-- ============================================================
-- Tabla de autenticación. Cada Usuario representa una cuenta
-- que puede loguearse en el sistema. Los datos personales y
-- profesionales NO viven acá: viven en Profesional (V003), que
-- tiene una FK a Usuario (composición, no herencia).
--
-- El "rol" indica el tipo principal de cuenta. Un Profesional
-- puede tener además un RolRevisor asociado (rol secundario),
-- pero eso se modela en V003 con una tabla aparte.
-- ============================================================

CREATE TABLE usuario (
                         id              BIGSERIAL    PRIMARY KEY,
                         email           VARCHAR(255) NOT NULL UNIQUE,
                         password_hash   VARCHAR(255) NOT NULL,
                         rol             VARCHAR(20)  NOT NULL,
                         activo          BOOLEAN      NOT NULL DEFAULT TRUE,
                         created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT chk_usuario_rol CHECK (rol IN ('PROFESIONAL', 'ADMIN'))
);

CREATE INDEX idx_usuario_email ON usuario(email);

COMMENT ON TABLE  usuario               IS 'Tabla de autenticación. Solo datos para loguearse.';
COMMENT ON COLUMN usuario.email         IS 'Email único, usado como username de login.';
COMMENT ON COLUMN usuario.password_hash IS 'Hash BCrypt del password (NUNCA texto plano).';
COMMENT ON COLUMN usuario.rol           IS 'Rol principal. PROFESIONAL o ADMIN. Revisor NO es un rol acá, es secundario (RolRevisor en V003).';
COMMENT ON COLUMN usuario.activo        IS 'Soft disable. Si es false, el usuario no puede loguearse.';