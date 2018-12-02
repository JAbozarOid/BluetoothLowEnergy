package com.zamanak.bluetoothlowenergy.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zamanak.bluetoothlowenergy.R;
import com.zamanak.bluetoothlowenergy.TalkActivity;
import com.zamanak.bluetoothlowenergy.adapters.BleGattProfileListAdapter;

import java.util.List;

public class ConnectActivity extends AppCompatActivity {

    /**
     * Constants
     **/
    private static final String TAG = ConnectActivity.class.getSimpleName();
    //public static final String PERIPHERAL_MAC_ADDRESS_KEY = "D3:E3:AF:4F:A8:A2";

    /**
     * Bluetooth Stuff
     **/
    private BleCommManager mBleCommManager;
    private BlePeripheral mBlePeripheral;

    /**
     * Functional stuff
     **/
    private String mPeripheralMacAddress;
    private String mBlePeripheralName;

    /**
     * Activity State
     **/
    private boolean mBleConnected = false;
    private boolean mLeaveActivity = false;

    /**
     * UI Stuff
     **/
    private MenuItem mProgressSpinner;
    private MenuItem mConnectItem, mDisconnectItem;
    private TextView mPeripheralAdvertiseNameTV, mPeripheralAddressTV;

    /**
     * UI for profile GATT Services and Characteristic
     */
    private ExpandableListView mGattProfileListView;
    private BleGattProfileListAdapter mGattProfileListAdapter;
    private TextView mGattProfileListEmptyTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // grab information passed to the savedInstanceState,
        // from when the user clicked on the list in MainActivty
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mPeripheralMacAddress = extras.getString("PERIPHERAL_MAC_ADDRESS_KEY");
            }
        } else {
            mPeripheralMacAddress = savedInstanceState.getString("PERIPHERAL_MAC_ADDRESS_KEY");
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mBlePeripheral = new BlePeripheral();
        loadUI();
        attachCallbacks();

    }

    /**
     * Prepare the UI elements
     */
    public void loadUI() {
        mPeripheralAdvertiseNameTV = (TextView) findViewById(R.id.advertise_name);
        mPeripheralAddressTV = (TextView) findViewById(R.id.mac_address);
        mGattProfileListEmptyTV = (TextView) findViewById(R.id.gatt_profile_list_empty);
        mGattProfileListView = (ExpandableListView) findViewById(R.id.peripherals_list_expan);
        mGattProfileListAdapter = new BleGattProfileListAdapter();
        mGattProfileListView.setAdapter(mGattProfileListAdapter);
        mGattProfileListView.setEmptyView(mGattProfileListEmptyTV);
    }

    /**
     * Attach callback handlers to UI elements
     */
    public void attachCallbacks() {
        // When a user clicks on a Service
        // expand the Characteristics  belonging to that Service.
        // When a user clicks on a Characteristic, open in TalkActivity
        mGattProfileListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Log.v(TAG, "List View click: groupPosition: " + groupPosition + ", childPosition: " + childPosition);
                BluetoothGattService service = mGattProfileListAdapter.getGroup(groupPosition).getService();
                BluetoothGattCharacteristic characteristic = mGattProfileListAdapter.getChild(groupPosition, childPosition).getCharacteristic();
                // start the Talk Activity and connect
                Intent intent = new Intent(getBaseContext(), TalkActivity.class);
                intent.putExtra("PERIPHERAL_NAME", mBlePeripheralName);
                intent.putExtra("PERIPHERAL_MAC_ADDRESS_KEY", mPeripheralMacAddress);
                intent.putExtra("CHARACTERISTIC_KEY", characteristic.getUuid().toString());
                intent.putExtra("SERVICE_KEY", service.getUuid().toString());
                Log.v(TAG, "Setting intent: " + TalkActivity.CHARACTERISTIC_KEY + ": " + characteristic.getUuid().toString());
                startActivity(intent);
                return false;
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        // connect the bluetooth device
        initializeBluetooth();
    }

    @Override
    public void onPause() {
        super.onPause();
        disconnect();
    }

    /**
     * Create the menu
     *
     * @param menu
     * @return true if successful
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu;
        // this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect, menu);
        mConnectItem = menu.findItem(R.id.action_connect);
        mDisconnectItem = menu.findItem(R.id.action_disconnect);
        mProgressSpinner = menu.findItem(R.id.scan_progress_item);
        initializeBluetooth();
        connect();
        return true;
    }

    /**
     * User clicked a menu button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect:
                // User chose the "Scan" item
                connect();
                return true;
            case R.id.action_disconnect:
                // User chose the "Stop" item
                mLeaveActivity = true;
                quitActivity();
                return true;
            default:
                // The user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Turn on Bluetooth radio
     */
    public void initializeBluetooth() {
        try {
            mBleCommManager = new BleCommManager(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }
    }

    /**
     * Connect to Peripheral
     */
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
     * Disconnect from Peripheral
     */
    public void disconnect() {
        // disconnect from the Peripheral.
        mProgressSpinner.setVisible(true);
        mBlePeripheral.disconnect();
    }

    /**
     * Bluetooth Peripheral connected.  Update UI
     */
    public void onBleConnected() {
        // update UI to reflect a connection
        BluetoothDevice bluetoothDevice = mBlePeripheral.getBluetoothDevice();
        mBlePeripheralName = bluetoothDevice.getName();
        mPeripheralAdvertiseNameTV.setText(bluetoothDevice.getName());
        mPeripheralAddressTV.setText(bluetoothDevice.getAddress());
        mConnectItem.setVisible(false);
        mDisconnectItem.setVisible(true);
        mProgressSpinner.setVisible(false);
    }

    /**
     * Bluetooth Peripheral GATT Profile discovered.  Update UI
     * <p>
     * New in this chapter
     */
    public void onBleServiceDiscoveryStopped() {
        // update UI to reflect the GATT profile of the connected Perihperal
        mProgressSpinner.setVisible(false);
        mConnectItem.setVisible(false);
        mDisconnectItem.setVisible(true);
    }

    private void onBleServiceDiscoveryStarted() {
        mProgressSpinner.setVisible(true);
        mConnectItem.setVisible(true);
        mDisconnectItem.setVisible(false);
    }


    /**
     * Quit the activity if the Peripheral is disconnected.
     * Otherwise disconnect and try again
     */
    public void quitActivity() {
        if (!mBleConnected) {
            finish();
        } else {
            disconnect();
        }
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /**
         * Characteristic value changed
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // We don't care about this here as we aren't communicating yet
        }

        /**
         * Peripheral connected or disconnected
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // There has been a connection or a disconnection
            // with a Peripheral.
            // If this is a connection, update the UI to reflect the change
            // and discover the GATT profile of the connected Peripheral
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v(TAG, "Connected to peripheral");
                mBleConnected = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBleConnected();
                        onBleServiceDiscoveryStarted();
                    }
                });
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mBlePeripheral.close();
                mBleConnected = false;
                if (mLeaveActivity) quitActivity();
            }
        }

        /**
         * Gatt Profile discovered
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            // if services were discovered, iterate through and display them
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = bluetoothGatt.getServices();
                for (BluetoothGattService service : services) {
                    if (service != null) {
                        Log.v(TAG, "Service uuid: " + service.getUuid());
                        // add the gatt service to our list
                        mGattProfileListAdapter.addService(service);
                        // update the UI to reflect the new Service
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mGattProfileListAdapter.notifyDataSetChanged();
                            }
                        });
                        // ask for this service's characteristics:
                        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            if (characteristic != null) {
                                // add Characteristics to the Service's list
                                try {
                                    mGattProfileListAdapter.addCharacteristic(service, characteristic);
                                    // update the ListView UI
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mGattProfileListAdapter.notifyDataSetChanged();
                                        }
                                    });
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage());
                                }
                            }
                        }
                    }
                }
                disconnect(); // disconnect from the Peripheral
            } else {
                Log.e(TAG, "Problem discovering GATT services");
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleServiceDiscoveryStopped();
                }
            });
        }
    };


}

