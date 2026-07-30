package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.DatosContratoDto;
import ar.edu.utn.frc.previsar.dtos.request.CalcularAportesRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.ExpedienteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.AportesResponseDto;
import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;

import java.util.List;

public interface ExpedienteService {
    ExpedienteResponseDto crear(ExpedienteRequestDto request);
    ExpedienteResponseDto actualizarParcial(Long id, ExpedienteRequestDto request);
    ExpedienteResponseDto completar(Long id);
    ExpedienteResponseDto obtener(Long id);
    List<ExpedienteResponseDto > listarMisExpedientes();
    void eliminar(Long id);
    AportesResponseDto calcularAportes(CalcularAportesRequestDto request);
    /** Guarda los campos editables del contrato (panel de armado) y devuelve el expediente actualizado. */
    ExpedienteResponseDto actualizarDatosContrato(Long id, DatosContratoDto datos);
    byte[] generarContrato(Long id);
    byte[] generarCaratula(Long id);
    /**
     * PDF de la ranura generable identificada por su código (documento_requerido.generable),
     * con los datos del expediente. Devuelve null si ese código no tiene generador asociado.
     * Único lugar que traduce código de ranura -> documento del sistema.
     */
    byte[] generarDocumento(Long id, String codigoDocumento);
    /** Verifica que el expediente exista y pertenezca al profesional actual (404 ante ajenos). */
    void verificarPropio(Long id);
}
