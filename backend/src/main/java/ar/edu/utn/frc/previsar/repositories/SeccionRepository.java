package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    // Trae la estructura completa de una provincia en una sola query
    // (fetch join evita N+1).
    @Query("""
        SELECT DISTINCT s
        FROM Seccion s
        LEFT JOIN FETCH s.documentos d
        WHERE s.tipoTarea.id = :tipoTareaId
          AND s.provincia.id = :provinciaId
          AND s.activo = true
          AND (d.activo = true OR d IS NULL)
        ORDER BY s.orden, d.orden
        """)
    List<Seccion> findEstructura(@Param("tipoTareaId") Long tipoTareaId,
                                 @Param("provinciaId") Long provinciaId);

    // Estructura scopeada a un expediente: además de los documentos activos, incluye
    // los INACTIVOS que este expediente ya tiene cargados, para no esconder un archivo
    // subido a una ranura recién desactivada por el revisor.
    @Query("""
        SELECT DISTINCT s
        FROM Seccion s
        LEFT JOIN FETCH s.documentos d
        WHERE s.tipoTarea.id = :tipoTareaId
          AND s.provincia.id = :provinciaId
          AND s.activo = true
          AND (d IS NULL
               OR d.activo = true
               OR d.id IN (SELECT dc.documentoRequerido.id FROM DocumentoCargado dc
                           WHERE dc.expediente.id = :expedienteId AND dc.activo = true))
        ORDER BY s.orden, d.orden
        """)
    List<Seccion> findEstructuraParaExpediente(@Param("tipoTareaId") Long tipoTareaId,
                                               @Param("provinciaId") Long provinciaId,
                                               @Param("expedienteId") Long expedienteId);

    boolean existsByProvinciaIdAndTipoTareaIdAndCodigoAndActivoTrue(
            Long provinciaId, Long tipoTareaId, String codigo);

    @Query("""
        SELECT COALESCE(MAX(s.orden), 0)
        FROM Seccion s
        WHERE s.tipoTarea.id = :tipoTareaId
          AND s.provincia.id = :provinciaId
          AND s.activo = true
        """)
    int maxOrden(@Param("tipoTareaId") Long tipoTareaId,
                 @Param("provinciaId") Long provinciaId);
}
