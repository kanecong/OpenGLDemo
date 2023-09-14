package com.example.gles_video.second.video;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.example.gles_video.OpenGLUtils;
import com.example.gles_video.second.video.render.DisplayRenderDrawer;
import com.example.gles_video.second.video.render.OriginalRenderDrawer;
import com.example.gles_video.second.video.render.RecordRenderDrawer;

/**
 * Created By gaorui on 2018/8/31
 * 统一管理所有的RenderDrawer 和 FBO
 */
public class RenderDrawerGroups {
    private int mInputTexture;
    private int mFrameBuffer;
    private OriginalRenderDrawer mOriginalDrawer;
    private DisplayRenderDrawer mDisplayDrawer;
    private RecordRenderDrawer mRecordDrawer;

    public RenderDrawerGroups(Context context) {
        this.mOriginalDrawer = new OriginalRenderDrawer(context);
        this.mDisplayDrawer = new DisplayRenderDrawer(context);
        this.mRecordDrawer = new RecordRenderDrawer(context);
        this.mFrameBuffer = 0;
        this.mInputTexture = 0;
    }

    public void setInputTexture(int texture) {
        this.mInputTexture = texture;
    }

    public void deleteFrameBuffer() {
        GLES20.glDeleteFramebuffers(1, new int[]{mFrameBuffer}, 0);
    }

    public void create() {
        this.mOriginalDrawer.initPos();
        this.mDisplayDrawer.initPos();
        this.mRecordDrawer.create();
    }

    public void surfaceChangedSize(int width, int height) {

        deleteFrameBuffer();

        mFrameBuffer = OpenGLUtils.createFBO();
        mOriginalDrawer.setVideoSize(720,1280);
        mOriginalDrawer.setWorldSize(height, width);

        mDisplayDrawer.setVideoSize(720,1280);
        mDisplayDrawer.setWorldSize(height, width, false);
        mRecordDrawer.setWorldSize(height, width, false);

        this.mOriginalDrawer.setInputTextureId(mInputTexture);
        int textureId = this.mOriginalDrawer.getOutputTextureId();
        mDisplayDrawer.setInputTextureId(textureId);
        mRecordDrawer.setInputTextureId(textureId);
    }

    public void drawRender(BaseVideoDrawer drawer, boolean useFrameBuffer, long timestamp) {
        if (useFrameBuffer) {
            OpenGLUtils.bindFBO(mFrameBuffer, drawer.getOutputTextureId());
        }
        drawer.draw(timestamp);
        if (useFrameBuffer) {
            OpenGLUtils.unBindFBO();

//            drawer.drawNormal();
        }
    }

    public void draw(long timestamp) {
        if (mInputTexture == 0 || mFrameBuffer == 0) {
            Log.e("gaorui", "draw: mInputTexture or mFramebuffer or list is zero");
            return;
        }
        drawRender(mOriginalDrawer, true, timestamp);
        // 绘制顺序会控制着 水印绘制哪一层
        drawRender(mDisplayDrawer, false,  timestamp);
        drawRender(mRecordDrawer, false, timestamp);
    }

    public void startRecord() {
        mRecordDrawer.startRecord();
    }

    public void stopRecord() {
        mRecordDrawer.stopRecord();
    }
}
