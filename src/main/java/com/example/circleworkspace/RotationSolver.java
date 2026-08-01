package com.example.circleworkspace;

import java.util.*;
import static com.example.circleworkspace.Model.*;

public final class RotationSolver {
    public record Result(double rateDegPerTick, int depth, DriveMode mode, Integer sourceCircleId) {}

    public Map<Integer, Result> solve(List<CircleState> circles, List<ContactState> contacts) {
        var byCircle = new HashMap<Integer,CircleState>();
        circles.forEach(c -> byCircle.put(c.id(), c));
        var byContact = new HashMap<Integer,ContactState>();
        contacts.forEach(c -> byContact.put(c.id(), c));
        var memo = new HashMap<Integer,Result>();
        for (var c : circles) resolve(c.id(), byCircle, byContact, memo, new LinkedHashSet<>());
        return Map.copyOf(memo);
    }

    private Result resolve(int id, Map<Integer,CircleState> circles, Map<Integer,ContactState> contacts,
                           Map<Integer,Result> memo, Set<Integer> path) {
        if (memo.containsKey(id)) return memo.get(id);
        var circle = circles.get(id);
        if (circle == null) return new Result(0,0,DriveMode.STOPPED,null);
        if (circle.powered()) {
            var r = new Result(circle.ownRateDegPerTick(),0,DriveMode.POWERED,id);
            memo.put(id,r); return r;
        }
        if (circle.slaveContactId() == null || !path.add(id)) {
            var r = new Result(0,0,DriveMode.STOPPED,null);
            memo.put(id,r); return r;
        }
        var contact = contacts.get(circle.slaveContactId());
        if (contact == null || (contact.aId()!=id && contact.bId()!=id)) {
            var r = new Result(0,0,DriveMode.STOPPED,null);
            memo.put(id,r); return r;
        }
        int parentId = contact.other(id);
        var parent = circles.get(parentId);
        var parentResult = resolve(parentId,circles,contacts,memo,path);
        path.remove(id);
        if (parent == null || parentResult.mode()==DriveMode.STOPPED) {
            var r = new Result(0,0,DriveMode.STOPPED,null);
            memo.put(id,r); return r;
        }
        double sign = contact.type()==Tangency.EXTERNAL ? -1.0 : 1.0;
        double rate = sign * parentResult.rateDegPerTick() * parent.radius()/circle.radius();
        var r = new Result(rate,parentResult.depth()+1,DriveMode.SLAVED,parentId);
        memo.put(id,r); return r;
    }
}
