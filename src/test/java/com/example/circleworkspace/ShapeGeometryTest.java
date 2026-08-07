package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class ShapeGeometryTest {
    @Test void circleBoundaryIsConstant() {
        var circle = new CircleState(1, 0, 0, 42, 0, false, 0, null);
        assertEquals(42, circle.boundaryRadius(0), 1e-12);
        assertEquals(42, circle.boundaryRadius(2.345), 1e-12);
        assertEquals(42, circle.boundaryProgression(1.2), 1e-12);
    }

    @Test void radialInterpolationWrapsAndIsDeterministic() {
        var radial = new RadialShapeState(1, 0, 0, 10, 0, false,
                new RotationRate(0, RotationRateUnit.DEGREES_PER_TICK), null, 12,
                java.util.List.of(1.0, 2.0, 1.5, .5));
        assertEquals(radial.boundaryRadius(0), radial.boundaryRadius(Math.TAU), 1e-12);
        assertEquals(7.5, radial.boundaryRadius(Math.TAU - Math.TAU / 8), 1e-9);
        assertEquals(radial.boundaryRadius(.73), radial.boundaryRadius(.73), 0);
        assertTrue(Double.isFinite(radial.boundaryProgression(.73)));
        assertTrue(radial.boundaryProgression(.73) > 0);
    }

    @Test void individualRadiiReplaceTheProfileAndSampleCount() {
        var radial = RadialShapeState.harmonic(1, 0, 0, 10, 6, .2)
                .withRadialRadii(java.util.List.of(8.0, 9.0, 10.0, 11.0, 12.0));
        assertEquals(5, radial.radialSampleCount());
        assertEquals(java.util.List.of(8.0, 9.0, 10.0, 11.0, 12.0),
                radial.radialRadii());
    }

    @Test void invalidProfilesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new RadialShapeState(1, 0, 0, 10, 0, false,
                        new RotationRate(0, RotationRateUnit.DEGREES_PER_TICK), null, 12,
                        java.util.List.of(1.0, 1.0)));
        assertThrows(IllegalArgumentException.class, () ->
                new RadialShapeState(1, 0, 0, 10, 0, false,
                        new RotationRate(0, RotationRateUnit.DEGREES_PER_TICK), null, 12,
                        java.util.List.of(1.0, 0.0, 1.0)));
    }

    @Test void largeSupportedProfileWorks() {
        var radial = RadialShapeState.harmonic(1, 0, 0, 10, 1024, .2);
        assertEquals(1024, radial.radialSampleCount());
        assertTrue(Double.isFinite(radial.boundaryLength()));
    }
}
