package com.shimmerresearch.multishimmertemplate;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TabHost;

import com.shimmerresearch.adapters.ExpandableListViewAdapter;
import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.database.ShimmerConfiguration;
import com.shimmerresearch.multishimmertemplate.menucontent.MenuContent;
import com.shimmerresearch.service.MultiShimmerTemplateService;

import java.util.ArrayList;
import java.util.List;

import de.bogutzky.datacollector.app.R;


/**
 * A fragment representing a single Item detail screen. This fragment is either
 * contained in a {@link ItemListActivity} in two-pane mode (on tablets) or a
 * {@link ItemDetailActivity} on handsets.
 */
public class ItemDetailFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    static ExpandableListViewAdapter mAdapter;
    public final int MSG_BLUETOOTH_ADDRESS = 1;
    public final int MSG_CONFIGURE_SHIMMER = 2;
    public final int MSG_CONFIGURE_SENSORS_SHIMMER = 3;
    public ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            // TODO Auto-generated method stub
            Log.d("Shimmer", "SERRRVVVIIICE");
            if (mItem.content.equals("Configure")) {
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub

        }
    };
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */

    boolean mServiceBind = false;
    ListView listViewGestures;
    List<String> listValues = new ArrayList<String>();
    ExpandableListView listViewShimmers;
    DatabaseHandler db;
    MultiShimmerTemplateService mService;
    String[] deviceNames;
    String[] deviceBluetoothAddresses;
    String[][] mShimmerConfigs;
    int numberofChilds[];
    ImageButton mButtonAddDevice;
    int tempPosition;
    Dialog dialog;
    List<ShimmerConfiguration> mShimmerConfigurationList = new ArrayList<ShimmerConfiguration>();
    boolean firstTime = true;
    View rootView = null;
    Handler mHandler;
    MenuContent mc = new MenuContent();
    private TabHost tabHost;
    /**
     * The dummy content this fragment is presenting.
     */
    private MenuContent.MenuItem mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            String itemString = getArguments().getString(ARG_ITEM_ID);
            //mItem = MenuContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Intent intent = new Intent(getActivity().getApplicationContext(), MultiShimmerTemplateService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //this values should be loaded from a database, but for now this will do, when you exit this fragment this list should be saved to a database

        if (mItem.content.equals("Configure")) {
            rootView = inflater.inflate(R.layout.configure_main, container, false);
        } else if (mItem.content.equals("Control")) {
            rootView = inflater.inflate(R.layout.control_main, container, false);
        } else {
            rootView = inflater.inflate(R.layout.control_main, container, false);
        }


        // Show the dummy content as text in a TextView.
        /*if (mItem != null) {
			((TextView) rootView.findViewById(R.id.item_detail))
					.setText(mItem.content);
		}*/

        return rootView;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d("Activity Name", activity.getClass().getSimpleName());

        if (!isMyServiceRunning()) {
            Intent intent = new Intent(getActivity(), MultiShimmerTemplateService.class);
            getActivity().startService(intent);
        }
    }

    protected boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.shimmerresearch.service.MultiShimmerGestureService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
