package com.example.circleworkspace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceCameraTest {
    @Test void panAndCoordinateConversionAreInverse() {
        var camera = new WorkspaceCamera();
        camera.panBy(120, -35);
        var screen = camera.toScreen(25, 40);
        var world = camera.toWorld(screen.x(), screen.y());
        assertEquals(25, world.x(), 1e-12);
        assertEquals(40, world.y(), 1e-12);
    }

    @Test void zoomKeepsPointerAnchorFixed() {
        var camera = new WorkspaceCamera();
        var before = camera.toWorld(300, 220);
        camera.zoomAt(300, 220, 2);
        var after = camera.toWorld(300, 220);
        assertEquals(before.x(), after.x(), 1e-12);
        assertEquals(before.y(), after.y(), 1e-12);
        assertEquals(2, camera.scale(), 1e-12);
    }

    @Test void zoomIsClamped() {
        var camera = new WorkspaceCamera();
        camera.zoomAt(0, 0, 1e9);
        assertEquals(WorkspaceCamera.MAX_SCALE, camera.scale(), 0);
        camera.zoomAt(0, 0, 1e-9);
        assertEquals(WorkspaceCamera.MIN_SCALE, camera.scale(), 0);
    }

    @Test void resetRestoresIdentityView() {
        var camera = new WorkspaceCamera();
        camera.panBy(50, 60);
        camera.zoomAt(10, 20, 3);
        camera.reset();
        assertEquals(0, camera.offsetX(), 0);
        assertEquals(0, camera.offsetY(), 0);
        assertEquals(1, camera.scale(), 0);
    }
}
