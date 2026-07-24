package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.request.BajaCuentaRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.CambiarPasswordRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.ProfesionalUpdateRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ProfesionalResponseDto;

/**
 * Operaciones sobre el perfil del Profesional autenticado.
 */
public interface ProfesionalService {
    /**
     * Devuelve el perfil completo del profesional autenticado.
     */
    ProfesionalResponseDto obtenerPerfilActual();

    /**
     * Actualiza el perfil del profesional autenticado.
     */
    ProfesionalResponseDto actualizarPerfilActual(ProfesionalUpdateRequestDto request);

    /**
     * Cambiar contraseña, requiere la actual para compara
     */
    void cambiarPassword(CambiarPasswordRequestDto request);

    /**
     * Da de baja la cuenta del profesional autenticado (soft-delete): deshabilita
     * el acceso poniendo usuario.activo = false. Requiere la contraseña actual como
     * confirmación. Los datos y expedientes se conservan por integridad referencial.
     */
    void darDeBajaCuenta(BajaCuentaRequestDto request);

    /**
     * Registra la solicitud del profesional autenticado para obtener el rol de
     * revisor: notifica por correo a la institución. Falla si ya es revisor.
     * El alta efectiva del rol es manual (desde la administración).
     */
    void solicitarRolRevisor(String mensaje);
}
