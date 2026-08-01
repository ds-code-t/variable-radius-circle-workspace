package com.example.circleworkspace;

import static com.example.circleworkspace.Model.*;

final class RotationLinkPolicy {
    private static final double RATE_EPSILON = 1e-9;

    private RotationLinkPolicy() {
    }

    static Integer chooseSlaveCircle(ContactState contact,
                                     CircleState a, RotationSolver.Result aResult,
                                     CircleState b, RotationSolver.Result bResult) {
        if (contact == null || a == null || b == null || aResult == null || bResult == null) return null;
        if (!connects(contact, a.id(), b.id())) return null;

        boolean aRotating = isActivelyRotating(aResult);
        boolean bRotating = isActivelyRotating(bResult);
        if (aRotating == bRotating) return null;

        CircleState candidate = aRotating ? b : a;
        return candidate.powered() ? null : candidate.id();
    }

    static boolean isActivelyRotating(RotationSolver.Result result) {
        return result != null
                && result.mode() != DriveMode.STOPPED
                && Math.abs(result.rateDegPerTick()) > RATE_EPSILON;
    }

    static double displayedAngle(CircleState circle, RotationSolver.Result result, long tick) {
        double rate = result == null ? 0 : result.rateDegPerTick();
        return normalize(circle.startAngleDeg() + rate * tick);
    }

    static CircleState stopPreservingDisplayedAngle(CircleState circle,
                                                     RotationSolver.Result currentResult,
                                                     long tick) {
        if (circle.slaveContactId() == null) return circle;
        return circle.withSlave(null).withStartAngle(displayedAngle(circle, currentResult, tick));
    }

    static CircleState slavePreservingDisplayedAngle(CircleState circle,
                                                      int contactId,
                                                      RotationSolver.Result currentResult,
                                                      RotationSolver.Result slavedResult,
                                                      long tick) {
        if (slavedResult == null || slavedResult.mode() != DriveMode.SLAVED) return circle;
        double displayedAngle = displayedAngle(circle, currentResult, tick);
        double startAngle = normalize(displayedAngle - slavedResult.rateDegPerTick() * tick);
        return circle.withSlave(contactId).withStartAngle(startAngle);
    }

    private static boolean connects(ContactState contact, int firstId, int secondId) {
        return (contact.aId() == firstId && contact.bId() == secondId)
                || (contact.aId() == secondId && contact.bId() == firstId);
    }

    static double normalize(double degrees) {
        degrees %= 360;
        if (degrees < 0) degrees += 360;
        return degrees;
    }
}
