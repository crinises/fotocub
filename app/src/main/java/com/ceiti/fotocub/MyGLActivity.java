package com.ceiti.fotocub;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

public class MyGLActivity extends Activity {

    private static final int REQUEST_WRITE_STORAGE = 112;

    private GLSurfaceView glView;
    private MyGLRenderer renderer;

    private float lastX, lastY;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private SeekBar speedBar;
    private TextView speedLabel;
    private ImageButton btnPause, btnReset, btnScreenshot;
    private View controlPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glView = findViewById(R.id.gl_surface);
        renderer = new MyGLRenderer(this);
        glView.setEGLContextClientVersion(1);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        renderer.zoom(detector.getScaleFactor());
                        return true;
                    }
                });

        gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        renderer.resetTransform();
                        Toast.makeText(MyGLActivity.this, "Reset view", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });

        glView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            handleManualRotation(event);
            return true;
        });

        controlPanel  = findViewById(R.id.control_panel);
        speedBar      = findViewById(R.id.speed_seekbar);
        speedLabel    = findViewById(R.id.speed_label);
        btnPause      = findViewById(R.id.btn_pause);
        btnReset      = findViewById(R.id.btn_reset);
        btnScreenshot = findViewById(R.id.btn_screenshot);

        speedBar.setProgress(5);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                float speed = p / 10f;
                renderer.setSpeed(speed);
                speedLabel.setText(String.format("Speed: %.1f", speed));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        btnPause.setOnClickListener(v -> {
            boolean paused = renderer.togglePause();
            btnPause.setImageResource(paused
                    ? android.R.drawable.ic_media_play
                    : android.R.drawable.ic_media_pause);
        });

        btnReset.setOnClickListener(v -> {
            renderer.resetTransform();
            speedBar.setProgress(5);
            Toast.makeText(this, "Resetat", Toast.LENGTH_SHORT).show();
        });

        btnScreenshot.setOnClickListener(v -> captureScreenshot());

        TextView tapHint = findViewById(R.id.tap_hint);
        tapHint.setOnClickListener(v -> {
            int vis = controlPanel.getVisibility() == View.VISIBLE
                    ? View.GONE : View.VISIBLE;
            controlPanel.setVisibility(vis);
        });
    }

    // ── Screenshot ───────────────────────────────────────────────────────

    private void captureScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            doCapture();
            return;
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            doCapture();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Delay de 300ms ca sa lase GLSurfaceView sa se reia dupa dialog
                glView.postDelayed(this::doCapture, 300);
            } else {
                Toast.makeText(this,
                        "Permisiunea refuzata — nu pot salva!",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void doCapture() {
        glView.queueEvent(() -> {
            try {
                final int w = glView.getWidth();
                final int h = glView.getHeight();

                if (w == 0 || h == 0) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Ecran invalid, incearca din nou!",
                                    Toast.LENGTH_SHORT).show());
                    return;
                }

                IntBuffer buf = IntBuffer.allocate(w * h);
                android.opengl.GLES10.glReadPixels(
                        0, 0, w, h,
                        GL10.GL_RGBA,
                        GL10.GL_UNSIGNED_BYTE,
                        buf);

                int[] pixels = buf.array();
                int[] argb = new int[w * h];
                for (int row = 0; row < h; row++) {
                    for (int col = 0; col < w; col++) {
                        int px = pixels[row * w + col];
                        int r = (px)       & 0xFF;
                        int g = (px >> 8)  & 0xFF;
                        int b = (px >> 16) & 0xFF;
                        int a = (px >> 24) & 0xFF;
                        argb[(h - row - 1) * w + col] =
                                (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }

                final Bitmap bmp = Bitmap.createBitmap(
                        argb, w, h, Bitmap.Config.ARGB_8888);

                runOnUiThread(() -> saveToGallery(bmp));

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Eroare captura: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    private void saveToGallery(Bitmap bmp) {
        try {
            String fileName = "FotoCub_" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/FotoCub");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri uri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
                    Toast.makeText(this,
                            "Salvat in Galerie → FotoCub!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Eroare: URI null!", Toast.LENGTH_SHORT).show();
                }

            } else {
                values.put(MediaStore.Images.Media.DATA,
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES) + "/" + fileName);

                Uri uri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                    Toast.makeText(this, "Screenshot salvat!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Eroare: URI null!", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Toast.makeText(this,
                    "Eroare salvare: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ── Touch ────────────────────────────────────────────────────────────

    private void handleManualRotation(MotionEvent event) {
        if (scaleDetector.isInProgress()) return;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastX;
                float dy = event.getY() - lastY;
                renderer.addManualRotation(dy * 0.4f, dx * 0.4f);
                lastX = event.getX();
                lastY = event.getY();
                break;
        }
    }

    @Override protected void onPause()  { super.onPause();  glView.onPause();  }
    @Override protected void onResume() { super.onResume(); glView.onResume(); }
}