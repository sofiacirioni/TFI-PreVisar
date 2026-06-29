package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.DocumentoValidadoDto;
import ar.edu.utn.frc.previsar.dtos.ObservacionDto;
import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.services.ValidacionService;
import ar.edu.utn.frc.previsar.services.ValidadorExpediente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

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
        List<ObservacionDto> generales = new ArrayList<>();
        Map<Long, DocumentoValidadoDto> porDoc = new LinkedHashMap<>();

        validadores.stream().sorted(Comparator.comparingInt(ValidadorExpediente::nivel))
                .forEach(v -> {
                    ValidacionResultadoDto r = v.validar(expedienteId);
                    generales.addAll(r.generales());
                    for (DocumentoValidadoDto dv : r.documentos()) {
                        porDoc.merge(dv.documentoCargadoId(), dv, (a, b) -> {
                            List<ObservacionDto> union = new ArrayList<>(a.observaciones());
                            union.addAll(b.observaciones());
                            return new DocumentoValidadoDto(a.documentoCargadoId(),
                                    a.documentoRequeridoId(), a.nombreOriginal(), union);
                        });
                    }
                });

        return new ValidacionResultadoDto(new ArrayList<>(porDoc.values()), generales);
    }
}
