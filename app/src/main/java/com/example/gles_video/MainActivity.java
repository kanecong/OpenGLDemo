package com.example.gles_video;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import com.example.gles_video.second.SecondActivity;
import com.example.gles_video.view.MyGLSurfaceView;
import com.example.gles_video.vr.VRActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CaptureData{

    private VideoDrawer drawer;
    private SoulVideoDrawer drawer2;
    private ArrayList<IDrawer> mDrawerList = new ArrayList<>();
    private MyGLSurfaceView mGlSurfaceView = null;
    private MediaPlayer mediaPlayer = null, mediaPlayer2 = null;
    private TextView mText = null;
    private SimpleRender simpleRender = null;
    private SurfaceTexture mDrawerSurfaceTexture = null;
    private SurfaceTexture mDrawer2SurfaceTexture = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGlSurfaceView = findViewById(R.id.gl_surface);

        mText = findViewById(R.id.gl_text);
        mText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("gaorui", "onClick");
//                if (mediaPlayer != null) {
//                    if (mediaPlayer.isPlaying()) {
//                        mediaPlayer.pause();
//                    } else {
//                        mediaPlayer.start();
//                    }
//                }
//
//                drawer2.setCaptureDataListener(MainActivity.this);

                startActivity(new Intent(getApplicationContext(), SecondActivity.class));

            }
        });

        initRender();
        initSecondRender();
        initGL();
    }

    public void initGL() {
        mGlSurfaceView.setEGLContextClientVersion(2);
        mGlSurfaceView.addDrawer(drawer2);
        simpleRender = new SimpleRender(mDrawerList);
        mGlSurfaceView.setRenderer(simpleRender);
    }

    private void initRender() {
        drawer = new VideoDrawer(getApplicationContext());
        drawer.setVideoSize(1280,720);
        drawer.getSurfaceTexture(new SurfaceTextureHandle() {
            @Override
            public void handleSurfaceTexture(SurfaceTexture surfaceTexture) {
                Log.e("gaorui", "handleSurfaceTexture");
                mDrawerSurfaceTexture = surfaceTexture;
                initPlayer(surfaceTexture, getExternalFilesDir("Caches").getAbsolutePath() + "/video.mp4", true);
            }
        });

        mDrawerList.add(drawer);
    }

    private void initSecondRender() {
        drawer2 = new SoulVideoDrawer(getApplicationContext());
        drawer2.setVideoSize(720,1280);
        drawer2.setAlpha(0.5f);
        drawer2.getSurfaceTexture(new SurfaceTextureHandle() {
            @Override
            public void handleSurfaceTexture(SurfaceTexture surfaceTexture) {
                Log.e("gaorui", "handleSurfaceTexture");
                mDrawer2SurfaceTexture = surfaceTexture;
                initPlayer(surfaceTexture, getExternalFilesDir("Caches").getAbsolutePath() + "/carton.mp4", false);
            }
        });

        mDrawerList.add(drawer2);
    }

    public void initPlayer(SurfaceTexture surfaceTexture, String path, boolean isFirst){
        if (isFirst) {
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
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mp.reset();
                    mp.release();
                    Log.e("gaorui", "出错了！！！ what = " + what + ", extra = " + extra);
                    return false;
                }
            });

            try {
                mediaPlayer.prepare();
            }catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e("gaorui", "initPlayer2");
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
            initPlayer(mDrawerSurfaceTexture, getExternalFilesDir("Caches").getAbsolutePath() + "/video.mp4", true);
        }

        if (mediaPlayer2 != null && !mediaPlayer2.isPlaying()) {
            mediaPlayer2.reset();
            try {
                mediaPlayer2.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer2.start();
        } else if (mDrawer2SurfaceTexture != null){
            initPlayer(mDrawer2SurfaceTexture, getExternalFilesDir("Caches").getAbsolutePath() + "/carton.mp4", false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }

        if (mediaPlayer2 != null) {
            mediaPlayer2.pause();
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

        if (mediaPlayer2 != null) {
            mediaPlayer2.stop();
            mediaPlayer2.release();
            mediaPlayer2 = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (simpleRender != null) {
            simpleRender.release();
        }
    }

    @Override
    public void capture(Bitmap bmp, int width, int height) {
        saveBitmap(bmp);
    }

    public void saveBitmap(Bitmap bmp){
        try {
            String filePath = Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera/" ;
            File path = new File(filePath);

            if (!path.exists()){
                path.mkdirs();
                Log.e("MainActivity", "onImageAvailable: 路径正在创建中，，，，");
            }

            File file = new File(path, "demo_" + System.currentTimeMillis() + ".jpg");

            FileOutputStream fileOutputStream = new FileOutputStream(file);

            //bitmap转byte数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);

            fileOutputStream.write(baos.toByteArray());

            fileOutputStream.flush();
            fileOutputStream.close();

        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}