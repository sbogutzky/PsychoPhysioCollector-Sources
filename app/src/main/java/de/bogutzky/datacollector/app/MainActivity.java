package de.bogutzky.datacollector.app;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.SimpleCursorAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;


public class MainActivity extends ListActivity {

    // This is the Adapter being used to display the list's data
    ArrayAdapter adapter;
    ArrayList<String> sensors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getSensors());
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public ArrayList<String> getSensors() {
        if (sensors == null) {
            sensors = new ArrayList<String>();
            sensors.add(getString(R.string.no_sensors));
        }
        return sensors;
    }

    public void setSensors(ArrayList<String> sensors) {
        this.sensors = sensors;
    }
}
