package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    /**
     * Buscar un usuario por email. Usado en login y en validaciones de unicidad.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Verificar si ya existe un usuario con ese email.
     * Más eficiente que findByEmail si solo necesitás saber si existe.
     */
    boolean existsByEmail(String email);
}
