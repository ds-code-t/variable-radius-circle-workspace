package com.example.circleworkspace;

import java.util.*;

import static com.example.circleworkspace.Model.*;

public final class RotationSolver {
    public record Result(double rateDegPerTick, int depth, DriveMode mode, Integer sourceCircleId) {
        public Integer sourceShapeId() { return sourceCircleId; }
    }

    public Map<Integer, Result> solve(List<? extends WorkspaceShape> shapes, List<ContactState> contacts) {
        return solve(shapes, contacts, 0);
    }

    public Map<Integer, Result> solve(List<? extends WorkspaceShape> shapes,
                                      List<ContactState> contacts, long tick) {
        var byShape = new HashMap<Integer, WorkspaceShape>();
        shapes.forEach(shape -> byShape.put(shape.id(), shape));
        var byContact = new HashMap<Integer, ContactState>();
        contacts.forEach(contact -> byContact.put(contact.id(), contact));
        var memo = new HashMap<Integer, Result>();
        for (var shape : shapes) resolve(shape.id(), byShape, byContact, memo, new LinkedHashSet<>(), tick);
        return Map.copyOf(memo);
    }

    private Result resolve(int id,
                           Map<Integer, WorkspaceShape> shapes,
                           Map<Integer, ContactState> contacts,
                           Map<Integer, Result> memo,
                           Set<Integer> path,
                           long tick) {
        if (memo.containsKey(id)) return memo.get(id);
        var shape = shapes.get(id);
        if (shape == null) return stopped();

        if (shape.powered()) {
            var result = new Result(shape.ownRateDegForTick(tick), 0, DriveMode.POWERED, id);
            memo.put(id, result);
            return result;
        }

        if (shape.slaveContactId() == null || !path.add(id)) {
            var result = stopped();
            memo.put(id, result);
            return result;
        }

        var contact = contacts.get(shape.slaveContactId());
        if (contact == null || (contact.aId() != id && contact.bId() != id)) {
            var result = stopped();
            memo.put(id, result);
            path.remove(id);
            return result;
        }

        int parentId = contact.other(id);
        var parent = shapes.get(parentId);
        var parentResult = resolve(parentId, shapes, contacts, memo, path, tick);
        path.remove(id);
        if (parent == null || parentResult.mode() == DriveMode.STOPPED) {
            var result = stopped();
            memo.put(id, result);
            return result;
        }

        double sign = contact.type() == Tangency.EXTERNAL ? -1.0 : 1.0;
        double parentAngle = Math.toRadians(contact.touchFor(parentId));
        double childAngle = Math.toRadians(contact.touchFor(id));
        double parentProgression = parent.boundaryProgression(parentAngle);
        double childProgression = shape.boundaryProgression(childAngle);
        double rate = sign * parentResult.rateDegPerTick() * parentProgression / childProgression;
        if (!Double.isFinite(rate)) rate = 0;

        var result = new Result(rate, parentResult.depth() + 1, DriveMode.SLAVED, parentId);
        memo.put(id, result);
        return result;
    }

    private static Result stopped() {
        return new Result(0, 0, DriveMode.STOPPED, null);
    }
}
