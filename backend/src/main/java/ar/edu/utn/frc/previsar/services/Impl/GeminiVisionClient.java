package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.config.GeminiProperties;
import ar.edu.utn.frc.previsar.exception.GeminiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Service
public class GeminiVisionClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Client client;
    private final String model;
    private final int timeout;

    public GeminiVisionClient(Client client, GeminiProperties props) {
        this.client = client; this.model = props.model(); this.timeout = props.timeoutSegundos();
    }

    /** Envía una imagen + prompt y devuelve el JSON parseado. El schema concreto lo define SCRUM-165. */
    public JsonNode analizar(byte[] imagenPng, String prompt) {
        Content contenido = Content.fromParts(
                Part.fromText(prompt),
                Part.fromBytes(imagenPng, "image/png"));
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .build();
        try {
            GenerateContentResponse resp = CompletableFuture
                    .supplyAsync(() -> client.models.generateContent(model, contenido, config))
                    .orTimeout(timeout, TimeUnit.SECONDS)
                    .join();
            return MAPPER.readTree(resp.text());
        } catch (JsonProcessingException e) {
            throw new GeminiException("Gemini devolvió un JSON inválido", e);
        } catch (CompletionException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            throw new GeminiException("Fallo al consultar Gemini (o timeout)", causa);
        }
    }
}
