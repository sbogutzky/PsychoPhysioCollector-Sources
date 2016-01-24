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

public class ShimmerSensorConfigurationActivity extends Activity{

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.shimmer_commands);

	    Bundle extras = getIntent().getExtras();
        //String bluetoothAddress = extras.getString("Enabled_Sensors");
    	double mSamplingRateV = extras.getDouble("SamplingRate");
    	int mAccelerometerRangeV = extras.getInt("AccelerometerRange");
    	//int mGSRRangeV = extras.getInt("GSRRange");
    	
    	String[] samplingRates = new String [] {"10","51,2","102,4","128","170,7","204,8","256","512"};
    	String[] accelRange = new String [] {getString(R.string.acceleration_range_1_5g),getString(R.string.acceleration_range_6_0g)};
    	//String[] gsrRange = new String [] {"10kOhm to 56kOhm","56kOhm to 220kOhm","220kOhm to 680kOhm","680kOhm to 4.7MOhm","Auto Range"};

    	final ListView listViewSamplingRate = (ListView) findViewById(R.id.listViewSamplingRates);
        final ListView listViewAccelRange = (ListView) findViewById(R.id.listViewAccelerationRanges);
        //final ListView listViewGsrRange = (ListView) findViewById(R.id.listViewGsrRange);
        
        final TextView textViewCurrentSamplingRate = (TextView) findViewById(R.id.textViewCurrentSamplingRate);
        final TextView textViewCurrentAccelRange = (TextView) findViewById(R.id.textViewCurrentAccelerationRange);
        //final TextView textViewCurrentGsrRange = (TextView) findViewById(R.id.textViewCurrentGsrRange);
        
        textViewCurrentSamplingRate.setTextColor(Color.rgb(0, 135, 202));
        textViewCurrentAccelRange.setTextColor(Color.rgb(0, 135, 202));
        //textViewCurrentGsrRange.setTextColor(Color.rgb(0, 135, 202));
        if (mSamplingRateV!=-1){
        	textViewCurrentSamplingRate.setText(String.format("%3.1f", Math.round(mSamplingRateV * 10.0)/10.0));
        } else {
        	textViewCurrentSamplingRate.setText("");
        }
        
        if (mAccelerometerRangeV==0){
        	textViewCurrentAccelRange.setText(R.string.acceleration_range_1_5g);
        }
        else if (mAccelerometerRangeV==3){
        	textViewCurrentAccelRange.setText(R.string.acceleration_range_6_0g);
        } else {
        	textViewCurrentAccelRange.setText("");
        }

		/*
        if (mGSRRangeV==0) {
        	textViewCurrentGsrRange.setText("10kOhm to 56kOhm");
        } else if (mGSRRangeV==1) {
        	textViewCurrentGsrRange.setText("56kOhm to 220kOhm");
        } else if (mGSRRangeV==2) {
        	textViewCurrentGsrRange.setText("220kOhm to 680kOhm");
        } else if (mGSRRangeV==3) {
        	textViewCurrentGsrRange.setText("680kOhm to 4.7MOhm"); 
        } else if (mGSRRangeV==4) {
        	textViewCurrentGsrRange.setText("Auto Range");
        } else {
        	textViewCurrentGsrRange.setText("");
        }
        */
        
    	ArrayList<String> samplingRateList = new ArrayList<>();
    	samplingRateList.addAll( Arrays.asList(samplingRates) );
        ArrayAdapter<String> sR = new ArrayAdapter<>(this, R.layout.commands_name,samplingRateList);
    	listViewSamplingRate.setAdapter(sR);
    	
    	ArrayList<String> accelRangeList = new ArrayList<>();
    	accelRangeList.addAll( Arrays.asList(accelRange) );  
        ArrayAdapter<String> sR2 = new ArrayAdapter<>(this, R.layout.commands_name,accelRangeList);
    	listViewAccelRange.setAdapter(sR2);
    	
    	/*
    	ArrayList<String> gsrRangeList = new ArrayList<>();
    	gsrRangeList.addAll( Arrays.asList(gsrRange) );  
        ArrayAdapter<String> sR3 = new ArrayAdapter<>(this, R.layout.commands_name,gsrRangeList);
    	listViewGsrRange.setAdapter(sR3);
    	*/
    	
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
			  Number number = null;
			  try {
				  number = format.parse(o.toString());
			  } catch (ParseException e) {
				  e.printStackTrace();
			  }

			  intent.putExtra("SamplingRate", number.doubleValue());

			  // Set result and finish this Activity
			  setResult(Activity.RESULT_OK, intent);
			  finish();
  		  }
  		});
  	
  	listViewAccelRange.setOnItemClickListener(new AdapterView.OnItemClickListener() {

		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

		    Object o = listViewAccelRange.getItemAtPosition(position);
		    int accelRange=0;
		    if (o.toString().equals(getString(R.string.acceleration_range_1_5g))){
		    	accelRange=0;
		    } else if (o.toString().equals(getString(R.string.acceleration_range_6_0g))){
		    	accelRange=3;
		    }
		    Intent intent = new Intent();
	            intent.putExtra("AccelRange",accelRange);
	            // Set result and finish this Activity
	            setResult(Activity.RESULT_OK, intent);
	            finish();
		    
		  }
		});
	/*
  	listViewGsrRange.setOnItemClickListener(new AdapterView.OnItemClickListener() {

  		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
  			
  		    Object o = listViewGsrRange.getItemAtPosition(position);
  		    Log.d("Shimmer",o.toString());
  		    int gsrRange=0;
  		    if (o.toString()=="10kOhm to 56kOhm"){
  		    	gsrRange=0;
  		    } else if (o.toString()=="56kOhm to 220kOhm"){
  		    	gsrRange=1;
  		    } else if (o.toString()=="220kOhm to 680kOhm"){
  		    	gsrRange=2;
  		    } else if (o.toString()=="680kOhm to 4.7MOhm"){
  		    	gsrRange=3;
  		    } else if (o.toString()=="Auto Range"){
  		    	gsrRange=4;
  		    }
  		    Intent intent = new Intent();
	            intent.putExtra("GSRRange",gsrRange);
	            // Set result and finish this Activity
	            setResult(Activity.RESULT_OK, intent);
	            finish();
  		  }
  		});
    	*/
	}
}
