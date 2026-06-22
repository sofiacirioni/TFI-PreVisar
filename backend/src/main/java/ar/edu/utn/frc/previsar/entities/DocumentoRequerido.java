package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documento_requerido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoRequerido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false)
    @Builder.Default
    private boolean obligatorio = true;

    @Column(nullable = false)
    private Integer orden;

    @Column(name="permite_multiples")
    private boolean permiteMultiples;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
