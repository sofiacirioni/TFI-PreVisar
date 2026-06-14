package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.TipoTareaAporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoTareaAporteRepository extends JpaRepository<TipoTareaAporte, Long> {
    @Query("""
        select tta from TipoTareaAporte tta
        join fetch tta.concepto
        where tta.tipoTarea.id = :tipoTareaId
          and tta.activo = true
        """)
    List<TipoTareaAporte> findByTipoTareaConConcepto(@Param("tipoTareaId") Long tipoTareaId);
}
