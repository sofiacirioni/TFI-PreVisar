package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.request.ProfesionalUpdateRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ProfesionalResponseDto;
import ar.edu.utn.frc.previsar.entities.CondicionIva;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Regional;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.ProfesionalMapper;
import ar.edu.utn.frc.previsar.repositories.CondicionIvaRepository;
import ar.edu.utn.frc.previsar.repositories.ProfesionalRepository;
import ar.edu.utn.frc.previsar.repositories.RegionalRepository;
import ar.edu.utn.frc.previsar.repositories.RolRevisorRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import ar.edu.utn.frc.previsar.services.ProfesionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfesionalServiceImpl implements ProfesionalService {
    private final ProfesionalRepository profesionalRepository;
    private final RegionalRepository regionalRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final RolRevisorRepository rolRevisorRepository;
    private final SecurityUtils securityUtils;
    private final ProfesionalMapper profesionalMapper;

    @Override
    @Transactional(readOnly = true)
    public ProfesionalResponseDto obtenerPerfilActual() {
        Profesional profesional = securityUtils.getProfesionalActual();

        // Recargar con relaciones para evitar N+1 queries al serializar
        Profesional conRelaciones = profesionalRepository
                .findByIdConRelaciones(profesional.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profesional no encontrado: " + profesional.getId()));

        boolean esRevisor = rolRevisorRepository.existsByProfesionalId(conRelaciones.getId());

        return mapearAResponse(conRelaciones, esRevisor);
    }

    @Override
    @Transactional
    public ProfesionalResponseDto actualizarPerfilActual(ProfesionalUpdateRequestDto request) {
        Profesional profesional = securityUtils.getProfesionalActual();

        log.info("Actualizando perfil del profesional id={}", profesional.getId());

        // Validar que los catálogos existen
        Regional regional = regionalRepository.findById(request.getRegionalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Regional no encontrada: " + request.getRegionalId()));

        CondicionIva condicionIva = condicionIvaRepository.findById(request.getCondicionIvaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CondicionIva no encontrada: " + request.getCondicionIvaId()));

        // Aplicar cambios
        profesional.setNombre(request.getNombre());
        profesional.setApellido(request.getApellido());
        profesional.setTitulo(request.getTitulo());
        profesional.setDomicilio(request.getDomicilio());
        profesional.setTelefono(request.getTelefono());
        profesional.setRegional(regional);
        profesional.setCondicionIva(condicionIva);
        profesional.setAfiliadoCaja8470(request.getAfiliadoCaja8470());

        // No hace falta save() explícito: con @Transactional y la
        // entidad managed, Hibernate hace el UPDATE al commit.

        boolean esRevisor = rolRevisorRepository.existsByProfesionalId(profesional.getId());

        // Recargar con relaciones para devolver datos completos
        Profesional actualizado = profesionalRepository
                .findByIdConRelaciones(profesional.getId())
                .orElseThrow();

        return mapearAResponse(actualizado, esRevisor);
    }

    // -------------------------- Mappers --------------------------

    /**
     * Mapper manual de entidad a DTO.
     * En la próxima sesión refactorizamos esto con MapStruct.
     */
    private ProfesionalResponseDto mapearAResponse(Profesional p, boolean esRevisor) {
        ProfesionalResponseDto response = profesionalMapper.toResponse(p);
        response.setEsRevisor(esRevisor);
        return response;
    }
}
