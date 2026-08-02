package ar.edu.utn.frc.previsar.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del hash de contenido.
 *
 * De este hash dependen dos cosas: la detección de archivos duplicados dentro de
 * un expediente (nivel 1) y el cacheo del análisis visual por contenido (nivel 3).
 */
class HashUtilTest {

    @Test
    @DisplayName("El mismo contenido siempre da el mismo hash")
    void mismoContenidoMismoHash() {
        byte[] contenido = "contenido del PDF".getBytes(StandardCharsets.UTF_8);

        assertThat(HashUtil.sha256(contenido)).isEqualTo(HashUtil.sha256(contenido));
    }

    @Test
    @DisplayName("Contenidos distintos dan hashes distintos")
    void contenidosDistintosHashesDistintos() {
        assertThat(HashUtil.sha256("archivo A".getBytes(StandardCharsets.UTF_8)))
                .isNotEqualTo(HashUtil.sha256("archivo B".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Devuelve 64 caracteres hexadecimales en minúscula (entra en el VARCHAR(64))")
    void formatoHexDe64Caracteres() {
        String hash = HashUtil.sha256("lo que sea".getBytes(StandardCharsets.UTF_8));

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("El SHA-256 del vacío es el valor conocido del algoritmo")
    void hashDelContenidoVacio() {
        assertThat(HashUtil.sha256(new byte[0]))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}
