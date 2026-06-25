package ar.edu.utn.frc.previsar.dtos;

import org.springframework.core.io.Resource;

// Holder para que el controller arme la descarga
public record DescargaDocumentoDto(
        Resource recurso, String nombre, String tipoMime
) {}
