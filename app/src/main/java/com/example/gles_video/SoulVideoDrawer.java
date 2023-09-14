package com.example.gles_video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class SoulVideoDrawer implements IDrawer {

    /**上下颠倒的顶点矩阵*/
    private float[] mReserveVertexCoors = new float[]{
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
    private float[] mVertexCoors = mDefVertexCoors;

    // 灵魂帧缓冲
    private int mSoulFrameBuffer = -1;

    // 灵魂纹理ID
    private int mSoulTextureId = -1;

    // 灵魂纹理接收者
    private int mSoulTextureHandler = -1;

    // 灵魂缩放进度接收者
    private int mProgressHandler = -1;

    private float progress = 0;

    // 是否更新FBO纹理
    private int mDrawFbo = 1;

    // 更新FBO标记接收者
    private int mDrawFobHandler = -1;

    // 一帧灵魂的时间
    private long mModifyTime = -1;

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
    private float[] mOriginMatrix = new float[]{
            1f, 0f, 0f,0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
    };

    //矩阵变换接收者
    private int mVertexMatrixHandler = -1;

    private SurfaceTexture mSurfaceTexture = null;
    private SurfaceTextureHandle mSurfaceTextureHandle = null;


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

    private volatile boolean mNeedCapture = false;
    private volatile boolean mNeedFBOCapture = false;
    private CaptureData mCall = null;

    public SoulVideoDrawer(Context context) {
        mContext = context;
        initPos();//【步骤1: 初始化顶点坐标】
    }

    public void initPos() {
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
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {

            }
        });
        mSurfaceTextureHandle.handleSurfaceTexture(mSurfaceTexture);
    }

    public int getTextureId() {
        return mTextureId;
    }

    public void setCaptureDataListener(CaptureData call) {
        mNeedCapture = true;
        mNeedFBOCapture = true;
        mCall = call;
    }

    public void drawVideo() {
        if (mTextureId != -1) {
            Log.e("gaorui", "drawVideo");

            //【步骤2: 创建、编译并启动OpenGL着色器】
            createGLPrg();
            //【步骤3.1: 激活并绑定纹理单元】

//            activateSoulTexture();   //激活soul纹理单元，方便下方显示

            activateDefTexture();


            //--------执行基本画面渲染，画面将渲染到设备上--------------

            //【步骤5: 开始渲染绘制】
            doDraw();
        }
    }

    public void draw() {
        if (mTextureId != -1) {

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
            updateTexture();

            //--------执行基本画面渲染，画面将渲染到设备上--------------

            //【步骤5: 开始渲染绘制】
            doDraw();

            if (mNeedCapture) {
                mNeedCapture = false;
                Log.e("gaorui", "draw - mNeedCapture");
                IntBuffer dataCap = IntBuffer.allocate(mWorldHeight*mWorldWidth);
                // 斜对角 pixel 偏差
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                GLES20.glReadPixels(0,0,mWorldWidth, mWorldHeight, GLES20.GL_RGBA ,GLES20.GL_UNSIGNED_BYTE,dataCap);

                int[] colors = dataCap.array();
                int[] colors2 = new int[colors.length];

                // 上下颠倒
                for (int y = 0; y < mWorldHeight; y++) {
                    System.arraycopy(colors, y * mWorldWidth, colors2, (mWorldHeight - 1 - y) * mWorldWidth, mWorldWidth);
                }

                Bitmap bitmap = Bitmap.createBitmap(mWorldWidth, mWorldHeight, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(IntBuffer.wrap(colors2));

                mCall.capture(bitmap, mWorldWidth, mWorldHeight);
            }
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
            updateTexture();
            // 绘制到FBO
            doDraw();

            if (mNeedFBOCapture) {
                mNeedFBOCapture = false;
                Log.e("gaorui", "updateFBO - mNeedCapture");
                ByteBuffer dataCap = ByteBuffer.allocate(mVideoHeight*mVideoWidth*4);

                //本身坐标系是倒着的，glReadPixels是倒着读的，所以 最后image是正着的，不需要变换
                GLES20.glReadPixels(0,0,mVideoWidth, mVideoHeight, GLES20.GL_RGBA ,GLES20.GL_UNSIGNED_BYTE,dataCap);

                Bitmap bitmap = Bitmap.createBitmap(mVideoWidth, mVideoHeight, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(dataCap);

                mCall.capture(bitmap, mVideoWidth, mVideoHeight);
            }

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
        GLES20.glViewport(0, 0, mVideoWidth, mVideoHeight);
    }

    /**
     * 配置默认显示的窗口
     */
    private void configDefViewport() {
        mDrawFbo = 0;
//        mMatrix = null;
        // 恢复顶点坐标
        mVertexCoors = mDefVertexCoors;
        initPos();
//        initDefMatrix();

        // 恢复窗口
        GLES20.glViewport(0, 0, mWorldWidth, mWorldHeight);
    }

    private void activateDefTexture() {
        activateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId, 0, mTextureHandler);
    }

    private void activateSoulTexture() {
        activateTexture(GLES20.GL_TEXTURE_2D, mSoulTextureId, 1, mSoulTextureHandler);
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

            mProgressHandler = GLES20.glGetUniformLocation(mProgram, "progress");
            mDrawFobHandler = GLES20.glGetUniformLocation(mProgram, "drawFbo");
            mSoulTextureHandler = GLES20.glGetUniformLocation(mProgram, "uSoulTexture");
        }
        //使用OpenGL程序
        GLES20.glUseProgram(mProgram);
    }

    private void activateTexture(int type, int textureId, int index, int textureHandler) {
        //激活指定纹理单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + index);
        //绑定纹理ID到纹理单元
        GLES20.glBindTexture(type, textureId);
        //将激活的纹理单元传递到着色器里面
        GLES20.glUniform1i(textureHandler, index);
        //配置边缘过渡参数
        GLES20.glTexParameterf(type, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(type, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(type, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(type, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
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

        GLES20.glUniformMatrix4fv(mVertexMatrixHandler, 1, false, mDrawFbo==1 ? mOriginMatrix: mMatrix, 0);

        GLES20.glVertexAttrib1f(mAlphaHandler, mAlpha);

        GLES20.glUniform1f(mDrawFobHandler, mDrawFbo);

        GLES20.glUniform1f(mProgressHandler, (System.currentTimeMillis() - mModifyTime) / 1000f);

        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void release() {
        GLES20.glDisableVertexAttribArray(mVertexPosHandler);
        GLES20.glDisableVertexAttribArray(mTexturePosHandler);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
        GLES20.glDeleteProgram(mProgram);

        OpenGLUtils.deleteFBO(new int[]{mSoulFrameBuffer}, new int[]{mSoulTextureId});
    }

    private String getVertexShader() {

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

    public int createFrameTexture(int width, int height) {
        if (width <= 0 || height <= 0) {
            Log.e("gaorui", "createOutputTexture: width or height is 0");
            return -1;
        }
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        if (textures[0] == 0) {
            Log.e("gaorui", "createFrameTexture: glGenTextures is 0");
            return -1;
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        return textures[0];
    }

}
