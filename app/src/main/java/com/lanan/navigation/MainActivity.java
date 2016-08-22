package com.lanan.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lanan.navigation.control.DataTask;
import com.lanan.navigation.draw.MyDraw;
import com.lanan.zigbeetransmission.Order;
import com.lanan.zigbeetransmission.PortClass;
import com.lanan.zigbeetransmission.dataclass.LocationInfo;
import com.lanan.zigbeetransmission.dataclass.NavigationInfo;
import com.lanan.zigbeetransmission.dataclass.SendPackage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    MyDraw myDraw;
    Button sendLocation;
    Button recvLocation;
    Button drawPath;
    Button sendNavigation;
    Button recvNavigation;
    Button closePort;
    Button gps;

    TextView nw;
    TextView ne;
    TextView sw;
    TextView se;
    private static TextView screen;

    private NavigationInfo nInfo;
    public LocationManager lm;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private static final String TAG = "Emilio";

    double[][] data = { {112.992146,28.210455}, {112.992156,28.210127}, {112.991599,28.210092},
                        {112.991491,28.209355}, {112.990148,28.209538}, {112.990193,28.209646},
                        {112.990553,28.209638}, {112.991042,28.20955},  {112.995085,28.209264},
                        {112.994559,28.213748}, {112.992834,28.213947}, {112.99252,28.210115},
                        {112.992116,28.210115}} ;

    double[][] sData = {{112.951909,28.179999}, {112.9519,28.180102},   {112.95141,28.180632},
                        {112.950673,28.181634}, {112.948847,28.182287}, {112.95017,28.183147},
                        {112.950458,28.183513}, {112.950458,28.183975}, {112.950512,28.184691},
                        {112.950938,28.185491}} ;

    final int SL = 0;
    final int RL = 1;
    final int SN = 2;
    final int RN = 3;
    final static int RECV_LOCATION = 4;

    Order myOrder;
    DataTask dataTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myDraw = (MyDraw) this.findViewById(R.id.path);
        screen = (TextView) this.findViewById(R.id.showtext);
        screen.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendLocation = (Button) this.findViewById(R.id.sendlocation);
        recvLocation = (Button) this.findViewById(R.id.recvlocation);
        drawPath = (Button) this.findViewById(R.id.drawpath);
        sendNavigation = (Button) this.findViewById(R.id.sendnavigation);
        recvNavigation = (Button) this.findViewById(R.id.recvnavigation);
        closePort = (Button) this.findViewById(R.id.closeport);
        gps = (Button) this.findViewById(R.id.gps);

        sendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (myOrder != null) {
                            myOrder.getPort().close();
                        }
                        PortClass port = new PortClass(getCurrentDevFile(), 115200, 0);
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
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (myOrder != null) {
                            myOrder.getPort().close();
                        }
                        PortClass port = new PortClass(getCurrentDevFile(), 115200, 0);
                        port.open(MainActivity.this);
                        ArrayList<SendPackage> list = new ArrayList<>();
                        myOrder = new Order(list, port, Order.Type.UNPACK_ORIGIN);
                        myOrder.read();
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
                    myOrder.getPort().close();
                }
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
                            }
                            PortClass port = new PortClass(getCurrentDevFile(), 115200, 0);
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
                            PortClass port = new PortClass(getCurrentDevFile(), 115200, 0);
                            port.open(MainActivity.this);

                            myOrder = new Order();
                            myOrder.setOrder(port, Order.Type.SET_NAVIGATION);
                            myOrder.setPackage();
                            myOrder.write();
                            while (true) {
                                NavigationInfo info = dataTask.getInfo();
                                Message message = new Message();
                                message.what = RN;
                                Bundle bundle = new Bundle();
                                if (info != null) {
                                    bundle.putDouble("distance", info.getDistance());
                                    bundle.putDouble("angle", info.getAngle());
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

                        PortClass port = new PortClass(getCurrentDevFile(), 115200, 0);
                        port.open(MainActivity.this);

                        ArrayList<SendPackage> list = new ArrayList<>();
                        myOrder = new Order(list, port, Order.Type.UNPACK_NAVIGATION);
                        myOrder.read();
                        while (true) {
                            myOrder.getPackage();
                            NavigationInfo j = myOrder.getNavigationInfo();
                            if (j != null) {
                                Log.d("Emilio", j.getDistance() + "," + j.getAngle());
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

        nw = (TextView) this.findViewById(R.id.nw);
        ne = (TextView) this.findViewById(R.id.ne);
        sw = (TextView) this.findViewById(R.id.sw);
        se = (TextView) this.findViewById(R.id.se);
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
            myDraw.setBrush(false);
            myDraw.drawNew(info);
            Log.d(TAG, "时间："+location.getTime());
            Log.d(TAG, "经度："+location.getLongitude());
            Log.d(TAG, "纬度："+location.getLatitude());
            Log.d(TAG, "海拔："+location.getAltitude());

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

    private File getCurrentDevFile() {
        File devDir = new File("/dev/");
        if (devDir.canRead() && devDir.isDirectory()) {
            File[] devFiles = devDir.listFiles();
            if (devFiles != null) {
                for (File file: devFiles) {
                    if (file.getName().startsWith("ttyUSB")) {
                        Log.d("Emilio", "Device File Name: " + file.getName());
                        return file;
                    }
                }
            }
        }
        return null;
    }

    private void refreshScreen(String msg){
        screen.append(msg);
        int offset = screen.getLineCount()*screen.getLineHeight();
        if(offset > screen.getHeight()){
            screen.scrollTo(0, offset - screen.getHeight());
        }
    }

    private android.os.Handler mHandler = new android.os.Handler(){
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
                    refreshScreen("Time: " + time + " Distance: " + dis + " Angle: " + angle + "\n");
                    break;
            }
        }
    };

    public static Handler showHandler = new Handler(){
        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            int t = bundle.getInt("type");
            switch (t) {
                case RECV_LOCATION:
                    screen.append("已接受到路径点信息\n");
            }
        }
    };

    @Override
    protected void onResume() throws SecurityException{
        super.onResume();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, locationListener);

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
    }
}
