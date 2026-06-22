package ar.edu.utn.frc.previsar.dtos;

import ar.edu.utn.frc.previsar.enums.GrupoAporte;

import java.math.BigDecimal;
import java.util.List;

public record AportesCalculadosDto(List<LineaAporte> lineas) {

    public record LineaAporte(String conceptoCodigo, String conceptoNombre,
                              GrupoAporte grupo, BigDecimal monto) {}

    public BigDecimal totalCiec() {
        return totalPorGrupo(GrupoAporte.CIEC);
    }

    public BigDecimal totalCaja() {
        return totalPorGrupo(GrupoAporte.CAJA);
    }

    public BigDecimal total() {
        return totalCiec().add(totalCaja());
    }

    private BigDecimal totalPorGrupo(GrupoAporte grupo) {
        return lineas.stream()
                .filter(l -> l.grupo() == grupo)
                .map(LineaAporte::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
