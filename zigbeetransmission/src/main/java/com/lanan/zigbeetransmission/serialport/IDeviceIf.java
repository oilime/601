package com.lanan.zigbeetransmission.serialport;

import android.content.Context;

interface IDeviceIf {
    /**
     * 打开设备
     */
    void Open(Context context);

    /**
     * 关闭设备
     */
    void Close();

    /**
     * 读数据
     *
     * @param buf
     * @param offset
     * @param len
     */
    int Read(byte[] buf, int offset, int len);

    /**
     * 写数据
     *
     * @param buf
     * @param offset
     * @param len
     * @return
     */
    @SuppressWarnings("SameReturnValue")
    int Write(byte[] buf, int offset, int len);
}
