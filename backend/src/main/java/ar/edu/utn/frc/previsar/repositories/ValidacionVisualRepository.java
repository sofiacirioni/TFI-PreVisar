package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.ValidacionVisual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ValidacionVisualRepository extends JpaRepository<ValidacionVisual, Long> {
    Optional<ValidacionVisual> findByDocumentoCargadoIdAndHashDocumento(Long documentoCargadoId, String hashDocumento);
}
