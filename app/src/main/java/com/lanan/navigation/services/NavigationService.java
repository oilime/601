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

    private int voiceRate = 30;
    private int navRate = 2000;
    private ExecutorService pool = Executors.newFixedThreadPool(5);
    private VoiceThread voiceThread;
    private NavigationThread navThread;

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
        return new ServiceBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        speech = new TextToSpeech(NavigationService.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }

    private static class broadcastHandler extends Handler {
        private final WeakReference<Service> mService;

        public broadcastHandler(Service service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                default:
                    break;
            }
        }
    }

    private class VoiceThread extends Thread {

        private int voiceRate;
        private boolean isVoiceStop;

        public void setVoiceRate(int rate) {
            this.voiceRate = rate;
        }

        public void setVoiceStop(boolean flag) {
            this.isVoiceStop = flag;
        }

        @Override
        public void run() {
            setVoiceStop(false);
            while (!isVoiceStop) {
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

    private class NavigationThread extends Thread {

        private int navRate;
        private boolean isNavStop;

        public void setNavRate(int rate) {
            this.navRate = rate;
        }

        public void setNavStop(boolean flag) {
            this.isNavStop = flag;
        }

        @Override
        public void run() {
            setNavStop(false);
            while (!isNavStop) {
                info = dataTask.getInfo();
                if (info != null) {
                    if (info.isArrived()) {
                        dataTask.setInterrupt(true);
                        isNavStop = true;
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

        dataTask = new DataTask();
        dataTask.setDestination(locationInfos);
        dataTask.start();

        if (voiceThread != null) {

        }

        voiceThread = new VoiceThread();
        voiceThread.setVoiceRate(voiceRate);
        pool.execute(voiceThread);

        navThread = new NavigationThread();
        navThread.setNavRate(navRate);
        pool.execute(navThread);
    }

    @Override
    public void stopNav() {
        if (voiceThread != null && voiceThread.isAlive()) {
            voiceThread.setVoiceStop(true);
            voiceThread = null;
        }

        if (navThread != null && navThread.isAlive()) {
            navThread.setNavStop(true);
            navThread = null;
        }
    }

    @Override
    public void setVoiceRate(int rate) {
        this.voiceRate = rate;
    }

    @Override
    public void setNavRate(int rate) {
        this.navRate = rate;
    }

    @Override
    public NavigationInfo getNavInfo() {
        if (dataTask != null && !dataTask.isInterrupt()) {
            return dataTask.getInfo();
        } else {
            return null;
        }
    }

    private broadcastHandler mHandler = new broadcastHandler(this);
}
