package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.request.ObraRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ObraResponseDto;
import ar.edu.utn.frc.previsar.entities.Comitente;
import ar.edu.utn.frc.previsar.entities.Obra;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Provincia;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.ObraMapper;
import ar.edu.utn.frc.previsar.repositories.ComitenteRepository;
import ar.edu.utn.frc.previsar.repositories.ObraRepository;
import ar.edu.utn.frc.previsar.repositories.ProvinciaRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import ar.edu.utn.frc.previsar.services.ObraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObraServiceImpl implements ObraService {
    private final ObraRepository obraRepository;
    private final ProvinciaRepository provinciaRepository;
    private final ComitenteRepository comitenteRepository;
    private final SecurityUtils securityUtils;
    private final ObraMapper obraMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ObraResponseDto> listarTodasMisObras() {
        Profesional profesional = securityUtils.getProfesionalActual();
        return obraRepository.findActivasByProfesionalId(profesional.getId())
                .stream()
                .map(obraMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObraResponseDto> listarPorComitente(Long comitenteId) {
        // Valida que el comitente sea del profesional actual
        Comitente comitente = buscarYValidarComitente(comitenteId);

        return obraRepository.findByComitenteIdAndDeletedAtIsNull(comitente.getId())
                .stream()
                .map(obraMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ObraResponseDto obtenerPorId(Long id) {
        Obra obra = buscarYValidarPropiedadObra(id);
        return obraMapper.toResponse(obra);
    }

    @Override
    @Transactional
    public ObraResponseDto crear(Long comitenteId, ObraRequestDto request) {
        Comitente comitente = buscarYValidarComitente(comitenteId);

        Provincia provincia = provinciaRepository.findById(request.getProvinciaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provincia no encontrada: " + request.getProvinciaId()));

        Obra nueva = Obra.builder()
                .comitente(comitente)
                .provincia(provincia)
                .designacion(request.getDesignacion())
                .calle(request.getCalle())
                .numero(request.getNumero())
                .barrio(request.getBarrio())
                .localidad(request.getLocalidad())
                .codigoPostal(request.getCodigoPostal())
                .circunscripcion(request.getCircunscripcion())
                .seccion(request.getSeccion())
                .manzana(request.getManzana())
                .parcela(request.getParcela())
                .build();

        Obra guardada = obraRepository.save(nueva);

        log.info("Obra creada: id={}, comitente={}, designacion={}",
                guardada.getId(), comitente.getId(), guardada.getDesignacion());

        return obraMapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public ObraResponseDto actualizar(Long id, ObraRequestDto request) {
        Obra obra = buscarYValidarPropiedadObra(id);

        Provincia provincia = provinciaRepository.findById(request.getProvinciaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provincia no encontrada: " + request.getProvinciaId()));

        obra.setProvincia(provincia);
        obra.setDesignacion(request.getDesignacion());
        obra.setCalle(request.getCalle());
        obra.setNumero(request.getNumero());
        obra.setBarrio(request.getBarrio());
        obra.setLocalidad(request.getLocalidad());
        obra.setCodigoPostal(request.getCodigoPostal());
        obra.setCircunscripcion(request.getCircunscripcion());
        obra.setSeccion(request.getSeccion());
        obra.setManzana(request.getManzana());
        obra.setParcela(request.getParcela());

        log.info("Obra actualizada: id={}", obra.getId());

        return obraMapper.toResponse(obra);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Obra obra = buscarYValidarPropiedadObra(id);
        obra.softDelete();
        log.info("Obra eliminada (soft): id={}", obra.getId());
    }

    // -------------------------- Helpers --------------------------

    /**
     * Busca un comitente por id y valida que sea del profesional actual y
     * no esté eliminado. Devuelve 404 si no cumple (sin revelar la causa).
     */
    private Comitente buscarYValidarComitente(Long comitenteId) {
        Profesional profesional = securityUtils.getProfesionalActual();

        Comitente comitente = comitenteRepository.findById(comitenteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comitente no encontrado: " + comitenteId));

        if (!comitente.getProfesional().getId().equals(profesional.getId())
                || comitente.isDeleted()) {
            throw new ResourceNotFoundException("Comitente no encontrado: " + comitenteId);
        }

        return comitente;
    }

    /**
     * Busca una obra por id y valida que pertenezca a un comitente del
     * profesional actual y no esté eliminada.
     */
    private Obra buscarYValidarPropiedadObra(Long id) {
        Profesional profesional = securityUtils.getProfesionalActual();

        Obra obra = obraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Obra no encontrada: " + id));

        // La obra pertenece a un comitente, que a su vez pertenece a un profesional
        if (!obra.getComitente().getProfesional().getId().equals(profesional.getId())
                || obra.isDeleted()
                || obra.getComitente().isDeleted()) {
            throw new ResourceNotFoundException("Obra no encontrada: " + id);
        }

        return obra;
    }
}
