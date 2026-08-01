package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class RotationResyncPolicyTest {
    @Test
    void existingTouchingCircleStartsSlavingWhenFirstCircleBeginsRotating() {
        var circles = List.of(
                circle(1, 0, 0, 50, true, 8, null),
                circle(2, 100, 0, 50, false, 0, null));
        var contacts = List.of(contact(7, 1, 2));

        var updated = RotationResyncPolicy.resync(circles, contacts, 1, 12);

        assertEquals(7, find(updated, 2).slaveContactId());
        assertEquals(-8, new RotationSolver().solve(updated, contacts).get(2).rateDegPerTick(), 1e-9);
    }

    @Test
    void existingTouchingCircleStartsSlavingInTheOppositeSnapDirection() {
        var circles = List.of(
                circle(1, 0, 0, 50, false, 0, null),
                circle(2, 100, 0, 50, true, 8, null));
        var contacts = List.of(contact(7, 1, 2));

        var updated = RotationResyncPolicy.resync(circles, contacts, 2, 12);

        assertEquals(7, find(updated, 1).slaveContactId());
        assertEquals(-8, new RotationSolver().solve(updated, contacts).get(1).rateDegPerTick(), 1e-9);
    }

    @Test
    void resyncCascadesThroughDirectlyTouchingNeighbors() {
        var circles = List.of(
                circle(1, 0, 0, 50, true, 6, null),
                circle(2, 100, 0, 50, false, 0, null),
                circle(3, 200, 0, 50, false, 0, null));
        var contacts = List.of(contact(7, 1, 2), contact(8, 2, 3));

        var updated = RotationResyncPolicy.resync(circles, contacts, 1, 5);
        var results = new RotationSolver().solve(updated, contacts);

        assertEquals(7, find(updated, 2).slaveContactId());
        assertEquals(8, find(updated, 3).slaveContactId());
        assertEquals(-6, results.get(2).rateDegPerTick(), 1e-9);
        assertEquals(6, results.get(3).rateDegPerTick(), 1e-9);
    }

    @Test
    void assignmentPreservesTheCurrentDisplayedAngle() {
        long tick = 10;
        var circles = List.of(
                circle(1, 0, 0, 50, true, 9, null),
                new CircleState(2, 100, 0, 50, 37, false, 0, null));
        var contacts = List.of(contact(7, 1, 2));

        var updated = RotationResyncPolicy.resync(circles, contacts, 1, tick);
        var result = new RotationSolver().solve(updated, contacts).get(2);

        assertEquals(37, RotationLinkPolicy.displayedAngle(find(updated, 2), result, tick), 1e-9);
    }

    @Test
    void poweredCircleIsNeverAutomaticallyOverridden() {
        var circles = List.of(
                circle(1, 0, 0, 50, true, 6, null),
                circle(2, 100, 0, 50, true, 0, null));
        var contacts = List.of(contact(7, 1, 2));

        var updated = RotationResyncPolicy.resync(circles, contacts, 1, 0);

        assertNull(find(updated, 2).slaveContactId());
        assertEquals(DriveMode.POWERED, new RotationSolver().solve(updated, contacts).get(2).mode());
    }

    private static CircleState circle(int id, double x, double y, double radius,
                                      boolean powered, double rate, Integer slaveContactId) {
        return new CircleState(id, x, y, radius, 0, powered, rate, slaveContactId);
    }

    private static ContactState contact(int id, int aId, int bId) {
        return new ContactState(id, aId, bId, Tangency.EXTERNAL, 90, 270);
    }

    private static CircleState find(List<CircleState> circles, int id) {
        return circles.stream().filter(circle -> circle.id() == id).findFirst().orElseThrow();
    }
}
