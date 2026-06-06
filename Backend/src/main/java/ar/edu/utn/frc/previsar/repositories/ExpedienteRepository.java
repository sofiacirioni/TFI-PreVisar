package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Expediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {
    // Lista "Mis expedientes"
    List<Expediente> findByProfesionalIdAndActivoTrueOrderByUpdatedAtDesc(Long profesionalId);

    // Fetch con dueño: base del patron 404 ante recursos ajenos
    Optional<Expediente> findByIdAndProfesionalIdAndActivoTrue(Long id, Long profesionalId);
}
