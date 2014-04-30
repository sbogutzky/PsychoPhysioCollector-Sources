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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.service.MultiShimmerTemplateService;
import com.shimmerresearch.service.MultiShimmerTemplateService.LocalBinder;

import de.bogutzky.datacollector.app.R;

/**
 * An activity representing a list of Items. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link ItemDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ItemListFragment} and the item details (if present) is a
 * {@link ItemDetailFragment}.
 * <p/>
 * This activity also implements the required {@link ItemListFragment.Callbacks}
 * interface to listen for item selections.
 */
public class ItemListActivity extends FragmentActivity implements
        ItemListFragment.Callbacks {

    public static final String ARG_ITEM_ID = "item_id";
    protected boolean mServiceFirstTime = true;
    protected String tempConfigurationName = "Temp";
    MultiShimmerTemplateService mService;
    boolean mServiceBind = false;
    Dialog dialog;
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
            } else {

            }


        }

        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
            mServiceBind = false;
        }
    };
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);
        dialog = new Dialog(this);
        Log.d("ShimmerH", "Oncreate2");
        Intent intent = new Intent(this, MultiShimmerTemplateService.class);
        startService(intent);
        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ItemListFragment) getSupportFragmentManager().findFragmentById(
                    R.id.item_list)).setActivateOnItemClick(true);
        }
        // TODO: If exposing deep links into your app, handle intents here.
    }

    /**
     * Callback method from {@link ItemListFragment.Callbacks} indicating that
     * the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (id.equals("2")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
                ConfigurationFragment fragment = new ConfigurationFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Configure").commit();
            } else if (id.equals("3")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
                PlotFragment fragment = new PlotFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Plot").commit();
            } else if (id.equals("4")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
                LogFragment fragment = new LogFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Log").commit();
            } else if (id.equals("5")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
                PPGFragment fragment = new PPGFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Heart Rate").commit();
            } else if (id.equals("6")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
                BlankFragment fragment = new BlankFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Blank").commit();
            } else if (id.equals("1")) {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
                ControlFragment fragment = new ControlFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment, "Control").commit();
            } else {
                Bundle arguments = new Bundle();
                arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
                ItemDetailFragment fragment = new ItemDetailFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.item_detail_container, fragment).commit();
            }

        } else {
            // This is for small screens
            Intent detailIntent = new Intent(this, ItemDetailActivity.class);
            detailIntent.putExtra(ItemListActivity.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

    public MultiShimmerTemplateService getService() {
        return mService;
    }

    public void test() {
        Log.d("ShimmerTest", "test");
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

    public void onPause() {
        super.onPause();
        if (mServiceBind == true) {
            getApplicationContext().unbindService(mTestServiceConnection);
        }
    }

    public void onResume() {
        super.onResume();
        Intent intent = new Intent(this, MultiShimmerTemplateService.class);
        Log.d("ShimmerH", "on Resume");
        getApplicationContext().bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);
        if (isMyServiceRunning()) {
            Log.d("ShimmerH", "Started");

        } else {
            Log.d("ShimmerH", "Not Started");
        }

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        ControlFragment controlF = (ControlFragment) getSupportFragmentManager().findFragmentByTag("Control");
        ConfigurationFragment configF = (ConfigurationFragment) getSupportFragmentManager().findFragmentByTag("Configure");
        PlotFragment plotF = (PlotFragment) getSupportFragmentManager().findFragmentByTag("Plot");
        LogFragment logF = (LogFragment) getSupportFragmentManager().findFragmentByTag("Log");
        PPGFragment ppgF = (PPGFragment) getSupportFragmentManager().findFragmentByTag("Heart Rate");

        if (controlF != null) {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        } else if (configF != null) {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        } else if (plotF != null) {
            getMenuInflater().inflate(R.menu.activity_main_plot, menu);
        } else if (logF != null) {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        } else if (ppgF != null) {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        } else {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Log.d("ShimmerMenu", "Settings");
                if (mService.allShimmersDisconnected()) {
                    showMenuDialog();
                } else {
                    Toast.makeText(this, "Ensure all devices are disconnected before proceeding.", Toast.LENGTH_LONG).show();
                }
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showMenuDialog() {

        dialog.setContentView(R.layout.menu_dialog);
        dialog.setTitle("Please Select Option:");
        dialog.setCancelable(true);
        dialog.show();

        Button buttonNew = (Button) dialog.findViewById(R.id.ButtonNew);
        Button buttonLoad = (Button) dialog.findViewById(R.id.ButtonLoad);
        Button buttonReset = (Button) dialog.findViewById(R.id.ButtonReset);

        buttonNew.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                showSaveDialog();
            }

        });

        buttonLoad.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                showLoadDialog();
            }

        });
        buttonReset.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                //mShimmerConfigurationList = new ArrayList<ShimmerConfiguration>();
                db.createNewConfiguration("Temp"); //this will reset Temp
                mService.mShimmerConfigurationList = db.getShimmerConfigurations("Temp");

                ControlFragment controlF = (ControlFragment) getSupportFragmentManager().findFragmentByTag("Control");
                if (controlF != null) {
                    if (controlF.isVisible()) {
                        ((ControlFragment) getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).updateShimmerConfigurationList(mService.mShimmerConfigurationList);                       // add your code here
                    }
                }
                ConfigurationFragment configF = (ConfigurationFragment) getSupportFragmentManager().findFragmentByTag("Configure");
                if (configF != null) {
                    if (configF.isVisible()) {
                        ((ConfigurationFragment) getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).updateShimmerConfigurationList(mService.mShimmerConfigurationList);                       // add your code here
                    }
                }
                if (getSupportFragmentManager().findFragmentById(R.id.item_detail_container) != null) {

                }
                dialog.dismiss();
            }

        });
    }

    public void showSaveDialog() {
        dialog.setContentView(R.layout.save_dialog);
        dialog.setTitle("Save Settings:");
        ListView listView1 = (ListView) dialog.findViewById(R.id.listViewSave);
        final String[] mArrayofSettings = db.getArrayofSettings();
        final EditText et = (EditText) dialog.findViewById(R.id.editTextConfigName);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mArrayofSettings);

        listView1.setAdapter(adapter);

        listView1.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                                    long arg3) {
                // TODO Auto-generated method stub
                et.setText((mArrayofSettings[position]));
            }

        });

        Button buttonSave = (Button) dialog.findViewById(R.id.buttonSave);

        buttonSave.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                db.saveShimmerConfigurations(et.getText().toString(), mService.mShimmerConfigurationList);
                dialog.dismiss();
            }

        });

        dialog.setCancelable(true);
        dialog.show();

    }

    public void showLoadDialog() {
        dialog.setContentView(R.layout.load_dialog);
        dialog.setTitle("Load Settings:");
        ListView listView1 = (ListView) dialog.findViewById(R.id.listViewLoad);
        final String[] mArrayofSettings = db.getArrayofSettings();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mArrayofSettings);

        listView1.setAdapter(adapter);

        listView1.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                                    long arg3) {
                // TODO Auto-generated method stub
                //reset saved states
                mService.resetExpStates();
                mService.mShimmerConfigurationList = db.getShimmerConfigurations(mArrayofSettings[position]);
                db.saveShimmerConfigurations("Temp", mService.mShimmerConfigurationList);
                // the updates should be pass on to all fragments
                if (getSupportFragmentManager().findFragmentById(R.id.item_detail_container) != null) {
                    ((ConfigurationFragment) getSupportFragmentManager().findFragmentById(R.id.item_detail_container)).updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                }
                dialog.dismiss();

            }

        });

        listView1.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int position, long arg3) {
                showDeleteDialog(mArrayofSettings[position]);
                return false;
            }

        });


        dialog.setCancelable(true);
        dialog.show();

    }

    public void showDeleteDialog(final String configName) {
        dialog.setContentView(R.layout.confirm_dialog);
        dialog.setTitle("Delete?");
        Button buttonYes = (Button) dialog.findViewById(R.id.ButtonYes);
        Button buttonNo = (Button) dialog.findViewById(R.id.ButtonNo);
        buttonYes.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                db.deleteConfiguration(configName);
                dialog.dismiss();
            }

        });

        buttonNo.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                dialog.dismiss();
            }

        });


        dialog.setCancelable(true);
        dialog.show();

    }

    public void showTextDialog(String text, String title) {
        dialog.setContentView(R.layout.text_dialog);
        dialog.setTitle(title);
        dialog.setCancelable(true);
        dialog.show();
        TextView TVNew = (TextView) dialog.findViewById(R.id.textViewDialog);
        TVNew.setText(text);
    }

}
