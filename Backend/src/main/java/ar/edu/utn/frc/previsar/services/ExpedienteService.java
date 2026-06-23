package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.request.CalcularAportesRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.ExpedienteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.AportesResponseDto;
import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;
import ar.edu.utn.frc.previsar.pdf.GenerarContratoRequest;

import java.util.List;

public interface ExpedienteService {
    ExpedienteResponseDto crear(ExpedienteRequestDto request);
    ExpedienteResponseDto actualizarParcial(Long id, ExpedienteRequestDto request);
    ExpedienteResponseDto completar(Long id);
    ExpedienteResponseDto obtener(Long id);
    List<ExpedienteResponseDto > listarMisExpedientes();
    void eliminar(Long id);
    AportesResponseDto calcularAportes(CalcularAportesRequestDto request);
    byte[] generarContrato(Long id, GenerarContratoRequest req);
}
