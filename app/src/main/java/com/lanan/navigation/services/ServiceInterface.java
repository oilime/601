package com.lanan.navigation.services;

import com.lanan.zigbeetransmission.dataclass.LocationInfo;
import com.lanan.zigbeetransmission.dataclass.NavigationInfo;

import java.util.ArrayList;

public interface ServiceInterface {

    void startNav(ArrayList<LocationInfo> infos);

    void stopNav();

    void setVoiceRate(int voiceRate);

    NavigationInfo getNavInfo();

    void setLocInfo(LocationInfo info);

    boolean isNavStop();

    void mSpeak(String text, int mode);

    void close();
}
