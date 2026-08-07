package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class DivisionDistributionTest {
    @Test void lengthDistributionIsDefault() {
        var radial = RadialShapeState.harmonic(1, 0, 0, 10, 8, .2);
        assertEquals(DivisionDistribution.EQUAL_LENGTH, radial.divisionDistribution());
    }

    @Test void angleDistributionUsesEqualAngularSteps() {
        var radial = RadialShapeState.harmonic(1, 0, 0, 10, 8, .4)
                .withDivisions(5)
                .withDivisionDistribution(DivisionDistribution.EQUAL_ANGLE);
        assertEquals(0, radial.divisionAngleRad(0), 1e-12);
        assertEquals(Math.PI / 2, radial.divisionAngleRad(1), 1e-12);
        assertEquals(Math.PI, radial.divisionAngleRad(2), 1e-12);
    }

    @Test void lengthDistributionUsesEqualBoundaryProgress() {
        var radial = new RadialShapeState(1, 0, 0, 10, 0, false,
                new RotationRate(0, RotationRateUnit.DEGREES_PER_TICK), null, 5,
                DivisionDistribution.EQUAL_LENGTH, List.of(1.0, 3.0, 1.0, 1.0));
        double first = radial.divisionAngleRad(1);
        assertNotEquals(Math.PI / 2, first, 1e-3);
        assertTrue(first > 0 && first < Math.TAU);
    }

    @Test void divisionsPerTickFollowsSelectedDistribution() {
        var base = new RadialShapeState(1, 0, 0, 10, 0, true,
                new RotationRate(1, RotationRateUnit.DIVISIONS_PER_TICK), null, 5,
                DivisionDistribution.EQUAL_LENGTH, List.of(1.0, 3.0, 1.0, 1.0));
        double lengthAngle = base.displayedAngle(1, base.ownRateDegForTick(0));
        var angular = base.withDivisionDistribution(DivisionDistribution.EQUAL_ANGLE);
        assertEquals(90, angular.displayedAngle(1, angular.ownRateDegForTick(0)), 1e-9);
        assertNotEquals(90, lengthAngle, 1e-3);
    }

    @Test void circlesRemainEquivalentInBothModes() {
        var circle = new CircleState(1, 0, 0, 10, 0, true,
                new RotationRate(1, RotationRateUnit.DIVISIONS_PER_TICK), null, 13,
                DivisionDistribution.EQUAL_LENGTH);
        assertEquals(30, circle.ownRateDegForTick(0), 1e-12);
        assertEquals(Math.PI / 2, circle.withDivisionDistribution(DivisionDistribution.EQUAL_ANGLE)
                .withDivisions(5).divisionAngleRad(1), 1e-12);
    }
}
