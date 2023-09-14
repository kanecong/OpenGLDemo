package com.example.gles_video.EGL;

import android.opengl.EGLContext;
import android.opengl.EGLSurface;

public class EGLSurfaceHolder {
    private EGLCore mEGLCore = null;
    private EGLSurface mEGLSurface = null;

    public void init(EGLContext shareContext, int flag) {
        mEGLCore = new EGLCore();
        mEGLCore.init(shareContext, flag);
    }

    public void createEGLSurface(boolean isVideo, Object surface, int width, int height) {
        if (surface != null) {
            mEGLSurface = mEGLCore.createWindowSurface(surface);
        } else {
            mEGLSurface = mEGLCore.createOffScreenSurface(width, height);
        }
    }

    public EGLSurface createEGLSurface(Object surface) {
        EGLSurface eglSurface = null;
        if (surface != null) {
            eglSurface = mEGLCore.createWindowSurface(surface);
        }
        return eglSurface;
    }

    public void makeCurrent(boolean isVideo) {
        if (mEGLSurface != null) {
            mEGLCore.makeCurrent(mEGLSurface);
        }
    }

    public boolean makeCurrent(EGLSurface eglSurface) {
        if (eglSurface != null) {
            mEGLCore.makeCurrent(eglSurface);
            return true;
        }
        return false;
    }

    public void swapBuffers() {
        if (mEGLSurface != null) {
            mEGLCore.swapBuffers(mEGLSurface);
        }
    }

    public void swapBuffers(EGLSurface eglSurface) {
        if (eglSurface != null) {
            mEGLCore.swapBuffers(eglSurface);
        }
    }

    public void destroyEGLSurface() {
        if (mEGLSurface != null) {
            mEGLCore.destroySurface(mEGLSurface);
            mEGLSurface = null;
        }
    }

    public void destroySurface(EGLSurface eglSurface) {
        if (eglSurface != null) {
            mEGLCore.destroySurface(eglSurface);
        }
    }

    public void release() {
        mEGLCore.release();
    }

    public void setTimestamp(Long timestamp) {
        if (mEGLSurface != null) {
            mEGLCore.setPresentationTime(mEGLSurface, timestamp);
        }
    }

    public void setPresentationTime(EGLSurface eglSurface, long time) {
        if (eglSurface != null) {
            mEGLCore.setPresentationTime(eglSurface, time);
        }
    }
}
