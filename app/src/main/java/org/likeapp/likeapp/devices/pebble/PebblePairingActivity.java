/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Taavi Eomäe

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
package org.likeapp.likeapp.devices.pebble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.likeapp.likeapp.util.AndroidUtils;
import org.likeapp.likeapp.util.BondingInterface;
import org.likeapp.likeapp.util.BondingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.greenrobot.dao.query.Query;
import org.likeapp.likeapp.GBApplication;
import org.likeapp.likeapp.R;
import org.likeapp.likeapp.activities.AbstractGBActivity;
import org.likeapp.likeapp.activities.ControlCenterv2;
import org.likeapp.likeapp.activities.DiscoveryActivity;
import org.likeapp.likeapp.database.DBHandler;
import org.likeapp.likeapp.devices.DeviceCoordinator;
import org.likeapp.likeapp.entities.DaoSession;
import org.likeapp.likeapp.entities.Device;
import org.likeapp.likeapp.entities.DeviceDao;
import org.likeapp.likeapp.impl.GBDevice;
import org.likeapp.likeapp.impl.GBDeviceCandidate;
import org.likeapp.likeapp.util.DeviceHelper;
import org.likeapp.likeapp.util.GB;

import static org.likeapp.likeapp.util.BondingUtil.STATE_DEVICE_CANDIDATE;

public class PebblePairingActivity extends AbstractGBActivity implements BondingInterface {
    private static final Logger LOG = LoggerFactory.getLogger(PebblePairingActivity.class);
    private final BroadcastReceiver pairingReceiver = BondingUtil.getPairingReceiver(this);
    private final BroadcastReceiver bondingReceiver = BondingUtil.getBondingReceiver(this);

    private TextView message;
    private boolean isPairing;

    private GBDeviceCandidate deviceCandidate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pebble_pairing);

        message = findViewById(R.id.pebble_pair_message);
        deviceCandidate = getIntent().getParcelableExtra(DeviceCoordinator.EXTRA_DEVICE_CANDIDATE);

        String macAddress = null;
        if (deviceCandidate != null) {
            macAddress = deviceCandidate.getMacAddress();
        }

        if (macAddress == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
            onBondingComplete(false);
            return;
        }

        BluetoothDevice btDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
        if (btDevice == null) {
            GB.toast(this, "No such Bluetooth Device: " + macAddress, Toast.LENGTH_LONG, GB.ERROR);
            onBondingComplete(false);
            return;
        }

        startBonding(btDevice);
    }

    private void startBonding(BluetoothDevice btDevice) {
        isPairing = true;
        message.setText(getString(R.string.pairing, btDevice.getAddress()));

        GBDevice device;
        if (BondingUtil.isLePebble(btDevice)) {
            if (!GBApplication.getPrefs().getBoolean("pebble_force_le", false)) {
                GB.toast(this, "Please switch on \"Always prefer BLE\" option in Pebble settings before pairing you Pebble LE", Toast.LENGTH_LONG, GB.ERROR);
                onBondingComplete(false);
                return;
            }

            device = getMatchingParentDeviceFromDBAndSetVolatileAddress(btDevice);
            if (device == null) {
                onBondingComplete(false);
                return;
            }

            removeBroadcastReceivers();
            BondingUtil.connectThenComplete(this, device);
            return;
        }

        if (btDevice.getBondState() == BluetoothDevice.BOND_BONDED ||
                btDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
            BondingUtil.connectThenComplete(this, deviceCandidate);
        } else {
            BondingUtil.tryBondThenComplete(this, deviceCandidate);
        }
    }

    private void stopBonding() {
        isPairing = false;
        BondingUtil.stopBluetoothBonding(deviceCandidate.getDevice());
    }

    private GBDevice getMatchingParentDeviceFromDBAndSetVolatileAddress(BluetoothDevice btDevice) {
        String expectedSuffix = btDevice.getName();
        expectedSuffix = expectedSuffix.replace("Pebble-LE ", "");
        expectedSuffix = expectedSuffix.replace("Pebble Time LE ", "");
        expectedSuffix = expectedSuffix.substring(0, 2) + ":" + expectedSuffix.substring(2);
        LOG.info("Trying to find a Pebble with BT address suffix " + expectedSuffix);
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            DeviceDao deviceDao = session.getDeviceDao();
            Query<Device> query = deviceDao.queryBuilder().where(DeviceDao.Properties.Type.eq(1), DeviceDao.Properties.Identifier.like("%" + expectedSuffix)).build();

            List<Device> devices = query.list();
            if (devices.size() == 0) {
                GB.toast("Please pair your non-LE Pebble before pairing the LE one", Toast.LENGTH_SHORT, GB.INFO);
                onBondingComplete(false);
                return null;
            } else if (devices.size() > 1) {
                GB.toast("Can not match this Pebble LE to a unique device", Toast.LENGTH_SHORT, GB.INFO);
                onBondingComplete(false);
                return null;
            }

            GBDevice gbDevice = DeviceHelper.getInstance().toGBDevice(devices.get(0));
            gbDevice.setVolatileAddress(btDevice.getAddress());
            return gbDevice;
        } catch (Exception e) {
            GB.toast(getString(R.string.error_retrieving_devices_database), Toast.LENGTH_SHORT, GB.ERROR, e);
            onBondingComplete(false);
            return null;
        }
    }

    @Override
    public void onBondingComplete(boolean success) {
        unregisterBroadcastReceivers();
        if (success) {
            startActivity(new Intent(this, ControlCenterv2.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            startActivity(new Intent(this, DiscoveryActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }

        // If it's not a LE Pebble, initiate a connection when bonding is complete
        if (!BondingUtil.isLePebble(getCurrentTarget()) && success) {
            BondingUtil.attemptToFirstConnect(getCurrentTarget());
        }
        finish();
    }

    @Override
    public BluetoothDevice getCurrentTarget() {
        return this.deviceCandidate.getDevice();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BondingUtil.handleActivityResult(this, requestCode, resultCode, data);
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
    protected void onStart() {
        removeBroadcastReceivers();
        super.onStart();
    }

    @Override
    protected void onResume() {
        removeBroadcastReceivers();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcastReceivers();
        if (isPairing) {
            stopBonding();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        unregisterBroadcastReceivers();
        if (isPairing) {
            stopBonding();
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        // WARN: Do not stop bonding process during pause!
        // Bonding process can pause the activity and you might miss broadcasts
        super.onPause();
    }

    public void unregisterBroadcastReceivers() {
        AndroidUtils.safeUnregisterBroadcastReceiver(LocalBroadcastManager.getInstance(this), pairingReceiver);
        AndroidUtils.safeUnregisterBroadcastReceiver(this, bondingReceiver);
    }

    public void removeBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(pairingReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));
        registerReceiver(bondingReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    @Override
    public Context getContext() {
        return this;
    }
}
