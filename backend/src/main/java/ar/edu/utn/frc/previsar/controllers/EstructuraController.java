package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.DocumentoRequeridoDto;
import ar.edu.utn.frc.previsar.dtos.EstructuraExpedienteDto;
import ar.edu.utn.frc.previsar.dtos.SeccionDto;
import ar.edu.utn.frc.previsar.dtos.request.DocumentoRequeridoRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.SeccionRequestDto;
import ar.edu.utn.frc.previsar.services.EstructuraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Estructura documental configurable (secciones y documentos requeridos) por
 * (provincia, tipo de tarea).
 *
 * Lectura: cualquier usuario autenticado. Escritura: solo el revisor, y solo
 * sobre la estructura de la provincia que ocupa (se valida en el service).
 */
@RestController
@RequestMapping("/api/estructura")
@RequiredArgsConstructor
@Tag(name = "Estructura documental",
        description = "Secciones y documentos requeridos configurables por provincia y tipo de tarea")
public class EstructuraController {

    private final EstructuraService estructuraService;

    @GetMapping
    @Operation(summary = "Estructura documental de un tipo de tarea en una provincia")
    public ResponseEntity<EstructuraExpedienteDto> obtenerEstructura(
            @RequestParam Long tipoTareaId,
            @RequestParam Long provinciaId) {
        return ResponseEntity.ok(estructuraService.obtenerEstructura(tipoTareaId, provinciaId));
    }

    // --- Secciones ----------------------------------------------------------

    @PostMapping("/secciones")
    @Operation(summary = "Crea una sección en la provincia del revisor (solo revisor)")
    public ResponseEntity<SeccionDto> crearSeccion(@Valid @RequestBody SeccionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estructuraService.crearSeccion(request));
    }

    @PutMapping("/secciones/{seccionId}")
    @Operation(summary = "Modifica una sección (solo revisor de su provincia)")
    public ResponseEntity<SeccionDto> actualizarSeccion(
            @PathVariable Long seccionId,
            @Valid @RequestBody SeccionRequestDto request) {
        return ResponseEntity.ok(estructuraService.actualizarSeccion(seccionId, request));
    }

    @DeleteMapping("/secciones/{seccionId}")
    @Operation(summary = "Da de baja una sección y sus documentos (solo revisor de su provincia)")
    public ResponseEntity<Void> eliminarSeccion(@PathVariable Long seccionId) {
        estructuraService.eliminarSeccion(seccionId);
        return ResponseEntity.noContent().build();
    }

    // --- Documentos requeridos ---------------------------------------------

    @PostMapping("/secciones/{seccionId}/documentos")
    @Operation(summary = "Agrega un documento requerido a una sección (solo revisor de su provincia)")
    public ResponseEntity<DocumentoRequeridoDto> crearDocumento(
            @PathVariable Long seccionId,
            @Valid @RequestBody DocumentoRequeridoRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estructuraService.crearDocumento(seccionId, request));
    }

    @PutMapping("/documentos/{documentoId}")
    @Operation(summary = "Modifica un documento requerido (solo revisor de su provincia)")
    public ResponseEntity<DocumentoRequeridoDto> actualizarDocumento(
            @PathVariable Long documentoId,
            @Valid @RequestBody DocumentoRequeridoRequestDto request) {
        return ResponseEntity.ok(estructuraService.actualizarDocumento(documentoId, request));
    }

    @DeleteMapping("/documentos/{documentoId}")
    @Operation(summary = "Da de baja un documento requerido (solo revisor de su provincia)")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable Long documentoId) {
        estructuraService.eliminarDocumento(documentoId);
        return ResponseEntity.noContent().build();
    }

    // --- Clonado ------------------------------------------------------------

    @PostMapping("/clonar")
    @Operation(summary = "Clona la estructura de otra provincia hacia la del revisor (solo revisor)")
    public ResponseEntity<Integer> clonarEstructura(
            @RequestParam Long tipoTareaId,
            @RequestParam Long provinciaOrigenId) {
        return ResponseEntity.ok(estructuraService.clonarEstructura(tipoTareaId, provinciaOrigenId));
    }
}
