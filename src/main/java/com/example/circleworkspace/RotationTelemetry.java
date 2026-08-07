package com.example.circleworkspace;

import java.util.*;

import static com.example.circleworkspace.Model.*;

/**
 * Accumulates signed angular movement as ticks occur. Configuration changes
 * affect future ticks only; already accrued rotation is never reconstructed
 * using a newly edited rate, profile, or coupling graph.
 */
public final class RotationTelemetry {
    public record Value(double stepDegrees, double totalDegrees) {
        public double totalTurns() {
            return totalDegrees / 360.0;
        }

        public long completedFullRotations() {
            return totalDegrees >= 0
                    ? (long) Math.floor(totalDegrees / 360.0)
                    : -(long) Math.floor(-totalDegrees / 360.0);
        }
    }

    private final RotationSolver solver = new RotationSolver();
    private List<WorkspaceShape> activeShapes = List.of();
    private List<ContactState> activeContacts = List.of();
    private final Map<Integer, Double> cumulativeDegrees = new HashMap<>();
    private long accumulatedTicks;
    private boolean initialized;
    private boolean configurationDirty;

    /**
     * Marks that domain state may have changed. Totals are deliberately kept;
     * the next call snapshots the new configuration for subsequent ticks.
     */
    public void invalidate() {
        configurationDirty = true;
    }

    public void reset() {
        activeShapes = List.of();
        activeContacts = List.of();
        cumulativeDegrees.clear();
        accumulatedTicks = 0;
        initialized = false;
        configurationDirty = false;
    }

    public Map<Integer, Value> at(List<? extends WorkspaceShape> shapes,
                                  List<ContactState> contacts,
                                  long tick) {
        long targetTick = Math.max(0, tick);
        var shapeSnapshot = List.<WorkspaceShape>copyOf(shapes);
        var contactSnapshot = List.copyOf(contacts);

        if (!initialized || targetTick < accumulatedTicks) {
            initialize(shapeSnapshot, contactSnapshot);
            accumulateTo(targetTick);
        } else {
            // Advance elapsed ticks using the configuration that was active
            // during that interval. Edits observed at targetTick take effect
            // only after all earlier ticks have been accounted for.
            accumulateTo(targetTick);
            if (configurationDirty
                    || !shapeSnapshot.equals(activeShapes)
                    || !contactSnapshot.equals(activeContacts)) {
                adopt(shapeSnapshot, contactSnapshot);
            }
        }

        var current = solver.solve(activeShapes, activeContacts, targetTick);
        var result = new LinkedHashMap<Integer, Value>();
        for (var shape : activeShapes) {
            double stepDegrees = current.getOrDefault(shape.id(), stopped()).rateDegPerTick();
            result.put(shape.id(), new Value(
                    stepDegrees, cumulativeDegrees.getOrDefault(shape.id(), 0.0)));
        }
        return Map.copyOf(result);
    }

    private void initialize(List<WorkspaceShape> shapes, List<ContactState> contacts) {
        activeShapes = shapes;
        activeContacts = contacts;
        cumulativeDegrees.clear();
        shapes.forEach(shape -> cumulativeDegrees.put(shape.id(), 0.0));
        accumulatedTicks = 0;
        initialized = true;
        configurationDirty = false;
    }

    private void accumulateTo(long targetTick) {
        while (accumulatedTicks < targetTick) {
            var step = solver.solve(activeShapes, activeContacts, accumulatedTicks);
            for (var shape : activeShapes) {
                cumulativeDegrees.merge(shape.id(),
                        step.getOrDefault(shape.id(), stopped()).rateDegPerTick(),
                        Double::sum);
            }
            accumulatedTicks++;
        }
    }

    private void adopt(List<WorkspaceShape> shapes, List<ContactState> contacts) {
        var retained = new HashMap<Integer, Double>();
        for (var shape : shapes) {
            retained.put(shape.id(), cumulativeDegrees.getOrDefault(shape.id(), 0.0));
        }
        cumulativeDegrees.clear();
        cumulativeDegrees.putAll(retained);
        activeShapes = shapes;
        activeContacts = contacts;
        configurationDirty = false;
    }

    private static RotationSolver.Result stopped() {
        return new RotationSolver.Result(0, 0, DriveMode.STOPPED, null);
    }
}
