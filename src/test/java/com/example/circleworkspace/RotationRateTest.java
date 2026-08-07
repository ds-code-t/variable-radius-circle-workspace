package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class RotationRateTest {
    @Test void degreeModeIsIndependentOfDivisions() {
        var rate = new RotationRate(-3.5, RotationRateUnit.DEGREES_PER_TICK);
        assertEquals(-3.5, rate.degreesPerTick(12), 1e-12);
        assertEquals(-3.5, rate.degreesPerTick(100), 1e-12);
    }

    @Test void divisionModeUsesThreeHundredSixtyOverN() {
        var rate = new RotationRate(1, RotationRateUnit.DIVISIONS_PER_TICK);
        assertEquals(30, rate.degreesPerTick(12), 1e-12);
        assertEquals(3.6, rate.degreesPerTick(100), 1e-12);
        assertEquals(-1.8, new RotationRate(-.5, RotationRateUnit.DIVISIONS_PER_TICK)
                .degreesPerTick(100), 1e-12);
    }

    @Test void unitConversionPreservesAngularSpeed() {
        var degrees = new RotationRate(30, RotationRateUnit.DEGREES_PER_TICK);
        var divisions = degrees.convertedTo(RotationRateUnit.DIVISIONS_PER_TICK, 12);
        assertEquals(1, divisions.value(), 1e-12);
        assertEquals(30, divisions.degreesPerTick(12), 1e-12);
    }

    @Test void zeroDivisionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new RotationRate(1, RotationRateUnit.DIVISIONS_PER_TICK).degreesPerTick(0));
    }
}
