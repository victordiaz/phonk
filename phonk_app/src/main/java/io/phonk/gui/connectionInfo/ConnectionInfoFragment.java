/*
 * Part of Phonk http://www.phonk.io
 * A prototyping platform for Android devices
 *
 * Copyright (C) 2013 - 2017 Victor Diaz Barrales @victordiaz (Protocoder)
 * Copyright (C) 2017 - Victor Diaz Barrales @victordiaz (Phonk)
 *
 * Phonk is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Phonk is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Phonk. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.phonk.gui.connectionInfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import io.phonk.App;
import io.phonk.R;
import io.phonk.events.Events;
import io.phonk.gui.settings.UserPreferences;
import io.phonk.helpers.PhonkAppHelper;
import io.phonk.runner.apprunner.AppRunner;
import io.phonk.runner.base.utils.MLog;
import io.phonk.runner.base.views.FitRecyclerView;

public class ConnectionInfoFragment extends Fragment {

    private final String TAG = ConnectionInfoFragment.class.getSimpleName();

    private AppRunner mAppRunner;
    private View rootView;

    private ToggleButton mToggleServers;
    private TextView mTxtConnectionMessage;
    private TextView mTxtConnectionIp;
    private String mRealIp = "";
    private String mMaskedIp = "XXX.XXX.XXX.XXX";
    private String mLastConnectionMessage;
    private String mLastIp;

    private FitRecyclerView mEventsRecyclerView;
    private EventsAdapter mEventsAdapter;

    public ConnectionInfoFragment() {
    }

    public static ConnectionInfoFragment newInstance() {
        ConnectionInfoFragment fragment = new ConnectionInfoFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        rootView = inflater.inflate(R.layout.connection_info_fragment, container, false);

        mTxtConnectionMessage = rootView.findViewById(R.id.connection_message);
        mTxtConnectionIp = rootView.findViewById(R.id.connection_ip);

        if ((boolean) UserPreferences.getInstance().get("servers_mask_ip")) {
            mTxtConnectionIp.setOnClickListener(view -> {
                if (mTxtConnectionIp.getText().toString().contains("XXX")) {
                    mTxtConnectionIp.setText("http://" + mRealIp);
                } else {
                    mTxtConnectionIp.setText("http://" + mMaskedIp);
                }
            });
        }

        mToggleServers = rootView.findViewById(R.id.webide_connection_toggle);
        Button connectWifi = rootView.findViewById(R.id.connect_to_wifi);
        Button startHotspot = rootView.findViewById(R.id.start_hotspot);
        // Button webide_connection_help = (Button) findViewById(R.id.webide_connection_help);

        connectWifi.setOnClickListener(view -> PhonkAppHelper.launchWifiSettings(getActivity()));
        startHotspot.setOnClickListener(view -> PhonkAppHelper.launchHotspotSettings(getActivity()));

        mToggleServers.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) EventBus.getDefault().postSticky(new Events.AppUiEvent("startServers", ""));
            else EventBus.getDefault().postSticky(new Events.AppUiEvent("stopServers", ""));
        });

        if ((boolean) UserPreferences.getInstance().get("servers_enabled_on_start")) {
            mToggleServers.performClick(); // startServers();
        }

        mEventsRecyclerView = rootView.findViewById(R.id.recyclerViewEventLog);
        mEventsAdapter = new EventsAdapter(getActivity(), mEventsRecyclerView);
        mEventsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mEventsRecyclerView.setAdapter(mEventsAdapter);

        mEventsRecyclerView.setOnClickListener(view -> {
            rootView.findViewById(R.id.update_layout).getLayoutParams().height = 500;
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        setConnectionMessage(mLastConnectionMessage, mLastIp);
        EventBus.getDefault().register(this);

        ArrayList<EventManager.EventLogItem> mEventsList = ((App) getActivity().getApplication()).eventManager.getEventsList();
        mEventsAdapter.setEventList(mEventsList);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void setConnectionMessage(String connectionMessage, String ip) {
        mLastConnectionMessage = connectionMessage;
        mLastIp = ip;
        mTxtConnectionMessage.setText(connectionMessage);
        mTxtConnectionIp.setText("http://" + ip);

        if (ip != null) {
            mTxtConnectionIp.setVisibility(View.VISIBLE);
        } else {
            mTxtConnectionIp.setVisibility(View.GONE);
        }
    }

    // network notification
    @Subscribe
    public void onEventMainThread(Events.Connection e) {
        String type = e.getType();
        String address = e.getAddress();
        mRealIp = address;

        boolean isMaskingIp = (boolean) UserPreferences.getInstance().get("servers_mask_ip");
        if (isMaskingIp) {
            address = mMaskedIp;
        }

        if (type == "wifi") {
            setConnectionMessage(getResources().getString(io.phonk.R.string.connection_message_wifi), address);
        } else if (type == "tethering") {
            setConnectionMessage(getResources().getString(io.phonk.R.string.connection_message_tethering), address);
        } else {
            setConnectionMessage(getResources().getString(io.phonk.R.string.connection_message_not_connected), null);
        }
    }

    @Subscribe
    public void onEventMainThread(Events.AppUiEvent e) {
        String action = e.getAction();

        switch (action) {
            case "stopServers":
                mToggleServers.setChecked(false);
                break;
            case "startServers":
                mToggleServers.setChecked(true);
                break;
            case Events.NEW_EVENT:
                mEventsAdapter.notifyNewEvents();
                break;
        }
    }

    // folder choose
    @Subscribe
    public void onEventMainThread(Events.FolderChosen e) {
        // addEvent("list", e.getFullFolder());
    }

}
