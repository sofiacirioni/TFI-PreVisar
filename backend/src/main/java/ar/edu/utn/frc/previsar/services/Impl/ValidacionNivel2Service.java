package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.DocumentoValidadoDto;
import ar.edu.utn.frc.previsar.dtos.ObservacionDto;
import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;
import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;
import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import ar.edu.utn.frc.previsar.enums.NivelObservacion;
import ar.edu.utn.frc.previsar.repositories.DocumentoCargadoRepository;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import ar.edu.utn.frc.previsar.services.ValidadorExpediente;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ValidacionNivel2Service implements ValidadorExpediente {
    /** Tolerancia de coherencia de honorarios: 5% respecto del referencial. */
    private static final BigDecimal TOLERANCIA = new BigDecimal("0.05");

    /** Montos en formato AR (miles ".", decimales ","): 1.500.000,00 — o números planos. */
    private static final Pattern MONTO =
            Pattern.compile("\\d{1,3}(?:\\.\\d{3})+(?:,\\d+)?|\\d+(?:,\\d+)?");

    private final ExpedienteService expedienteService;
    private final DocumentoCargadoRepository documentoCargadoRepository;
    private final FileStorageService storage;

    @Override
    @Transactional(readOnly = true)
    public ValidacionResultadoDto validar(Long expedienteId) {
        ExpedienteResponseDto exp = expedienteService.obtener(expedienteId);
        String cuit = digitos(exp.getComitenteDniCuit());
        BigDecimal referencial = exp.getHonorariosReferenciales();

        // Coherencia por documento. El dígito verificador del CUIT ya se valida al dar de
        // alta el comitente (ver ComitenteServiceImpl), así que acá solo se cruzan datos
        // del expediente contra el contenido de los PDFs subidos.
        List<DocumentoValidadoDto> docs = new ArrayList<>();
        for (DocumentoCargado d : documentoCargadoRepository.findByExpedienteIdAndActivoTrue(expedienteId)) {
            String codigo = d.getDocumentoRequerido().getCodigo();
            boolean esContrato = "CONTRATO_LOCACION".equals(codigo);
            boolean esPlanilla = "PLANILLA_HONORARIOS".equals(codigo);
            // Solo contrato y planilla tienen reglas de coherencia, y solo si son PDF con texto.
            if ((!esContrato && !esPlanilla) || !"application/pdf".equals(d.getTipoMime())) continue;

            List<ObservacionDto> obs = new ArrayList<>();
            String texto = leerTexto(d);
            if (texto == null) {
                obs.add(new ObservacionDto("NO_LEGIBLE", NivelObservacion.INFO,
                        "No se pudo leer el documento para verificar coherencia."));
            } else if (texto.isBlank()) {
                obs.add(new ObservacionDto("SIN_TEXTO", NivelObservacion.INFO,
                        "No se pudo verificar el contenido automáticamente (el documento parece escaneado)."));
            } else {
                if (esContrato) obs.addAll(verificarCuit(texto, cuit));
                obs.addAll(verificarHonorarios(texto, referencial));
            }
            if (!obs.isEmpty())
                docs.add(new DocumentoValidadoDto(d.getId(), d.getDocumentoRequerido().getId(),
                        d.getNombreOriginal(), obs));
        }
        return new ValidacionResultadoDto(docs, List.of());
    }

    @Override
    public int nivel() {
        return 2;
    }

    // Reglas puras (package-private para poder testearlas sin cargar PDFs reales).

    /** El CUIT/DNI del comitente debe aparecer en el contrato. */
    List<ObservacionDto> verificarCuit(String texto, String cuit) {
        if (cuit.isBlank() || digitos(texto).contains(cuit)) return List.of();
        return List.of(new ObservacionDto("CUIT_NO_COINCIDE", NivelObservacion.ADVERTENCIA,
                "El CUIT del comitente del expediente no aparece en el contrato subido."));
    }

    /** El honorario referencial debe aproximarse (±5%) a algún monto del documento. */
    List<ObservacionDto> verificarHonorarios(String texto, BigDecimal referencial) {
        if (referencial == null || referencial.signum() <= 0) return List.of();

        List<BigDecimal> montos = extraerMontos(texto);
        if (montos.isEmpty())
            return List.of(new ObservacionDto("SIN_MONTOS", NivelObservacion.INFO,
                    "No se encontraron montos en el documento para comparar con el honorario referencial."));

        boolean coincide = montos.stream().anyMatch(m -> dentroDeTolerancia(m, referencial));
        if (!coincide)
            return List.of(new ObservacionDto("HONORARIOS_NO_COINCIDEN", NivelObservacion.ADVERTENCIA,
                    "El honorario referencial registrado difiere en más del 5% de los montos del documento."));
        return List.of();
    }

    /** |monto - referencial| <= 5% del referencial (sin división, para evitar redondeos). */
    private static boolean dentroDeTolerancia(BigDecimal monto, BigDecimal referencial) {
        BigDecimal diferencia = monto.subtract(referencial).abs();
        return diferencia.compareTo(referencial.multiply(TOLERANCIA)) <= 0;
    }

    /** Texto del PDF; cadena vacía si está escaneado, null si no se pudo leer. */
    private String leerTexto(DocumentoCargado d) {
        try (PDDocument pdf = Loader.loadPDF(storage.leerBytes(d.getRutaRelativa()))) {
            return new PDFTextStripper().getText(pdf);
        } catch (IOException e) {
            return null;
        }
    }

    static List<BigDecimal> extraerMontos(String texto) {
        List<BigDecimal> montos = new ArrayList<>();
        Matcher m = MONTO.matcher(texto);
        while (m.find()) {
            String normalizado = m.group().replace(".", "").replace(",", ".");
            try {
                montos.add(new BigDecimal(normalizado));
            } catch (NumberFormatException ignore) { /* token no parseable, se ignora */ }
        }
        return montos;
    }

    /** Todos los dígitos de la cadena (no valida longitud, a diferencia de CuitValidator.soloDigitos). */
    private static String digitos(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }
}
