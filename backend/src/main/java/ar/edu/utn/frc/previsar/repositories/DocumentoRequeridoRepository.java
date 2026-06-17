package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.DocumentoRequerido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoRequeridoRepository extends JpaRepository<DocumentoRequerido, Long> {

    boolean existsBySeccionIdAndCodigoAndActivoTrue(Long seccionId, String codigo);

    @Query("""
        SELECT COALESCE(MAX(d.orden), 0)
        FROM DocumentoRequerido d
        WHERE d.seccion.id = :seccionId
          AND d.activo = true
        """)
    int maxOrden(@Param("seccionId") Long seccionId);
}
