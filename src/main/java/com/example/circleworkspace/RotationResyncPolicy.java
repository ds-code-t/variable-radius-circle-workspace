package com.example.circleworkspace;

import java.util.*;

import static com.example.circleworkspace.Model.*;

/**
 * Re-evaluates automatic rotation inheritance for the contact network around a circle.
 * The policy is independent of JavaFX so it can be exercised with unit tests.
 */
final class RotationResyncPolicy {
    private RotationResyncPolicy() {
    }

    static List<CircleState> resync(List<CircleState> circles,
                                    List<ContactState> contacts,
                                    int changedCircleId,
                                    long tick) {
        if (circles.stream().noneMatch(circle -> circle.id() == changedCircleId)) {
            return List.copyOf(circles);
        }

        var affectedCircleIds = connectedCircleIds(changedCircleId, contacts);
        var orderedContacts = contacts.stream()
                .filter(contact -> affectedCircleIds.contains(contact.aId())
                        && affectedCircleIds.contains(contact.bId()))
                .sorted(Comparator
                        .comparing((ContactState contact) -> !touches(contact, changedCircleId))
                        .thenComparingInt(ContactState::id))
                .toList();

        var updated = new ArrayList<>(circles);
        var solver = new RotationSolver();

        // One successful assignment can make the next circle in the chain rotate,
        // so repeat until no more stopped circles can inherit a rotating neighbor.
        for (int pass = 0; pass <= affectedCircleIds.size(); pass++) {
            var results = solver.solve(updated, contacts);
            boolean assigned = false;

            for (var contact : orderedContacts) {
                var a = find(updated, contact.aId());
                var b = find(updated, contact.bId());
                if (a == null || b == null) continue;

                Integer slaveId = RotationLinkPolicy.chooseSlaveCircle(
                        contact, a, results.get(a.id()), b, results.get(b.id()));
                if (slaveId == null) continue;

                var replacement = assignPreservingDisplayedAngle(
                        updated, contacts, slaveId, contact.id(), results, tick, solver);
                if (replacement == null) continue;

                replace(updated, replacement);
                assigned = true;
                break;
            }

            if (!assigned) break;
        }

        return List.copyOf(updated);
    }

    static Set<Integer> connectedCircleIds(int startingCircleId, List<ContactState> contacts) {
        var connected = new LinkedHashSet<Integer>();
        var queue = new ArrayDeque<Integer>();
        connected.add(startingCircleId);
        queue.add(startingCircleId);

        while (!queue.isEmpty()) {
            int circleId = queue.removeFirst();
            for (var contact : contacts) {
                if (!touches(contact, circleId)) continue;
                int otherId = contact.other(circleId);
                if (connected.add(otherId)) queue.addLast(otherId);
            }
        }

        return Set.copyOf(connected);
    }

    private static CircleState assignPreservingDisplayedAngle(
            List<CircleState> circles,
            List<ContactState> contacts,
            int slaveId,
            int contactId,
            Map<Integer, RotationSolver.Result> currentResults,
            long tick,
            RotationSolver solver) {
        var circle = find(circles, slaveId);
        var currentResult = currentResults.get(slaveId);
        if (circle == null || circle.powered() || currentResult == null) return null;

        var candidateCircles = new ArrayList<>(circles);
        replace(candidateCircles, circle.withSlave(contactId));
        var slavedResult = solver.solve(candidateCircles, contacts).get(slaveId);
        if (slavedResult == null
                || slavedResult.mode() != DriveMode.SLAVED
                || !RotationLinkPolicy.isActivelyRotating(slavedResult)) {
            return null;
        }

        return RotationLinkPolicy.slavePreservingDisplayedAngle(
                circle, contactId, currentResult, slavedResult, tick);
    }

    private static CircleState find(List<CircleState> circles, int id) {
        return circles.stream().filter(circle -> circle.id() == id).findFirst().orElse(null);
    }

    private static void replace(List<CircleState> circles, CircleState replacement) {
        for (int i = 0; i < circles.size(); i++) {
            if (circles.get(i).id() == replacement.id()) {
                circles.set(i, replacement);
                return;
            }
        }
    }

    private static boolean touches(ContactState contact, int circleId) {
        return contact.aId() == circleId || contact.bId() == circleId;
    }
}
