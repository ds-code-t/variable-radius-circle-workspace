package com.example.circleworkspace;

import java.util.*;

public final class Model {
    private Model() {}

    public static final int MIN_DIVISIONS = 2;
    public static final int MAX_DIVISIONS = 10_000;
    public static final int MIN_RADIAL_SAMPLES = 3;
    public static final int MAX_RADIAL_SAMPLES = 2_048;
    public static final double MIN_RADIUS = 0.1;
    public static final double DEFAULT_MARKER_LENGTH = 10.0;

    public enum Tangency { EXTERNAL, INTERNAL }
    public enum ContactFollowMode { FIXED_CONTACT_ALLOW_OVERLAP, NO_OVERLAP_SWITCH_CONTACT }
    public enum DriveMode { STOPPED, POWERED, SLAVED }
    public enum ShapeType { CIRCLE, RADIAL }
    public enum RotationRateUnit { DEGREES_PER_TICK, DIVISIONS_PER_TICK }
    public enum DivisionDistribution { EQUAL_LENGTH, EQUAL_ANGLE }

    public record RotationRate(double value, RotationRateUnit unit) {
        public RotationRate {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("rotation rate must be finite");
            Objects.requireNonNull(unit, "unit");
        }

        public double degreesPerTick(int divisions) {
            requireDivisions(divisions);
            return unit == RotationRateUnit.DEGREES_PER_TICK ? value : value * 360.0 / divisions;
        }

        public RotationRate convertedTo(RotationRateUnit target, int divisions) {
            Objects.requireNonNull(target, "target");
            if (target == unit) return this;
            double degrees = degreesPerTick(divisions);
            return new RotationRate(
                    target == RotationRateUnit.DEGREES_PER_TICK ? degrees : degrees * divisions / 360.0,
                    target);
        }
    }

    public sealed interface WorkspaceShape permits CircleState, RadialShapeState {
        int id();
        double x();
        double y();
        double radius();
        double startAngleDeg();
        boolean powered();
        RotationRate rotationRate();
        Integer slaveContactId();
        /**
         * Number of circumference unit points, including the closing endpoint.
         * A value of U therefore produces U - 1 distinct line intervals.
         */
        int divisions();
        DivisionDistribution divisionDistribution();
        ShapeType shapeType();

        default int lineDivisionCount() {
            return divisions() - 1;
        }

        WorkspaceShape withPosition(double x, double y);
        WorkspaceShape withRadius(double radius);
        WorkspaceShape withStartAngle(double angle);
        WorkspaceShape withPower(boolean powered);
        WorkspaceShape withRotationRate(RotationRate rate);
        WorkspaceShape withSlave(Integer contactId);
        WorkspaceShape withDivisions(int divisions);
        WorkspaceShape withDivisionDistribution(DivisionDistribution distribution);

        default WorkspaceShape scaledToBoundaryLength(double targetLength) {
            if (!Double.isFinite(targetLength) || targetLength <= 0) {
                throw new IllegalArgumentException("target boundary length must be positive");
            }
            double currentLength = boundaryLength();
            if (!Double.isFinite(currentLength) || currentLength <= 0) {
                throw new IllegalStateException("shape boundary length must be positive");
            }
            return withRadius(Math.max(MIN_RADIUS, radius() * targetLength / currentLength));
        }

        default double ownRateDegPerTick() {
            return ownRateDegForTick(0);
        }

        default double ownRateDegForTick(long tick) {
            if (rotationRate().unit() == RotationRateUnit.DEGREES_PER_TICK
                    || divisionDistribution() == DivisionDistribution.EQUAL_ANGLE
                    || this instanceof CircleState) {
                return rotationRate().degreesPerTick(lineDivisionCount());
            }
            double turnsPerTick = rotationRate().value() / lineDivisionCount();
            double before = BoundaryDivisionMap.unwrappedAngleDegrees(this, tick * turnsPerTick);
            double after = BoundaryDivisionMap.unwrappedAngleDegrees(this, (tick + 1) * turnsPerTick);
            return after - before;
        }

        default double divisionAngleRad(int divisionIndex) {
            int count = lineDivisionCount();
            if (divisionIndex < 0 || divisionIndex >= count) {
                throw new IllegalArgumentException("line division index out of range");
            }
            return divisionDistribution() == DivisionDistribution.EQUAL_ANGLE
                    ? Math.TAU * divisionIndex / count
                    : BoundaryDivisionMap.angleRadiansForFraction(
                            this, (double) divisionIndex / count);
        }

        default double circumference() {
            return boundaryLength();
        }

        default double diameter() {
            return radius() * 2.0;
        }

        /** Radius at a local mathematical angle in radians, before start-angle rotation. */
        double boundaryRadius(double localAngleRad);

        /**
         * Local boundary progression ds/dtheta. Implementations return a positive,
         * finite metric suitable for the deterministic first-order coupling rule.
         */
        double boundaryProgression(double localAngleRad);

        default double boundaryLength() {
            int samples = this instanceof RadialShapeState radial
                    ? Math.max(256, Math.min(32_768, radial.radialSampleCount() * 32))
                    : 256;
            double step = Math.TAU / samples;
            double total = 0;
            double previous = boundaryRadius(0);
            for (int i = 1; i <= samples; i++) {
                double current = boundaryRadius(i * step);
                double chord = Math.sqrt(previous * previous + current * current
                        - 2 * previous * current * Math.cos(step));
                total += chord;
                previous = current;
            }
            return total;
        }

        default double displayedAngle(long tick, double effectiveRate) {
            if (powered()
                    && rotationRate().unit() == RotationRateUnit.DIVISIONS_PER_TICK
                    && divisionDistribution() == DivisionDistribution.EQUAL_LENGTH
                    && !(this instanceof CircleState)) {
                double turns = tick * rotationRate().value() / lineDivisionCount();
                return normalize(startAngleDeg()
                        + BoundaryDivisionMap.unwrappedAngleDegrees(this, turns));
            }
            return normalize(startAngleDeg() + effectiveRate * tick);
        }

        default double localAngleForWorldDirection(double worldDirectionDeg, double displayedAngleDeg) {
            return Math.toRadians(normalize(worldDirectionDeg - displayedAngleDeg));
        }
    }

    public record CircleState(
            int id,
            double x,
            double y,
            double radius,
            double startAngleDeg,
            boolean powered,
            RotationRate rotationRate,
            Integer slaveContactId,
            int divisions,
            DivisionDistribution divisionDistribution
    ) implements WorkspaceShape {
        public CircleState {
            validateCommon(id, x, y, radius, startAngleDeg, rotationRate, divisions);
            Objects.requireNonNull(divisionDistribution, "divisionDistribution");
        }

        public CircleState(int id, double x, double y, double radius, double startAngleDeg,
                           boolean powered, RotationRate rotationRate,
                           Integer slaveContactId, int divisions) {
            this(id, x, y, radius, startAngleDeg, powered, rotationRate,
                    slaveContactId, divisions, DivisionDistribution.EQUAL_LENGTH);
        }

        /** Compatibility constructor for the original persistence/tests/API. */
        public CircleState(int id, double x, double y, double radius, double startAngleDeg,
                           boolean powered, double ownRateDegPerTick, Integer slaveContactId) {
            this(id, x, y, radius, startAngleDeg, powered,
                    new RotationRate(ownRateDegPerTick, RotationRateUnit.DEGREES_PER_TICK),
                    slaveContactId, Math.max(4, (int) Math.round(Math.TAU * radius)) + 1,
                    DivisionDistribution.EQUAL_LENGTH);
        }

        @Override public ShapeType shapeType() { return ShapeType.CIRCLE; }
        @Override public double boundaryRadius(double localAngleRad) { return radius; }
        @Override public double boundaryProgression(double localAngleRad) { return radius; }
        @Override public double boundaryLength() { return Math.TAU * radius; }

        @Override public CircleState withPosition(double nx, double ny) {
            return new CircleState(id, nx, ny, radius, startAngleDeg, powered, rotationRate, slaveContactId, divisions, divisionDistribution);
        }
        @Override public CircleState withRadius(double r) {
            return new CircleState(id, x, y, r, startAngleDeg, powered, rotationRate, slaveContactId, divisions, divisionDistribution);
        }
        @Override public CircleState withStartAngle(double a) {
            return new CircleState(id, x, y, radius, a, powered, rotationRate, slaveContactId, divisions, divisionDistribution);
        }
        @Override public CircleState withPower(boolean p) {
            return new CircleState(id, x, y, radius, startAngleDeg, p, rotationRate, p ? null : slaveContactId, divisions, divisionDistribution);
        }
        public CircleState withRate(double degreesPerTick) {
            return withRotationRate(new RotationRate(degreesPerTick, RotationRateUnit.DEGREES_PER_TICK));
        }
        @Override public CircleState withRotationRate(RotationRate rate) {
            return new CircleState(id, x, y, radius, startAngleDeg, powered, rate, slaveContactId, divisions, divisionDistribution);
        }
        @Override public CircleState withSlave(Integer contactId) {
            return new CircleState(id, x, y, radius, startAngleDeg, false, rotationRate, contactId, divisions, divisionDistribution);
        }
        @Override public CircleState withDivisions(int count) {
            return new CircleState(id, x, y, radius, startAngleDeg, powered, rotationRate, slaveContactId, count, divisionDistribution);
        }
        @Override public CircleState withDivisionDistribution(DivisionDistribution distribution) {
            return new CircleState(id, x, y, radius, startAngleDeg, powered, rotationRate,
                    slaveContactId, divisions, distribution);
        }
    }

    public record RadialShapeState(
            int id,
            double x,
            double y,
            double radius,
            double startAngleDeg,
            boolean powered,
            RotationRate rotationRate,
            Integer slaveContactId,
            int divisions,
            DivisionDistribution divisionDistribution,
            List<Double> radialMultipliers
    ) implements WorkspaceShape {
        public RadialShapeState {
            validateCommon(id, x, y, radius, startAngleDeg, rotationRate, divisions);
            Objects.requireNonNull(divisionDistribution, "divisionDistribution");
            radialMultipliers = List.copyOf(radialMultipliers == null ? List.of() : radialMultipliers);
            if (radialMultipliers.size() < MIN_RADIAL_SAMPLES || radialMultipliers.size() > MAX_RADIAL_SAMPLES) {
                throw new IllegalArgumentException("radial sample count out of range");
            }
            for (double value : radialMultipliers) {
                if (!Double.isFinite(value) || value <= 0) {
                    throw new IllegalArgumentException("radial values must be finite and positive");
                }
            }
        }


        public RadialShapeState(int id, double x, double y, double radius, double startAngleDeg,
                                boolean powered, RotationRate rotationRate,
                                Integer slaveContactId, int divisions,
                                List<Double> radialMultipliers) {
            this(id, x, y, radius, startAngleDeg, powered, rotationRate, slaveContactId,
                    divisions, DivisionDistribution.EQUAL_LENGTH, radialMultipliers);
        }

        public static RadialShapeState harmonic(int id, double x, double y, double radius,
                                                int samples, double variation) {
            if (samples < MIN_RADIAL_SAMPLES || samples > MAX_RADIAL_SAMPLES) {
                throw new IllegalArgumentException("radial sample count out of range");
            }
            if (!Double.isFinite(variation) || variation < 0 || variation >= 0.85) {
                throw new IllegalArgumentException("variation must be in [0, 0.85)");
            }
            var values = new ArrayList<Double>(samples);
            int lobes = Math.max(2, Math.min(6, samples / 2));
            for (int i = 0; i < samples; i++) {
                values.add(1.0 + variation * Math.cos(Math.TAU * lobes * i / samples));
            }
            return new RadialShapeState(id, x, y, radius, 0, false,
                    new RotationRate(0, RotationRateUnit.DEGREES_PER_TICK),
                    null, 37, DivisionDistribution.EQUAL_LENGTH, values);
        }

        @Override public ShapeType shapeType() { return ShapeType.RADIAL; }

        @Override public double boundaryRadius(double localAngleRad) {
            double phase = positiveRadians(localAngleRad) / Math.TAU * radialMultipliers.size();
            int lower = (int) Math.floor(phase) % radialMultipliers.size();
            int upper = (lower + 1) % radialMultipliers.size();
            double fraction = phase - Math.floor(phase);
            return radius * lerp(radialMultipliers.get(lower), radialMultipliers.get(upper), fraction);
        }

        @Override public double boundaryProgression(double localAngleRad) {
            int count = radialMultipliers.size();
            double phase = positiveRadians(localAngleRad) / Math.TAU * count;
            int lower = (int) Math.floor(phase) % count;
            int upper = (lower + 1) % count;
            double r = boundaryRadius(localAngleRad);
            double drDTheta = radius * (radialMultipliers.get(upper) - radialMultipliers.get(lower))
                    * count / Math.TAU;
            double progression = Math.hypot(r, drDTheta);
            return Double.isFinite(progression) && progression > 0 ? progression : MIN_RADIUS;
        }

        public int radialSampleCount() { return radialMultipliers.size(); }

        public double variation() {
            double min = radialMultipliers.stream().mapToDouble(Double::doubleValue).min().orElse(1);
            double max = radialMultipliers.stream().mapToDouble(Double::doubleValue).max().orElse(1);
            return (max - min) / 2.0;
        }

        public RadialShapeState withSampleCount(int count) {
            return harmonic(id, x, y, radius, count, Math.min(variation(), 0.84))
                    .withStartAngle(startAngleDeg)
                    .withPower(powered)
                    .withRotationRate(rotationRate)
                    .withSlave(slaveContactId)
                    .withDivisions(divisions)
                    .withDivisionDistribution(divisionDistribution);
        }

        public RadialShapeState withVariation(double variation) {
            return harmonic(id, x, y, radius, radialSampleCount(), variation)
                    .withStartAngle(startAngleDeg)
                    .withPower(powered)
                    .withRotationRate(rotationRate)
                    .withSlave(slaveContactId)
                    .withDivisions(divisions)
                    .withDivisionDistribution(divisionDistribution);
        }

        public RadialShapeState withRadialRadii(List<Double> radii) {
            Objects.requireNonNull(radii, "radii");
            var multipliers = radii.stream().map(value -> {
                if (value == null || !Double.isFinite(value) || value <= 0) {
                    throw new IllegalArgumentException("radial values must be finite and positive");
                }
                return value / radius;
            }).toList();
            return new RadialShapeState(id, x, y, radius, startAngleDeg, powered, rotationRate,
                    slaveContactId, divisions, divisionDistribution, multipliers);
        }

        public List<Double> radialRadii() {
            return radialMultipliers.stream().map(value -> value * radius).toList();
        }

        @Override public RadialShapeState withPosition(double nx, double ny) {
            return new RadialShapeState(id, nx, ny, radius, startAngleDeg, powered, rotationRate,
                    slaveContactId, divisions, divisionDistribution, radialMultipliers);
        }
        @Override public RadialShapeState withRadius(double r) {
            return new RadialShapeState(id, x, y, r, startAngleDeg, powered, rotationRate,
                    slaveContactId, divisions, divisionDistribution, radialMultipliers);
        }
        @Override public RadialShapeState withStartAngle(double a) {
            return new RadialShapeState(id, x, y, radius, a, powered, rotationRate,
                    slaveContactId, divisions, divisionDistribution, radialMultipliers);
        }
        @Override public RadialShapeState withPower(boolean p) {
            return new RadialShapeState(id, x, y, radius, startAngleDeg, p, rotationRate,
                    p ? null : slaveContactId, divisions, divisionDistribution, radialMultipliers);
        }
        @Override public RadialShapeState withRotationRate(RotationRate rate) {
            return new RadialShapeState(id, x, y, radius, startAngleDeg, powered, rate,
                    slaveContactId, divisions, divisionDistribution, radialMultipliers);
        }
        @Override public RadialShapeState withSlave(Integer contactId) {
            return new RadialShapeState(id, x, y, radius, startAngleDeg, false, rotationRate,
                    contactId, divisions, divisionDistribution, radialMultipliers);
        }
        @Override public RadialShapeState withDivisions(int count) {
            return new RadialShapeState(id, x, y, radius, startAngleDeg, powered, rotationRate,
                    slaveContactId, count, divisionDistribution, radialMultipliers);
        }
        @Override public RadialShapeState withDivisionDistribution(DivisionDistribution distribution) {
            return new RadialShapeState(id, x, y, radius, startAngleDeg, powered, rotationRate,
                    slaveContactId, divisions, distribution, radialMultipliers);
        }
    }

    public record ContactState(int id, int aId, int bId, Tangency type,
                               double aTouchDeg, double bTouchDeg,
                               ContactFollowMode followMode, double aToBBearingDeg) {
        public ContactState {
            if (id <= 0 || aId <= 0 || bId <= 0 || aId == bId) {
                throw new IllegalArgumentException("invalid contact");
            }
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(followMode, "followMode");
            if (!Double.isFinite(aTouchDeg) || !Double.isFinite(bTouchDeg)
                    || !Double.isFinite(aToBBearingDeg)) {
                throw new IllegalArgumentException("contact angles must be finite");
            }
            aToBBearingDeg = normalize(aToBBearingDeg);
        }

        public ContactState(int id, int aId, int bId, Tangency type,
                            double aTouchDeg, double bTouchDeg) {
            this(id, aId, bId, type, aTouchDeg, bTouchDeg,
                    ContactFollowMode.NO_OVERLAP_SWITCH_CONTACT, 0);
        }

        public int other(int shapeId) {
            if (shapeId == aId) return bId;
            if (shapeId == bId) return aId;
            throw new IllegalArgumentException("shape is not part of contact");
        }

        public double touchFor(int shapeId) {
            if (shapeId == aId) return aTouchDeg;
            if (shapeId == bId) return bTouchDeg;
            throw new IllegalArgumentException("shape is not part of contact");
        }

        public ContactState withTouchFor(int shapeId, double degrees) {
            if (shapeId == aId) return new ContactState(id, aId, bId, type, degrees, bTouchDeg, followMode, aToBBearingDeg);
            if (shapeId == bId) return new ContactState(id, aId, bId, type, aTouchDeg, degrees, followMode, aToBBearingDeg);
            throw new IllegalArgumentException("shape is not part of contact");
        }

        public ContactState withFollowMode(ContactFollowMode mode) {
            return new ContactState(id, aId, bId, type, aTouchDeg, bTouchDeg, mode, aToBBearingDeg);
        }

        public double bearingFrom(int sourceShapeId) {
            if (sourceShapeId == aId) return aToBBearingDeg;
            if (sourceShapeId == bId) return normalize(aToBBearingDeg + 180.0);
            throw new IllegalArgumentException("shape is not part of contact");
        }
    }

    public record WorkspaceData(List<WorkspaceShape> shapes, List<ContactState> contacts,
                                long tick, double ticksPerSecond,
                                double globalMarkerLength) {
        public WorkspaceData {
            shapes = List.copyOf(shapes == null ? List.of() : shapes);
            contacts = List.copyOf(contacts == null ? List.of() : contacts);
            if (!Double.isFinite(ticksPerSecond) || ticksPerSecond <= 0) {
                throw new IllegalArgumentException("ticksPerSecond must be positive");
            }
            if (!Double.isFinite(globalMarkerLength) || globalMarkerLength <= 0) {
                throw new IllegalArgumentException("globalMarkerLength must be positive");
            }
        }

        public WorkspaceData(List<WorkspaceShape> shapes, List<ContactState> contacts,
                             long tick, double ticksPerSecond) {
            this(shapes, contacts, tick, ticksPerSecond, DEFAULT_MARKER_LENGTH);
        }

        /** Convenience for legacy callers that still think in circles. */
        public List<CircleState> circles() {
            return shapes.stream().filter(CircleState.class::isInstance)
                    .map(CircleState.class::cast).toList();
        }
    }

    public static void requireDivisions(int divisions) {
        if (divisions < MIN_DIVISIONS || divisions > MAX_DIVISIONS) {
            throw new IllegalArgumentException("division count out of range");
        }
    }

    public static double normalize(double degrees) {
        degrees %= 360.0;
        return degrees < 0 ? degrees + 360.0 : degrees;
    }

    private static double positiveRadians(double radians) {
        radians %= Math.TAU;
        return radians < 0 ? radians + Math.TAU : radians;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static void validateCommon(int id, double x, double y, double radius,
                                       double angle, RotationRate rate, int divisions) {
        if (id <= 0) throw new IllegalArgumentException("id must be positive");
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("position must be finite");
        if (!Double.isFinite(radius) || radius < MIN_RADIUS) throw new IllegalArgumentException("radius must be positive");
        if (!Double.isFinite(angle)) throw new IllegalArgumentException("angle must be finite");
        Objects.requireNonNull(rate, "rotationRate");
        requireDivisions(divisions);
    }
}
