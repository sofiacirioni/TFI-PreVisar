package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.response.RevisionExternaDetalleDto;
import ar.edu.utn.frc.previsar.dtos.response.RevisionExternaResumenDto;
import ar.edu.utn.frc.previsar.dtos.response.RevisionMetricasDto;
import ar.edu.utn.frc.previsar.services.RevisionExternaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Funciones del rol revisor: subir un expediente completo (PDF) y obtener un
 * resumen general generado por IA, más el historial de revisiones.
 *
 * El rol de revisor se valida en el service (no hay method-security): cada
 * operación exige RolRevisor y opera solo sobre revisiones propias.
 */
@RestController
@RequestMapping("/api/revisor/revisiones")
@RequiredArgsConstructor
public class RevisorController {
    private final RevisionExternaService service;

    /** Sube el PDF, crea la revisión (sync, con SecurityContext) y dispara el análisis async. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Long>> crear(@RequestParam("archivo") MultipartFile archivo) {
        Long id = service.crear(archivo);          // valida rol + PDF, almacena, estado EN_PROGRESO
        service.analizarAsync(id);                 // cross-bean → el proxy @Async aplica
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("id", id));
    }

    @GetMapping
    public List<RevisionExternaResumenDto> listar() {
        return service.listar();
    }

    /**
     * Métricas del historial propio. Va antes de /{id} para dejar explícito que
     * "metricas" es un recurso y no un identificador.
     */
    @GetMapping("/metricas")
    public RevisionMetricasDto metricas() {
        return service.metricas();
    }

    @GetMapping("/{id}")
    public RevisionExternaDetalleDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
