package com.zamanak.bluetoothlowenergy;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.zamanak.bluetoothlowenergy.adapters.BleGattProfileListAdapter;
import com.zamanak.bluetoothlowenergy.adapters.BlePeripheralListAdapter;
import com.zamanak.bluetoothlowenergy.ble.BleCommManager;
import com.zamanak.bluetoothlowenergy.ble.BlePeripheral;
import com.zamanak.bluetoothlowenergy.ble.ConnectActivity;
import com.zamanak.bluetoothlowenergy.ble.callBacks.BlePeripheralCallback;
import com.zamanak.bluetoothlowenergy.ble.callBacks.BleScanCallbackv18;
import com.zamanak.bluetoothlowenergy.ble.callBacks.BleScanCallbackv21;
import com.zamanak.bluetoothlowenergy.interfaces.AdvertiseReceiver;
import com.zamanak.bluetoothlowenergy.models.BlePeripheralListItem;

import java.util.List;

public class MainActivityNew extends AppCompatActivity {

    /**
     * Constant
     **/
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    /**
     * Bluetooth Stuff
     **/
    private BleCommManager mBleCommManager;
    private BlePeripheral blePeripheral;

    /**
     * Activity state
     **/
    private boolean mScanningActive = false;

    /**
     * UI Stuff
     **/
    private MenuItem mScanProgressSpinner;
    private MenuItem mStartScanItem, mStopScanItem;
    private ListView mBlePeripheralsListView;
    private TextView mPeripheralsListEmptyTV;
    private BlePeripheralListAdapter mBlePeripheralsListAdapter;

    private TextView mAdvertisingNameTV;
    private Switch mBluetoothOnSwitch, mAdvertisingSwitch, mCentralConnectedSwitch;

    private ExpandableListView mGattProfileListView;
    private BleGattProfileListAdapter mGattProfileListAdapter;
    private TextView mGattProfileListEmptyTV;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_peripheral);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // notify when bluetooth is turned on or off ******* for advertising
       /* IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBleAdvertiseReceiver, filter);*/
        // *****************************************************************
        boolean permissionGranted = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissionGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            if (permissionGranted) {
                doStuff();
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            }
        }
        loadUI();
        attachCallbacks();
    }

    private void doStuff() {
        // if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("This app needs location access");
        builder.setMessage("please grant location access so this app can detect peripherals.");
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
        // }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeBluetooth();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // stop scanning when the activity pauses
        //mBleCommManager.stopScanning(mBleScanCallbackv18, mScanCallbackv21);

        // stop advertising when the activity pauses
        blePeripheral.stopAdvertising();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBleAdvertiseReceiver);
    }

    /**
     * Load UI components
     */
    public void loadUI() {
        // load UI components, set up the Peripheral list ***
        mPeripheralsListEmptyTV = findViewById(R.id.peripheral_list_empty);
        mBlePeripheralsListView = findViewById(R.id.peripherals_list);
        mBlePeripheralsListAdapter = new BlePeripheralListAdapter();
        mBlePeripheralsListView.setAdapter(mBlePeripheralsListAdapter);
        mBlePeripheralsListView.setEmptyView(mPeripheralsListEmptyTV);
        // ****************************************************

        mAdvertisingNameTV = (TextView) findViewById(R.id.advertising_name);
        mBluetoothOnSwitch = (Switch) findViewById(R.id.bluetooth_on);
        mAdvertisingSwitch = (Switch) findViewById(R.id.advertising);
        mCentralConnectedSwitch = (Switch) findViewById(R.id.central_connected);
        mAdvertisingNameTV.setText(blePeripheral.ADVERTISING_NAME);
    }

    /**
     * Attach callback listeners to UI elements
     */
    public void attachCallbacks() {
        // if a list item is clicked,
        // open corresponding Peripheral in the ConnectActivity
        mBlePeripheralsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // only open if the selected item represents a Peripheral
                BlePeripheralListItem selectedPeripheralListItem = (BlePeripheralListItem) mBlePeripheralsListView.getItemAtPosition(position);
                Log.v(TAG, "Click at position: " + position + ", id: " + id);
                BlePeripheralListItem listItem = (BlePeripheralListItem) mBlePeripheralsListAdapter.getItem(position);
                connectToPeripheral(listItem.getMacAddress()); //*****************************
                stopScan();
            }
        });
    }

    private void connectToPeripheral(String macAddress) {
        startActivity(new Intent(this, ConnectActivity.class).putExtra("PERIPHERAL_MAC_ADDRESS_KEY", macAddress));
    }


    /**
     * Create a menu
     *
     * @param menu The menu
     * @return <b>true</b> if processed successfully
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu;
        // this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mStartScanItem = menu.findItem(R.id.action_start_scan);
        mStopScanItem = menu.findItem(R.id.action_stop_scan);
        mScanProgressSpinner = menu.findItem(R.id.scan_progress_item);
        return true;
    }

    /**
     * Handle a menu item click
     *
     * @param item the Menuitem
     * @return <b>true</b> if processed successfully
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Start a BLE scan when a user clicks the "start scanning" menu button
        // and stop a BLE scan
        // when a user clicks the "stop scanning" menu button
        switch (item.getItemId()) {
            case R.id.action_start_scan:
                // User chose the "Scan" item
                startScan();
                return true;
            case R.id.action_stop_scan:
                // User chose the "Stop" item
                stopScan();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Initialize the Bluetooth Radio
     */
    public void initializeBluetooth() {
        // this try catch is for scan for peripherals list *********
        try {
            mBleCommManager = new BleCommManager(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }
        // should prompt user to open seting if blutooth is not enabled
        if (!mBleCommManager.getmBluetoothAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // ************************************************************

        // reset connection variables -> this try catch is for advertising *****************
        try {
            blePeripheral = new BlePeripheral(this, mBlePeripheralCallback);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }
        mBluetoothOnSwitch.setChecked(blePeripheral.getmBluetoothAdapter().isEnabled());
        // should prompt user to open settings if Bluetooth is not enabled.
        if (!blePeripheral.getmBluetoothAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startAdvertising();
        }
        // *********************************************************************************

    }

    /**
     * Event trigger when Central has connected
     *
     * @param bluetoothDevice
     */
    public void onBleCentralConnected(final BluetoothDevice bluetoothDevice) {
        mCentralConnectedSwitch.setChecked(true);
    }

    /**
     * Event trigger when Central has disconnected
     *
     * @param bluetoothDevice
     */
    public void onBleCentralDisconnected(final BluetoothDevice bluetoothDevice) {
        mCentralConnectedSwitch.setChecked(false);
    }


    /**
     * Start advertising Peripheral
     */
    public void startAdvertising() {
        Log.v(TAG, "starting advertising...");
        try {
            blePeripheral.startAdvertising();
        } catch (Exception e) {
            Log.e(TAG, "problem starting advertising");
        }
    }

    /**
     * Event trigger when BLE Advertising has stopped
     */
    public void onBleAdvertisingStarted() {
        mAdvertisingSwitch.setChecked(true);
    }

    /**
     * Advertising Failed to start
     */
    public void onBleAdvertisingFailed() {
        mAdvertisingSwitch.setChecked(false);
    }

    /**
     * Event trigger when BLE Advertising has stopped
     */
    public void onBleAdvertisingStopped() {
        mAdvertisingSwitch.setChecked(false);
    }

    /**
     * When the Bluetooth radio turns on, initialize the Bluetooth connection
     */
    private final AdvertiseReceiver mBleAdvertiseReceiver = new AdvertiseReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                );
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.v(TAG, "Bluetooth turned off");
                        initializeBluetooth();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.v(TAG, "Bluetooth turned on");
                        startAdvertising();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    /**
     * Respond to changes to the Bluetooth Peripheral state
     */
    private final BlePeripheralCallback mBlePeripheralCallback = new BlePeripheralCallback() {
        public void onAdvertisingStarted() {
            Log.v(TAG, "Advertising started");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleAdvertisingStarted();
                }
            });
        }

        public void onAdvertisingFailed(int errorCode) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleAdvertisingFailed();
                }
            });
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "Failed to start; advertising is already started.");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Log.e(TAG, "Failed to start; advertised data > 31 bytes.");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "This feature is not supported on this platform.");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    Log.e(TAG, "Operation failed due to an internal error.");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Log.e(TAG, "Failed to start; advertising instance is available.");
                    break;
                default:
                    Log.e(TAG, "unknown problem");
            }
        }

        public void onAdvertisingStopped() {
            Log.v(TAG, "Advertising stopped");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleAdvertisingStopped();
                }
            });
        }

        @Override
        public void onCentralConnected(final BluetoothDevice bluetoothDevice) {
            Log.v(TAG, "Central connected");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleCentralConnected(bluetoothDevice);
                }
            });
        }

        @Override
        public void onCentralDisconnected(final BluetoothDevice bluetoothDevice) {
            Log.v(TAG, "Central disconnected");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleCentralDisconnected(bluetoothDevice);
                }
            });
        }
    };


    /**
     * Start scanning for Peripherals
     * <p>
     * New in this chapter
     */
    public void startScan() {
        // update UI components
        mStartScanItem.setVisible(false);
        mStopScanItem.setVisible(true);
        mScanProgressSpinner.setVisible(true);
        // clear the list of Peripherals and start scanning
        mBlePeripheralsListAdapter.clear();
        try {
            mScanningActive = true;
            mBleCommManager.scanForPeripherals(mBleScanCallbackv18, mScanCallbackv21);
        } catch (Exception e) {
            Log.e(TAG, "Could not open Ble Device Scanner");
        }
    }

    /**
     * Stop scanning for Peripherals
     * <p>
     * New in this chapter
     */
    public void stopScan() {
        mBleCommManager.stopScanning(mBleScanCallbackv18, mScanCallbackv21);
    }

    /**
     * Event trigger when BLE Scanning has stopped
     * <p>
     * New in this chapter
     */
    public void onBleScanStopped() {
        // update UI compenents to reflect that a BLE scan has stopped
        // Possible for this method to be called before the menu has been created
        // Check to see if menu items are initialized, or Activity will crash
        mScanningActive = false;
        if (mStopScanItem != null) mStopScanItem.setVisible(false);
        if (mScanProgressSpinner != null) {
            mScanProgressSpinner.setVisible(false);
        }
        if (mStartScanItem != null) mStartScanItem.setVisible(true);
    }

    /**
     * Event trigger when new Peripheral is discovered
     * <p>
     * New in this chapter
     */
    public void onBlePeripheralDiscovered(BluetoothDevice bluetoothDevice, int rssi) {
        Log.v(TAG, "Found " + bluetoothDevice.getName() + ", " + bluetoothDevice.getAddress());
        // only add the peripheral if
        // - it has a name, on
        // - doesn't already exist in our list, or
        // - is transmitting at a higher power (is closer)
        //   than a similar peripheral
        boolean addPeripheral = true;
        if (bluetoothDevice.getName() == null) {
            addPeripheral = false;
        }
        for (BlePeripheralListItem listItem : mBlePeripheralsListAdapter.getItems()) {
            if (listItem.getAdvertiseName().equals(bluetoothDevice.getName())) {
                addPeripheral = false;
            }
        }
        if (addPeripheral) {
            mBlePeripheralsListAdapter.addBluetoothPeripheral(bluetoothDevice, rssi);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBlePeripheralsListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    /**
     * Use this callback for Android API 21 (Lollipop) or greater
     * <p>
     * New in this chapter
     */
    private final BleScanCallbackv21 mScanCallbackv21 = new BleScanCallbackv21() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice bluetoothDevice = result.getDevice();
            int rssi = result.getRssi();
            onBlePeripheralDiscovered(bluetoothDevice, rssi);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                BluetoothDevice bluetoothDevice = result.getDevice();
                int rssi = result.getRssi();
                onBlePeripheralDiscovered(bluetoothDevice, rssi);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "Scan with the same settings already started");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.e(TAG, "App cannot be registered.");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "Power optimized scan is not supported.");
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
     * <p>
     * New in this chapter
     */
    public final BleScanCallbackv18 mBleScanCallbackv18 = new BleScanCallbackv18() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
            onBlePeripheralDiscovered(bluetoothDevice, rssi);
        }

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
