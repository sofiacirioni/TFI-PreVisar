package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.AportesCalculadosDto;
import ar.edu.utn.frc.previsar.entities.ConceptoAporte;
import ar.edu.utn.frc.previsar.entities.ParametroAporte;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.entities.TipoTareaAporte;
import ar.edu.utn.frc.previsar.enums.BaseCalculo;
import ar.edu.utn.frc.previsar.repositories.ParametroAporteRepository;
import ar.edu.utn.frc.previsar.repositories.TipoTareaAporteRepository;
import ar.edu.utn.frc.previsar.services.AporteCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AportesCalculatorServiceImpl implements AporteCalculatorService {
    private static final int ESCALA_MONTO = 2;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final TipoTareaAporteRepository tipoTareaAporteRepo;
    private final ParametroAporteRepository parametroRepo;

    @Override
    public AportesCalculadosDto calcular(TipoTarea tipoTarea, BigDecimal honorarios) {
        if (tipoTarea == null) {
            throw new IllegalArgumentException("El tipo de tarea es obligatorio para calcular aportes");
        }
        if (honorarios == null || honorarios.signum() < 0) {
            throw new IllegalArgumentException("Los honorarios referenciales deben ser un valor no negativo");
        }

        LocalDate hoy = LocalDate.now();
        List<TipoTareaAporte> aportes = tipoTareaAporteRepo.findByTipoTareaConConcepto(tipoTarea.getId());

        List<AportesCalculadosDto.LineaAporte> lineas = aportes.stream()
                .map(tta -> new AportesCalculadosDto.LineaAporte(
                        tta.getConcepto().getCodigo(),
                        tta.getConcepto().getNombre(),
                        tta.getConcepto().getGrupo(),
                        montoDe(tta, honorarios, hoy)))
                .toList();

        return new AportesCalculadosDto(lineas);
    }

    private BigDecimal montoDe(TipoTareaAporte tta, BigDecimal honorarios, LocalDate fecha) {
        ConceptoAporte concepto = tta.getConcepto();
        ParametroAporte p = parametroRepo.findVigente(concepto.getId(), fecha)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay parametro de aporte vigente para " + concepto.getCodigo() + " al " + fecha));

        return switch (p.getTipoValor()) {
            case FIJO -> p.getValor().setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
            case PORCENTAJE -> {
                BigDecimal base = resolverBase(p.getBaseCalculo(), honorarios);
                yield base.multiply(p.getValor())
                        .divide(CIEN, ESCALA_MONTO, RoundingMode.HALF_UP);
            }
        };
    }

    private BigDecimal resolverBase(BaseCalculo base, BigDecimal honorarios) {
        return switch (base) {
            case HONORARIOS -> honorarios;
            case MONTO_OBRA -> throw new UnsupportedOperationException(
                    "Base MONTO_OBRA no soportada en el MVP");
        };
    }
}
