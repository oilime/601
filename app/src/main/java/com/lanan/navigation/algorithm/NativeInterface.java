package com.lanan.navigation.algorithm;

import com.lanan.navigation.model.Direction;
import com.lanan.navigation.model.TriRegion;

public class NativeInterface {
	static{
		System.loadLibrary("navnostar");
	}
	public static final  double LIMIT_REGEN = 10.0;
	public native static TriRegion gen(double Cj, double Cw, double Dj, double Dw);
    public native static boolean rch(double Cj,double Cw,double Dj,double Dw);
    public native static boolean rck(double Cj,double Cw,double Aj,double Aw ,double Bj,double Bw,double Oj,double Ow);
    public native static Direction nav(double Cj,double Cw,double Dj,double Dw);
}
