package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfesionalRepository extends JpaRepository<Profesional,Long> {
    /**
     * Buscar por id del usuario asociado (útil después del login: tengo
     * el id del usuario autenticado, quiero traer su perfil profesional).
     */
    Optional<Profesional> findByUsuarioId(Long usuarioId);

    /**
     * Buscar por DNI. Usado para validar unicidad.
     */
    Optional<Profesional> findByDni(String dni);

    /**
     * Buscar por CUIT. Usado para validar unicidad.
     */
    Optional<Profesional> findByCuit(String cuit);

    /**
     * Buscar profesional por matrícula.
     */
    Optional<Profesional> findByMatricula(String matricula);

    /**
     * Verificar si existe un profesional con ese DNI.
     */
    boolean existsByDni(String dni);

    /**
     * Verificar si existe un profesional con ese CUIT.
     */
    boolean existsByCuit(String cuit);

    /**
     * Verificar si existe un profesional con esa matrícula.
     */
    boolean existsByMatricula(String matricula);

    /**
     * Trae el profesional con TODAS sus relaciones cargadas de una sola query.
     * Útil cuando sabés que vas a usar usuario + regional + condicionIva
     * (ej: endpoint de perfil completo).
     */
    @Query("""
        SELECT p FROM Profesional p
        JOIN FETCH p.usuario
        JOIN FETCH p.regional r
        JOIN FETCH r.provincia
        JOIN FETCH p.condicionIva
        WHERE p.id = :id
        """)
    Optional<Profesional> findByIdConRelaciones(Long id);

}
