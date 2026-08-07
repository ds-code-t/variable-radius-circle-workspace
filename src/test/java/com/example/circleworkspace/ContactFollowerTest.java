package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class ContactFollowerTest {
    private final RotationSolver rotationSolver = new RotationSolver();
    private final ContactFollower follower = new ContactFollower();

    @Test void drivenShapeMovesOnlyAlongStoredBearing() {
        var driver = RadialShapeState.harmonic(1, 10, 20, 50, 12, .2)
                .withPower(true)
                .withRotationRate(new RotationRate(7, RotationRateUnit.DEGREES_PER_TICK));
        var driven = RadialShapeState.harmonic(2, 110, 20, 35, 10, .15).withSlave(1);
        var contact = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270,
                ContactFollowMode.FIXED_CONTACT_ALLOW_OVERLAP, 0);

        var rotations = rotationSolver.solve(List.of(driver, driven), List.of(contact));
        var atZero = follower.solve(List.of(driver, driven), List.of(contact), 0, rotations).get(2);
        var later = follower.solve(List.of(driver, driven), List.of(contact), 17, rotations).get(2);

        assertEquals(driver.y(), atZero.y(), 1e-9);
        assertEquals(driver.y(), later.y(), 1e-9);
        assertNotEquals(atZero.x(), later.x());
    }

    @Test void screenSpaceBearingIsConvertedToBoundaryAngleConvention() {
        var driver = new RadialShapeState(1, 0, 0, 10, 0, true,
                new RotationRate(90, RotationRateUnit.DEGREES_PER_TICK), null, 8,
                List.of(2.0, 1.0, 1.0, 1.0));
        var driven = new CircleState(2, 30, 0, 5, 0, false, 0, 1);
        var contact = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270,
                ContactFollowMode.FIXED_CONTACT_ALLOW_OVERLAP, 0);
        var shapes = List.of(driver, driven);
        var rotations = rotationSolver.solve(shapes, List.of(contact));

        var atZero = follower.solve(shapes, List.of(contact), 0, rotations).get(2);
        var afterQuarterTurn = follower.solve(shapes, List.of(contact), 1, rotations).get(2);

        // Bearing 0° points right. At tick zero the right-facing radial sample is
        // the second value (10); after a 90° turn the first value (20) faces right.
        assertEquals(15, atZero.x(), 1e-9);
        assertEquals(25, afterQuarterTurn.x(), 1e-9);
        assertEquals(0, atZero.y(), 1e-9);
        assertEquals(0, afterQuarterTurn.y(), 1e-9);
    }

    @Test void fixedContactModeUsesTheStoredContactRay() {
        var driver = RadialShapeState.harmonic(1, 0, 0, 50, 12, .3)
                .withPower(true)
                .withRotationRate(new RotationRate(9, RotationRateUnit.DEGREES_PER_TICK));
        var driven = RadialShapeState.harmonic(2, 100, 0, 40, 12, .25).withSlave(1);
        var contact = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270,
                ContactFollowMode.FIXED_CONTACT_ALLOW_OVERLAP, 0);
        var rotations = rotationSolver.solve(List.of(driver, driven), List.of(contact));

        for (long tick = 0; tick < 30; tick++) {
            var posed = follower.solve(List.of(driver, driven), List.of(contact), tick, rotations);
            var a = posed.get(1);
            var b = posed.get(2);
            double aAngle = a.displayedAngle(tick, rotations.get(1).rateDegPerTick());
            double bAngle = b.displayedAngle(tick, rotations.get(2).rateDegPerTick());
            double expected = a.boundaryRadius(a.localAngleForWorldDirection(0, aAngle))
                    + b.boundaryRadius(b.localAngleForWorldDirection(180, bAngle));
            assertEquals(expected, b.x() - a.x(), 1e-8);
            assertEquals(a.y(), b.y(), 1e-9);
        }
    }


    @Test void noOverlapDoesNotAnticipateAProtrusionThatHasNotCollided() {
        var driver = new RadialShapeState(1, 0, 0, 10, 0, true,
                new RotationRate(0, RotationRateUnit.DEGREES_PER_TICK), null, 16,
                List.of(1.0, 3.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0));
        var driven = new CircleState(2, 15, 0, 5, 0, false, 0, 1);
        var contact = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270,
                ContactFollowMode.NO_OVERLAP_SWITCH_CONTACT, 0);
        var shapes = List.of(driver, driven);
        var rotations = rotationSolver.solve(shapes, List.of(contact));

        var posed = follower.solve(shapes, List.of(contact), 0, rotations);

        // The diagonal protrusion has a large projection on the contact axis,
        // but it is still clear of the driven circle. The original right-side
        // perimeter contact must therefore be retained.
        assertEquals(15, posed.get(2).x(), 1e-7);
        assertEquals(0, posed.get(2).y(), 1e-9);
    }

    @Test void noOverlapModeChangesDistanceAsSupportingProtrusionsChange() {
        var driver = RadialShapeState.harmonic(1, 0, 0, 50, 12, .3)
                .withPower(true)
                .withRotationRate(new RotationRate(11, RotationRateUnit.DEGREES_PER_TICK));
        var driven = RadialShapeState.harmonic(2, 100, 0, 40, 10, .25).withSlave(1);
        var contact = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270,
                ContactFollowMode.NO_OVERLAP_SWITCH_CONTACT, 0);
        var rotations = rotationSolver.solve(List.of(driver, driven), List.of(contact));

        double first = follower.solve(List.of(driver, driven), List.of(contact), 0, rotations).get(2).x();
        boolean changed = false;
        for (long tick = 1; tick < 40; tick++) {
            var target = follower.solve(List.of(driver, driven), List.of(contact), tick, rotations).get(2);
            assertEquals(0, target.y(), 1e-9);
            changed |= Math.abs(target.x() - first) > 1e-6;
        }
        assertTrue(changed);
    }

    @Test void chainedSlaveUsesAlreadyPosedIntermediateDriver() {
        var root = new CircleState(1, 0, 0, 30, 0, true, 5, null);
        var middle = RadialShapeState.harmonic(2, 60, 0, 25, 12, .2).withSlave(1);
        var leaf = new CircleState(3, 120, 0, 20, 0, false, 0, 2);
        var first = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270,
                ContactFollowMode.FIXED_CONTACT_ALLOW_OVERLAP, 0);
        var second = new ContactState(2, 2, 3, Tangency.EXTERNAL, 90, 270,
                ContactFollowMode.FIXED_CONTACT_ALLOW_OVERLAP, 0);
        var shapes = List.of(root, middle, leaf);
        var contacts = List.of(first, second);
        var rotations = rotationSolver.solve(shapes, contacts);
        var posed = follower.solve(shapes, contacts, 9, rotations);

        assertTrue(posed.get(2).x() > posed.get(1).x());
        assertTrue(posed.get(3).x() > posed.get(2).x());
        assertEquals(0, posed.get(3).y(), 1e-9);
    }

    @Test void circlesRemainAtExactRadiusSumInBothModes() {
        var driver = new CircleState(1, 0, 0, 40, 0, true, 7, null);
        var driven = new CircleState(2, 80, 0, 25, 0, false, 0, 1);
        for (var mode : ContactFollowMode.values()) {
            var contact = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270, mode, 0);
            var rotations = rotationSolver.solve(List.of(driver, driven), List.of(contact));
            var posed = follower.solve(List.of(driver, driven), List.of(contact), 100, rotations);
            assertEquals(65, posed.get(2).x(), 1e-9);
            assertEquals(0, posed.get(2).y(), 1e-9);
        }
    }
}
