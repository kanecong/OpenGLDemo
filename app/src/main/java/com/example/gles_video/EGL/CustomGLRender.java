package com.example.gles_video.EGL;

import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.example.gles_video.IDrawer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import androidx.annotation.NonNull;

public class CustomGLRender implements SurfaceHolder.Callback {

    private final RenderThread mThread = new RenderThread();

    private WeakReference<SurfaceView> mSurfaceView = null;

    private final ArrayList<IDrawer> mDrawers = new ArrayList<IDrawer>();

    private Surface mSurface = null;
    private Surface mVideoSurface = null;
    private volatile boolean isVideo = false;

    public CustomGLRender() {
        mThread.start();
    }

    public void setSurface(SurfaceView surface) {
        mSurfaceView = new WeakReference<SurfaceView>(surface);
        surface.getHolder().addCallback(this);

        surface.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {

            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                mThread.onSurfaceStop();
                mSurface = null;
                mVideoSurface = null;
            }
        });
    }

    public void stopVideo() {
        isVideo = false;
        mThread.onSurafceCreate();
        mThread.onSurfaceChanged(mSurfaceView.get().getWidth(), mSurfaceView.get().getHeight());
    }

    //-------------------新增部分-----------------

    // 新增设置Surface接口
    public void setSurface(Surface surface, int width, int height) {
        mVideoSurface = surface;
        isVideo = true;
        mThread.onSurafceCreate();
//        mThread.onSurfaceChanged(width, height);
    }

    // 新增设置渲染模式 RenderMode见下面
    public void setRenderMode(RenderMode mode) {
        mThread.setRenderMode(mode);
    }

//----------------------------------------------

    public void addDrawer(IDrawer drawer) {
        mDrawers.add(drawer);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mSurface = holder.getSurface();
        mThread.onSurafceCreate();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        mThread.onSurfaceChanged(width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mThread.onSurfaceDestroy();
    }

    public EGLContext getEGLContextCurr() {
        return mThread.getCurrentEGLContext();
    }

    //---------新增渲染模式定义------------
    public enum RenderMode {
        // 自动循环渲染
        RENDER_CONTINUOUSLY,
        // 由外部通过notifySwap通知渲染
        RENDER_WHEN_DIRTY
    }
    //---------------------

    class RenderThread extends Thread{

        private static final int EGL_RECORDABLE_ANDROID = 0x3142;

        private RenderState mState = RenderState.NO_SURFACE;

        private EGLSurfaceHolder mEGLSurface = null;

        private boolean mHaveBindGLContext = false;

        //是否以已经新建过 EGL 上下文，用于判断是否需要生产新的纹理ID
        private boolean mNeverCreateEGLContext = true;

        private int mWidth = 0;
        private int mHeight = 0;

        private RenderMode mRenderMode = RenderMode.RENDER_WHEN_DIRTY;

        private Long mLastTimestamp = 0L;

        private Object mWaitLock = new Object();

        private boolean mNeedRecord = false;


        public void setRenderMode(RenderMode mode) {
            mRenderMode = mode;
        }

//1、线程等待与解锁--------------------
        public void holdOn() {
            synchronized (mWaitLock){
                try {
                    mWaitLock.wait();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void notifyGo() {
            synchronized (mWaitLock){
                try {
                    mWaitLock.notify();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //-------------------------------

//2、surface 声明周期转发函数------------------------
        public void onSurafceCreate() {
            mState = RenderState.FRESH_SURFACE;
            notifyGo();
            Log.e("gaorui", "onSurafceCreate");
        }

        public void onSurfaceChanged(int width, int height) {
            mWidth = width;
            mHeight = height;
            mState = RenderState.SURFACE_CHANGE;
            notifyGo();
            Log.e("gaorui", "onSurfaceChanged");
        }

        public void onSurfaceDestroy() {
            mState = RenderState.SURFACE_DESTROY;
            holdOn();
            Log.e("gaorui", "onSurfaceDestroy");
        }

        public void onSurfaceStop() {
            mState = RenderState.STOP;
            notifyGo();
        }
        //-------------------------------------

//3、openGL 渲染循环-------------------------------------
        @Override
        public void run() {
            //3.1、初始化 EGL
            initEGL();

            while (true) {
                switch (mState) {
                    case FRESH_SURFACE:
                        //3.2 使用surface 初始化 EGLSurface，绑定上下文
                        if (isVideo) {
                            mNeedRecord = true;
                            createEGLSurfaceFirst();
                            mState = RenderState.RENDERING;
                        } else if (mNeedRecord){
                            createEGLSurfaceFirst();
                            mNeedRecord = false;
                            mState = RenderState.RENDERING;
                        } else {
                            createEGLSurfaceFirst();
                            holdOn();
                        }

                        Log.e("gaorui", "FRESH_SURFACE");
                        break;

                    case SURFACE_CHANGE:
                        createEGLSurfaceFirst();
                        //3.3 初始化 openGL 世界宽高
                        GLES20.glViewport(0,0,mWidth, mHeight);
                        configWorldSize();
                        mState = RenderState.RENDERING;
                        Log.e("gaorui", "SURFACE_CHANGE");
                        break;

                    case RENDERING:
                        //3.4 渲染
                        render();
                        break;

                    case SURFACE_DESTROY:
                        //3.5 销毁 EGLSurface ,解绑上下文
                        destroyEGLSurface();
                        mState = RenderState.NO_SURFACE;
                        notifyGo();
                        Log.e("gaorui", "SURFACE_DESTROY");
                        break;

                    case STOP:
                        //3.6 释放所有资源
                        releseEGL();
                        return;

                    default:
                        holdOn();

                }

                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        //----------------------------------

//4、EGL 相关操作 --------------------------------------------
        private void initEGL() {
            mEGLSurface = new EGLSurfaceHolder();
            mEGLSurface.init(null, EGL_RECORDABLE_ANDROID);
        }

        public void createEGLSurfaceFirst() {
            if (!mHaveBindGLContext || mNeedRecord) {
                mHaveBindGLContext = true;
                createEGLSurface();
                if (mNeverCreateEGLContext) {
                    mNeverCreateEGLContext = false;
                    generateTextureId();
                }
            }
        }

        public void createEGLSurface() {
            mEGLSurface.createEGLSurface(
                     false, mNeedRecord? mVideoSurface:mSurface,
                    -1, -1
            );

            //只要你是在渲染线程中调用任何OpenGL ES的API（比如生产纹理ID的方法GLES20.glGenTextures），OpenGL会自动根据当前线程，切换上下文（也就是切换OpenGL的渲染信息和资源）。
            mEGLSurface.makeCurrent(false);
            Log.e("gaorui", "SURFACE_DESTROY");
        }

        public void destroyEGLSurface() {
            mEGLSurface.destroyEGLSurface();
            mHaveBindGLContext = false;
            mNeedRecord = false;
        }

        public void releseEGL() {
            mEGLSurface.release();
        }
        //---------------------------------------

//5、openGL ES 相关操作-----------------------------------
        public void generateTextureId() {
            int[] textureID = new int[mDrawers.size()];
            GLES20.glGenTextures(mDrawers.size(), textureID, 0);

            for (IDrawer drawer : mDrawers) {
                drawer.setTextureID(textureID[mDrawers.indexOf(drawer)]);
            }
        }

        public void configWorldSize() {
            for (IDrawer drawer: mDrawers) {
                drawer.setWorldSize(mHeight, mWidth);
            }
        }

        public void render() {

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);

            for (IDrawer drawer:mDrawers) {
                drawer.draw();
            }

            mEGLSurface.swapBuffers();

        }

        public EGLContext getCurrentEGLContext() {
            return EGL14.eglGetCurrentContext();
        }
    }
}
