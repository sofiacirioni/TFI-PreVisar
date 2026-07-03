package ar.edu.utn.frc.previsar.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Hashing de contenido de archivos. Compartido por las validaciones (nivel 1 y nivel 3). */
public final class HashUtil {
    private HashUtil() {}

    /** SHA-256 del contenido, en hexadecimal minúscula. */
    public static String sha256(byte[] contenido) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(contenido);
            StringBuilder sb = new StringBuilder();
            for (byte x : h) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
