/*  Copyright (C) 2018-2019 Daniele Gobbetti, maxirnilian

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.likeapp.likeapp.devices.lenovo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.likeapp.likeapp.GBApplication;
import org.likeapp.likeapp.R;
import org.likeapp.likeapp.activities.AbstractGBActivity;
import org.likeapp.likeapp.activities.ControlCenterv2;
import org.likeapp.likeapp.activities.DiscoveryActivity;
import org.likeapp.likeapp.devices.DeviceCoordinator;
import org.likeapp.likeapp.impl.GBDevice;
import org.likeapp.likeapp.impl.GBDeviceCandidate;
import org.likeapp.likeapp.util.AndroidUtils;
import org.likeapp.likeapp.util.DeviceHelper;
import org.likeapp.likeapp.util.GB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class LenovoWatchPairingActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(LenovoWatchPairingActivity.class);

    private static final String STATE_DEVICE_CANDIDATE = "stateDeviceCandidate";

    private TextView message;
    private GBDeviceCandidate deviceCandidate;

    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                LOG.debug("pairing activity: device changed: " + device);
                if (deviceCandidate.getMacAddress().equals(device.getAddress())) {
                    if (device.isInitialized()) {
                        pairingFinished();
                    } else if (device.isConnecting() || device.isInitializing()) {
                        LOG.info("still connecting/initializing device...");
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch9_pairing);

        message = findViewById(R.id.watch9_pair_message);
        Intent intent = getIntent();
        deviceCandidate = intent.getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);
        if (deviceCandidate == null && savedInstanceState != null) {
            deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
        }
        if (deviceCandidate == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DiscoveryActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        startPairing();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DEVICE_CANDIDATE, deviceCandidate);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        deviceCandidate = savedInstanceState.getParcelable(STATE_DEVICE_CANDIDATE);
    }

    @Override
    protected void onDestroy() {
        AndroidUtils.safeUnregisterBroadcastReceiver(LocalBroadcastManager.getInstance(this), mPairingReceiver);
        super.onDestroy();
    }

    private void startPairing() {
        message.setText(getString(R.string.pairing, deviceCandidate));

        IntentFilter filter = new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mPairingReceiver, filter);

        GBApplication.deviceService().disconnect();
        GBDevice device = DeviceHelper.getInstance().toSupportedDevice(deviceCandidate);
        if (device != null) {
            GBApplication.deviceService().connect(device, true);
        } else {
            GB.toast(this, "Unable to connect, can't recognize the device type: " + deviceCandidate, Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void pairingFinished() {
        AndroidUtils.safeUnregisterBroadcastReceiver(LocalBroadcastManager.getInstance(this), mPairingReceiver);

        Intent intent = new Intent(this, ControlCenterv2.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        finish();
    }
}
