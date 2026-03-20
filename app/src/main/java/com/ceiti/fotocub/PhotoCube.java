package com.ceiti.fotocub;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

/**
 * A textured cube built from 6 individual quad faces.
 * Each face loads a separate image from res/drawable.
 */
public class PhotoCube {

    private FloatBuffer vertexBuffer;
    private FloatBuffer texBuffer;
    private FloatBuffer normalBuffer;

    private static final int NUM_FACES = 6;


    private final int[] imageFileIDs = {
            R.drawable.pic1,
            R.drawable.pic2,
            R.drawable.pic3,
            R.drawable.pic4,
            R.drawable.pic5,
            R.drawable.pic6
    };

    private final int[] textureIDs = new int[NUM_FACES];
    private final Bitmap[] bitmaps  = new Bitmap[NUM_FACES];

    private static final float HALF = 1.5f;   // half-size of cube side

    // ── Per-face transforms: {rotAngle, rx, ry, rz, tx, ty, tz} ────────
    private static final float[][] FACE_TRANSFORMS = {
            {   0f, 0f, 1f, 0f,  0f,  0f,  HALF },  // Front
            { 270f, 0f, 1f, 0f,  0f,  0f,  HALF },  // Right  (+90 around Y)
            { 180f, 0f, 1f, 0f,  0f,  0f,  HALF },  // Back
            {  90f, 0f, 1f, 0f,  0f,  0f,  HALF },  // Left   (−90 around Y)
            { 270f, 1f, 0f, 0f,  0f,  0f,  HALF },  // Top
            {  90f, 1f, 0f, 0f,  0f,  0f,  HALF },  // Bottom
    };

    public PhotoCube(Context context) {
        // ── Vertex buffer (4 verts × 3 floats × 6 faces) ────────────────
        ByteBuffer vbb = ByteBuffer.allocateDirect(4 * 3 * NUM_FACES * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();

        // ── Normal buffer  ───────────────────────────────────────────────
        ByteBuffer nbb = ByteBuffer.allocateDirect(4 * 3 * NUM_FACES * 4);
        nbb.order(ByteOrder.nativeOrder());
        normalBuffer = nbb.asFloatBuffer();

        for (int face = 0; face < NUM_FACES; face++) {
            bitmaps[face] = BitmapFactory.decodeStream(
                    context.getResources().openRawResource(imageFileIDs[face]));

            float imgW = bitmaps[face].getWidth();
            float imgH = bitmaps[face].getHeight();
            float faceW = 2f, faceH = 2f;
            if (imgW > imgH) faceH = faceH * imgH / imgW;
            else              faceW = faceW * imgW / imgH;

            float l = -faceW / 2, r = -l;
            float t = faceH / 2,  b = -t;

            vertexBuffer.put(new float[]{ l, b, 0f,  r, b, 0f,  l, t, 0f,  r, t, 0f });
            normalBuffer.put(new float[]{ 0,0,1, 0,0,1, 0,0,1, 0,0,1 });   // +Z facing
        }
        vertexBuffer.position(0);
        normalBuffer.position(0);

        // ── Texture-coord buffer (same UVs for every face) ───────────────
        float[] uvs = { 0f,1f,  1f,1f,  0f,0f,  1f,0f };
        ByteBuffer tbb = ByteBuffer.allocateDirect(uvs.length * 4 * NUM_FACES);
        tbb.order(ByteOrder.nativeOrder());
        texBuffer = tbb.asFloatBuffer();
        for (int i = 0; i < NUM_FACES; i++) texBuffer.put(uvs);
        texBuffer.position(0);
    }

    // ── Draw ─────────────────────────────────────────────────────────────

    public void draw(GL10 gl) {
        gl.glFrontFace(GL10.GL_CCW);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texBuffer);
        gl.glNormalPointer(GL10.GL_FLOAT, 0, normalBuffer);

        for (int face = 0; face < NUM_FACES; face++) {
            float[] t = FACE_TRANSFORMS[face];
            gl.glPushMatrix();
            gl.glRotatef(t[0], t[1], t[2], t[3]);
            gl.glTranslatef(t[4], t[5], t[6]);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIDs[face]);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, face * 4, 4);
            gl.glPopMatrix();
        }

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
    }

    // ── Load textures ────────────────────────────────────────────────────

    public void loadTexture(GL10 gl) {
        gl.glGenTextures(NUM_FACES, textureIDs, 0);
        for (int face = 0; face < NUM_FACES; face++) {
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIDs[face]);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmaps[face], 0);
            bitmaps[face].recycle();
        }
    }
}