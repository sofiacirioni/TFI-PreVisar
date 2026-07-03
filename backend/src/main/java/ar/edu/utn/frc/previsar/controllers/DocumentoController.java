package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.DescargaDocumentoDto;
import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;
import ar.edu.utn.frc.previsar.dtos.response.DocumentoCargadoResponseDto;
import ar.edu.utn.frc.previsar.pdf.PdfResponseFactory;
import ar.edu.utn.frc.previsar.services.CompilacionService;
import ar.edu.utn.frc.previsar.services.DocumentoCargadoService;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import ar.edu.utn.frc.previsar.services.ValidacionService;
import ar.edu.utn.frc.previsar.services.Impl.AnalisisEstadoTracker;
import ar.edu.utn.frc.previsar.services.Impl.ValidacionNivel3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expedientes/{expedienteId}/documentos")
@RequiredArgsConstructor
public class DocumentoController {
    private final DocumentoCargadoService service;
    private final ValidacionService validacionService;
    private final CompilacionService compilacionService;
    private final ExpedienteService expedienteService;
    private final ValidacionNivel3Service validacionNivel3Service;
    private final AnalisisEstadoTracker analisisEstadoTracker;

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

    @GetMapping("/compilado")
    public ResponseEntity<ByteArrayResource> compilar(@PathVariable Long expedienteId) {
        byte[] pdf = compilacionService.compilar(expedienteId);
        return PdfResponseFactory.attachment(pdf, "expediente-" + expedienteId);
    }

    @GetMapping("/validacion")
    public ValidacionResultadoDto validar(@PathVariable Long expedienteId) {
        return validacionService.validarTodo(expedienteId);   // on-demand, nada se persiste
    }

    @PostMapping("/validacion/ia")
    public ResponseEntity<Void> analizarConIa(@PathVariable Long expedienteId) {
        expedienteService.verificarPropio(expedienteId);          // sync: acá SÍ hay SecurityContext
        if (!analisisEstadoTracker.iniciarSiLibre(expedienteId))  // gate atómico: evita doble disparo
            return ResponseEntity.status(HttpStatus.CONFLICT).build();   // 409: ya hay un análisis en curso
        validacionNivel3Service.analizarAsync(expedienteId);      // cross-bean → el proxy @Async aplica
        return ResponseEntity.accepted().build();                 // 202
    }

    @GetMapping("/validacion/ia/estado")
    public Map<String, String> estadoIa(@PathVariable Long expedienteId) {
        expedienteService.verificarPropio(expedienteId);
        var estado = analisisEstadoTracker.estado(expedienteId);
        return Map.of("estado", estado != null ? estado.name() : "SIN_INICIAR");
    }

    @DeleteMapping("/{documentoId}")
    public ResponseEntity<Void> eliminar(@PathVariable Long expedienteId, @PathVariable Long documentoId) {
        service.eliminar(expedienteId, documentoId);
        return ResponseEntity.noContent().build();
    }
}
