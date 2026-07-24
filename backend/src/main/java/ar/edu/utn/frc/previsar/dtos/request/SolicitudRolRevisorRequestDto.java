package ar.edu.utn.frc.previsar.dtos.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Solicitud de un profesional para obtener el rol de revisor.
 *
 * El mensaje es opcional (el profesional puede justificar su pedido); si no lo
 * envía, la notificación a la institución igual se manda.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudRolRevisorRequestDto {

    @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
    private String mensaje;
}
