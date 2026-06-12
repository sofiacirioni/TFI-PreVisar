package ar.edu.utn.frc.previsar.entities;

import ar.edu.utn.frc.previsar.enums.BaseCalculo;
import ar.edu.utn.frc.previsar.enums.ConceptoAporte;
import ar.edu.utn.frc.previsar.enums.TipoValor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parametro_aporte")
@Getter
@Setter
@NoArgsConstructor
public class ParametroAporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "concepto", nullable = false, length = 30)
    private ConceptoAporte concepto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_valor", nullable = false, length = 15)
    private TipoValor tipoValor;

    @Column(name = "valor", nullable = false, precision = 15, scale = 4)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_calculo", length = 15)
    private BaseCalculo baseCalculo;   // null para los FIJO

    @Column(name = "vigencia_desde", nullable = false)
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;   // null = vigente

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
