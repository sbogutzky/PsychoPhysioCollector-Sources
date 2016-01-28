package de.bogutzky.psychophysiocollector.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ShimmerSensorConfigurationActivity extends Activity {
    private static final String TAG = "ShimmerSConfigActivity";

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.shimmer_commands);

	    Bundle extras = getIntent().getExtras();
    	double samplingRate = extras.getDouble("SamplingRate");
    	int accelerometerRange = extras.getInt("AccelerometerRange");
    	int gyroscopeRange = extras.getInt("GyroscopeRange");
        Log.d(TAG, "Gyroscope Range: " + gyroscopeRange);
    	
    	String[] samplingRates = new String [] {"10","51,2","102,4","128","170,7","204,8","256","512"};
    	String[] accelerometerRanges = new String [] {getString(R.string.acceleration_range_1_5g),getString(R.string.acceleration_range_6_0g)};

    	final ListView listViewSamplingRate = (ListView) findViewById(R.id.listViewSamplingRates);
        final ListView listViewAccelRange = (ListView) findViewById(R.id.listViewAccelerationRanges);
        
        final TextView textViewCurrentSamplingRate = (TextView) findViewById(R.id.textViewCurrentSamplingRate);
        final TextView textViewCurrentAccelRange = (TextView) findViewById(R.id.textViewCurrentAccelerationRange);
        
        textViewCurrentSamplingRate.setTextColor(Color.rgb(0, 135, 202));
        textViewCurrentAccelRange.setTextColor(Color.rgb(0, 135, 202));

        if (samplingRate != -1){
        	textViewCurrentSamplingRate.setText(String.format("%3.1f", Math.round(samplingRate * 10.0)/10.0));
        } else {
        	textViewCurrentSamplingRate.setText("");
        }
        
        if (accelerometerRange == 0){
        	textViewCurrentAccelRange.setText(R.string.acceleration_range_1_5g);
        }
        else if (accelerometerRange == 3){
        	textViewCurrentAccelRange.setText(R.string.acceleration_range_6_0g);
        } else {
        	textViewCurrentAccelRange.setText("");
        }
        
    	ArrayList<String> samplingRateList = new ArrayList<>();
    	samplingRateList.addAll( Arrays.asList(samplingRates) );
        ArrayAdapter<String> sR = new ArrayAdapter<>(this, R.layout.commands_name,samplingRateList);
    	listViewSamplingRate.setAdapter(sR);
    	
    	ArrayList<String> accelRangeList = new ArrayList<>();
    	accelRangeList.addAll( Arrays.asList(accelerometerRanges) );
        ArrayAdapter<String> sR2 = new ArrayAdapter<>(this, R.layout.commands_name,accelRangeList);
    	listViewAccelRange.setAdapter(sR2);
    	
    	Button buttonToggleLED = (Button) findViewById(R.id.buttonToggleLed);
	    
    	buttonToggleLED.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				Intent intent = new Intent();
	            intent.putExtra("ToggleLED", true);

	            // Set result and finish this Activity
	            setResult(Activity.RESULT_OK, intent);
	            finish();
			}
        });
    	
    	listViewSamplingRate.setOnItemClickListener(new AdapterView.OnItemClickListener() {

  		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
			  Object o = listViewSamplingRate.getItemAtPosition(position);
			  Intent intent = new Intent();

			  Locale current = getResources().getConfiguration().locale;
			  NumberFormat format = NumberFormat.getInstance(current);
			  try {
                  Number number = format.parse(o.toString());
                  intent.putExtra("SamplingRate", number.doubleValue());

                  // Set result and finish this Activity
                  setResult(Activity.RESULT_OK, intent);
                  finish();
			  } catch (ParseException e) {
				  e.printStackTrace();
			  }
  		  }
  		});
  	
  	listViewAccelRange.setOnItemClickListener(new AdapterView.OnItemClickListener() {

		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

		        Object o = listViewAccelRange.getItemAtPosition(position);
		        int accelerometerRange = 0;
		        if (o.toString().equals(getString(R.string.acceleration_range_1_5g))) {
		    	    accelerometerRange = 0;
		        } else if (o.toString().equals(getString(R.string.acceleration_range_6_0g))) {
		    	    accelerometerRange = 3;
		        }
		        Intent intent = new Intent();
                intent.putExtra("AccelerometerRange", accelerometerRange);

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);
                finish();
		  }
    });

	}
}
