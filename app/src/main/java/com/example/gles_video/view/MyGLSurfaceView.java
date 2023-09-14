package com.example.gles_video.view;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.example.gles_video.IDrawer;
import com.example.gles_video.vr.VRVideoRender;

public class MyGLSurfaceView extends GLSurfaceView {
    public MyGLSurfaceView(Context context) {
        super(context);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private PointF mPrePoint = new PointF();

    private IDrawer mDrawer = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN :
                mPrePoint.x = event.getX();
                mPrePoint.y = event.getY();
                break;
            case MotionEvent.ACTION_MOVE :

                if (mDrawer instanceof VRVideoRender) {
                    float dx = (event.getX() - mPrePoint.x);
                    float dy = (event.getY() - mPrePoint.y);

                    ((VRVideoRender) mDrawer).setRotateAttr(getRotateAxis(dx, dy), getRotateAngle(dx, dy));
                } else {
                    float dx = (event.getX() - mPrePoint.x) / getMeasuredWidth();
                    float dy = (event.getY() - mPrePoint.y) / getMeasuredHeight();
                    mDrawer.translate(dx, dy);
                }

                mPrePoint.x = event.getX();
                mPrePoint.y = event.getY();

                break;
        }
        return true;
    }

    public float[] getRotateAxis(float dx , float dy) {
        float[] axis = new float[]{0 ,0 , 1};

        float dis = (float) Math.sqrt(dx * dx + dy * dy);
        if (dis < 0.00001f) {
            return axis;
        }

        axis[0] = -dy;
        axis[1] = dx;
        axis[2] = 0;

        return axis;
    }

    public float getRotateAngle(float dx, float dy) {
        float w = getWidth();
        float h = getHeight();

        float rate = 360 / (float) Math.sqrt(w * w + h * h);

        float d = (float) Math.sqrt(dx * dx + dy * dy);
        return d * rate;
    }

    public void addDrawer(IDrawer drawer) {
        mDrawer = drawer;
    }
}
