package com.example.circleworkspace;

import java.awt.Shape;
import java.awt.geom.*;
import java.util.*;

import static com.example.circleworkspace.Model.*;

public final class ContactFollower {
    private static final int NEWTON_STEPS = 6;
    private static final int COLLISION_SUBDIVISIONS = 8;
    private static final int COLLISION_BISECTION_STEPS = 28;

    public Map<Integer, WorkspaceShape> solve(List<? extends WorkspaceShape> shapes,
                                              List<ContactState> contacts,
                                              long tick,
                                              Map<Integer, RotationSolver.Result> rotations) {
        var original = new LinkedHashMap<Integer, WorkspaceShape>();
        shapes.forEach(shape -> original.put(shape.id(), shape));
        var byContact = new HashMap<Integer, ContactState>();
        contacts.forEach(contact -> byContact.put(contact.id(), contact));

        var ordered = new ArrayList<>(shapes);
        ordered.sort(Comparator.comparingInt(shape ->
                rotations.getOrDefault(shape.id(),
                        new RotationSolver.Result(0, 0, DriveMode.STOPPED, null)).depth()));

        var posed = new LinkedHashMap<Integer, WorkspaceShape>();
        for (var shape : ordered) {
            WorkspaceShape result = shape;
            Integer contactId = shape.slaveContactId();
            if (contactId != null) {
                var contact = byContact.get(contactId);
                if (contact != null && (contact.aId() == shape.id() || contact.bId() == shape.id())) {
                    int driverId = contact.other(shape.id());
                    WorkspaceShape driver = posed.getOrDefault(driverId, original.get(driverId));
                    if (driver != null) result = follow(driver, shape, contact, tick, rotations);
                }
            }
            posed.put(shape.id(), result);
        }
        return Map.copyOf(posed);
    }

    private WorkspaceShape follow(WorkspaceShape driver,
                                  WorkspaceShape driven,
                                  ContactState contact,
                                  long tick,
                                  Map<Integer, RotationSolver.Result> rotations) {
        double bearingDeg = contact.bearingFrom(driver.id());
        double bearingRad = Math.toRadians(bearingDeg);
        double separation = separation(driver, driven, contact, bearingDeg, tick, rotations);
        return driven.withPosition(
                driver.x() + Math.cos(bearingRad) * separation,
                driver.y() + Math.sin(bearingRad) * separation);
    }

    private double separation(WorkspaceShape driver,
                              WorkspaceShape driven,
                              ContactState contact,
                              double bearingDeg,
                              long tick,
                              Map<Integer, RotationSolver.Result> rotations) {
        double driverAngle = displayedAngle(driver, rotations.get(driver.id()), tick);
        double drivenAngle = displayedAngle(driven, rotations.get(driven.id()), tick);

        // Contact bearings use screen-space center-line angles: 0° is right and
        // 90° is down. Shape boundary angles use the renderer convention:
        // 0° is up and 90° is right.
        double driverRayDeg = Model.normalize(bearingDeg + 90.0);
        double drivenRayDeg = Model.normalize(driverRayDeg + 180.0);

        if (contact.type() == Tangency.INTERNAL) {
            double outer = radiusOnRay(driver, driverRayDeg, driverAngle);
            double inner = radiusOnRay(driven, drivenRayDeg, drivenAngle);
            return Math.max(0, outer - inner);
        }

        if (contact.followMode() == ContactFollowMode.FIXED_CONTACT_ALLOW_OVERLAP) {
            return radiusOnRay(driver, driverRayDeg, driverAngle)
                    + radiusOnRay(driven, drivenRayDeg, drivenAngle);
        }

        double retainedContactSeparation =
                radiusOnRay(driver, driverRayDeg, driverAngle)
                        + radiusOnRay(driven, drivenRayDeg, drivenAngle);

        var collision = CollisionPair.create(
                driver, driven, driverAngle, drivenAngle, bearingDeg);

        // Preserve the existing point contact for as long as it does not cause
        // actual boundary overlap. This prevents distant protrusions from
        // "reserving" space before they reach the other shape.
        if (!collision.overlaps(retainedContactSeparation)) {
            return retainedContactSeparation;
        }

        // A protrusion has crossed the retained-contact configuration. Move only
        // far enough along the contact line to reach the first non-overlapping
        // configuration, which becomes the new rolling contact.
        double lower = retainedContactSeparation;
        double upper = Math.max(lower,
                support(driver, driverRayDeg, driverAngle)
                        + support(driven, drivenRayDeg, drivenAngle));

        double expansion = Math.max(1.0, driver.radius() + driven.radius());
        for (int i = 0; i < 8 && collision.overlaps(upper); i++) {
            upper += expansion;
            expansion *= 2.0;
        }

        if (collision.overlaps(upper)) {
            return upper;
        }

        for (int i = 0; i < COLLISION_BISECTION_STEPS; i++) {
            double middle = (lower + upper) * 0.5;
            if (collision.overlaps(middle)) {
                lower = middle;
            } else {
                upper = middle;
            }
        }
        return upper;
    }


    /**
     * Polygonal collision proxy used only to decide when the retained contact
     * must switch. It follows the same deterministic radial boundary function
     * as rendering, with bounded subdivisions per radial interval.
     */
    private record CollisionPair(Area driverArea, Shape drivenAtOrigin) {
        static CollisionPair create(WorkspaceShape driver,
                                    WorkspaceShape driven,
                                    double driverAngleDeg,
                                    double drivenAngleDeg,
                                    double bearingDeg) {
            return new CollisionPair(
                    new Area(boundaryPath(driver, driverAngleDeg, bearingDeg)),
                    boundaryPath(driven, drivenAngleDeg, bearingDeg));
        }

        boolean overlaps(double separation) {
            Area driven = new Area(drivenAtOrigin);
            driven.transform(AffineTransform.getTranslateInstance(separation, 0));
            if (!driverArea.getBounds2D().intersects(driven.getBounds2D())) {
                return false;
            }
            Area intersection = new Area(driverArea);
            intersection.intersect(driven);
            return !intersection.isEmpty();
        }

        private static Path2D.Double boundaryPath(WorkspaceShape shape,
                                                  double displayedAngleDeg,
                                                  double bearingDeg) {
            int segments = shape instanceof CircleState
                    ? 128
                    : Math.max(96, Math.min(16_384,
                            ((RadialShapeState) shape).radialSampleCount()
                                    * COLLISION_SUBDIVISIONS));

            double bearingRad = Math.toRadians(bearingDeg);
            double cosBearing = Math.cos(bearingRad);
            double sinBearing = Math.sin(bearingRad);
            double displayedRad = Math.toRadians(displayedAngleDeg);
            var path = new Path2D.Double(Path2D.WIND_NON_ZERO, segments);

            for (int i = 0; i < segments; i++) {
                double local = Math.TAU * i / segments;
                double radius = shape.boundaryRadius(local);
                double world = local + displayedRad;
                double screenX = Math.sin(world) * radius;
                double screenY = -Math.cos(world) * radius;

                // Rotate into contact-line coordinates, where +x points from the
                // driver center toward the driven center.
                double x = cosBearing * screenX + sinBearing * screenY;
                double y = -sinBearing * screenX + cosBearing * screenY;
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            path.closePath();
            return path;
        }
    }

    private static double radiusOnRay(WorkspaceShape shape,
                                      double worldDirectionDeg,
                                      double displayedAngleDeg) {
        return shape.boundaryRadius(
                shape.localAngleForWorldDirection(worldDirectionDeg, displayedAngleDeg));
    }

    /**
     * Maximum projection on a world-space direction. Circles are exact. A radial
     * profile is piecewise linear in polar radius, so each profile interval is
     * maximized independently using its endpoints and a bounded Newton solve.
     */
    private static double support(WorkspaceShape shape,
                                  double worldDirectionDeg,
                                  double displayedAngleDeg) {
        if (shape instanceof CircleState) return shape.radius();
        var radial = (RadialShapeState) shape;
        var values = radial.radialMultipliers();
        int count = values.size();
        double interval = Math.TAU / count;
        double offset = Math.toRadians(displayedAngleDeg - worldDirectionDeg);
        double maximum = 0;

        for (int i = 0; i < count; i++) {
            double r0 = radial.radius() * values.get(i);
            double r1 = radial.radius() * values.get((i + 1) % count);
            double slope = r1 - r0;
            double theta0 = i * interval;
            maximum = Math.max(maximum, projection(r0, theta0 + offset));
            maximum = Math.max(maximum, projection(r1, theta0 + interval + offset));

            double t = .5;
            for (int step = 0; step < NEWTON_STEPS; step++) {
                double r = r0 + slope * t;
                double phase = theta0 + interval * t + offset;
                double first = slope * Math.cos(phase) - interval * r * Math.sin(phase);
                double second = -2 * slope * interval * Math.sin(phase)
                        - interval * interval * r * Math.cos(phase);
                if (Math.abs(second) < 1e-12) break;
                t = Math.clamp(t - first / second, 0.0, 1.0);
            }
            double r = r0 + slope * t;
            maximum = Math.max(maximum,
                    projection(r, theta0 + interval * t + offset));
        }
        return maximum;
    }

    private static double projection(double radius, double relativeAngle) {
        return radius * Math.cos(relativeAngle);
    }

    private static double displayedAngle(WorkspaceShape shape,
                                         RotationSolver.Result result,
                                         long tick) {
        return shape.displayedAngle(tick, result == null ? 0 : result.rateDegPerTick());
    }
}
