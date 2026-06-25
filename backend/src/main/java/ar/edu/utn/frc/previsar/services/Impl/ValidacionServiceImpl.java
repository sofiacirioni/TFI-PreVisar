package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.DocumentoValidadoDto;
import ar.edu.utn.frc.previsar.dtos.ObservacionDto;
import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.services.ValidacionService;
import ar.edu.utn.frc.previsar.services.ValidadorExpediente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidacionServiceImpl implements ValidacionService {
    private final List<ValidadorExpediente> validadores;   // Spring inyecta todos los beans

    @Override
    public ValidacionResultadoDto validarNivel(Long expedienteId, int nivel) {
        return validadores.stream()
                .filter(v -> v.nivel() == nivel).findFirst()
                .orElseThrow(() -> new BusinessException("Nivel de validación inexistente: " + nivel))
                .validar(expedienteId);
    }

    @Override
    public ValidacionResultadoDto validarTodo(Long expedienteId) {
        List<DocumentoValidadoDto> docs = new ArrayList<>();
        List<ObservacionDto> generales = new ArrayList<>();
        validadores.stream().sorted(Comparator.comparingInt(ValidadorExpediente::nivel))
                .forEach(v -> {
                    ValidacionResultadoDto r = v.validar(expedienteId);
                    docs.addAll(r.documentos());
                    generales.addAll(r.generales());
                });
        return new ValidacionResultadoDto(docs, generales);
    }
}
