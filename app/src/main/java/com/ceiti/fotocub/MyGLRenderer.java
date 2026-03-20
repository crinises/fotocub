package com.ceiti.fotocub;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private com.ceiti.fotocub.PhotoCube cube;

    // Auto-rotation axes
    private float angleX = 0f;
    private float angleY = 0f;

    // Manual (touch) rotation
    private float manualX = 0f;
    private float manualY = 0f;

    // Zoom (camera distance)
    private float zoom = 4.5f;
    private static final float ZOOM_MIN = 2.5f;
    private static final float ZOOM_MAX = 14f;

    private float speed = 0.5f;
    private boolean paused = false;

    public MyGLRenderer(Context context) {
        cube = new PhotoCube(context);
    }

    // ── Called by Activity ───────────────────────────────────────────────

    public void setSpeed(float s)              { speed = s; }
    public boolean togglePause()               { paused = !paused; return paused; }
    public void addManualRotation(float dx, float dy) {
        manualX += dx;
        manualY += dy;
    }
    public void zoom(float factor) {
        zoom /= factor;
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom));
    }
    public void resetTransform() {
        angleX = 0; angleY = 0;
        manualX = 0; manualY = 0;
        zoom = 6f;
    }

    // ── GLSurfaceView.Renderer ───────────────────────────────────────────

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.05f, 0.05f, 0.10f, 1.0f);   // dark-blue background
        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glDisable(GL10.GL_DITHER);

        // Lighting – one warm directional light
        gl.glEnable(GL10.GL_LIGHTING);
        gl.glEnable(GL10.GL_LIGHT0);
        float[] lightPos   = { 5f,  5f, 10f, 1f };
        float[] ambient    = { 0.3f, 0.3f, 0.3f, 1f };
        float[] diffuse    = { 0.9f, 0.85f, 0.8f, 1f };
        float[] specular   = { 0.5f, 0.5f, 0.5f, 1f };
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPos, 0);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT,  ambient,  0);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE,  diffuse,  0);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, specular, 0);
        gl.glEnable(GL10.GL_COLOR_MATERIAL);

        cube.loadTexture(gl);
        gl.glEnable(GL10.GL_TEXTURE_2D);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (height == 0) height = 1;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 45f, (float) width / height, 0.1f, 100f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        // Camera position (zoom)
        gl.glTranslatef(0f, 0f, -zoom);

        // Apply manual touch rotation first
        gl.glRotatef(manualX, 1f, 0f, 0f);
        gl.glRotatef(manualY, 0f, 1f, 0f);

        // Apply auto rotation
        gl.glRotatef(angleX, 1f, 0f, 0f);
        gl.glRotatef(angleY, 0f, 1f, 0f);

        cube.draw(gl);

        // Advance auto-rotation if not paused
        if (!paused) {
            angleX += speed * 0.3f;
            angleY += speed;
        }
    }
}