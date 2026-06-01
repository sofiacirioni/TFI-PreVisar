package ar.edu.utn.frc.previsar.dtos.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionalResponseDto {
    private Long id;
    private String nombre;
    private Long provinciaId;
    private String provinciaNombre;
}
