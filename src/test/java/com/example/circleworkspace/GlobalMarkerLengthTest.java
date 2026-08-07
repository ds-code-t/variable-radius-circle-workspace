package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class GlobalMarkerLengthTest {
    @Test void sevenCircumferenceUnitsProduceSixLineDivisions() {
        var circle = new CircleState(1, 0, 0, 10, 0, false,
                new RotationRate(1, RotationRateUnit.DIVISIONS_PER_TICK),
                null, 7, DivisionDistribution.EQUAL_LENGTH);

        assertEquals(6, circle.lineDivisionCount());
        assertEquals(60, circle.scaledToBoundaryLength(
                circle.lineDivisionCount() * 10).boundaryLength(), 1e-9);
        assertEquals(60, circle.ownRateDegForTick(0), 1e-12);
    }

    @Test void equalLengthCircleScalesToUniversalMarkerLength() {
        WorkspaceShape circle = new CircleState(1, 0, 0, 10, 0, false,
                new RotationRate(0, RotationRateUnit.DEGREES_PER_TICK),
                null, 21, DivisionDistribution.EQUAL_LENGTH);

        WorkspaceShape scaled = circle.scaledToBoundaryLength(circle.lineDivisionCount() * 7.5);

        assertEquals(150, scaled.boundaryLength(), 1e-9);
        assertEquals(150 / Math.TAU, scaled.radius(), 1e-9);
    }

    @Test void changingDivisionCountScalesAllRadialRadiiProportionally() {
        var radial = new RadialShapeState(1, 0, 0, 10, 0, false,
                new RotationRate(0, RotationRateUnit.DEGREES_PER_TICK),
                null, 11, DivisionDistribution.EQUAL_LENGTH,
                List.of(1.0, 2.0, 1.5, .5));
        var initialRatios = radial.radialRadii().stream()
                .map(value -> value / radial.radialRadii().getFirst()).toList();

        var resized = (RadialShapeState) radial.withDivisions(21)
                .scaledToBoundaryLength(20 * 5.0);
        var resizedRatios = resized.radialRadii().stream()
                .map(value -> value / resized.radialRadii().getFirst()).toList();

        assertEquals(100, resized.boundaryLength(), 1e-6);
        assertEquals(initialRatios, resizedRatios);
    }

    @Test void workspacePersistsGlobalMarkerLength() throws Exception {
        var workspace = new WorkspaceData(List.of(), List.of(), 0, 2, 6.25);
        var path = java.nio.file.Files.createTempFile("marker-length", ".json");
        var store = new WorkspaceStore();

        store.save(path, workspace);
        var loaded = store.load(path);

        assertEquals(6.25, loaded.globalMarkerLength(), 1e-12);
    }
}
