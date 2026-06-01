package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="titulo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Titulo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;

    @Column(name = "permite_texto_libre", nullable = false)
    private Boolean permiteTextoLibre;

    @Column(nullable = false)
    private Boolean activo;
}
