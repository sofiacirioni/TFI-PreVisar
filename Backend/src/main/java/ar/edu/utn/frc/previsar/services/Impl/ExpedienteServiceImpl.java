package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.AportesCalculados;
import ar.edu.utn.frc.previsar.dtos.request.ExpedienteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;
import ar.edu.utn.frc.previsar.entities.Expediente;
import ar.edu.utn.frc.previsar.entities.Obra;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.enums.EstadoExpediente;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.ExpedienteMapper;
import ar.edu.utn.frc.previsar.repositories.ExpedienteRepository;
import ar.edu.utn.frc.previsar.repositories.ObraRepository;
import ar.edu.utn.frc.previsar.repositories.TipoTareaRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import ar.edu.utn.frc.previsar.services.AporteCalculatorService;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpedienteServiceImpl implements ExpedienteService {
    private final ExpedienteRepository expedienteRepository;
    private final TipoTareaRepository tipoTareaRepository;
    private final ObraRepository obraRepository;
    private final AporteCalculatorService aporteCalculator;
    private final ExpedienteMapper mapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public ExpedienteResponseDto crear(ExpedienteRequestDto request) {
        Expediente e = new Expediente();
        e.setProfesional(securityUtils.getProfesionalActual());
        e.setEstado(EstadoExpediente.BORRADOR);
        aplicar(e, request);
        return mapper.toResponse(expedienteRepository.save(e));
    }

    @Override
    @Transactional
    public ExpedienteResponseDto actualizarParcial(Long id, ExpedienteRequestDto request) {
        Expediente e = buscarPropio(id);
        aplicar(e, request);
        return mapper.toResponse(expedienteRepository.save(e));
    }

    @Override
    @Transactional
    public ExpedienteResponseDto completar(Long id) {
        Expediente e = buscarPropio(id);

        List<String> faltan = new ArrayList<>();
        if (e.getObra() == null) faltan.add("obra");
        if (e.getTipoTarea() == null) faltan.add("tipo de tarea");
        if (e.getHonorariosReferenciales() == null) faltan.add("honorarios referenciales");
        if (!faltan.isEmpty()) {
            throw new BusinessException("No se puede completar el expediente. Faltan: " + String.join(", ", faltan));
        }

        e.setEstado(EstadoExpediente.COMPLETO);
        return mapper.toResponse(expedienteRepository.save(e));
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponseDto obtener(Long id) {
        return mapper.toResponse(buscarPropio(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDto> listarMisExpedientes() {
        Long profesionalId = securityUtils.getProfesionalActual().getId();
        return expedienteRepository
                .findByProfesionalIdAndActivoTrueOrderByUpdatedAtDesc(profesionalId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Expediente e = buscarPropio(id);
        e.setActivo(false);
        expedienteRepository.save(e);
    }

    // --- helpers ---

    /** Aplica solo los campos presentes (PATCH parcial) y recalcula aportes. */
    private void aplicar(Expediente e, ExpedienteRequestDto req) {
        if (req.getTipoTareaId() != null) {
            e.setTipoTarea(buscarTipoTarea(req.getTipoTareaId()));
        }
        if (req.getObraId() != null) {
            e.setObra(buscarObraPropia(req.getObraId()));
        }
        if (req.getHonorariosReferenciales() != null) {
            e.setHonorariosReferenciales(req.getHonorariosReferenciales());
        }
        recalcularAportes(e);
    }

    private void recalcularAportes(Expediente e) {
        if (e.getTipoTarea() != null && e.getHonorariosReferenciales() != null) {
            AportesCalculados a = aporteCalculator.calcular(e.getTipoTarea(), e.getHonorariosReferenciales());
            e.setAporteRod(a.aporteRod());
            e.setAporteArancelAdmin(a.aporteArancelAdmin());
            e.setAporteCajaProfesional(a.aporteCajaProfesional());
            e.setAporteCajaComitente(a.aporteCajaComitente());
        } else {
            e.setAporteRod(null);
            e.setAporteArancelAdmin(null);
            e.setAporteCajaProfesional(null);
            e.setAporteCajaComitente(null);
        }
    }

    private Expediente buscarPropio(Long id) {
        Long profesionalId = securityUtils.getProfesionalActual().getId();
        return expedienteRepository
                .findByIdAndProfesionalIdAndActivoTrue(id, profesionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado"));
    }

    private TipoTarea buscarTipoTarea(Long tipoTareaId) {
        TipoTarea tt = tipoTareaRepository.findById(tipoTareaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de tarea no encontrado"));
        if (!tt.isActivo()) {
            throw new BusinessException("El tipo de tarea no esta disponible");
        }
        return tt;
    }

    private Obra buscarObraPropia(Long obraId) {
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada"));
        // Pertenencia: obra -> comitente -> profesional. 404 (no 403) ante ajenos.
        Long dueno = obra.getComitente().getProfesional().getId();
        if (!dueno.equals(securityUtils.getProfesionalActual().getId())) {
            throw new ResourceNotFoundException("Obra no encontrada");
        }
        return obra;
    }
}
