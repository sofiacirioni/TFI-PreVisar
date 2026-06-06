package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.response.*;
import ar.edu.utn.frc.previsar.services.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
@Tag(name = "Catalogos", description = "Datos de referencia: provincias, regionales, condiciones IVA")
public class CatalogoController {
    private final CatalogoService catalogoService;

    @GetMapping("/provincias")
    @Operation(summary = "Lista todas las provincias")
    public ResponseEntity<List<ProvinciaResponseDto>> listarProvincias() {
        return ResponseEntity.ok(catalogoService.listarProvincias());
    }

    @GetMapping("/regionales")
    @Operation(summary = "Lista todas las regionales con su provincia")
    public ResponseEntity<List<RegionalResponseDto>> listarRegionales() {
        return ResponseEntity.ok(catalogoService.listarRegionales());
    }

    @GetMapping("/condiciones-iva")
    @Operation(summary = "Lista todas las condiciones frente al IVA")
    public ResponseEntity<List<CondicionIvaResponseDto>> listarCondicionesIva() {
        return ResponseEntity.ok(catalogoService.listarCondicionesIva());
    }

    @GetMapping("/titulos")
    @Operation(summary = "Lista todos los títulos de ingeniería habilitados")
    public ResponseEntity<List<TituloResponseDto>> listarTitulos() {
        return ResponseEntity.ok(catalogoService.listarTitulos());
    }

    @GetMapping("/tipos-tarea")
    public ResponseEntity<List<TipoTareaResponseDto>> listarTiposTarea() {
        return ResponseEntity.ok(catalogoService.listarTiposTarea());
    }
}
