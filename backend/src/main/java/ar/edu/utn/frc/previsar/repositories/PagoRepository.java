package ar.edu.utn.frc.previsar.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frc.previsar.entities.Pago;
import ar.edu.utn.frc.previsar.enums.EstadoPago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByMpPaymentId(String mpPaymentId);

    boolean existsByExpedienteIdAndEstado(Long expedienteId, EstadoPago estado); // para derivar "pagado"
}
