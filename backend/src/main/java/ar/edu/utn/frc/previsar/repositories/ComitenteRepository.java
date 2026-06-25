package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Comitente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComitenteRepository extends JpaRepository<Comitente, Long> {
    /**
     * Listar todos los comitentes activos (no soft-deleted) de un profesional.
     * Es la query principal de la cartera.
     */
    List<Comitente> findByProfesionalIdAndDeletedAtIsNull(Long profesionalId);

    /**
     * Buscar un comitente específico de un profesional por su DNI/CUIT.
     * Usado para la reutilización: "ya tengo este cliente, traémelo".
     * Solo busca entre los NO eliminados.
     */
    Optional<Comitente> findByProfesionalIdAndDniCuitAndDeletedAtIsNull(
            Long profesionalId, String dniCuit);

    /**
     * Verificar si un profesional ya tiene un comitente con ese DNI/CUIT activo.
     */
    boolean existsByProfesionalIdAndDniCuitAndDeletedAtIsNull(
            Long profesionalId, String dniCuit);

    /**
     * Listar TODOS los comitentes de un profesional, incluyendo los borrados.
     * Útil para pantalla de papelera o auditoría.
     */
    List<Comitente> findByProfesionalId(Long profesionalId);
}
