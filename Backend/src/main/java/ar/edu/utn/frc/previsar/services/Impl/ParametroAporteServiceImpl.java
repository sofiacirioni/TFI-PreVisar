package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.request.ActualizarArancelRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ArancelResponseDto;
import ar.edu.utn.frc.previsar.entities.ConceptoAporte;
import ar.edu.utn.frc.previsar.entities.ParametroAporte;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.enums.TipoValor;
import ar.edu.utn.frc.previsar.exception.ForbiddenException;
import ar.edu.utn.frc.previsar.repositories.ConceptoAporteRepository;
import ar.edu.utn.frc.previsar.repositories.ParametroAporteRepository;
import ar.edu.utn.frc.previsar.repositories.RolRevisorRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import ar.edu.utn.frc.previsar.services.ParametroAporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ParametroAporteServiceImpl implements ParametroAporteService {
    private final ParametroAporteRepository parametroAporteRepository;
    private final ConceptoAporteRepository conceptoAporteRepository;
    private final RolRevisorRepository rolRevisorRepository;
    private final SecurityUtils securityUtils;


    @Override
    @Transactional(readOnly = true)
    public ArancelResponseDto obtenerArancelVigente() {
        ConceptoAporte arancel = obtenerConceptoArancel();
        return parametroAporteRepository.findVigente(arancel.getId(), LocalDate.now())
                .map(p -> ArancelResponseDto.builder()
                        .valor(p.getValor())
                        .vigenciaDesde(p.getVigenciaDesde())
                        .build())
                .orElseGet(() -> ArancelResponseDto.builder().build());
    }

    @Override
    @Transactional
    public void actualizarArancel(ActualizarArancelRequestDto request) {
        // 1. Autorizacion: solo revisor
        Profesional actual = securityUtils.getProfesionalActual();
        if (!rolRevisorRepository.existsByProfesionalId(actual.getId())) {
            throw new ForbiddenException("Solo un revisor puede actualizar el arancel administrativo");
        }

        ConceptoAporte arancel = obtenerConceptoArancel();

        LocalDate hoy = LocalDate.now();

        // 2. Cerrar el arancel vigente (vigencia_hasta = ayer)
        parametroAporteRepository.findVigente(arancel.getId(), hoy).ifPresent(vigente -> {
            vigente.setVigenciaHasta(hoy.minusDays(1));
            parametroAporteRepository.save(vigente);
        });

        // 3. Insertar el nuevo valor vigente
        ParametroAporte nuevo = new ParametroAporte();
        nuevo.setConcepto(arancel);
        nuevo.setTipoValor(TipoValor.FIJO);
        nuevo.setValor(request.getValor());
        nuevo.setBaseCalculo(null);          // FIJO no tiene base
        nuevo.setVigenciaDesde(hoy);
        nuevo.setVigenciaHasta(null);        // null = vigente
        nuevo.setActivo(true);
        parametroAporteRepository.save(nuevo);

    }

    private ConceptoAporte obtenerConceptoArancel() {
        return conceptoAporteRepository.findByCodigo("ARANCEL_ADMIN")
                .orElseThrow(() -> new IllegalStateException("No existe el concepto ARANCEL_ADMIN"));
    }
}
