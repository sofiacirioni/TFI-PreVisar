package ar.edu.utn.frc.previsar.dtos.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteResponseDto {
    private Long id;
    private String nombre;
    private String estado;
    private Long obraId;
    private String obraDesignacion;
    private Long provinciaId;     // derivado de obra -> provincia (define la estructura documental)
    private String provinciaNombre;
    private Long comitenteId;   // derivado de obra -> comitente (para el wizard)
    private String comitenteNombre;
    private String comitenteDniCuit;   // DNI o CUIT del comitente (para validaciones de coherencia)
    private Long tipoTareaId;
    private String tipoTareaCodigo;
    private String tipoTareaNombre;
    private Long especialidadId;
    private String especialidadNombre;
    private BigDecimal honorariosReferenciales;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
