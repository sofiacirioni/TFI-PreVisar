package ar.edu.utn.frc.previsar.enums;

public enum EstadoExpediente {
    /** En armado en el wizard; guarda progreso parcial. */
    BORRADOR,
    /** Generado: el wizard lo confirmó y pasó al armado documental. */
    EN_PROCESO
    // La entrega/visado formal vive en otro sistema; esta app es la capa de
    // prevalidación, por eso no hay un estado terminal de "entregado/completo".
}
