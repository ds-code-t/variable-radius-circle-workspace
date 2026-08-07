package com.example.circleworkspace;

import java.util.*;

import static com.example.circleworkspace.Model.*;

/**
 * Deterministic cumulative boundary-length mapping used by equal-length
 * circumference divisions and divisions-per-tick rotation.
 */
public final class BoundaryDivisionMap {
    private static final int CACHE_LIMIT = 256;
    private static final Map<Key, Table> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(64, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Key, Table> eldest) {
                    return size() > CACHE_LIMIT;
                }
            });

    private BoundaryDivisionMap() {}

    public static double angleRadiansForFraction(WorkspaceShape shape, double fraction) {
        if (shape instanceof CircleState) return positiveFraction(fraction) * Math.TAU;
        return table(shape).angleForFraction(positiveFraction(fraction));
    }

    public static double unwrappedAngleDegrees(WorkspaceShape shape, double turns) {
        if (!Double.isFinite(turns)) throw new IllegalArgumentException("turns must be finite");
        double wholeTurns = Math.floor(turns);
        double fraction = turns - wholeTurns;
        return wholeTurns * 360.0 + Math.toDegrees(angleRadiansForFraction(shape, fraction));
    }

    private static Table table(WorkspaceShape shape) {
        Key key = Key.of(shape);
        return CACHE.computeIfAbsent(key, ignored -> build(shape));
    }

    private static Table build(WorkspaceShape shape) {
        int samples = shape instanceof RadialShapeState radial
                ? Math.max(256, Math.min(32_768, radial.radialSampleCount() * 32))
                : 256;
        double[] angles = new double[samples + 1];
        double[] cumulative = new double[samples + 1];
        double step = Math.TAU / samples;
        double previousRadius = shape.boundaryRadius(0);
        for (int i = 1; i <= samples; i++) {
            double angle = i * step;
            double radius = shape.boundaryRadius(angle);
            double chord = Math.sqrt(previousRadius * previousRadius + radius * radius
                    - 2 * previousRadius * radius * Math.cos(step));
            angles[i] = angle;
            cumulative[i] = cumulative[i - 1] + chord;
            previousRadius = radius;
        }
        return new Table(angles, cumulative);
    }

    private static double positiveFraction(double value) {
        double fraction = value - Math.floor(value);
        return fraction < 0 ? fraction + 1 : fraction;
    }

    private record Key(ShapeType type, double radius, List<Double> radialMultipliers) {
        static Key of(WorkspaceShape shape) {
            return shape instanceof RadialShapeState radial
                    ? new Key(shape.shapeType(), shape.radius(), radial.radialMultipliers())
                    : new Key(shape.shapeType(), shape.radius(), List.of());
        }
    }

    private record Table(double[] angles, double[] cumulative) {
        double angleForFraction(double fraction) {
            double target = fraction * cumulative[cumulative.length - 1];
            int index = Arrays.binarySearch(cumulative, target);
            if (index >= 0) return angles[index];
            int upper = -index - 1;
            if (upper <= 0) return 0;
            if (upper >= cumulative.length) return Math.TAU;
            int lower = upper - 1;
            double span = cumulative[upper] - cumulative[lower];
            double t = span == 0 ? 0 : (target - cumulative[lower]) / span;
            return angles[lower] + (angles[upper] - angles[lower]) * t;
        }
    }
}
