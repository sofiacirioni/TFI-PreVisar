package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Rol secundario que habilita a un Profesional a revisar expedientes
 * en una Provincia.
 *
 * Si un Profesional tiene un RolRevisor asociado, puede revisar
 * expedientes cuyas obras estén ubicadas en la provincia indicada.
 * La validación "este revisor puede ver este expediente" se hace
 * en el service comparando obra.regional.provincia con rolRevisor.provincia.
 *
 * Tabla creada en V005__create_rol_revisor.sql.
 */

@Entity
@Table(name="rol_revisor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolRevisor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="profesional_id", nullable = false, unique = true)
    private Profesional profesional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provincia_id", nullable = false)
    private Provincia provincia;

    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof RolRevisor)) return false;
//        RolRevisor that = (RolRevisor) o;
//        return id != null && id.equals(that.id);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id);
//    }
}
