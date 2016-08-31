package com.lanan.navigation.activities;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.lanan.zigbeetransmission.dataclass.PortClass;
import com.lanan.zigbeetransmission.dataclass.SendPackage;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class NavigationActivity extends AppCompatActivity {

    private MyDraw myDraw;
    private CoordinatorLayout container;

    private static TextView screen;
    private static TextView longitude;
    private static TextView latitude;
//  /* 海东青 ---> 往西 ---> 烈士公园西门 ---> 烈士公园南门 ---> 海东青 */
//    private final double[][] data = {{112.992146, 28.210455}, {112.992538,28.21043},
//            {112.99274,28.21384}, {112.988868,28.214198}, {112.98885,28.214421},
//            {112.992776,28.214086}, {112.993234,28.214055}, {112.997968,28.213482},
//            {112.998148,28.21458}, {112.999747,28.21458}, {112.999396,28.209455},
//            {112.993674,28.209391}, {112.992291,28.209455}, {112.992146, 28.210455}};

    /* 海东青 ---> 烈士公园 */
    private final double[][] data = {{112.992146, 28.210455}, {112.992538, 28.21043}, {112.99274, 28.21384},
            {112.997968, 28.213482}, {112.998148, 28.21458}, {112.999747, 28.21458}};

    private boolean isDrawStop;

    //    private NavigationInfo nInfo;
    private Order myOrder;
    private DataTask dataTask;
    private LocationClient mLocationClient;
    private ArrayList<LocationInfo> locationInfos = new ArrayList<>();
//    private final LinkedList<LocationInfo> showList = new LinkedList<>();

    private static final int SL = 0;
    private static final int RL = 1;
    private static final int SN = 2;
    private static final int RN = 3;
    private static final int LNG_LAT = 4;
    private static final int RECV_LOCATION = 5;
    private static final int TEXT = 6;
    private static final int ACCESS_FINE_LOCATION = 7;
    private static final int WRITE_EXTERNAL_STORAGE = 8;

    private static final String TAG = "Emilio";
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
    private static final DecimalFormat normalFormat = new DecimalFormat("#.00");
    private static final DecimalFormat lngFormat = new DecimalFormat("#.000000");
    private static PortClass.portType type = PortClass.portType.PORT;
    private static int rate = 2000;
    private static int voiceRate = 30000;
    private static int curPos = 1;
    private static int curMode = 0;
    private static int curRate = 0;
    private static int curVoiceRate = 0;
    private static int yawCount = 0;

    private static TextToSpeech speech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        container = (CoordinatorLayout) this.findViewById(R.id.snack_container);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
        }

        myDraw = (MyDraw) this.findViewById(R.id.path);
        longitude = (TextView) this.findViewById(R.id.longitude);
        latitude = (TextView) this.findViewById(R.id.latitude);
        screen = (TextView) this.findViewById(R.id.showtext);
        screen.setMovementMethod(ScrollingMovementMethod.getInstance());

        baiduLbsSet();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.port_change:
                AlertDialog.Builder builder = new AlertDialog.Builder(NavigationActivity.this);
                builder.setTitle("请选择串口通信方式");
                final String[] portList = {"设备文件方式", "CDCACM", "CP21", "FTDI", "PROLIFIC"};
                builder.setSingleChoiceItems(portList, curMode, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        curMode = which;
                        switch (which) {
                            case 0:
                                NavigationActivity.type = PortClass.portType.PORT;
                                break;
                            case 1:
                                NavigationActivity.type = PortClass.portType.CDCACM;
                                break;
                            case 2:
                                NavigationActivity.type = PortClass.portType.CP21;
                                break;
                            case 3:
                                NavigationActivity.type = PortClass.portType.FTDI;
                                break;
                            case 4:
                                NavigationActivity.type = PortClass.portType.PROLIFIC;
                                break;
                        }
                        Snackbar snackbar = Snackbar.make(container, "当前串口方式为：" + portList[which], Snackbar.LENGTH_SHORT);
                        snackbar.getView().setBackgroundColor(Color.parseColor("#A0141010"));
                        snackbar.show();
                        dialog.dismiss();
                    }
                });
                builder.show();
                break;
            case R.id.rate_change:
                AlertDialog.Builder rateBuilder = new AlertDialog.Builder(NavigationActivity.this);
                rateBuilder.setTitle("请选择串口发送速率");
                final String[] rateList = {"2000ms", "1000ms", "500ms", "100ms"};
                rateBuilder.setSingleChoiceItems(rateList, curRate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        curRate = which;
                        switch (which) {
                            case 0:
                                NavigationActivity.rate = 2000;
                                break;
                            case 1:
                                NavigationActivity.rate = 1000;
                                break;
                            case 2:
                                NavigationActivity.rate = 500;
                                break;
                            case 3:
                                NavigationActivity.rate = 100;
                                break;
                        }
                        Snackbar snackbar = Snackbar.make(container, "当前串口发送速率为：" + rateList[which], Snackbar.LENGTH_SHORT);
                        snackbar.getView().setBackgroundColor(Color.parseColor("#A0141010"));
                        snackbar.show();
                        dialog.dismiss();
                    }
                });
                rateBuilder.show();
                break;
            case R.id.voice_rate:
                AlertDialog.Builder voiceBuilder = new AlertDialog.Builder(NavigationActivity.this);
                voiceBuilder.setTitle("请选择语音导航播报速率");
                final String[] voiceRateList = {"30s", "60s", "90s", "15s"};
                voiceBuilder.setSingleChoiceItems(voiceRateList, curVoiceRate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        curVoiceRate = which;
                        switch (which) {
                            case 0:
                                NavigationActivity.voiceRate = 30000;
                                break;
                            case 1:
                                NavigationActivity.voiceRate = 60000;
                                break;
                            case 2:
                                NavigationActivity.voiceRate = 90000;
                                break;
                            case 3:
                                NavigationActivity.voiceRate = 15000;
                                break;
                        }
                        Snackbar snackbar = Snackbar.make(container, "当前语音播报速率为：" + voiceRateList[which], Snackbar.LENGTH_SHORT);
                        snackbar.getView().setBackgroundColor(Color.parseColor("#A0141010"));
                        snackbar.show();
                        dialog.dismiss();
                    }
                });
                voiceBuilder.show();
                break;
            case R.id.send:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (myOrder != null) {
                            myOrder.stop();
                            myOrder = null;
                        }
                        PortClass port;
                        switch (type) {
                            case PORT:
                                File file = getComFile();
                                if (file != null) {
                                    port = new PortClass(file, 115200, 0);
                                    port.open(NavigationActivity.this);
                                    myOrder = new Order(data, port);
                                    myOrder.setSendRate(rate);
                                    myOrder.write();
                                    while (!myOrder.getStatus()) {
                                        try {
                                            Thread.sleep(500);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    myOrder.stop();
                                    myOrder = null;
                                } else {
                                    Snackbar snackbar = Snackbar.make(container, "设备文件不存在", Snackbar.LENGTH_SHORT);
                                    snackbar.getView().setBackgroundColor(Color.parseColor("#A0141010"));
                                    snackbar.show();
                                }
                                break;
                            default:
                                port = new PortClass(type);
                                port.open(NavigationActivity.this);
                                myOrder = new Order(data, port);
                                myOrder.write();
                                while (!myOrder.getStatus()) {
                                    try {
                                        Thread.sleep(500);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                myOrder.stop();
                                myOrder = null;
                                break;
                        }
                    }
                }).start();
                break;
            case R.id.recv:
                refreshScreen("start to recv...\n");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (myOrder != null) {
                            myOrder.stop();
                            myOrder = null;
                        }

                        PortClass port;
                        switch (type) {
                            case PORT:
                                File file = getComFile();
                                if (file == null){
                                    refreshScreen("未找到传输设备文件,传输中止！\n");
                                    return;
                                }

                                port = new PortClass(file, 115200, 0);
                                break;
                            default:
                                port = new PortClass(type);
                        }
                        port.open(NavigationActivity.this);
                        ArrayList<SendPackage> list = new ArrayList<>();
                        myOrder = new Order(list, port, Order.Type.UNPACK_ORIGIN);
                        myOrder.read();
                        while (myOrder != null && !myOrder.getStatus()) {
                            try {
                                Thread.sleep(500);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (myOrder == null) {
                            return;
                        }
                        myOrder.getPackage();
                        locationInfos = myOrder.getLocationDataList();
                        myOrder.stop();
                        myOrder = null;
                        Message message = new Message();
                        message.what = RECV_LOCATION;
                        showHandler.sendMessage(message);
                    }
                }).start();
                break;
            case R.id.start_navigation:
                if (myOrder != null) {
                    myOrder.stop();
                    myOrder = null;
                }
                myOrder = new Order(data);
                myOrder.setPackage();
                locationInfos = myOrder.getLocationDataList();
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
                Message message = new Message();
                message.what = TEXT;
                Bundle bundle = new Bundle();
                bundle.putString("text", "导航开始");
                showHandler.sendMessage(message);

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
                        Message message = new Message();
                        message.what = TEXT;
                        Bundle bundle = new Bundle();
                        bundle.putString("text", "导航结束");
                        message.setData(bundle);
                        showHandler.sendMessage(message);
                    }
                }).start();
                break;
            case R.id.close:
                if (myOrder != null) {
                    myOrder.stop();
                    myOrder = null;
                }
                isDrawStop = true;

                if (dataTask != null && dataTask.isAlive()) {
                    dataTask.setInterrupt(true);
                    dataTask = null;
                }

                Message clMessage = new Message();
                clMessage.what = TEXT;
                Bundle bundle1 = new Bundle();
                bundle1.putString("text", "停止！");
                clMessage.setData(bundle1);
                showHandler.sendMessage(clMessage);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("unused")
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
                    break;
                case SN:
                    break;
                case RN:
                    double dis = bundle.getDouble("distance");
                    double angle = bundle.getDouble("angle");
                    int pos = bundle.getInt("pos");
                    boolean yaw = bundle.getBoolean("yaw", false);
                    refreshScreen("Time: " + time + " Target: " + pos +
                            "\nDistance: " + normalFormat.format(dis) +
                            "m Angle: " + normalFormat.format(angle) + "°\n");
                    if (pos > curPos) {
                        int a = Double.valueOf(dis).intValue();
                        int b = Double.valueOf(angle).intValue();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            speech.speak("已到达第" + (pos - 1) + "路径点", TextToSpeech.QUEUE_FLUSH, null, null);
                            speech.speak("距离第" + pos + "路径点" + a + "米", TextToSpeech.QUEUE_ADD, null, null);
                            speech.speak("方位角为" + b + "度", TextToSpeech.QUEUE_ADD, null, null);
                        } else {
                            speech.speak("已到达第" + (pos - 1) + "路径点", TextToSpeech.QUEUE_FLUSH, null);
                            speech.speak("距离第" + pos + "路径点" + a + "米", TextToSpeech.QUEUE_ADD, null);
                            speech.speak("方位角为" + b + "度", TextToSpeech.QUEUE_ADD, null);
                        }
                        curPos = pos;
                    }
                    if (yaw) {
                        yawCount++;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("第");
                        stringBuilder.append(yawCount);
                        stringBuilder.append("次偏航!");
                        refreshScreen(stringBuilder + "\n偏航！偏航！");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            speech.speak("偏航", TextToSpeech.QUEUE_FLUSH, null, null);
                        } else {
                            speech.speak("偏航", TextToSpeech.QUEUE_FLUSH, null);
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

    @SuppressWarnings("unused")
    private static class ShowHandler extends Handler {
        private final WeakReference<Activity> mActivity;

        public ShowHandler(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECV_LOCATION:
                    refreshScreen("已接受到路径点信息\n");
                    break;
                case TEXT:
                    Bundle bundle = msg.getData();
                    if (bundle != null && bundle.containsKey("text")) {
                        String text = bundle.getString("text");
                        refreshScreen(text + "\n");
                    }
                    break;
            }
        }
    }

    @SuppressWarnings("unused")
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
            } else {
                speech.speak("距离第" + pos + "路径点" + a + "米", TextToSpeech.QUEUE_ADD, null);
                speech.speak("方位角为" + b + "度", TextToSpeech.QUEUE_ADD, null);
            }
        }
    }

    private final Handler mHandler = new MyHandler(this);
    private final Handler voiceHandler = new VoiceHandler(this);
    private final Handler showHandler = new ShowHandler(this);

    private class VoiceThread extends Thread {
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
                        Thread.sleep(voiceRate);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Nullable
    private File getComFile() {
        File devDir = new File("/dev");
        if (devDir.isDirectory() && devDir.canRead()) {
            File[] files = devDir.listFiles();
            for (File file : files) {
                if (file.getName().startsWith("ttyUSB")) {
                    return file;
                }
            }
        }
        return null;
    }

    private void baiduLbsSet() {
        mLocationClient = new LocationClient(this);
        LocationClientOption mOption = new LocationClientOption();
        mOption.setOpenGps(true);
        mOption.setCoorType("bd09ll");
        mOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
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

//                Log.d(TAG, "" + System.currentTimeMillis());

                LocationInfo info = new LocationInfo(bdLocation.getLongitude(), bdLocation.getLatitude());
//                if (!showList.isEmpty()) {
//                    if (showList.size() > 13) {
//                        showList.pollFirst();
//                    }
//                    double lngCount = info.getLng();
//                    double latCount = info.getLat();
//                    for (LocationInfo s : showList) {
//                        lngCount += s.getLng();
//                        latCount += s.getLat();
//                    }
//                    double calLng = lngCount / (showList.size() + 1);
//                    double calLat = latCount / (showList.size() + 1);
//                    LocationInfo s = new LocationInfo(calLng, calLat);
//                    showList.addLast(s);
//                } else {
//                    showList.addLast(info);
//                }

                Message message = new Message();
                message.what = LNG_LAT;
                Bundle bundle = new Bundle();
                bundle.putDouble("longitude", info.getLng());
                bundle.putDouble("latitude", info.getLat());
//                bundle.putDouble("longitude", showList.getLast().getLng());
//                bundle.putDouble("latitude", showList.getLast().getLat());
                message.setData(bundle);
                mHandler.sendMessage(message);
                dataTask.setmLocation(info);
            }
        });
        mLocationClient.start();
        mLocationClient.requestLocation();
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
        }
    }

    @Override
    protected void onDestroy() throws SecurityException {
        super.onDestroy();
        isDrawStop = true;
        if (myOrder != null) {
            myOrder.stop();
            myOrder = null;
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
    }
}