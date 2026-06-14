package ar.edu.utn.frc.previsar.dtos.response;

import java.math.BigDecimal;
import java.util.List;

public record AportesResponseDto(List<LineaAporteResponse> lineas,
                                 BigDecimal totalCiec,
                                 BigDecimal totalCaja,
                                 BigDecimal total) {
    public record LineaAporteResponse(String conceptoCodigo, String conceptoNombre,
                                      String grupo, BigDecimal monto) {}
}
