package com.cyclinglab.platform.library.structure;

import java.util.List;

/**
 * Block shapes accepted by the structure_json of a {@code workout_template}.
 * Defined in doc/ARCHITECTURE.md §7.4 and §15.1. The schema is the same one
 * that the M2 ZWO generator consumes, so keeping it strict here means M2 can
 * drop in without re-shaping the data.
 *
 * <p>Validation rules (see also §15.1.3 / §15.1.4):
 * <ul>
 *   <li>blocks list is required and non-empty.</li>
 *   <li>{@code power*} fields must lie in {@code [0, 1.5]} (1.5 = 150% FTP).</li>
 *   <li>{@code durationSec} must be {@code > 0} when set.</li>
 *   <li>{@code repeats} must be {@code >= 1}.</li>
 *   <li>type-specific fields must be present (e.g. warmup must have
 *       powerLow/powerHigh; intervals must have on/off).</li>
 * </ul>
 */
public final class WorkoutStructure {

    private WorkoutStructure() {}

    public static final double MAX_POWER_FACTOR = 1.5;

    public record Document(List<Block> blocks) {
    }

    public sealed interface Block permits WarmupBlock, SteadyBlock, IntervalsBlock, CooldownBlock, RestBlock {
        String type();
    }

    public record WarmupBlock(String type, long durationSec, double powerLow, double powerHigh)
        implements Block {
    }

    public record SteadyBlock(String type, long durationSec, double power)
        implements Block {
    }

    public record CooldownBlock(String type, long durationSec, double powerLow, double powerHigh)
        implements Block {
    }

    public record RestBlock(String type, long durationSec) implements Block {
    }

    public record IntervalsBlock(
        String type,
        int repeats,
        Segment on,
        Segment off
    ) implements Block {
        public record Segment(long durationSec, double power) {
        }
    }
}
