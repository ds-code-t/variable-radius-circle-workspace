package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class MixedShapeCouplingTest {
    @Test void legacyCircleRatioIsUnchanged() {
        var shapes = List.of(
                new CircleState(1, 0, 0, 100, 0, true, 6, null),
                new CircleState(2, 150, 0, 50, 0, false, 0, 1));
        var contacts = List.of(new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270));
        assertEquals(-12, new RotationSolver().solve(shapes, contacts).get(2).rateDegPerTick(), 1e-9);
    }

    @Test void everyRequiredMixedDirectionIsFiniteAndSigned() {
        var circle = new CircleState(1, 0, 0, 50, 0, true, 8, null);
        var radial = RadialShapeState.harmonic(2, 100, 0, 45, 12, .15).withSlave(1);
        var contact = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270);
        double circleToRadial = new RotationSolver().solve(List.of(circle, radial), List.of(contact))
                .get(2).rateDegPerTick();
        assertTrue(Double.isFinite(circleToRadial));
        assertTrue(circleToRadial < 0);

        var radialDriver = radial.withSlave(null).withPower(true)
                .withRotationRate(new RotationRate(8, RotationRateUnit.DEGREES_PER_TICK));
        var circleDriven = circle.withPower(false).withSlave(1);
        double radialToCircle = new RotationSolver().solve(List.of(circleDriven, radialDriver), List.of(contact))
                .get(1).rateDegPerTick();
        assertTrue(Double.isFinite(radialToCircle));
        assertTrue(radialToCircle < 0);

        var second = RadialShapeState.harmonic(3, 200, 0, 55, 16, .12).withSlave(2);
        var secondContact = new ContactState(2, 2, 3, Tangency.EXTERNAL, 90, 270);
        double radialToRadial = new RotationSolver().solve(
                List.of(radialDriver, second), List.of(secondContact)).get(3).rateDegPerTick();
        assertTrue(Double.isFinite(radialToRadial));
        assertTrue(radialToRadial < 0);
    }

    @Test void cyclesStopDeterministically() {
        var a = new CircleState(1, 0, 0, 50, 0, false, 0, 1);
        var b = new CircleState(2, 100, 0, 50, 0, false, 0, 2);
        var contacts = List.of(
                new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270),
                new ContactState(2, 1, 2, Tangency.EXTERNAL, 90, 270));
        var result = new RotationSolver().solve(List.of(a, b), contacts);
        assertEquals(DriveMode.STOPPED, result.get(1).mode());
        assertEquals(DriveMode.STOPPED, result.get(2).mode());
    }
}
