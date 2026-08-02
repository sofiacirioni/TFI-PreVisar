package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Comitente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Búsqueda parcial por DNI/CUIT dentro de la cartera del profesional.
     *
     * Los DNI se guardan sin separadores (22444111) y los CUIT con ellos
     * (23-12345678-5), así que comparar el texto crudo haría que "23123456785"
     * no encuentre nada. Se normalizan LOS DOS lados sacando el guion, que por
     * la validación del request es el único carácter no numérico posible.
     *
     * `fragmento` debe llegar ya normalizado desde el service.
     */
    @Query("""
            select c from Comitente c
            where c.profesional.id = :profesionalId
              and c.deletedAt is null
              and replace(c.dniCuit, '-', '') like concat('%', :fragmento, '%')
            order by c.nombreRazonSocial asc
            """)
    List<Comitente> buscarPorFragmentoDniCuit(
            @Param("profesionalId") Long profesionalId,
            @Param("fragmento") String fragmento);

    /**
     * Listar TODOS los comitentes de un profesional, incluyendo los borrados.
     * Útil para pantalla de papelera o auditoría.
     */
    List<Comitente> findByProfesionalId(Long profesionalId);
}
