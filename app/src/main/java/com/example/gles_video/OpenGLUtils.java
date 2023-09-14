package com.example.gles_video;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES20;

public class OpenGLUtils {
    public static int createFBOTextureID(int width, int height) {
        //新建纹理
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        //根据参数，为上面的纹理ID，生成一个2D纹理
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        //配置边缘过渡参数
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textures[0];
    }

    public static int createFBO() {
        int[] frameBuffer = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        return frameBuffer[0];
    }

    public static void bindFBO(int fbo, int textureID) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureID, 0);
    }

    public static void unBindFBO() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public static void deleteFBO(int[] fbo, int[] textureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteFramebuffers(1, fbo, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDeleteTextures(1, textureId, 0);
    }
}
