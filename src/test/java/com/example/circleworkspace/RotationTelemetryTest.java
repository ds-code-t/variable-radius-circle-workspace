package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class RotationTelemetryTest {
    @Test void accumulatesPoweredAndSlavedRotationSinceTickZero() {
        var driver = new CircleState(1, 0, 0, 100, 0, true, 90, null);
        var driven = new CircleState(2, 150, 0, 50, 0, false, 0, 1);
        var contact = new ContactState(1, 1, 2, Tangency.EXTERNAL, 90, 270);
        var telemetry = new RotationTelemetry().at(
                List.of(driver, driven), List.of(contact), 4);

        assertEquals(360, telemetry.get(1).totalDegrees(), 1e-9);
        assertEquals(1, telemetry.get(1).completedFullRotations());
        assertEquals(-720, telemetry.get(2).totalDegrees(), 1e-9);
        assertEquals(-2, telemetry.get(2).completedFullRotations());
        assertEquals(-180, telemetry.get(2).stepDegrees(), 1e-9);
    }


    @Test void configurationChangesDoNotRewriteEarlierRotation() {
        var slow = new CircleState(1, 0, 0, 10, 0, true, 10, null);
        var fast = slow.withRate(100);
        var telemetry = new RotationTelemetry();

        assertEquals(50, telemetry.at(List.of(slow), List.of(), 5)
                .get(1).totalDegrees(), 1e-9);

        telemetry.invalidate();
        assertEquals(50, telemetry.at(List.of(fast), List.of(), 5)
                .get(1).totalDegrees(), 1e-9);

        assertEquals(150, telemetry.at(List.of(fast), List.of(), 6)
                .get(1).totalDegrees(), 1e-9);
    }

    @Test void addingAndRemovingShapesPreservesSurvivorTotals() {
        var first = new CircleState(1, 0, 0, 10, 0, true, 30, null);
        var second = new CircleState(2, 30, 0, 10, 0, true, 60, null);
        var telemetry = new RotationTelemetry();

        telemetry.at(List.of(first), List.of(), 4);
        telemetry.invalidate();
        var added = telemetry.at(List.of(first, second), List.of(), 4);
        assertEquals(120, added.get(1).totalDegrees(), 1e-9);
        assertEquals(0, added.get(2).totalDegrees(), 1e-9);

        var advanced = telemetry.at(List.of(first, second), List.of(), 5);
        assertEquals(150, advanced.get(1).totalDegrees(), 1e-9);
        assertEquals(60, advanced.get(2).totalDegrees(), 1e-9);
    }

    @Test void rewindingRecomputesFromStart() {
        var shape = new CircleState(1, 0, 0, 10, 0, true, 30, null);
        var telemetry = new RotationTelemetry();
        assertEquals(300, telemetry.at(List.of(shape), List.of(), 10)
                .get(1).totalDegrees(), 1e-9);
        assertEquals(90, telemetry.at(List.of(shape), List.of(), 3)
                .get(1).totalDegrees(), 1e-9);
    }
}
