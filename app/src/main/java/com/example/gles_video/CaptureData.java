package com.example.gles_video;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

interface CaptureData {
    void capture(Bitmap bmp, int width, int height);
}
