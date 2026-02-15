package free.svoss.rpg.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import free.svoss.rpg.models.AppConfig;
import free.svoss.rpg.models.CharacterResponse;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OllamaClient {
    private final String BASE_URL = "http://localhost:11434/api";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AppConfig config;

    public OllamaClient(AppConfig config) {
        this.config = config;
    }

    /**
     * Sends a prompt to the LLM with dynamic schema loading.
     */
    public CharacterResponse chat(String systemPrompt, String userPrompt) throws Exception {
        // 1. Resolve JSON Schema (External vs Internal)
        JsonNode schemaNode;
        if (config.schemaPath != null && new File(config.schemaPath).exists()) {
            String schemaContent = Files.readString(Paths.get(config.schemaPath));
            schemaNode = mapper.readTree(schemaContent);
        } else {
            try (InputStream is = getClass().getResourceAsStream("/schema.json")) {
                if (is == null) throw new RuntimeException("Default schema.json not found in jar!");
                schemaNode = mapper.readTree(is);
            }
        }

        // 2. Build Request Body
        ObjectNode root = mapper.createObjectNode();
        root.put("model", config.modelName);
        root.set("format", schemaNode);
        root.put("stream", false);

        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        // 3. Send Request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama error: " + response.body());
        }

        // 4. Parse Response
        JsonNode responseJson = mapper.readTree(response.body());
        String content = responseJson.get("message").get("content").asText();

        return mapper.readValue(content, CharacterResponse.class);
    }

    public List<String> getInstalledModels() throws Exception {
        List<String> modelNames = new ArrayList<>();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tags"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(response.body());
        JsonNode models = root.get("models");

        if (models != null && models.isArray()) {
            for (JsonNode model : models) {
                modelNames.add(model.get("name").asText());
            }
        }
        return modelNames;
    }

    /**
     * Generates vectors for RAG using nomic-embed-text.
     */
    public float[] getEmbedding(String text) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", "nomic-embed-text");
        root.put("prompt", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/embeddings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = mapper.readTree(response.body());

        if (!node.has("embedding")) {
            throw new RuntimeException("Embedding failed: " + response.body());
        }

        return mapper.convertValue(node.get("embedding"), float[].class);
    }
}