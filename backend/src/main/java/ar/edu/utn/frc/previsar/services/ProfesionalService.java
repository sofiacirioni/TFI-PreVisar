package ar.edu.utn.frc.previsar.services;

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
}
