package com.zamanak.bluetoothlowenergy.models;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * display UUID and the permissions of a characteristic
 */
public class BleGattCharacteristicListItem {

    private int mItemId;
    private BluetoothGattCharacteristic mCharacteristic;

    public BleGattCharacteristicListItem(BluetoothGattCharacteristic characteristic, int itemId) {
        mCharacteristic = characteristic;
        mItemId = itemId;
    }

    public int getItemId() { return mItemId; }
    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }

}
