package com.lanan.zigbeetransmission;

import android.util.Log;

import com.lanan.zigbeetransmission.dataclass.LocationInfo;
import com.lanan.zigbeetransmission.dataclass.NavigationInfo;
import com.lanan.zigbeetransmission.dataclass.PortClass;
import com.lanan.zigbeetransmission.dataclass.SendPackage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class Order {

    private final byte[] head = new byte[]{(byte) 0xfe, (byte) 0xdd, (byte) 0xdf};    //数据包头
    private final byte[] oriLen = new byte[]{0x1a};                                     //a-->b数据包长度:26
    private final byte[] naviLen = new byte[]{0x23};                                     //b-->c数据包长度:35
    private final byte[] userId = new byte[]{0x01};                                     //用户id
    private final byte[] startPos = new byte[]{0x24};                                     //起始位
    private final byte[] lngSig = new byte[]{0x20};                                     //标志位，后方数据为经度
    private final byte[] latSig = new byte[]{0x2d};                                     //标志位，后方数据为纬度
    private final byte[] packageC = new byte[]{0x00};                                     //标志位，后方还有数据包
    private final byte[] packageO = new byte[]{0x01};                                     //标志位，最后一个数据包
    private final byte[] distanceSig = new byte[]{0x28};                                     //标志位，后方数据为距离
    private final byte[] angleSig = new byte[]{0x30};                                     //标志位，后方数据为角度
    private final byte[] posSig = new byte[]{0x43};                                     //标志位，后方数据为路径点号
    private final byte[] arriveSig = new byte[]{0x1a};                                     //标志位，后方数据为是否达到
    private final byte[] yawSig = new byte[]{0x11};                                     //标志位，后方数据为是否偏航
    private byte[] doBuffer = new byte[1024];

    private ArrayList<SendPackage> sendList = new ArrayList<>();
    private ArrayList<SendPackage> recvList;
    private ArrayList<LocationInfo> locationList;
    private ArrayList<NavigationInfo> navigationList = new ArrayList<>();
    private LinkedList<NavigationInfo> recvNavigationList = new LinkedList<>();
    private PortClass port;

    public enum Type {SET_ORIGIN, UNPACK_ORIGIN, SET_NAVIGATION, UNPACK_NAVIGATION}
    private Type orderType;

    private boolean isReadFinished = false;
    private boolean isWriteFinished = false;

    private int count = 0;
    private int doCount = 0;
    private int rate = 2000;

    private Thread receive;
    private Thread send;
    private Thread handleNav;
    private Thread handleOri;

    public Order() {
        super();
    }

    public Order(double[][] data) {
        locationList = new ArrayList<>();
        for (double[] p : data) {
            LocationInfo info = new LocationInfo(p[0], p[1]);
            locationList.add(info);
        }
        this.orderType = Type.SET_ORIGIN;
    }

    public Order(double[][] data, PortClass port) {
        this.port = port;
        locationList = new ArrayList<>();
        for (double[] p : data) {
            LocationInfo info = new LocationInfo(p[0], p[1]);
            locationList.add(info);
        }
        this.orderType = Type.SET_ORIGIN;

        sendList = new ArrayList<>();
        try {
            for (int i = 0; i < locationList.size(); i++) {
                LocationInfo point = locationList.get(i);
                byte[] packageData = new byte[26];

                System.arraycopy(head, 0, packageData, 0, 3);
                System.arraycopy(oriLen, 0, packageData, 3, 1);
                System.arraycopy(userId, 0, packageData, 4, 1);
                System.arraycopy(startPos, 0, packageData, 5, 1);

                byte[] longitude = double2Bytes(point.getLng());
                byte[] latitude = double2Bytes(point.getLat());

                System.arraycopy(lngSig, 0, packageData, 6, 1);
                System.arraycopy(longitude, 0, packageData, 7, 8);
                System.arraycopy(latSig, 0, packageData, 15, 1);
                System.arraycopy(latitude, 0, packageData, 16, 8);

                if (i == locationList.size() - 1) {
                    System.arraycopy(packageO, 0, packageData, 24, 1);
                } else {
                    System.arraycopy(packageC, 0, packageData, 24, 1);
                }

                byte[] crc = new byte[]{getCrc(packageData, packageData.length - 1)};
                System.arraycopy(crc, 0, packageData, 25, 1);

                sendList.add(new SendPackage(packageData));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Order(NavigationInfo info, PortClass port) {
        this.port = port;
        this.orderType = Type.SET_NAVIGATION;
        navigationList.add(info);
    }

    public Order(ArrayList<SendPackage> list, PortClass port, Type type) {
        this.port = port;
        locationList = new ArrayList<>();
        this.recvList = list;
        this.orderType = type;
    }

    public void addNavigationPoint(NavigationInfo info) {
        navigationList.add(info);
    }

    public boolean setPackage() {
        switch (orderType) {
            case SET_NAVIGATION:
                int sendLen = sendList.size();
                int navLen = navigationList.size();

                if (sendLen >= navLen) {
                    break;
                } else {
                    for (int i = sendLen; i < navLen; i++) {
                        try {
                            NavigationInfo info = navigationList.get(i);
                            byte[] packageData = new byte[35];

                            System.arraycopy(head, 0, packageData, 0, 3);
                            System.arraycopy(naviLen, 0, packageData, 3, 1);
                            System.arraycopy(userId, 0, packageData, 4, 1);
                            System.arraycopy(startPos, 0, packageData, 5, 1);

                            byte[] distance = double2Bytes(info.getDistance());
                            byte[] angle = double2Bytes(info.getAngle());
                            byte[] pos = int2Bytes(info.getPos());

                            byte[] aStatus;
                            if (info.isArrived()) {
                                aStatus = new byte[]{0x01};
                            } else {
                                aStatus = new byte[]{0x00};
                            }

                            byte[] yStatus;
                            if (info.isYawed()) {
                                yStatus = new byte[]{0x01};
                            } else {
                                yStatus = new byte[]{0x00};
                            }

                            System.arraycopy(distanceSig, 0, packageData, 6, 1);
                            System.arraycopy(distance, 0, packageData, 7, 8);
                            System.arraycopy(angleSig, 0, packageData, 15, 1);
                            System.arraycopy(angle, 0, packageData, 16, 8);
                            System.arraycopy(posSig, 0, packageData, 24, 1);
                            System.arraycopy(pos, 0, packageData, 25, 4);
                            System.arraycopy(arriveSig, 0, packageData, 29, 1);
                            System.arraycopy(aStatus, 0, packageData, 30, 1);
                            System.arraycopy(yawSig, 0, packageData, 31, 1);
                            System.arraycopy(yStatus, 0, packageData, 32, 1);
                            System.arraycopy(packageC, 0, packageData, 33, 1);

                            byte[] crc = new byte[]{getCrc(packageData, packageData.length - 1)};
                            System.arraycopy(crc, 0, packageData, 34, 1);

                            sendList.add(new SendPackage(packageData));
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    return true;
                }
        }
        return false;
    }

    public void getPackage() {
        switch (orderType) {
            case UNPACK_ORIGIN:
                for (int i = 0; i < recvList.size(); i++) {
                    byte[] packageData = recvList.get(i).getPackageData();

                    byte[] testHead = new byte[3];
                    System.arraycopy(packageData, 0, testHead, 0, 3);
                    if (Arrays.equals(testHead, head)) {
                        LocationInfo point = new LocationInfo();

                        byte[] lngCheck = new byte[1];
                        System.arraycopy(packageData, 6, lngCheck, 0, 1);
                        if (Arrays.equals(lngCheck, lngSig)) {
                            byte[] recvLng = new byte[8];
                            System.arraycopy(packageData, 7, recvLng, 0, 8);
                            point.setLng(bytes2Double(recvLng));
                        } else {
                            break;
                        }

                        byte[] latCheck = new byte[1];
                        System.arraycopy(packageData, 15, latCheck, 0, 1);
                        if (Arrays.equals(latCheck, latSig)) {
                            byte[] recvLat = new byte[8];
                            System.arraycopy(packageData, 16, recvLat, 0, 8);
                            point.setLat(bytes2Double(recvLat));
                        } else {
                            break;
                        }

                        locationList.add(point);
                    } else {
                        break;
                    }
                }
                break;
            case UNPACK_NAVIGATION:
                int recvLen = recvList.size();
                int rnavLen = recvNavigationList.size();
                if (rnavLen >= recvLen) {
                    break;
                } else {
                    for (int i = rnavLen; i < recvLen; i++) {
                        byte[] packageData = recvList.get(i).getPackageData();

                        byte[] testHead = new byte[3];
                        System.arraycopy(packageData, 0, testHead, 0, 3);
                        if (Arrays.equals(testHead, head)) {
                            NavigationInfo nPoint = new NavigationInfo();

                            byte[] disCheck = new byte[1];
                            System.arraycopy(packageData, 6, disCheck, 0, 1);
                            if (Arrays.equals(disCheck, distanceSig)) {
                                byte[] recvDis = new byte[8];
                                System.arraycopy(packageData, 7, recvDis, 0, 8);
                                nPoint.setDistance(bytes2Double(recvDis));
                            } else {
                                break;
                            }

                            byte[] angleCheck = new byte[1];
                            System.arraycopy(packageData, 15, angleCheck, 0, 1);
                            if (Arrays.equals(angleCheck, angleSig)) {
                                byte[] recvAngle = new byte[8];
                                System.arraycopy(packageData, 16, recvAngle, 0, 8);
                                nPoint.setAngle(bytes2Double(recvAngle));
                            } else {
                                break;
                            }

                            byte[] posCheck = new byte[1];
                            System.arraycopy(packageData, 24, posCheck, 0, 1);
                            if (Arrays.equals(posCheck, posSig)) {
                                byte[] recvPos = new byte[4];
                                System.arraycopy(packageData, 25, recvPos, 0, 4);
                                nPoint.setPos(bytes2Int(recvPos));
                            }

                            byte[] arStatusCheck = new byte[1];
                            System.arraycopy(packageData, 29, angleCheck, 0, 1);
                            if (Arrays.equals(arStatusCheck, arriveSig)) {
                                byte[] recvAS = new byte[1];
                                System.arraycopy(packageData, 30, recvAS, 0, 1);
                                if (Arrays.equals(recvAS, new byte[]{0x01})) {
                                    nPoint.setArriveStatus(true);
                                } else if (Arrays.equals(recvAS, new byte[]{0x00})) {
                                    nPoint.setArriveStatus(false);
                                }
                            }

                            byte[] yawStatusCheck = new byte[1];
                            System.arraycopy(packageData, 31, yawStatusCheck, 0, 1);
                            if (Arrays.equals(yawStatusCheck, yawSig)) {
                                byte[] recvYS = new byte[1];
                                System.arraycopy(packageData, 32, recvYS, 0, 1);
                                if (Arrays.equals(recvYS, new byte[]{0x01})) {
                                    nPoint.setYawStatus(true);
                                } else if (Arrays.equals(recvYS, new byte[]{0x00})) {
                                    nPoint.setYawStatus(false);
                                }
                            }
                            recvNavigationList.add(nPoint);
                        } else {
                            break;
                        }
                    }
                    break;
                }
        }
    }

    private class receiveThread extends Thread {
        @Override
        public void run() {
            try {
                bufferSet(doBuffer);
                while (!isInterrupted()) {
                    int len;
                    byte[] readBuffer = new byte[1024];
                    bufferSet(readBuffer);
                    while ((len = port.read(readBuffer, 0, readBuffer.length)) != -1) {
                        setDoBuffer(readBuffer, len);
                        bufferSet(readBuffer);
                        Thread.sleep(500);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class sendThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                if (!sendList.isEmpty() && count < sendList.size()) {
                    for (int i = count; i < sendList.size(); i++) {
                        byte[] navigationPackage = sendList.get(i).getPackageData();
                        port.write(navigationPackage, 0, navigationPackage.length);
                        count++;
                        try {
                            Thread.sleep(rate);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d("Emilio", "package " + count + ": " + parseByte2HexStr(navigationPackage));
                    }
                }
            }
        }
    }

    private class handleThread extends Thread {
        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    byte[] p = getDoBuffer(35);
                    if (p != null) {
                        Log.d("Emilio", "get: " + parseByte2HexStr(p));
                        recvList.add(new SendPackage(p));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class handleOriThread extends Thread {
        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    byte[] p = getDoBuffer(26);
                    if (p != null) {
                        Log.d("Emilio", "get: " + parseByte2HexStr(p));
                        recvList.add(new SendPackage(p));
                        if (p[24] == packageO[0]) {
                            receive.interrupt();
                            break;
                        }
                    }
                }
                isReadFinished = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void read() {
        stopAllThread();
        receive = new receiveThread();
        receive.start();
        switch (orderType) {
            case UNPACK_ORIGIN:
                handleOri = new handleOriThread();
                handleOri.start();
                break;
            case UNPACK_NAVIGATION:
                handleNav = new handleThread();
                handleNav.start();
                break;
        }
    }

    public void write() {
        stopAllThread();
        switch (orderType) {
            case SET_ORIGIN:
                for (SendPackage p : sendList) {
                    byte[] writeBuffer = p.getPackageData();
                    port.write(writeBuffer, 0, writeBuffer.length);
                    try {
                        Thread.sleep(rate);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.d("Emilio", "write:" + parseByte2HexStr(writeBuffer));
                }
                isWriteFinished = true;
                break;
            case SET_NAVIGATION:
                send = new sendThread();
                send.start();
                break;
        }
    }

    public ArrayList<LocationInfo> getLocationDataList() {
        return locationList;
    }

    public NavigationInfo getNavigationInfo() {
        if (!recvNavigationList.isEmpty()) {
            NavigationInfo info = recvNavigationList.getLast();
            recvNavigationList.clear();
            recvList.clear();
            return info;
        } else
            return null;
    }

    public void setOrder(PortClass port, Type type) {
        this.port = port;
        this.orderType = type;
    }

    public void setSendRate (int rate) {
        this.rate = rate;
    }

    private void stopAllThread() {
        if (send != null && send.isAlive()) {
            Log.d("Emilio", "send interrupt");
            send.interrupt();
            send = null;
        }
        if (receive != null && receive.isAlive()) {
            Log.d("Emilio", "receive interrupt");
            receive.interrupt();
            receive = null;
        }
        if (handleNav != null && handleNav.isAlive()) {
            Log.d("Emilio", "handleNav interrupt");
            handleNav.interrupt();
            handleNav = null;
        }
        if (handleOri != null && handleOri.isAlive()) {
            Log.d("Emilio", "handleOri interrupt");
            handleOri.interrupt();
            handleOri = null;
        }
    }

    private synchronized void setDoBuffer(byte[] buffer, int len) {
        System.arraycopy(buffer, 0, doBuffer, doCount, len);
        doCount += len;
    }

    private synchronized byte[] getDoBuffer(int len) {
        if (doCount < len)
            return null;
        else {
            byte[] check = new byte[len];
            System.arraycopy(doBuffer, 0, check, 0, len);
            if (getCrc(check, len - 1) == check[len - 1]) {
                byte[] leave = new byte[doCount - len];
                System.arraycopy(doBuffer, 35, leave, 0, leave.length);
                bufferSet(doBuffer);
                System.arraycopy(leave, 0, doBuffer, 0, leave.length);
                doCount -= len;
                return check;
            }
        }
        return null;
    }

    public void stop() {
        stopAllThread();
        this.port.close();
        doBuffer = null;
        sendList = null;
        recvList = null;
        recvNavigationList = null;
        locationList = null;
        navigationList = null;
    }

    public boolean getStatus() {
        switch (orderType) {
            case SET_ORIGIN:
            case SET_NAVIGATION:
                return isWriteFinished;
            case UNPACK_ORIGIN:
                return isReadFinished;
            default:
                return false;
        }
    }

    private int bytes2Int(byte[] b) {
        int i = (b[0] << 24) & 0xFF000000;
        i |= (b[1] << 16) & 0xFF0000;
        i |= (b[2] << 8) & 0xFF00;
        i |= b[3] & 0xFF;
        return i;
    }

    private byte[] int2Bytes(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) (i >>> 24);
        b[1] = (byte) (i >>> 16);
        b[2] = (byte) (i >>> 8);
        b[3] = (byte) i;
        return b;
    }

    private double bytes2Double(byte[] b) {
        return Double.longBitsToDouble(bytes2Long(b));
    }

    private long bytes2Long(byte[] b) {
        long l = ((long) b[0] << 56) & 0xFF00000000000000L;
        l |= ((long) b[1] << 48) & 0xFF000000000000L;
        l |= ((long) b[2] << 40) & 0xFF0000000000L;
        l |= ((long) b[3] << 32) & 0xFF00000000L;
        l |= ((long) b[4] << 24) & 0xFF000000L;
        l |= ((long) b[5] << 16) & 0xFF0000L;
        l |= ((long) b[6] << 8) & 0xFF00L;
        l |= (long) b[7] & 0xFFL;
        return l;
    }

    private byte[] double2Bytes(double d) {
        return long2Bytes(Double.doubleToLongBits(d));
    }

    private byte[] long2Bytes(long l) {
        byte[] b = new byte[8];
        b[0] = (byte) (l >>> 56);
        b[1] = (byte) (l >>> 48);
        b[2] = (byte) (l >>> 40);
        b[3] = (byte) (l >>> 32);
        b[4] = (byte) (l >>> 24);
        b[5] = (byte) (l >>> 16);
        b[6] = (byte) (l >>> 8);
        b[7] = (byte) (l);
        return b;
    }

    private byte getCrc(byte[] data, int len) {
        byte crc = 0;
        for (int i = 0; i < len; i++) {
            crc ^= data[i];
        }
        return crc;
    }

    private String parseByte2HexStr(byte[] buf) {
        StringBuilder sb = new StringBuilder();
        for (byte i : buf) {
            String hex = Integer.toHexString(i & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    @SuppressWarnings("UnusedReturnValue")
    private void bufferSet(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = 0;
        }
    }
}