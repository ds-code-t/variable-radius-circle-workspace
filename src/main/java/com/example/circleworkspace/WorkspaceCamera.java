package com.example.circleworkspace;

/**
 * Mutable viewport transform kept outside workspace/domain state.
 * World coordinates are persisted; camera coordinates are session-only.
 */
public final class WorkspaceCamera {
    public static final double MIN_SCALE = 0.1;
    public static final double MAX_SCALE = 8.0;

    private double offsetX;
    private double offsetY;
    private double scale = 1.0;

    public record Point(double x, double y) {}

    public double offsetX() { return offsetX; }
    public double offsetY() { return offsetY; }
    public double scale() { return scale; }

    public Point toWorld(double screenX, double screenY) {
        return new Point((screenX - offsetX) / scale, (screenY - offsetY) / scale);
    }

    public Point toScreen(double worldX, double worldY) {
        return new Point(worldX * scale + offsetX, worldY * scale + offsetY);
    }

    public void panBy(double screenDeltaX, double screenDeltaY) {
        if (!Double.isFinite(screenDeltaX) || !Double.isFinite(screenDeltaY)) {
            throw new IllegalArgumentException("pan delta must be finite");
        }
        offsetX += screenDeltaX;
        offsetY += screenDeltaY;
    }

    public void zoomAt(double screenX, double screenY, double factor) {
        if (!Double.isFinite(screenX) || !Double.isFinite(screenY)
                || !Double.isFinite(factor) || factor <= 0) {
            throw new IllegalArgumentException("zoom inputs must be finite and positive");
        }
        Point anchor = toWorld(screenX, screenY);
        scale = Math.clamp(scale * factor, MIN_SCALE, MAX_SCALE);
        offsetX = screenX - anchor.x * scale;
        offsetY = screenY - anchor.y * scale;
    }

    public void reset() {
        offsetX = 0;
        offsetY = 0;
        scale = 1;
    }
}
