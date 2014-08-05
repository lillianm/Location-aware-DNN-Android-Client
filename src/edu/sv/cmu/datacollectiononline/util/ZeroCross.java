package edu.sv.cmu.datacollectiononline.util;

import java.util.ArrayList;

import edu.sv.cmu.datacollectiononline.thread.AudioThread;

public class ZeroCross {
	
	public int zcCount;
	public int zcCountSum;
	public int zcCountFrameSum;
	public int runningSum;
	public int threshold;
	public int zcCountSize;
	public ArrayList<Short> dcQueue;
	public ArrayList<Integer> counterQueue;
	
	public ZeroCross(int threshold, int zcCountSize){
		zcCount = 0;
		runningSum = 0;
		this.threshold = threshold;
		this.zcCountSize = zcCountSize;
		dcQueue = new ArrayList<Short>();
		counterQueue = new ArrayList<Integer>();
	}
	public void reset(){
		zcCount = 0;
		runningSum = 0;
		dcQueue = new ArrayList<Short>();
		counterQueue = new ArrayList<Integer>();
	}
	public void update(short[] buffer, int packet_size){
		/* Calculate threshold */
		for(int i = 0; i < buffer.length; i++) {
			if(dcQueue.size() < packet_size) {
				dcQueue.add(buffer[i]);

				runningSum += buffer[i];
				
			} else {
				short sub = dcQueue.remove(0);
				dcQueue.add(buffer[i]);

				runningSum = runningSum - sub + buffer[i];
			}
		}

		threshold = (short) (runningSum / Math.min(packet_size,dcQueue.size()));

		/* zero crossing count in one frame */
		zcCount = 0;
		int sgn_old = (buffer[0] > threshold) ? 1 : -1;
		for (int i=1; i < AudioThread.BUFFER_SIZE; ++i) {
			int sgn =  (buffer[i] > threshold) ? 1 : -1;
			if ((sgn_old - sgn) != 0) {
				//Log.v(TAG, "ZERO CROSSED");
				zcCount++;
			}
			sgn_old = sgn;				
		}

		/* the number of 4 frames that across the ZERO_THRESH count*/
		if(counterQueue.size() < zcCountSize) {
			counterQueue.add(zcCount);
			zcCountSum+=zcCount;
			int add = (zcCount > AudioThread.ZERO_THRESH) ? 1 : 0;
			zcCountFrameSum+=add;
		} else {
			counterQueue.add(zcCount);
			int rCount  = counterQueue.remove(0);
			zcCountSum = zcCountSum-rCount+zcCount;
			int sub = (rCount > AudioThread.ZERO_THRESH) ? 1 : 0;
			int add = (zcCount > AudioThread.ZERO_THRESH) ? 1 : 0;
			zcCountFrameSum = zcCountFrameSum - sub + add;
		}
	}
}
