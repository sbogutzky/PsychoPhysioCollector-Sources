package com.shimmerresearch.multishimmertemplate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.driver.Configuration;

import java.util.ArrayList;
import java.util.Arrays;

import de.bogutzky.datacollector.app.R;

public class CommandsActivity extends Activity {
    public static String mDone = "Done";
    String mAttribute;
    String[] mValues = {""};
    double mAttributeValue;
    double mShimmerVersion;
    TextView mTVAttribute;
    CheckBox mChechBox;
    int mPosition;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Bundle extras = getIntent().getExtras();
        mShimmerVersion = extras.getInt("ShimmerVersion");
        mAttributeValue = extras.getDouble("AttributeValue");
        mAttribute = extras.getString("Attribute");
        mPosition = extras.getInt("Position");
        if (mShimmerVersion != Shimmer.SHIMMER_3) {
            if (mAttribute.equals("Magnetometer")) {
                setContentView(R.layout.select_from_list_with_topcb);
                mChechBox = (CheckBox) findViewById(R.id.checkBoxLowPower);
                if (extras.getInt("MagLowPower", -1) != -1) {
                    if (extras.getInt("MagLowPower", -1) == 1) {
                        mChechBox.setChecked(true);
                    } else {
                        mChechBox.setChecked(false);
                    }
                }
                mChechBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                        // TODO Auto-generated method stub
                        Intent intent = new Intent();
                        intent.putExtra("Position", mPosition);
                        if (checked) {
                            intent.putExtra("LowPower", 1);
                        } else {
                            intent.putExtra("LowPower", 0);
                        }
                        intent.putExtra("Attribute", mAttribute);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                });

            } else {
                setContentView(R.layout.select_from_list);
            }
        } else {

            if (mAttribute.equals("Accelerometer") || mAttribute.equals("Gyroscope") || mAttribute.equals("Magnetometer")) {
                setContentView(R.layout.select_from_list_with_topcb);
                mChechBox = (CheckBox) findViewById(R.id.checkBoxLowPower);
                if (mAttribute.equals("Accelerometer")) {
                    if (extras.getInt("AccelLowPower", -1) != -1) {
                        if (extras.getInt("AccelLowPower", -1) == 1) {
                            mChechBox.setChecked(true);
                        } else {
                            mChechBox.setChecked(false);
                        }
                    }
                } else if (mAttribute.equals("Gyroscope")) {
                    if (extras.getInt("GyroLowPower", -1) != -1) {
                        if (extras.getInt("GyroLowPower", -1) == 1) {
                            mChechBox.setChecked(true);
                        } else {
                            mChechBox.setChecked(false);
                        }
                    }
                } else if (mAttribute.equals("Magnetometer")) {
                    if (extras.getInt("MagLowPower", -1) != -1) {
                        if (extras.getInt("MagLowPower", -1) == 1) {
                            mChechBox.setChecked(true);
                        } else {
                            mChechBox.setChecked(false);
                        }
                    }
                }

                mChechBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                        // TODO Auto-generated method stub
                        Intent intent = new Intent();
                        intent.putExtra("Position", mPosition);
                        if (checked) {
                            intent.putExtra("LowPower", 1);
                        } else {
                            intent.putExtra("LowPower", 0);
                        }
                        intent.putExtra("Attribute", mAttribute);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                });
            } else {
                setContentView(R.layout.select_from_list);
            }

        }

        mTVAttribute = (TextView) findViewById(R.id.textViewAttribute);
        final ListView listViewAttributes = (ListView) findViewById(R.id.listViewAttributes);


        if (mAttribute.equals("Accelerometer")) {
            setTitle("Set Accel Range");
            mTVAttribute.setText("Select Accel Range :");
            if (mShimmerVersion != Shimmer.SHIMMER_3) {
                mValues = Configuration.Shimmer2.ListofAccelRange;
            } else {
                mValues = Configuration.Shimmer3.ListofAccelRange;
            }
        } else if (mAttribute.equals("SamplingRate")) {
            setTitle("Set Sampling Rate (Hz)");
            mTVAttribute.setText("Select Sampling Rate (Hz) :");
            mValues = new String[]{"10", "51.2", "102.4", "128", "170.7", "204.8", "256", "512", "1024"};
        } else if (mAttribute.equals("GSR")) {
            setTitle("Set GSR Range");
            mTVAttribute.setText("Select GSR Range :");
            mValues = new String[]{"10kOhm to 56kOhm", "56kOhm to 220kOhm", "220kOhm to 680kOhm", "680kOhm to 4.7MOhm", "Auto Range"};
        } else if (mAttribute.equals("Gyroscope")) {
            setTitle("Set Gyro Range");
            mTVAttribute.setText("Select Gyro Range :");
            mValues = Configuration.Shimmer3.ListofGyroRange;
        } else if (mAttribute.equals("Magnetometer")) {
            setTitle("Set Mag Range");
            mTVAttribute.setText("Select Mag Range :");
            if (mShimmerVersion != Shimmer.SHIMMER_3) {
                mValues = Configuration.Shimmer2.ListofMagRange;
            } else {
                mValues = Configuration.Shimmer3.ListofMagRange;
            }
        } else if (mAttribute.equals("Pressure")) {
            mValues = Configuration.Shimmer3.ListofPressureResolution;
            mTVAttribute.setText("Select Pressure Resolution :");
        } else if (mAttribute.equals("EXG")) {
            mValues = Configuration.Shimmer3.ListofDefaultEXG;
            mTVAttribute.setText("Select EXG Setting :");
        } else if (mAttribute.equals("Int Exp Power")) {
            setTitle("Enable Int Exp Power");
            mTVAttribute.setText("Enable Int Exp Power :");
            if (mShimmerVersion == Shimmer.SHIMMER_3) {
                mValues = new String[2];
                mValues[0] = "Disable";
                mValues[1] = "Enable";

            }
        }

        ArrayList<String> samplingRateList = new ArrayList<String>();
        samplingRateList.addAll(Arrays.asList(mValues));
        ArrayAdapter<String> sR = new ArrayAdapter<String>(CommandsActivity.this, R.layout.attribute_name, samplingRateList);
        listViewAttributes.setAdapter(sR);


        listViewAttributes.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Intent intent = new Intent();
                intent.putExtra("Position", mPosition);

                if (mAttribute.equals("Accelerometer")) {
                    Object o = listViewAttributes.getItemAtPosition(position);
                    Log.d("Shimmer", o.toString());
                    int accelRange = 0;
                    if (mShimmerVersion != Shimmer.SHIMMER_3) {
                        if (o.toString() == "+/- 1.5g") {
                            accelRange = 0;
                        } else if (o.toString() == "+/- 6g") {
                            accelRange = 3;
                        }
                    } else {
                        if (o.toString() == "+/- 2g") {
                            accelRange = 0;
                        } else if (o.toString() == "+/- 4g") {
                            accelRange = 1;
                        } else if (o.toString() == "+/- 8g") {
                            accelRange = 2;
                        } else if (o.toString() == "+/- 16g") {
                            accelRange = 3;
                        }
                    }
                    intent.putExtra("AccelRange", accelRange);
                    // Set result and finish this Activity

                } else if (mAttribute.equals("SamplingRate")) {
                    Object o = listViewAttributes.getItemAtPosition(position);
                    Log.d("Shimmer", o.toString());
                    intent.putExtra("SamplingRate", Double.valueOf(o.toString()).doubleValue());
                    // Set result and finish this Activity

                } else if (mAttribute.equals("GSR")) {
                    Object o = listViewAttributes.getItemAtPosition(position);
                    Log.d("Shimmer", o.toString());
                    int gsrRange = 0;
                    if (o.toString() == "10kOhm to 56kOhm") {
                        gsrRange = 0;
                    } else if (o.toString() == "56kOhm to 220kOhm") {
                        gsrRange = 1;
                    } else if (o.toString() == "220kOhm to 680kOhm") {
                        gsrRange = 2;
                    } else if (o.toString() == "680kOhm to 4.7MOhm") {
                        gsrRange = 3;
                    } else if (o.toString() == "Auto Range") {
                        gsrRange = 4;
                    }
                    intent.putExtra("GSRRange", gsrRange);

                } else if (mAttribute.equals("Gyroscope")) {
                    Object o = listViewAttributes.getItemAtPosition(position);
                    Log.d("Shimmer", o.toString());
                    int gyroRange = 0;
                    if (o.toString() == "250dps") {
                        gyroRange = 0;
                    } else if (o.toString() == "500dps") {
                        gyroRange = 1;
                    } else if (o.toString() == "100dps") {
                        gyroRange = 2;
                    } else if (o.toString() == "2000dps") {
                        gyroRange = 3;
                    }
                    intent.putExtra("GyroRange", gyroRange);

                } else if (mAttribute.equals("Magnetometer")) {
                    Object o = listViewAttributes.getItemAtPosition(position);
                    Log.d("Shimmer", o.toString());
                    if (mShimmerVersion == Shimmer.SHIMMER_3) {
                        int magRange = position + 1;
                        intent.putExtra("MagRange", magRange);
                    } else {
                        int magRange = position;
                        intent.putExtra("MagRange", magRange);
                    }
                } else if (mAttribute.equals("Pressure")) {
                    Object o = listViewAttributes.getItemAtPosition(position);
                    Log.d("Shimmer", o.toString());
                    int presRes = position;
                    intent.putExtra("PressureResolution", presRes);

                } else if (mAttribute.equals("Int Exp Power")) {
                    Object o = listViewAttributes.getItemAtPosition(position);
                    Log.d("Shimmer", o.toString());
                    int intexppow = position;
                    intent.putExtra("IntExpPower", intexppow);
                } else if (mAttribute.equals("EXG")) {
                    Object o = listViewAttributes.getItemAtPosition(position);
                    intent.putExtra("EXG", o.toString());
                }
                setResult(Activity.RESULT_OK, intent);
                finish();
            }

        });
    }


}
