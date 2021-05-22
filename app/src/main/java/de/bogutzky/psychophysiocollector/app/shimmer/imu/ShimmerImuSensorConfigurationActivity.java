/**
 * The MIT License (MIT)
 Copyright (c) 2016 Copyright (c) 2016 University of Applied Sciences Bremen

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

package de.bogutzky.psychophysiocollector.app.shimmer.imu;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import de.bogutzky.psychophysiocollector.app.R;

public class ShimmerImuSensorConfigurationActivity extends Activity {
    //private static final String TAG = "ShimmerSConfigActivity";
	public final static String[] samplingRates = {"51,2","102,4","128","170,7","204,8","256","512","1024"};
	public final static String[] accelerometerRanges = {"+/- 1,5","+/- 6"};
	public final static String[] gyroscopeRanges = {"+/- 250","+/- 500"}; // {"+/- 250","+/- 500","+/- 1000","+/- 2000"}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.shimmer_commands);

	    Bundle extras = getIntent().getExtras();
    	double samplingRate = extras.getDouble("SamplingRate");
    	int accelerometerRange = extras.getInt("AccelerometerRange");
    	int gyroscopeRange = extras.getInt("GyroscopeRange");

    	final ListView listViewSamplingRate = (ListView) findViewById(R.id.listViewCommands);
        final ListView listViewAccelerometerRange = (ListView) findViewById(R.id.listViewAccelerometerRanges);
		final ListView listViewGyroscopeRange = (ListView) findViewById(R.id.listViewGyroscopeRange);
        
        final TextView textViewCurrentSamplingRate = (TextView) findViewById(R.id.textViewCurrentSamplingRate);
        final TextView textViewCurrentAccelerometerRange = (TextView) findViewById(R.id.textViewCurrentAccelerometerRange);
		final TextView textViewCurrentGyroscopeRange = (TextView) findViewById(R.id.textViewCurrentGyroscopeRange);
        
        textViewCurrentSamplingRate.setTextColor(Color.rgb(0, 135, 202));
        textViewCurrentAccelerometerRange.setTextColor(Color.rgb(0, 135, 202));
		textViewCurrentGyroscopeRange.setTextColor(Color.rgb(0, 135, 202));

        if (samplingRate != -1){
        	textViewCurrentSamplingRate.setText(String.format("%3.1f", Math.round(samplingRate * 10.0)/10.0));
        } else {
        	textViewCurrentSamplingRate.setText("");
        }
        
        if (accelerometerRange == 0){
        	textViewCurrentAccelerometerRange.setText(R.string.acceleration_range_1_5);
        }
        else if (accelerometerRange == 3){
        	textViewCurrentAccelerometerRange.setText(R.string.acceleration_range_6_0);
        } else {
        	textViewCurrentAccelerometerRange.setText("");
        }

		textViewCurrentGyroscopeRange.setText(gyroscopeRanges[gyroscopeRange]);
        
    	ArrayList<String> samplingRateList = new ArrayList<>();
    	samplingRateList.addAll(Arrays.asList(samplingRates));
        ArrayAdapter<String> samplingRateArrayAdapter = new ArrayAdapter<>(this, R.layout.commands_name, samplingRateList);
    	listViewSamplingRate.setAdapter(samplingRateArrayAdapter);
    	
    	ArrayList<String> accelerometerRangeList = new ArrayList<>();
    	accelerometerRangeList.addAll(Arrays.asList(accelerometerRanges));
        ArrayAdapter<String> accelerometerRangeArrayAdapter = new ArrayAdapter<>(this, R.layout.commands_name, accelerometerRangeList);
    	listViewAccelerometerRange.setAdapter(accelerometerRangeArrayAdapter);

		ArrayList<String> gyroscopeRangeList = new ArrayList<>();
		gyroscopeRangeList.addAll(Arrays.asList(gyroscopeRanges));
		ArrayAdapter<String> gyroscopeRangeArrayAdapter = new ArrayAdapter<>(this, R.layout.commands_name, gyroscopeRangeList);
		listViewGyroscopeRange.setAdapter(gyroscopeRangeArrayAdapter);
    	
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

		listViewAccelerometerRange.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

				Object o = listViewAccelerometerRange.getItemAtPosition(position);
				int accelerometerRange = 0;
				if (o.toString().equals(getString(R.string.acceleration_range_1_5))) {
					accelerometerRange = 0;
				} else if (o.toString().equals(getString(R.string.acceleration_range_6_0))) {
					accelerometerRange = 3;
				}
				Intent intent = new Intent();
				intent.putExtra("AccelerometerRange", accelerometerRange);

				// Set result and finish this Activity
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		});


		listViewGyroscopeRange.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

				Intent intent = new Intent();
				intent.putExtra("GyroscopeRange", position);

				// Set result and finish this Activity
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		});
	}
}
