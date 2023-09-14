package com.example.gles_video;

public interface IDrawer {
    void draw();
    void setTextureID(int textureID);
    void release();
    void getSurfaceTexture(SurfaceTextureHandle surfaceTextureHandle);
    void setWorldSize(int worldHeight, int worldWidth);   // GL窗口
    void setVideoSize(int videoHeight, int videoWidth);   // video原始尺寸
    void setAlpha(float alpha);
    void translate(float dx, float dy);
    int getTextureId();
}
