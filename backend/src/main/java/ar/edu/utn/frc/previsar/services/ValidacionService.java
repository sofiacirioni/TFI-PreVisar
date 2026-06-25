package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;

public interface ValidacionService {
    /** Corre el nivel pedido. */
    ValidacionResultadoDto validarNivel(Long expedienteId, int nivel);
    /** Corre todos los niveles disponibles y fusiona observaciones. */
    ValidacionResultadoDto validarTodo(Long expedienteId);
}
