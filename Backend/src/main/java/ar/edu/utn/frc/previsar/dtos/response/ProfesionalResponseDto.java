package ar.edu.utn.frc.previsar.dtos.response;

import lombok.*;

/**
 * Datos completos del profesional para devolver al frontend.
 * NO incluye el passwordHash ni otros datos sensibles del Usuario.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesionalResponseDto {
    private Long id;

    // Datos del usuario asociado
    private String email;
    private String rol;
    private Boolean activo;

    // Datos personales/profesionales
    private String nombre;
    private String apellido;
    private String dni;
    private String cuit;
    private String matricula;
    private Long tituloId;
    private String tituloNombre;            // del catálogo
    private String tituloOtroDescripcion;   // solo si aplica
    private String domicilio;
    private String telefono;

    // Catálogos: devuelve id + descripcion para que el frontend muestre
    private Long regionalId;
    private String regionalNombre;
    private String provinciaNombre;

    private Long condicionIvaId;
    private String condicionIvaDescripcion;

    private Boolean afiliadoCaja8470;

    // Indica si este profesional tiene rol secundario de revisor
    private Boolean esRevisor;
}
