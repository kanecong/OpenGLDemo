package com.example.gles_video;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SimpleRender implements GLSurfaceView.Renderer {
    private ArrayList<IDrawer> mDrawer = new ArrayList<>();

    public SimpleRender(ArrayList<IDrawer> drawer) {
        mDrawer = drawer;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.e("gaorui", "onSurfaceCreated");

        // 开启很混合模式;
        GLES20.glEnable(GLES20.GL_BLEND | GLES20.GL_DEPTH_TEST);
        // 配置混合算法
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int[] textureID = new int[mDrawer.size()];
        GLES20.glGenTextures(mDrawer.size(), textureID, 0);
        for (IDrawer drawer: mDrawer) {
            drawer.setTextureID(textureID[mDrawer.indexOf(drawer)]);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mDrawer.forEach( it -> {
                it.setWorldSize(height, width);
        });
        Log.e("gaorui", "onSurfaceChanged - height = " + height + ", width = " + width);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0, 0, 0 , 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mDrawer.forEach(IDrawer::draw);
    }

    public void release() {
        mDrawer.forEach(IDrawer::release);
    }
}
