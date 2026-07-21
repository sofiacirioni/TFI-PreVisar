package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.AportesCalculadosDto;
import ar.edu.utn.frc.previsar.dtos.request.CalcularAportesRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.ExpedienteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.AportesResponseDto;
import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;
import ar.edu.utn.frc.previsar.entities.Expediente;
import ar.edu.utn.frc.previsar.entities.Obra;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.enums.EstadoArancel;
import ar.edu.utn.frc.previsar.enums.EstadoExpediente;
import ar.edu.utn.frc.previsar.enums.EstadoPago;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.ExpedienteMapper;
import ar.edu.utn.frc.previsar.pdf.*;
import ar.edu.utn.frc.previsar.repositories.ExpedienteRepository;
import ar.edu.utn.frc.previsar.repositories.ObraRepository;
import ar.edu.utn.frc.previsar.repositories.PagoRepository;
import ar.edu.utn.frc.previsar.repositories.TipoTareaRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import ar.edu.utn.frc.previsar.services.AporteCalculatorService;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import ar.edu.utn.frc.previsar.services.PdfGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExpedienteServiceImpl implements ExpedienteService {
    private final ExpedienteRepository expedienteRepository;
    private final TipoTareaRepository tipoTareaRepository;
    private final ObraRepository obraRepository;
    private final AporteCalculatorService aporteCalculator;
    private final ExpedienteMapper mapper;
    private final SecurityUtils securityUtils;
    private final PdfGenerationService pdfGenerationService;
    private final PagoRepository pagoRepository;

    @Override
    @Transactional
    public ExpedienteResponseDto crear(ExpedienteRequestDto request) {
        Expediente e = new Expediente();
        e.setProfesional(securityUtils.getProfesionalActual());
        e.setEstado(EstadoExpediente.BORRADOR);
        aplicar(e, request);
        return aResponse(expedienteRepository.save(e));
    }

    @Override
    @Transactional
    public ExpedienteResponseDto actualizarParcial(Long id, ExpedienteRequestDto request) {
        Expediente e = buscarPropio(id);
        aplicar(e, request);
        return aResponse(expedienteRepository.save(e));
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
            throw new BusinessException("No se puede generar el expediente. Faltan: " + String.join(", ", faltan));
        }

        // Generar = transición BORRADOR -> EN_PROCESO (pasa al armado documental).
        // No hay estado posterior: la entrega/visado formal vive en otro sistema.
        e.setEstado(EstadoExpediente.EN_PROCESO);
        return aResponse(expedienteRepository.save(e));
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponseDto obtener(Long id) {
        return aResponse(buscarPropio(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDto> listarMisExpedientes() {
        Long profesionalId = securityUtils.getProfesionalActual().getId();
        List<Expediente> expedientes = expedienteRepository
                .findByProfesionalIdAndActivoTrueOrderByUpdatedAtDesc(profesionalId);
        if (expedientes.isEmpty()) {
            return List.of();
        }
        // Dos queries para todo el lote (en vez de un exists() por expediente): evita N+1.
        List<Long> ids = expedientes.stream().map(Expediente::getId).toList();
        Set<Long> aprobados = pagoRepository.findExpedienteIdsConEstado(ids, EstadoPago.APROBADO);
        Set<Long> pendientes = pagoRepository.findExpedienteIdsConEstado(ids, EstadoPago.PENDIENTE);
        return expedientes.stream()
                .map(e -> {
                    ExpedienteResponseDto dto = mapper.toResponse(e);
                    dto.setEstadoArancel(derivarEstadoArancel(
                            aprobados.contains(e.getId()), pendientes.contains(e.getId())));
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Expediente e = buscarPropio(id);
        e.setActivo(false);
        expedienteRepository.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public AportesResponseDto calcularAportes(CalcularAportesRequestDto request) {
        if (request.getTipoTareaId() == null || request.getHonorariosReferenciales() == null) {
            throw new BusinessException("Tipo de tarea y honorarios son obligatorios para calcular aportes");
        }
        TipoTarea tipoTarea = tipoTareaRepository.findById(request.getTipoTareaId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de tarea no encontrado"));

        AportesCalculadosDto calc = aporteCalculator.calcular(tipoTarea, request.getHonorariosReferenciales());

        List<AportesResponseDto.LineaAporteResponse> lineas = calc.lineas().stream()
                .map(l -> new AportesResponseDto.LineaAporteResponse(
                        l.conceptoCodigo(), l.conceptoNombre(), l.grupo().name(), l.monto()))
                .toList();

        return new AportesResponseDto(lineas, calc.totalCiec(), calc.totalCaja(), calc.total());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarContrato(Long id, GenerarContratoRequest req) {
        // Mismo patrón de aislamiento 404 que el resto del service.
        Expediente exp = buscarPropio(id);

        BigDecimal referenciales = exp.getHonorariosReferenciales();
        // Si no mandan pactado, se usa el referencial como default.
        BigDecimal honorariosPactados = req != null ? req.honorariosPactados() : null;
        BigDecimal pactados = honorariosPactados != null ? honorariosPactados : referenciales;

        Obra obra = exp.getObra();
        ContratoData data = new ContratoData(
                nombreCompleto(exp.getProfesional()),
                formatMatriculaOrden(exp.getProfesional()),
                exp.getTipoTarea().getEspecialidad().getNombre(),
                exp.getProfesional().getDomicilio(),
                obra.getComitente().getNombreRazonSocial(),
                obra.getComitente().getDniCuit(),
                obra.getComitente().getDomicilio(),
                exp.getTipoTarea().getNombre(),
                domicilioObra(obra),
                localidadConCp(obra),
                obra.getProvincia().getNombre(),
                pactados,
                referenciales,
                obra.getLocalidad(),   // ciudad = localidad de la obra
                // Campos que el profesional completa desde el panel lateral (fallback a puntos si vienen vacíos).
                req != null ? req.documentacionConfeccion() : null,
                req != null ? req.tareasEspeciales() : null,
                req != null ? req.formaPago() : null,
                req != null ? req.plazoEntrega() : null,
                req != null ? req.gastosEspeciales() : null
        );

        return pdfGenerationService.generar(new ContratoTemplate(data));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarCaratula(Long id) {
        Expediente exp = buscarPropio(id);
        CaratulaData data = new CaratulaData(
                exp.getProfesional().getTitulo().getNombre(),
                apellidoNombre(exp.getProfesional()),              // "APELLIDO Nombre"
                formatMatriculaOrden(exp.getProfesional()),        // el helper del contrato
                exp.getObra().getComitente().getNombreRazonSocial(),
                exp.getObra().getComitente().getDniCuit(),
                null,                                              // distribuidora: en blanco
                exp.getObra().getDesignacion(),
                ubicacionObra(exp.getObra()),                      // domicilio - localidad (CP) - provincia
                exp.getTipoTarea().getNombre()
        );
        return pdfGenerationService.generar(new CaratulaTemplate(data));
    }

    /** "Nombre Apellido" del profesional para el encabezado del contrato. */
    private String nombreCompleto(Profesional p) {
        return p.getNombre() + " " + p.getApellido();
    }

    /** "APELLIDO Nombre" — formato de la carátula. */
    private String apellidoNombre(Profesional p) {
        return p.getApellido() + " " + p.getNombre();
    }

    /** "17.373.068 / 4315" — matrícula y, si tiene, número de orden. */
    private String formatMatriculaOrden(Profesional p) {
        return p.getNumeroOrden() != null && !p.getNumeroOrden().isBlank()
                ? p.getMatricula() + " / " + p.getNumeroOrden()
                : p.getMatricula();
    }

    /** "Calle 123 - Barrio" (omite el barrio si no está cargado). */
    private String domicilioObra(Obra obra) {
        String base = obra.getCalle() + " " + obra.getNumero();
        return obra.getBarrio() != null && !obra.getBarrio().isBlank()
                ? base + " - " + obra.getBarrio()
                : base;
    }

    /** "Localidad (CP)" para el renglón de ubicación de la obra. */
    private String localidadConCp(Obra obra) {
        return obra.getLocalidad() + " (" + obra.getCodigoPostal() + ")";
    }

    /** "Calle 123 - Barrio - Localidad (CP) - Provincia" para la carátula. */
    private String ubicacionObra(Obra obra) {
        return domicilioObra(obra) + " - " + localidadConCp(obra) + " - " + obra.getProvincia().getNombre();
    }

    // --- helpers ---

    /** Mapea el expediente a DTO y deriva el estado del arancel a partir de sus pagos. */
    private ExpedienteResponseDto aResponse(Expediente e) {
        ExpedienteResponseDto dto = mapper.toResponse(e);
        dto.setEstadoArancel(derivarEstadoArancel(
                pagoRepository.existsByExpedienteIdAndEstado(e.getId(), EstadoPago.APROBADO),
                pagoRepository.existsByExpedienteIdAndEstado(e.getId(), EstadoPago.PENDIENTE)));
        return dto;
    }

    /** Prioridad APROBADO &gt; PENDIENTE &gt; NINGUNO (un pago rechazado no cuenta). */
    private EstadoArancel derivarEstadoArancel(boolean tieneAprobado, boolean tienePendiente) {
        return EstadoArancel.de(tieneAprobado, tienePendiente);
    }

    /** Aplica solo los campos presentes (PATCH parcial) y recalcula aportes. */
    private void aplicar(Expediente e, ExpedienteRequestDto req) {
        if (req.getNombre() != null) {
            e.setNombre(req.getNombre());
        }
        if (req.getTipoTareaId() != null) {
            e.setTipoTarea(buscarTipoTarea(req.getTipoTareaId()));
        }
        if (req.getObraId() != null) {
            e.setObra(buscarObraPropia(req.getObraId()));
        }
        if (req.getHonorariosReferenciales() != null) {
            e.setHonorariosReferenciales(req.getHonorariosReferenciales());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void verificarPropio(Long id) {
        buscarPropio(id);
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
