package com.lanan.zigbeetransmission.serialport;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SPProlificDevIfImp implements IDeviceIf {
	private static List<USBPid> mSupportedDevices = new ArrayList<USBPid>(Arrays.asList(new USBPid[] {
			new USBPid(0x067b, 0x2303), new USBPid(0x067b, 0x04bb), new USBPid(0x067b, 0x1234),
			new USBPid(0x067b, 0xaaa0), new USBPid(0x067b, 0xaaa2), new USBPid(0x067b, 0x0611),
			new USBPid(0x067b, 0x0612), new USBPid(0x067b, 0x0609), new USBPid(0x067b, 0x331a),
			new USBPid(0x067b, 0x0307), new USBPid(0x067b, 0xe1f1), new USBPid(0x067b, 64198),
			new USBPid(0x067b, 24594), new USBPid(2220, 4133), new USBPid(5590, 1), new USBPid(0x067b, 24599) }));

	
	private static final int USB_READ_TIMEOUT_MILLIS = 1000;
	private static final int USB_WRITE_TIMEOUT_MILLIS = 5000;

	private static final int USB_RECIP_INTERFACE = 0x01;

	private static final int PROLIFIC_VENDOR_WRITE_REQUEST = 0x01;

	private static final int PROLIFIC_VENDOR_OUT_REQTYPE = UsbConstants.USB_DIR_OUT | UsbConstants.USB_TYPE_VENDOR;

	private static final int PROLIFIC_CTRL_OUT_REQTYPE = UsbConstants.USB_DIR_OUT | UsbConstants.USB_TYPE_CLASS
			| USB_RECIP_INTERFACE;

	private static final int FLUSH_RX_REQUEST = 0x08;
	private static final int FLUSH_TX_REQUEST = 0x09;

	private static final int SET_LINE_REQUEST = 0x20;
	private static final int SET_CONTROL_REQUEST = 0x22;

	private static final int CONTROL_DTR = 0x01;
	private static final int CONTROL_RTS = 0x02;

	public static final String SP_OPTION_SBONE = "One";
	public static final String SP_OPTION_SBONEPOINTFIVE = "OnePointFive";
	public static final String SP_OPTION_SBTWO = "Two";

	public static final String SP_OPTION_PODD = "Odd";
	public static final String SP_OPTION_PEVEN = "Even";
	public static final String SP_OPTION_PNONE = "None";
	public static final String SP_OPTION_PMARK = "Mark";
	public static final String SP_OPTION_PSPACE = "Space";

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
			Context context = mContext;
			UsbManager usbManager = (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE);
			HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
			Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
			UsbDevice device = null;

			while (deviceIterator.hasNext()) {
				UsbDevice dev = deviceIterator.next();
				USBPid ft = new USBPid(dev.getVendorId(), dev.getProductId());
				Log.d("Emilio", "msg is getVendorId " +dev.getVendorId() + "ProductId" +dev.getProductId()  );
				if (mSupportedDevices.contains(ft)) {
					device = dev;
					break;
				}
			}
			if (device == null) {
				throw new RuntimeException("设备不存在");
			}
			if (!usbManager.hasPermission(device)) {
				PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent("com.scd.USBPermission"), 0);
				usbManager.requestPermission(device, pi);
			}
			if (!usbManager.hasPermission(device)) {
				throw new RuntimeException("无访问权限");
			}

			mConnection = usbManager.openDevice(device);
			if (mConnection == null) {
				throw new RuntimeException("打开设备失败");
			}
			if (mConnection.claimInterface(device.getInterface(0), true) == false) {
				throw new RuntimeException("打开设备接口失败");
			}
			
			mDevice = device;
			purgeHwBuffers();

			int baudRate = Integer.valueOf("115200");
			String stopBits = "One";
			if (TextUtils.isEmpty(stopBits)) {
				stopBits = "One";
			}
			String parity = "None";
			if (TextUtils.isEmpty(parity)) {
				parity = "0";
			}
			setParameters(baudRate, 8, stopBits, parity);

			setRtsAndDtr(false, false);
			mDevice = device;
			readEP = this.mDevice.getInterface(0).getEndpoint(2);
			writeEP = this.mDevice.getInterface(0).getEndpoint(1);

			isOpen = true;
		} finally {
			if (!isOpen) {
				try {
					mConnection.close();
				} catch (Exception e) {
                    e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void Close() {
		if (mConnection != null) {
			mConnection.close();
			mConnection = null;
		}
		mDevice = null;
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
				throw new RuntimeException("发送失败");
			}

			count += sendedLength;
		}

		return 1;
	}

	private void purgeHwBuffers() {
		int ret = this.mConnection.controlTransfer(PROLIFIC_VENDOR_OUT_REQTYPE, PROLIFIC_VENDOR_WRITE_REQUEST,
				FLUSH_RX_REQUEST, 0, null, 0, USB_READ_TIMEOUT_MILLIS);
		if (ret != 0) {
			throw new RuntimeException("Reset rxbuffer failed: result=" + ret);
		}
		ret = this.mConnection.controlTransfer(PROLIFIC_VENDOR_OUT_REQTYPE, PROLIFIC_VENDOR_WRITE_REQUEST,
				FLUSH_TX_REQUEST, 0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
		if (ret != 0) {
			throw new RuntimeException("Reset txbuffer failed: result=" + ret);
		}
	}

	private void setParameters(int baudRate, int dataBits, String stopBits, String parity) {

		byte[] lineRequestData = new byte[7];
		lineRequestData[0] = (byte) (baudRate & 0xff);
		lineRequestData[1] = (byte) ((baudRate >> 8) & 0xff);
		lineRequestData[2] = (byte) ((baudRate >> 16) & 0xff);
		lineRequestData[3] = (byte) ((baudRate >> 24) & 0xff);

		if (stopBits.equals(SP_OPTION_SBONE)) {
			lineRequestData[4] = 0;
		} else if (stopBits.equals(SP_OPTION_SBONEPOINTFIVE)) {
			lineRequestData[4] = 1;
		} else if (stopBits.equals(SP_OPTION_SBTWO)) {
			lineRequestData[4] = 2;
		} else {
			throw new IllegalArgumentException("Unknown stopBits value: " + stopBits);
		}

		if (parity.equals(SP_OPTION_PNONE)) {
			lineRequestData[5] = 0;
		} else if (parity.equals(SP_OPTION_PODD)) {
			lineRequestData[5] = 1;
		} else if (parity.equals(SP_OPTION_PEVEN)) {
			lineRequestData[5] = 2;
		} else if (parity.equals(SP_OPTION_PMARK)) {
			lineRequestData[5] = 3;
		} else if (parity.equals(SP_OPTION_PSPACE)) {
			lineRequestData[5] = 4;
		} else {
			throw new IllegalArgumentException("Unknown parity value: " + parity);
		}

		lineRequestData[6] = (byte) dataBits;

		int ret = mConnection.controlTransfer(PROLIFIC_CTRL_OUT_REQTYPE, SET_LINE_REQUEST, 0, 0, lineRequestData,
				lineRequestData.length, USB_WRITE_TIMEOUT_MILLIS);
		if (ret != lineRequestData.length) {
			throw new RuntimeException(String.format("fail to set up parameter"));
		}
	}

	private void setRtsAndDtr(boolean rts, boolean dtr) {
		int newControlLinesValue = 0;
		if (rts) {
			newControlLinesValue |= CONTROL_RTS;
		} else {
			newControlLinesValue &= ~CONTROL_RTS;
		}
		mConnection.controlTransfer(PROLIFIC_CTRL_OUT_REQTYPE, SET_CONTROL_REQUEST, newControlLinesValue,
                0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
		newControlLinesValue = 0;
		if (dtr) {
			newControlLinesValue |= CONTROL_DTR;
		} else {
			newControlLinesValue &= ~CONTROL_DTR;
		}

		int ret = mConnection.controlTransfer(PROLIFIC_CTRL_OUT_REQTYPE, SET_CONTROL_REQUEST, newControlLinesValue, 0,
				null, 0, USB_WRITE_TIMEOUT_MILLIS);
		if (ret != 0) {
			throw new RuntimeException(String.format("fail to set up dtr and rts"));
		}
	}

}
