package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.AportesCalculados;
import ar.edu.utn.frc.previsar.entities.ParametroAporte;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.enums.BaseCalculo;
import ar.edu.utn.frc.previsar.enums.ConceptoAporte;
import ar.edu.utn.frc.previsar.repositories.ParametroAporteRepository;
import ar.edu.utn.frc.previsar.services.AporteCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AportesCalculatorServiceImpl implements AporteCalculatorService {
    private static final int ESCALA_MONTO = 2;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ParametroAporteRepository parametroRepo;

    @Override
    public AportesCalculados calcular(TipoTarea tipoTarea, BigDecimal honorarios) {
        if (tipoTarea == null) {
            throw new IllegalArgumentException("El tipo de tarea es obligatorio para calcular aportes");
        }
        if (honorarios == null || honorarios.signum() < 0) {
            throw new IllegalArgumentException("Los honorarios referenciales deben ser un valor no negativo");
        }

        LocalDate hoy = LocalDate.now();

        BigDecimal rod = tipoTarea.isAplicaRod()
                ? montoConcepto(ConceptoAporte.ROD, honorarios, hoy)
                : cero();
        BigDecimal arancel = tipoTarea.isAplicaArancelAdmin()
                ? montoConcepto(ConceptoAporte.ARANCEL_ADMIN, honorarios, hoy)
                : cero();
        BigDecimal cajaProf = tipoTarea.isAplicaCaja()
                ? montoConcepto(ConceptoAporte.CAJA_PROFESIONAL, honorarios, hoy)
                : cero();
        BigDecimal cajaComit = tipoTarea.isAplicaCaja()
                ? montoConcepto(ConceptoAporte.CAJA_COMITENTE, honorarios, hoy)
                : cero();

        return new AportesCalculados(rod, arancel, cajaProf, cajaComit);
    }

    private BigDecimal montoConcepto(ConceptoAporte concepto, BigDecimal honorarios, LocalDate fecha) {
        ParametroAporte p = parametroRepo.findVigente(concepto, fecha)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay parametro de aporte vigente para " + concepto + " al " + fecha));

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

    private BigDecimal cero() {
        return BigDecimal.ZERO.setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
    }
}
