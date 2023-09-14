package com.example.gles_video.vr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import com.example.gles_video.IDrawer;
import com.example.gles_video.R;
import com.example.gles_video.SimpleRender;
import com.example.gles_video.SoulVideoDrawer;
import com.example.gles_video.SurfaceTextureHandle;
import com.example.gles_video.view.MyGLSurfaceView;

import java.io.IOException;
import java.util.ArrayList;

public class VRActivity extends AppCompatActivity {

    private MyGLSurfaceView glSurfaceView;
    private SensorManager mSensorManager = null;
    private Sensor mSensor = null;
    private SimpleRender mSimpleRender = null;
    private MediaPlayer mediaPlayer = null;
    private VRVideoRender drawer2 = null;
    private float[] mMatrix = new float[16];
    private ArrayList<IDrawer> mDrawerList = new ArrayList<>();
    private SurfaceTexture mDrawerSurfaceTexture = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vractivity);

        glSurfaceView = findViewById(R.id.vr_gl_surface);

        mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        initSecondRender();

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                SensorManager.getRotationMatrixFromVector(mMatrix, event.values);
                if (drawer2 != null) {
                    drawer2.setMatrix(mMatrix);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        initGL();
    }

    public void initGL() {
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.addDrawer(drawer2);
        mSimpleRender = new SimpleRender(mDrawerList);
        glSurfaceView.setRenderer(mSimpleRender);
    }

    private void initSecondRender() {
        drawer2 = new VRVideoRender(getApplicationContext());
        drawer2.setVideoSize(720,1280);
        drawer2.setAlpha(0.2f);
        drawer2.getSurfaceTexture(new SurfaceTextureHandle() {
            @Override
            public void handleSurfaceTexture(SurfaceTexture surfaceTexture) {
                Log.e("gaorui", "handleSurfaceTexture");
                mDrawerSurfaceTexture = surfaceTexture;
                initPlayer(surfaceTexture, getExternalFilesDir("Caches").getAbsolutePath() + "/carton.mp4");
            }
        });

        mDrawerList.add(drawer2);
    }

    public void initPlayer(SurfaceTexture surfaceTexture, String path){
        Log.e("gaorui", "initPlayer");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(path);
        }catch (Exception e){
            e.printStackTrace();
        }

        mediaPlayer.setLooping(true);
        mediaPlayer.setSurface(new Surface(surfaceTexture));
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.e("gaorui", "onPrepared");
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                    } else {
                        mediaPlayer.start();
                    }
                }
            }
        });

        try {
            mediaPlayer.prepare();
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.reset();
            try {
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
        } else if (mDrawerSurfaceTexture != null){
            initPlayer(mDrawerSurfaceTexture, getExternalFilesDir("Caches").getAbsolutePath() + "/carton.mp4");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}