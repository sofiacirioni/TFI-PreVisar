package ar.edu.utn.frc.previsar.repositories;

import ar.edu.utn.frc.previsar.entities.ParametroAporte;
import ar.edu.utn.frc.previsar.enums.ConceptoAporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ParametroAporteRepository extends JpaRepository<ParametroAporte, Long> {
    @Query("""
        select p from ParametroAporte p
        where p.concepto = :concepto
          and p.activo = true
          and p.vigenciaDesde <= :fecha
          and (p.vigenciaHasta is null or p.vigenciaHasta >= :fecha)
        """)
    Optional<ParametroAporte> findVigente(@Param("concepto") ConceptoAporte concepto,
                                          @Param("fecha") LocalDate fecha);
}
