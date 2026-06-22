package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.config.StorageProperties;
import ar.edu.utn.frc.previsar.exception.StorageException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path base;

    public FileStorageService(StorageProperties props) {
        this.base = Paths.get(props.basePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new StorageException("No se pudo inicializar el almacenamiento", e);
        }
    }

    /** Guarda el archivo y devuelve la ruta relativa a la base (lo que va en BD). */
    public String guardar(MultipartFile archivo, Long expedienteId) {
        if (archivo == null || archivo.isEmpty()) throw new StorageException("Archivo vacío", null);

        String relativa = expedienteId + "/" + UUID.randomUUID() + extension(archivo.getOriginalFilename());
        Path destino = base.resolve(relativa).normalize();
        if (!destino.startsWith(base)) throw new StorageException("Ruta inválida", null); // anti traversal

        try {
            Files.createDirectories(destino.getParent());
            archivo.transferTo(destino);
        } catch (IOException e) {
            throw new StorageException("No se pudo guardar el archivo", e);
        }
        return relativa;
    }

    public Resource cargar(String rutaRelativa) {
        Path archivo = base.resolve(rutaRelativa).normalize();
        if (!archivo.startsWith(base)) throw new StorageException("Ruta inválida", null);
        try {
            Resource r = new UrlResource(archivo.toUri());
            if (!r.exists() || !r.isReadable()) throw new StorageException("Archivo no encontrado", null);
            return r;
        } catch (MalformedURLException e) {
            throw new StorageException("Ruta inválida", e);
        }
    }

    public void eliminar(String rutaRelativa) {
        try {
            Files.deleteIfExists(base.resolve(rutaRelativa).normalize());
        } catch (IOException e) {
            // log y seguir: si el archivo físico ya no está, la baja lógica igual procede
        }
    }

    private static String extension(String nombre) {
        if (nombre == null) return "";
        int i = nombre.lastIndexOf('.');
        return i >= 0 ? nombre.substring(i).toLowerCase() : "";
    }
}
