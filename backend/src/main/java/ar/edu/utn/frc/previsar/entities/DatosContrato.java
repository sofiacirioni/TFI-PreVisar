package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

/**
 * Campos del contrato de locación que completa el profesional en la pantalla de armado.
 * Van embebidos en las columnas de {@link Expediente} (no son una tabla aparte): pertenecen
 * al expediente y se leen y escriben siempre junto con él.
 *
 * Todos opcionales: el PDF dibuja una línea de puntos donde falta un dato.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosContrato {

    @Column(name = "honorarios_pactados", precision = 15, scale = 2)
    private BigDecimal honorariosPactados;

    @Column(name = "documentacion_confeccion", length = 1000)
    private String documentacionConfeccion;

    @Column(name = "tareas_especiales", length = 1000)
    private String tareasEspeciales;

    @Column(name = "forma_pago", length = 500)
    private String formaPago;

    @Column(name = "plazo_entrega", length = 255)
    private String plazoEntrega;

    @Column(name = "gastos_especiales", length = 500)
    private String gastosEspeciales;
}
