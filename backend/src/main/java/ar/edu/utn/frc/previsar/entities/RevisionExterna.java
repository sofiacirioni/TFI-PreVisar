package ar.edu.utn.frc.previsar.entities;

import ar.edu.utn.frc.previsar.enums.EstadoRevision;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Un expediente técnico completo (PDF único) que un revisor sube para obtener
 * un resumen general generado por IA: tipo de expediente, profesional que lo
 * presenta, comitente, documentos identificados y problemas graves de calidad.
 *
 * A diferencia del flujo del profesional (que arma el expediente ranura por
 * ranura), acá el revisor NO conoce la estructura exigida: la IA solo describe
 * lo que encuentra, no juzga qué falta.
 *
 * Tabla creada en V029__create_revision_externa.sql.
 */
@Entity
@Table(name = "revision_externa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevisionExterna {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dueño de la revisión: el usuario (revisor) que subió el PDF. El historial es por usuario. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    /** Ruta relativa a la base de almacenamiento (lo que devuelve FileStorageService). */
    @Column(name = "ruta_relativa", nullable = false, length = 500)
    private String rutaRelativa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoRevision estado;

    /** JSON crudo del resumen que devolvió Gemini. Null mientras EN_PROGRESO o si hubo ERROR. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resultado", columnDefinition = "jsonb")
    private String resultado;

    /** Motivo legible cuando estado = ERROR (ej. "Gemini respondió 503"). Null si todo salió bien. */
    @Column(name = "detalle", columnDefinition = "text")
    private String detalle;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
