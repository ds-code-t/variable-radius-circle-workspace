package com.example.circleworkspace;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static com.example.circleworkspace.Model.*;

public final class WorkspaceStore {
    private final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    public WorkspaceData load(Path path) throws IOException {
        return decode(mapper.readTree(path.toFile()));
    }

    public void save(Path path, WorkspaceData data) throws IOException {
        mapper.writeValue(path.toFile(), encode(data));
    }

    public WorkspaceData loadResource(String name) throws IOException {
        try (InputStream in = WorkspaceStore.class.getResourceAsStream(name)) {
            if (in == null) throw new FileNotFoundException(name);
            return decode(mapper.readTree(in));
        }
    }

    ObjectNode encode(WorkspaceData data) {
        ObjectNode root = mapper.createObjectNode();
        root.put("version", 4);
        ArrayNode shapes = root.putArray("shapes");
        for (var shape : data.shapes()) {
            ObjectNode node = shapes.addObject();
            node.put("shapeType", shape.shapeType().name());
            node.put("id", shape.id());
            node.put("x", shape.x());
            node.put("y", shape.y());
            node.put("radius", shape.radius());
            node.put("startAngleDeg", shape.startAngleDeg());
            node.put("powered", shape.powered());
            node.put("rotationRate", shape.rotationRate().value());
            node.put("rotationRateUnit", shape.rotationRate().unit().name());
            if (shape.slaveContactId() == null) node.putNull("slaveContactId");
            else node.put("slaveContactId", shape.slaveContactId());
            node.put("circumferenceUnits", shape.divisions());
            node.put("divisionDistribution", shape.divisionDistribution().name());
            if (shape instanceof RadialShapeState radial) {
                ArrayNode profile = node.putArray("radialMultipliers");
                radial.radialMultipliers().forEach(profile::add);
            }
        }
        root.set("contacts", mapper.valueToTree(data.contacts()));
        root.put("tick", data.tick());
        root.put("ticksPerSecond", data.ticksPerSecond());
        root.put("globalMarkerLength", data.globalMarkerLength());
        return root;
    }

    WorkspaceData decode(JsonNode root) throws IOException {
        ArrayNode shapeNodes;
        boolean legacy = !root.has("shapes") && root.has("circles");
        JsonNode candidate = legacy ? root.get("circles") : root.get("shapes");
        if (!(candidate instanceof ArrayNode array)) throw new IOException("workspace requires shapes or legacy circles");
        shapeNodes = array;

        var shapes = new ArrayList<WorkspaceShape>();
        for (JsonNode node : shapeNodes) shapes.add(decodeShape(node, legacy));

        var contacts = new ArrayList<ContactState>();
        JsonNode contactsNode = root.path("contacts");
        if (contactsNode.isArray()) {
            for (JsonNode node : contactsNode) {
                try {
                    int aId = requiredInt(node, "aId");
                    int bId = requiredInt(node, "bId");
                    WorkspaceShape a = shapes.stream().filter(shape -> shape.id() == aId).findFirst().orElse(null);
                    WorkspaceShape b = shapes.stream().filter(shape -> shape.id() == bId).findFirst().orElse(null);
                    double bearing = node.has("aToBBearingDeg")
                            ? requiredDouble(node, "aToBBearingDeg")
                            : a != null && b != null
                                ? Math.toDegrees(Math.atan2(b.y() - a.y(), b.x() - a.x()))
                                : 0;
                    ContactFollowMode mode = node.has("followMode")
                            ? ContactFollowMode.valueOf(requiredText(node, "followMode"))
                            : ContactFollowMode.NO_OVERLAP_SWITCH_CONTACT;
                    contacts.add(new ContactState(
                            requiredInt(node, "id"), aId, bId,
                            Tangency.valueOf(requiredText(node, "type")),
                            requiredDouble(node, "aTouchDeg"),
                            requiredDouble(node, "bTouchDeg"),
                            mode, bearing));
                } catch (RuntimeException ex) {
                    throw new IOException("invalid contact", ex);
                }
            }
        }
        long tick = root.path("tick").asLong(0);
        double ticksPerSecond = root.path("ticksPerSecond").asDouble(2);
        double globalMarkerLength = root.path("globalMarkerLength")
                .asDouble(DEFAULT_MARKER_LENGTH);
        try {
            return new WorkspaceData(shapes, contacts, tick, ticksPerSecond,
                    globalMarkerLength);
        } catch (IllegalArgumentException ex) {
            throw new IOException("invalid workspace", ex);
        }
    }

    private WorkspaceShape decodeShape(JsonNode node, boolean legacy) throws IOException {
        try {
            ShapeType type = legacy || !node.has("shapeType")
                    ? ShapeType.CIRCLE : ShapeType.valueOf(requiredText(node, "shapeType"));
            int id = requiredInt(node, "id");
            double x = requiredDouble(node, "x");
            double y = requiredDouble(node, "y");
            double radius = requiredDouble(node, "radius");
            double angle = node.path("startAngleDeg").asDouble(0);
            boolean powered = node.path("powered").asBoolean(false);
            Integer slave = node.path("slaveContactId").isIntegralNumber()
                    ? node.get("slaveContactId").intValue() : null;
            int divisions;
            if (node.has("circumferenceUnits")) {
                divisions = requiredInt(node, "circumferenceUnits");
            } else if (node.has("divisions")) {
                // Versions 1-3 stored the number of visible lines. Version 4
                // stores unit points, including the closing endpoint.
                divisions = Math.addExact(requiredInt(node, "divisions"), 1);
            } else {
                divisions = Math.max(4, (int) Math.round(Math.TAU * radius)) + 1;
            }

            DivisionDistribution distribution = node.has("divisionDistribution")
                    ? DivisionDistribution.valueOf(requiredText(node, "divisionDistribution"))
                    : DivisionDistribution.EQUAL_LENGTH;

            RotationRate rate;
            if (node.has("rotationRate")) {
                rate = new RotationRate(requiredDouble(node, "rotationRate"),
                        RotationRateUnit.valueOf(requiredText(node, "rotationRateUnit")));
            } else {
                rate = new RotationRate(node.path("ownRateDegPerTick").asDouble(0),
                        RotationRateUnit.DEGREES_PER_TICK);
            }

            if (type == ShapeType.CIRCLE) {
                return new CircleState(id, x, y, radius, angle, powered, rate, slave, divisions, distribution);
            }

            JsonNode profile = node.get("radialMultipliers");
            if (!(profile instanceof ArrayNode values)) throw new IOException("radial shape requires radialMultipliers");
            var multipliers = new ArrayList<Double>();
            values.forEach(value -> multipliers.add(value.doubleValue()));
            return new RadialShapeState(id, x, y, radius, angle, powered, rate, slave, divisions, distribution, multipliers);
        } catch (IllegalArgumentException ex) {
            throw new IOException("invalid shape", ex);
        }
    }

    private static int requiredInt(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber()) throw new IOException("missing integer " + field);
        return value.intValue();
    }

    private static double requiredDouble(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) throw new IOException("missing number " + field);
        return value.doubleValue();
    }

    private static String requiredText(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) throw new IOException("missing text " + field);
        return value.textValue();
    }
}
