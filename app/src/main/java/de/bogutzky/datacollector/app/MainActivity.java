package de.bogutzky.datacollector.app;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import java.util.ArrayList;

public class MainActivity extends ListActivity {

    // This is the Adapter being used to display the list's data
    ArrayAdapter adapter;
    ArrayList<String> sensors;
    //MultiShimmerTemplateService multiShimmerTemplateService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Intent intent = new Intent(this, MultiShimmerTemplateService.class);
        //startService(intent);

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
            getSensors().add("Sensor " + getSensors().size());
            adapter.notifyDataSetChanged();

            //mService.mShimmerConfigurationList.add(new ShimmerConfiguration("Device", "", mService.mShimmerConfigurationList.size(), Shimmer.SENSOR_ACCEL, 51.2, -1, -1, -1,-1,-1,-1,-1,-1,0,-1));
            //updateShimmerConfigurationList(mService.mShimmerConfigurationList);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public ArrayList<String> getSensors() {
        if (sensors == null) {
            sensors = new ArrayList<String>();
        }
        return sensors;
    }

    public void setSensors(ArrayList<String> sensors) {
        this.sensors = sensors;
    }
}