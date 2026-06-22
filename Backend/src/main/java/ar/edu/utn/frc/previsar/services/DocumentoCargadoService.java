package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.DescargaDocumentoDto;
import ar.edu.utn.frc.previsar.dtos.response.DocumentoCargadoResponseDto;
import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import ar.edu.utn.frc.previsar.entities.Expediente;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentoCargadoService {
    DocumentoCargadoResponseDto subir(Long expedienteId, Long documentoRequeridoId, MultipartFile archivo);
    List<DocumentoCargadoResponseDto> listar(Long expedienteId);
    DescargaDocumentoDto descargar(Long expedienteId, Long documentoId);
    void eliminar(Long expedienteId, Long documentoId);
}
