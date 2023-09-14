package com.example.gles_video;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

public class PermissionActivity extends Activity {

    private Intent mIntent;
    private boolean isReturnResult;
    private boolean criticalPermissionDenied;
    private int mNumPermissionToRequest;
    private boolean mShouldRequestPermission;
    private boolean mFlagHasCameraPermission;
    private boolean mShouldRequestMicroPhonePermission;
    private boolean mShouldRequestStoragePermission;
    private boolean mFlagHasMicroPhonePermission;
    private boolean mFlagHasStoragePermission;
    private int mIndexPermissionRequestCamera;
    private int mIndexPermissionRequestMicroPhone;
    private int mIndexPermissionRequestStorage;
    private boolean mDialogFlag;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        Log.e("MainActivity", "PermissionActivity - onCreate 启用");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("MainActivity", "PermissionActivity - onResume 启用");

        mIntent = getIntent();
        isReturnResult = false;
        if (!criticalPermissionDenied && !isReturnResult){
            mNumPermissionToRequest = 0;
            checkPermission();
        }else{
            criticalPermissionDenied = false;
        }
    }

    private void checkPermission(){
        Log.e("MainActivity", "PermissionActivity - checkPermission 启用");

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            mNumPermissionToRequest++;
            mShouldRequestStoragePermission = true;
        }else {
            mFlagHasStoragePermission = true;
        }
        if (mNumPermissionToRequest != 0){
            buildPermissionRequest();
        }else {
            handlePermissionSuccess();
        }
    }

    private void buildPermissionRequest(){
        Log.e("MainActivity", "PermissionActivity - buildPermissionRequest 启用");
        String[] permissionToRequest = new String[mNumPermissionToRequest];
        int permissionRequestIndex = 0;

        if (mShouldRequestStoragePermission){
            permissionToRequest[permissionRequestIndex] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            mIndexPermissionRequestStorage = permissionRequestIndex;
            permissionRequestIndex++;
        }
        Log.e("MainActivity", "PermissionActivity - requestPermissions 启用");
        ActivityCompat.requestPermissions(this, permissionToRequest, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        Log.e("MainActivity", "PermissionActivity - onRequestPermissionsResult 启用");

        if (mShouldRequestStoragePermission){
            if ((grantResults.length >= mIndexPermissionRequestStorage + 1) &&
                    (grantResults[mIndexPermissionRequestStorage] == PackageManager.PERMISSION_GRANTED)){
                mFlagHasStoragePermission = true;
            }else {
                criticalPermissionDenied = true;
            }
        }
        if (mFlagHasStoragePermission){
            handlePermissionSuccess();
        }else if (criticalPermissionDenied){

        }
    }

    private void handlePermissionSuccess(){
        Log.e("MainActivity", "PermissionActivity - handlePermissionSuccess 启用");
        if (mIntent != null){
            setRequstPermissionShow();
            isReturnResult = true;
            mIntent.setClass(this, MainActivity.class);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(mIntent);
            finish();
        }else {
            isReturnResult = false;
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setRequstPermissionShow(){
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isrequestShown = prefs.getBoolean("requestPermission", false);
        if (!isrequestShown){
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("requestPermission", true);
            editor.apply();
        }
    }
}
