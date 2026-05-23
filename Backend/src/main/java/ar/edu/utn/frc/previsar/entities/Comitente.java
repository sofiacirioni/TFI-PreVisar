package ar.edu.utn.frc.previsar.entities;

import ar.edu.utn.frc.previsar.enums.TipoPersona;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cliente de un Profesional. Cada profesional tiene su propia cartera.
 *
 * La unicidad de DNI/CUIT es por profesional (no global). Dos profesionales
 * que atienden al mismo CUIT tienen registros distintos.
 *
 * Soft delete: al "borrar" se setea deleted_at en lugar de hacer DELETE,
 * para preservar la integridad de expedientes históricos.
 *
 * Tabla creada en V006__create_comitente.sql.
 */

@Entity
@Table(name="comitente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comitente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="profesional_id", nullable = false)
    private Profesional profesional;

    @Enumerated(EnumType.STRING)
    @Column(name="tipo_persona", nullable = false, length = 10)
    private TipoPersona tipoPersona;

    @Column(name="nombre_razon_social", nullable = false, length = 200)
    private String nombreRazonSocial;

    @Column(name="dni_cuit", nullable = false, length = 13)
    private String dniCuit;

    @Column(nullable = false, length = 255)
    private String domicilio;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name="deleted_at")
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
    // Helpers de soft delete (opcional, para legibilidad)
    // ------------------------------------------------------------
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Comitente)) return false;
//        Comitente that = (Comitente) o;
//        return id != null && id.equals(that.id);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id);
//    }
}
