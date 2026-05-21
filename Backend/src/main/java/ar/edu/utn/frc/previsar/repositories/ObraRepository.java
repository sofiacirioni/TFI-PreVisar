package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Obra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObraRepository extends JpaRepository<Obra,Long> {
    /**
     * Listar todas las obras activas de un comitente específico.
     */
    List<Obra> findByComitenteIdAndDeletedAtIsNull(Long comitenteId);

    /**
     * Listar todas las obras activas de un profesional (a través de sus comitentes).
     */
    @Query("""
        SELECT o FROM Obra o
        WHERE o.comitente.profesional.id = :profesionalId
          AND o.deletedAt IS NULL
          AND o.comitente.deletedAt IS NULL
        """)
    List<Obra> findActivasByProfesionalId(Long profesionalId);

    /**
     * Trae una obra con todas sus relaciones cargadas de una sola query.
     * Útil para mostrar detalle de obra o generar PDFs (donde necesitás
     * datos del comitente y la regional).
     */
    @Query("""
        SELECT o FROM Obra o
        JOIN FETCH o.comitente c
        JOIN FETCH c.profesional
        WHERE o.id = :id
        """)
    Optional<Obra> findByIdConRelaciones(Long id);
}
