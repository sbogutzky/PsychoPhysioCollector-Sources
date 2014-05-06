package de.bogutzky.data_collector.app;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.driver.ObjectCluster;

import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends ListActivity {

    private static final String TAG = "MainActivity";
    public static final Handler handler = new Handler() {
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Shimmer.MESSAGE_READ:
                    if ((msg.obj instanceof ObjectCluster)) {
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                        Log.d(TAG, "Data from: " + objectCluster.mBluetoothAddress);
                    }
                    break;
                case Shimmer.MESSAGE_TOAST:
                    Log.d(TAG, msg.getData().getString(Shimmer.TOAST));
                    break;
                case Shimmer.MESSAGE_STATE_CHANGE:
                    String bluetoothAddress = "None";
                    if ((msg.obj instanceof ObjectCluster)) {
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                        bluetoothAddress = objectCluster.mBluetoothAddress;
                    }
                    switch (msg.arg1) {
                        case Shimmer.STATE_CONNECTED:
                            Log.d(TAG, "Connected: " + bluetoothAddress);
                            break;
                        case Shimmer.STATE_CONNECTING:
                            Log.d(TAG, "Connecting: " + bluetoothAddress);
                            break;
                        case Shimmer.STATE_NONE:
                            Log.d(TAG, "None State: " + bluetoothAddress);
                            getConnectedShimmers().remove(bluetoothAddress);
                            break;
                        case Shimmer.MSG_STATE_FULLY_INITIALIZED:
                            Log.d(TAG, "Fully initialized: " + bluetoothAddress);
                            getConnectedShimmers().put(bluetoothAddress, getShimmers().get(bluetoothAddress));
                            break;
                    }
                    break;
                case Shimmer.MESSAGE_PACKET_LOSS_DETECTED:
                    Log.d(TAG, "Packet loss detected");
                    break;
            }
        }
    };
    private static final int MSG_BLUETOOTH_ADDRESS = 1;
    private static HashMap<String, Object> shimmers;
    private static HashMap<String, Object> connectedShimmers;
    private ArrayAdapter adapter;
    private ArrayList<String> bluetoothAddresses;
    private boolean noShimmerIsStreaming = true;

    public static HashMap<String, Object> getShimmers() {
        if (shimmers == null) {
            shimmers = new HashMap<String, Object>(4);
        }
        return shimmers;
    }

    public static HashMap<String, Object> getConnectedShimmers() {
        if (connectedShimmers == null) {
            connectedShimmers = new HashMap<String, Object>(4);
        }
        return connectedShimmers;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getBluetoothAddresses());
        setListAdapter(adapter);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "Connect to: " + getBluetoothAddresses().get(i));
                connectShimmer(getBluetoothAddresses().get(i), "test");
            }
        });
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
                    boolean isNewAddress = !getBluetoothAddresses().contains(bluetoothAddress);

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
        getBluetoothAddresses().add(bluetoothAddress);
        adapter.notifyDataSetChanged();
    }

    public void findBluetoothAddress() {
        if (noShimmerIsStreaming) {
            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(intent, MSG_BLUETOOTH_ADDRESS);
        } else {
            Toast.makeText(this, getString(R.string.ensure_no_device_is_streaming), Toast.LENGTH_LONG).show();
        }
    }

    public void connectShimmer(String bluetoothAddress, String deviceName) {
        Shimmer shimmer;
        if (!getShimmers().containsKey(bluetoothAddress)) {
            shimmer = new Shimmer(this, handler, deviceName, false);
            getShimmers().put(bluetoothAddress, shimmer);
        } else {
            Log.d(TAG, "Already added");
            if (!getConnectedShimmers().containsKey(bluetoothAddress)) {
                shimmer = (Shimmer) getShimmers().get(bluetoothAddress);
                shimmer.connect(bluetoothAddress, "default");
            } else {
                Log.d(TAG, "Already connected");
                //shimmer = (Shimmer) getConnectedShimmers().get(bluetoothAddress);
                //shimmer.toggleLed();
                disconnectShimmer(bluetoothAddress);
            }
        }
    }

    public void disconnectShimmer(String bluetoothAddress) {
        if (getConnectedShimmers().containsKey(bluetoothAddress)) {
            Shimmer shimmer = (Shimmer) getConnectedShimmers().get(bluetoothAddress);
            if (shimmer.getShimmerState() == Shimmer.STATE_CONNECTED) {
                shimmer.stop();
            }
        }
    }
}
