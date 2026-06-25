package ar.edu.utn.frc.previsar.dtos.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TipoTareaResponseDto {
    private Long id;
    private String codigo;
    private String nombre;
    private Long especialidadId;
    private String especialidadNombre;
}
