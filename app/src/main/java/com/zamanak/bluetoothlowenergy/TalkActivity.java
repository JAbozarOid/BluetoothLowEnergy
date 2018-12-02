package com.zamanak.bluetoothlowenergy;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import com.zamanak.bluetoothlowenergy.ble.BleCommManager;
import com.zamanak.bluetoothlowenergy.ble.BlePeripheral;
import com.zamanak.bluetoothlowenergy.ble.callBacks.BleScanCallbackv18;
import com.zamanak.bluetoothlowenergy.ble.callBacks.BleScanCallbackv21;
import com.zamanak.bluetoothlowenergy.utilities.DataConverter;
import java.util.List;
import java.util.UUID;

public class TalkActivity extends AppCompatActivity {

    /**
     * Constants
     **/
    private static final String TAG = TalkActivity.class.getSimpleName();
    private static final String CHARACTER_ENCODING = "ASCII";
    public static final String PERIPHERAL_NAME = "com.example.com.exampleble.PERIPHERAL_NAME";
    public static final String PERIPHERAL_MAC_ADDRESS_KEY = "com.example.com.exampleble.PERIPHERAL_MAC_ADDRESS";
    public static final String CHARACTERISTIC_KEY = "com.example.com.exampleble.CHARACTERISTIC_UUID";
    public static final String SERVICE_KEY = "com.example.com.exampleble.SERVICE_UUID";

    /**
     * Bluetooth Stuff
     **/
    private BleCommManager mBleCommManager;
    private BlePeripheral mBlePeripheral;
    private BluetoothGattCharacteristic mCharacteristic;

    /**
     * Functional stuff
     **/
    private String mPeripheralMacAddress;
    private String mBlePeripheralName;
    private boolean mScanningActive = false;
    private UUID mCharacteristicUUID, mServiceUUID;

    /**
     * UI Stuff
     **/
    private MenuItem mProgressSpinner;
    private TextView mResponseText;
    private TextView mPeripheralAdvertiseNameTV;
    private TextView mPeripheralAddressTV;
    private TextView mServiceUUIDTV;
    private Button mReadButton;
    private CheckBox mSubscribeCheckbox;

    private BluetoothGattDescriptor mDescriptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // grab a Characteristic from the savedInstanceState,
        // passed when a user clicked on a Characteristic in the Connect Activity
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mBlePeripheralName = extras.getString("PERIPHERAL_NAME");
                mPeripheralMacAddress = extras.getString("PERIPHERAL_MAC_ADDRESS_KEY");
                mCharacteristicUUID = UUID.fromString(extras.getString("CHARACTERISTIC_KEY"));
                mServiceUUID = UUID.fromString(extras.getString("SERVICE_KEY"));
            }
        } else {
            mBlePeripheralName = savedInstanceState.getString("PERIPHERAL_NAME");
            mPeripheralMacAddress = savedInstanceState.getString("PERIPHERAL_MAC_ADDRESS_KEY");
            mCharacteristicUUID = UUID.fromString(savedInstanceState.getString("CHARACTERISTIC_KEY"));
            mServiceUUID = UUID.fromString(savedInstanceState.getString("SERVICE_KEY"));
        }
        Log.v(TAG, "Incoming mac address: " + mPeripheralMacAddress);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_talk);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mBlePeripheral = new BlePeripheral();
        loadUI();
    }

    /**
     * Load UI components
     */
    public void loadUI() {
        mResponseText = (TextView) findViewById(R.id.response_text);
        mPeripheralAdvertiseNameTV = (TextView) findViewById(R.id.advertise_name);
        mPeripheralAddressTV = (TextView) findViewById(R.id.mac_address);
        mServiceUUIDTV = (TextView) findViewById(R.id.service_uuid);
        mReadButton = (Button) findViewById(R.id.read_button);
        mPeripheralAdvertiseNameTV.setText(R.string.connecting);
        Log.v(TAG, "Incoming Service UUID: " + mServiceUUID.toString());
        Log.v(TAG, "Incoming Characteristic UUID: " + mCharacteristicUUID.toString());
        mServiceUUIDTV.setText(mCharacteristicUUID.toString());
        mReadButton.setVisibility(View.GONE);
        mResponseText.setVisibility(View.GONE);
        mSubscribeCheckbox = (CheckBox) findViewById(R.id.subscribe_checkbox);
        mSubscribeCheckbox.setVisibility(View.GONE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu;
        // this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_talk, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mProgressSpinner = menu.findItem(R.id.scan_progress_item);
        initializeBluetooth();
        //connect(); // when working with non-Android peripherals
        startScan(); // when working with Android Peripherals
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_disconnect:
                // User chose the "Stop" item
                disconnect();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    public void initializeBluetooth() {
        try {
            mBleCommManager = new BleCommManager(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }
    }


    public void connect() {
        // grab the Peripheral Device address and attempt to connect
        BluetoothDevice bluetoothDevice = mBleCommManager.getmBluetoothAdapter().getRemoteDevice(mPeripheralMacAddress);
        mProgressSpinner.setVisible(true);
        try {
            mBlePeripheral.connect(bluetoothDevice, mGattCallback, getApplicationContext());
        } catch (Exception e) {
            mProgressSpinner.setVisible(false);
            Log.e(TAG, "Error connecting to peripheral");
        }
    }

    /**
     * Peripheral has connected.  Update UI
     */
    public void onBleConnected() {
        BluetoothDevice bluetoothDevice = mBlePeripheral.getBluetoothDevice();
        mPeripheralAdvertiseNameTV.setText(bluetoothDevice.getName());
        mPeripheralAddressTV.setText(bluetoothDevice.getAddress());
        mProgressSpinner.setVisible(false);
    }


    /**
     * Charactersitic supports reads.  Update UI
     */
    public void onCharacteristicReadable() {
        Log.v(TAG, "Characteristic is readable");
        mReadButton.setVisibility(View.VISIBLE);
        mResponseText.setVisibility(View.VISIBLE);
        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Read button clicked");
                mBlePeripheral.readValueFromCharacteristic(mCharacteristic);

            }
        });
    }

    /**
     * Characteristic supports notifications. Update UI
     *
     * New in this chapter
     */
    public void onCharacteristicNotifiable() {
        mSubscribeCheckbox.setVisibility(View.VISIBLE);
        mSubscribeCheckbox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton buttonView, boolean isChecked) {
                        // subscribe to notifications from this channel
                        mBlePeripheral.setCharacteristicNotification(mCharacteristic, isChecked);
                        //mBlePeripheral.setCharacteristicNotification2(descriptor , isChecked);
                    }
                }
        );
    }



    /**
     * Update TextView when a new message is read from a Charactersitic
     * Also scroll to the bottom so that new messages are always in view
     *
     * @param message the Characterstic value to display in the UI as text
     */
    public void updateResponseText(String message) {
        mResponseText.append(message + "\n");
        /*final int scrollAmount = mResponseText.getLayout().getLineTop(mResponseText.getLineCount()) - mResponseText.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount < 0) {
            mResponseText.scrollTo(0, scrollAmount);
        } else {
            mResponseText.scrollTo(0, 0);
        }*/
        mResponseText.setText(message);
        Log.d(TAG, "updateResponseText: " + message);
    }

    /**
     * BluetoothGattCallback handles connections, state changes,
     * reads, writes, and GATT profile listings to a Peripheral
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /**
         * Charactersitic successfully read
         *
         * @param gatt connection to GATT
         * @param characteristic The charactersitic that was read
         * @param status the status of the operation
         */
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            // characteristic was read.  Convert the data to something usable
            // on Android and display it in the UI
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final byte[] data = characteristic.getValue();
                String m = "";
                try {
                    m = new String(data, CHARACTER_ENCODING);
                } catch (Exception e) {
                    Log.e(TAG, "Could not convert message byte array to String");
                }
                final String message = m;
                Log.v(TAG, "Characteristic read hex value: " + DataConverter.bytesToHex(data));
                Log.v(TAG, "Characteristic read int value: " + DataConverter.bytesToInt(data));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateResponseText(message);
                    }
                });
            }
        }

        /**
         * Characteristic was written successfully.  update the UI
         *
         * @param gatt Connection to the GATT
         * @param characteristic The Characteristic that was written
         * @param status write status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        }

        /**
         * Charactersitic value changed.  Read new value.
         * @param gatt Connection to the GATT
         * @param characteristic The Characterstic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            //mBlePeripheral.readValueFromCharacteristic(characteristic);

                final byte[] data = characteristic.getValue();
                String m = "";
                try {
                    m = new String(data, CHARACTER_ENCODING);
                } catch (Exception e) {
                    Log.e(TAG, "Could not convert message byte array to String");
                }
                final String message = m;
                Log.v(TAG, "Characteristic read hex value: " + DataConverter.bytesToHex(data));
                Log.v(TAG, "Characteristic read int value: " + DataConverter.bytesToInt(data));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateResponseText(String.valueOf(DataConverter.bytesToHex(data)));
                    }
                });

        }

        /**
         * Peripheral connected or disconnected.  Update UI
         * @param bluetoothGatt Connection to GATT
         * @param status status of the operation
         * @param newState new connection state
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v(TAG, "Connected to peripheral");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBleConnected();
                    }
                });
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v(TAG, "Disconnected from peripheral");
                disconnect();
                mBlePeripheral.close();
            }
        }

        /**
         * GATT Profile discovered.  Update UI
         * @param bluetoothGatt connection to GATT
         * @param status status of operation
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {

            // if services were discovered,
            // then let's iterate through them and display them on screen
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // connect to a specific service

                BluetoothGattService gattService = bluetoothGatt.getService(mServiceUUID);
                // while we are here, let's ask for this service's characteristics:
                List<BluetoothGattCharacteristic> characteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    if (characteristic != null) {
                        Log.v(TAG, "found characteristic: " + characteristic.getUuid().toString());
                    }
                }

                // determine the read/write/notify permissions
                // of the Characterstic
                Log.v(TAG, "desired service is: " + mServiceUUID.toString());
                Log.v(TAG, "desired charactersitic is: " + mCharacteristicUUID.toString());
                Log.v(TAG, "this service: " + bluetoothGatt.getService(mServiceUUID).getUuid().toString());
                Log.v(TAG, "this characteristic: " + bluetoothGatt.getService(mServiceUUID).getCharacteristic(mCharacteristicUUID).getUuid().toString());

                mCharacteristic = bluetoothGatt.getService(mServiceUUID).getCharacteristic(mCharacteristicUUID);
                //mDescriptor = mCharacteristic.getDescriptor(UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"));

                for (BluetoothGattDescriptor descriptor:mCharacteristic.getDescriptors()){
                    if (mCharacteristic.getUuid().equals(UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"))){
                        mDescriptor = descriptor;
                    }
                    Log.e(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
                }
                if (BlePeripheral.isCharacteristicReadable(mCharacteristic)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onCharacteristicReadable();
                        }
                    });
                }
                if (BlePeripheral.isCharacteristicNotifiable(mCharacteristic)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onCharacteristicNotifiable();
                        }
                    });
                }

            } else {
                Log.e(TAG, "Problem occured while discovering “ + GATT services from this peripheral");
            }
        }


    };

    /**
     * Disconnect
     */
    private void disconnect() {
        // close the Activity when disconnecting.
        // No actions can be done without a connection
        mBlePeripheral.disconnect();
        finish();
    }

    /**
     * Android-based Peripherals change MAC addresses between connections
     * As a result we can't simply connect to the last known MAC address
     * Instead, we must scan for matching adertised names again
     */

    /**
     * Start scanning for Peripherals
     */
    public void startScan() {
        try {
            mScanningActive = true;
            mBleCommManager.scanForPeripherals(mBleScanCallbackv18, mScanCallbackv21);
        } catch (Exception e) {
            Log.e(TAG, "Could not open Ble Device Scanner");
        }
    }

    /**
     * Stop scanning for Peripherals
     */
    public void stopScan() {
        mBleCommManager.stopScanning(mBleScanCallbackv18, mScanCallbackv21);
    }

    /**
     * Event trigger when BLE Scanning has stopped
     */
    public void onBleScanStopped() {
        // update UI compenents to reflect that a BLE scan has stopped
        // it's possible that this method
        // will be called before the menu has been instantiated
        // Check to see if menu items are initialized, or Activity will crash
        mScanningActive = false;
    }

    /**
     * Event trigger when new Peripheral is discovered
     */
    public void onBlePeripheralDiscovered(final BluetoothDevice bluetoothDevice, int rssi) {
        Log.v(TAG, "Found " + mBlePeripheralName + ": " + bluetoothDevice.getName() + ", " + bluetoothDevice.getAddress());
        // only add the peripheral if
        // - it has a name, on
        // - doesn't already exist in our list, or
        // - is transmitting at a higher power (is closer)
        // than an existing peripheral
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bluetoothDevice.getName()!=null && bluetoothDevice.getName().equals(mBlePeripheralName)) {
                    Log.v(TAG, "desired device found.  connecting");
                    stopScan();
                    mPeripheralMacAddress = bluetoothDevice.getAddress();
                    connect();
                }
            }
        });

    }


    /**
     * Use this callback for Android API 21 (Lollipop) or greater
     */
    private final BleScanCallbackv21 mScanCallbackv21 = new BleScanCallbackv21() {
        /**
         * New Peripheral discovered
         *
         * @param callbackType int: Determines how this callback
         *        was triggered. Could be one of CALLBACK_TYPE_ALL_MATCHES,
         *        CALLBACK_TYPE_FIRST_MATCH or CALLBACK_TYPE_MATCH_LOST
         * @param result a Bluetooth Low Energy Scan Result,
         *         containing the Bluetooth Device, RSSI,
         *         and other information
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice bluetoothDevice = result.getDevice();
            int rssi = result.getRssi();
            onBlePeripheralDiscovered(bluetoothDevice, rssi);
        }

        /**
         * Several peripherals discovered when scanning in low power mode
         *
         * @param results List: List of previously scanned results.
         */
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                BluetoothDevice bluetoothDevice = result.getDevice();
                int rssi = result.getRssi();
                onBlePeripheralDiscovered(bluetoothDevice, rssi);
            }
        }

        /**
         * Scan failed to initialize
         *
         * @param errorCode int: Error code (one of SCAN_FAILED_*) for scan failure.
         */
        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "Fails to start scan as BLE scan with the “ + same settings is already started by the app. ");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.e(TAG, "Fails to start scan as app cannot be registered.");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "Fails to start; this feature is not supported.");
                    break;
                default: // SCAN_FAILED_INTERNAL_ERROR
                    Log.e(TAG, "Fails to start scan due an internal error");
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleScanStopped();
                }
            });
        }

        /**
         * Scan completed
         */
        public void onScanComplete() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleScanStopped();
                }
            });
        }
    };

    /**
     * Use this callback for Android API 18, 19, and 20 (before Lollipop)
     */
    public final BleScanCallbackv18 mBleScanCallbackv18 = new BleScanCallbackv18() {
        /**
         * New Peripheral discovered
         * @param bluetoothDevice The Peripheral Device
         * @param rssi The Peripheral's RSSI indicating
         *             how strong the radio signal is
         * @param scanRecord Other information about the scan result
         */
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
            onBlePeripheralDiscovered(bluetoothDevice, rssi);
        }

        /**
         * Scan completed
         */
        @Override
        public void onScanComplete() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleScanStopped();
                }
            });
        }
    };
}
