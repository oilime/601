package com.lanan.navigation.activities;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.lanan.navigation.R;
import com.lanan.navigation.control.DataTask;
import com.lanan.navigation.draw.MyDraw;
import com.lanan.zigbeetransmission.Order;
import com.lanan.zigbeetransmission.dataclass.LocationInfo;
import com.lanan.zigbeetransmission.dataclass.NavigationInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

public class NavigationActivity extends Activity {

    private MyDraw myDraw;

    private static TextView screen;
    private static TextView longitude;
    private static TextView latitude;

    private final double[][] data = {{112.992146, 28.210455}, {112.992511, 28.210442},
            {112.992792, 28.213875}, {112.993196, 28.213836}, {112.995725, 28.213625},
            {112.99771, 28.213358}, {112.997912, 28.213342}, {112.997822, 28.212547}};

    private boolean isDrawStop;
//    private boolean isGpsStop = false;

    //    private NavigationInfo nInfo;
    private Order myOrder;
    private DataTask dataTask;
    private final LinkedList<LocationInfo> showList = new LinkedList<>();
    private OutputStreamWriter out;
    private LocationClient mLocationClient;

    private static int curPos = 1;
    private static final int SL = 0;
    private static final int RL = 1;
    private static final int SN = 2;
    private static final int RN = 3;
    private static final int LNG_LAT = 5;
    private static final int RECV_LOCATION = 6;
    private static final int ACCESS_FINE_LOCATION = 7;
    private static final int WRITE_EXTERNAL_STORAGE = 8;
    private static final int READ_PHONE_STATE = 9;
    private static final String TAG = "Emilio";
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
    private static final DecimalFormat normalFormat = new DecimalFormat("#.00");
    private static final DecimalFormat lngFormat = new DecimalFormat("#.000000");

    private static TextToSpeech speech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.huawei);

        try {
            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/gpsdata/info.txt");
            if (!file.exists()) {
                @SuppressWarnings("UnusedAssignment") boolean recv = file.createNewFile();
            }
            out = new OutputStreamWriter(new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }

        myDraw = (MyDraw) this.findViewById(R.id.path);
        longitude = (TextView) this.findViewById(R.id.longitude);
        latitude = (TextView) this.findViewById(R.id.latitude);
        screen = (TextView) this.findViewById(R.id.showtext);
        screen.setMovementMethod(ScrollingMovementMethod.getInstance());

        Button sendLocation = (Button) this.findViewById(R.id.sendlocation);
        Button recvLocation = (Button) this.findViewById(R.id.recvlocation);
        Button drawPath = (Button) this.findViewById(R.id.drawpath);
        Button closePort = (Button) this.findViewById(R.id.closeport);

        baiduLbsSet();

        sendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (myOrder != null) {
//                            myOrder.getPort().close();
//                            myOrder.stop();
//                            myOrder = null;
//                        }
//
//                        PortClass port = new PortClass(PortClass.portType.FTDI);
//                        port.open(NavigationActivity.this);
//                        myOrder = new Order(data, port);
//                        myOrder.write();
//                        port.close();
//                    }
//                }).start();
            }
        });

        recvLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                screen.append("start to recv...\n");
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (myOrder != null) {
//                            myOrder.getPort().close();
//                            myOrder.stop();
//                            myOrder = null;
//                        }
//
//                        PortClass port = new PortClass(PortClass.portType.FTDI);
//                        port.open(NavigationActivity.this);
//                        ArrayList<SendPackage> list = new ArrayList<>();
//                        myOrder = new Order(list, port, Order.Type.UNPACK_ORIGIN);
//                        myOrder.read();
//                        while (!myOrder.getStatus()){
//                            try {
//                                Thread.sleep(500);
//                            }catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        Message message = new Message();
//                        message.what = RECV_LOCATION;
//                        showHandler.sendMessage(message);
//                    }
//                }).start();
            }
        });

        drawPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* Test code */
                if (myOrder != null) {
                    myOrder = null;
                }
//                PortClass port = new PortClass(PortClass.portType.FTDI);
//                port.open(NavigationActivity.this);
                myOrder = new Order(data);

                ArrayList<LocationInfo> locationInfos = myOrder.getLocationDataList();
                for (LocationInfo infos : locationInfos) {
                    if (infos.equals(locationInfos.get(0)))
                        myDraw.drawOrigin(infos);
                    else
                        myDraw.drawNew(infos);
                }
                dataTask = new DataTask();
                dataTask.setDestination(locationInfos);
                dataTask.start();

                VoiceThread voiceThread = new VoiceThread();
                voiceThread.start();
                isDrawStop = false;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!isDrawStop) {
                            NavigationInfo info = dataTask.getInfo();
                            if (info != null) {
                                if (info.isArrived()) {
                                    dataTask.setInterrupt(true);
                                    isDrawStop = true;
                                    break;
                                }
                                Message message = new Message();
                                message.what = RN;
                                Bundle bundle = new Bundle();
                                bundle.putDouble("distance", info.getDistance());
                                bundle.putDouble("angle", info.getAngle());
                                bundle.putInt("pos", info.getPos());
                                bundle.putBoolean("yaw", info.isYawed());
                                message.setData(bundle);
                                mHandler.sendMessage(message);
                                try {
                                    Thread.sleep(2000);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();
            }
        });

        closePort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myOrder != null) {
                    myOrder = null;
                }
//                isGpsStop = true;
                isDrawStop = true;

                if (dataTask != null && dataTask.isAlive()) {
                    dataTask.setInterrupt(true);
                    dataTask = null;
                }
            }
        });

        TextView nw = (TextView) this.findViewById(R.id.nw);
        TextView ne = (TextView) this.findViewById(R.id.ne);
        TextView sw = (TextView) this.findViewById(R.id.sw);
        TextView se = (TextView) this.findViewById(R.id.se);
        nw.append(myDraw.getParam(MyDraw.location.WEST) + ", " + myDraw.getParam(MyDraw.location.NORTH));
        ne.append(myDraw.getParam(MyDraw.location.EAST) + ", " + myDraw.getParam(MyDraw.location.NORTH));
        sw.append(myDraw.getParam(MyDraw.location.WEST) + ", " + myDraw.getParam(MyDraw.location.SOUTH));
        se.append(myDraw.getParam(MyDraw.location.EAST) + ", " + myDraw.getParam(MyDraw.location.SOUTH));

        PermissionCheck();

        speech = new TextToSpeech(NavigationActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });
    }

//    public synchronized void setnInfo(NavigationInfo info){
//        nInfo = info;
//    }
//
//    public synchronized NavigationInfo getnInfo(){
//        return nInfo;
//    }
//
//    private String parseByte2HexStr(byte[] buf) {
//        StringBuilder sb = new StringBuilder();
//        for (byte i: buf) {
//            String hex = Integer.toHexString(i & 0xFF);
//            if (hex.length() == 1) {
//                hex = '0' + hex;
//            }
//            sb.append(hex.toUpperCase());
//        }
//        return sb.toString();
//    }

    private void baiduLbsSet() {
        mLocationClient = new LocationClient(this);
        LocationClientOption mOption = new LocationClientOption();
        mOption.setOpenGps(true);
        mOption.setCoorType("bd09ll");
        mOption.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        mOption.setScanSpan(1000);

        mLocationClient.setLocOption(mOption);
        mLocationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                if (bdLocation == null) {
                    return;
                }

                if (bdLocation.getLocType() != 61 && bdLocation.getLocType() != 161) {
                    return;
                }

                LocationInfo info = new LocationInfo(bdLocation.getLongitude(), bdLocation.getLatitude());
                String curDate = timeFormat.format(System.currentTimeMillis());
                String data = "时间：" + curDate + " 经度：" + info.getLng() + " 纬度：" + info.getLat() + "\n";

                try {
                    out.write(data);
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (!showList.isEmpty()) {
                    if (showList.size() > 4) {
                        showList.pollFirst();
                    }
                    double lngCount = info.getLng();
                    double latCount = info.getLat();
                    for (LocationInfo s : showList) {
                        lngCount += s.getLng();
                        latCount += s.getLat();
                    }
                    double calLng = lngCount / (showList.size() + 1);
                    double calLat = latCount / (showList.size() + 1);
                    LocationInfo s = new LocationInfo(calLng, calLat);
                    showList.addLast(s);
                } else {
                    showList.addLast(info);
                }

                Message message = new Message();
                message.what = LNG_LAT;
                Bundle bundle = new Bundle();
                bundle.putDouble("longitude", showList.getLast().getLng());
                bundle.putDouble("latitude", showList.getLast().getLat());
                message.setData(bundle);
                mHandler.sendMessage(message);
                dataTask.setmLocation(showList.getLast());
            }
        });
        mLocationClient.start();
        mLocationClient.requestLocation();
    }

    private static class MyHandler extends Handler {
        private final WeakReference<Activity> mActivity;

        public MyHandler(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            String time = timeFormat.format(System.currentTimeMillis());
            switch (message.what) {
                case SL:
                    break;
                case RL:
                    double lng = bundle.getDouble("longitude");
                    double lat = bundle.getDouble("latitude");
                    refreshScreen("Time: " + time + " Longitude: " + lng + " Latitude: " + lat + "\n");
                    break;
                case SN:
                    break;
                case RN:
                    double dis = bundle.getDouble("distance");
                    double angle = bundle.getDouble("angle");
                    int pos = bundle.getInt("pos");
                    boolean yaw = bundle.getBoolean("yaw", false);
                    refreshScreen("Time: " + time +
                            "\nDistance: " + normalFormat.format(dis) +
                            " Angle: " + normalFormat.format(angle) +
                            "\nPos: " + pos + "\n");
                    if (pos > curPos) {
                        int a = Double.valueOf(dis).intValue();
                        int b = Double.valueOf(angle).intValue();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            speech.speak("已到达第" + (pos - 1) + "路径点", TextToSpeech.QUEUE_FLUSH, null, null);
                            speech.speak("距离第" + pos + "路径点" + a + "米", TextToSpeech.QUEUE_ADD, null, null);
                            speech.speak("方位角为" + b + "度", TextToSpeech.QUEUE_ADD, null, null);
                        }
                        curPos = pos;
                    }
                    if (yaw) {
                        refreshScreen("偏航！\n偏航！偏航！\n偏航！偏航！偏航！\n");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            speech.speak("偏航", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                    break;
                case LNG_LAT:
                    double ln = bundle.getDouble("longitude");
                    double la = bundle.getDouble("latitude");
                    longitude.setText(lngFormat.format(ln));
                    latitude.setText(lngFormat.format(la));
                    break;
            }
        }
    }

//    private static class ShowHandler extends Handler{
//        private final WeakReference<Activity> mActivity;
//        public ShowHandler(Activity activity) {
//            mActivity = new WeakReference<>(activity);
//        }
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case RECV_LOCATION:
//                    screen.append("已接受到路径点信息\n");
//                    break;
//            }
//        }
//    }

    private static class VoiceHandler extends Handler {

        private final WeakReference<Activity> mActivity;
        public VoiceHandler(Activity activity) {
            mActivity = new WeakReference<>(activity);
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
            }
        }
    }

    private final Handler mHandler = new MyHandler(this);
    private final Handler voiceHandler = new VoiceHandler(this);
    //    public Handler showHandler = new ShowHandler(this);

    private class VoiceThread extends Thread{
        @Override
        public void run() {
            while (!isDrawStop) {
                NavigationInfo info = dataTask.getInfo();
                if (info != null) {
                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putDouble("distance", info.getDistance());
                    bundle.putDouble("angle", info.getAngle());
                    bundle.putInt("pos", info.getPos());
                    message.setData(bundle);
                    voiceHandler.sendMessage(message);
                    try {
                        Thread.sleep(60000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void refreshScreen(String msg) {
        screen.append(msg);
        int offset = screen.getLineCount() * screen.getLineHeight();
        if (offset > screen.getHeight()) {
            screen.scrollTo(0, offset - screen.getHeight());
        }
    }

    private void PermissionCheck() {
        if (ContextCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "无gps权限");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION);
        } else if (ContextCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "无写文件权限");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE);
        } else if (ContextCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "无读取手机状态权限");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},
                    READ_PHONE_STATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ACCESS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "已获取gps权限");
                } else {
                    Toast.makeText(this, "未获取手机权限！", Toast.LENGTH_LONG).show();
                }
                break;
            case WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "已获取写权限");
                } else {
                    Toast.makeText(this, "未获取手机权限！", Toast.LENGTH_LONG).show();
                }
                break;
            case READ_PHONE_STATE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "已获取读取手机状态权限");
                } else {
                    Toast.makeText(this, "未获取手机权限！", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() throws SecurityException {
        super.onDestroy();
        isDrawStop = true;
        if (myOrder != null) {
//            myOrder.stop();
            myOrder = null;
        }
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.stop();
            mLocationClient = null;
        }

        if (speech != null) {
            speech.stop();
            speech.shutdown();
        }

        if (dataTask != null && dataTask.isAlive()) {
            dataTask.setInterrupt(true);
            dataTask = null;
        }

        screen = null;
        longitude = null;
        latitude = null;
    }
}