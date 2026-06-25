package ar.edu.utn.frc.previsar.pdf;

public final class NumeroALetras {
    private NumeroALetras() {}

    private static final String[] U = {"","UNO","DOS","TRES","CUATRO","CINCO","SEIS","SIETE","OCHO","NUEVE",
            "DIEZ","ONCE","DOCE","TRECE","CATORCE","QUINCE","DIECISÉIS","DIECISIETE","DIECIOCHO","DIECINUEVE",
            "VEINTE","VEINTIUNO","VEINTIDÓS","VEINTITRÉS","VEINTICUATRO","VEINTICINCO","VEINTISÉIS",
            "VEINTISIETE","VEINTIOCHO","VEINTINUEVE"};
    private static final String[] D = {"","","","TREINTA","CUARENTA","CINCUENTA","SESENTA","SETENTA","OCHENTA","NOVENTA"};
    private static final String[] C = {"","CIENTO","DOSCIENTOS","TRESCIENTOS","CUATROCIENTOS","QUINIENTOS",
            "SEISCIENTOS","SETECIENTOS","OCHOCIENTOS","NOVECIENTOS"};

    /** Devuelve el contenido para "(SON PESOS ____)": ej. "CINCO MILLONES ... CON 04/100". */
    public static String enLetras(java.math.BigDecimal monto) {
        java.math.BigDecimal m = monto.setScale(2, java.math.RoundingMode.HALF_UP);
        long entero = m.longValue();
        int cent = m.subtract(java.math.BigDecimal.valueOf(entero)).movePointRight(2).intValueExact();
        String palabras = entero == 0 ? "CERO" : convertir(entero);
        return String.format("%s CON %02d/100", palabras, cent);
    }

    private static String apocope(String s) { // "UNO"->"UN" antes de MIL/MILLÓN
        if (s.endsWith("VEINTIUNO")) return s.substring(0, s.length()-9) + "VEINTIÚN";
        if (s.endsWith("UNO")) return s.substring(0, s.length()-3) + "UN";
        return s;
    }

    private static String convertir(long n) {
        if (n < 30) return U[(int) n];
        if (n < 100) { int d=(int)n/10, u=(int)n%10; return D[d] + (u>0 ? " Y " + U[u] : ""); }
        if (n == 100) return "CIEN";
        if (n < 1000) { int c=(int)n/100, r=(int)n%100; return C[c] + (r>0 ? " " + convertir(r) : ""); }
        if (n < 1_000_000) {
            long miles=n/1000, r=n%1000;
            String p = miles==1 ? "MIL" : apocope(convertir(miles)) + " MIL";
            return p + (r>0 ? " " + convertir(r) : "");
        }
        long mill=n/1_000_000, r=n%1_000_000;
        String p = mill==1 ? "UN MILLÓN" : apocope(convertir(mill)) + " MILLONES";
        return p + (r>0 ? " " + convertir(r) : "");
    }
}
