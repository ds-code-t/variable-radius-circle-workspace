package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import static com.example.circleworkspace.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class RotationLinkPolicyTest {
    private static final ContactState CONTACT =
            new ContactState(7, 1, 2, Tangency.EXTERNAL, 90, 270);

    @Test
    void stoppedCircleAIsChosenWhenCircleBIsRotating() {
        var a = circle(1, false, null);
        var b = circle(2, true, null);

        Integer slaveId = RotationLinkPolicy.chooseSlaveCircle(
                CONTACT,
                a, result(0, DriveMode.STOPPED),
                b, result(6, DriveMode.POWERED));

        assertEquals(1, slaveId);
    }

    @Test
    void stoppedCircleBIsChosenWhenCircleAIsRotating() {
        var a = circle(1, true, null);
        var b = circle(2, false, null);

        Integer slaveId = RotationLinkPolicy.chooseSlaveCircle(
                CONTACT,
                a, result(6, DriveMode.POWERED),
                b, result(0, DriveMode.STOPPED));

        assertEquals(2, slaveId);
    }

    @Test
    void poweredStoppedCircleIsNotAutomaticallyOverridden() {
        var a = circle(1, true, null);
        var b = circle(2, true, null);

        Integer slaveId = RotationLinkPolicy.chooseSlaveCircle(
                CONTACT,
                a, result(0, DriveMode.POWERED),
                b, result(6, DriveMode.POWERED));

        assertNull(slaveId);
    }

    @Test
    void noAssignmentWhenBothRotateOrBothAreStopped() {
        var a = circle(1, false, null);
        var b = circle(2, false, null);

        assertNull(RotationLinkPolicy.chooseSlaveCircle(
                CONTACT, a, result(4, DriveMode.SLAVED), b, result(-8, DriveMode.SLAVED)));
        assertNull(RotationLinkPolicy.chooseSlaveCircle(
                CONTACT, a, result(0, DriveMode.STOPPED), b, result(0, DriveMode.STOPPED)));
    }

    @Test
    void startingAndStoppingSlavingPreserveTheDisplayedAngle() {
        long tick = 10;
        var circle = new CircleState(1, 0, 0, 50, 15, false, 0, null);
        var stopped = result(0, DriveMode.STOPPED);
        var slaved = result(-12, DriveMode.SLAVED);

        var linked = RotationLinkPolicy.slavePreservingDisplayedAngle(circle, 7, stopped, slaved, tick);
        assertEquals(15, RotationLinkPolicy.displayedAngle(linked, slaved, tick), 1e-9);

        var detached = RotationLinkPolicy.stopPreservingDisplayedAngle(linked, slaved, tick);
        assertNull(detached.slaveContactId());
        assertEquals(15, detached.startAngleDeg(), 1e-9);
    }

    private static CircleState circle(int id, boolean powered, Integer slaveContactId) {
        return new CircleState(id, 0, 0, 50, 0, powered, powered ? 6 : 0, slaveContactId);
    }

    private static RotationSolver.Result result(double rate, DriveMode mode) {
        return new RotationSolver.Result(rate, mode == DriveMode.SLAVED ? 1 : 0, mode, null);
    }
}
