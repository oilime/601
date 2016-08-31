package com.lanan.zigbeetransmission.serialport;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SPCp21xxDevlflImp implements IDeviceIf {
    @SuppressWarnings("CanBeFinal")
    private static List<USBPid> mSupportedDevices = new ArrayList<>(
            Arrays.asList(new USBPid[]{new USBPid(0x10c4, 0xea60), new USBPid(0x10c4, 0xea70),
                    new USBPid(0x10c4, 0xea71), new USBPid(0x10c4, 0xea80),}));

    private static final int DEFAULT_BAUD_RATE = 115200;

    private static final int USB_WRITE_TIMEOUT_MILLIS = 5000;

    private static final int REQTYPE_HOST_TO_DEVICE = 0x41;

    private static final int SILABSER_IFC_ENABLE_REQUEST_CODE = 0x00;
    private static final int SILABSER_SET_BAUDDIV_REQUEST_CODE = 0x01;
    private static final int SILABSER_SET_LINE_CTL_REQUEST_CODE = 0x03;
    private static final int SILABSER_SET_MHS_REQUEST_CODE = 0x07;
    private static final int SILABSER_SET_BAUDRATE = 0x1E;

    private static final int UART_ENABLE = 0x0001;
    private static final int UART_DISABLE = 0x0000;

    private static final int BAUD_RATE_GEN_FREQ = 0x384000;

    private static final int MCR_ALL = 0x0003;

    private static final int CONTROL_WRITE_DTR = 0x0100;
    private static final int CONTROL_WRITE_RTS = 0x0200;

    @SuppressWarnings("FieldCanBeLocal")
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint readEP, writeEP;
    private byte[] mReadBuffer, mWriteBuffer;

    @Override
    public boolean Open(Context mContext) {
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
                Toast.makeText(mContext, "设备不存在", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!usbManager.hasPermission(device)) {
                PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent("com.scd.USBPermission"), 0);
                usbManager.requestPermission(device, pi);
            }
            if (!usbManager.hasPermission(device)) {
                Toast.makeText(mContext, "无访问权限", Toast.LENGTH_SHORT).show();
                return false;
            }
            mConnection = usbManager.openDevice(device);
            if (mConnection == null) {
                Toast.makeText(mContext, "打开设备失败", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!mConnection.claimInterface(device.getInterface(0), true)) {
                Toast.makeText(mContext, "打开设备接口失败", Toast.LENGTH_SHORT).show();
                return false;
            }
            purgeHwBuffers();

            int baudRate = Integer.valueOf("115200");
            setBaud(baudRate);
            String stopBits = "1";
            if (TextUtils.isEmpty(stopBits)) {
                stopBits = "1";
            }
            String parity = "None";
            if (TextUtils.isEmpty(parity)) {
                parity = "None";
            }
            setDataCharacteristics(8, stopBits, parity);

            mDevice = device;
            readEP = this.mDevice.getInterface(0).getEndpoint(0);
            writeEP = this.mDevice.getInterface(0).getEndpoint(1);


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
        return true;
    }

    @Override
    public void Close() {
        if (mConnection == null) {
            return;
        }
        try {
            mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, SILABSER_IFC_ENABLE_REQUEST_CODE, UART_DISABLE, 0, null,
                    0, USB_WRITE_TIMEOUT_MILLIS);
            mConnection.close();
        } finally {
            mConnection = null;
        }

    }

    @Override
    public int Read(byte[] buf, int offset, int len) {
        int totalBytesRead;
        if (len > mReadBuffer.length) {
            len = mReadBuffer.length;
        }
        totalBytesRead = mConnection.bulkTransfer(readEP, mReadBuffer, len, 5000);
        if (totalBytesRead <= 0)
            return 0;
        int payloadBytesRead = totalBytesRead;
        if (payloadBytesRead > 0) {
            System.arraycopy(mReadBuffer, 0, buf, offset, payloadBytesRead);
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

        mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, SILABSER_IFC_ENABLE_REQUEST_CODE, UART_ENABLE, 0, null, 0,
                USB_WRITE_TIMEOUT_MILLIS);
        mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, SILABSER_SET_MHS_REQUEST_CODE,
                MCR_ALL | CONTROL_WRITE_DTR | CONTROL_WRITE_RTS, 0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
        mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, SILABSER_SET_BAUDDIV_REQUEST_CODE,
                BAUD_RATE_GEN_FREQ / DEFAULT_BAUD_RATE, 0, null, 0, USB_WRITE_TIMEOUT_MILLIS);

    }

    private void setDataCharacteristics(@SuppressWarnings("SameParameterValue") int dataBits, String stopBits, String parity) {

        int configDataBits = 0;
        switch (dataBits) {
            case 5:
                configDataBits |= 0x0500;
                break;
            case 6:
                configDataBits |= 0x0600;
                break;
            case 7:
                configDataBits |= 0x0700;
                break;
            case 8:
                configDataBits |= 0x0800;
                break;
            default:
                configDataBits |= 0x0800;
                break;
        }

        switch (parity) {
            case "ODD":
                configDataBits |= 0x0010;
                break;
            case "EVET":
                configDataBits |= 0x0020;
                break;
            default:

                break;
        }

        switch (stopBits) {
            case "1":
                configDataBits |= 0;
                break;
            case "2":
                configDataBits |= 2;
                break;
        }
        mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, SILABSER_SET_LINE_CTL_REQUEST_CODE, configDataBits, 0, null,
                0, USB_WRITE_TIMEOUT_MILLIS);
    }

    private void setBaud(int baudRate) {
        byte[] data = new byte[]{(byte) (baudRate & 0xff), (byte) ((baudRate >> 8) & 0xff),
                (byte) ((baudRate >> 16) & 0xff), (byte) ((baudRate >> 24) & 0xff)};
        int ret = mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, SILABSER_SET_BAUDRATE, 0, 0, data, 4,
                USB_WRITE_TIMEOUT_MILLIS);
        if (ret < 0) {
            // ���ò����ʴ���
            Log.e("msg", "���������ó���");
        }
    }

}
