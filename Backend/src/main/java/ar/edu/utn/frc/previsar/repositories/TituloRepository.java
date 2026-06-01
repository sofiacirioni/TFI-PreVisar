package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Titulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TituloRepository extends JpaRepository<Titulo,Long> {
    List<Titulo> findAllByActivoTrueOrderByNombreAsc();
}
