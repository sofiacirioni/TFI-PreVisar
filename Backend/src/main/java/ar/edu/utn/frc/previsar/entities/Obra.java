package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Ubicación física donde se ejecuta la tarea profesional.
 *
 * Asociada a un Comitente (cliente).
 * La provincia determina qué revisor puede
 * ver expedientes asociados a esta obra: la provincia de la
 * regional debe coincidir con la provincia del rol del revisor.
 *
 * Tabla creada en V007__create_obra.sql.
 */

@Entity
@Table(name="obra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Obra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comitente_id", nullable = false)
    private Comitente comitente;

    @Column(nullable = false, length = 255)
    private String designacion;

    @Column(nullable = false, length = 150)
    private String calle;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(length = 100)
    private String barrio;

    @Column(nullable = false, length = 100)
    private String localidad;

    @Column(name = "codigo_postal", nullable = false, length = 10)
    private String codigoPostal;

    @Column(length = 10)
    private String circunscripcion;

    @Column(length = 10)
    private String seccion;

    @Column(length = 10)
    private String manzana;

    @Column(length = 10)
    private String parcela;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Arma la nomenclatura catastral en formato estándar (XX-XX-XXX-XXX).
     * Devuelve null si no hay datos cargados.
     */
    public String getNomenclaturaCatastral() {
        if (circunscripcion == null || seccion == null || manzana == null || parcela == null) {
            return null;
        }
        return String.format("%s-%s-%s-%s", circunscripcion, seccion, manzana, parcela);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Obra)) return false;
        Obra that = (Obra) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
