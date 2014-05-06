package de.bogutzky.data_collector.app;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends ListActivity {

    private static final String TAG = "MainActivity";
    private static final int MSG_BLUETOOTH_ADDRESS = 1;

    ArrayAdapter adapter;
    ArrayList<String> bluetoothAddresses;
    boolean noDeviceIsStreaming = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getBluetoothAddresses());
        setListAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add_sensor) {
            findBluetoothAddress();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case MSG_BLUETOOTH_ADDRESS:

                // When DeviceListActivity returns with a device address to connect
                if (resultCode == Activity.RESULT_OK) {
                    String bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Log.d(TAG, "Bluetooth Address: " + bluetoothAddress);

                    // Check if the bluetooth address has been previously selected
                    boolean isNewAddress = !bluetoothAddresses.contains(bluetoothAddress);

                    if (isNewAddress) {
                        addBluetoothAddress(bluetoothAddress);
                    } else {
                        Toast.makeText(this, getString(R.string.device_is_already_in_list), Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public ArrayList<String> getBluetoothAddresses() {
        if (bluetoothAddresses == null) {
            bluetoothAddresses = new ArrayList<String>();
        }
        return bluetoothAddresses;
    }

    public void addBluetoothAddress(String bluetoothAddress) {
        bluetoothAddresses.add(bluetoothAddress);
        adapter.notifyDataSetChanged();
    }

    public void findBluetoothAddress() {
        if (noDeviceIsStreaming) {
            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(intent, MSG_BLUETOOTH_ADDRESS);
        } else {
            Toast.makeText(this, getString(R.string.ensure_no_device_is_streaming), Toast.LENGTH_LONG).show();
        }
    }
}
