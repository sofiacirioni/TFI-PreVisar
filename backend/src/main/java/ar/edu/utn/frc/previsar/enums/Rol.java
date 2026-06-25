package ar.edu.utn.frc.previsar.enums;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Rol principal de un Usuario.
 *
 * - PROFESIONAL: matriculado del CIEC que crea expedientes.
 *   Puede tener además un RolRevisor secundario que lo habilita
 *   a revisar expedientes de su provincia.
 *
 * - ADMIN: rol técnico para administración del sistema (no
 *   pertenece al flujo del CIEC, pensado para tareas internas).
 *
 * IMPORTANTE: revisor NO es un rol acá. Un revisor del CIEC ES
 * un profesional matriculado con un RolRevisor asociado.
 */

public enum Rol {
    PROFESIONAL,
    ADMIN
}
