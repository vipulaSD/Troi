package org.ahlab.troi.service;

import android.util.Log;

import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.google.common.collect.EvictingQueue;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class EmpaticaListener implements EmpaDataDelegate {

	private static EmpaticaListener instance;
	private final String TAG = "###EMPATICA_LISTENER###";
	private EvictingQueue<Double> bvpQueue;
	private EvictingQueue<Double> edaQueue;
	private EvictingQueue<Double> tempQueue;
	private EvictingQueue<Double> accelXQueue;
	private EvictingQueue<Double> accelYQueue;
	private EvictingQueue<Double> accelZQueue;

	private long lastUpdateTs;

	private EmpaticaListener() {
		initQueues();
	}

	private void initQueues() {
		edaQueue = EvictingQueue.create(16);
		tempQueue = EvictingQueue.create(16);
		bvpQueue = EvictingQueue.create(256);
		accelXQueue = EvictingQueue.create(128);
		accelYQueue = EvictingQueue.create(128);
		accelZQueue = EvictingQueue.create(128);
	}

	public static EmpaticaListener getInstance() {
		if (instance == null) {
			instance = new EmpaticaListener();
		}
		return instance;
	}

	public long getLastUpdateTs() {
		return lastUpdateTs;
	}

	public boolean isDataReady() {
		return (accelXQueue.remainingCapacity() + edaQueue.remainingCapacity() + bvpQueue.remainingCapacity() + tempQueue.remainingCapacity() == 0);
	}

	public float[] getAccSnapshot() {

		List<Double> acc = new ArrayList<>(accelXQueue);
		acc.addAll(accelYQueue);
		acc.addAll(accelZQueue);

		float[] ret = new float[384];

		double[] accArray = ArrayUtils.toPrimitive(acc.toArray(new Double[0]), 0.0);
		accArray = StatUtils.normalize(accArray);

		for (int i = 0; i < 384; i++) {
			ret[i] = (float) accArray[i];
		}
		return ret;

	}

	public float[] getEDASnapshot() {
		float[] ret = new float[16];
		List<Double> eda = new ArrayList<>(edaQueue);
		double[] edaD = ArrayUtils.toPrimitive(eda.toArray(new Double[0]), 0.0);
		edaD = StatUtils.normalize(edaD);
		for (int i = 0; i < 16; i++) {
			ret[i] = (float) edaD[i];
		}
		return ret;
	}

	public float[] getBVPSnapshot() {
		float[] ret = new float[256];
		List<Double> bvp = new ArrayList<>(bvpQueue);
		double[] bvpD = ArrayUtils.toPrimitive(bvp.toArray(new Double[0]), 0.0);
		bvpD = StatUtils.normalize(bvpD);
		for (int i = 0; i < 256; i++) {
			ret[i] = (float) bvpD[i];
		}
		return ret;
	}

	public float[] getTempSnapshot() {
		float[] ret = new float[16];
		double[] tmpD = ArrayUtils.toPrimitive(tempQueue.toArray(new Double[0]), 0.0);
		tmpD = StatUtils.normalize(tmpD);
		for (int i = 0; i < 16; i++) {
			ret[i] = (float) tmpD[i];
		}
		return ret;
	}

	@Override
	public void didReceiveGSR(float gsr, double timestamp) {
//		Log.i(TAG, "didReceiveGSR: " + gsr);
		lastUpdateTs = System.currentTimeMillis();
		edaQueue.add((double) gsr);
	}

	@Override
	public void didReceiveBVP(float bvp, double timestamp) {
//		Log.i(TAG, "didReceiveBVP: " + bvp);
		lastUpdateTs = System.currentTimeMillis();
		bvpQueue.add((double) bvp);
	}

	@Override
	public void didReceiveIBI(float ibi, double timestamp) {
	}

	@Override
	public void didReceiveTemperature(float t, double timestamp) {
//		Log.i(TAG, "didReceiveTemperature: " + t);
		lastUpdateTs = System.currentTimeMillis();
		tempQueue.add((double) t);
	}

	@Override
	public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
//		Log.i(TAG, "didReceiveAcceleration: " + x + ", " + y + ", " + z);
		lastUpdateTs = System.currentTimeMillis();
		accelXQueue.add((double) x);
		accelYQueue.add((double) y);
		accelZQueue.add((double) z);
	}

	@Override
	public void didReceiveBatteryLevel(float level, double timestamp) {

	}

	@Override
	public void didReceiveTag(double timestamp) {

	}

	// it looks like error in empatica implementation. it expext this method here, but defined in EmpaStatusDelegate
	public void didUpdateOnWristStatus(int status) {
		Log.i(TAG, "didUpdateOnWristStatus: ");
	}

}
