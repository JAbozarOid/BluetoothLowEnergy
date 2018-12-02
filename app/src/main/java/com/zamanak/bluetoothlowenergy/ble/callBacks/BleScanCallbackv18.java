package com.zamanak.bluetoothlowenergy.ble.callBacks;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public abstract class BleScanCallbackv18 implements BluetoothAdapter.LeScanCallback {

    /**
     * new Peripheral found
     *
     * @param bluetoothDevice the peripheral device
     * @param rssi the peripheral's RSSI indicating how strong the radio signal is
     * @param scanRecord other information about the scan result
     */
    @Override
    public abstract void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord);
        /**
         * BLE Scan complete
         */
    public abstract void onScanComplete();

}
