/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer

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
package org.likeapp.likeapp.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import org.likeapp.likeapp.GBApplication;
import org.likeapp.likeapp.R;
import org.likeapp.likeapp.devices.DeviceCoordinator;
import org.likeapp.likeapp.impl.GBDevice;
import org.likeapp.likeapp.impl.GBDeviceCandidate;
import org.likeapp.likeapp.util.DeviceHelper;
import org.likeapp.likeapp.util.GB;

/**
 * Adapter for displaying GBDeviceCandate instances.
 */
public class DeviceCandidateAdapter extends ArrayAdapter<GBDeviceCandidate> {

    private final Context context;

    public DeviceCandidateAdapter(Context context, List<GBDeviceCandidate> deviceCandidates) {
        super(context, 0, deviceCandidates);

        this.context = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        GBDeviceCandidate device = getItem(position);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_with_details, parent, false);
        }
        ImageView deviceImageView = view.findViewById(R.id.item_image);
        TextView deviceNameLabel = view.findViewById(R.id.item_name);
        TextView deviceAddressLabel = view.findViewById(R.id.item_details);
        TextView deviceStatus = view.findViewById(R.id.item_status);

        String name = formatDeviceCandidate(device);
        deviceNameLabel.setText(name);
        deviceAddressLabel.setText(device.getMacAddress());
        deviceImageView.setImageResource(device.getDeviceType().getIcon());

        String status = "";
        if (device.getDevice().getBondState() == BluetoothDevice.BOND_BONDED) {
            status += getContext().getString(R.string.device_is_currently_bonded);
            if (!GBApplication.getPrefs().getBoolean("ignore_bonded_devices", false)) { // This could be passed to the constructor instead
                deviceImageView.setImageResource(device.getDeviceType().getDisabledIcon());
            }
        }

        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_REQUIRE_KEY) {
            if (device.getDevice().getBondState() == BluetoothDevice.BOND_BONDED) {
                status += "\n";
            }
            status += getContext().getString(R.string.device_requires_key);
        }

        deviceStatus.setText(status);
        return view;
    }

    private String formatDeviceCandidate(GBDeviceCandidate device) {
        if (device.getRssi() > GBDevice.RSSI_UNKNOWN) {
            return context.getString(R.string.device_with_rssi, device.getName(), GB.formatRssi(device.getRssi()));
        }
        return device.getName();
    }
}
