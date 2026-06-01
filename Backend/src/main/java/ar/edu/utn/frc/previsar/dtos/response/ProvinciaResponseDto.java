package ar.edu.utn.frc.previsar.dtos.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvinciaResponseDto {
    private Long id;
    private String nombre;
    private String codigo;
}
