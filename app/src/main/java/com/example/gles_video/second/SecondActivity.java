package com.example.gles_video.second;


import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaPlayer;
import android.opengl.EGLSurface;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.example.gles_video.EGL.CustomGLRender;
import com.example.gles_video.EGL.EGLCore;
import com.example.gles_video.IDrawer;
import com.example.gles_video.R;
import com.example.gles_video.SoulVideoDrawer;
import com.example.gles_video.SurfaceTextureHandle;
import com.example.gles_video.second.video.TestRender;
import com.example.gles_video.second.video.Utils.PermisstionUtil;
import com.example.gles_video.second.video.encode.VideoEncoder;

import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView = null;
    private CustomGLRender mCustomGLRender = null;
    private IDrawer drawer2 = null;
    private MediaPlayer mediaPlayer = null, mediaPlayer2 = null;
    private TextView mText = null;
    private HandlerThread recordThread = null;
    private Handler recordHandler = null;
    private volatile boolean isStart = false;
    private Surface inputSurface = null;
    private static final int FRAME_RATE = 30;
    private static final int IFRAME_INTERVAL = 10;
    private MediaCodec.BufferInfo bufferInfo = null;
    private VideoEncoder mVideoEncoder = null;
    private EGLCore mEGLCore = null;
    private EGLSurface mEGLSurface = null;
    private GLSurfaceView mGLSurfcaeView = null;
    private TestRender testRender = null;

    private final static int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        mSurfaceView = findViewById(R.id.surface_view);
        mGLSurfcaeView = findViewById(R.id.gl_sec_surface_view);

        mText = findViewById(R.id.sv_text);
        mText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (PermisstionUtil.checkPermissionsAndRequest(SecondActivity.this, PermisstionUtil.STORAGE, REQUEST_CODE, "请求访问SD卡权限被拒绝")) {

                    if (!isStart) {
                        isStart = true;
                        testRender.startRecord();
                    } else {
                        isStart = false;
                        testRender.stopRecord();
                    }
                }

            }
        });

//        initSecondRender();

//        mCustomGLRender = new CustomGLRender();
//        mCustomGLRender.addDrawer(drawer2);
//        mCustomGLRender.setSurface(mSurfaceView, videoEncoderWrapper);


        testRender = new TestRender(mGLSurfcaeView,this, new SurfaceTextureHandle() {
            @Override
            public void handleSurfaceTexture(SurfaceTexture surfaceTexture) {
                initPlayer(surfaceTexture, getExternalFilesDir("Caches").getAbsolutePath() + "/carton.mp4", false);
            }
        });
        mGLSurfcaeView.setEGLContextClientVersion(2);
        mGLSurfcaeView.setRenderer(testRender);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE ) {
            Log.e("gaorui", "onRequestPermissionsResult - REQUEST_CODE ");
        }
    }

    private void initSecondRender() {
        drawer2 = new SoulVideoDrawer(getApplicationContext());
        drawer2.setVideoSize(720,1280);
        drawer2.setAlpha(0.3f);
        drawer2.getSurfaceTexture(new SurfaceTextureHandle() {
            @Override
            public void handleSurfaceTexture(SurfaceTexture surfaceTexture) {
                Log.e("gaorui", "handleSurfaceTexture");
                initPlayer(surfaceTexture, getExternalFilesDir("Caches").getAbsolutePath() + "/carton.mp4", true);
            }
        });
    }

    public void initPlayer(SurfaceTexture surfaceTexture, String path, boolean isFirst){
        Log.e("gaorui", "initPlayer");

        if (isFirst) {
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
        } else {
            mediaPlayer2 = new MediaPlayer();
            mediaPlayer2.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer2.setDataSource(path);
            }catch (Exception e){
                e.printStackTrace();
            }

            mediaPlayer2.setLooping(true);
            mediaPlayer2.setSurface(new Surface(surfaceTexture));
            mediaPlayer2.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.e("gaorui", "onPrepared");
                    if (mediaPlayer2 != null) {
                        if (mediaPlayer2.isPlaying()) {
                            mediaPlayer2.pause();
                        } else {
                            mediaPlayer2.start();
                        }
                    }
                }
            });

            try {
                mediaPlayer2.prepare();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        if (mediaPlayer2 != null) {
            if (mediaPlayer2.isPlaying()) {
                mediaPlayer2.pause();
            }
            mediaPlayer2.stop();
            mediaPlayer2.release();
        }
    }
}