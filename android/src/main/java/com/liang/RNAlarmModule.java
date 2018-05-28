
package com.liang;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TimeUtils;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.uimanager.IllegalViewOperationException;

import java.io.Console;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.R.attr.track;
import static android.R.attr.type;

import java.io.File;
import android.os.Environment;
import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.net.Uri;

public class RNAlarmModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private SharedPreferences sharedPreferences;

    public RNAlarmModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.sharedPreferences = reactContext.getSharedPreferences(getName(),Context.MODE_PRIVATE);
    }

    @Override
    public String getName() {
        return "RNAlarm";
    }

    public Boolean getAlarmStatus(String triggerTime){
        String value = sharedPreferences.getString(triggerTime, null);
        if (value == null)
            return null;
        else {
            return value == "error" ? false : true;
        }
    }

    public void setAlarm1(String triggerTime, String value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(triggerTime,value);
        editor.commit();
    }

    @ReactMethod
    public void playTipSound(String fileName){
        MediaPlayer mp = new MediaPlayer();
        try {
            if (mp.isPlaying()) {
                mp.stop();
            }
            if(mp!=null){
                mp.release();
                mp = new MediaPlayer();
            }
            AssetFileDescriptor descriptor = reactContext.getResources().getAssets().openFd(fileName + ".mp3");
            mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(),
                    descriptor.getLength());
            mp.prepare();
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(false);
                    mp.start();
                }
            });
        }catch(Exception ex){
            ex.printStackTrace();
        }

    }

    @ReactMethod
    public  void initAlarm(@Nullable Callback successCallback){
        if(successCallback != null) {
            successCallback.invoke(true);
        }
    }

    @ReactMethod
    public void clearAlarm(){
        SharedPreferences sharedPreferences = reactContext.getSharedPreferences(getName(),Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
    }

    @ReactMethod
    public void setAlarm(String triggerTime, String title, @Nullable String isRetry, @Nullable String musicUri, @Nullable Callback successCallback, @Nullable Callback errorCallback) {
        try {
            Boolean alarmStatus = getAlarmStatus(triggerTime);
            if(isRetry != null && !isRetry.isEmpty())
                setAlarm1(triggerTime,null);

            if (alarmStatus != null)
            {
                if (alarmStatus) {
                    successCallback.invoke();
                    return;
                }else {
                    errorCallback.invoke();
                    return;
                }
            }

            if(musicUri!=null){
              if(musicUri.length()>0){
                File file = new File(Environment.getExternalStorageDirectory(),"/myRingtonFolder/Audio/");
                if (!file.exists()) {
                    file.mkdirs();
                }

                String path = Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/myRingtonFolder/Audio/";

                File f = new File(path + "/", musicUri + ".mp3");
                if(!f.exists()){
                  // Uri mUri = Uri.parse("android.resource://"+ getReactApplicationContext().getPackageName() + "/raw/sounds/" + musicUri);
                  Uri mUri = Uri.parse("android.resource://"+ getReactApplicationContext().getPackageName() + "/" + getReactApplicationContext().getResources().getIdentifier(musicUri, "raw", getReactApplicationContext().getPackageName()));
                  if(mUri!=null){
                    Log.d("gugum","M URI IS NOT NULL");
                  }else{
                    Log.d("gugum","M URI IS NULL");
                  }
                  ContentResolver mCr = getReactApplicationContext().getContentResolver();
                  if(mCr!=null){
                    Log.d("gugum","CONTENT RESOLVEEER IS NOT NULL");
                  }else{
                    Log.d("gugum","CONTENT RESOLVEEER IS NULL");
                  }
                  AssetFileDescriptor soundFile;
                  try {
                      soundFile = mCr.openAssetFileDescriptor(mUri, "r");
                  } catch (FileNotFoundException e) {
                      soundFile = null;
                  }
                  try {
                      byte[] readData = new byte[1024];
                      FileInputStream fis = soundFile.createInputStream();
                      FileOutputStream fos = new FileOutputStream(f);
                      int i = fis.read(readData);

                      while (i != -1) {
                          fos.write(readData, 0, i);
                          i = fis.read(readData);
                      }

                      fos.close();
                  } catch (IOException io) {
                  }
                  ContentValues values = new ContentValues();
                  values.put(MediaStore.MediaColumns.DATA, f.getAbsolutePath());
                  values.put(MediaStore.MediaColumns.TITLE, musicUri);
                  values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
                  values.put(MediaStore.MediaColumns.SIZE, f.length());
                  values.put(MediaStore.Audio.Media.ARTIST, musicUri);
                  values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                  values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
                  values.put(MediaStore.Audio.Media.IS_ALARM, true);
                  values.put(MediaStore.Audio.Media.IS_MUSIC, true);
                  Uri uri = MediaStore.Audio.Media.getContentUriForPath(f.getAbsolutePath());
                  Uri newUri = mCr.insert(uri, values);
                }
                musicUri = f.getAbsolutePath();
                Log.d("ReactNativeJS(11008)","MUSIC URI : "+musicUri);
              }
            }

            AlarmManager alarmManager = (AlarmManager) reactContext.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(RNAlarmConstants.REACT_NATIVE_ALARM);
            intent.putExtra(RNAlarmConstants.REACT_NATIVE_ALARM_TITLE,title);
            intent.putExtra(RNAlarmConstants.REACT_NATIVE_ALARM_MUSIC_URI, musicUri);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(reactContext, type, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

            long triggerTimeMillis = Long.parseLong(triggerTime);
            Calendar originalTimeCal = Calendar.getInstance();
            originalTimeCal.setTimeInMillis(triggerTimeMillis);
            Log.w("originalTimeCal",formatter.format(originalTimeCal.getTime()));

            Calendar currentTimeCal = Calendar.getInstance();
            currentTimeCal.setTime(new Date());
            Log.w("currentTimeCal",formatter.format(currentTimeCal.getTime()));

            //compare alarm and currentTime
            if (triggerTimeMillis - currentTimeCal.getTimeInMillis() > 0)
            {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(triggerTimeMillis);
                formatter.format(calendar.getTime());


                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
                setAlarm1(triggerTime,triggerTime);

                successCallback.invoke();
                return;

            }else {
                setAlarm1(triggerTime, "error");
                if (errorCallback != null) {
                    // -1 闹钟时间设置不能在当前时间之前
                    errorCallback.invoke("-1");
                    return;
                }
            }

        } catch (IllegalViewOperationException e) {
            if(errorCallback == null ){
                System.out.print(e.toString());
            }else{
                setAlarm1(triggerTime,"error");
                errorCallback.invoke(e.getMessage());
            }
        } catch (NumberFormatException e) {
            if(errorCallback == null ){
                System.out.print(e.toString());
            }else{
                setAlarm1(triggerTime,"error");
                errorCallback.invoke(e.getMessage());
            }
        }
    }

//    @ReactMethod
//    public void setAlarm(String triggerTime, String title, @Nullable String musicUri, Promise promise) {
//        try {
//            AlarmManager alarmManager = (AlarmManager) reactContext.getSystemService(Context.ALARM_SERVICE);
//            Intent intent = new Intent(RNAlarmConstants.REACT_NATIVE_ALARM);
//            intent.putExtra(RNAlarmConstants.REACT_NATIVE_ALARM_TITLE,title);
//            intent.putExtra(RNAlarmConstants.REACT_NATIVE_ALARM_MUSIC_URI, musicUri);
//            PendingIntent pendingIntent = PendingIntent.getBroadcast(reactContext, type, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//            long startTime = Long.parseLong(triggerTime);
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, startTime, pendingIntent);
//            promise.resolve(0);
//        } catch (IllegalViewOperationException e) {
//            promise.reject(e);
//        } catch (NumberFormatException e) {
//            promise.reject(e);
//        }
//    }
}
