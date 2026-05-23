package ar.edu.utn.frc.previsar.dtos.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Datos de la Obra devueltos al frontend.
 *
 * Incluye datos del comitente asociado (id + nombre) para que el frontend
 * pueda mostrar contexto sin requerir otra llamada.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObraResponseDto {
    private Long id;

    // Comitente asociado
    private Long comitenteId;
    private String comitenteNombre;
    private String comitenteDniCuit;

    // Datos básicos de la obra
    private String designacion;
    private String calle;
    private String numero;
    private String barrio;
    private String localidad;
    private String codigoPostal;

    // Datos catastrales
    private String circunscripcion;
    private String seccion;
    private String manzana;
    private String parcela;

    // Nomenclatura catastral consolidada (XX-XX-XXX-XXX). Null si faltan partes.
    private String nomenclaturaCatastral;

    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
