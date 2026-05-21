package ar.edu.utn.frc.previsar.entities;

import ar.edu.utn.frc.previsar.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Tabla de autenticación del sistema.
 *
 * Contiene solo lo necesario para loguearse: email, password hash, rol y estado.
 * Los datos personales y profesionales viven en Profesional (composición, no herencia).
 *
 * Tabla creada en V002__create_usuario.sql.
 */

@Entity
@Table(name="usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name="password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ------------------------------------------------------------
    // Callbacks JPA para timestamps automáticos
    // ------------------------------------------------------------
    @PrePersist //justo antes del insert
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.activo == null) {
            this.activo = true;
        }
    }

    @PreUpdate //justo antes del update
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ------------------------------------------------------------
    // equals / hashCode por id (patrón estándar JPA)
    // ------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario that = (Usuario) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
