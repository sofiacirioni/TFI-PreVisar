package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.ValidacionVisual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ValidacionVisualRepository extends JpaRepository<ValidacionVisual, Long> {
    /**
     * Análisis existente para un documento y un contenido dado. NO se usa como caché
     * para saltear el análisis (el profesional espera que "Analizar" vuelva a correr):
     * se usa para ACTUALIZAR esa fila, porque hay un índice único
     * (documento_cargado_id, hash_documento) y un insert nuevo la violaría.
     * Devuelve a lo sumo uno, justamente por ese índice.
     */
    Optional<ValidacionVisual> findByDocumentoCargadoIdAndHashDocumento(Long documentoCargadoId, String hashDocumento);

    /** Último resultado persistido para un documento (el más reciente por contenido analizado). */
    Optional<ValidacionVisual> findFirstByDocumentoCargadoIdOrderByCreatedAtDesc(Long documentoCargadoId);
}
