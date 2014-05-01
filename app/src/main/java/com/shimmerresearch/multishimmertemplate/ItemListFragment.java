package com.shimmerresearch.multishimmertemplate;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.multishimmertemplate.menucontent.MenuContent;
import com.shimmerresearch.multishimmertemplate.menucontent.MenuContent.MenuItem;
import com.shimmerresearch.service.MultiShimmerTemplateService;

/**
 * A list fragment representing a list of Items. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link ItemDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ItemListFragment extends ListFragment {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String id) {
        }
    };
    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;
    boolean mServiceBind = false;
    MultiShimmerTemplateService mService;
    DatabaseHandler db;
    public ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            // TODO Auto-generated method stub
            // TODO Auto-generated method stub
            Log.d("ShimmerService", "service connected from main activity");
            com.shimmerresearch.service.MultiShimmerTemplateService.LocalBinder binder = (com.shimmerresearch.service.MultiShimmerTemplateService.LocalBinder) service;
            mService = binder.getService();
            mService.testService();
            mServiceBind = true;
            db = mService.mDataBase;
            mService.mMenuContent.clear();
            mService.mMenuContent.addItem(new MenuItem("1", "Control"));
            mService.mMenuContent.addItem(new MenuItem("2", "Configure"));
            //mService.mMenuContent.addItem(new MenuItem("3", "Plot"));
            //mService.mMenuContent.addItem(new MenuItem("4", "Log"));
            //mService.mMenuContent.addItem(new MenuItem("5", "Heart Rate"));
            //mService.mMenuContent.addItem(new MenuItem("6", "Blank"));
            mService.mMenuContent.addItem(new MenuItem("7", "Flow"));
            setListAdapter(new ArrayAdapter<MenuContent.MenuItem>(getActivity(),
                    android.R.layout.simple_list_item_activated_1,
                    android.R.id.text1, mService.mMenuContent.ITEMS));

        }

        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
            mServiceBind = false;
        }
    };
    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(getActivity().getApplicationContext(), MultiShimmerTemplateService.class);
        if (!mServiceBind) {
            getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.

        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState
                    .getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
        if (mServiceBind) {
            getActivity().unbindService(mConnection);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position,
                                long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(mService.mMenuContent.ITEMS.get(position).id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(
                activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
                        : ListView.CHOICE_MODE_NONE
        );
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String id);
    }
}
