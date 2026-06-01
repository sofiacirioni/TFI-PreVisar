package ar.edu.utn.frc.previsar.dtos.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Datos que llegan en POST /auth/register.
 *
 * Crea simultáneamente un Usuario (autenticación) y un Profesional
 * (datos del negocio) en una sola transacción.
 *
 * Las validaciones de formato se aplican automáticamente cuando el
 * controller use @Valid sobre el DTO. Si algo falla, Spring devuelve
 * HTTP 400 con detalle antes de ejecutar el service.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDto {
    // ---------- Datos de autenticación ----------

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "El password es obligatorio")
    @Size(min = 8, max = 100, message = "El password debe tener entre 8 y 100 caracteres")
    private String password;

    // ---------- Datos personales del profesional ----------

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100)
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^\\d{7,8}$", message = "El DNI debe tener 7 u 8 dígitos")
    private String dni;

    @NotBlank(message = "El CUIT es obligatorio")
    @Pattern(regexp = "^\\d{2}-\\d{8}-\\d{1}$",
            message = "El CUIT debe tener formato XX-XXXXXXXX-X")
    private String cuit;

    @NotBlank(message = "La matrícula es obligatoria")
    @Size(max = 20)
    private String matricula;

    @NotNull(message = "Debe seleccionar un título")
    private Long tituloId;

    @Size(max = 150)
    private String tituloOtroDescripcion;

    @NotBlank(message = "El domicilio es obligatorio")
    @Size(max = 255)
    private String domicilio;

    @Size(max = 30)
    private String telefono;

    // ---------- Referencias a catálogos ----------

    @NotNull(message = "Debe seleccionar una regional")
    private Long regionalId;

    @NotNull(message = "Debe seleccionar una condición frente al IVA")
    private Long condicionIvaId;

    @NotNull(message = "Debe declarar si está afiliado a la Caja Ley 8470")
    private Boolean afiliadoCaja8470;
}
