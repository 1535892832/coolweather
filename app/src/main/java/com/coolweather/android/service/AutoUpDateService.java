package com.coolweather.android.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpDateService extends Service {

    public AutoUpDateService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AlarmManager manager =(AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8*60*60*1000; //八小时毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime()+anHour;
        Intent intent1 = new Intent(this,AutoUpDateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,intent,0);
        manager.cancel(pi); //取消
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 更新天气信息
     */
    public void updateWeather(){
        final SharedPreferences prfs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherContent = prfs.getString("weather",null);
        if(weatherContent!=null){
            Weather weather = Utility.handleWeatherResponse(weatherContent);
            String weatherId = weather.basic.weatherId;
            String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId;
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                     String weatherContent = response.body().string();
                     Weather updateWeather = Utility.handleWeatherResponse(weatherContent);
                     if(updateWeather!=null&&"ok".equals(updateWeather.basic.weatherId)){
                        SharedPreferences.Editor editor =  prfs.edit();
                        editor.putString("weather",weatherContent);
                        editor.apply();
                     }
                }
            });
        }
    }

    /**
     * 更新每日一图
     */
    public void updateBingPic(){
        String address ="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String bingPic = response.body().string();
                response.body().close();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpDateService.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
       return null;
    }
}
