package com.zamanak.bluetoothlowenergy.models;

import android.bluetooth.BluetoothDevice;

public class BlePeripheralListItem {

    private int mItemId;
    private int mRssi;
    private BluetoothDevice mBluetoothDevice;

    public BlePeripheralListItem(BluetoothDevice mBluetoothDevice) {
        this.mBluetoothDevice = mBluetoothDevice;
    }

    public void setmItemId(int mItemId) {
        this.mItemId = mItemId;
    }

    public void setmRssi(int mRssi) {
        this.mRssi = mRssi;
    }

    public int getmItemId() {
        return mItemId;
    }

    public String getAdvertiseName(){
        return mBluetoothDevice.getName();
    }

    public String getMacAddress(){
        return mBluetoothDevice.getAddress();

    }

    public int getmRssi(){
        return mRssi;
    }

    public BluetoothDevice getDevice(){
        return mBluetoothDevice;
    }
}
