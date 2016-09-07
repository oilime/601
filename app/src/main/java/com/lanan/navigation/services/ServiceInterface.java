package com.lanan.navigation.services;

import com.lanan.zigbeetransmission.dataclass.LocationInfo;
import com.lanan.zigbeetransmission.dataclass.NavigationInfo;

import java.util.ArrayList;

public interface ServiceInterface {

    /**
     * 开始导航
     */
    void startNav(ArrayList<LocationInfo> infos);

    /**
     * 停止导航
     */
    void stopNav();

    /**
     * 设置语音播报速率
     *
     * @param voiceRate 速率(s)
     */
    void setVoiceRate(int voiceRate);

    /**
     * 获取导航信息
     *
     * @return 基于当前位置和目标路径点的导航信息
     */
    NavigationInfo getNavInfo();

    /**
     * 设置当前位置
     *
     * @param info 当前位置的经纬度
     */
    void setLocInfo(LocationInfo info);

    /**
     * 略过当前路径点
     */
    void nextDest();

    /**
     * 导航过程的标识位
     *
     * @return 导航是否停止
     */
    boolean isNavStop();

    /**
     * TextToSpeech封装函数
     *
     * @param text 播报内容
     * @param mode 播放方式(FLUSH/ADD)
     */
    void mSpeak(String text, int mode);

    /**
     * 停止所有服务
     */
    void close();
}
