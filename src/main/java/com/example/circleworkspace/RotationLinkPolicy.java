package com.example.circleworkspace;

import static com.example.circleworkspace.Model.*;

final class RotationLinkPolicy {
    private static final double RATE_EPSILON = 1e-9;

    private RotationLinkPolicy() {}

    static Integer chooseSlaveCircle(ContactState contact,
                                     WorkspaceShape a, RotationSolver.Result aResult,
                                     WorkspaceShape b, RotationSolver.Result bResult) {
        if (contact == null || a == null || b == null || aResult == null || bResult == null) return null;
        if (!connects(contact, a.id(), b.id())) return null;
        boolean aRotating = isActivelyRotating(aResult);
        boolean bRotating = isActivelyRotating(bResult);
        if (aRotating == bRotating) return null;
        WorkspaceShape candidate = aRotating ? b : a;
        return candidate.powered() ? null : candidate.id();
    }

    static boolean isActivelyRotating(RotationSolver.Result result) {
        return result != null && result.mode() != DriveMode.STOPPED
                && Math.abs(result.rateDegPerTick()) > RATE_EPSILON;
    }

    static double displayedAngle(WorkspaceShape shape, RotationSolver.Result result, long tick) {
        double rate = result == null ? 0 : result.rateDegPerTick();
        return normalize(shape.startAngleDeg() + rate * tick);
    }

    @SuppressWarnings("unchecked")
    static <S extends WorkspaceShape> S stopPreservingDisplayedAngle(S shape,
                                                                     RotationSolver.Result currentResult,
                                                                     long tick) {
        if (shape.slaveContactId() == null) return shape;
        return (S) shape.withSlave(null).withStartAngle(displayedAngle(shape, currentResult, tick));
    }

    @SuppressWarnings("unchecked")
    static <S extends WorkspaceShape> S slavePreservingDisplayedAngle(S shape,
                                                                      int contactId,
                                                                      RotationSolver.Result currentResult,
                                                                      RotationSolver.Result slavedResult,
                                                                      long tick) {
        if (slavedResult == null || slavedResult.mode() != DriveMode.SLAVED) return shape;
        double displayed = displayedAngle(shape, currentResult, tick);
        double start = normalize(displayed - slavedResult.rateDegPerTick() * tick);
        return (S) shape.withSlave(contactId).withStartAngle(start);
    }

    private static boolean connects(ContactState contact, int firstId, int secondId) {
        return (contact.aId() == firstId && contact.bId() == secondId)
                || (contact.aId() == secondId && contact.bId() == firstId);
    }

    static double normalize(double degrees) {
        return Model.normalize(degrees);
    }
}
