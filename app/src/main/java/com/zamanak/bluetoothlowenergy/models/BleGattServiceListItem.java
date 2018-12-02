package com.zamanak.bluetoothlowenergy.models;

import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class BleGattServiceListItem {

    private final int mItemId;
    private final BluetoothGattService mService;

    public BleGattServiceListItem(BluetoothGattService gattService, int serviceItemID) {
        mItemId = serviceItemID;
        mService = gattService;
    }

    public int getItemId() { return mItemId; }
    public UUID getUuid() { return mService.getUuid(); }
    public int getType() { return mService.getType(); }
    public BluetoothGattService getService() { return mService; }
}
