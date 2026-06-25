package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {
    List<Especialidad> findByActivoTrueOrderByNombre();

    Optional<Especialidad> findByCodigo(String codigo);
}
