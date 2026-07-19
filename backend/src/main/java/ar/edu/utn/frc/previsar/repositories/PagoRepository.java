package ar.edu.utn.frc.previsar.repositories;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ar.edu.utn.frc.previsar.entities.Pago;
import ar.edu.utn.frc.previsar.enums.EstadoPago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByMpPaymentId(String mpPaymentId);

    boolean existsByExpedienteIdAndEstado(Long expedienteId, EstadoPago estado); // para derivar "pagado"

    /** Ids de los expedientes (dentro de la lista dada) que tienen un pago en el estado pedido.
     *  Evita el N+1 al derivar "pagado" en listados. */
    @Query("select distinct p.expediente.id from Pago p "
            + "where p.estado = :estado and p.expediente.id in :expedienteIds")
    Set<Long> findExpedienteIdsConEstado(Collection<Long> expedienteIds, EstadoPago estado);
}
