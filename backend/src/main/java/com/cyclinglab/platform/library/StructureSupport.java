package com.cyclinglab.platform.library;

import com.cyclinglab.platform.library.structure.WorkoutStructure;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Small helper for converting a parsed JSON block back into the typed
 * {@link WorkoutStructure.Block} sealed-interface members. Kept out of
 * {@link WorkoutStructure} to avoid coupling it to Jackson.
 */
final class StructureSupport {

    private StructureSupport() {}

    static WorkoutStructure.Block deserializeBlock(ObjectNode n) {
        String type = n.path("type").asText();
        return switch (type) {
            case "warmup" -> new WorkoutStructure.WarmupBlock(
                type,
                n.path("durationSec").asLong(),
                n.path("powerLow").asDouble(),
                n.path("powerHigh").asDouble()
            );
            case "steady" -> new WorkoutStructure.SteadyBlock(
                type,
                n.path("durationSec").asLong(),
                n.path("power").asDouble()
            );
            case "cooldown" -> new WorkoutStructure.CooldownBlock(
                type,
                n.path("durationSec").asLong(),
                n.path("powerLow").asDouble(),
                n.path("powerHigh").asDouble()
            );
            case "rest" -> new WorkoutStructure.RestBlock(
                type,
                n.path("durationSec").asLong()
            );
            case "intervals" -> new WorkoutStructure.IntervalsBlock(
                type,
                n.path("repeats").asInt(1),
                new WorkoutStructure.IntervalsBlock.Segment(
                    n.path("on").path("durationSec").asLong(),
                    n.path("on").path("power").asDouble()
                ),
                new WorkoutStructure.IntervalsBlock.Segment(
                    n.path("off").path("durationSec").asLong(),
                    n.path("off").path("power").asDouble()
                )
            );
            default -> throw new IllegalArgumentException("Unknown block type: " + type);
        };
    }
}
