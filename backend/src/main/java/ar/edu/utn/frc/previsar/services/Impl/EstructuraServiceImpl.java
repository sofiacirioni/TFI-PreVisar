package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.DocumentoRequeridoDto;
import ar.edu.utn.frc.previsar.dtos.EstructuraExpedienteDto;
import ar.edu.utn.frc.previsar.dtos.SeccionDto;
import ar.edu.utn.frc.previsar.dtos.request.DocumentoRequeridoRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.SeccionRequestDto;
import ar.edu.utn.frc.previsar.entities.DocumentoRequerido;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Provincia;
import ar.edu.utn.frc.previsar.entities.Seccion;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ForbiddenException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.EstructuraMapper;
import ar.edu.utn.frc.previsar.repositories.DocumentoRequeridoRepository;
import ar.edu.utn.frc.previsar.repositories.RolRevisorRepository;
import ar.edu.utn.frc.previsar.repositories.SeccionRepository;
import ar.edu.utn.frc.previsar.repositories.TipoTareaRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import ar.edu.utn.frc.previsar.services.EstructuraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstructuraServiceImpl implements EstructuraService {

    private final SeccionRepository seccionRepository;
    private final DocumentoRequeridoRepository documentoRepository;
    private final TipoTareaRepository tipoTareaRepository;
    private final RolRevisorRepository rolRevisorRepository;
    private final EstructuraMapper estructuraMapper;
    private final SecurityUtils securityUtils;

    @Override
    public EstructuraExpedienteDto obtenerEstructura(Long tipoTareaId, Long provinciaId) {
        TipoTarea tipoTarea = tipoTareaRepository.findById(tipoTareaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de tarea no encontrado: " + tipoTareaId));

        List<Seccion> secciones = seccionRepository.findEstructura(tipoTareaId, provinciaId);

        return new EstructuraExpedienteDto(
                tipoTarea.getId(),
                tipoTarea.getCodigo(),
                estructuraMapper.toDtoList(secciones));
    }

    @Override
    @Transactional
    public SeccionDto crearSeccion(SeccionRequestDto request) {
        Provincia provincia = provinciaDelRevisorActual();
        TipoTarea tipoTarea = buscarTipoTarea(request.getTipoTareaId());

        if (seccionRepository.existsByProvinciaIdAndTipoTareaIdAndCodigoAndActivoTrue(
                provincia.getId(), tipoTarea.getId(), request.getCodigo())) {
            throw new BusinessException(
                    "Ya existe una sección con el código '" + request.getCodigo() + "' para ese tipo de tarea");
        }

        int orden = request.getOrden() != null
                ? request.getOrden()
                : seccionRepository.maxOrden(tipoTarea.getId(), provincia.getId()) + 1;

        Seccion seccion = Seccion.builder()
                .provincia(provincia)
                .tipoTarea(tipoTarea)
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .orden(orden)
                .activo(true)
                .build();

        return estructuraMapper.toDto(seccionRepository.save(seccion));
    }

    @Override
    @Transactional
    public SeccionDto actualizarSeccion(Long seccionId, SeccionRequestDto request) {
        Seccion seccion = buscarSeccion(seccionId);
        assertRevisorDeProvincia(seccion.getProvincia());

        // El codigo es clave estable: no se cambia al modificar. Solo nombre/orden.
        seccion.setNombre(request.getNombre());
        if (request.getOrden() != null) {
            seccion.setOrden(request.getOrden());
        }

        return estructuraMapper.toDto(seccionRepository.save(seccion));
    }

    @Override
    @Transactional
    public void eliminarSeccion(Long seccionId) {
        Seccion seccion = buscarSeccion(seccionId);
        assertRevisorDeProvincia(seccion.getProvincia());

        seccion.setActivo(false);
        seccion.getDocumentos().forEach(d -> d.setActivo(false));
        seccionRepository.save(seccion);
    }

    @Override
    @Transactional
    public DocumentoRequeridoDto crearDocumento(Long seccionId, DocumentoRequeridoRequestDto request) {
        Seccion seccion = buscarSeccion(seccionId);
        assertRevisorDeProvincia(seccion.getProvincia());

        if (documentoRepository.existsBySeccionIdAndCodigoAndActivoTrue(seccionId, request.getCodigo())) {
            throw new BusinessException(
                    "Ya existe un documento con el código '" + request.getCodigo() + "' en esa sección");
        }

        int orden = request.getOrden() != null
                ? request.getOrden()
                : documentoRepository.maxOrden(seccionId) + 1;

        DocumentoRequerido documento = DocumentoRequerido.builder()
                .seccion(seccion)
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .obligatorio(request.isObligatorio())
                .orden(orden)
                .activo(true)
                .build();

        return estructuraMapper.toDto(documentoRepository.save(documento));
    }

    @Override
    @Transactional
    public DocumentoRequeridoDto actualizarDocumento(Long documentoId, DocumentoRequeridoRequestDto request) {
        DocumentoRequerido documento = buscarDocumento(documentoId);
        assertRevisorDeProvincia(documento.getSeccion().getProvincia());

        documento.setNombre(request.getNombre());
        documento.setObligatorio(request.isObligatorio());
        if (request.getOrden() != null) {
            documento.setOrden(request.getOrden());
        }

        return estructuraMapper.toDto(documentoRepository.save(documento));
    }

    @Override
    @Transactional
    public void eliminarDocumento(Long documentoId) {
        DocumentoRequerido documento = buscarDocumento(documentoId);
        assertRevisorDeProvincia(documento.getSeccion().getProvincia());

        documento.setActivo(false);
        documentoRepository.save(documento);
    }

    @Override
    @Transactional
    public int clonarEstructura(Long tipoTareaId, Long provinciaOrigenId) {
        Provincia destino = provinciaDelRevisorActual();
        if (destino.getId().equals(provinciaOrigenId)) {
            throw new BusinessException("La provincia de origen y destino no pueden ser la misma");
        }
        buscarTipoTarea(tipoTareaId);

        List<Seccion> origen = seccionRepository.findEstructura(tipoTareaId, provinciaOrigenId);
        if (origen.isEmpty()) {
            throw new ResourceNotFoundException(
                    "La provincia de origen no tiene estructura para ese tipo de tarea");
        }
        if (!seccionRepository.findEstructura(tipoTareaId, destino.getId()).isEmpty()) {
            throw new BusinessException(
                    "Tu provincia ya tiene una estructura para ese tipo de tarea; eliminala antes de clonar");
        }

        for (Seccion s : origen) {
            Seccion copia = seccionRepository.save(Seccion.builder()
                    .provincia(destino)
                    .tipoTarea(s.getTipoTarea())
                    .codigo(s.getCodigo())
                    .nombre(s.getNombre())
                    .orden(s.getOrden())
                    .activo(true)
                    .build());

            s.getDocumentos().forEach(d -> documentoRepository.save(DocumentoRequerido.builder()
                    .seccion(copia)
                    .codigo(d.getCodigo())
                    .nombre(d.getNombre())
                    .obligatorio(d.isObligatorio())
                    .orden(d.getOrden())
                    .activo(true)
                    .build()));
        }
        return origen.size();
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private Provincia provinciaDelRevisorActual() {
        Profesional actual = securityUtils.getProfesionalActual();
        return rolRevisorRepository.findByProfesionalId(actual.getId())
                .orElseThrow(() -> new ForbiddenException(
                        "Solo un revisor puede administrar la estructura documental"))
                .getProvincia();
    }

    private void assertRevisorDeProvincia(Provincia provinciaRecurso) {
        Provincia delRevisor = provinciaDelRevisorActual();
        if (!delRevisor.getId().equals(provinciaRecurso.getId())) {
            throw new ForbiddenException(
                    "No podés modificar la estructura documental de otra provincia");
        }
    }

    private TipoTarea buscarTipoTarea(Long id) {
        return tipoTareaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de tarea no encontrado: " + id));
    }

    private Seccion buscarSeccion(Long id) {
        return seccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sección no encontrada: " + id));
    }

    private DocumentoRequerido buscarDocumento(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento requerido no encontrado: " + id));
    }
}
