package com.zamanak.bluetoothlowenergy.adapters;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.zamanak.bluetoothlowenergy.R;
import com.zamanak.bluetoothlowenergy.models.BlePeripheralListItem;

import java.util.ArrayList;

/**
 * this class will populate a list of discovered peripherals in the main activity
 */
public class BlePeripheralListAdapter extends BaseAdapter {

    private static String TAG = BlePeripheralListAdapter.class.getSimpleName();
    private ArrayList<BlePeripheralListItem> mBluetoothPeripheralListItems = new ArrayList<BlePeripheralListItem>(); // list of peripherals

    /**
     * How many items are in the ListView
     *
     * @return the number of items in this ListView
     */
    @Override
    public int getCount() {
        return mBluetoothPeripheralListItems.size();
    }

    /**
     * add a new pheripheral to the list view
     *
     * @param bluetoothDevice peripheral device information
     * @param rssi            peripheral's RSSI, indicating it's radio signal quality
     */
    public void addBluetoothPeripheral(BluetoothDevice bluetoothDevice, int rssi) {

        // update UI stuff
        int listItemId = mBluetoothPeripheralListItems.size();
        BlePeripheralListItem listItem = new BlePeripheralListItem(bluetoothDevice);
        listItem.setmItemId(listItemId);
        listItem.setmRssi(rssi);

        // add to list
        mBluetoothPeripheralListItems.add(listItem);
    }

    /**
     * Get current state of list view
     *
     * @return Arraylist of BlePeripheralListItems
     */
    public ArrayList<BlePeripheralListItem> getItems() {
        return mBluetoothPeripheralListItems;
    }

    /**
     * Clear all items from the list view
     */
    public void clear() {
        mBluetoothPeripheralListItems.clear();
    }

    /**
     * Get the BlePeripheralListItem held at some position in the list view
     *
     * @param position the position of a desired item in the list
     * @return the BlePeripheralListItem at some position
     */
    @Override
    public Object getItem(int position) {
        return mBluetoothPeripheralListItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mBluetoothPeripheralListItems.get(position).getmItemId();
    }

    /**
     * This ViewHolder represents what UI components are in each list item
     */

    public static class ViewHolder {
        public TextView mAdvertiseNameTV;
        public TextView mMacAddressTV;
        public TextView mRssiTV;
    }

    /**
     * Generate a new listItem for some known position in the list view
     *
     * @param position    the position of the Listview
     * @param convertView an exiting list item
     * @param parent      the parent ViewGroup
     * @return the list item
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder peripheralListItemView;

        // if this ListItem does not exit yet,generate it
        // otherwise, use it
        if (convertView == null) {
            // convert list_item_peripheral.xml to a view
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            v = inflater.inflate(R.layout.list_item_peripheral, null);
            // match the ui stuff in the list item to what's in the xml file
            peripheralListItemView = new ViewHolder();
            peripheralListItemView.mAdvertiseNameTV = v.findViewById(R.id.advertise_name);
            peripheralListItemView.mMacAddressTV = v.findViewById(R.id.mac_address);
            peripheralListItemView.mRssiTV = v.findViewById(R.id.rssi);
            v.setTag(peripheralListItemView);
        } else {
            peripheralListItemView = (ViewHolder) v.getTag();
        }
        Log.d(TAG, "ListItem Size: " + mBluetoothPeripheralListItems.size());
        // if there are known peripherals, create a listItem that says so
        // otherwise, display a listItem with Bluetooth peripheral information
        if (mBluetoothPeripheralListItems.size() <= 0) {
            peripheralListItemView.mAdvertiseNameTV.setText(R.string.peripheral_list_empty);
        } else {
            BlePeripheralListItem item = mBluetoothPeripheralListItems.get(position);
            peripheralListItemView.mAdvertiseNameTV.setText(item.getAdvertiseName());
            peripheralListItemView.mMacAddressTV.setText(item.getMacAddress());
            peripheralListItemView.mRssiTV.setText(String.valueOf(item.getmRssi()));
        }
        return v;
    }
}
