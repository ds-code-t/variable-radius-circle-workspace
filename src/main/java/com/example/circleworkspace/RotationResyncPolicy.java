package com.example.circleworkspace;

import java.util.*;

import static com.example.circleworkspace.Model.*;

final class RotationResyncPolicy {
    private RotationResyncPolicy() {}

    static <S extends WorkspaceShape> List<S> resync(List<S> shapes,
                                                     List<ContactState> contacts,
                                                     int changedShapeId,
                                                     long tick) {
        if (shapes.stream().noneMatch(shape -> shape.id() == changedShapeId)) return List.copyOf(shapes);
        var affected = connectedCircleIds(changedShapeId, contacts);
        var orderedContacts = contacts.stream()
                .filter(contact -> affected.contains(contact.aId()) && affected.contains(contact.bId()))
                .sorted(Comparator.comparing((ContactState c) -> !touches(c, changedShapeId))
                        .thenComparingInt(ContactState::id))
                .toList();
        var updated = new ArrayList<>(shapes);
        var solver = new RotationSolver();

        for (int pass = 0; pass <= affected.size(); pass++) {
            var results = solver.solve(updated, contacts);
            boolean assigned = false;
            for (var contact : orderedContacts) {
                var a = find(updated, contact.aId());
                var b = find(updated, contact.bId());
                if (a == null || b == null) continue;
                Integer slaveId = RotationLinkPolicy.chooseSlaveCircle(
                        contact, a, results.get(a.id()), b, results.get(b.id()));
                if (slaveId == null) continue;
                var replacement = assign(updated, contacts, slaveId, contact.id(), results, tick, solver);
                if (replacement == null) continue;
                replace(updated, replacement);
                assigned = true;
                break;
            }
            if (!assigned) break;
        }
        return List.copyOf(updated);
    }

    static Set<Integer> connectedCircleIds(int startingShapeId, List<ContactState> contacts) {
        var connected = new LinkedHashSet<Integer>();
        var queue = new ArrayDeque<Integer>();
        connected.add(startingShapeId);
        queue.add(startingShapeId);
        while (!queue.isEmpty()) {
            int id = queue.removeFirst();
            for (var contact : contacts) {
                if (!touches(contact, id)) continue;
                int other = contact.other(id);
                if (connected.add(other)) queue.addLast(other);
            }
        }
        return Set.copyOf(connected);
    }

    @SuppressWarnings("unchecked")
    private static <S extends WorkspaceShape> S assign(
            List<S> shapes, List<ContactState> contacts, int slaveId, int contactId,
            Map<Integer, RotationSolver.Result> currentResults, long tick, RotationSolver solver) {
        S shape = find(shapes, slaveId);
        var current = currentResults.get(slaveId);
        if (shape == null || shape.powered() || current == null) return null;
        var candidates = new ArrayList<>(shapes);
        replace(candidates, (S) shape.withSlave(contactId));
        var slaved = solver.solve(candidates, contacts).get(slaveId);
        if (slaved == null || slaved.mode() != DriveMode.SLAVED
                || !RotationLinkPolicy.isActivelyRotating(slaved)) return null;
        return RotationLinkPolicy.slavePreservingDisplayedAngle(shape, contactId, current, slaved, tick);
    }

    private static <S extends WorkspaceShape> S find(List<S> shapes, int id) {
        return shapes.stream().filter(shape -> shape.id() == id).findFirst().orElse(null);
    }

    private static <S extends WorkspaceShape> void replace(List<S> shapes, S replacement) {
        for (int i = 0; i < shapes.size(); i++) {
            if (shapes.get(i).id() == replacement.id()) {
                shapes.set(i, replacement);
                return;
            }
        }
    }

    private static boolean touches(ContactState contact, int id) {
        return contact.aId() == id || contact.bId() == id;
    }
}
