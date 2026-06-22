package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.DescargaDocumentoDto;
import ar.edu.utn.frc.previsar.dtos.response.DocumentoCargadoResponseDto;
import ar.edu.utn.frc.previsar.services.DocumentoCargadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/expedientes/{expedienteId}/documentos")
@RequiredArgsConstructor
public class DocumentoController {
    private final DocumentoCargadoService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoCargadoResponseDto> subir(
            @PathVariable Long expedienteId,
            @RequestParam Long documentoRequeridoId,
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.subir(expedienteId, documentoRequeridoId, archivo));
    }

    @GetMapping
    public List<DocumentoCargadoResponseDto> listar(@PathVariable Long expedienteId) {
        return service.listar(expedienteId);
    }

    @GetMapping("/{documentoId}")
    public ResponseEntity<Resource> descargar(@PathVariable Long expedienteId, @PathVariable Long documentoId) {
        DescargaDocumentoDto d = service.descargar(expedienteId, documentoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.nombre()).build().toString())
                .body(d.recurso());
    }

    @DeleteMapping("/{documentoId}")
    public ResponseEntity<Void> eliminar(@PathVariable Long expedienteId, @PathVariable Long documentoId) {
        service.eliminar(expedienteId, documentoId);
        return ResponseEntity.noContent().build();
    }
}
