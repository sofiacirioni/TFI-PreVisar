package ar.edu.utn.frc.previsar.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "validacion_visual")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidacionVisual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_cargado_id", nullable = false)
    private DocumentoCargado documentoCargado;

    @Column(name = "hash_documento", nullable = false)
    private String hashDocumento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resultado", nullable = false, columnDefinition = "jsonb")
    private String resultado;      // JSON crudo de Gemini

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
