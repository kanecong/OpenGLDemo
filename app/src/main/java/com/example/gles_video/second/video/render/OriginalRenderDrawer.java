package com.example.gles_video.second.video.render;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import com.example.gles_video.SoulVideoDrawer;
import com.example.gles_video.second.video.BaseVideoDrawer;

/**
 * Created By gaorui on 2018/8/27
 */
public class OriginalRenderDrawer extends BaseVideoDrawer {

    public OriginalRenderDrawer(Context context) {
        super(context);
    }

    @Override
    public void draw(long timestamp) {
        super.draw();
    }

}
