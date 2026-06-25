package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Provincia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProvinciaRepository extends JpaRepository<Provincia,Long> {
    /**
     * Buscar una provincia por su código (ej: "CBA").
     * Útil para queries explícitas en lugar de hardcodear ids.
     */
    Optional<Provincia> findByCodigo(String codigo);
}
