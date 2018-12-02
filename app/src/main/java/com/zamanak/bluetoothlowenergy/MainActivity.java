package com.zamanak.bluetoothlowenergy;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import com.zamanak.bluetoothlowenergy.ble.BleCommManager;
import com.zamanak.bluetoothlowenergy.ble.BlePeripheral;
import com.zamanak.bluetoothlowenergy.interfaces.AdvertiseReceiver;

public class MainActivity extends AppCompatActivity {

    /**
     * Constant
     **/
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    /**
     * Bluetooth Stuff
     **/
    private BleCommManager mBleCommManager; // for central
    private BlePeripheral blePeripheral; // for peripheral

    /**
     * UI Stuff
     **/
    private TextView mBluetoothStatusTV;
    private Switch mBluetoothOnSwith;

    /**
     * Load activity for the first time
     *
     * @param savedInstanceState
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // notify when bluetooth is turned on or off
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothAdvertiseReceiver,filter);
        loadUI();
    }

    /**
     * Load UI components
     */
    private void loadUI() {
        mBluetoothStatusTV = findViewById(R.id.bluetooth_status);
        mBluetoothOnSwith = findViewById(R.id.bluetooth_on);
    }

    /**
     * Turn on Bluetooth Radio notification when App resumes
     */
    @Override
    protected void onResume() {
        super.onResume();
        //initializeBluetoothCentral(); // this method is just for setting bluetooth - > Central
        initializeBluetoothSupportPeripheral(); // Peripheral
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBluetoothAdvertiseReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mBluetoothAdvertiseReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "receiver not registered");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate the menu;
        // this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    /**
     * Bluetooth Radio has been turn on. update UI
     */
    private void onBluetoothActive() {
        mBluetoothStatusTV.setText("Bluetooth Activated");
    }

    /**
     * Initialize the Bluetooth Radio
     */
    public void initializeBluetoothCentral() {
        // notify when bluetooth is turned on or off
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothAdvertiseReceiver, filter);
        try {
            mBleCommManager = new BleCommManager(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }
        // should prompt user to open seting if blutooth is not enabled
        if (mBleCommManager.getmBluetoothAdapter().isEnabled()) {
            onBluetoothActive();
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void initializeBluetoothSupportPeripheral(){
        // reset connection variables
        try{
            //blePeripheral = new BlePeripheral(this);
        }catch (Exception e){
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage() );
            finish();
        }
        mBluetoothOnSwith.setChecked(blePeripheral.getmBluetoothAdapter().isEnabled());

        // should prompt user to open settings if bluetooth is not enabled.
        if (!blePeripheral.getmBluetoothAdapter().isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent , REQUEST_ENABLE_BT);
        }
    }

    /**
     * keep track of changes to the bluetooth radio -> when use BleCommManager
     * when the bluetooth radio turns on, initialize the bluetooth connection -> when use Peripheral
     */
    private final AdvertiseReceiver mBluetoothAdvertiseReceiver = new AdvertiseReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "Bluetooth turned off");
                        //initializeBluetooth();
                        initializeBluetoothSupportPeripheral();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth turned on");
                        /*runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onBluetoothActive();
                            }
                        });*/
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };


}
