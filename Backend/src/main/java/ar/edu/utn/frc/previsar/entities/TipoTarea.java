package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_tarea")
@Getter
@Setter
@NoArgsConstructor
public class TipoTarea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "aplica_rod", nullable = false)
    private boolean aplicaRod;

    @Column(name = "aplica_arancel_admin", nullable = false)
    private boolean aplicaArancelAdmin;

    @Column(name = "aplica_caja", nullable = false)
    private boolean aplicaCaja;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
