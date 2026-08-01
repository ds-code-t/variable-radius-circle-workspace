package com.example.circleworkspace;

import java.util.*;

public final class Model {
    private Model() {}
    public enum Tangency { EXTERNAL, INTERNAL }
    public enum DriveMode { STOPPED, POWERED, SLAVED }

    public record CircleState(int id, double x, double y, double radius, double startAngleDeg,
                              boolean powered, double ownRateDegPerTick, Integer slaveContactId) {
        public CircleState {
            if (id <= 0) throw new IllegalArgumentException("id must be positive");
            if (!Double.isFinite(radius) || radius <= 0) throw new IllegalArgumentException("radius must be positive");
        }
        public double circumference() { return Math.TAU * radius; }
        public double diameter() { return radius * 2.0; }
        public CircleState withPosition(double nx, double ny) { return new CircleState(id,nx,ny,radius,startAngleDeg,powered,ownRateDegPerTick,slaveContactId); }
        public CircleState withRadius(double r) { return new CircleState(id,x,y,r,startAngleDeg,powered,ownRateDegPerTick,slaveContactId); }
        public CircleState withStartAngle(double a) { return new CircleState(id,x,y,radius,a,powered,ownRateDegPerTick,slaveContactId); }
        public CircleState withPower(boolean p) { return new CircleState(id,x,y,radius,startAngleDeg,p,ownRateDegPerTick,p?null:slaveContactId); }
        public CircleState withRate(double r) { return new CircleState(id,x,y,radius,startAngleDeg,powered,r,slaveContactId); }
        public CircleState withSlave(Integer c) { return new CircleState(id,x,y,radius,startAngleDeg,false,ownRateDegPerTick,c); }
    }

    public record ContactState(int id, int aId, int bId, Tangency type, double aTouchDeg, double bTouchDeg) {
        public ContactState {
            if (id <= 0 || aId <= 0 || bId <= 0 || aId == bId) throw new IllegalArgumentException("invalid contact");
            Objects.requireNonNull(type);
        }
        public int other(int circleId) { return circleId == aId ? bId : aId; }
        public double touchFor(int circleId) { return circleId == aId ? aTouchDeg : bTouchDeg; }
        public ContactState withTouchFor(int circleId, double deg) {
            return circleId == aId ? new ContactState(id,aId,bId,type,deg,bTouchDeg)
                    : new ContactState(id,aId,bId,type,aTouchDeg,deg);
        }
    }

    public record WorkspaceData(List<CircleState> circles, List<ContactState> contacts, long tick, double ticksPerSecond) {
        public WorkspaceData {
            circles = List.copyOf(circles == null ? List.of() : circles);
            contacts = List.copyOf(contacts == null ? List.of() : contacts);
        }
    }
}
