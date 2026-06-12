package ar.edu.utn.frc.previsar.dtos;

import java.math.BigDecimal;

public record AportesCalculados(
        BigDecimal aporteRod,
        BigDecimal aporteArancelAdmin,
        BigDecimal aporteCajaProfesional,
        BigDecimal aporteCajaComitente
) {
    public BigDecimal totalCiec() {
        return aporteRod.add(aporteArancelAdmin);
    }

    public BigDecimal totalCaja() {
        return aporteCajaProfesional.add(aporteCajaComitente);
    }

    public BigDecimal total() {
        return totalCiec().add(totalCaja());
    }
}
