package ar.edu.utn.frc.previsar.entities;

import ar.edu.utn.frc.previsar.enums.BaseCalculo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_tarea_aporte")
@Getter
@Setter
@NoArgsConstructor
public class TipoTareaAporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_tarea_id", nullable = false)
    private TipoTarea tipoTarea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concepto_id", nullable = false)
    private ConceptoAporte concepto;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_calculo", nullable = false, length = 15)
    private BaseCalculo baseCalculo;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
