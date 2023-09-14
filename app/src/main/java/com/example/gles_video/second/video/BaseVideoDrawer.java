package com.example.gles_video.second.video;

import android.content.Context;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.example.gles_video.OpenGLUtils;
import com.example.gles_video.SurfaceTextureHandle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BaseVideoDrawer {

    /**上下颠倒的顶点矩阵*/
    public float[] mReserveVertexCoors = new float[]{
            -1f, 1f,
            1f, 1f,
            -1f, -1f,
            1f, -1f
    };

    private float[] mDefVertexCoors = new float[]{
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
    };

    // 顶点坐标
    public float[] mVertexCoors = mDefVertexCoors;

    // 灵魂帧缓冲
    private int mSoulFrameBuffer = -1;

    // 灵魂纹理ID
    private int mSoulTextureId = -1;

    // 灵魂纹理接收者
    public int mSoulTextureHandler = -1;

    // 灵魂缩放进度接收者
    public int mProgressHandler = -1;

    private float progress = 0;

    // 是否更新FBO纹理
    public int mDrawFbo = 1;

    // 更新FBO标记接收者
    public int mDrawFobHandler = -1;

    // 一帧灵魂的时间
    private long mModifyTime = -1;

    // 半透值接收者
    public int mAlphaHandler = -1;

    // 半透明值
    public float mAlpha = 1f;

    public int mWorldWidth = -1;
    public int mWorldHeight = -1;
    public int mVideoWidth = -1;
    public int mVideoHeight = -1;

    private float mHeightRatio = 1f, mWidthRatio = 1f;

    //坐标变换矩阵
    public float[] mMatrix = null;
    public float[] mOriginMatrix = new float[]{
            1f, 0f, 0f,0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
    };

    //矩阵变换接收者
    public int mVertexMatrixHandler = -1;

    private SurfaceTexture mSurfaceTexture = null;
    private SurfaceTextureHandle mSurfaceTextureHandle = null;


    // 纹理坐标
    public float[] mTextureCoors = new float[]{
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };

    private int mTextureId = -1;

    //OpenGL程序ID
    public int mProgram = -1;
    // 顶点坐标接收者
    public int mVertexPosHandler = -1;
    // 纹理坐标接收者
    public int mTexturePosHandler = -1;
    // 纹理接收者
    public int mTextureHandler = -1;

    public FloatBuffer mVertexBuffer;
    public FloatBuffer mTextureBuffer;
    private Context mContext;

    private volatile boolean mNeedCapture = false;
    private volatile boolean mNeedFBOCapture = false;

    public int mVertexBufferId = -1;
    public int mDisplayTextureBufferId = -1;

    private int mOutputTextureId = -1;

    public BaseVideoDrawer(Context context) {
        mContext = context;
        initPos();//【步骤1: 初始化顶点坐标】
    }

    public void initPos() {

        int[] vbo = new int[2];
        GLES30.glGenBuffers(2, vbo, 0);

        mVertexBuffer = ByteBuffer.allocateDirect(mVertexCoors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        mVertexBuffer.put(mVertexCoors);
        mVertexBuffer.position(0);
        mVertexBufferId = vbo[0];

        // ARRAY_BUFFER 将使用 Float*Array 而 ELEMENT_ARRAY_BUFFER 必须使用 Uint*Array
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mVertexCoors.length * 4, mVertexBuffer, GLES30.GL_STATIC_DRAW);

        mTextureBuffer = ByteBuffer.allocateDirect(mTextureCoors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureBuffer.put(mTextureCoors);
        mTextureBuffer.position(0);
        mDisplayTextureBufferId = vbo[1];

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mDisplayTextureBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mTextureCoors.length * 4, mTextureBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0);
    }

    public void setInputTextureId(int textureId) {
        Log.e("gaorui", "BaseVideoDrawer - setInputTextureId");
        mTextureId = textureId;
    }

    public void draw(long stamp) {

    }

    public void draw() {
        if (mTextureId != -1) {

            GLES30.glClearColor(0, 0, 0 , 1);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

            GLES30.glViewport(0, 0, mWorldWidth, mWorldHeight);

            initDefMatrix();

            //【步骤2: 创建、编译并启动OpenGL着色器】
            createGLPrg();
            //【步骤3.1: 激活并绑定纹理单元】

            updateFBO();             //soul纹理绘制

            activateSoulTexture();   //激活soul纹理单元，方便下方显示

            //--------执行基本画面渲染，画面将渲染到设备上--------------

            //【步骤3.2: 激活并绑定纹理单元】
            activateDefTexture();
            //【步骤4: 绑定图片到纹理单元】
//            updateTexture();

            //--------执行基本画面渲染，画面将渲染到设备上--------------

            //【步骤5: 开始渲染绘制】
            doDraw();

            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        }
    }

    public void updateFBO() {
        //【1，创建FBO纹理】
        if (mSoulTextureId == -1) {
            mSoulTextureId = OpenGLUtils.createFBOTextureID(mVideoWidth, mVideoHeight);
        }
        // 【2，创建FBO】
        if (mSoulFrameBuffer == -1) {
            mSoulFrameBuffer = OpenGLUtils.createFBO();
        }

        // 【3，渲染到FBO】
        if (System.currentTimeMillis() - mModifyTime > 1000) {
            mModifyTime = System.currentTimeMillis();

            //--------画面将渲染到FBO上--------------

            // 绑定FBO
            OpenGLUtils.bindFBO(mSoulFrameBuffer, mSoulTextureId);
            // 配置FBO窗口
            configFboViewport();

            //--------执行正常画面渲染，画面将渲染到FBO上--------------

            // 激活默认的纹理单元
            activateDefTexture();
            // 更新纹理
//            updateTexture();
            // 绘制到FBO
            doDraw();

            //---------------------------------------------------

            // 解绑FBO
            OpenGLUtils.unBindFBO();

            //--------画面将渲染到FBO上--------------

            // 恢复默认绘制窗口
            configDefViewport();
        }
    }

    /**
     * 配置FBO窗口
     */
    private void configFboViewport() {
        mDrawFbo = 1;
        // 将变换矩阵回复为单位矩阵（将画面拉升到整个窗口大小，设置窗口比例和FBO纹理比例一致，画面刚好可以正常绘制到FBO纹理上）
//        Matrix.setIdentityM(mMatrix, 0);
        // 设置颠倒的顶点坐标
        mVertexCoors = mReserveVertexCoors;
        //重新初始化顶点坐标
        initPos();
        GLES30.glViewport(0, 0, mVideoWidth, mVideoHeight);
    }

    /**
     * 配置默认显示的窗口
     */
    private void configDefViewport() {
        mDrawFbo = 0;
        // 恢复顶点坐标
        mVertexCoors = mDefVertexCoors;
        initPos();

        // 恢复窗口
        GLES30.glViewport(0, 0, mWorldWidth, mWorldHeight);
    }

    private void activateDefTexture() {
        activateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId, 0, mTextureHandler);
    }

    private void activateSoulTexture() {
        activateTexture(GLES30.GL_TEXTURE_2D, mSoulTextureId, 1, mSoulTextureHandler);
    }

    public void initDefMatrix() {
        if (mMatrix != null) return;
        if (mVideoWidth != -1 && mVideoHeight != -1 &&
                mWorldWidth != -1 && mWorldHeight != -1) {
            mMatrix = new float[16];

            float originRatio = (float)mVideoWidth / mVideoHeight;
            float worldRatio = (float)mWorldWidth / mWorldHeight;

            if (mWorldWidth > mWorldHeight) {
                if (originRatio > worldRatio) {
                    mHeightRatio = originRatio / worldRatio;
                    Matrix.orthoM(
                            mMatrix, 0,
                            -1f, 1f,
                            -mHeightRatio, mHeightRatio,
                            -1f, 3f
                    );
                } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    mWidthRatio = worldRatio / originRatio;
                    Matrix.orthoM(
                            mMatrix, 0,
                            -mWidthRatio, mWidthRatio,
                            -1f, 1f,
                            -1f, 3f
                    );
                }
            } else {
                if (originRatio > worldRatio) {
                    mHeightRatio = originRatio / worldRatio;
                    Matrix.orthoM(
                            mMatrix, 0,
                            -1f, 1f,
                            -mHeightRatio, mHeightRatio,
                            -1f, 3f
                    );
                } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    mWidthRatio = worldRatio / originRatio;
                    Matrix.orthoM(
                            mMatrix, 0,
                            -mWidthRatio, mWidthRatio,
                            -1f, 1f,
                            -1f, 3f
                    );
                }
            }
        }
    }

    public void createGLPrg() {
        if (mProgram == -1) {
            int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, getVertexShader());
            int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, getFragmentShader());

            //创建OpenGL ES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
            mProgram = GLES30.glCreateProgram();
            //将顶点着色器加入到程序
            GLES30.glAttachShader(mProgram, vertexShader);
            //将片元着色器加入到程序中
            GLES30.glAttachShader(mProgram, fragmentShader);
            //连接到着色器程序
            GLES30.glLinkProgram(mProgram);

            mVertexPosHandler = GLES30.glGetAttribLocation(mProgram, "aPosition");
            mTextureHandler = GLES30.glGetUniformLocation(mProgram, "uTexture");
            mTexturePosHandler = GLES30.glGetAttribLocation(mProgram, "aCoordinate");
            mVertexMatrixHandler = GLES30.glGetUniformLocation(mProgram, "uMatrix");
            mAlphaHandler = GLES30.glGetAttribLocation(mProgram, "alpha");

            mProgressHandler = GLES30.glGetUniformLocation(mProgram, "progress");
            mDrawFobHandler = GLES30.glGetUniformLocation(mProgram, "drawFbo");
            mSoulTextureHandler = GLES30.glGetUniformLocation(mProgram, "uSoulTexture");
        }
        //使用OpenGL程序
        GLES30.glUseProgram(mProgram);
    }

    private void activateTexture(int type, int textureId, int index, int textureHandler) {
        //激活指定纹理单元
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + index);
        //绑定纹理ID到纹理单元
        GLES30.glBindTexture(type, textureId);
        //将激活的纹理单元传递到着色器里面
        GLES30.glUniform1i(textureHandler, index);
        //配置边缘过渡参数
        GLES30.glTexParameterf(type, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(type, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(type, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(type, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    }

    private void updateTexture() {
        mSurfaceTexture.updateTexImage();
    }

    private void doDraw() {
        //启用顶点的句柄
        GLES30.glEnableVertexAttribArray(mVertexPosHandler);
        GLES30.glEnableVertexAttribArray(mTexturePosHandler);
        //设置着色器参数， 第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
//        GLES30.glVertexAttribPointer(mVertexPosHandler, 2, GLES30.GL_FLOAT, false, 0, mVertexBuffer);
//        GLES30.glVertexAttribPointer(mTexturePosHandler, 2, GLES30.GL_FLOAT, false, 0, mTextureBuffer);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES30.glVertexAttribPointer(mVertexPosHandler, 2, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mDisplayTextureBufferId);
        GLES30.glVertexAttribPointer(mTexturePosHandler, 2, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0);

        GLES30.glUniformMatrix4fv(mVertexMatrixHandler, 1, false, mDrawFbo==1 ? mOriginMatrix: mMatrix, 0);

        GLES30.glVertexAttrib1f(mAlphaHandler, mAlpha);

        GLES30.glUniform1f(mDrawFobHandler, mDrawFbo);

        GLES30.glUniform1f(mProgressHandler, (System.currentTimeMillis() - mModifyTime) / 1000f);

        //开始绘制
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void release() {
        GLES30.glDisableVertexAttribArray(mVertexPosHandler);
        GLES30.glDisableVertexAttribArray(mTexturePosHandler);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glDeleteTextures(1, new int[]{mTextureId}, 0);
        GLES30.glDeleteProgram(mProgram);

        OpenGLUtils.deleteFBO(new int[]{mSoulFrameBuffer}, new int[]{mSoulTextureId});
    }

    public String getVertexShader() {

        return "attribute vec4 aPosition;" +
                "attribute vec2 aCoordinate;" +
                "uniform mat4 uMatrix;" +
                "varying vec2 vCoordinate;" +
                "attribute float alpha;" +
                "varying float inAlpha;" +
                "void main() {" +
                "    gl_Position = uMatrix * aPosition;" +
                "    vCoordinate = aCoordinate;" +
                "    inAlpha = alpha;" +
                "}";
    }

    public String getFragmentShader() {

        //一定要加换行"\n"，否则会和下一行的precision混在一起，导致编译出错
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;" +
                "varying vec2 vCoordinate;" +
                "varying float inAlpha;" +
                "uniform samplerExternalOES uTexture;" +
                "uniform float progress;" +
                "uniform int drawFbo;" +
                "uniform sampler2D uSoulTexture;" +
                "void main() {" +

                "  if (drawFbo <= 1) {" +

                // 透明度[0,0.6]
                "  float alpha = 0.6 * (1.0 - progress);" +
                // 缩放比例[1.0,1.5]
                "  float scale = 1.0 + (1.5 - 1.0) * progress;" +

                // 放大纹理坐标
                "  float soulX = 0.5 + (vCoordinate.x - 0.5) / scale;\n" +
                "  float soulY = 0.5 + (vCoordinate.y - 0.5) / scale;\n" +
                "  vec2 soulTextureCoords = vec2(soulX, soulY);" +
                // 获取对应放大纹理坐标下的像素(颜色值rgba)
                "  vec4 soulMask = texture2D(uSoulTexture, soulTextureCoords);" +

                "  vec4 color = texture2D(uTexture, vCoordinate);" +

                "  if (drawFbo == 0) {" +
                // 颜色混合 默认颜色混合方程式 = mask * (1.0-alpha) + weakMask * alpha
                "    gl_FragColor = color * (1.0 - alpha) + soulMask * alpha;" +
                "  } else {" +
                "    gl_FragColor = vec4(color.r, color.g, color.b, inAlpha);" +
                "  }" +

                "  } else {" +
                "  vec4 color = texture2D(uSoulTexture, vCoordinate);" +
                "  gl_FragColor = vec4(color.r, color.g, color.b, inAlpha);" +
                "  }" +
                "}";
    }

    public int loadShader(int type, String shaderCode) {
        //根据type创建顶点着色器或者片元着色器
        int shader = GLES30.glCreateShader(type);
        //将资源加入到着色器中，并编译
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        return shader;
    }

    public void setWorldSize(int worldHeight, int worldWidth) {
        mWorldHeight = worldHeight;
        mWorldWidth = worldWidth;

        mOutputTextureId = OpenGLUtils.createFBOTextureID(mWorldWidth, mWorldHeight);
        Log.e("gaorui", "BaseVideoDrawer - setWorldSize - mOutputTextureId : " + mOutputTextureId);
    }

    public int getOutputTextureId() {
        return mOutputTextureId;
    }

    public void setWorldSize(int worldHeight, int worldWidth, boolean need) {
        mWorldHeight = worldHeight;
        mWorldWidth = worldWidth;
    }

    public void setVideoSize(int videoHeight, int videoWidth) {
        mVideoHeight = videoHeight;
        mVideoWidth = videoWidth;
    }

}
