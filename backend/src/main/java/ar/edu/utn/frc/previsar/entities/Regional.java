package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

/**
 * Sede del CIEC dentro de una provincia.
 *
 * Es donde un profesional está matriculado y donde se presenta cada obra.
 * Dato administrativo, no de competencia: un revisor de la regional X
 * puede aprobar expedientes de cualquier regional de la misma provincia.
 *
 * Tabla creada en V001__create_jurisdiccion.sql.
 */

@Entity
@Table(name="regional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Regional {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String nombre;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="provincia_id", nullable=false)
    private Provincia provincia;

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Regional)) return false;
//        Regional that = (Regional) o;
//        return id != null && id.equals(that.id);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id);
//    }

}
