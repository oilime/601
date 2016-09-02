package com.lanan.zigbeetransmission.serialport;

import android.content.Context;

interface IDeviceIf {
    /**
     * 打开设备
     */
    boolean Open(Context context);

    /**
     * 关闭设备
     */
    void Close();

    /**
     * 读数据
     *
     * @param buf 读数据的buffer
     * @param offset 读数据起始偏移量
     * @param len 最大可读数据长度
     */
    int Read(byte[] buf, int offset, int len);

    /**
     * 写数据
     *
     * @param buf 写数据的buffer
     * @param offset 写数据起始偏移量
     * @param len 写数据的长度
     * @return 是否成功的标识
     */
    int Write(byte[] buf, int offset, int len);
}
