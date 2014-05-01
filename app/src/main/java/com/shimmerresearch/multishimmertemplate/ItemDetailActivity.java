package com.shimmerresearch.multishimmertemplate;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.service.MultiShimmerTemplateService;
import com.shimmerresearch.service.MultiShimmerTemplateService.LocalBinder;

import de.bogutzky.datacollector.app.R;
import de.bogutzky.datacollector.app.fragments.FlowFragment;

/**
 * An activity representing a single Item detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link ItemListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link ItemDetailFragment}.
 */
public class ItemDetailActivity extends FragmentActivity {

    MultiShimmerTemplateService mService;
    boolean mServiceBind = false;
    DatabaseHandler db;
    protected ServiceConnection mTestServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            // TODO Auto-generated method stub
            Log.d("ShimmerService", "service connected");
            LocalBinder binder = (com.shimmerresearch.service.MultiShimmerTemplateService.LocalBinder) service;
            mService = binder.getService();
            db = mService.mDataBase;
            mServiceBind = true;
            //update the view

            // this is needed because sometimes when there is an actitivity switch the service is not connecte yet, before the fragment was created, thus the fragment has no access to the service
            ControlFragment controlF = (ControlFragment) getSupportFragmentManager().findFragmentByTag("Control");
            ConfigurationFragment configF = (ConfigurationFragment) getSupportFragmentManager().findFragmentByTag("Configure");
            PlotFragment plotF = (PlotFragment) getSupportFragmentManager().findFragmentByTag("Plot");
            LogFragment logF = (LogFragment) getSupportFragmentManager().findFragmentByTag("Log");
            BlankFragment bF = (BlankFragment) getSupportFragmentManager().findFragmentByTag("Blank");
            PPGFragment ppgF = (PPGFragment) getSupportFragmentManager().findFragmentByTag("Heart Rate");
            FlowFragment flowFragment = (FlowFragment) getSupportFragmentManager().findFragmentByTag("Flow");

            if (controlF != null) {
                controlF.mService = mService;
                controlF.setup();
            } else if (configF != null) {
                configF.mService = mService;
                configF.setup();
            } else if (plotF != null) {
                plotF.mService = mService;
                plotF.setup();
            } else if (logF != null) {
                logF.mService = mService;
                logF.setup();
            } else if (flowFragment != null) {
                flowFragment.setMultiShimmerTemplateService(mService);
                flowFragment.setup();
            } else if (bF != null) {
                bF.mService = mService;
                bF.setup();
            } else if (ppgF != null) {
                ppgF.mService = mService;
                ppgF.setup();
            }

        }

        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
            mServiceBind = false;
        }
    };
    Dialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //

        dialog = new Dialog(this);
        /*if (!isMyServiceRunning())
        {
        	Log.d("ShimmerH","Oncreate2");
        	Intent intent=new Intent(this, MultiShimmerTemplateService.class);
        	startService(intent);

        } else {
        	Intent intent = new Intent(this, MultiShimmerTemplateService.class);
            bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);
        }
		*/
        Intent intent = new Intent(getApplicationContext(), MultiShimmerTemplateService.class);
        startService(intent);
        getApplicationContext().bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.


            if (getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID).equals("2")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));
                ConfigurationFragment fragment = new ConfigurationFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Configure").commit();
            } else if (getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID).equals("3")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));
                PlotFragment fragment = new PlotFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Plot").commit();
            } else if (getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID).equals("4")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));
                LogFragment fragment = new LogFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Log").commit();
            } else if (getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID).equals("5")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));
                PPGFragment fragment = new PPGFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Heart Rate").commit();
            } else if (getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID).equals("6")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));
                BlankFragment fragment = new BlankFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Blank").commit();
            } else if (getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID).equals("7")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));
                FlowFragment fragment = new FlowFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Flow").commit();
            } else if (getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID).equals("9")) {

            } else if (getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID).equals("1")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));
                ControlFragment fragment = new ControlFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Control").commit();
            } else {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));
                ItemDetailFragment fragment = new ItemDetailFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment).commit();
            }
        }
    }

    protected boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.shimmerresearch.service.MultiShimmerTemplateService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpTo(this,
                        new Intent(this, ItemListActivity.class));
                return true;
            case R.id.help:
                PPGFragment ppgFragment = (PPGFragment) getSupportFragmentManager().findFragmentByTag("Heart Rate");
                if (ppgFragment != null) {
                    showTextDialog("In order to use this, a PPG sensor should be connected to a Shimmer3 GSR+ board via Internal ADC13. Only 51.2Hz and 102.4Hz sampling rate is supported. Ensure the GSR and Internal ADC A13 sensor is enabled within the Configuration Panel. Finally Internal Exp Power should be enabled within this panel. Once the device starts streaming the calculated Heart Rate value will show after a 10 Second duration.", "PPG to Heart Rate Conversion");
                }

                LogFragment logFragment = (LogFragment) getSupportFragmentManager().findFragmentByTag("Log");
                if (logFragment != null) {
                    showTextDialog("This logging example will only log data from one Shimmer device. When logging do not change orientation of the screen or switch tabs. Remain in this view for the duration of your logging experiment.", "Logging Instructions");
                }

                return true;

        }
        return super.onOptionsItemSelected(item);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        ControlFragment controlF = (ControlFragment) getSupportFragmentManager().findFragmentByTag("Control");
        ConfigurationFragment configF = (ConfigurationFragment) getSupportFragmentManager().findFragmentByTag("Configure");
        PlotFragment plotF = (PlotFragment) getSupportFragmentManager().findFragmentByTag("Plot");
        LogFragment logF = (LogFragment) getSupportFragmentManager().findFragmentByTag("Log");
        PPGFragment ppgF = (PPGFragment) getSupportFragmentManager().findFragmentByTag("Heart Rate");
        FlowFragment flowFragment = (FlowFragment) getSupportFragmentManager().findFragmentByTag("Flow");

        if (controlF != null) {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        } else if (configF != null) {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        } else if (plotF != null) {
            getMenuInflater().inflate(R.menu.activity_main_plot, menu);
        } else if (logF != null) {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        } else if (flowFragment != null) {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        } else if (ppgF != null) {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        } else {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        }
        return true;
    }


    public void test() {

    }

    public void showTextDialog(String text, String title) {
        dialog.setContentView(R.layout.text_dialog);
        dialog.setTitle(title);
        dialog.setCancelable(true);
        dialog.show();
        TextView TVNew = (TextView) dialog.findViewById(R.id.textViewDialog);
        TVNew.setText(text);
    }

    public MultiShimmerTemplateService getService() {
        return mService;
    }
}
