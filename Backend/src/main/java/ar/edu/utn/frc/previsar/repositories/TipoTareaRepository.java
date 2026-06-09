package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.TipoTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoTareaRepository extends JpaRepository<TipoTarea, Long> {
    List<TipoTarea> findByActivoTrueOrderByOrden();
    List<TipoTarea> findByEspecialidadIdAndActivoTrueOrderByOrden(Long especialidadId);
    Optional<TipoTarea> findByCodigo(String codigo);
}
