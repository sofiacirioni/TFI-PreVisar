package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;

public interface ValidadorExpediente {
    ValidacionResultadoDto validar(Long expedienteId);
    int nivel();

}
