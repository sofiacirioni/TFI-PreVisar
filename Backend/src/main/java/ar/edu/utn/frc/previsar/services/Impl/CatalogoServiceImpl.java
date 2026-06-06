package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.response.*;
import ar.edu.utn.frc.previsar.mapper.TipoTareaMapper;
import ar.edu.utn.frc.previsar.repositories.*;
import ar.edu.utn.frc.previsar.services.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoServiceImpl implements CatalogoService {
    private final ProvinciaRepository provinciaRepository;
    private final RegionalRepository regionalRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final TituloRepository tituloRepository;
    private final TipoTareaRepository tipoTareaRepository;
    private final TipoTareaMapper tipoTareaMapper;

    @Override
    public List<ProvinciaResponseDto> listarProvincias() {
        return provinciaRepository.findAll(Sort.by("nombre")).stream()
                .map(p -> ProvinciaResponseDto.builder()
                        .id(p.getId())
                        .nombre(p.getNombre())
                        .codigo(p.getCodigo())
                        .build())
                .toList();
    }

    @Override
    public List<RegionalResponseDto> listarRegionales() {
        return regionalRepository.findAll(Sort.by("nombre")).stream()
                .map(r -> RegionalResponseDto.builder()
                        .id(r.getId())
                        .nombre(r.getNombre())
                        .provinciaId(r.getProvincia().getId())
                        .provinciaNombre(r.getProvincia().getNombre())
                        .build())
                .toList();
    }

    @Override
    public List<CondicionIvaResponseDto> listarCondicionesIva() {
        return condicionIvaRepository.findAll().stream()
                .map(c -> CondicionIvaResponseDto.builder()
                        .id(c.getId())
                        .descripcion(c.getDescripcion())
                        .codigo(c.getCodigo())
                        .build())
                .toList();
    }

    @Override
    public List<TituloResponseDto> listarTitulos() {
        return tituloRepository.findAllByActivoTrueOrderByNombreAsc().stream()
                .map(t -> TituloResponseDto.builder()
                        .id(t.getId())
                        .nombre(t.getNombre())
                        .permiteTextoLibre(t.getPermiteTextoLibre())
                        .build())
                .toList();
    }

    @Override
    public List<TipoTareaResponseDto> listarTiposTarea() {
        return tipoTareaRepository.findByActivoTrueOrderByOrden()
                .stream()
                .map(tipoTareaMapper::toResponse)
                .toList();
    }
}
