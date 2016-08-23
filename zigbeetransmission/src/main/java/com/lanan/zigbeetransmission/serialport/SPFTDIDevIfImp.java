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

public class SPFTDIDevIfImp implements IDeviceIf{
	private final String TAG = SPFTDIDevIfImp.class.getSimpleName();

    private static List<USBPid> mSupportedDevices = new ArrayList<USBPid>(
            Arrays.asList(new USBPid[] { new USBPid(0x1027, 0x24597), new USBPid(0x1027, 0x24596),
                    new USBPid(0x1027, 0x24593), new USBPid(0x1027, 0x24592), new USBPid(1027, 24577),
                    new USBPid(0x1027, 0x24582), new USBPid(0x1027, 0x64193), new USBPid(0x1027, 0x64194),
                    new USBPid(0x1027, 0x64195), new USBPid(0x1027, 0x64196), new USBPid(0x1027, 0x64197),
                    new USBPid(0x1027, 0x64198), new USBPid(0x1027, 0x24594), new USBPid(0x2220, 0x4133),
                    new USBPid(0x5590, 0x1),     new USBPid(0x1027, 0x24599), new USBPid()}));

    public static final int USB_RECIP_DEVICE = 0x00;
    public static final int USB_RECIP_INTERFACE = 0x01;
    public static final int USB_RECIP_ENDPOINT = 0x02;
    public static final int USB_RECIP_OTHER = 0x03;
    public static final int USB_ENDPOINT_IN = 0x80;
    public static final int USB_ENDPOINT_OUT = 0x00;
    private static final int  SIO_SET_DTR_MASK = 0x1;
    private static final int  SIO_SET_DTR_HIGH =( 1 | ( SIO_SET_DTR_MASK  << 8));
    private static final int  SIO_SET_DTR_LOW =(SIO_SET_DTR_MASK  << 8);
    private static final int  SIO_SET_RTS_MASK = 0x2;
    private static final int  SIO_SET_RTS_HIGH =( 2 | ( SIO_SET_RTS_MASK << 8 ));
    private static final int  SIO_SET_RTS_LOW = ( SIO_SET_RTS_MASK << 8 );

    private static final int SIO_RESET_REQUEST = 0;
    private static final int SIO_SET_MODEM_CTRL_REQUEST = 1;
    private static final int SIO_SET_BAUD_RATE_REQUEST = 3;
    private static final int SIO_SET_DATA_REQUEST = 4;
    private static final int SIO_RESET_SIO = 0;

	public static final String SP_OPTION_PODD = "Odd";
	public static final String SP_OPTION_PEVEN = "Even";
	public static final String SP_OPTION_PNONE = "None";
	public static final String SP_OPTION_PMARK = "Mark";
	public static final String SP_OPTION_PSPACE = "Space";

    public static final String SP_OPTION_SBONE = "One";
    public static final String SP_OPTION_SBONEPOINTFIVE = "OnePointFive";
    public static final String SP_OPTION_SBTWO = "Two";
   
    public static final int FTDI_DEVICE_OUT_REQTYPE =
            UsbConstants.USB_TYPE_VENDOR;
   
    public static final int FTDI_DEVICE_IN_REQTYPE =
            UsbConstants.USB_TYPE_VENDOR | USB_ENDPOINT_IN;
   
    /**
     * Length of the modem status header, transmitted with every read.
     */
    private static final int MODEM_STATUS_HEADER_LENGTH = 2;
	
	
	private DeviceType mType;
	
	private UsbDevice mDevice;
	private UsbDeviceConnection mConnection;
	private UsbEndpoint readEP,writeEP;
	private byte[] mReadBuffer, mWriteBuffer;
	
	
	public SPFTDIDevIfImp() {
		mReadBuffer = new byte[1024];
		mWriteBuffer = new byte[1024];
	}
	
	@Override
	public void Open(Context mContext) {
		boolean isOpen = false;
		try{
			Context context = mContext;
			UsbManager usbManager = (UsbManager)context.getApplicationContext().getSystemService(Context.USB_SERVICE);
			HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList(); 
			Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();  
			UsbDevice device=null;
			while (deviceIterator.hasNext()) {
                UsbDevice dev = deviceIterator.next();
                USBPid ft = new USBPid(dev.getVendorId(), dev.getProductId());
                Log.d("Emilio", "msg is getVendorId " +dev.getVendorId() + "ProductId" +dev.getProductId()  );
                if (mSupportedDevices.contains(ft)) {
                    device = dev;
                    break;
                }
			}  
			if(device == null){
				throw new RuntimeException("设备不存在");
			}
			if(!usbManager.hasPermission(device)){
				PendingIntent pi =  PendingIntent.getBroadcast(context, 0, new Intent("com.scd.USBPermission"), 0);  
				usbManager.requestPermission(device, pi);
			}
			if(!usbManager.hasPermission(device)){
				throw new RuntimeException("无访问权限");
			}
			mConnection = usbManager.openDevice(device);
			if(mConnection == null){
				throw new RuntimeException("打开设备失败");
			}
			if(!mConnection.claimInterface(device.getInterface(0), true)){
				throw new RuntimeException("打开设备接口失败");
			}
			purgeHwBuffers();

			int baudRate = Integer.valueOf("115200");
			setBaud(baudRate);

			String stopBits = "One";
			if(TextUtils.isEmpty(stopBits)){
				stopBits = "One";
			}
			String parity = "None";
			if(TextUtils.isEmpty(parity)){
				parity = "0";
			}
			setDataCharacteristics(8,stopBits,parity);
			setRtsAndDtr(false,false);
			mDevice = device;
			readEP = this.mDevice.getInterface(0).getEndpoint(0);
			writeEP = this.mDevice.getInterface(0).getEndpoint(1);
			isOpen = true;
		}finally{
			if(!isOpen){
				try{
					mConnection.close();
				}catch(Exception e1){
					//忽略
				}
			}
		}
	}

	@Override
	public void Close() {
		if(mConnection != null){
			mConnection.close();
			mConnection = null;
		}
		mDevice = null;
	}
	
	@Override
	public int Read(byte[] buf, int offset, int len) {		
	    int totalBytesRead;
	    if(len > mReadBuffer.length){
	    	len = mReadBuffer.length;
	    }
	    totalBytesRead = mConnection.bulkTransfer(readEP, mReadBuffer, len, 5000);
	    if (totalBytesRead < MODEM_STATUS_HEADER_LENGTH) {
	    	return 0;
	     // throw new RuntimeException("Expected at least 2 bytes");
	    }

	    int payloadBytesRead = totalBytesRead - MODEM_STATUS_HEADER_LENGTH;
	    
	    if (payloadBytesRead > 0) {
	      System.arraycopy(mReadBuffer, MODEM_STATUS_HEADER_LENGTH, buf, offset, payloadBytesRead);
	    }
	    return payloadBytesRead;
	}

	@Override
	public int Write(byte[] buf, int offset, int len) {		
	    int count = 0;
	    Log.d("Emilio", writeEP.toString());
	    while (count < len)
	    {
	      int writeLength = Math.min(len - count, this.mWriteBuffer.length);
	      System.arraycopy(buf, offset + count, this.mWriteBuffer, 0, writeLength);
	      int sendedLength = this.mConnection.bulkTransfer(writeEP, mWriteBuffer, writeLength, 2000);
	      
	      if (sendedLength <= 0) {
	        throw new RuntimeException("发送失败");
	      }

	      count += sendedLength;
	    }
	    
	    return 1;
	}

	private void purgeHwBuffers(){
		int result = this.mConnection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE, SIO_RESET_REQUEST, 
				SIO_RESET_SIO, 0, null, 0, 5000);
		if (result != 0) {
			throw new RuntimeException("Reset failed: result=" + result);
		}
		this.mType = DeviceType.TYPE_R;
	}
	
	private int setBaud(int baudRate){
	    long[] vals = convertBaudrate(baudRate);
	    long actualBaudrate = vals[0];
	    long index = vals[1];
	    long value = vals[2];
	    Log.i(this.TAG, "Requested baudrate=" + baudRate + ", actual=" + actualBaudrate);
	    
	    int result = this.mConnection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE, 
	    		SIO_SET_BAUD_RATE_REQUEST, (int)value, (int)index, null, 0, 5000);
	    if (result != 0) {
	    	throw new RuntimeException("Setting baudrate failed: result=" + result);
	    }
	    return (int)actualBaudrate;
	}
	private long[] convertBaudrate(int baudrate)
	{
		int divisor = 24000000 / baudrate;
	    int bestDivisor = 0;
	    int bestBaud = 0;
	    int bestBaudDiff = 0;
	    int[] fracCode = { 0, 3, 2, 4, 1, 5, 6, 7 };

	    for (int i = 0; i < 2; i++) {
	    	int tryDivisor = divisor + i;
	    	if (tryDivisor <= 8){
	    		tryDivisor = 8;
	    	} else if ((this.mType != DeviceType.TYPE_AM) && (tryDivisor < 12)){
	    		tryDivisor = 12;
	    	} else if (divisor < 16){
	    		tryDivisor = 16;
	    	}else if (this.mType != DeviceType.TYPE_AM){
	    		if (tryDivisor > 131071){
	    			tryDivisor = 131071;
	    		}
	    	}

	    	int baudEstimate = (24000000 + tryDivisor / 2) / tryDivisor;
	    	int baudDiff;
	    	if (baudEstimate < baudrate){
	    		baudDiff = baudrate - baudEstimate;
	    	}else {
	    		baudDiff = baudEstimate - baudrate;
	    	}

	    	if ((i == 0) || (baudDiff < bestBaudDiff))
	    	{
	    		bestDivisor = tryDivisor;
	    		bestBaud = baudEstimate;
	    		bestBaudDiff = baudDiff;
	    		if (baudDiff == 0) {
	    			break;
	    		}
	    	}
	    }

	    long encodedDivisor = bestDivisor >> 3 | fracCode[(bestDivisor & 0x7)] << 14;

	    if (encodedDivisor == 1L){
	    	encodedDivisor = 0L;
	    }else if (encodedDivisor == 16385L) {
	    	encodedDivisor = 1L;
	    }

	    long value = encodedDivisor & 0xFFFF;
	    long index;
	    if ((this.mType == DeviceType.TYPE_2232C) || (this.mType == DeviceType.TYPE_2232H) ||  (this.mType == DeviceType.TYPE_4232H)) {
	      index = encodedDivisor >> 8 & 0xFFFF;
	      index &= 65280L;
	      index |= 0L;
	    } else {
	      index = encodedDivisor >> 16 & 0xFFFF;
	    }

	    return new long[] {  bestBaud, index, value };
	  }
	private boolean setDataCharacteristics(int dataBits, String stopBits, String parity)
	{
        int config = dataBits; 
        if(parity.equals(SP_OPTION_PNONE)){
        	config |= (0);
        }else if(parity.equals(SP_OPTION_PODD)){
        	config |= (0x01 << 8);
        }else if(parity.equals(SP_OPTION_PEVEN)){
        	config |= (0x02 << 8);
        }else if(parity.equals(SP_OPTION_PMARK)){
        	config |= (0x03 << 8);
        }else if(parity.equals(SP_OPTION_PSPACE)){
        	 config |= (0x04 << 8);
        }else {
        	throw new IllegalArgumentException("Unknown parity value: " + parity);
		}
        if(stopBits.equals(SP_OPTION_SBONE)){
        	config |= (0);
        }else if(stopBits.equals(SP_OPTION_SBONEPOINTFIVE)){
        	config |= (0x01 << 11);
        }else if(stopBits.equals(SP_OPTION_SBTWO)){
        	config |= (0x02 << 11);
        }else {
        	throw new IllegalArgumentException("Unknown stopBits value: " + stopBits);
		}
        
        int result = mConnection.controlTransfer(FTDI_DEVICE_OUT_REQTYPE,
                SIO_SET_DATA_REQUEST, config, 0 /* index */,null, 0, 5000);
        if (result != 0) {
            throw new RuntimeException("Setting parameters failed: result=" + result);
        }
        return true;
	  }
	private void setRtsAndDtr(boolean rts,boolean dtr){
		short usb_val = 0;
        if (dtr){
            usb_val = SIO_SET_DTR_HIGH;
        }else{
            usb_val = SIO_SET_DTR_LOW;
        }
        if (rts) {
            usb_val |= SIO_SET_RTS_HIGH;
        }else {
            usb_val |= SIO_SET_RTS_LOW;
        }

      mConnection.controlTransfer( FTDI_DEVICE_OUT_REQTYPE, 
    		  SIO_SET_MODEM_CTRL_REQUEST, /* Set the modem control register */
              usb_val, /* clear/set RTS/DTR */
              0,/* index */
              null, /* buffer is null */
              0, /* length is 0 for null buffer */
              5000 /* timeout in millisecond */
              );
	}
	
	private enum DeviceType {
		TYPE_BM, TYPE_AM, TYPE_2232C, TYPE_R, TYPE_2232H, TYPE_4232H
	}
}
