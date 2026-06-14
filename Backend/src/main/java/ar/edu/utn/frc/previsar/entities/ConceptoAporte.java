package ar.edu.utn.frc.previsar.entities;

import ar.edu.utn.frc.previsar.enums.GrupoAporte;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "concepto_aporte")
@Getter
@Setter
@NoArgsConstructor
public class ConceptoAporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo", nullable = false, length = 10)
    private GrupoAporte grupo;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
