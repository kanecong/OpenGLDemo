package com.example.gles_video.second.video.render;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.gles_video.second.video.Utils.EGLHelper;
import com.example.gles_video.second.video.Utils.StorageUtil;
import com.example.gles_video.second.video.encode.VideoEncoder;

import java.io.File;

/**
 * Created By gaorui1 on 2018/9/21
 */
public class RecordRenderDrawer extends DisplayRenderDrawer implements Runnable{
    // 绘制的纹理 ID
    private int mTextureId;
    private VideoEncoder mVideoEncoder;
    private String mVideoPath;
    private Handler mMsgHandler;
    private EGLHelper mEglHelper;
    private EGLSurface mEglSurface;
    private boolean isRecording;
    private EGLContext mEglContext;


    private Context mContext = null;


    public RecordRenderDrawer(Context context) {
        super(context);
        mContext = context;
        this.mVideoEncoder = null;
        this.mEglHelper = null;
        this.mTextureId = 0;
        this.isRecording = false;
        new Thread(this).start();
    }


    public void setInputTextureId(int textureId) {
        this.mTextureId = textureId;
        Log.d("gaorui", "RecordRenderDrawer - setInputTextureId: " + textureId);
    }


    public int getOutputTextureId() {
        return mTextureId;
    }


    public void create() {
        mEglContext = EGL14.eglGetCurrentContext();
    }

    public void startRecord() {
        Log.d("gaorui", "startRecord context : " + mEglContext.toString());
        Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_START_RECORD, mWorldWidth, mWorldHeight, mEglContext);
        mMsgHandler.sendMessage(msg);
        isRecording = true;
    }

    public void stopRecord() {
        Log.d("gaorui", "stopRecord");
        isRecording = false;
        mMsgHandler.sendMessage(mMsgHandler.obtainMessage(MsgHandler.MSG_STOP_RECORD));
    }

    public void quit() {
        mMsgHandler.sendMessage(mMsgHandler.obtainMessage(MsgHandler.MSG_QUIT));
    }

    public void draw(long timestamp) {
        if (isRecording) {
            Log.d("gaorui", "draw: ");
            Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_FRAME, timestamp);
            mMsgHandler.sendMessage(msg);
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        mMsgHandler = new MsgHandler();
        Looper.loop();
    }

    private class MsgHandler extends Handler {
        public static final int MSG_START_RECORD = 1;
        public static final int MSG_STOP_RECORD = 2;
        public static final int MSG_UPDATE_CONTEXT = 3;
        public static final int MSG_UPDATE_SIZE = 4;
        public static final int MSG_FRAME = 5;
        public static final int MSG_QUIT = 6;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD:
                    prepareVideoEncoder((EGLContext) msg.obj, msg.arg1, msg.arg2);
                    break;
                case MSG_STOP_RECORD:
                    stopVideoEncoder();
                    break;
                case MSG_UPDATE_SIZE:
                    updateChangedSize(msg.arg1, msg.arg2);
                    break;
                case MSG_FRAME:
                    drawFrame((long)msg.obj);
                    break;
                case MSG_QUIT:
                    quitLooper();
                    break;
                default:
                    break;
            }
        }
    }

    private void prepareVideoEncoder(EGLContext context, int width, int height) {
        mEglHelper = new EGLHelper();
        mEglHelper.createGL(context);

        mVideoPath = StorageUtil.getVedioPath(true) + "glvideo.mp4";//mContext.getApplicationContext().getExternalFilesDir("Caches").getAbsolutePath()

        Log.e("gaorui", "prepareVideoEncoder - mVideoPath = " + mVideoPath);


        mVideoEncoder = new VideoEncoder(width, height, new File(mVideoPath));
        mEglSurface = mEglHelper.createWindowSurface(mVideoEncoder.getInputSurface());
        boolean error = mEglHelper.makeCurrent(mEglSurface);
        if (!error) {
            Log.e("gaorui", "prepareVideoEncoder: make current error");
        }

        super.initPos();
    }

    private void stopVideoEncoder() {
        mVideoEncoder.drainEncoder(true);
        if (mEglHelper != null) {
            mEglHelper.destroySurface(mEglSurface);
            mEglHelper.destroyGL();
            mEglSurface = EGL14.EGL_NO_SURFACE;
            mVideoEncoder.release();
            mEglHelper = null;
            mVideoEncoder = null;
        }
    }

    private void drawFrame(long timeStamp) {
        Log.d("gaorui", "drawFrame: " + timeStamp );
        mEglHelper.makeCurrent(mEglSurface);
        onDraw();
        mVideoEncoder.drainEncoder(false);
        mEglHelper.setPresentationTime(mEglSurface, timeStamp);
        mEglHelper.swapBuffers(mEglSurface);
    }

    private void updateChangedSize(int width, int height) {
        onChanged(width, height);
    }

    private void quitLooper() {
        Looper.myLooper().quit();
    }


    protected void onChanged(int width, int height) {

    }


    protected void onDraw() {
//        GLES20.glClearColor(0, 0, 0 , 0);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        createGLPrg(true);

        GLES20.glViewport(0, 0, mWorldWidth, mWorldHeight);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
        GLES30.glUniform1i(mSoulTextureHandler, 0);

        //启用顶点的句柄
        GLES20.glEnableVertexAttribArray(mVertexPosHandler);
        GLES20.glEnableVertexAttribArray(mTexturePosHandler);
        //设置着色器参数， 第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glVertexAttribPointer(mTexturePosHandler, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);

        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }
}
