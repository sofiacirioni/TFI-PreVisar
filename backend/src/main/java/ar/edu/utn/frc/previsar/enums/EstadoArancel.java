package ar.edu.utn.frc.previsar.enums;

/**
 * Estado del arancel de un expediente, DERIVADO de sus pagos (no se persiste).
 * Prioridad: APROBADO &gt; PENDIENTE &gt; NINGUNO. Un pago RECHAZADO no cuenta
 * como
 * intento vigente, por eso equivale a NINGUNO (el expediente puede volver a
 * pagarse).
 */
public enum EstadoArancel {
    /**
     * No hay ningun pago aprobado ni pendiente (nunca se pago o el ultimo fue
     * rechazado).
     */
    NINGUNO,
    /**
     * Hay un pago en curso (efectivo/transferencia acreditandose), sin aprobar aun.
     */
    PENDIENTE,
    /** Hay al menos un pago aprobado del arancel. */
    APROBADO;

    /**
     * Aplica la prioridad APROBADO &gt; PENDIENTE &gt; NINGUNO. Vive acá para que la
     * regla no quede duplicada entre el armado del expediente y la sincronización
     * con Mercado Pago.
     */
    public static EstadoArancel de(boolean tieneAprobado, boolean tienePendiente) {
        if (tieneAprobado) {
            return APROBADO;
        }
        return tienePendiente ? PENDIENTE : NINGUNO;
    }
}
