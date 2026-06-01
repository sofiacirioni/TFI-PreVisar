package ar.edu.utn.frc.previsar.dtos.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CondicionIvaResponseDto {
    private Long id;
    private String descripcion;
    private String codigo;
}
