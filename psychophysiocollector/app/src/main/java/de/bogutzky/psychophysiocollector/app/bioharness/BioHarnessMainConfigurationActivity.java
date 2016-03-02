package de.bogutzky.psychophysiocollector.app.bioharness;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;

import de.bogutzky.psychophysiocollector.app.R;

public class BioHarnessMainConfigurationActivity extends Activity {
    //private static final String TAG = "BioHarnessMConfigActivity";
    private static final int REQUEST_SHOW_GRAPH = 13;

    private String bluetoothDeviceAddress;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_commands);

        Intent sender = getIntent();
        String currentDeviceName = sender.getExtras().getString("DeviceName");
        bluetoothDeviceAddress = sender.getExtras().getString("BluetoothDeviceAddress");
        setTitle(getTitle() + ": " + currentDeviceName);

        final ListView listViewCommands = (ListView) findViewById(R.id.listViewCommands);

        String[] commands = new String[]{getString(R.string.imu_config_show_sensor_data)};
        ArrayList<String> commandsList = new ArrayList<>();
        commandsList.addAll(Arrays.asList(commands));
        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(this, R.layout.commands_name, commandsList);
        listViewCommands.setAdapter(stringArrayAdapter);

        listViewCommands.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                switch (position) {
                    case 0:
                        Intent intent = new Intent();
                        intent.putExtra("bluetoothDeviceAddress", bluetoothDeviceAddress);
                        intent.putExtra("action", REQUEST_SHOW_GRAPH);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                }
            }
        });
    }
}
