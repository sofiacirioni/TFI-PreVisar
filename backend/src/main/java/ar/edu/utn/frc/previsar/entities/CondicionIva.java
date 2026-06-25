package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

/**
 * Catálogo de categorías AFIP frente al IVA.
 *
 * Modelado como tabla (no enum) porque las categorías AFIP pueden
 * agregarse, modificarse o discontinuarse sin necesidad de recompilar.
 *
 * Tabla creada en V003__create_condicion_iva.sql.
 */

@Entity
@Table(name="condicion_iva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CondicionIva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo;

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof CondicionIva)) return false;
//        CondicionIva that = (CondicionIva) o;
//        return id != null && id.equals(that.id);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id);
//    }

}
