package com.cyclinglab.platform.library.structure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import com.cyclinglab.platform.library.exception.StructureValidationException;

/**
 * Parses and validates a {@code structure_json} payload. On failure raises
 * {@link com.cyclinglab.platform.library.exception.StructureValidationException}
 * which the global exception handler maps to HTTP 422.
 *
 * <p>Validation is hand-rolled (rather than {@code jakarta.validation}) because
 * the structure is a discriminated union and we want a JSON Pointer in the
 * error response, which the design (§15.1.3) calls out.
 */
public class StructureValidator {

    private final ObjectMapper mapper;

    public StructureValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public WorkoutStructure.Document parse(String raw) {
        try {
            JsonNode root = mapper.readTree(raw);
            List<String> errors = new ArrayList<>();
            JsonNode blocks = root.get("blocks");
            if (blocks == null || !blocks.isArray() || blocks.isEmpty()) {
                throw new IllegalArgumentException("'blocks' must be a non-empty array");
            }
            return new WorkoutStructure.Document(parseBlocks(blocks, errors));
        } catch (StructureValidationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new StructureValidationException(List.of(error("", e.getMessage())));
        } catch (Exception e) {
            throw new StructureValidationException(List.of(error("", "structure_json is not valid JSON: " + e.getMessage())));
        }
    }

    private List<WorkoutStructure.Block> parseBlocks(JsonNode arr, List<String> errors) {
        List<WorkoutStructure.Block> out = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            JsonNode n = arr.get(i);
            String p = "/blocks/" + i;
            String type = textOrNull(n, "type");
            try {
                if (type == null) {
                    throw new IllegalArgumentException("missing 'type'");
                }
                switch (type) {
                    case "warmup" -> {
                        long dur = requireDuration(n, p);
                        double lo = requirePower(n, "powerLow", p + "/powerLow");
                        double hi = requirePower(n, "powerHigh", p + "/powerHigh");
                        if (lo >= hi) {
                            throw new IllegalArgumentException("powerLow must be < powerHigh");
                        }
                        out.add(new WorkoutStructure.WarmupBlock(type, dur, lo, hi));
                    }
                    case "steady" -> {
                        long dur = requireDuration(n, p);
                        double power = requirePower(n, "power", p + "/power");
                        out.add(new WorkoutStructure.SteadyBlock(type, dur, power));
                    }
                    case "cooldown" -> {
                        long dur = requireDuration(n, p);
                        double lo = requirePower(n, "powerLow", p + "/powerLow");
                        double hi = requirePower(n, "powerHigh", p + "/powerHigh");
                        if (lo <= hi) {
                            throw new IllegalArgumentException("cooldown powerLow must be > powerHigh");
                        }
                        out.add(new WorkoutStructure.CooldownBlock(type, dur, lo, hi));
                    }
                    case "rest" -> {
                        long dur = requireDuration(n, p);
                        out.add(new WorkoutStructure.RestBlock(type, dur));
                    }
                    case "intervals" -> {
                        int repeats = intOrDefault(n, "repeats", 1);
                        if (repeats < 1) {
                            throw new IllegalArgumentException("repeats must be >= 1");
                        }
                        JsonNode on = n.get("on");
                        JsonNode off = n.get("off");
                        if (on == null || off == null) {
                            throw new IllegalArgumentException("intervals block must have 'on' and 'off'");
                        }
                        WorkoutStructure.IntervalsBlock.Segment onSeg = parseSegment(on, p + "/on");
                        WorkoutStructure.IntervalsBlock.Segment offSeg = parseSegment(off, p + "/off");
                        out.add(new WorkoutStructure.IntervalsBlock(type, repeats, onSeg, offSeg));
                    }
                    default -> throw new IllegalArgumentException("unknown block type: " + type);
                }
            } catch (IllegalArgumentException e) {
                // The error message we throw always starts with the JSON pointer
                // of the offending field, so we just use that as the pointer.
                String msg = e.getMessage() == null ? "" : e.getMessage();
                String pointer = msg.contains(": ") ? msg.substring(0, msg.indexOf(": ")) : p;
                throw new StructureValidationException(List.of(error(pointer, msg)));
            }
        }
        return out;
    }

    private WorkoutStructure.IntervalsBlock.Segment parseSegment(JsonNode n, String p) {
        long dur = requireDuration(n, p);
        double power = requirePower(n, "power", p + "/power");
        return new WorkoutStructure.IntervalsBlock.Segment(dur, power);
    }

    private long requireDuration(JsonNode n, String p) {
        JsonNode d = n.get("durationSec");
        if (d == null || !d.canConvertToLong() || d.asLong() <= 0) {
            throw new IllegalArgumentException(p + ": durationSec must be > 0");
        }
        return d.asLong();
    }

    private double requirePower(JsonNode n, String field, String p) {
        JsonNode v = n.get(field);
        if (v == null || !v.isNumber()) {
            throw new IllegalArgumentException(p + ": " + field + " must be a number");
        }
        double d = v.asDouble();
        if (d < 0.0 || d > WorkoutStructure.MAX_POWER_FACTOR) {
            throw new IllegalArgumentException(p + ": " + field + " must be in [0, "
                + WorkoutStructure.MAX_POWER_FACTOR + "]");
        }
        return d;
    }

    private int intOrDefault(JsonNode n, String field, int def) {
        JsonNode v = n.get(field);
        if (v == null || !v.canConvertToInt()) return def;
        return v.asInt(def);
    }

    private String textOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static StructureValidationException.Detail error(String pointer, String message) {
        return new StructureValidationException.Detail(pointer, message);
    }
}
