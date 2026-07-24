package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.RevisionExterna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RevisionExternaRepository extends JpaRepository<RevisionExterna, Long> {

    /** Historial del revisor: sus revisiones, más nueva primero. */
    List<RevisionExterna> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    /**
     * Busca una revisión asegurando que pertenezca al usuario. Devuelve Optional
     * vacío ante revisiones ajenas: nadie puede ver ni borrar la de otro cambiando el id.
     */
    Optional<RevisionExterna> findByIdAndUsuarioId(Long id, Long usuarioId);
}
