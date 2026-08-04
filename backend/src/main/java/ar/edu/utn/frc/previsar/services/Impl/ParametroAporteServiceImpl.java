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
        var vigenteOpt = parametroAporteRepository.findVigente(arancel.getId(), hoy);

        // 2. Si ya se fijó un arancel HOY, se corrige ESE mismo registro en vez de crear
        //    uno nuevo: no tiene sentido generar historia intra-día, y cerrar el vigente con
        //    vigencia_hasta = ayer dejaría hasta < desde y violaría chk_parametro_vigencia
        //    (el bug que aparecía al cambiar el monto dos veces el mismo día).
        if (vigenteOpt.isPresent() && hoy.equals(vigenteOpt.get().getVigenciaDesde())) {
            ParametroAporte vigenteHoy = vigenteOpt.get();
            vigenteHoy.setValor(request.getValor());
            parametroAporteRepository.save(vigenteHoy);
            return;
        }

        // 3. El vigente empezó antes de hoy: se cierra (vigencia_hasta = ayer) y se abre uno nuevo.
        //
        // saveAndFlush y no save: el índice uq_parametro_vigente (V031) admite una
        // sola fila vigente por concepto, y el id de ParametroAporte es IDENTITY, así
        // que el persist del paso 4 dispara su INSERT de inmediato —Hibernate necesita
        // el id generado—. Con un save() normal el UPDATE que cierra esta fila queda
        // encolado hasta el commit y el INSERT llega primero: por un instante hay dos
        // filas vigentes y la base lo rechaza. El flush explícito fuerza el orden.
        vigenteOpt.ifPresent(vigente -> {
            vigente.setVigenciaHasta(hoy.minusDays(1));
            parametroAporteRepository.saveAndFlush(vigente);
        });

        // 4. Insertar el nuevo valor vigente
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
