package ar.edu.utn.frc.previsar.services;

import java.math.BigDecimal;

/**
 * Envío de notificaciones por correo. Las implementaciones envían de forma
 * asíncrona y NUNCA propagan errores: un fallo de correo no puede romper la
 * operación que lo disparó (acreditar un pago, solicitar el rol revisor).
 */
public interface EmailService {

    /** Le avisa al profesional dueño del expediente que su arancel quedó acreditado. */
    void notificarPagoAcreditado(Long expedienteId, String destinatario, BigDecimal monto);

    /** Le avisa a la institución que un profesional solicita el rol de revisor. */
    void notificarSolicitudRolRevisor(SolicitudRevisor datos);

    /**
     * Datos ya resueltos (fuera de toda sesión JPA) de una solicitud de rol
     * revisor. Se pasan como valores, no como entidad, porque el envío corre en
     * otro hilo: tocar relaciones lazy de un Profesional detached ahí explota
     * con LazyInitializationException.
     */
    record SolicitudRevisor(
            String nombre,
            String apellido,
            String matricula,
            String numeroOrden,
            String email,
            String provincia,
            String mensaje) {}
}
