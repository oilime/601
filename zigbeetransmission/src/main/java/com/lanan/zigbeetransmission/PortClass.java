package com.lanan.zigbeetransmission;

import android.content.Context;

import com.lanan.zigbeetransmission.serialport.SPCdcAcmDevlflImp;
import com.lanan.zigbeetransmission.serialport.SPCp21xxDevlflImp;
import com.lanan.zigbeetransmission.serialport.SPFTDIDevIfImp;
import com.lanan.zigbeetransmission.serialport.SPProlificDevIfImp;
import com.lanan.zigbeetransmission.serialport.SerialPort;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class PortClass {

    public enum portType {CDCACM, CP21, FTDI, PROLIFIC, PORT}

    private portType mType;
    private static SPCdcAcmDevlflImp cdcAcmDevlflImp;
    private static SPCp21xxDevlflImp cp21xxDevlflImp;
    private static SPFTDIDevIfImp spftdiDevIfImp;
    private static SPProlificDevIfImp prolificDevIfImp;
    private SerialPort serialPort;

    private File portFile;
    private int baudRate;
    private int flags;

    private InputStream in;
    private OutputStream out;

    public PortClass(File device, int baudRate, int flags) {
        this.portFile = device;
        this.baudRate = baudRate;
        this.flags = flags;
        this.mType = portType.PORT;
    }

    public PortClass(portType type) {
        this.mType = type;
        switch (mType) {
            case CDCACM:
                cdcAcmDevlflImp = new SPCdcAcmDevlflImp();
                break;
            case CP21:
                cp21xxDevlflImp = new SPCp21xxDevlflImp();
                break;
            case FTDI:
                spftdiDevIfImp = new SPFTDIDevIfImp();
                break;
            case PROLIFIC:
                prolificDevIfImp = new SPProlificDevIfImp();
                break;
        }
    }

    public void open(Context mContext) {
        switch (mType) {
            case CDCACM:
                cdcAcmDevlflImp.Open(mContext);
                break;
            case CP21:
                cp21xxDevlflImp.Open(mContext);
                break;
            case FTDI:
                spftdiDevIfImp.Open(mContext);
                break;
            case PROLIFIC:
                prolificDevIfImp.Open(mContext);
                break;
            case PORT:
                try {
                    serialPort = new SerialPort(portFile, baudRate, flags);
                    in = serialPort.getInputStream();
                    out = serialPort.getOutputStream();
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
        }
    }

    public int read(byte[] dest, int offset, int len) {
        switch (mType) {
            case CDCACM:
                return cdcAcmDevlflImp.Read(dest, offset, len);
            case CP21:
                return cp21xxDevlflImp.Read(dest, offset, len);
            case FTDI:
                return spftdiDevIfImp.Read(dest, offset, len);
            case PROLIFIC:
                return prolificDevIfImp.Read(dest, offset, len);
            case PORT:
                try {
                    return in.read(dest, offset, len);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            default:
                return -1;
        }
    }

    public int write(byte[] buf, int offset, int len) {
        switch (mType) {
            case CDCACM:
                return cdcAcmDevlflImp.Write(buf, offset, len);
            case CP21:
                return cp21xxDevlflImp.Write(buf, offset, len);
            case FTDI:
                return spftdiDevIfImp.Write(buf, offset, len);
            case PROLIFIC:
                return prolificDevIfImp.Write(buf, offset, len);
            case PORT:
                try {
                    out.write(buf, offset, len);
                    out.flush();
                }catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
                return 1;
            default:
                return -1;
        }
    }

    public void close() {
        switch (mType) {
            case CDCACM:
                cdcAcmDevlflImp.Close();
                break;
            case CP21:
                cp21xxDevlflImp.Close();
                break;
            case FTDI:
                spftdiDevIfImp.Close();
                break;
            case PROLIFIC:
                prolificDevIfImp.Close();
                break;
            case PORT:
                serialPort.close();
                break;
        }
    }
}
