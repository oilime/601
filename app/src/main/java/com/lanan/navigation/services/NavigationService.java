package com.lanan.navigation.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.lanan.navigation.control.DataTask;
import com.lanan.zigbeetransmission.dataclass.LocationInfo;
import com.lanan.zigbeetransmission.dataclass.NavigationInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NavigationService extends Service implements ServiceInterface {

    private DataTask dataTask;
    private ArrayList<LocationInfo> locationInfos = new ArrayList<>();
    private NavigationInfo info;
    private LocationInfo location;

    private int voiceRate = 30;
    private boolean isNavStop = false;
    private boolean navFlag = true;
    private ExecutorService pool = Executors.newCachedThreadPool();
    private VoiceThread voiceThread;
    private NavigationThread navThread;
    private LocationThread locationThread;

    private final String TAG = "Emilio";

    private static TextToSpeech speech;

    public NavigationService() {
        super();
    }

    public class ServiceBinder extends Binder {
        public NavigationService getService() {
            return NavigationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        speech = new TextToSpeech(NavigationService.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });
        Log.d(TAG, "语音初始化完成");

        locationThread = new LocationThread();
        pool.execute(locationThread);
        Log.d(TAG, "gps获取线程开启成功");
        return new ServiceBinder();
    }

    private class LocationThread extends Thread {

        private boolean gpsStop;

        public void setGpsStop(boolean flag) {
            this.gpsStop = flag;
        }

        @Override
        public void run() {
            setGpsStop(false);
            while (!gpsStop) {
                if (dataTask != null && navFlag) {
                    dataTask.setmLocation(getLocInfo());
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private class VoiceThread extends Thread {

        private int voiceRate = 30;
        private boolean voiceStop;

        public void setVoiceRate(int rate) {
            this.voiceRate = rate;
        }

        public void setVoiceStop(boolean flag) {
            this.voiceStop = flag;
        }

        @Override
        public void run() {
            setVoiceStop(false);
            while (!voiceStop) {
                if (dataTask != null) {
                    info = dataTask.getInfo();
                    if (info != null) {
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putDouble("distance", info.getDistance());
                        bundle.putDouble("angle", info.getAngle());
                        bundle.putInt("pos", info.getPos());
                        message.setData(bundle);
                        voiceHandler.sendMessage(message);
                        try {
                            Thread.sleep(voiceRate * 1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private class NavigationThread extends Thread {

        private int navRate = 2000;
        private boolean navStop;

        public void setNavRate(int rate) {
            this.navRate = rate;
        }

        public void setNavStop(boolean flag) {
            this.navStop = flag;
        }

        @Override
        public void run() {
            setNavStop(false);
            while (!navStop) {
                if (dataTask != null) {
                    info = dataTask.getInfo();
                    if (info != null) {
                        if (info.isArrived()) {
                            dataTask.setInterrupt(true);
                            setNavStop(true);
                            break;
                        }
                        try {
                            Thread.sleep(navRate);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 语音提示Handler
     */
    private static class VoiceHandler extends Handler {
        private final WeakReference<Service> mService;

        public VoiceHandler(Service service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            double dis = bundle.getDouble("distance");
            int a = Double.valueOf(dis).intValue();
            double angle = bundle.getDouble("angle");
            int b = Double.valueOf(angle).intValue();
            int pos = bundle.getInt("pos");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                speech.speak("距离第" + pos + "路径点" + a + "米", TextToSpeech.QUEUE_ADD, null, null);
                speech.speak("方位角为" + b + "度", TextToSpeech.QUEUE_ADD, null, null);
            } else {
                speech.speak("距离第" + pos + "路径点" + a + "米", TextToSpeech.QUEUE_ADD, null);
                speech.speak("方位角为" + b + "度", TextToSpeech.QUEUE_ADD, null);
            }
        }
    }

    public final Handler voiceHandler = new VoiceHandler(this);

    @Override
    public void startNav(ArrayList<LocationInfo> infos) {
        this.locationInfos.clear();
        this.locationInfos = infos;

        stopNav();

        dataTask = new DataTask();
        dataTask.setDestination(locationInfos);
        dataTask.start();
        Log.d(TAG, "dataTask开始");

        isNavStop = false;
        voiceThread = new VoiceThread();
        voiceThread.setVoiceRate(voiceRate);
        pool.execute(voiceThread);
        Log.d(TAG, "voiceThread开始");

        navThread = new NavigationThread();
        pool.execute(navThread);
        Log.d(TAG, "navThread开始");
    }

    @Override
    public void stopNav() {
        isNavStop = true;

        if (voiceThread != null) {
            voiceThread.setVoiceStop(true);
            voiceThread = null;
            Log.d(TAG, "voiceThread退出");
        }

        if (navThread != null) {
            navThread.setNavStop(true);
            navThread = null;
            Log.d(TAG, "navThread退出");
        }

        if (dataTask != null) {
            dataTask.setInterrupt(true);
            dataTask = null;
            Log.d(TAG, "dataTask退出");
        }
    }

    @Override
    public void setVoiceRate(int rate) {
        this.voiceRate = rate;
    }

    @Override
    public NavigationInfo getNavInfo() {
        if (dataTask != null) {
            return dataTask.getInfo();
        } else {
            return null;
        }
    }

    @Override
    public synchronized void setLocInfo(LocationInfo loc) {
        if (location != null && location.equals(loc)) {
            navFlag = false;
        } else {
            navFlag = true;
            this.location = loc;
        }
    }

    @Override
    public boolean isNavStop() {
        return isNavStop;
    }

    @Override
    public void mSpeak(String text, int mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            speech.speak(text, mode, null, null);
        } else {
            speech.speak(text, mode, null);
        }
    }

    @Override
    public void close() {
        stopNav();
        if (speech != null) {
            speech.stop();
            speech.shutdown();
            speech = null;
            Log.d(TAG, "speech关闭");
        }

        if (locationThread != null) {
            locationThread.setGpsStop(true);
            locationThread = null;
            Log.d(TAG, "gps关闭");
        }
    }

    @Override
    public void onDestroy() {
        close();
    }

    private synchronized LocationInfo getLocInfo() {
        if (location != null) {
            return location;
        } else {
            return null;
        }
    }
}
