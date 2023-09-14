package com.example.gles_video.second.video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.example.gles_video.SurfaceTextureHandle;
import com.example.gles_video.second.video.Utils.GlesUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TestRender implements GLSurfaceView.Renderer{

    private RenderDrawerGroups renderDrawerGroups = null;
    private int mCameraTextureId = -1;
    private SurfaceTexture mCameraTexture = null;
    private long timestamp = 0l;
    private SurfaceTextureHandle surfaceTextureHandle = null;
    private GLSurfaceView mGLView = null;

    public TestRender(GLSurfaceView glSurfaceView, Context context, SurfaceTextureHandle surfaceHandle) {
        surfaceTextureHandle = surfaceHandle;
        mGLView = glSurfaceView;
        renderDrawerGroups = new RenderDrawerGroups(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.e("gaorui", "TestRender - onSurfaceCreated");

        mCameraTextureId = GlesUtil.createCameraTexture();
        renderDrawerGroups.setInputTexture(mCameraTextureId);
        renderDrawerGroups.create();
        initCameraTexture();

    }

    public void initCameraTexture() {
        mCameraTexture = new SurfaceTexture(mCameraTextureId);
        mCameraTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (mGLView != null) {
                    mGLView.requestRender();
                }
            }
        });
        surfaceTextureHandle.handleSurfaceTexture(mCameraTexture);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        renderDrawerGroups.surfaceChangedSize(width, height);
        Log.e("gaorui", "TestRender - onSurfaceChanged - height = " + height + ", width = " + width);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        if (mCameraTexture != null) {
            mCameraTexture.updateTexImage();
            timestamp = mCameraTexture.getTimestamp();
            renderDrawerGroups.draw(timestamp);
        }
    }

    public void releaseSurfaceTexture() {
        if (mCameraTexture != null) {
            mCameraTexture.release();
            mCameraTexture = null;
        }
    }

    public void resumeSurfaceTexture() {
        initCameraTexture();
    }

    public void startRecord() {
        renderDrawerGroups.startRecord();
    }

    public void stopRecord() {
        renderDrawerGroups.stopRecord();
    }
}
