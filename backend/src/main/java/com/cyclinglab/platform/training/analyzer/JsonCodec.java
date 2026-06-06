package com.cyclinglab.platform.training.analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** Tiny helper to serialise records to JSON without dragging Spring into the analyzer. */
final class JsonCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonCodec() {}

    static String toJson(Object value) {
        if (value == null) return null;
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + value, e);
        }
    }

    static <T> List<T> readList(String json, com.fasterxml.jackson.core.type.TypeReference<List<T>> ref) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, ref);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}