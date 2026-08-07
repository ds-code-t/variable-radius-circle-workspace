package com.example.circleworkspace;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceStoreCompatibilityTest {
    @Test void legacyCircleLoadsInDegreeMode() throws Exception {
        String json = """
                {
                  "circles":[{"id":1,"x":2,"y":3,"radius":40,"startAngleDeg":5,
                    "powered":true,"ownRateDegPerTick":7,"slaveContactId":null}],
                  "contacts":[],"tick":4,"ticksPerSecond":2
                }
                """;
        var path = Files.createTempFile("legacy-workspace", ".json");
        Files.writeString(path, json);
        var loaded = new WorkspaceStore().load(path);
        var circle = (CircleState) loaded.shapes().getFirst();
        assertEquals(RotationRateUnit.DEGREES_PER_TICK, circle.rotationRate().unit());
        assertEquals(7, circle.ownRateDegPerTick(), 1e-12);
        assertEquals(DivisionDistribution.EQUAL_LENGTH, circle.divisionDistribution());
        assertTrue(circle.divisions() >= 2);
    }

    @Test void mixedWorkspaceAndRateModesRoundTrip() throws Exception {
        var circle = new CircleState(1, 0, 0, 40, 0, true,
                new RotationRate(1.5, RotationRateUnit.DIVISIONS_PER_TICK), null, 24);
        var radial = RadialShapeState.harmonic(2, 80, 0, 35, 12, .2).withSlave(1);
        var contact = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270,
                ContactFollowMode.NO_OVERLAP_SWITCH_CONTACT, 0);
        var data = new WorkspaceData(List.of(circle, radial), List.of(contact), 7, 3);

        var path = Files.createTempFile("mixed-workspace", ".json");
        var store = new WorkspaceStore();
        store.save(path, data);
        var loaded = store.load(path);

        assertEquals(data, loaded);
        assertEquals(RotationRateUnit.DIVISIONS_PER_TICK,
                loaded.shapes().getFirst().rotationRate().unit());
    }

    @Test void versionThreeLineCountMigratesToCircumferenceUnits() throws Exception {
        String json = """
                {"version":3,"shapes":[{"shapeType":"CIRCLE","id":1,"x":0,"y":0,
                "radius":10,"startAngleDeg":0,"powered":false,"rotationRate":0,
                "rotationRateUnit":"DEGREES_PER_TICK","divisions":6,
                "divisionDistribution":"EQUAL_LENGTH"}],"contacts":[],
                "tick":0,"ticksPerSecond":2,"globalMarkerLength":10}
                """;
        var path = Files.createTempFile("v3-workspace", ".json");
        Files.writeString(path, json);

        var shape = new WorkspaceStore().load(path).shapes().getFirst();

        assertEquals(7, shape.divisions());
        assertEquals(6, shape.lineDivisionCount());
    }

    @Test void unknownShapeTypeFails() throws Exception {
        String json = """
                {"version":2,"shapes":[{"shapeType":"ALIEN","id":1,"x":0,"y":0,
                "radius":10,"startAngleDeg":0,"powered":false,"rotationRate":0,
                "rotationRateUnit":"DEGREES_PER_TICK","divisions":12}],"contacts":[],
                "tick":0,"ticksPerSecond":2}
                """;
        var path = Files.createTempFile("bad-workspace", ".json");
        Files.writeString(path, json);
        assertThrows(java.io.IOException.class, () -> new WorkspaceStore().load(path));
    }
}
