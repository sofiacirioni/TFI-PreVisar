package ar.edu.utn.frc.previsar.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Campos del contrato de locación, en los dos sentidos: se reciben al guardarlos desde el
 * panel de armado y viajan en la respuesta del expediente para precargar ese panel.
 * Todos opcionales; los límites espejan las columnas de la V030.
 */
public record DatosContratoDto(
        @DecimalMin(value = "0", message = "Los honorarios pactados no pueden ser negativos")
        BigDecimal honorariosPactados,

        @Size(max = 1000) String documentacionConfeccion,
        @Size(max = 1000) String tareasEspeciales,
        @Size(max = 500) String formaPago,
        @Size(max = 255) String plazoEntrega,
        @Size(max = 500) String gastosEspeciales
) {}
