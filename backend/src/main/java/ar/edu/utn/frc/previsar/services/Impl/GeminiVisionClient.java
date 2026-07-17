package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.config.GeminiProperties;
import ar.edu.utn.frc.previsar.exception.GeminiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class GeminiVisionClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long BACKOFF_BASE_MS = 2000;
    /** Códigos HTTP que vale la pena reintentar: sobrecarga (503), rate limit (429), 5xx de gateway. */
    private static final Set<Integer> TRANSITORIOS = Set.of(429, 500, 502, 503, 504);

    private final Client client;
    private final String model;
    private final int timeout;
    private final int reintentos;

    public GeminiVisionClient(Client client, GeminiProperties props) {
        this.client = client;
        this.model = props.model();
        this.timeout = props.timeoutSegundos();
        this.reintentos = props.reintentos();
    }

    /**
     * Envía una imagen + prompt y devuelve el JSON parseado. Reintenta ante errores
     * transitorios de Gemini (503 sobrecarga, 429 cuota, timeout) con backoff exponencial.
     */
    public JsonNode analizar(byte[] imagenPng, String prompt) {
        Content contenido = Content.fromParts(
                Part.fromText(prompt),
                Part.fromBytes(imagenPng, "image/png"));
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .build();

        GeminiException ultimo = null;
        for (int intento = 1; intento <= reintentos; intento++) {
            try {
                GenerateContentResponse resp = CompletableFuture
                        .supplyAsync(() -> client.models.generateContent(model, contenido, config))
                        .orTimeout(timeout, TimeUnit.SECONDS)
                        .join();
                return MAPPER.readTree(resp.text());
            } catch (JsonProcessingException e) {
                throw new GeminiException("Gemini devolvió un JSON inválido", e);   // no es transitorio
            } catch (CompletionException e) {
                Throwable causa = e.getCause() != null ? e.getCause() : e;
                ultimo = new GeminiException(descripcion(causa), causa);
                if (!esTransitorio(causa) || intento == reintentos) throw ultimo;
                long espera = backoffMs(intento);
                log.warn("Gemini: intento {}/{} falló ({}); reintento en {} ms",
                        intento, reintentos, descripcion(causa), espera);
                dormir(espera);
            }
        }
        // Inalcanzable con reintentos >= 1 (el bucle siempre retorna o lanza), pero defensivo.
        throw new GeminiException("No se pudo consultar Gemini tras " + reintentos + " intentos", ultimo);
    }

    /** Reintentable: timeout o respuesta HTTP transitoria (5xx / 429) en cualquier punto de la cadena. */
    private boolean esTransitorio(Throwable causa) {
        for (Throwable t = causa; t != null; t = t.getCause()) {
            if (t instanceof TimeoutException) return true;
            if (t instanceof ApiException api) return TRANSITORIOS.contains(api.code());
        }
        return false;
    }

    /** Mensaje corto y legible (va a los logs y a la observación de error que ve el usuario). */
    private String descripcion(Throwable causa) {
        for (Throwable t = causa; t != null; t = t.getCause()) {
            if (t instanceof TimeoutException) return "Gemini no respondió a tiempo (timeout de " + timeout + "s)";
            if (t instanceof ApiException api) return "Gemini respondió " + api.code() + " (" + api.status() + ")";
        }
        return causa.getMessage() != null ? causa.getMessage() : causa.getClass().getSimpleName();
    }

    private long backoffMs(int intento) {
        return BACKOFF_BASE_MS * (1L << (intento - 1));   // 2s, 4s, 8s…
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new GeminiException("Análisis interrumpido durante el backoff", ie);
        }
    }
}
