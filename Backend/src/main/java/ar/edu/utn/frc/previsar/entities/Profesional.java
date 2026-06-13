package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Datos personales y profesionales del matriculado CIEC.
 *
 * Composición con Usuario: un Profesional TIENE un Usuario
 * que lo autentica. Si el profesional adquiere el rol secundario de revisor,
 * se crea un RolRevisor asociado.
 *
 * Tabla creada en V004__create_profesional.sql.
 */

@Entity
@Table(name="profesional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profesional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 20)
    private String dni;

    @Column(nullable = false, unique = true, length = 13)
    private String cuit;

    @Column(nullable = false, unique = true, length = 20)
    private String matricula;

    @Column(name = "numero_orden", length = 4)
    private String numeroOrden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "titulo_id", nullable = false)
    private Titulo titulo;

    @Column(name = "titulo_otro_descripcion", length = 150)
    private String tituloOtroDescripcion;

    @Column(nullable = false, length = 255)
    private String domicilio;

    @Column(length = 30)
    private String telefono;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="regional_id", nullable = false)
    private Regional regional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="condicion_iva_id", nullable = false)
    private CondicionIva condicionIva;

    @Column(name="afiliado_caja_8470", nullable = false)
    private Boolean afiliadoCaja8470;

    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.afiliadoCaja8470 == null) {
            this.afiliadoCaja8470 = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Profesional)) return false;
//        Profesional that = (Profesional) o;
//        return id != null && id.equals(that.id);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id);
//    }

}
