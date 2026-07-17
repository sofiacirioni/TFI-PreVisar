package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoCargadoRepository extends JpaRepository<DocumentoCargado, Long> {
    List<DocumentoCargado> findByExpedienteIdAndActivoTrue(Long expedienteId);
    List<DocumentoCargado> findByExpedienteIdAndDocumentoRequeridoIdAndActivoTrue(Long expedienteId, Long docReqId);
    /** Documentos activos de una sección (documento_cargado -> documento_requerido -> seccion). */
    List<DocumentoCargado> findByExpedienteIdAndDocumentoRequeridoSeccionIdAndActivoTrue(Long expedienteId, Long seccionId);
    Optional<DocumentoCargado> findByIdAndExpedienteIdAndActivoTrue(Long id, Long expedienteId);
}
