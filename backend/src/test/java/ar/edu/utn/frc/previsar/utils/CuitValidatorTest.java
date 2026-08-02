package ar.edu.utn.frc.previsar.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del validador de CUIT, que es lo que decide si un registro entra o no.
 *
 * Los CUITs de prueba se construyeron con el dígito verificador real (módulo 11)
 * para que el test valide la cuenta y no una constante inventada.
 */
class CuitValidatorTest {

    // -------------------- soloDigitos --------------------

    @Test
    @DisplayName("Extrae los 11 dígitos venga con guiones o sin ellos")
    void extraeLosOnceDigitos() {
        assertThat(CuitValidator.soloDigitos("20-17373068-4")).isEqualTo("20173730684");
        assertThat(CuitValidator.soloDigitos("20173730684")).isEqualTo("20173730684");
    }

    @Test
    @DisplayName("Devuelve null si no son exactamente 11 dígitos")
    void rechazaLongitudDistintaDeOnce() {
        assertThat(CuitValidator.soloDigitos("2017373068")).isNull();     // 10
        assertThat(CuitValidator.soloDigitos("201737306899")).isNull();   // 12
        assertThat(CuitValidator.soloDigitos("")).isNull();
        assertThat(CuitValidator.soloDigitos(null)).isNull();
    }

    // -------------------- Dígito verificador --------------------

    @ParameterizedTest
    @ValueSource(strings = {"20-17373068-4", "27-17373068-9", "30-71234567-1"})
    @DisplayName("Acepta CUITs con dígito verificador correcto")
    void aceptaDigitoVerificadorValido(String cuit) {
        assertThat(CuitValidator.tieneDigitoVerificadorValido(cuit)).isTrue();
    }

    @Test
    @DisplayName("Rechaza un CUIT con el verificador cambiado")
    void rechazaDigitoVerificadorInvalido() {
        assertThat(CuitValidator.tieneDigitoVerificadorValido("20-17373068-0")).isFalse();
        assertThat(CuitValidator.tieneDigitoVerificadorValido("20-17373068-5")).isFalse();
    }

    @Test
    @DisplayName("Un CUIT mal formado no pasa el verificador")
    void rechazaCuitMalFormado() {
        assertThat(CuitValidator.tieneDigitoVerificadorValido("no-es-un-cuit")).isFalse();
        assertThat(CuitValidator.tieneDigitoVerificadorValido(null)).isFalse();
    }

    // -------------------- Prefijo --------------------

    @ParameterizedTest
    @ValueSource(strings = {"20-17373068-4", "23-17373068-3", "27-17373068-9"})
    @DisplayName("Los prefijos 20, 23 y 27 son de persona física")
    void prefijosDePersonaFisica(String cuit) {
        assertThat(CuitValidator.tienePrefijoValido(cuit, CuitValidator.TipoEntidad.PROFESIONAL)).isTrue();
        assertThat(CuitValidator.tienePrefijoValido(cuit, CuitValidator.TipoEntidad.FISICA)).isTrue();
    }

    @Test
    @DisplayName("Un CUIT de empresa no sirve para registrar un profesional")
    void prefijoDeEmpresaNoEsProfesional() {
        assertThat(CuitValidator.tienePrefijoValido("30-71234567-1",
                CuitValidator.TipoEntidad.PROFESIONAL)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"30-71234567-1", "33-71234567-0", "34-71234567-7"})
    @DisplayName("Los prefijos 30, 33 y 34 son de persona jurídica")
    void prefijosDePersonaJuridica(String cuit) {
        assertThat(CuitValidator.tienePrefijoValido(cuit, CuitValidator.TipoEntidad.JURIDICA)).isTrue();
    }

    // -------------------- Coherencia con el DNI --------------------

    @Test
    @DisplayName("Los 8 dígitos centrales del CUIT tienen que ser el DNI")
    void cuitCoincideConDni() {
        assertThat(CuitValidator.coincideConDni("20-17373068-4", "17373068")).isTrue();
        assertThat(CuitValidator.coincideConDni("20-17373068-4", "17.373.068")).isTrue();
    }

    @Test
    @DisplayName("Un DNI de 7 dígitos se completa con cero a la izquierda")
    void dniDeSieteDigitosSeRellena() {
        assertThat(CuitValidator.coincideConDni("27-05123456-7", "5123456")).isTrue();
    }

    @Test
    @DisplayName("Rechaza cuando el DNI no coincide con el centro del CUIT")
    void rechazaDniQueNoCoincide() {
        assertThat(CuitValidator.coincideConDni("20-17373068-4", "99999999")).isFalse();
    }

    @Test
    @DisplayName("Un DNI fuera de rango (menos de 7 u 8 dígitos) no coincide con nada")
    void rechazaDniConLongitudInvalida() {
        assertThat(CuitValidator.coincideConDni("20-17373068-4", "123")).isFalse();
        assertThat(CuitValidator.coincideConDni("20-17373068-4", "123456789")).isFalse();
        assertThat(CuitValidator.coincideConDni("20-17373068-4", null)).isFalse();
    }
}
