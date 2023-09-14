package com.example.gles_video.second.video.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import com.example.gles_video.second.video.BaseVideoDrawer;

/**
 * Created By gaorui on 2018/8/27
 */
public class DisplayRenderDrawer extends BaseVideoDrawer {

    private int av_Position;
    private int af_Position;
    private int s_Texture;

    private int mTextureId;

    public DisplayRenderDrawer(Context context) {
        super(context);
    }

    @Override
    public void draw(long timestamp) {
//        GLES20.glClearColor(0, 0, 0 , 0);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        createGLPrg(true);

        onDraw();
    }

    public void createGLPrg(boolean isNeedLocal) {
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
            mTexturePosHandler = GLES30.glGetAttribLocation(mProgram, "aCoordinate");
            mSoulTextureHandler = GLES30.glGetUniformLocation(mProgram, "uSoulTexture");
        }
        //使用OpenGL程序
        GLES30.glUseProgram(mProgram);
    }

    protected void onDraw() {

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glUniform1i(mSoulTextureHandler, 0);

        //启用顶点的句柄
        GLES20.glEnableVertexAttribArray(mVertexPosHandler);
        GLES20.glEnableVertexAttribArray(mTexturePosHandler);
        //设置着色器参数， 第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
//        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
//        GLES20.glVertexAttribPointer(mTexturePosHandler, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mDisplayTextureBufferId);
        GLES20.glVertexAttribPointer(mTexturePosHandler, 2, GLES20.GL_FLOAT, false, 0, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0);

        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void setInputTextureId(int textureId) {
        this.mTextureId = textureId;
        Log.e("gaorui", "DisplayRenderDrawer - setInputTextureId = " + this.mTextureId);
    }

    @Override
    public String getVertexShader() {

        return "attribute vec4 aPosition;" +
                "attribute vec2 aCoordinate;" +
                "varying vec2 vCoordinate;" +
                "void main() {" +
                "    gl_Position = aPosition;" +
                "    vCoordinate = aCoordinate;" +
                "}";
    }

    @Override
    public String getFragmentShader() {

        //一定要加换行"\n"，否则会和下一行的precision混在一起，导致编译出错
        return  "precision mediump float;" +
                "varying vec2 vCoordinate;" +
                "uniform sampler2D uSoulTexture;" +
                "void main() {" +
                "  vec4 color = texture2D(uSoulTexture, vec2(vCoordinate.x, 1.0 - vCoordinate.y));" +
                "  gl_FragColor = vec4(color.r, color.g, color.b, 1);" +
                "}";
    }
}
