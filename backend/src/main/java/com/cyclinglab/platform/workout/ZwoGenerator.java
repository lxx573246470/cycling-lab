package com.cyclinglab.platform.workout;

import com.cyclinglab.platform.library.structure.WorkoutStructure;
import com.cyclinglab.platform.library.structure.WorkoutStructure.Block;
import com.cyclinglab.platform.library.structure.WorkoutStructure.CooldownBlock;
import com.cyclinglab.platform.library.structure.WorkoutStructure.IntervalsBlock;
import com.cyclinglab.platform.library.structure.WorkoutStructure.RestBlock;
import com.cyclinglab.platform.library.structure.WorkoutStructure.SteadyBlock;
import com.cyclinglab.platform.library.structure.WorkoutStructure.WarmupBlock;
import java.util.Locale;
import java.util.List;

/**
 * Pure function that turns a {@link WorkoutStructure.Document} into a
 * Zwift-compatible .zwo XML string. Output is deterministic and
 * hand-formatted with newlines + two-space indent so the file is diffable
 * and matches the existing {@code workouts/zwo/*.zwo} style.
 *
 * <p>Block mapping:
 * <ul>
 *   <li>{@code warmup}     -&gt; {@code <Warmup PowerLow PowerHigh>} </li>
 *   <li>{@code steady}     -&gt; {@code <SteadyState Power>} </li>
 *   <li>{@code cooldown}   -&gt; {@code <Cooldown PowerLow PowerHigh>} </li>
 *   <li>{@code rest}       -&gt; {@code <FreeRide>} (ZWO has no native rest) </li>
 *   <li>{@code intervals}  -&gt; {@code <IntervalsT Repeat OnDuration OffDuration OnPower OffPower>} </li>
 * </ul>
 *
 * <p>Power values are kept as fractions of FTP (0..1.5) just like the input.
 * Zwift and TrainingPeaks both understand fractional FTP out of the box when
 * the file is loaded as a "workout" (not an ERG file).
 */
public final class ZwoGenerator {

    private static final Locale ROOT = Locale.ROOT;

    private ZwoGenerator() {}

    public record Header(
        String name,
        String author,
        String description,
        String sportType,
        List<String> tags
    ) {}

    public static String generate(WorkoutStructure.Document doc, Header header) {
        if (doc == null || doc.blocks() == null || doc.blocks().isEmpty()) {
            throw new IllegalArgumentException("structure has no blocks");
        }
        StringBuilder sb = new StringBuilder(512);
        sb.append("<workout_file>\n");
        sb.append("  <author>").append(escape(safe(header.author(), "cycling-lab"))).append("</author>\n");
        sb.append("  <name>").append(escape(safe(header.name(), "Workout"))).append("</name>\n");
        if (header.description() != null && !header.description().isBlank()) {
            sb.append("  <description>").append(escape(header.description().trim())).append("</description>\n");
        }
        sb.append("  <sportType>").append(escape(safe(header.sportType(), "bike"))).append("</sportType>\n");
        List<String> tags = header.tags() == null ? List.of() : header.tags();
        if (!tags.isEmpty()) {
            sb.append("  <tags>\n");
            for (String t : tags) {
                if (t == null) continue;
                String s = t.trim();
                if (s.isEmpty()) continue;
                sb.append("    <tag name=\"").append(escapeAttr(s)).append("\"/>\n");
            }
            sb.append("  </tags>\n");
        }
        sb.append("  <workout>\n");
        for (Block b : doc.blocks()) {
            render(b, sb);
        }
        sb.append("  </workout>\n");
        sb.append("</workout_file>\n");
        return sb.toString();
    }

    private static void render(Block b, StringBuilder sb) {
        if (b instanceof WarmupBlock w) {
            sb.append("    <Warmup Duration=\"")
              .append(w.durationSec())
              .append("\" PowerLow=\"").append(fmt(w.powerLow()))
              .append("\" PowerHigh=\"").append(fmt(w.powerHigh()))
              .append("\"/>\n");
        } else if (b instanceof SteadyBlock s) {
            sb.append("    <SteadyState Duration=\"")
              .append(s.durationSec())
              .append("\" Power=\"").append(fmt(s.power()))
              .append("\"/>\n");
        } else if (b instanceof CooldownBlock c) {
            sb.append("    <Cooldown Duration=\"")
              .append(c.durationSec())
              .append("\" PowerLow=\"").append(fmt(c.powerLow()))
              .append("\" PowerHigh=\"").append(fmt(c.powerHigh()))
              .append("\"/>\n");
        } else if (b instanceof RestBlock r) {
            // Zwift has no native rest element; FreeRide with no power is the
            // standard way to express "ride at your own pace for N seconds".
            sb.append("    <FreeRide Duration=\"").append(r.durationSec()).append("\"/>\n");
        } else if (b instanceof IntervalsBlock i) {
            sb.append("    <IntervalsT Repeat=\"")
              .append(i.repeats())
              .append("\" OnDuration=\"").append(i.on().durationSec())
              .append("\" OffDuration=\"").append(i.off().durationSec())
              .append("\" OnPower=\"").append(fmt(i.on().power()))
              .append("\" OffPower=\"").append(fmt(i.off().power()))
              .append("\"/>\n");
        } else {
            throw new IllegalArgumentException("Unknown block type: " + b.getClass().getSimpleName());
        }
    }

    private static String fmt(double power) {
        // 2 decimal places, half-up rounding, never strip trailing 0 (Zwift
        // happily accepts "0.65" but some parsers choke on "0.6").
        return String.format(ROOT, "%.2f", power);
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s.trim();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeAttr(String s) {
        return escape(s).replace("\"", "&quot;");
    }
}