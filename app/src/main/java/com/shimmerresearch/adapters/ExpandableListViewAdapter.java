package com.shimmerresearch.adapters;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.database.ShimmerConfiguration;
import com.shimmerresearch.multishimmertemplate.ConfigurationFragment;
import com.shimmerresearch.multishimmertemplate.ControlFragment;
import com.shimmerresearch.multishimmertemplate.PlotFragment;
import com.shimmerresearch.service.MultiShimmerTemplateService;
import com.shimmerresearch.service.MultiShimmerTemplateService.LocalBinder;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import de.bogutzky.datacollector.app.R;

public class ExpandableListViewAdapter extends BaseExpandableListAdapter {

    public final static int ENABLED_SENSORS_POSITION = 0;
    public final static int SAMPLING_RATE_POSITION = 1;
    public final static int ACCEL_RANGE_POSITION = 2;
    public final static int MAG_RANGE_POSITION = 3;
    public final static int GSR_RANGE_POSITION = 4;
    public final static int SET_DEVICE_NAME_POSITION = 5;
    public final static int SET_BLUETOOTH_ADDRESSS_POSITION = 6;
    public final static int DELETE_POSITION = 7;
    public final static int ENABLED_SENSORS_POSITION_S3 = 0;
    public final static int SAMPLING_RATE_POSITION_S3 = 1;
    public final static int ACCEL_OPTIONS_POSITION_S3 = 2;
    public final static int GYRO_OPTIONS_POSITION_S3 = 3;
    public final static int MAG_OPTIONS_POSITION_S3 = 4;
    public final static int PRES_OPTIONS_POSITION_S3 = 5;
    public final static int GSR_RANGE_POSITION_S3 = 6;
    public final static int INT_EXP_POWER_POSITION_S3 = 7;
    public final static int EXG_OPTIONS_POSITION_S3 = 8;
    public final static int SET_DEVICE_NAME_POSITION_S3 = 9;
    public final static int SET_BLUETOOTH_ADDRESSS_POSITION_S3 = 10;
    public final static int DELETE_POSITION_S3 = 11;
    public final static int CONNECT_DEVICE_POSITION = 0;
    public final static int DISCONNECT_DEVICE_POSITION = 1;
    public final static int TOGGLE_LED_POSITION = 2;
    public final static int START_STREAMING_POSITION = 3;
    public final static int STOP_STREAMING_POSITION = 4;
    private final Context context;
    public boolean mFirstTime = true;
    public boolean mFirstTimeChild = true;
    List<ShimmerConfiguration> mShimmerConfigurationList;
    MultiShimmerTemplateService mS;
    protected ServiceConnection mTestServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            // TODO Auto-generated method stub
            Log.d("ShimmerService", "service connected");
            LocalBinder binder = (MultiShimmerTemplateService.LocalBinder) service;
            mS = binder.getService();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
        }
    };
    Service ms;
    private String[][] mGroupChildren;
    private boolean[][] mGroupChildrenBoolean = null;
    private boolean[][] mGroupChildrenFormatBoolean = null;
    private int[][] mGroupChildrenColor;
    private String[] mGroupHeaders;
    private String[] mDeviceVersions;
    private boolean[] mGroupExpanded = null; // is true if expanded
    private ImageView imageViewUser;
    private ExpandableListView division;
    private String[] mGenderArray;
    private String[] mMiniTextArray;
    private boolean[] treatmentInDate;
    private int[] mNumberofChildren = null;
    private String callingClass, originalString, firstString, secondString;
    private StringTokenizer st;

    //control fragment
    public ExpandableListViewAdapter(String callingClass, String[] groups, String[] versions, String[][] children, Context context, ExpandableListView listViewUsers, int[] numberofChildren, String[] minitextArray, MultiShimmerTemplateService service, boolean[] expandableStates) {
        this.context = context;
        this.mGroupChildren = children;
        this.mGroupHeaders = groups;
        this.mDeviceVersions = versions;
        this.division = listViewUsers;
        this.callingClass = callingClass;
        this.mNumberofChildren = numberofChildren;
        this.mMiniTextArray = minitextArray;
        //mGroupExpanded=new boolean[groups.length];
        mGroupExpanded = expandableStates;
        //Arrays.fill(mGroupExpanded, false);
        mS = service;
        if (mGroupChildren.length != 0) {
            mGroupChildrenBoolean = new boolean[mGroupChildren.length][mGroupChildren[0].length];
            for (boolean[] row : mGroupChildrenBoolean)
                Arrays.fill(row, false);


            mGroupChildrenColor = new int[mGroupChildren.length][mGroupChildren[0].length];
            for (int[] row : mGroupChildrenColor)
                Arrays.fill(row, Color.rgb(0, 0, 0));
        }


        if (mS.getExapandableStates(callingClass) != null) {
            mGroupExpanded = mS.getExapandableStates(callingClass);

        } else {
            mGroupExpanded = new boolean[groups.length];
            Arrays.fill(mGroupExpanded, false);
        }


        //bindService();
    }

    //plot fragment
    public ExpandableListViewAdapter(String callingClass, String[] groups, String[][] children, Context context, ExpandableListView listViewUsers, int[] numberofChildren, String[] minitextArray, MultiShimmerTemplateService service, boolean[] expandableStates, boolean[][] checkBoxStates, int[][] groupChildrenColor, boolean[][] checkBoxFormatStates) {
        this.context = context;
        this.mGroupChildren = children;
        this.mGroupHeaders = groups;
        this.division = listViewUsers;
        this.callingClass = callingClass;
        this.mNumberofChildren = numberofChildren;
        this.mMiniTextArray = minitextArray;
        //mGroupExpanded=new boolean[groups.length];
        mGroupExpanded = expandableStates;
        //Arrays.fill(mGroupExpanded, false);
        mS = service;
        int max = 0;
        for (int i = 0; i < numberofChildren.length; i++) {
            if (max < numberofChildren[i]) {
                max = numberofChildren[i];
            }
        }
        if (mGroupChildren.length != 0) {
            if (checkBoxStates != null) {
                mGroupChildrenBoolean = checkBoxStates;
                mGroupChildrenFormatBoolean = checkBoxFormatStates;
            } else {
                mGroupChildrenBoolean = new boolean[mGroupChildren.length][max];
                mGroupChildrenFormatBoolean = new boolean[mGroupChildren.length][max];
                for (boolean[] row : mGroupChildrenBoolean) {
                    Arrays.fill(row, false);
                }

                for (boolean[] row : mGroupChildrenFormatBoolean) {
                    Arrays.fill(row, false);
                }

            }

        }

        if (groupChildrenColor != null) {
            mGroupChildrenColor = groupChildrenColor;
        } else {
            if (mGroupChildren.length != 0) {
                mGroupChildrenColor = new int[mGroupChildren.length][max];
                for (int[] row : mGroupChildrenColor) {
                    Arrays.fill(row, Color.rgb(0, 0, 0));
                }
            }
        }


        if (mS.getExapandableStates(callingClass) != null) {
            mGroupExpanded = mS.getExapandableStates(callingClass);
            if ((mGroupExpanded.length == 0 || mGroupExpanded.length != mGroupHeaders.length) && (mGroupChildren.length != 0)) {
                mGroupExpanded = new boolean[groups.length];
                Arrays.fill(mGroupExpanded, false);

                mGroupChildrenBoolean = new boolean[mGroupChildren.length][max];
                mGroupChildrenFormatBoolean = new boolean[mGroupChildren.length][max];
                mGroupChildrenColor = new int[mGroupChildren.length][max];

                for (int[] row : mGroupChildrenColor) {
                    Arrays.fill(row, Color.rgb(0, 0, 0));
                }

                for (boolean[] row : mGroupChildrenBoolean) {
                    Arrays.fill(row, false);
                }

                for (boolean[] row : mGroupChildrenFormatBoolean) {
                    Arrays.fill(row, false);
                }

                mGroupChildrenColor = new int[mGroupChildren.length][max];
                for (int[] row : mGroupChildrenColor) {
                    Arrays.fill(row, Color.rgb(0, 0, 0));
                }


            }
        } else {
            mGroupExpanded = new boolean[groups.length];
            Arrays.fill(mGroupExpanded, false);
        }


        //bindService();
    }

    public Object getChild(int groupPosition, int childPosition) {
        return mGroupChildren[groupPosition][childPosition];
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        if (mNumberofChildren != null) {
            return mNumberofChildren[groupPosition];
        } else {
            return mGroupChildren[groupPosition].length;
        }
    }

    public void bindService() {
        Intent intent = new Intent(this.context, MultiShimmerTemplateService.class);
        this.context.bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public TextView getGenericView() {
        @SuppressWarnings("deprecation")
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, 64);

        TextView textView = new TextView(context);
        textView.setLayoutParams(lp);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

        textView.setPadding(56, 0, 0, 0);
        return textView;
    }

    public boolean areAllItemsEnabled() {
        return false;
    }

    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View view, ViewGroup parent) {
        if (view == null) {
            LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (!callingClass.equals("PlotActivity")) {
                view = inf.inflate(R.layout.child_row, null);
            } else if (callingClass.equals("PlotActivity")) {
                view = inf.inflate(R.layout.child_row_cbox, null);
            }
            //view.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }


        view.setFocusable(false);
        view.setClickable(false);
        parent.setFocusable(false);
        parent.setClickable(false);

        if (!callingClass.equals("PlotActivity")) {
            TextView textView = (TextView) view.findViewById(R.id.grp_child);


            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            switch (metrics.densityDpi) {
                case DisplayMetrics.DENSITY_LOW:
                    textView.setTextSize(10);
                    break;
                case DisplayMetrics.DENSITY_MEDIUM:
                    textView.setTextSize(20);
                    break;
                case DisplayMetrics.DENSITY_HIGH:
                    textView.setTextSize(20);
                    break;
                case DisplayMetrics.DENSITY_XHIGH:
                    textView.setTextSize(22);
                    break;

            }


            textView.setFocusable(false);
            textView.setClickable(false);
            if (callingClass == "ConfigureActivity") {
                if (((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).getShimmerVersion(groupPosition) != Shimmer.SHIMMER_3) {
                    //convert Enabled Sensors number to String
                    if (childPosition == 0) {
                        originalString = getEnabledSensors(Integer.parseInt(getChild(groupPosition, childPosition).toString()), groupPosition);
                    } else if (childPosition == SAMPLING_RATE_POSITION) {
                        originalString = "Sampling Rate : " + getChild(groupPosition, childPosition).toString() + "Hz";
                    } else if (childPosition == ACCEL_RANGE_POSITION) {
                        if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 0) {
                            originalString = "Accel Range : +/- 1.5g";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 3) {
                            originalString = "Accel Range : +/- 6g";
                        } else {
                            originalString = "Accel Setting : Undefined";
                        }
                    } else if (childPosition == GSR_RANGE_POSITION) {
                        if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 0) {
                            originalString = "GSR Range : 10kOhm to 56kOhm";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 1) {
                            originalString = "GSR Range : 56kOhm to 220kOhm";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 2) {
                            originalString = "GSR Range : 220kOhm to 680kOhm";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 3) {
                            originalString = "GSR Range : 680kOhm to 4.7MOhm";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 4) {
                            originalString = "GSR Range : Auto Range";
                        } else {
                            originalString = "GSR Range : Undefined";
                        }
                    } else if (childPosition == MAG_RANGE_POSITION) {
                        if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 0) {
                            originalString = "Mag Settings :  +/- 0.8Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 1) {
                            originalString = "Mag Settings : +/- 1.3Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 2) {
                            originalString = "Mag Settings : +/- 1.9Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 3) {
                            originalString = "Mag Settings : +/- 2.5Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 4) {
                            originalString = "Mag Settings : +/- 4.0Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 5) {
                            originalString = "Mag Settings : +/- 4.7Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 6) {
                            originalString = "Mag Settings : +/- 5.6Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 7) {
                            originalString = "Mag Settings : +/- 8.1Ga";
                        } else {
                            originalString = "Mag Settings : undefined";
                        }
                    } else {
                        originalString = getChild(groupPosition, childPosition).toString();
                    }
                } else {
                    if (childPosition == 0) {
                        originalString = getEnabledSensors(Integer.parseInt(getChild(groupPosition, childPosition).toString()), groupPosition);
                    } else if (childPosition == SAMPLING_RATE_POSITION_S3) {
                        originalString = "Sampling Rate : " + getChild(groupPosition, childPosition).toString() + "Hz";
                    } else if (childPosition == ACCEL_OPTIONS_POSITION_S3) {
                        if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 0) {
                            originalString = "Accel Settings : +/- 2g";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 1) {
                            originalString = "Accel Settings : +/- 4g";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 2) {
                            originalString = "Accel Settings : +/- 8g";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 3) {
                            originalString = "Accel Settings : +/- 16g";
                        } else {
                            originalString = "Accel Settings : undefined";
                        }

                    } else if (childPosition == GYRO_OPTIONS_POSITION_S3) {
                        if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 0) {
                            originalString = "Gyro Settings : 250dps";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 1) {
                            originalString = "Gyro Settings : 500dps";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 2) {
                            originalString = "Gyro Settings : 1000dps";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 3) {
                            originalString = "Gyro Settings : 2000dps";
                        } else {
                            originalString = "Gyro Settings : undefined";
                        }
                    } else if (childPosition == MAG_OPTIONS_POSITION_S3) {
                        if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 1) {
                            originalString = "Mag Settings :  +/- 1.3Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 2) {
                            originalString = "Mag Settings : +/- 1.9Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 3) {
                            originalString = "Mag Settings : +/- 2.5Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 4) {
                            originalString = "Mag Settings : +/- 4.0Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 5) {
                            originalString = "Mag Settings : +/- 4.7Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 6) {
                            originalString = "Mag Settings : +/- 5.6Ga";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 7) {
                            originalString = "Mag Settings : +/- 8.1Ga";
                        } else {
                            originalString = "Mag Settings : undefined";
                        }
                    } else if (childPosition == GSR_RANGE_POSITION_S3) {
                        if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 0) {
                            originalString = "GSR Range : 10kOhm to 56kOhm";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 1) {
                            originalString = "GSR Range : 56kOhm to 220kOhm";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 2) {
                            originalString = "GSR Range : 220kOhm to 680kOhm";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 3) {
                            originalString = "GSR Range : 680kOhm to 4.7MOhm";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 4) {
                            originalString = "GSR Range : Auto Range";
                        }
                    } else if (childPosition == PRES_OPTIONS_POSITION_S3) {
                        if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 0) {
                            originalString = "Pressure Resolution :  Low";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 1) {
                            originalString = "Pressure Resolution :  Standard";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 2) {
                            originalString = "Pressure Resolution :  High";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 3) {
                            originalString = "Pressure Resolution :  XHigh";
                        }
                    } else if (childPosition == INT_EXP_POWER_POSITION_S3) {
                        if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 0) {
                            originalString = "Int Exp Power : Disabled";
                        } else if (Integer.parseInt(getChild(groupPosition, childPosition).toString()) == 1) {
                            originalString = "Int Exp Power : Enabled";
                        } else {
                            originalString = "Int Exp Power : Undefined";
                        }
                    } else {
                        originalString = getChild(groupPosition, childPosition).toString();
                    }
                }
            } else if (callingClass == "ControlActivity") {
                originalString = getChild(groupPosition, childPosition).toString();
            } else {
                originalString = getChild(groupPosition, childPosition).toString();
            }

            textView.setText(originalString);
        } else if (callingClass == "PlotActivity") {
            view.setFocusable(false);
            view.setClickable(false);
            final CheckBox cb = (CheckBox) view.findViewById(R.id.checkBoxPlot);
            final CheckBox cbformat = (CheckBox) view.findViewById(R.id.checkBoxPlotFormat);
            originalString = getChild(groupPosition, childPosition).toString();
            cb.setTag(R.id.PARENT_ID, groupPosition);
            cb.setTag(R.id.CHILD_ID, childPosition);
            cbformat.setTag(R.id.PARENT_ID, groupPosition);
            cbformat.setTag(R.id.CHILD_ID, childPosition);
            cb.setText(originalString);
            cb.setTextColor(mGroupChildrenColor[groupPosition][childPosition]);
            cb.setOnCheckedChangeListener(null);
            cbformat.setOnCheckedChangeListener(null);


            if (mGroupChildrenBoolean != null) {
                if (mGroupChildrenBoolean[groupPosition][childPosition] == false) {
                    cb.setChecked(false);
                } else {
                    cb.setChecked(true);
                    if (mFirstTimeChild == true) {
                        //if this is the first time regenerate the plot
                        //((PlotActivity)context).setFilteredSignals(groupPosition, childPosition, mMiniTextArray[groupPosition], mGroupChildren[groupPosition][childPosition]);
                        //((PlotActivity)context).setPlotFormat(mMiniTextArray[groupPosition], mGroupChildren[groupPosition][childPosition], mGroupChildrenColor[groupPosition][childPosition]);
                        cb.setTextColor(mGroupChildrenColor[groupPosition][childPosition]);

                    }
                }
            }

            if (mGroupChildrenFormatBoolean != null) {
                if (mGroupChildrenFormatBoolean[groupPosition][childPosition] == false) {
                    cbformat.setChecked(false);
                } else {
                    cbformat.setChecked(true);
                }
            }


            cbformat.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton cBox,
                                             boolean boxChecked) {
                    // TODO Auto-generated method stub
                    mGroupChildrenFormatBoolean[groupPosition][childPosition] = boxChecked;
                    if (boxChecked) {
                        ((PlotFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setSensorsforPlotFormat(groupPosition, childPosition, boxChecked);

                    } else {
                        ((PlotFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setSensorsforPlotFormat(groupPosition, childPosition, boxChecked);

                    }
                }

            });


            cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton cBox,
                                             boolean boxChecked) {
                    // TODO Auto-generated method stub
                    Log.d("ShimmerCB", cBox.getTag(R.id.PARENT_ID).toString() + ":" + cBox.getTag(R.id.CHILD_ID).toString());
                    //call a function in PlotActivity to plot the appropriate sensors
                    mGroupChildrenBoolean[groupPosition][childPosition] = boxChecked;
                    if (boxChecked) {
                        ((PlotFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setFilteredSignals(groupPosition, childPosition, mMiniTextArray[groupPosition], mGroupChildren[groupPosition][childPosition]);
                        mGroupChildrenColor[groupPosition][childPosition] = Color.rgb((int) (255 * Math.random()), (int) (255 * Math.random()), (int) (255 * Math.random()));
                        ((PlotFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setPlotFormat(mMiniTextArray[groupPosition], mGroupChildren[groupPosition][childPosition], mGroupChildrenColor[groupPosition][childPosition]);
                        cb.setTextColor(mGroupChildrenColor[groupPosition][childPosition]);

                    } else {
                        ((PlotFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setFilteredSignals(groupPosition, childPosition, "", "");
                        mGroupChildrenColor[groupPosition][childPosition] = Color.BLACK;
                        cb.setTextColor(mGroupChildrenColor[groupPosition][childPosition]);

                    }
                }


            });
        }

        if (groupPosition == mGroupHeaders.length - 1 && childPosition == mNumberofChildren[groupPosition]) {
            mFirstTimeChild = false;
        }

        return view;
    }

    public Object getGroup(int groupPosition) {
        return mGroupHeaders[groupPosition];
    }

    public int getGroupCount() {
        return mGroupHeaders.length;
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, final boolean isExpanded, View viewGroup, ViewGroup parent) {
        //TextView textView = getGenericView();

        if (viewGroup == null) {
            LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            viewGroup = inf.inflate(R.layout.group_row, null);
        }

        //expandIcon = (ImageView) view.findViewById(R.id.expandicon);
        imageViewUser = (ImageView) viewGroup.findViewById(R.id.usericon);
        TextView textViewDeviceName = (TextView) viewGroup.findViewById(R.id.row_name);
        TextView textViewBluetoothAddress = (TextView) viewGroup.findViewById(R.id.name);
        TextView textViewDeviceState = (TextView) viewGroup.findViewById(R.id.state);


        //null means this is the first time you are opening this

        if (mFirstTime) {
            if (mGroupExpanded[groupPosition] == true) {
                division.expandGroup(groupPosition);
            } else {
                division.collapseGroup(groupPosition);
            }
            // also check to see if there are any graphs to be plotted
            if (callingClass.equals("PlotActivity")) {
                for (int i = 0; i < mNumberofChildren[groupPosition]; i++) {
                    if (mGroupChildrenBoolean[groupPosition][i] == true) {

                        ((PlotFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setFilteredSignals(groupPosition, i, mMiniTextArray[groupPosition], mGroupChildren[groupPosition][i]);
                        //mGroupChildrenColor[groupPosition][i]=Color.rgb((int)(255*Math.random()), (int)(255*Math.random()), (int)(255*Math.random()));
                        ((PlotFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setPlotFormat(mMiniTextArray[groupPosition], mGroupChildren[groupPosition][i], mGroupChildrenColor[groupPosition][i]);

                    }
                    if (mGroupChildrenFormatBoolean[groupPosition][i] == true) {
                        ((PlotFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setSensorsforPlotFormat(groupPosition, i, mGroupChildrenFormatBoolean[groupPosition][i]);
                    }

                }
            }
        }


        if (mGroupExpanded != null && mFirstTime != true) {
            if (!isExpanded) {
                //expandIcon.setImageResource(R.drawable.expandtwo);
                // imageViewUser.setImageResource(R.drawable.expandthree);
                mGroupExpanded[groupPosition] = false;
            } else {
                mGroupExpanded[groupPosition] = true;
                //expandIcon.setImageResource(R.drawable.unexpandtwo);
                // imageViewUser.setImageResource(R.drawable.unexpandthree);
            }
        }


        if (callingClass.equals("ControlActivity")) {
            //not sure if this is the best way to do it, may require future revision
            //send the view back to the main control so it can update the images for connection, transmission and so on

        }

        //expandIcon.setFocusable(false);
        //expandIcon.setTag(groupPosition);
        imageViewUser.setFocusable(false);
        imageViewUser.setTag(groupPosition);

        viewGroup.setTag(groupPosition);
        viewGroup.setBackgroundColor(Color.parseColor("#F0F0F0"));
        viewGroup.setClickable(true);
        viewGroup.setFocusable(true);
        viewGroup.setEnabled(true);
        viewGroup.setActivated(true);

			 /*
			 Integer realPosition = (Integer) imageView.getTag();
			 onGroupCollapse(realPosition);
			 System.out.println("Group " +realPosition+ "collapsed");
			 */

        if (callingClass.equals("ControlActivity") || callingClass.equals("ConfigureActivity") || callingClass.equals("PlotActivity")) {
            textViewDeviceName.setText(mGroupHeaders[groupPosition]);
            textViewBluetoothAddress.setText(mMiniTextArray[groupPosition]);

            if (groupPosition != 0 && (callingClass.equals("ControlActivity"))) {
                if (mS.getShimmerState(mMiniTextArray[groupPosition]) == -1) {
                    textViewDeviceState.setText("Disconnected");
                    if (mGroupExpanded[groupPosition])
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_disconnected_selected);
                    else
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_disconnected_unselected);
                } else if (mS.getShimmerState(mMiniTextArray[groupPosition]) == 1 || (mS.getShimmerState(mMiniTextArray[groupPosition]) == 2) && !mS.isShimmerInitialized(mMiniTextArray[groupPosition])) {
                    textViewDeviceState.setText("Connecting");
                    if (mGroupExpanded[groupPosition])
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_connecting_selected);
                    else
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_connecting_unselected);
                } else if (mS.getShimmerState(mMiniTextArray[groupPosition]) == 2 && !mS.deviceStreaming(mMiniTextArray[groupPosition]) && mS.isShimmerInitialized(mMiniTextArray[groupPosition])) {
                    textViewDeviceState.setText("Connected");
                    if (mGroupExpanded[groupPosition])
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_connected_selected);
                    else
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_connected_unselected);
                } else if (mS.getShimmerState(mMiniTextArray[groupPosition]) == 2 && mS.deviceStreaming(mMiniTextArray[groupPosition])) {
                    DecimalFormat df = new DecimalFormat("#.00");
                    textViewDeviceState.setText("Streaming" + " ( Reception Rate: " + df.format(mS.getPacketReceptionRate(mMiniTextArray[groupPosition])) + "%)");
                    if (mGroupExpanded[groupPosition])
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_streaming_selected);
                    else
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_streaming_unselected);
                }
            } else if (callingClass.equals("ConfigureActivity") || callingClass.equals("PlotActivity")) {
                if (mS.getShimmerState(mMiniTextArray[groupPosition]) == -1) {
                    textViewDeviceState.setText("Disconnected");
                    if (mGroupExpanded[groupPosition])
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_disconnected_selected);
                    else
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_disconnected_unselected);
                } else if (mS.getShimmerState(mMiniTextArray[groupPosition]) == 1 || (mS.getShimmerState(mMiniTextArray[groupPosition]) == 2) && !mS.isShimmerInitialized(mMiniTextArray[groupPosition])) {
                    textViewDeviceState.setText("Connecting...");
                    if (mGroupExpanded[groupPosition])
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_connecting_selected);
                    else
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_connecting_unselected);
                } else if (mS.getShimmerState(mMiniTextArray[groupPosition]) == 2 && !mS.deviceStreaming(mMiniTextArray[groupPosition]) && mS.isShimmerInitialized(mMiniTextArray[groupPosition])) {
                    textViewDeviceState.setText("Connected");
                    if (mGroupExpanded[groupPosition])
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_connected_selected);
                    else
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_connected_unselected);
                } else if (mS.getShimmerState(mMiniTextArray[groupPosition]) == 2 && mS.deviceStreaming(mMiniTextArray[groupPosition])) {
                    DecimalFormat df = new DecimalFormat("#.00");
                    textViewDeviceState.setText("Streaming" + " (" + df.format(mS.getPacketReceptionRate(mMiniTextArray[groupPosition])) + "%)");
                    if (mGroupExpanded[groupPosition])
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_streaming_selected);
                    else
                        imageViewUser.setBackgroundResource(R.drawable.shimmer_status_streaming_unselected);
                }
            } else {
                textViewDeviceState.setText("");
            }
            if (mS.isShimmerConnected(mMiniTextArray[groupPosition])) { //minitextarray holds the bluetooth address
                imageViewUser.setImageResource(R.drawable.locker_selected);
            } else {
                imageViewUser.setImageResource(R.drawable.locker_default);
            }
            Log.d("ShimmerD", Integer.toString(groupPosition));

        }
			 
/*			 division.setOnGroupClickListener(new OnGroupClickListener() {
			        
				 public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
					 Log.d("ShimmerMEL", Integer.toString(groupPosition));
					 
					 if(callingClass.equals("ControlActivity")||callingClass.equals("ConfigureActivity")){
						 if (context instanceof ConfigureActivity && groupPosition!=0){ //!=0 cant delete the first entry which is all devices
						//	 ((ConfigureActivity)context).deleteShimmerFromList(groupPosition-1);
						 }
					 }
					 
					 return true;
				 }
			 });*/

        division.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                // TODO Auto-generated method stub

                return false;
            }

        });

        division.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int arg2, long arg3) {
                Log.d("ShimmerMEL", "works");
                return false;
            }
        });


        division.setOnChildClickListener(new OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView arg0, View view,
                                        int groupPosition, int childPosition, long arg4) {
                // TODO Auto-generated method stub
                Log.d("ShimmerCC", Integer.toString(groupPosition) + "-" + Integer.toString(childPosition));
                //mService.testService();
                if (callingClass.equals("ControlActivity") || callingClass.equals("ConfigureActivity")) {
                    if (callingClass.equals("ConfigureActivity")) {
                        if (((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).getShimmerVersion(groupPosition) != Shimmer.SHIMMER_3) {
                            //!=0 cant delete the first entry which is all devices
                            if (childPosition == SET_DEVICE_NAME_POSITION) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setDeviceName(groupPosition, mGroupHeaders[groupPosition]);
                            } else if (childPosition == DELETE_POSITION) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).deleteShimmerFromList(groupPosition);
                            } else if (childPosition == SET_BLUETOOTH_ADDRESSS_POSITION) {
                                if (!((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).isShimmerConnected(mMiniTextArray[groupPosition])) {
                                    ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setBluetoothAddress(groupPosition);
                                } else {
                                    Toast.makeText(((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).getActivity(), "Disconnect Device first before changing Bluetooth Address", Toast.LENGTH_LONG).show();
                                }
                            } else if (childPosition == SAMPLING_RATE_POSITION) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("SamplingRate", groupPosition);
                            } else if (childPosition == ACCEL_RANGE_POSITION) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("Accelerometer", groupPosition);
                            } else if (childPosition == GSR_RANGE_POSITION) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("GSR", groupPosition);
                            } else if (childPosition == ENABLED_SENSORS_POSITION) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setSensors(groupPosition);
                            } else if (childPosition == MAG_RANGE_POSITION) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("Magnetometer", groupPosition);
                            }
                        } else {
                            //!=0 cant delete the first entry which is all devices
                            if (childPosition == SET_DEVICE_NAME_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setDeviceName(groupPosition, mGroupHeaders[groupPosition]);
                            } else if (childPosition == DELETE_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).deleteShimmerFromList(groupPosition);
                            } else if (childPosition == SET_BLUETOOTH_ADDRESSS_POSITION_S3) {
                                if (!((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).isShimmerConnected(mMiniTextArray[groupPosition])) {
                                    ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setBluetoothAddress(groupPosition);
                                } else {
                                    Toast.makeText(((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).getActivity(), "Disconnect Device first before changing Bluetooth Address", Toast.LENGTH_LONG).show();
                                }
                            } else if (childPosition == SAMPLING_RATE_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("SamplingRate", groupPosition);
                            } else if (childPosition == ACCEL_OPTIONS_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("Accelerometer", groupPosition);
                            } else if (childPosition == GYRO_OPTIONS_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("Gyroscope", groupPosition);
                            } else if (childPosition == MAG_OPTIONS_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("Magnetometer", groupPosition);
                            } else if (childPosition == PRES_OPTIONS_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("Pressure", groupPosition);
                            } else if (childPosition == ENABLED_SENSORS_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).setSensors(groupPosition);
                            } else if (childPosition == GSR_RANGE_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("GSR", groupPosition);
                            } else if (childPosition == EXG_OPTIONS_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("EXG", groupPosition);
                            } else if (childPosition == INT_EXP_POWER_POSITION_S3) {
                                ((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).configureShimmer("Int Exp Power", groupPosition);
                            }
                        }

                    }

                    if (callingClass.equals("ControlActivity") && groupPosition != 0) { //!=0 cant delete the first entry which is all devices
                        if (childPosition == CONNECT_DEVICE_POSITION) {
                            ((ControlFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).connectShimmer(groupPosition - 1);
                        } else if (childPosition == DISCONNECT_DEVICE_POSITION) {
                            ((ControlFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).disconnectShimmer(groupPosition - 1);
                        } else if (childPosition == TOGGLE_LED_POSITION) {
                            ((ControlFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).toggleLed(groupPosition - 1);
                        } else if (childPosition == START_STREAMING_POSITION) {
                            ((ControlFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).startStreaming(groupPosition - 1);
                        } else if (childPosition == STOP_STREAMING_POSITION) {
                            ((ControlFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).stopStreaming(groupPosition - 1);
                        }
                    } else if (callingClass.equals("ControlActivity") && groupPosition == 0) {
                        if (childPosition == CONNECT_DEVICE_POSITION) {
                            ((ControlFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).connectAllShimmer();
                        } else if (childPosition == DISCONNECT_DEVICE_POSITION) {
                            ((ControlFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).disconnectAllShimmer();
                        } else if (childPosition == TOGGLE_LED_POSITION) {
                            ((ControlFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).toggleAllLeds();
                        } else if (childPosition == START_STREAMING_POSITION) {
                            ((ControlFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).startStreamingAllDevices();
                        } else if (childPosition == STOP_STREAMING_POSITION) {
                            ((ControlFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).stopStreamingAllDevices();
                        }
                    }
                }

                return false;
            }

        });

        viewGroup.setOnClickListener(new OnClickListener() {

            public void onClick(View view) {
                mFirstTime = false;
                Integer groupPosition = (Integer) view.getTag();
                System.out.println(groupPosition);
                Log.d("ShimmerELV", Integer.toString(groupPosition));
                if (mGroupExpanded[groupPosition] == true) {
                    onGroupCollapse(groupPosition);
                } else {
                    onGroupExpanded(groupPosition);
                }
            }
        });

        if (groupPosition == mGroupHeaders.length - 1) {
            mFirstTime = false;
        }

        return viewGroup;
    }


    @Override
    public void onGroupExpanded(int groupPosition) {
        //collapse the old expanded group, if not the same
        //as new group to expand
        division.expandGroup(groupPosition);
    }

    public void onGroupCollapse(int groupPosition) {
        division.collapseGroup(groupPosition);
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return true;
    }

    public boolean handleItemClick(View v, int position, long id) {
        return false;
    }

    public String getEnabledSensors(int enabledSensors, int groupPosition) {
        String enabledSensorNames = "Enabled Sensors: ";

        if (((enabledSensors & 0xFF) & Shimmer.SENSOR_ACCEL) > 0 || ((enabledSensors & 0xFFFF) & Shimmer.SENSOR_DACCEL) > 0) {
            enabledSensorNames = enabledSensorNames + "Accelerometer ";
        }
        if (((enabledSensors & 0xFF) & Shimmer.SENSOR_GYRO) > 0) {
            enabledSensorNames = enabledSensorNames + "Gyroscope ";
        }
        if (((enabledSensors & 0xFF) & Shimmer.SENSOR_MAG) > 0) {
            enabledSensorNames = enabledSensorNames + "Magnetometer ";
        }
        if (((enabledSensors & 0xFF) & Shimmer.SENSOR_GSR) > 0) {
            enabledSensorNames = enabledSensorNames + "GSR ";
        }
        if (((ConfigurationFragment) ((FragmentActivity) context).getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).getShimmerVersion(groupPosition) != Shimmer.SHIMMER_3) {
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_ECG) > 0) {
                enabledSensorNames = enabledSensorNames + "ECG ";
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_EMG) > 0) {
                enabledSensorNames = enabledSensorNames + "EMG ";
            }
        } else {
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_EXG1_24BIT) > 0) {
                enabledSensorNames = enabledSensorNames + "EXG1 ";
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_EXG2_24BIT) > 0) {
                enabledSensorNames = enabledSensorNames + "EXG2 ";
            }
            if ((enabledSensors & Shimmer.SENSOR_EXG1_16BIT) > 0) {
                enabledSensorNames = enabledSensorNames + "EXG1-16Bit ";
            }
            if ((enabledSensors & Shimmer.SENSOR_EXG2_16BIT) > 0) {
                enabledSensorNames = enabledSensorNames + "EXG2-16Bit ";
            }
        }

        if (((enabledSensors & 0xFF00) & Shimmer.SENSOR_STRAIN) > 0) {
            enabledSensorNames = enabledSensorNames + "StrainGauge ";
        }
        if (((enabledSensors & 0xFF00) & Shimmer.SENSOR_HEART) > 0) {
            enabledSensorNames = enabledSensorNames + "HeartRate ";
        }
        if (((enabledSensors & 0xFF) & Shimmer.SENSOR_EXP_BOARD_A0) > 0) {
            enabledSensorNames = enabledSensorNames + "ADC ";
        }
        if (((enabledSensors & 0xFF) & Shimmer.SENSOR_EXP_BOARD_A7) > 0) {
            enabledSensorNames = enabledSensorNames + "ADC ";
        }
        if ((enabledSensors & Shimmer.SENSOR_BMP180) > 0) {
            enabledSensorNames = enabledSensorNames + "Pressure ";
        }
        if ((enabledSensors & Shimmer.SENSOR_ALL_ADC_SHIMMER3) > 0) {
            enabledSensorNames = enabledSensorNames + "ADC ";
        }
        return enabledSensorNames;
    }

    public void test() {

    }

    public void updateGroupChildren(String[][] groupChildren) {
        mGroupChildren = groupChildren;
    }

    public void updateGroupHeaders(String[] groupHeaders) {
        mGroupHeaders = groupHeaders;
    }

    public void updatedata(String[] groups, String[] versions, String[][] children, int[] numberofChildren, String[] minitextArray) {
        this.mGroupChildren = children;
        this.mGroupHeaders = groups;
        this.mNumberofChildren = numberofChildren;
        this.mMiniTextArray = minitextArray;
        this.mDeviceVersions = versions;

        boolean[] groupExpandedTemp = mGroupExpanded;
        mGroupExpanded = new boolean[mGroupHeaders.length];
        if (groupExpandedTemp.length < mGroupExpanded.length) {
            for (int i = 0; i < groupExpandedTemp.length; i++) {
                mGroupExpanded[i] = groupExpandedTemp[i];
            }
        }

    }

    public boolean[] getExpandableStates() {
        return mGroupExpanded;
    }

    public boolean[][] getCheckBoxStates() {
        return mGroupChildrenBoolean;
    }

    public boolean[][] getCheckBoxFormatStates() {
        return mGroupChildrenBoolean;
    }

    public int[][] getGroupChildColor() {
        return mGroupChildrenColor;
    }

    public void updatedata(String[] groups, String[] versions, String[][] children, int[] numberofChildren, String[] minitextArray, List<ShimmerConfiguration> shimmerConfigurationList) {
        this.mGroupChildren = children;
        this.mGroupHeaders = groups;
        this.mDeviceVersions = versions;
        this.mNumberofChildren = numberofChildren;
        this.mMiniTextArray = minitextArray;
        this.mShimmerConfigurationList = shimmerConfigurationList;
        boolean[] groupExpandedTemp = mGroupExpanded;
        mGroupExpanded = new boolean[mGroupHeaders.length];
        if (groupExpandedTemp.length < mGroupExpanded.length) {
            for (int i = 0; i < groupExpandedTemp.length; i++) {
                mGroupExpanded[i] = groupExpandedTemp[i];
            }
        }

    }
}
