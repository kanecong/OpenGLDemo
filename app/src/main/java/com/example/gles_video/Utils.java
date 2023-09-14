package com.example.gles_video;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utils {
    public static String loadFileFromResource(Context context, int resourceId) {
        StringBuilder sb = new StringBuilder();
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;

        try {
            inputStream = context.getResources().openRawResource(resourceId);
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (inputStreamReader != null) {
                inputStreamReader.close();
            }

            if (inputStream != null) {
                inputStream.close();
            }
        }catch (Exception g) {
            g.printStackTrace();
        }

        return sb.toString();
    }
}
