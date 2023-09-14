package com.example.gles_video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class VideoDrawer implements IDrawer {

    // 半透值接收者
    private int mAlphaHandler = -1;

    // 半透明值
    private float mAlpha = 1f;

    private int mWorldWidth = -1;
    private int mWorldHeight = -1;
    private int mVideoWidth = -1;
    private int mVideoHeight = -1;

    private float mHeightRatio = 1f, mWidthRatio = 1f;

    //坐标变换矩阵
    private float[] mMatrix = null;

    //矩阵变换接收者
    private int mVertexMatrixHandler = -1;

    private SurfaceTexture mSurfaceTexture = null;
    private SurfaceTextureHandle mSurfaceTextureHandle = null;

    // 顶点坐标
    private float[] mVertexCoors = new float[]{
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
    };

    // 纹理坐标
    private float[] mTextureCoors = new float[]{
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };

    private int mTextureId = -1;

    //OpenGL程序ID
    private int mProgram = -1;
    // 顶点坐标接收者
    private int mVertexPosHandler = -1;
    // 纹理坐标接收者
    private int mTexturePosHandler = -1;
    // 纹理接收者
    private int mTextureHandler = -1;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;
    private Context mContext;

    public VideoDrawer(Context context) {
        mContext = context;
        initPos();//【步骤1: 初始化顶点坐标】
    }

    private void initPos() {
        mVertexBuffer = ByteBuffer.allocateDirect(mVertexCoors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        mVertexBuffer.put(mVertexCoors);
        mVertexBuffer.position(0);

        mTextureBuffer = ByteBuffer.allocateDirect(mTextureCoors.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureBuffer.put(mTextureCoors);
        mTextureBuffer.position(0);
    }

    public void setTextureID(int id) {
        mTextureId = id;
        mSurfaceTexture = new SurfaceTexture(id);
        mSurfaceTextureHandle.handleSurfaceTexture(mSurfaceTexture);
    }

    public void draw() {
        if (mTextureId != -1) {

            initDefMatrix();

            //【步骤2: 创建、编译并启动OpenGL着色器】
            createGLPrg();
            //【步骤3: 激活并绑定纹理单元】
            activateTexture();
            //【步骤4: 绑定图片到纹理单元】
            updateTexture();
            //【步骤5: 开始渲染绘制】
            doDraw();
        }
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

    private void createGLPrg() {
        if (mProgram == -1) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexShader());
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader());

            //创建OpenGL ES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
            mProgram = GLES20.glCreateProgram();
            //将顶点着色器加入到程序
            GLES20.glAttachShader(mProgram, vertexShader);
            //将片元着色器加入到程序中
            GLES20.glAttachShader(mProgram, fragmentShader);
            //连接到着色器程序
            GLES20.glLinkProgram(mProgram);

            mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "aPosition");
            mTextureHandler = GLES20.glGetUniformLocation(mProgram, "uTexture");
            mTexturePosHandler = GLES20.glGetAttribLocation(mProgram, "aCoordinate");
            mVertexMatrixHandler = GLES20.glGetUniformLocation(mProgram, "uMatrix");
            mAlphaHandler = GLES20.glGetAttribLocation(mProgram, "alpha");
        }
        //使用OpenGL程序
        GLES20.glUseProgram(mProgram);
    }

    private void activateTexture() {
        //激活指定纹理单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //绑定纹理ID到纹理单元
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
        //将激活的纹理单元传递到着色器里面
        GLES20.glUniform1i(mTextureHandler, 0);
        //配置边缘过渡参数
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void updateTexture() {
        mSurfaceTexture.updateTexImage();
    }

    private void doDraw() {
        //启用顶点的句柄
        GLES20.glEnableVertexAttribArray(mVertexPosHandler);
        GLES20.glEnableVertexAttribArray(mTexturePosHandler);
        //设置着色器参数， 第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glVertexAttribPointer(mTexturePosHandler, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);

        GLES20.glUniformMatrix4fv(mVertexMatrixHandler, 1, false, mMatrix, 0);

        GLES20.glVertexAttrib1f(mAlphaHandler, mAlpha);

        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void release() {
        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTexturePosHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
        GLES20.glDeleteProgram(mProgram);
    }

    private String getVertexShader() {

//        String vertexLanguage = Utils.loadFileFromResource(mContext, R.raw.vertex);
//        return vertexLanguage;
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

    private String getFragmentShader() {
//        String textureLanguage = Utils.loadFileFromResource(mContext, R.raw.fragment);
//        return textureLanguage;
        //一定要加换行"\n"，否则会和下一行的precision混在一起，导致编译出错
        return "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;" +
        "varying vec2 vCoordinate;" +
        "varying float inAlpha;" +
        "uniform samplerExternalOES uTexture;" +
        "void main() {" +
        "  vec4 color = texture2D(uTexture, vCoordinate);" +
        "  gl_FragColor = vec4(color.r, color.g, color.b, inAlpha);" +
        "}";
    }

    private int loadShader(int type, String shaderCode) {
        //根据type创建顶点着色器或者片元着色器
        int shader = GLES20.glCreateShader(type);
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public void getSurfaceTexture(SurfaceTextureHandle surfaceTextureHandle) {
        mSurfaceTextureHandle = surfaceTextureHandle;
    }

    @Override
    public void setWorldSize(int worldHeight, int worldWidth) {
        mWorldHeight = worldHeight;
        mWorldWidth = worldWidth;
    }

    @Override
    public void setVideoSize(int videoHeight, int videoWidth) {
        mVideoHeight = videoHeight;
        mVideoWidth = videoWidth;
    }

    @Override
    public void setAlpha(float alpha) {
        mAlpha = alpha;
    }

    // 平移
    public void translate(float dx, float dy) {
        Matrix.translateM(mMatrix, 0, dx * mWidthRatio * 2, -dy * mHeightRatio * 2, 0f);
    }

    @Override
    public int getTextureId() {
        return mTextureId;
    }

}
