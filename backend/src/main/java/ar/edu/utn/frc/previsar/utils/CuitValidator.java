package ar.edu.utn.frc.previsar.utils;

/**
 * Utilidad para validar CUITs/CUILs argentinos.
 * Reglas:
 *   - 11 dígitos totales (puede venir con o sin guiones)
 *   - Prefijo válido según tipo de entidad
 *   - Dígito verificador correcto (módulo 11)
 */
public class CuitValidator {
    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private CuitValidator() {}

    public enum TipoEntidad { PROFESIONAL, FISICA, JURIDICA }

    /** Devuelve los 11 dígitos puros del CUIT, o null si no son exactamente 11. */
    public static String soloDigitos(String cuit) {
        if (cuit == null) return null;
        String soloNumeros = cuit.replaceAll("\\D", "");
        return soloNumeros.length() == 11 ? soloNumeros : null;
    }

    /** Verifica el dígito verificador con módulo 11. */
    public static boolean tieneDigitoVerificadorValido(String cuit) {
        String digitos = soloDigitos(cuit);
        if (digitos == null) return false;

        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(digitos.charAt(i)) * PESOS[i];
        }
        int resto = suma % 11;
        int verificadorEsperado;
        if (resto == 0) verificadorEsperado = 0;
        else if (resto == 1) return false; // convención AFIP
        else verificadorEsperado = 11 - resto;

        int verificadorReal = Character.getNumericValue(digitos.charAt(10));
        return verificadorReal == verificadorEsperado;
    }

    /** Verifica el prefijo según el tipo de entidad. */
    public static boolean tienePrefijoValido(String cuit, TipoEntidad tipo) {
        String digitos = soloDigitos(cuit);
        if (digitos == null) return false;
        String prefijo = digitos.substring(0, 2);

        return switch (tipo) {
            case JURIDICA -> prefijo.equals("30") || prefijo.equals("33") || prefijo.equals("34");
            case PROFESIONAL, FISICA ->
                    prefijo.equals("20") || prefijo.equals("23") || prefijo.equals("27");
        };
    }

    /** Verifica coherencia entre DNI y CUIT (8 dígitos centrales del CUIT == DNI con padding). */
    public static boolean coincideConDni(String cuit, String dni) {
        String cuitDigitos = soloDigitos(cuit);
        if (cuitDigitos == null || dni == null) return false;

        String dniDigitos = dni.replaceAll("\\D", "");
        if (dniDigitos.length() < 7 || dniDigitos.length() > 8) return false;

        String dniPadded = String.format("%8s", dniDigitos).replace(' ', '0');
        String centroCuit = cuitDigitos.substring(2, 10);
        return dniPadded.equals(centroCuit);
    }
}
