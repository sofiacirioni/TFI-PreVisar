package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.RolRevisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRevisorRepository extends JpaRepository<RolRevisor,Long> {
    /**
     * Buscar el RolRevisor de un profesional. Devuelve Optional vacío
     * si el profesional NO es revisor.
     */
    Optional<RolRevisor> findByProfesionalId(Long profesionalId);

    /**
     * Verificar si un profesional tiene rol de revisor.
     */
    boolean existsByProfesionalId(Long profesionalId);
}
