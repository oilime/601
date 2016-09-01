package com.lanan.navigation.services;

import com.lanan.zigbeetransmission.dataclass.LocationInfo;
import com.lanan.zigbeetransmission.dataclass.NavigationInfo;

import java.util.ArrayList;

public interface ServiceInterface {

    void startNav(ArrayList<LocationInfo> infos);

    void stopNav();

    void setVoiceRate(int voiceRate);

    void setNavRate(int navRate);

    NavigationInfo getNavInfo();
}
