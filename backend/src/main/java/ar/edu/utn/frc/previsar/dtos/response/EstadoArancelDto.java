package ar.edu.utn.frc.previsar.dtos.response;

import ar.edu.utn.frc.previsar.enums.EstadoArancel;

/** Estado del arancel tras reconciliar contra Mercado Pago. */
public record EstadoArancelDto(EstadoArancel estadoArancel) {
}
