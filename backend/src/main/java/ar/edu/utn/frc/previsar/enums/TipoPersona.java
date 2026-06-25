package ar.edu.utn.frc.previsar.enums;

/**
 * Tipo de persona del Comitente, según clasificación AFIP.
 *
 * - FISICA:   persona individual (DNI).
 * - JURIDICA: empresa, asociación, sociedad (CUIT).
 *
 * En el expediente de referencia "Los Varta SA" es JURIDICA.
 */
public enum TipoPersona {
    FISICA,
    JURIDICA
}
