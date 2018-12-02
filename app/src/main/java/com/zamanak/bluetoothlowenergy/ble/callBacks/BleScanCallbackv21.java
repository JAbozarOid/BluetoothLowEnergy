package com.zamanak.bluetoothlowenergy.ble.callBacks;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import java.util.List;

public abstract class BleScanCallbackv21 extends ScanCallback {

    /**
     * New Perpheral found
     *
     * @param callbackType int: Determines how this callback was triggered
     * @param result a Bluetooth Low Energy Scan Result
     */
    @Override
    public abstract void onScanResult(int callbackType, ScanResult result);

    /**
     * New Perpherals found
     *
     * @param results List: List of scan results that are previously scanned
     */
    @Override
    public abstract void onBatchScanResults(List<ScanResult> results);

    /**
     * Problem initializing the scan . see the error code for reason
     *
     * @param errorCode int: Error Code(one of SCAN_FAILED_*)
     *                  for scan failure
     */
    @Override
    public abstract void onScanFailed(int errorCode);

    /**
     * Scan has completed
     */
    public abstract void onScanComplete();
}
