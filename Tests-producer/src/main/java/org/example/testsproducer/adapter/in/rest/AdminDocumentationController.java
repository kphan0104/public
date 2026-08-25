package org.example.testsproducer.adapter.in.rest;

import io.swagger.v3.oas.annotations.Hidden;
import org.example.testsproducer.config.AdminProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Hidden
@RestController
public class AdminDocumentationController {

    @GetMapping(
            path = "/admin/api-docs",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> adminApiDocs() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("openapi", "3.1.0");
        document.put("info", Map.of(
                "title", "Tests Producer Administration",
                "version", "1.0.0"
        ));
        document.put("tags", List.of(Map.of(
                "name", "Administration",
                "description", "Gestion des flux de tests"
        )));
        document.put("paths", paths());
        document.put("components", Map.of(
                "securitySchemes", Map.of(
                        "AdminToken", Map.of(
                                "type", "apiKey",
                                "in", "header",
                                "name", AdminProperties.TOKEN_HEADER
                        )
                )
        ));
        return document;
    }

    private Map<String, Object> paths() {
        Map<String, Object> paths = new LinkedHashMap<>();
        paths.put(
                "/internal/flows/{flow}",
                Map.of("put", upsertFlowOperation())
        );
        paths.put(
                "/internal/flows/{flow}/original-message",
                Map.of("put", upsertOriginalMessageOperation())
        );
        return paths;
    }

    private Map<String, Object> upsertFlowOperation() {
        Map<String, Object> operation = operation(
                "Créer un flux ou modifier son topic"
        );
        operation.put("parameters", List.of(
                pathParameter("flow", 255),
                queryParameter("topic", 249)
        ));
        operation.put("responses", Map.of(
                "200", response("Flux mis à jour"),
                "201", response("Flux créé"),
                "400", response("Requête invalide"),
                "401", response("Token absent ou invalide"),
                "500", response("Écriture impossible")
        ));
        return operation;
    }

    private Map<String, Object> upsertOriginalMessageOperation() {
        Map<String, Object> operation = operation(
                "Créer ou remplacer l'originalMessage d'un flux"
        );
        operation.put("parameters", List.of(pathParameter("flow", 255)));
        operation.put("requestBody", Map.of(
                "required", true,
                "content", Map.of(
                        MediaType.TEXT_PLAIN_VALUE,
                        Map.of("schema", Map.of("type", "string"))
                )
        ));
        operation.put("responses", Map.of(
                "200", response("originalMessage mis à jour"),
                "201", response("originalMessage créé"),
                "400", response("Requête invalide"),
                "401", response("Token absent ou invalide"),
                "404", response("Flux inconnu"),
                "413", response("Message trop volumineux"),
                "500", response("Écriture impossible")
        ));
        return operation;
    }

    private Map<String, Object> operation(String summary) {
        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("tags", List.of("Administration"));
        operation.put("summary", summary);
        operation.put("security", List.of(Map.of("AdminToken", List.of())));
        return operation;
    }

    private Map<String, Object> pathParameter(String name, int maxLength) {
        return parameter(name, "path", maxLength);
    }

    private Map<String, Object> queryParameter(String name, int maxLength) {
        return parameter(name, "query", maxLength);
    }

    private Map<String, Object> parameter(
            String name,
            String location,
            int maxLength
    ) {
        return Map.of(
                "name", name,
                "in", location,
                "required", true,
                "schema", Map.of(
                        "type", "string",
                        "minLength", 1,
                        "maxLength", maxLength,
                        "pattern", "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$"
                )
        );
    }

    private Map<String, Object> response(String description) {
        return Map.of("description", description);
    }
}
