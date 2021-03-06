package com.zamanak.bluetoothlowenergy.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.zamanak.bluetoothlowenergy.ble.callBacks.BlePeripheralCallback;
import com.zamanak.bluetoothlowenergy.utilities.DataConverter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * this class is responsible for checking if Peripheral mode is supported
 */
public class BlePeripheral {

    /**
     * Constants
     **/
    private static final String TAG = BlePeripheral.class.getSimpleName();

    // for connect and disconnect
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;


    /**
     * Peripheral and GATT Profile
     **/
    public static final String ADVERTISING_NAME = "MyDevice";
    public static final UUID SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
    private static final int CHARACTERISTIC_LENGTH = 20;

    // UUID of the descriptor used to enable and disable notifications
    // Client Characteristic Configuration Descriptor
    public static final UUID NOTIFY_DISCRIPTOR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");


    /**
     * Advertising settings
     **/
    // advertising mode can be one of:
    // - ADVERTISE_MODE_BALANCED,
    // - ADVERTISE_MODE_LOW_LATENCY,
    // - ADVERTISE_MODE_LOW_POWER
    int mAdvertisingMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;

    // transmission power mode can be one of:
    // - ADVERTISE_TX_POWER_HIGH
    // - ADVERTISE_TX_POWER_MEDIUM
    // - ADVERTISE_TX_POWER_LOW
    // - ADVERTISE_TX_POWER_ULTRA_LOW
    int mTransmissionPower = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

    /**
     * Callback Handlers
     **/
    public BlePeripheralCallback mBlePeripheralCallback;

    /**
     * Bluetooth Stuff
     **/
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mbBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mCharacteristic;
    private BluetoothGattCharacteristic mC;

    /**
     * Construct a new Peripheral
     *
     * //@param context the Application Context
     * @throws Exception Exception thrown if bluetooth is not supported
     */
    /*public BlePeripheral(final Context context) throws Exception {
        // make sure android device supports Bluetooth low energy
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new Exception("Bluetooth Not Supported");
        }
        // get a refrence to the Bluetooth Manager class, which allows us to talk to talk to ble radio
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Beware: this function doesn't work on some platforms ***
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            throw new Exception("Peripheral mode not supported");
        }
        mbBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        // use this method instead for better support
        if (mbBluetoothLeAdvertiser == null) {
            throw new Exception("Peripheral mode not supported");
        }
    }*/
    public BlePeripheral() {
    }

    /**
     * Connect to a Peripheral
     *
     * @param bluetoothDevice the Bluetooth Device
     * @param callback        The connection callback
     * @param context         The Activity that initialized the connection
     * @return a connection to the BluetoothGatt
     * @throws Exception if no device is given
     */
    public BluetoothGatt connect(BluetoothDevice bluetoothDevice, BluetoothGattCallback callback, final Context context) throws Exception {
        if (bluetoothDevice == null) {
            throw new Exception("No bluetooth device provided");
        }
        mBluetoothDevice = bluetoothDevice;
        mBluetoothGatt = bluetoothDevice.connectGatt(context, false, callback);
        return mBluetoothGatt;
    }

    /**
     * Disconnect from a Peripheral
     */
    public void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * A connection can only close after a successful disconnect.
     * Be sure to use the BluetoothGattCallback.onConnectionStateChanged event
     * to notify of a successful disconnect
     */
    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close(); // close connection to Peripheral
            mBluetoothGatt = null; // release from memory
        }
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }


    /**
     * Construct a new Peripheral
     *
     * @param context               The Application Context
     * @param blePeripheralCallback The callback handler
     *                              that interfaces with this Peripheral
     * @throws Exception Exception thrown if Bluetooth is not supported
     */
    public BlePeripheral(final Context context, BlePeripheralCallback blePeripheralCallback) throws Exception {
        mBlePeripheralCallback = blePeripheralCallback;
        // make sure Android device supports Bluetooth Low Energy
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new Exception("Bluetooth Not Supported");
        }
        // get a reference to the Bluetooth Manager class,
        // which allows us to talk to talk to the BLE radio
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mGattServer = bluetoothManager.openGattServer(context, mGattServerCallback);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Beware: this function doesn't work on some platforms
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            throw new Exception("Peripheral mode not supported");
        }
        mbBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        // Use this method instead for better support
        if (mbBluetoothLeAdvertiser == null) {
            throw new Exception("Peripheral mode not supported");
        }
    }


    /**
     * Get the system Bluetooth adapter
     *
     * @retrun BluetoothAdapter
     */
    public BluetoothAdapter getmBluetoothAdapter() {
        return mBluetoothAdapter;
    }


    /**
     * Set up the Advertising name and GATT profile
     */
    private void setupDevice() {
        mService = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mCharacteristic = new BluetoothGattCharacteristic(CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);

        mService.addCharacteristic(mCharacteristic);
        mGattServer.addService(mService);
        // write random characters to the
        // Read Characteristic every timerInterval_ms
        int timerInterval_ms = 1000;
        TimerTask updateReadCharacteristicTask = new TimerTask() {
            @Override
            public void run() {
                int stringLength = (int) (Math.random() % CHARACTERISTIC_LENGTH);
                String randomString = DataConverter.getRandomString(stringLength);
                try {
                    mCharacteristic.setValue(randomString);

                } catch (Exception e) {
                    Log.e(TAG, "Error converting String to byte array");
                }
            }
        };
        Timer randomStringTimer = new Timer();
        randomStringTimer.schedule(updateReadCharacteristicTask, 0, timerInterval_ms);
    }


    /**
     * Start Advertising
     *
     * @throws Exception Exception thrown
     *                   if Bluetooth Peripheral mode is not supported
     */
    public void startAdvertising() {
        // set the Advertised name
        mBluetoothAdapter.setName(ADVERTISING_NAME);

        // Build Advertise settings with transmission power
        // and advertise speed
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(mAdvertisingMode)
                .setTxPowerLevel(mTransmissionPower)
                .setConnectable(true)
                .build();
        AdvertiseData.Builder advertiseBuilder = new AdvertiseData.Builder();
        // set advertising name
        advertiseBuilder.setIncludeDeviceName(true);
        AdvertiseData advertiseData = advertiseBuilder.build();
        // begin advertising
        try {
            mbBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, mAdvertiseCallback);
        } catch (Exception e) {
            Log.e(TAG, "could not start advertising");
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stop advertising
     */
    public void stopAdvertising() {
        if (mbBluetoothLeAdvertiser != null) {
            mbBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mBlePeripheralCallback.onAdvertisingStopped();
        }
    }

    /**
     * Peripheral State Callbacks
     */
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.v(TAG, "Connected");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBlePeripheralCallback.onCentralConnected(device); //***************
                    stopAdvertising();
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBlePeripheralCallback.onCentralDisconnected(device); //************
                    try {
                        startAdvertising();
                    } catch (Exception e) {
                        Log.e(TAG, "error starting advertising");
                    }
                }
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            if (characteristic.getUuid() == mCharacteristic.getUuid()) {
                if (offset < CHARACTERISTIC_LENGTH) {
                    if (offset != 0) {
                        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, characteristic.getValue());
                    } else {
                        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                    }
                } else {
                    Log.d(
                            TAG,
                            "invalid offset when trying to read Characteristic"
                    );
                }
            }
        }
    };

    /**
     * Advertisement Success/Failure Callbacks
     * Handle callbacks from the Bluetooth Advertiser
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        /**
         * Advertising Started
         *
         * @param settingsInEffect The AdvertiseSettings that worked
         */
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            mBlePeripheralCallback.onAdvertisingStarted();
        }

        /**
         * Advertising Failed
         *
         * @param errorCode the reason for failure
         */
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            mBlePeripheralCallback.onAdvertisingFailed(errorCode);
        }
    };


    public boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        } catch (Exception localException) {
            Log.e(TAG, "An exception occurred while refreshing device");
        }
        return false;
    }

    // true if characteristic is writable
    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    // true if characteristic is readable
    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic pChar) {
        return ((pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    // true if characteristic can send notifications
    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic pChar) {
        return (pChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    /**
     * Request a data/value read from a Ble Characteristic
     * <p>
     * New in this chapter
     *
     * @param characteristic
     */
    public void readValueFromCharacteristic(
            final BluetoothGattCharacteristic characteristic) {
        // Reading a Characteristic requires both requesting the read and
        // handling the callback that is sent when the read is successful
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Subscribe or unsubscribe from Characteristic Notifications
     *
     * New in this chapter
     *
     * @param characteristic
     * @param enabled <b>true</b> for "subscribe" <b>false</b>
     *                for "unsubscribe"
     */
    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        // This is a 2-step process
        // Step 1: set the Characteristic Notification parameter locally
        mC = characteristic;
        mBluetoothGatt.setCharacteristicNotification(mC, enabled);
        // Step 2: Write a descriptor to the Bluetooth GATT
        // A delay is needed between setCharacteristicNotification
        // and setValue.
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try{
                //BluetoothGattDescriptor descriptor = characteristic.getDescriptor(NOTIFY_DISCRIPTOR_UUID);
                BluetoothGattDescriptor mDescriptor = null;
                for (BluetoothGattDescriptor descriptor:mC.getDescriptors()){
                    if (mC.getUuid().equals(UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"))){
                        mDescriptor = descriptor;
                    }
                    Log.e(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
                }

                if (mDescriptor != null) {
                    if (enabled) {
                        mDescriptor.setValue(
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    } else {
                        mDescriptor.setValue(
                                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        );
                    }
                }
                mBluetoothGatt.writeDescriptor(mDescriptor);
            }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }, 10);

    }

    public void setCharacteristicNotification2(BluetoothGattDescriptor descriptor , final boolean enable){
        if (descriptor != null){
            Log.e(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
        }
    }

}
