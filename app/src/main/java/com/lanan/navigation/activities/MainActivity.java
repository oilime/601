package com.lanan.navigation.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lanan.navigation.R;
import com.lanan.navigation.control.DataTask;
import com.lanan.navigation.draw.MyDraw;
import com.lanan.navigation.model.Gps;
import com.lanan.navigation.utils.PositionUtil;
import com.lanan.zigbeetransmission.Order;
import com.lanan.zigbeetransmission.dataclass.PortClass;
import com.lanan.zigbeetransmission.dataclass.LocationInfo;
import com.lanan.zigbeetransmission.dataclass.NavigationInfo;
import com.lanan.zigbeetransmission.dataclass.SendPackage;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private MyDraw myDraw;

    private static TextView screen;
    private static TextView longitude;
    private static TextView latitude;
    private static final String TAG = "Emilio";

    private NavigationInfo nInfo;
    private LocationManager lm;
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private double[][] data = { {112.992146,28.210455}, {112.992156,28.210127}, {112.991599,28.210092},
                        {112.991491,28.209355}, {112.990148,28.209538}, {112.990193,28.209646},
                        {112.990553,28.209638}, {112.991042,28.20955},  {112.995085,28.209264},
                        {112.994559,28.213748}, {112.992834,28.213947}, {112.99252,28.210115},
                        {112.992116,28.210115}} ;

    private static final int SL = 0;
    private static final int RL = 1;
    private static final int SN = 2;
    private static final int RN = 3;
    private static final int LNG_LAT = 4;
    private static final int RECV_LOCATION = 4;
    private final int ACCESS_FINE_LOCATION = 123;

    private Order myOrder;
    private DataTask dataTask;

    private boolean isGpsStop = false;
    private boolean isRecvNav = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.huawei);

        myDraw = (MyDraw) this.findViewById(R.id.path);
        longitude = (TextView) this.findViewById(R.id.longitude);
        latitude = (TextView) this.findViewById(R.id.latitude);
        screen = (TextView) this.findViewById(R.id.showtext);
        screen.setMovementMethod(ScrollingMovementMethod.getInstance());

        Button sendLocation = (Button) this.findViewById(R.id.sendlocation);
        Button recvLocation = (Button) this.findViewById(R.id.recvlocation);
        Button drawPath = (Button) this.findViewById(R.id.drawpath);
        Button sendNavigation = (Button) this.findViewById(R.id.sendnavigation);
        Button recvNavigation = (Button) this.findViewById(R.id.recvnavigation);
        Button closePort = (Button) this.findViewById(R.id.closeport);
        Button gps = (Button) this.findViewById(R.id.gps);

        PermissionCheck();

        sendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (myOrder != null) {
                            myOrder.getPort().close();
                            myOrder.stop();
                            myOrder = null;
                        }

                        PortClass port = new PortClass(PortClass.portType.FTDI);
                        port.open(MainActivity.this);
                        myOrder = new Order(data, port);
                        myOrder.write();
                        port.close();
                    }
                }).start();
            }
        });

        recvLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                screen.append("start to recv...\n");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (myOrder != null) {
                            myOrder.getPort().close();
                            myOrder.stop();
                            myOrder = null;
                        }

                        PortClass port = new PortClass(PortClass.portType.FTDI);
                        port.open(MainActivity.this);
                        ArrayList<SendPackage> list = new ArrayList<>();
                        myOrder = new Order(list, port, Order.Type.UNPACK_ORIGIN);
                        myOrder.read();
                        while (!myOrder.getStatus()){
                            try {
                                Thread.sleep(500);
                            }catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Message message = new Message();
                        message.what = RECV_LOCATION;
                        showHandler.sendMessage(message);
                    }
                }).start();
            }
        });

        drawPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myOrder.getPackage();
                ArrayList<LocationInfo> locationInfos = myOrder.getLocationDataList();
                dataTask.setDestination(locationInfos);
                dataTask.start();
                for (LocationInfo info: locationInfos) {
                    Message message = new Message();
                    message.what = RL;
                    Bundle bundle = new Bundle();
                    bundle.putDouble("longitude", info.getLng());
                    bundle.putDouble("latitude", info.getLat());
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                    if (info.equals(locationInfos.get(0)))
                        myDraw.drawOrigin(info);
                    else
                        myDraw.drawNew(info);
                }
            }
        });

        closePort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myOrder != null){
                    myOrder.stop();
                    myOrder = null;
                }
                isGpsStop = true;
                isRecvNav = true;
            }
        });

        sendNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            double x = Double.valueOf("18.457812");
                            double y = Double.valueOf("27.481875");

                            if (myOrder != null) {
                                myOrder.getPort().close();
                                myOrder.stop();
                                myOrder = null;
                            }

                            PortClass port = new PortClass(PortClass.portType.FTDI);
                            port.open(MainActivity.this);

                            NavigationInfo info = new NavigationInfo(x, y, 1, false, false);
                            myOrder = new Order(info, port);
                            myOrder.setPackage();
                            myOrder.write();
                            Random rand = new Random();
                            for (int i = 0; i < 9; i++) {
                                NavigationInfo info1 = new NavigationInfo(++x, --y, 1, false, false);
                                myOrder.addNavigationPoint(info1);
                                myOrder.setPackage();
                                Thread.sleep(rand.nextInt(1500) + 500);
                            }
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (myOrder != null) {
                                myOrder.getPort().close();
                                myOrder.stop();
                                myOrder = null;
                            }
                            PortClass port = new PortClass(PortClass.portType.FTDI);
                            port.open(MainActivity.this);

                            myOrder = new Order();
                            myOrder.setOrder(port, Order.Type.SET_NAVIGATION);
                            myOrder.setPackage();
                            myOrder.write();
                            isGpsStop = false;
                            while (!isGpsStop) {
                                NavigationInfo info = dataTask.getInfo();
                                Message message = new Message();
                                message.what = RN;
                                Bundle bundle = new Bundle();
                                if (info != null) {
                                    bundle.putDouble("distance", info.getDistance());
                                    bundle.putDouble("angle", info.getAngle());
                                    bundle.putInt("pos", info.getPos());
                                    message.setData(bundle);
                                    mHandler.sendMessage(message);
                                }
                                myOrder.addNavigationPoint(info);
                                myOrder.setPackage();
                                Thread.sleep(1000);
                            }
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        recvNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (myOrder != null) {
                            myOrder.getPort().close();
                        }

                        PortClass port = new PortClass(PortClass.portType.FTDI);
                        port.open(MainActivity.this);

                        ArrayList<SendPackage> list = new ArrayList<>();
                        myOrder = new Order(list, port, Order.Type.UNPACK_NAVIGATION);
                        myOrder.read();
                        isRecvNav = false;
                        while (!isRecvNav) {
                            myOrder.getPackage();
                            NavigationInfo j = myOrder.getNavigationInfo();
                            if (j != null) {
                                Log.d(TAG, j.getDistance() + "," + j.getAngle());
                                Message message = new Message();
                                message.what = RN;
                                Bundle bundle = new Bundle();
                                bundle.putDouble("distance", j.getDistance());
                                bundle.putDouble("angle", j.getAngle());
                                message.setData(bundle);
                                mHandler.sendMessage(message);
                            }
                        }
                    }
                }).start();
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
        registerGPS();

        dataTask = new DataTask();
    }

    public synchronized void setnInfo(NavigationInfo info){
        nInfo = info;
    }

    public synchronized NavigationInfo getnInfo(){
        return nInfo;
    }

    private String parseByte2HexStr(byte[] buf) {
        StringBuilder sb = new StringBuilder();
        for (byte i: buf) {
            String hex = Integer.toHexString(i & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }
    
    private void registerGPS() throws SecurityException{
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this, "请开启GPS导航...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent,0);
            return;
        }

        String bestProvider = lm.getBestProvider(getCriteria(), true);
        Location location = lm.getLastKnownLocation(bestProvider);
        lm.addGpsStatusListener(listener);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
    }

    private LocationListener locationListener=new LocationListener() {

        public void onLocationChanged(Location location) {
            LocationInfo info = new LocationInfo(location.getLongitude(), location.getLatitude());
            Gps gps = PositionUtil.gps84_To_Gcj02(info.getLat(), info.getLng());
            gps = PositionUtil.gcj02_To_Bd09(gps.getWgLat(), gps.getWgLon());
            info.set(gps.getWgLon(), gps.getWgLat());
            Message message = new Message();
            message.what = LNG_LAT;
            Bundle bundle = new Bundle();
            bundle.putDouble("longitude", info.getLng());
            bundle.putDouble("latitude", info.getLat());
            message.setData(bundle);
            mHandler.sendMessage(message);
            dataTask.setmLocation(info);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.d(TAG, "当前GPS状态为可见状态");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d(TAG, "当前GPS状态为服务区外状态");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d(TAG, "当前GPS状态为暂停服务状态");
                    break;
            }
        }

        public void onProviderEnabled(String provider) throws SecurityException {
            Location location = lm.getLastKnownLocation(provider);
        }

        public void onProviderDisabled(String provider) {
        }
    };

    GpsStatus.Listener listener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    GpsStatus gpsStatus=lm.getGpsStatus(null);
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
                    int count = 0;
                    while (iters.hasNext() && count <= maxSatellites) {
                        GpsSatellite s = iters.next();
                        count++;
                    }
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.d(TAG, "定位启动");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.d(TAG, "定位结束");
                    break;
            }
        }
    };

    private Criteria getCriteria(){
        Criteria criteria=new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);
        criteria.setBearingRequired(false);
        criteria.setAltitudeRequired(false);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }

    private static void refreshScreen(String msg){
        screen.append(msg);
        int offset = screen.getLineCount()*screen.getLineHeight();
        if(offset > screen.getHeight()){
            screen.scrollTo(0, offset - screen.getHeight());
        }
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
                    refreshScreen("Time: " + time + "\nDistance: " + dis + " Angle: " + angle + "\nPos: " + pos + "\n");
                    break;
                case LNG_LAT:
                    double ln = bundle.getDouble("longitude");
                    double la = bundle.getDouble("latitude");
                    longitude.setText(" " + ln);
                    latitude.setText(" " + la);
                    break;
            }
        }
    }

    private static class ShowHandler extends Handler{
        private final WeakReference<Activity> mActivity;
        public ShowHandler(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECV_LOCATION:
                    screen.append("已接受到路径点信息\n");
                    break;
            }
        }
    }

    private Handler mHandler = new MyHandler(this);
    public Handler showHandler = new ShowHandler(this);

    private void PermissionCheck(){
    /* 权限检查 */
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"不能写");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION);
        }
    }



    @Override
    protected void onResume() throws SecurityException{
        super.onResume();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

    }

    @Override
    protected void onPause() throws SecurityException{
        super.onPause();
        lm.removeUpdates(locationListener);
    }

    @Override
    protected void onDestroy() throws SecurityException{
        super.onDestroy();
        lm.removeUpdates(locationListener);
        if (myOrder != null) {
            myOrder.stop();
            myOrder = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ACCESS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "123");
                } else {
                    Toast.makeText(this, "未获取手机权限！", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}