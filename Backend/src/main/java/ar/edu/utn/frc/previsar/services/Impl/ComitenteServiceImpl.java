package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.request.ComitenteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ComitenteResponseDto;
import ar.edu.utn.frc.previsar.entities.Comitente;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.ComitenteMapper;
import ar.edu.utn.frc.previsar.repositories.ComitenteRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import ar.edu.utn.frc.previsar.services.ComitenteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComitenteServiceImpl implements ComitenteService {

    private final ComitenteRepository comitenteRepository;
    private final SecurityUtils securityUtils;
    private final ComitenteMapper comitenteMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ComitenteResponseDto> listar() {
        Profesional profesional = securityUtils.getProfesionalActual();
        return comitenteRepository
                .findByProfesionalIdAndDeletedAtIsNull(profesional.getId())
                .stream()
                .map(comitenteMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComitenteResponseDto obtenerPorId(Long id) {
        Comitente comitente = buscarYValidarPropiedad(id);
        return comitenteMapper.toResponse(comitente);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComitenteResponseDto> buscarPorDniCuit(String dniCuit) {
        Profesional profesional = securityUtils.getProfesionalActual();
        return comitenteRepository
                .findByProfesionalIdAndDniCuitAndDeletedAtIsNull(
                        profesional.getId(), dniCuit)
                .map(comitenteMapper::toResponse);
    }

    @Override
    @Transactional
    public ComitenteResponseDto crear(ComitenteRequestDto request) {
        Profesional profesional = securityUtils.getProfesionalActual();

        // Validar unicidad en la cartera del profesional
        if (comitenteRepository.existsByProfesionalIdAndDniCuitAndDeletedAtIsNull(
                profesional.getId(), request.getDniCuit())) {
            throw new BusinessException(
                    "Ya existe un comitente con ese DNI/CUIT en su cartera: "
                            + request.getDniCuit());
        }

        Comitente nuevo = Comitente.builder()
                .profesional(profesional)
                .tipoPersona(request.getTipoPersona())
                .nombreRazonSocial(request.getNombreRazonSocial())
                .dniCuit(request.getDniCuit())
                .domicilio(request.getDomicilio())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .build();

        Comitente guardado = comitenteRepository.save(nuevo);

        log.info("Comitente creado: id={}, profesional={}, dniCuit={}",
                guardado.getId(), profesional.getId(), guardado.getDniCuit());

        return comitenteMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public ComitenteResponseDto actualizar(Long id, ComitenteRequestDto request) {
        Comitente comitente = buscarYValidarPropiedad(id);

        // Si cambia el DNI/CUIT, validar que no choque con otro comitente
        if (!comitente.getDniCuit().equals(request.getDniCuit())) {
            boolean duplicado = comitenteRepository
                    .existsByProfesionalIdAndDniCuitAndDeletedAtIsNull(
                            comitente.getProfesional().getId(), request.getDniCuit());
            if (duplicado) {
                throw new BusinessException(
                        "Ya existe otro comitente con ese DNI/CUIT en su cartera: "
                                + request.getDniCuit());
            }
        }

        comitente.setTipoPersona(request.getTipoPersona());
        comitente.setNombreRazonSocial(request.getNombreRazonSocial());
        comitente.setDniCuit(request.getDniCuit());
        comitente.setDomicilio(request.getDomicilio());
        comitente.setEmail(request.getEmail());
        comitente.setTelefono(request.getTelefono());

        log.info("Comitente actualizado: id={}", comitente.getId());

        return comitenteMapper.toResponse(comitente);

    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Comitente comitente = buscarYValidarPropiedad(id);
        comitente.softDelete();
        log.info("Comitente eliminado (soft): id={}", comitente.getId());
    }

    // -------------------------- Helpers --------------------------

    /**
     * Busca un comitente por id y valida que pertenezca al profesional actual
     * y que no esté soft-deleted. Lanza ResourceNotFoundException si no cumple.
     *
     * Esta función centraliza la regla de aislamiento de carteras: ningún
     * profesional puede tocar comitentes de otro.
     */
    private Comitente buscarYValidarPropiedad(Long id) {
        Profesional profesional = securityUtils.getProfesionalActual();

        Comitente comitente = comitenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comitente no encontrado: " + id));

        // Validar propiedad
        if (!comitente.getProfesional().getId().equals(profesional.getId())) {
            // Importante: devolvemos "no encontrado" en lugar de "no autorizado"
            // para no revelar la existencia de comitentes ajenos.
            throw new ResourceNotFoundException("Comitente no encontrado: " + id);
        }

        // Validar que no esté eliminado
        if (comitente.isDeleted()) {
            throw new ResourceNotFoundException("Comitente no encontrado: " + id);
        }

        return comitente;
    }
}
