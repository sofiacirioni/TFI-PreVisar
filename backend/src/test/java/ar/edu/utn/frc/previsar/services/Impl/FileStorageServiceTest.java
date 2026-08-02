package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.config.StorageProperties;
import ar.edu.utn.frc.previsar.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del almacenamiento de archivos en disco.
 *
 * Se usa una carpeta temporal real (@TempDir) en vez de mockear el filesystem:
 * lo que interesa verificar es justamente que los archivos no se escriban ni se
 * lean fuera de la carpeta base (path traversal).
 */
class FileStorageServiceTest {

    @TempDir
    Path base;

    private FileStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new FileStorageService(new StorageProperties(base.toString()));
    }

    private MockMultipartFile pdf(String nombre) {
        return new MockMultipartFile("archivo", nombre, "application/pdf", "%PDF-1.4 contenido".getBytes());
    }

    @Test
    @DisplayName("Guarda bajo la carpeta del expediente y devuelve la ruta relativa")
    void guardaBajoLaCarpetaDelExpediente() {
        String ruta = storage.guardar(pdf("contrato.pdf"), 500L);

        assertThat(ruta).startsWith("500/").endsWith(".pdf");
        assertThat(base.resolve(ruta)).exists();
    }

    @Test
    @DisplayName("El nombre en disco es un UUID: dos archivos con el mismo nombre no se pisan")
    void nombresEnDiscoNoColisionan() {
        String primera = storage.guardar(pdf("contrato.pdf"), 500L);
        String segunda = storage.guardar(pdf("contrato.pdf"), 500L);

        assertThat(primera).isNotEqualTo(segunda);
        assertThat(base.resolve(primera)).exists();
        assertThat(base.resolve(segunda)).exists();
    }

    @Test
    @DisplayName("Conserva la extensión original, en minúscula")
    void conservaLaExtension() {
        assertThat(storage.guardar(pdf("PLANO.PDF"), 500L)).endsWith(".pdf");
        assertThat(storage.guardar(
                new MockMultipartFile("archivo", "foto.JPG", "image/jpeg", "x".getBytes()), 500L))
                .endsWith(".jpg");
    }

    @Test
    @DisplayName("Un archivo vacío no se guarda")
    void rechazaArchivoVacio() {
        MockMultipartFile vacio =
                new MockMultipartFile("archivo", "vacio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storage.guardar(vacio, 500L))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    @DisplayName("No se puede escribir fuera de la carpeta base (path traversal)")
    void noEscribeFueraDeLaBase() {
        assertThatThrownBy(() -> storage.guardar(pdf("contrato.pdf"), "../../etc"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Ruta inválida");
    }

    @Test
    @DisplayName("No se puede leer fuera de la carpeta base (path traversal)")
    void noLeeFueraDeLaBase() {
        assertThatThrownBy(() -> storage.cargar("../../etc/passwd"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Ruta inválida");
    }

    @Test
    @DisplayName("Leer los bytes devuelve el contenido tal cual se guardó")
    void leeElContenidoGuardado() {
        String ruta = storage.guardar(pdf("contrato.pdf"), 500L);

        assertThat(storage.leerBytes(ruta)).isEqualTo("%PDF-1.4 contenido".getBytes());
    }

    @Test
    @DisplayName("Leer un archivo que no existe falla de forma explícita")
    void leerInexistenteFalla() {
        assertThatThrownBy(() -> storage.leerBytes("500/no-existe.pdf"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Archivo no encontrado");
    }

    @Test
    @DisplayName("Eliminar borra el archivo del disco")
    void eliminaElArchivo() {
        String ruta = storage.guardar(pdf("contrato.pdf"), 500L);

        storage.eliminar(ruta);

        assertThat(base.resolve(ruta)).doesNotExist();
    }

    @Test
    @DisplayName("Eliminar algo que ya no está no explota: la baja lógica igual tiene que proceder")
    void eliminarInexistenteNoFalla() {
        storage.eliminar("500/no-existe.pdf");   // no debe lanzar
    }

    @Test
    @DisplayName("La carpeta base se crea sola si no existía")
    void creaLaCarpetaBase() {
        Path nueva = base.resolve("subcarpeta/nueva");

        new FileStorageService(new StorageProperties(nueva.toString()));

        assertThat(Files.isDirectory(nueva)).isTrue();
    }
}
