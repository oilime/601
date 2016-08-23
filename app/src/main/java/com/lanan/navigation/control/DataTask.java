package com.lanan.navigation.control;

import android.util.Log;

import com.lanan.navigation.algorithm.NativeInterface;
import com.lanan.navigation.model.Direction;
import com.lanan.navigation.model.TriRegion;
import com.lanan.zigbeetransmission.dataclass.LocationInfo;
import com.lanan.zigbeetransmission.dataclass.NavigationInfo;

import java.util.ArrayList;

public class DataTask extends Thread {
	
	private LocationInfo mLocation;
	private TriRegion triRegion;
	private ArrayList<LocationInfo> destination;
	private int currentL = 0;
	private boolean initFlag = true;
    private boolean interrupt = false;

    private NavigationInfo info;

    public ArrayList<LocationInfo> getDestination() {
		return destination;
	}
    
	public void setDestination(ArrayList<LocationInfo> destination) {
		this.destination = destination;
	}
    
	public boolean isInterrupt() {
		return interrupt;
	}

	public void setInterrupt(boolean interrupt) {
		this.interrupt = interrupt;
	}
    
	private   synchronized LocationInfo getmLocation() {
		return mLocation;
	}
	public  synchronized  void setmLocation(LocationInfo mLocation) {
		this.mLocation = mLocation;
	}
    
	@Override
	public void run() {
	    LocationInfo location ;
        Direction direction;
		while(!interrupt){
	        if(currentL == destination.size()){
			    break;
		    }

            while((location = getmLocation()) == null){
                Log.e("sqh","location is null");
                try {
                    Thread.sleep(500);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(initFlag){
                triRegion = null;
                triRegion = NativeInterface.gen(location.getLng(), location.getLat(),
                        destination.get(currentL).getLng(), destination.get(currentL).getLat());
                while(triRegion == null){
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    triRegion = NativeInterface.gen(location.getLng(), location.getLat(),
                            destination.get(currentL).getLng(), destination.get(currentL).getLat());
                }
                initFlag = false;
            }
            direction = NativeInterface.nav(location.getLng(), location.getLat(),
                    destination.get(currentL).getLng(), destination.get(currentL).getLat());
            if(direction.getDis() <=  NativeInterface.LIMIT_REGEN){
                currentL++;
                initFlag = true;
                setInfo(new NavigationInfo(direction.getDis(), direction.getAngle(), currentL + 1,
                        true, false));
                continue;
            }
            if(!NativeInterface.rck(location.getLng(), location.getLat(), triRegion.getmA().getLng(), triRegion.getmA().getLat(),
                    triRegion.getmB().getLng(), triRegion.getmB().getLat(),
                    triRegion.getmO().getLng(), triRegion.getmO().getLat())){
                initFlag = true;
                setInfo(new NavigationInfo(direction.getDis(), direction.getAngle(), currentL + 1,
                        false, true));
                continue;
            }
            setInfo(new NavigationInfo(direction.getDis(), direction.getAngle(), currentL + 1,
                    false, false));
        }
        if (currentL == destination.size()){
            Log.d("Emilio", "arrived");
        }
    }

    public NavigationInfo getInfo() {
        synchronized (this.getClass()){
            return info;
        }

    }
    private void setInfo (NavigationInfo ofni) {
        synchronized (this.getClass()){
            this.info = ofni;
        }
    }
}
