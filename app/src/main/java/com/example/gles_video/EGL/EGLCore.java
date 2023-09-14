package com.example.gles_video.EGL;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

public class EGLCore {
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    private static final int FLAG_RECORDABLE = 0x01;

    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLConfig mEGLConfig = null;

    public void init(EGLContext eglContext, int flag) {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            throw  new RuntimeException("EGL already set up ");
        }

        EGLContext sharedContext = (eglContext == null) ? EGL14.EGL_NO_CONTEXT: eglContext;

        //1、创建EGLDisplay
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("Unable to get EGL14 display");
        }

        //2、初始化 EGLDisplay
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            throw new RuntimeException("unable to initilize EGL14");
        }

        //3、初始化 EGLConfig, EGLContext
        if (mEGLContext == EGL14.EGL_NO_CONTEXT) {
            EGLConfig config = getConfig(flag, 2);

            if (config == null) {
                throw new RuntimeException("Unable to find a suitable EGLConfig");
            }

            int[] attr2List = new int[]{
                    EGL14.EGL_CONTEXT_CLIENT_VERSION,
                    2,
                    EGL14.EGL_NONE
            };

            EGLContext context = EGL14.eglCreateContext(
                    mEGLDisplay, config, sharedContext,attr2List,0
            );
            mEGLConfig = config;
            mEGLContext = context;
        }
    }

    public EGLConfig getConfig(int flag, int version) {

        int renderType = EGL14.EGL_OPENGL_ES2_BIT;

        if (version >= 3) {
            renderType = renderType | EGLExt.EGL_OPENGL_ES3_BIT_KHR;
        }

        //配置数组，主要是配置 RGBA 和 深度 位数，
        // 两个一对，前面是key， 后面是 value
        // 数组以 EGL14.EGL_NONE 结尾
        int[] attrList = new int[]{
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderType,
                EGL14.EGL_NONE, 0,
                EGL14.EGL_NONE
        };

        //配置 Android 指定标记
        if ((flag & FLAG_RECORDABLE) != 0) {
            attrList[attrList.length - 3] = EGL_RECORDABLE_ANDROID;
            attrList[attrList.length - 2] = 1;
        }

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];

        if (!EGL14.eglChooseConfig(
                mEGLDisplay,
                attrList,
                0,
                configs,
                0,
                configs.length,
                numConfigs,
                0)) {
            Log.e("gaorui", "Unable to find RGB888 / " + version + " EGLConfig");
            return null;
        }

        return configs[0];
    }

    //创建可显示的渲染缓存
    public EGLSurface createWindowSurface(Object surface) {
        if (!(surface instanceof Surface) && (surface instanceof SurfaceTexture)) {
            throw new RuntimeException("iNVALID surface : " + surface);
        }

        int[] surfaceAttr = new int[]{EGL14.EGL_NONE};

        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(
                mEGLDisplay, mEGLConfig, surface, surfaceAttr, 0
        );

        if (eglSurface == null) {
            throw new RuntimeException("Surafce is null");
        }

        return eglSurface;
    }

    //创建离屏渲染缓存
    public EGLSurface createOffScreenSurface(int width, int height) {

        int[] surfaceAttr = new int[]{
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };

        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(
                mEGLDisplay, mEGLConfig, surfaceAttr, 0
        );

        if (eglSurface == null) {
            throw new RuntimeException("surface is null");
        }

        return eglSurface;
    }

    //当前渲染线程 与 上下文 绑定
    public void makeCurrent(EGLSurface eglSurface) {
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("EGLDisplay is null, call init first");
        }
        if (!EGL14.eglMakeCurrent(
                mEGLDisplay, eglSurface, eglSurface, mEGLContext)
        ) {
            throw new RuntimeException("makeCurrent(eglsurface) failed");
        }
    }

    //当前进程 与 上下文 绑定
    public void makeCurrent(EGLSurface drawSurface, EGLSurface readSurface) {
        if (mEGLDisplay == null) {
            throw new RuntimeException("EGLDisplay is null, call init first");
        }

        if (EGL14.eglMakeCurrent(
                mEGLDisplay, drawSurface, readSurface, mEGLContext)
        ) {
            throw new RuntimeException("makeCurrent(draw,read) failed");
        }
    }

    //将缓存对象 发送到 设备显示
    public boolean swapBuffers(EGLSurface eglSurface) {
        return EGL14.eglSwapBuffers(mEGLDisplay, eglSurface);
    }

    //设置当前帧的时间，单位：纳秒
    public void setPresentationTime(EGLSurface eglSurface, long nsec) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, eglSurface, nsec);
    }

    //销毁 EGLSurface ,解绑 上下文
    public void destroySurface(EGLSurface eglSurface) {
        EGL14.eglMakeCurrent(
                mEGLDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
        );

        EGL14.eglDestroySurface(mEGLDisplay, eglSurface);
    }

    //释放资源
    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                    mEGLDisplay,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT
            );

            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }

        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLConfig = null;
    }

    /**
     * 绘制
     * @param timestamp Long 时间戳
     */
    public void draw(EGLSurface eglSurface, long timestamp) {

        // 设置当前帧的时间戳
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, eglSurface, timestamp);
        EGL14.eglSwapBuffers(mEGLDisplay, eglSurface);
    }
}
