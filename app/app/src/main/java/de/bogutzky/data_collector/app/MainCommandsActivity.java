package de.bogutzky.data_collector.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;


public class MainCommandsActivity extends Activity{
	String mCurrentDevice=null;
	Button mButtonConnect;
	Button mButtonCommand;
	int mCurrentSlot=-1;
	private boolean mServiceBind=false;
	private String[] commands = new String [] {"Enable Sensors","Sub Commands"};
	private double mSamplingRate=-1;
	private int mAccelRange=-1;
	private int mGSRRange=-1;
	
	
	 public void onCreate(Bundle savedInstanceState) {
		    super.onCreate(savedInstanceState);
		    setContentView(R.layout.main_commands);

		    Intent sender=getIntent();
		    String extraData=sender.getExtras().getString("LocalDeviceID");
		    mCurrentDevice=extraData;
		    setTitle("CMD: " + mCurrentDevice);
			mCurrentSlot=sender.getExtras().getInt("CurrentSlot");
		    Log.d("Shimmer","Create MC:  " + extraData);
		    
		    
		    final ListView listViewCommands = (ListView) findViewById(R.id.listView1);

		    
			ArrayList<String> commandsList = new ArrayList<String>();  
			commandsList.addAll( Arrays.asList(commands) );  
		    ArrayAdapter<String> sR = new ArrayAdapter<String>(this, R.layout.commands_name,commandsList);
			listViewCommands.setAdapter(sR);
		    
		    
			listViewCommands.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
					 if (position==1){
//						  	mSamplingRate=mService.getSamplingRate(mCurrentDevice);
//				      		mAccelRange=mService.getAccelRange(mCurrentDevice);
//				      		mGSRRange=mService.getGSRRange(mCurrentDevice);
							Intent mainCommandIntent=new Intent(MainCommandsActivity.this,CommandsSub.class);
							if (mCurrentDevice.equals("All Devices")){
								mainCommandIntent.putExtra("BluetoothAddress","");
								mainCommandIntent.putExtra("SamplingRate",-1.0);
								mainCommandIntent.putExtra("AccelerometerRange",-1);
								mainCommandIntent.putExtra("GSRRange",-1);
							} else {
								mainCommandIntent.putExtra("BluetoothAddress","");
								mainCommandIntent.putExtra("SamplingRate",mSamplingRate);
								mainCommandIntent.putExtra("AccelerometerRange",mAccelRange);
								mainCommandIntent.putExtra("GSRRange",mGSRRange);
							}
				     		startActivityForResult(mainCommandIntent, MainActivity.REQUEST_COMMANDS_SHIMMER);
					  }else if (position==0){
						  Intent mainCommandIntent=new Intent(MainCommandsActivity.this,ConfigureActivity.class);
						  startActivityForResult(mainCommandIntent, MainActivity.REQUEST_CONFIGURE_SHIMMER);
					  }
				  }
				});
			
		    
		    
		    
		   
		    
	 }
	 
	 	
	    public void onActivityResult(int requestCode, int resultCode, Intent data) {
			switch (requestCode) {
				case MainActivity.REQUEST_COMMANDS_SHIMMER:
					if (resultCode == Activity.RESULT_OK) {
						Log.d("Shimmer", "COmmands Received");
						Log.d("Shimmer", "iam");
						if (resultCode == Activity.RESULT_OK) {
							if (data.getExtras().getBoolean("ToggleLED", false) == true) {
								if (mCurrentDevice.equals("All Devices")) {
									Log.d("Shimmer", "Toggle ALL LEDS");
									//mService.toggleAllLEDS();
								} else {
									//mService.toggleLED(mCurrentDevice);
								}

							}

							if (data.getExtras().getDouble("SamplingRate", -1) != -1) {
								if (mCurrentDevice.equals("All Devices")) {
									Log.d("Shimmer", "Set Sampling Rate ALL LEDS");
									//mService.setAllSampingRate((data.getExtras().getDouble("SamplingRate",-1)));
								} else {
									//mService.writeSamplingRate(mCurrentDevice, (data.getExtras().getDouble("SamplingRate",-1)));
								}
							}

							if (data.getExtras().getInt("AccelRange", -1) != -1) {
								if (mCurrentDevice.equals("All Devices")) {
									Log.d("Shimmer", "Set AccelRange ALL LEDS");
									//mService.setAllAccelRange(data.getExtras().getInt("AccelRange",-1));
								} else {
									//mService.writeAccelRange(mCurrentDevice, data.getExtras().getInt("AccelRange",-1));
								}
							}

							if (data.getExtras().getInt("GSRRange", -1) != -1) {
								if (mCurrentDevice.equals("All Devices")) {
									Log.d("Shimmer", "Set ALL GSRRange");
									//mService.setAllGSRRange(data.getExtras().getInt("GSRRange",-1));
								} else {
									//mService.writeGSRRange(mCurrentDevice, data.getExtras().getInt("GSRRange",-1));
								}
							}

						}
					}
					break;
				case MainActivity.REQUEST_CONFIGURE_SHIMMER:
					if (resultCode == Activity.RESULT_OK) {
						if (mCurrentDevice.equals("All Devices")) {
							Log.d("Shimmer", "Configure Sensors ALL Devices");
							//mService.setAllEnabledSensors(data.getExtras().getInt(ConfigureActivity.mDone));

						} else {
							//mService.setEnabledSensors(data.getExtras().getInt(ConfigureActivity.mDone),mCurrentDevice);
						}
					}
					break;
			}
		}
	    
	    public void onPause(){
	  	  super.onPause();
	  	  
	  	Log.d("ShimmerH","MCA on Pause");
	  	 }
	    
	  public void onResume(){
	  	super.onResume();

	  	Intent intent=new Intent(MainCommandsActivity.this, MainActivity.class);
	  	Log.d("ShimmerH", "MCA on Resume");
	  }

}
