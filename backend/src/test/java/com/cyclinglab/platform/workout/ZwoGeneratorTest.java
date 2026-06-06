package com.cyclinglab.platform.workout;

import static org.assertj.core.api.Assertions.assertThat;

import com.cyclinglab.platform.library.structure.StructureValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZwoGeneratorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final StructureValidator validator = new StructureValidator(mapper);

    @Test
    void rendersAllFiveBlockTypes() throws Exception {
        String json = """
            {"blocks":[
              {"type":"warmup","durationSec":600,"powerLow":0.45,"powerHigh":0.62},
              {"type":"steady","durationSec":1800,"power":0.65},
              {"type":"intervals","repeats":3,"on":{"durationSec":120,"power":0.88},"off":{"durationSec":90,"power":0.55}},
              {"type":"rest","durationSec":300},
              {"type":"cooldown","durationSec":300,"powerLow":0.55,"powerHigh":0.40}
            ]}""";
        var doc = validator.parse(json);
        ZwoGenerator.Header h = new ZwoGenerator.Header(
            "Mixed", "cycling-lab", "test description", "bike", List.of("z2","sweet-spot")
        );
        String xml = ZwoGenerator.generate(doc, h);
        assertThat(xml).contains("<Warmup Duration=\"600\" PowerLow=\"0.45\" PowerHigh=\"0.62\"/>");
        assertThat(xml).contains("<SteadyState Duration=\"1800\" Power=\"0.65\"/>");
        assertThat(xml).contains("<IntervalsT Repeat=\"3\" OnDuration=\"120\" OffDuration=\"90\" OnPower=\"0.88\" OffPower=\"0.55\"/>");
        assertThat(xml).contains("<FreeRide Duration=\"300\"/>");
        assertThat(xml).contains("<Cooldown Duration=\"300\" PowerLow=\"0.55\" PowerHigh=\"0.40\"/>");
        assertThat(xml).contains("<tag name=\"z2\"/>");
        assertThat(xml).contains("<tag name=\"sweet-spot\"/>");
        assertThat(xml).contains("<author>cycling-lab</author>");
        assertThat(xml).contains("<name>Mixed</name>");
        assertThat(xml).contains("<description>test description</description>");
        assertThat(xml).endsWith("</workout_file>\n");
    }

    @Test
    void rejectsEmptyDocument() {
        var doc = new com.cyclinglab.platform.library.structure.WorkoutStructure.Document(List.of());
        ZwoGenerator.Header h = new ZwoGenerator.Header("X", null, null, "bike", List.of());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> ZwoGenerator.generate(doc, h));
    }

    @Test
    void fallsBackToDefaultsForMissingHeaderFields() throws Exception {
        String json = """
            {"blocks":[{"type":"steady","durationSec":600,"power":0.65}]}""";
        var doc = validator.parse(json);
        ZwoGenerator.Header h = new ZwoGenerator.Header(null, null, null, null, null);
        String xml = ZwoGenerator.generate(doc, h);
        assertThat(xml).contains("<name>Workout</name>");
        assertThat(xml).contains("<sportType>bike</sportType>");
        assertThat(xml).contains("<author>cycling-lab</author>");
        assertThat(xml).doesNotContain("<description>");
        assertThat(xml).doesNotContain("<tags>");
    }

    @Test
    void escapesXmlSpecialCharsInDescription() throws Exception {
        String json = """
            {"blocks":[{"type":"steady","durationSec":600,"power":0.65}]}""";
        var doc = validator.parse(json);
        ZwoGenerator.Header h = new ZwoGenerator.Header(
            "X", "cycling-lab", "<script>alert(1)</script>", "bike", List.of()
        );
        String xml = ZwoGenerator.generate(doc, h);
        assertThat(xml).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(xml).doesNotContain("<script>");
    }

    @Test
    void formatsPowerToTwoDecimalPlaces() throws Exception {
        String json = """
            {"blocks":[{"type":"steady","durationSec":600,"power":0.65}]}""";
        var doc = validator.parse(json);
        ZwoGenerator.Header h = new ZwoGenerator.Header("X", null, null, "bike", List.of());
        String xml = ZwoGenerator.generate(doc, h);
        assertThat(xml).contains("Power=\"0.65\"");
    }

    
}