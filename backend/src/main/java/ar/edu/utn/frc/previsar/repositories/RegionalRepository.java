package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Regional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionalRepository extends JpaRepository<Regional,Long> {
    /**
     * Listar todas las regionales de una provincia.
     * Útil para poblar el dropdown del formulario de profesional.
     */
    List<Regional> findByProvinciaId(Long provinciaId);
}
