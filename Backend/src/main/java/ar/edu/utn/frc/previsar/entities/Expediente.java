package ar.edu.utn.frc.previsar.entities;

import ar.edu.utn.frc.previsar.enums.EstadoExpediente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expediente")
@Getter
@Setter
@NoArgsConstructor
public class Expediente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    // Nullable a proposito: un BORRADOR puede no tener obra/tarea elegidas todavia.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_tarea_id")
    private TipoTarea tipoTarea;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoExpediente estado = EstadoExpediente.BORRADOR;

    @Column(name = "honorarios_referenciales", precision = 15, scale = 2)
    private BigDecimal honorariosReferenciales;

    // Aportes calculados (snapshot). Null hasta que se cargan honorarios.
    @Column(name = "aporte_rod", precision = 15, scale = 2)
    private BigDecimal aporteRod;

    @Column(name = "aporte_arancel_admin", precision = 15, scale = 2)
    private BigDecimal aporteArancelAdmin;

    @Column(name = "aporte_caja_profesional", precision = 15, scale = 2)
    private BigDecimal aporteCajaProfesional;

    @Column(name = "aporte_caja_comitente", precision = 15, scale = 2)
    private BigDecimal aporteCajaComitente;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
