package com.zamanak.bluetoothlowenergy.ble.callBacks;

import android.bluetooth.BluetoothDevice;

public abstract class BlePeripheralCallback {
    /**
     * Advertising Started
     */
    public abstract void onAdvertisingStarted();

    /**
     * Advertising Could not Start
     */
    public abstract void onAdvertisingFailed(int errorCode);

    /**
     * Advertising Stopped
     */
    public abstract void onAdvertisingStopped();


    /**
     * Central Connected
     *
     * @param bluetoothDevice the BluetoothDevice
     *                        representing the connected Central
     */
    public abstract void onCentralConnected(final BluetoothDevice bluetoothDevice);

    /**
     * Central Disconnected
     *
     * @param bluetoothDevice the BluetoothDevice
     *                        representing the disconnected Central
     */
    public abstract void onCentralDisconnected(final BluetoothDevice bluetoothDevice);

}
