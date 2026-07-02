package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;
import ar.edu.utn.frc.previsar.services.ValidadorExpediente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidacionNivel3Service implements ValidadorExpediente {
    @Override
    public ValidacionResultadoDto validar(Long expedienteId) {
        return null;
    }

    @Override
    public int nivel() {
        return 3;
    }
}
