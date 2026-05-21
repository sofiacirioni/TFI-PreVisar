package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.CondicionIva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CondicionIvaRepository extends JpaRepository<CondicionIva,Long> {
    /**
     * Buscar por código (ej: "RESPONSABLE_INSCRIPTO").
     */
    Optional<CondicionIva> findByCodigo(String codigo);

    /**
     * Listar solo las categorías activas. Lo que va al dropdown del frontend.
     */
    List<CondicionIva> findByActivoTrue();
}
