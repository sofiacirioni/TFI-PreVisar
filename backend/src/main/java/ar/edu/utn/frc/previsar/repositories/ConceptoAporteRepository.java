package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.ConceptoAporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConceptoAporteRepository extends JpaRepository<ConceptoAporte, Long> {
    Optional<ConceptoAporte> findByCodigo(String codigo);
}
