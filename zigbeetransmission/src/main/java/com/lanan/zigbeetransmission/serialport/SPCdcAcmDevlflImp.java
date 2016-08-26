package com.lanan.zigbeetransmission.serialport;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SPCdcAcmDevlflImp implements IDeviceIf {
    private static List<USBPid> mSupportedDevices = new ArrayList<>(
            Arrays.asList(new USBPid[]{new USBPid(0x2341, 0x0001), new USBPid(0x2341, 0x0010),
                    new USBPid(0x2341, 0x003b), new USBPid(0x2341, 0x003f), new USBPid(0x2341, 0x0042),
                    new USBPid(0x2341, 0x0043), new USBPid(0x2341, 0x0044), new USBPid(0x2341, 0x8036)}));

    private static final int USB_RECIP_INTERFACE = 0x01;
    private static final int USB_RT_ACM = UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE;

    private static final int SET_LINE_CODING = 0x20;
    private static final int SET_CONTROL_LINE_STATE = 0x22;

    private boolean mEnableAsyncReads;

    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint readEP, writeEP;
    private byte[] mReadBuffer, mWriteBuffer;

    @Override
    public void Open(Context mContext) {
        mReadBuffer = new byte[1024];
        mWriteBuffer = new byte[1024];
        boolean isOpen = false;
        try {
            UsbManager usbManager = (UsbManager) mContext.getApplicationContext().getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
            UsbDevice device = null;
            while (deviceIterator.hasNext()) {

                UsbDevice dev = deviceIterator.next();

                USBPid ft = new USBPid(dev.getVendorId(), dev.getProductId());

                Log.e("msg", "msg is getVendorId " + dev.getVendorId() + "ProductId" + dev.getProductId());
                if (mSupportedDevices.contains(ft)) {
                    device = dev;
                    break;
                }
            }
            if (device == null) {
                throw new RuntimeException("�豸������");
            }
            if (!usbManager.hasPermission(device)) {
                PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent("com.scd.USBPermission"), 0);
                usbManager.requestPermission(device, pi);
            }
            if (!usbManager.hasPermission(device)) {
                throw new RuntimeException("�޷���Ȩ��");
            }
            mConnection = usbManager.openDevice(device);
            if (mConnection == null) {
                throw new RuntimeException("���豸ʧ��");
            }
            if (!mConnection.claimInterface(device.getInterface(0), true)) {
                throw new RuntimeException("���豸�ӿ�ʧ��");
            }
            purgeHwBuffers();

            int baudRate = Integer.valueOf("115200");

            String stopBits = "1";
            if (TextUtils.isEmpty(stopBits)) {
                stopBits = "1";
            }
            String parity = "None";
            if (TextUtils.isEmpty(parity)) {
                parity = "None";
            }
            setParameters(baudRate, 8, 1, 0);

            setRtsAndDtr(false, false);
            mDevice = device;
            readEP = this.mDevice.getInterface(1).getEndpoint(1);
            writeEP = this.mDevice.getInterface(1).getEndpoint(0);

            isOpen = true;
        } finally {
            if (!isOpen) {
                try {
                    assert mConnection != null;
                    mConnection.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void Close() {
        assert mConnection != null;
        mConnection.close();
        mConnection = null;
    }

    @Override
    public int Read(byte[] dest, int offset, int len) {
        if (mEnableAsyncReads) {
            final UsbRequest request = new UsbRequest();
            try {
                request.initialize(mConnection, readEP);
                final ByteBuffer buf = ByteBuffer.wrap(dest);
                final UsbRequest response = mConnection.requestWait();

                final int nread = buf.position();
                if (nread > 0) {
                    return nread;
                } else {
                    return 0;
                }
            } finally {
                request.close();
            }
        }

        int totalBytesRead;
        if (len > mReadBuffer.length) {
            len = mReadBuffer.length;
        }
        totalBytesRead = mConnection.bulkTransfer(readEP, mReadBuffer, len, 5000);
        if (totalBytesRead <= 0)
            return 0;
        int payloadBytesRead = totalBytesRead;
        if (payloadBytesRead > 0) {
            System.arraycopy(mReadBuffer, 0, dest, offset, payloadBytesRead);
        }
        return payloadBytesRead;
    }

    @Override
    public int Write(byte[] buf, int offset, int len) {
        int count = 0;

        while (count < len) {
            int writeLength = Math.min(len - count, this.mWriteBuffer.length);
            System.arraycopy(buf, offset + count, this.mWriteBuffer, 0, writeLength);
            int sendedLength = mConnection.bulkTransfer(writeEP, mWriteBuffer, writeLength, 2000);

            if (sendedLength <= 0) {
                throw new RuntimeException("����ʧ��");
            }

            count += sendedLength;
        }

        return 1;
    }

    private void purgeHwBuffers() {
        mEnableAsyncReads = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1);
    }

    private void setParameters(int baudRate, int dataBits, int stopBits, int parity) {
        byte stopBitsByte;
        switch (stopBits) {
            case 1:
                stopBitsByte = 0;
                break;
            case 2:
                stopBitsByte = 1;
                break;
            case 3:
                stopBitsByte = 2;
                break;
            default:
                throw new IllegalArgumentException("Bad value for stopBits: " + stopBits);
        }

        byte parityBitesByte;
        switch (parity) {
            case 0:
                parityBitesByte = 0;
                break;
            case 1:
                parityBitesByte = 1;
                break;
            case 2:
                parityBitesByte = 2;
                break;
            case 3:
                parityBitesByte = 3;
                break;
            case 4:
                parityBitesByte = 4;
                break;
            default:
                throw new IllegalArgumentException("Bad value for parity: " + parity);
        }

        byte[] msg = {(byte) (baudRate & 0xff), (byte) ((baudRate >> 8) & 0xff), (byte) ((baudRate >> 16) & 0xff),
                (byte) ((baudRate >> 24) & 0xff), stopBitsByte, parityBitesByte, (byte) dataBits};
        mConnection.controlTransfer(USB_RT_ACM, SET_LINE_CODING, 0, 0, msg, msg != null ? msg.length : 0, 5000);
    }

    private void setRtsAndDtr(boolean b, boolean c) {
        int value = (b ? 0x2 : 0) | (c ? 0x1 : 0);
        mConnection.controlTransfer(USB_RT_ACM, SET_CONTROL_LINE_STATE, value, 0, null, 0, 5000);

    }
}
