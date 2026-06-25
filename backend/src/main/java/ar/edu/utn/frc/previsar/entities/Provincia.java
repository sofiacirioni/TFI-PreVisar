package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

/**
 * Catálogo de provincias argentinas.
 *
 * En el MVP solo se carga Córdoba. La jurisdicción provincial es relevante
 * para las reglas de visado: un revisor solo puede aprobar expedientes de
 * obras ubicadas en su misma provincia.
 *
 * Tabla creada en V001__create_jurisdiccion.sql.
 */

@Entity
@Table(name="provincia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provincia {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long  id;

    @Column(nullable=false, unique=true, length=100)
    private String nombre;

    @Column(nullable=false, unique = true, length=3)
    private String codigo;

    // ------------------------------------------------------------
    // equals / hashCode basados en id (estándar para entidades JPA)
    // ------------------------------------------------------------
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Provincia)) return false;
//        Provincia that = (Provincia) o;
//        return id != null && id.equals(that.id);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id);
//    }
}
