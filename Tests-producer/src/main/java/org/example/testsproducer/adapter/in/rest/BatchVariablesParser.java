package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.exception.InvalidVariablesException;
import org.example.testsproducer.domain.model.MessageVariable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public final class BatchVariablesParser {

    private final ObjectMapper objectMapper;

    public BatchVariablesParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, MessageVariable> parse(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        JsonNode definitions;
        try {
            definitions = objectMapper.reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(json);
        } catch (Exception exception) {
            throw new InvalidVariablesException("variables doit contenir un JSON valide");
        }
        if (definitions == null || !definitions.isObject()) {
            throw new InvalidVariablesException("variables doit être un objet JSON");
        }
        Map<String, MessageVariable> variables = new LinkedHashMap<>();
        for (var entry : definitions.properties()) {
            String name = entry.getKey();
            if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                throw new InvalidVariablesException("Nom de variable invalide : " + name);
            }
            try {
                variables.put(name, parseRule(entry.getValue()));
            } catch (IllegalArgumentException exception) {
                throw new InvalidVariablesException("Variable " + name + " : " + exception.getMessage());
            }
        }
        return variables;
    }

    private MessageVariable parseRule(JsonNode rule) {
        if (!rule.isObject()) {
            throw new IllegalArgumentException("la règle doit être un objet JSON");
        }
        String type = requiredText(rule, "type");
        Set<String> allowedFields = switch (type) {
            case "string" -> Set.of("type", "length", "characters");
            case "integer" -> Set.of("type", "min", "max");
            case "choice" -> Set.of("type", "values");
            default -> throw new IllegalArgumentException("type doit être string, integer ou choice");
        };
        for (var field : rule.properties()) {
            if (!allowedFields.contains(field.getKey())) {
                throw new IllegalArgumentException("champ inconnu pour " + type + " : " + field.getKey());
            }
        }
        return switch (type) {
            case "string" -> new MessageVariable.StringVariable(
                    requiredInteger(rule, "length"), requiredText(rule, "characters"));
            case "integer" -> new MessageVariable.IntegerVariable(
                    requiredInteger(rule, "min"), requiredInteger(rule, "max"));
            case "choice" -> choice(rule);
            default -> throw new IllegalArgumentException("Type de variable invalide");
        };
    }

    private MessageVariable choice(JsonNode rule) {
        JsonNode values = rule.get("values");
        if (values == null || !values.isArray()) {
            throw new IllegalArgumentException("values doit être une liste de chaînes de caractères");
        }
        var choices = new ArrayList<String>();
        for (var value : values) {
            if (!value.isString()) {
                throw new IllegalArgumentException("values doit contenir uniquement des chaînes de caractères");
            }
            choices.add(value.asText());
        }
        return new MessageVariable.ChoiceVariable(choices);
    }

    private String requiredText(JsonNode rule, String field) {
        JsonNode value = rule.get(field);
        if (value == null || !value.isString()) {
            throw new IllegalArgumentException(field + " doit être une chaîne de caractères");
        }
        return value.asText();
    }

    private int requiredInteger(JsonNode rule, String field) {
        JsonNode value = rule.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException(field + " doit être un entier entre -2147483648 et 2147483647");
        }
        return value.intValue();
    }
}
