package com.example.circleworkspace;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.*;

import java.util.*;

import static com.example.circleworkspace.Model.*;

public final class CircleWorkspaceApp extends Application {
    private static final DataFormat SHAPE_FORMAT = new DataFormat("application/x-circle-workspace-shape");
    private static final double SNAP_TOLERANCE = 18;
    private static final double SHAPE_DRAG_THRESHOLD_PIXELS = 4;

    private final ObservableList<WorkspaceShape> shapes = FXCollections.observableArrayList();
    private final ObservableList<ContactState> contacts = FXCollections.observableArrayList();
    private final RotationSolver solver = new RotationSolver();
    private final ContactFollower contactFollower = new ContactFollower();
    private final WorkspaceStore store = new WorkspaceStore();
    private final RotationTelemetry rotationTelemetry = new RotationTelemetry();
    private final LongProperty tick = new SimpleLongProperty();
    private final DoubleProperty ticksPerSecond = new SimpleDoubleProperty(2);
    private final DoubleProperty globalMarkerLength = new SimpleDoubleProperty(DEFAULT_MARKER_LENGTH);
    private final BooleanProperty running = new SimpleBooleanProperty();
    private final IntegerProperty selectedId = new SimpleIntegerProperty(-1);

    private Canvas canvas;
    private VBox inspector;
    private Label telemetryTickLabel;
    private Label telemetryStepLabel;
    private Label telemetryTotalLabel;
    private Label telemetryTurnsLabel;
    private WorkspaceData starting;
    private final WorkspaceCamera camera = new WorkspaceCamera();
    private WorkspaceShape dragged;
    private boolean shapeDragStarted;
    private boolean panning;
    private double panLastX;
    private double panLastY;
    private double pointerPressX;
    private double pointerPressY;
    private double dragOffsetX;
    private double dragOffsetY;
    private int nextShapeId = 1;
    private int nextContactId = 1;
    private long lastNanos;

    @Override
    public void start(Stage stage) throws Exception {
        starting = store.loadResource("/workspace.json");
        applyData(starting);

        canvas = new Canvas(1000, 760);
        var canvasPane = new StackPane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());

        inspector = new VBox(9);
        inspector.setPadding(new Insets(12));
        inspector.setPrefWidth(330);
        var inspectorScroll = new ScrollPane(inspector);
        inspectorScroll.setFitToWidth(true);

        var root = new BorderPane(canvasPane, toolbar(stage), inspectorScroll, null, palette());
        var scene = new Scene(root, 1450, 900);
        stage.setTitle("General Geometric-Computation Workspace");
        stage.setScene(scene);
        stage.show();

        installCanvasHandlers(scene);
        shapes.addListener((ListChangeListener<WorkspaceShape>) change -> refresh());
        contacts.addListener((ListChangeListener<ContactState>) change -> refresh());
        selectedId.addListener((obs, oldValue, newValue) -> rebuildInspector());
        tick.addListener(obs -> {
            redraw();
            updateTelemetry();
        });
        refresh();

        new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNanos == 0) lastNanos = now;
                if (running.get()) {
                    long interval = Math.max(1L,
                            Math.round(1_000_000_000.0 / ticksPerSecond.get()));
                    long elapsed = now - lastNanos;
                    if (elapsed >= interval) {
                        long steps = elapsed / interval;
                        tick.set(tick.get() + steps);
                        lastNanos += steps * interval;
                    }
                } else {
                    lastNanos = now;
                }
            }
        }.start();
    }

    private Node toolbar(Stage stage) {
        var bar = new HBox(8);
        bar.setPadding(new Insets(8));
        bar.setAlignment(Pos.CENTER_LEFT);
        var run = new ToggleButton("Run");
        run.selectedProperty().bindBidirectional(running);
        run.textProperty().bind(running.map(value -> value ? "Pause" : "Run"));

        var duplicate = new Button("Duplicate");
        duplicate.setOnAction(event -> duplicateSelected());
        var delete = new Button("Delete");
        delete.setOnAction(event -> deleteSelected());
        var save = new Button("Save…");
        save.setOnAction(event -> save(stage));
        var load = new Button("Load…");
        load.setOnAction(event -> load(stage));
        var reset = new Button("Reset start");
        reset.setOnAction(event -> applyData(starting));
        var resetView = new Button("Reset view");
        resetView.setOnAction(event -> {
            camera.reset();
            redraw();
        });
        var markerLengthField = doubleField(globalMarkerLength.get(), this::setGlobalMarkerLength);
        globalMarkerLength.addListener((obs, oldValue, newValue) ->
                markerLengthField.setText(format(newValue.doubleValue())));

        bar.getChildren().addAll(run, new Label("Tick"), boundLongField(tick),
                new Label("ticks/sec"), doubleField(ticksPerSecond.get(),
                        value -> ticksPerSecond.set(Math.max(.05, value))),
                new Label("global line length"), markerLengthField,
                new Separator(), duplicate, delete, reset, resetView, save, load);
        return bar;
    }

    private Node palette() {
        var box = new VBox(10);
        box.setPadding(new Insets(12));
        box.setPrefWidth(205);
        box.setStyle("-fx-background-color: #eef2f8; -fx-border-color: #c9d2df;");
        var title = new Label("Shape Palette");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        box.getChildren().addAll(title,
                paletteButton("Circle", ShapeType.CIRCLE),
                paletteButton("Multi-radius radial", ShapeType.RADIAL),
                new Label("Drag a prototype onto the workspace, or click to add at the center."));
        return box;
    }

    private Button paletteButton(String text, ShapeType type) {
        var button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> {
            var center = camera.toWorld(canvas.getWidth() / 2, canvas.getHeight() / 2);
            createShape(type, center.x(), center.y());
        });
        button.setOnDragDetected(event -> {
            var content = new ClipboardContent();
            content.put(SHAPE_FORMAT, type.name());
            var board = button.startDragAndDrop(TransferMode.COPY);
            board.setContent(content);
            event.consume();
        });
        return button;
    }

    private void installCanvasHandlers(Scene scene) {
        canvas.setFocusTraversable(true);
        canvas.setOnDragOver(event -> {
            if (event.getDragboard().hasContent(SHAPE_FORMAT)) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        canvas.setOnDragDropped(event -> {
            Object value = event.getDragboard().getContent(SHAPE_FORMAT);
            if (value != null) {
                var world = camera.toWorld(event.getX(), event.getY());
                createShape(ShapeType.valueOf(value.toString()), world.x(), world.y());
            }
            event.setDropCompleted(value != null);
            event.consume();
        });
        canvas.setOnScroll(event -> {
            camera.zoomAt(event.getX(), event.getY(), Math.pow(1.0015, event.getDeltaY()));
            redraw();
            event.consume();
        });
        canvas.setOnMousePressed(event -> {
            canvas.requestFocus();
            var world = camera.toWorld(event.getX(), event.getY());
            var hit = hit(world.x(), world.y());

            dragged = null;
            shapeDragStarted = false;
            panning = false;
            pointerPressX = event.getX();
            pointerPressY = event.getY();

            if (event.getButton() == MouseButton.SECONDARY) {
                selectedId.set(hit == null ? -1 : hit.id());
                event.consume();
                return;
            }

            if (event.getButton() == MouseButton.MIDDLE
                    || (event.getButton() == MouseButton.PRIMARY && hit == null)) {
                panning = true;
                panLastX = event.getX();
                panLastY = event.getY();
                if (event.getButton() == MouseButton.PRIMARY) selectedId.set(-1);
                event.consume();
                return;
            }

            if (event.getButton() == MouseButton.PRIMARY && hit != null) {
                selectedId.set(hit.id());
                dragged = hit;
                dragOffsetX = world.x() - hit.x();
                dragOffsetY = world.y() - hit.y();
                event.consume();
            }
        });
        canvas.setOnMouseDragged(event -> {
            if (panning) {
                camera.panBy(event.getX() - panLastX, event.getY() - panLastY);
                panLastX = event.getX();
                panLastY = event.getY();
                redraw();
                event.consume();
                return;
            }
            if (dragged == null || !event.isPrimaryButtonDown()) return;

            if (!shapeDragStarted) {
                double distance = Math.hypot(
                        event.getX() - pointerPressX,
                        event.getY() - pointerPressY);
                if (distance < SHAPE_DRAG_THRESHOLD_PIXELS) return;
                detach(dragged.id(), true);
                dragged = selected();
                shapeDragStarted = dragged != null;
                if (!shapeDragStarted) return;
            }

            var world = camera.toWorld(event.getX(), event.getY());
            replace(dragged.withPosition(world.x() - dragOffsetX, world.y() - dragOffsetY));
            dragged = selected();
            event.consume();
        });
        canvas.setOnMouseReleased(event -> {
            if (shapeDragStarted && dragged != null) snapSelected();
            dragged = null;
            shapeDragStarted = false;
            panning = false;
            event.consume();
        });
        scene.setOnKeyPressed(event -> {
            var shape = selected();
            if (shape == null) return;
            double amount = event.isShiftDown() ? 10 : 1;
            switch (event.getCode()) {
                case LEFT -> move(shape, -amount, 0);
                case RIGHT -> move(shape, amount, 0);
                case UP -> move(shape, 0, -amount);
                case DOWN -> move(shape, 0, amount);
                case DELETE, BACK_SPACE -> deleteSelected();
                default -> { }
            }
        });
    }

    private void createShape(ShapeType type, double x, double y) {
        WorkspaceShape shape = type == ShapeType.CIRCLE
                ? new CircleState(nextShapeId++, x, y, 60, 0, false,
                    new RotationRate(0, RotationRateUnit.DEGREES_PER_TICK), null, 37)
                : RadialShapeState.harmonic(nextShapeId++, x, y, 60, 12, .18);
        shape = normalizeEqualLengthShape(shape);
        shapes.add(shape);
        selectedId.set(shape.id());
    }

    private void rebuildInspector() {
        if (inspector == null) return;
        inspector.getChildren().clear();
        var shape = selected();
        inspector.getChildren().add(new Label(shape == null ? "No shape selected"
                : shape.shapeType() + " #" + shape.id()));
        if (shape == null) return;

        var result = solver.solve(shapes, contacts, tick.get()).get(shape.id());
        addMetric("X", shape.x(), value -> replace(shape.withPosition(value, shape.y())));
        addMetric("Y", shape.y(), value -> replace(shape.withPosition(shape.x(), value)));
        addMetric("Base radius", shape.radius(),
                value -> replace(shape.withRadius(Math.max(MIN_RADIUS, value))),
                shape.divisionDistribution() == DivisionDistribution.EQUAL_LENGTH);
        if (shape.divisionDistribution() == DivisionDistribution.EQUAL_LENGTH) {
            inspector.getChildren().add(new Label("Perimeter target: "
                    + format(shape.lineDivisionCount() * globalMarkerLength.get())
                    + " (" + shape.lineDivisionCount() + " × "
                    + format(globalMarkerLength.get()) + ")"));
        }
        addMetric("Rotation degrees", shape.displayedAngle(tick.get(), result.rateDegPerTick()), value ->
                replace(shape.withStartAngle(normalize(value - result.rateDegPerTick() * tick.get()))));
        addInteger("Circumference units", shape.divisions(),
                value -> replace(shape.withDivisions(Math.clamp(value, MIN_DIVISIONS, MAX_DIVISIONS))));

        var distribution = new ComboBox<DivisionDistribution>();
        distribution.getItems().setAll(DivisionDistribution.values());
        distribution.setValue(shape.divisionDistribution());
        distribution.setOnAction(event ->
                replace(shape.withDivisionDistribution(distribution.getValue())));
        inspector.getChildren().addAll(new Label("Line distribution"), distribution);

        var powered = new CheckBox("Own rotation enabled");
        powered.setSelected(shape.powered());
        powered.setOnAction(event -> replace(shape.withPower(powered.isSelected())));
        inspector.getChildren().add(powered);

        var units = new ComboBox<RotationRateUnit>();
        units.getItems().setAll(RotationRateUnit.values());
        units.setValue(shape.rotationRate().unit());
        units.setOnAction(event -> replace(shape.withRotationRate(
                shape.rotationRate().convertedTo(units.getValue(), shape.lineDivisionCount()))));
        inspector.getChildren().addAll(new Label("Rotation-rate unit"), units);
        addMetric("Rotation rate", shape.rotationRate().value(),
                value -> replace(shape.withRotationRate(new RotationRate(value, shape.rotationRate().unit()))),
                !shape.powered());

        if (shape instanceof RadialShapeState radial) {
            inspector.getChildren().add(new Separator());
            inspector.getChildren().add(new Label("Radial profile"));
            addInteger("Radial sample count", radial.radialSampleCount(),
                    value -> replace(radial.withSampleCount(Math.clamp(value, MIN_RADIAL_SAMPLES, MAX_RADIAL_SAMPLES))));
            addMetric("Variation", radial.variation(),
                    value -> replace(radial.withVariation(Math.clamp(value, 0, .84))));
            inspector.getChildren().add(radialRadiiEditor(radial));
        }

        inspector.getChildren().add(new Label("Effective: " + format(result.rateDegPerTick())
                + "°/tick — " + result.mode() + " depth " + result.depth()));
        inspector.getChildren().add(new Separator());
        inspector.getChildren().add(new Label("Rotation since start"));
        telemetryTickLabel = new Label();
        telemetryStepLabel = new Label();
        telemetryTotalLabel = new Label();
        telemetryTurnsLabel = new Label();
        inspector.getChildren().addAll(telemetryTickLabel, telemetryStepLabel,
                telemetryTotalLabel, telemetryTurnsLabel);
        updateTelemetry();
        inspector.getChildren().add(new Separator());
        inspector.getChildren().add(new Label("Contacts"));
        for (var contact : contacts.stream()
                .filter(c -> c.aId() == shape.id() || c.bId() == shape.id()).toList()) {
            inspector.getChildren().add(contactEditor(shape, contact));
        }
    }

    private Node radialRadiiEditor(RadialShapeState radial) {
        var box = new VBox(5);
        var label = new Label("Individual radii");
        var help = new Label("One radius per line, or separated by commas/spaces.");
        help.setWrapText(true);
        var area = new TextArea(radial.radialRadii().stream()
                .map(CircleWorkspaceApp::format)
                .collect(java.util.stream.Collectors.joining(System.lineSeparator())));
        area.setPrefRowCount(Math.clamp(radial.radialSampleCount(), 4, 12));
        area.setWrapText(false);
        var apply = new Button("Apply radii");
        var error = new Label();
        error.setStyle("-fx-text-fill: #b42318;");
        apply.setOnAction(event -> {
            try {
                var values = parseRadii(area.getText());
                replace(radial.withRadialRadii(values));
                error.setText("");
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
            }
        });
        box.getChildren().addAll(label, help, area, apply, error);
        return box;
    }

    private static List<Double> parseRadii(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Enter at least three radii.");
        }
        var tokens = text.trim().split("[\\s,;]+");
        if (tokens.length < MIN_RADIAL_SAMPLES || tokens.length > MAX_RADIAL_SAMPLES) {
            throw new IllegalArgumentException("Enter between " + MIN_RADIAL_SAMPLES
                    + " and " + MAX_RADIAL_SAMPLES + " radii.");
        }
        var values = new ArrayList<Double>(tokens.length);
        for (int i = 0; i < tokens.length; i++) {
            final double value;
            try {
                value = Double.parseDouble(tokens[i]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Radius " + (i + 1) + " is not a number.");
            }
            if (!Double.isFinite(value) || value <= 0) {
                throw new IllegalArgumentException("Radius " + (i + 1) + " must be positive.");
            }
            values.add(value);
        }
        return List.copyOf(values);
    }

    private Node contactEditor(WorkspaceShape shape, ContactState contact) {
        var box = new VBox(5);
        box.setPadding(new Insets(7));
        box.setStyle("-fx-border-color: #c9d2df; -fx-border-radius: 4;");
        box.getChildren().add(new Label("Contact " + contact.id() + " with #" + contact.other(shape.id())));
        var input = new RadioButton("Use as rotation input");
        input.setSelected(Objects.equals(shape.slaveContactId(), contact.id()));
        input.setDisable(shape.powered());
        input.setOnAction(event -> replace(shape.withSlave(input.isSelected() ? contact.id() : null)));
        var mode = new ComboBox<ContactFollowMode>();
        mode.getItems().setAll(ContactFollowMode.values());
        mode.setValue(contact.followMode());
        mode.setDisable(contact.type() == Tangency.INTERNAL);
        mode.setOnAction(event -> replaceContact(contact.withFollowMode(mode.getValue())));
        var remove = new Button("Break contact");
        remove.setOnAction(event -> breakContact(contact.id()));
        box.getChildren().addAll(input, new Label("Contact following"), mode, remove);
        return box;
    }

    private void redraw() {
        if (canvas == null) return;
        var g = canvas.getGraphicsContext2D();
        g.setTransform(1, 0, 0, 1, 0, 0);
        g.setFill(Color.web("#f7f8fb"));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.save();
        g.translate(camera.offsetX(), camera.offsetY());
        g.scale(camera.scale(), camera.scale());
        var results = solver.solve(shapes, contacts, tick.get());
        var posed = contactFollower.solve(shapes, contacts, tick.get(), results);
        var telemetry = rotationTelemetry.at(
                new ArrayList<>(shapes), new ArrayList<>(contacts), tick.get());
        for (var shape : shapes) {
            var displayed = posed.getOrDefault(shape.id(), shape);
            drawShape(g, displayed, results.get(shape.id()), telemetry.get(shape.id()));
        }
        g.setStroke(Color.web("#9aa6b6"));
        g.setLineWidth(1.2 / camera.scale());
        for (var contact : contacts) {
            var a = posed.get(contact.aId());
            var b = posed.get(contact.bId());
            if (a != null && b != null) g.strokeLine(a.x(), a.y(), b.x(), b.y());
        }
        g.restore();
    }

    private void drawShape(GraphicsContext g, WorkspaceShape shape,
                           RotationSolver.Result result, RotationTelemetry.Value telemetry) {
        double displayed = shape.displayedAngle(tick.get(), result.rateDegPerTick());
        double inverseZoom = 1.0 / camera.scale();
        Color stroke = switch (result.mode()) {
            case POWERED -> Color.DODGERBLUE;
            case SLAVED -> Color.FORESTGREEN;
            case STOPPED -> Color.DARKGRAY;
        };
        int segments = shape instanceof CircleState ? 96 : Math.max(96,
                Math.min(1024, ((RadialShapeState) shape).radialSampleCount() * 12));
        double[] xs = new double[segments];
        double[] ys = new double[segments];
        for (int i = 0; i < segments; i++) {
            double local = Math.TAU * i / segments;
            double radius = shape.boundaryRadius(local);
            double world = local + Math.toRadians(displayed);
            xs[i] = shape.x() + Math.sin(world) * radius;
            ys[i] = shape.y() - Math.cos(world) * radius;
        }
        g.setFill(stroke.deriveColor(0, .5, 1.1, shape.id() == selectedId.get() ? .13 : .06));
        g.fillPolygon(xs, ys, segments);
        g.setStroke(stroke);
        g.setLineWidth((shape.id() == selectedId.get() ? 3.2 : 2.0) * inverseZoom);
        g.strokePolygon(xs, ys, segments);

        int lineCount = shape.lineDivisionCount();
        int markerStride = Math.max(1, (int) Math.ceil(lineCount / 600.0));
        double averageSpacingPixels = shape.boundaryLength() * camera.scale() / lineCount;
        int labelStride = Math.max(markerStride,
                (int) Math.ceil(18.0 / Math.max(averageSpacingPixels, 0.001)));
        double markerLengthPixels = 8.0;
        double markerLengthWorld = markerLengthPixels * inverseZoom;
        g.setLineWidth(inverseZoom);
        g.setFont(Font.font(10.0 * inverseZoom));
        g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);

        for (int i = 0; i < lineCount; i += markerStride) {
            double local = shape.divisionAngleRad(i);
            double radius = shape.boundaryRadius(local);
            double world = local + Math.toRadians(displayed);
            double ux = Math.sin(world);
            double uy = -Math.cos(world);
            double length = Math.min(radius * .22, markerLengthWorld);
            double boundaryX = shape.x() + ux * radius;
            double boundaryY = shape.y() + uy * radius;
            g.strokeLine(boundaryX, boundaryY,
                    shape.x() + ux * (radius - length),
                    shape.y() + uy * (radius - length));

            if (i % labelStride == 0) {
                double labelOffset = 9.0 * inverseZoom;
                g.setFill(Color.web("#202733"));
                g.fillText(Integer.toString(i + 1),
                        boundaryX + ux * labelOffset,
                        boundaryY + uy * labelOffset);
            }
        }

        g.setFill(Color.web("#202733"));
        g.setFont(Font.font(11.0 * inverseZoom));
        g.fillText("#" + shape.id(), shape.x(), shape.y() - 12.0 * inverseZoom);
        if (telemetry != null) {
            g.setFont(Font.font(9.0 * inverseZoom));
            g.fillText("t " + tick.get() + "  Σ " + format(telemetry.totalDegrees()) + "°",
                    shape.x(), shape.y() + 2.0 * inverseZoom);
            g.fillText(format(telemetry.totalTurns()) + " rev  ("
                            + telemetry.completedFullRotations() + " full)",
                    shape.x(), shape.y() + 14.0 * inverseZoom);
        }
    }

    private WorkspaceShape hit(double x, double y) {
        var results = solver.solve(shapes, contacts, tick.get());
        var posed = contactFollower.solve(shapes, contacts, tick.get(), results);
        var reversed = shapes.stream()
                .map(shape -> posed.getOrDefault(shape.id(), shape))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.reverse(reversed);
        for (var shape : reversed) {
            double dx = x - shape.x();
            double dy = y - shape.y();
            double distance = Math.hypot(dx, dy);
            double worldDirection = normalize(Math.toDegrees(Math.atan2(dx, -dy)));
            double displayed = shape.displayedAngle(tick.get(), results.get(shape.id()).rateDegPerTick());
            double radius = shape.boundaryRadius(shape.localAngleForWorldDirection(worldDirection, displayed));
            if (distance <= radius + 5) return shape;
        }
        return null;
    }

    private void snapSelected() {
        var moving = selected();
        if (moving == null) return;
        WorkspaceShape best = null;
        double bestError = Double.MAX_VALUE;
        for (var other : shapes) {
            if (other.id() == moving.id()) continue;
            double distance = Math.hypot(other.x() - moving.x(), other.y() - moving.y());
            double desired = moving.radius() + other.radius();
            double error = Math.abs(distance - desired);
            if (error < SNAP_TOLERANCE / camera.scale() && error < bestError) {
                best = other;
                bestError = error;
            }
        }
        if (best == null) return;
        double angle = Math.atan2(moving.y() - best.y(), moving.x() - best.x());
        double desired = moving.radius() + best.radius();
        replace(moving.withPosition(best.x() + Math.cos(angle) * desired,
                best.y() + Math.sin(angle) * desired));
        double movingTouch = normalize(Math.toDegrees(angle) - 180 + 90);
        double otherTouch = normalize(Math.toDegrees(angle) + 90);
        double bearing = normalize(Math.toDegrees(angle));
        var contact = new ContactState(nextContactId++, best.id(), moving.id(),
                Tangency.EXTERNAL, otherTouch, movingTouch,
                ContactFollowMode.NO_OVERLAP_SWITCH_CONTACT, bearing);
        contacts.add(contact);
        resync(best.id());
    }

    private void resync(int changedId) {
        var updated = RotationResyncPolicy.resync(new ArrayList<>(shapes),
                new ArrayList<>(contacts), changedId, tick.get());
        shapes.setAll(updated);
    }

    private void replace(WorkspaceShape replacement) {
        replacement = normalizeEqualLengthShape(replacement);
        for (int i = 0; i < shapes.size(); i++) {
            if (shapes.get(i).id() == replacement.id()) {
                shapes.set(i, replacement);
                resync(replacement.id());
                return;
            }
        }
    }

    private WorkspaceShape normalizeEqualLengthShape(WorkspaceShape shape) {
        if (shape.divisionDistribution() != DivisionDistribution.EQUAL_LENGTH) {
            return shape;
        }
        return shape.scaledToBoundaryLength(shape.lineDivisionCount() * globalMarkerLength.get());
    }

    private void setGlobalMarkerLength(double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("global line length must be positive");
        }
        globalMarkerLength.set(value);
        shapes.setAll(shapes.stream().map(this::normalizeEqualLengthShape).toList());
    }

    private void detach(int shapeId, boolean resetAngle) {
        var shape = find(shapeId);
        if (shape == null) return;
        var results = solver.solve(shapes, contacts, tick.get());
        var result = results.get(shapeId);
        var posed = contactFollower.solve(shapes, contacts, tick.get(), results)
                .getOrDefault(shapeId, shape);
        contacts.removeIf(contact -> contact.aId() == shapeId || contact.bId() == shapeId);
        WorkspaceShape replacement = shape.withPosition(posed.x(), posed.y()).withSlave(null);
        if (resetAngle) replacement = replacement.withStartAngle(0);
        else replacement = replacement.withStartAngle(
                RotationLinkPolicy.displayedAngle(shape, result, tick.get()));
        replaceDirect(replacement);
        shapes.replaceAll(candidate -> {
            if (candidate.slaveContactId() != null
                    && contacts.stream().noneMatch(c -> c.id() == candidate.slaveContactId())) {
                return candidate.withSlave(null);
            }
            return candidate;
        });
    }

    private void replaceContact(ContactState replacement) {
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).id() == replacement.id()) {
                contacts.set(i, replacement);
                return;
            }
        }
    }

    private void breakContact(int contactId) {
        contacts.removeIf(contact -> contact.id() == contactId);
        shapes.replaceAll(shape -> Objects.equals(shape.slaveContactId(), contactId)
                ? shape.withSlave(null) : shape);
    }

    private void move(WorkspaceShape shape, double dx, double dy) {
        detach(shape.id(), true);
        replaceDirect(shape.withPosition(shape.x() + dx, shape.y() + dy));
    }

    private void duplicateSelected() {
        var shape = selected();
        if (shape == null) return;
        WorkspaceShape copy = shape instanceof CircleState circle
                ? new CircleState(nextShapeId++, circle.x() + 25, circle.y() + 25, circle.radius(),
                    circle.startAngleDeg(), false, circle.rotationRate(), null,
                    circle.divisions(), circle.divisionDistribution())
                : new RadialShapeState(nextShapeId++, shape.x() + 25, shape.y() + 25, shape.radius(),
                    shape.startAngleDeg(), false, shape.rotationRate(), null, shape.divisions(),
                    shape.divisionDistribution(), ((RadialShapeState) shape).radialMultipliers());
        shapes.add(copy);
        selectedId.set(copy.id());
    }

    private void deleteSelected() {
        int id = selectedId.get();
        if (id < 0) return;
        contacts.removeIf(contact -> contact.aId() == id || contact.bId() == id);
        shapes.removeIf(shape -> shape.id() == id);
        shapes.replaceAll(shape -> shape.slaveContactId() != null
                && contacts.stream().noneMatch(c -> c.id() == shape.slaveContactId())
                ? shape.withSlave(null) : shape);
        selectedId.set(-1);
    }

    private void save(Stage stage) {
        var chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        var file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            store.save(file.toPath(), snapshot());
        } catch (Exception ex) {
            showError("Save failed", ex);
        }
    }

    private void load(Stage stage) {
        var chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        var file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            applyData(store.load(file.toPath()));
        } catch (Exception ex) {
            showError("Load failed", ex);
        }
    }

    private WorkspaceData snapshot() {
        return new WorkspaceData(new ArrayList<>(shapes), new ArrayList<>(contacts),
                tick.get(), ticksPerSecond.get(), globalMarkerLength.get());
    }

    private void applyData(WorkspaceData data) {
        globalMarkerLength.set(data.globalMarkerLength());
        shapes.setAll(data.shapes().stream().map(this::normalizeEqualLengthShape).toList());
        contacts.setAll(data.contacts());
        tick.set(data.tick());
        ticksPerSecond.set(data.ticksPerSecond());
        selectedId.set(-1);
        nextShapeId = shapes.stream().mapToInt(WorkspaceShape::id).max().orElse(0) + 1;
        nextContactId = contacts.stream().mapToInt(ContactState::id).max().orElse(0) + 1;
    }

    private WorkspaceShape selected() { return find(selectedId.get()); }
    private WorkspaceShape find(int id) {
        return shapes.stream().filter(shape -> shape.id() == id).findFirst().orElse(null);
    }

    private void replaceDirect(WorkspaceShape replacement) {
        for (int i = 0; i < shapes.size(); i++) {
            if (shapes.get(i).id() == replacement.id()) {
                shapes.set(i, replacement);
                return;
            }
        }
    }

    private void refresh() {
        rotationTelemetry.invalidate();
        redraw();
        rebuildInspector();
    }

    private void updateTelemetry() {
        if (telemetryTickLabel == null || telemetryStepLabel == null
                || telemetryTotalLabel == null || telemetryTurnsLabel == null) {
            return;
        }
        var shape = selected();
        if (shape == null) return;
        var telemetry = rotationTelemetry.at(
                new ArrayList<>(shapes), new ArrayList<>(contacts), tick.get());
        var value = telemetry.get(shape.id());
        if (value == null) return;
        telemetryTickLabel.setText("Ticks: " + tick.get());
        telemetryStepLabel.setText("This tick: " + format(value.stepDegrees()) + "°");
        telemetryTotalLabel.setText("Total: " + format(value.totalDegrees()) + "°");
        telemetryTurnsLabel.setText("Rotations: " + format(value.totalTurns())
                + " (" + value.completedFullRotations() + " full)");
    }

    private void addMetric(String name, double value, java.util.function.DoubleConsumer commit) {
        addMetric(name, value, commit, false);
    }

    private void addMetric(String name, double value, java.util.function.DoubleConsumer commit, boolean disabled) {
        var row = new HBox(7, new Label(name), doubleField(value, commit));
        ((TextField) row.getChildren().get(1)).setDisable(disabled);
        inspector.getChildren().add(row);
    }

    private void addInteger(String name, int value, java.util.function.IntConsumer commit) {
        var field = new TextField(Integer.toString(value));
        field.setPrefColumnCount(8);
        Runnable apply = () -> {
            try { commit.accept(Integer.parseInt(field.getText().trim())); }
            catch (RuntimeException ex) { field.setText(Integer.toString(value)); }
        };
        field.setOnAction(event -> apply.run());
        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) apply.run();
        });
        inspector.getChildren().add(new HBox(7, new Label(name), field));
    }

    private static TextField doubleField(double value, java.util.function.DoubleConsumer commit) {
        var field = new TextField(format(value));
        field.setPrefColumnCount(8);
        Runnable apply = () -> {
            try { commit.accept(Double.parseDouble(field.getText().trim())); }
            catch (RuntimeException ex) { field.setText(format(value)); }
        };
        field.setOnAction(event -> apply.run());
        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) apply.run();
        });
        return field;
    }

    private static TextField boundLongField(LongProperty property) {
        var field = new TextField(Long.toString(property.get()));
        field.setPrefColumnCount(7);
        property.addListener((obs, oldValue, newValue) -> field.setText(Long.toString(newValue.longValue())));
        field.setOnAction(event -> {
            try { property.set(Long.parseLong(field.getText().trim())); }
            catch (NumberFormatException ex) { field.setText(Long.toString(property.get())); }
        });
        return field;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static void showError(String title, Exception exception) {
        var alert = new Alert(Alert.AlertType.ERROR, exception.getMessage(), ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
