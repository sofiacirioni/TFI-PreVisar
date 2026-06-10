package ar.edu.utn.frc.previsar.dtos.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteResponseDto {
    private Long id;
    private String nombre;
    private String estado;
    private Long obraId;
    private String obraDesignacion;
    private Long comitenteId;   // derivado de obra -> comitente (para el wizard)
    private String comitenteNombre;
    private Long tipoTareaId;
    private String tipoTareaCodigo;
    private String tipoTareaNombre;
    private Long especialidadId;
    private String especialidadNombre;
    private BigDecimal honorariosReferenciales;
    private BigDecimal aporteRod;
    private BigDecimal aporteArancelAdmin;
    private BigDecimal aporteCajaProfesional;
    private BigDecimal aporteCajaComitente;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Totales derivados (no se persisten). Devuelven null si aun no se calcularon.
    public BigDecimal getTotalCiec() {
        return sumar(aporteRod, aporteArancelAdmin);
    }

    public BigDecimal getTotalCaja() {
        return sumar(aporteCajaProfesional, aporteCajaComitente);
    }

    public BigDecimal getTotal() {
        if (getTotalCiec() == null && getTotalCaja() == null) return null;
        return nz(getTotalCiec()).add(nz(getTotalCaja()));
    }

    private static BigDecimal sumar(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return null;
        return nz(a).add(nz(b));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
