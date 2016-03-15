/**
 * The MIT License (MIT)
 Copyright (c) 2016 Simon Bogutzky, Jan Christoph Schrader

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction,
 including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included
 in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
