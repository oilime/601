package com.lanan.navigation.control;

import android.util.Log;

import com.lanan.navigation.algorithm.NativeInterface;
import com.lanan.navigation.model.Direction;
import com.lanan.navigation.model.TriRegon;
import com.lanan.zigbeetransmission.dataclass.LocationInfo;
import com.lanan.zigbeetransmission.dataclass.NavigationInfo;

import java.util.ArrayList;

public class DataTask extends Thread {
	
	private LocationInfo mLocation;
	private TriRegon triregon;
	private ArrayList<LocationInfo> destination;
	private int currentL = 0;
	private boolean initflag = true;
    private boolean interrupt = false;

    NavigationInfo info;

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
    
	public  synchronized LocationInfo getmLocation() {
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
//            location = new LocationInfo(112.951909, 28.179999);
            if(initflag){
                triregon = null;
                triregon = NativeInterface.gen(location.getLng(), location.getLat(),
                        destination.get(currentL).getLng(), destination.get(currentL).getLat());
                while(triregon == null){
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    triregon = NativeInterface.gen(location.getLng(), location.getLat(),
                            destination.get(currentL).getLng(), destination.get(currentL).getLat());
                }
                initflag = false;
            }
            direction = NativeInterface.nav(location.getLng(), location.getLat(),
                    destination.get(currentL).getLng(), destination.get(currentL).getLat());
            if(direction.getDis() <=  NativeInterface.LIMIT_REGEN){
                currentL++;
                initflag = true;
                setInfo(new NavigationInfo(direction.getDis(), direction.getAngle(), currentL,
                        true, false));
                continue;
            }
            if(NativeInterface.rck(location.getLng(), location.getLat(), triregon.getmA().getLng(), triregon.getmA().getLat(),
                    triregon.getmB().getLng(), triregon.getmB().getLat(),
                    triregon.getmO().getLng(), triregon.getmO().getLat())){
                initflag = true;
                setInfo(new NavigationInfo(direction.getDis(), direction.getAngle(), currentL,
                        false, true));
                continue;
            }
            setInfo(new NavigationInfo(direction.getDis(), direction.getAngle(), currentL,
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
    public void setInfo (NavigationInfo ofni) {
        synchronized (this.getClass()){
            this.info = ofni;
        }
    }
}
