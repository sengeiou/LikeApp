/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniel
    Dakhno, Daniele Gobbetti, José Rebelo

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
package org.likeapp.likeapp.devices.qhybrid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

import org.likeapp.likeapp.GBApplication;
import org.likeapp.likeapp.GBException;
import org.likeapp.likeapp.R;
import org.likeapp.likeapp.devices.AbstractDeviceCoordinator;
import org.likeapp.likeapp.devices.DeviceCoordinator;
import org.likeapp.likeapp.devices.InstallHandler;
import org.likeapp.likeapp.devices.SampleProvider;
import org.likeapp.likeapp.entities.DaoSession;
import org.likeapp.likeapp.entities.Device;
import org.likeapp.likeapp.impl.GBDevice;
import org.likeapp.likeapp.impl.GBDeviceCandidate;
import org.likeapp.likeapp.model.ActivitySample;
import org.likeapp.likeapp.model.DeviceType;
import org.likeapp.likeapp.model.ItemWithDetails;
import org.likeapp.likeapp.service.DeviceCommunicationService;
import org.likeapp.likeapp.service.devices.qhybrid.QHybridSupport;
import org.likeapp.likeapp.util.DeviceHelper;

public class QHybridCoordinator extends AbstractDeviceCoordinator {
    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        for(ParcelUuid uuid : candidate.getServiceUuids()){
            if(uuid.getUuid().toString().equals("3dda0001-957f-7d4a-34a6-74696673696d")){
                return DeviceType.FOSSILQHYBRID;
            }
        }
        return DeviceType.UNKNOWN;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        return Collections.singletonList(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("3dda0001-957f-7d4a-34a6-74696673696d")).build());
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.FOSSILQHYBRID;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }


    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return false;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    private boolean supportsAlarmConfiguration() {
        GBDevice connectedDevice = GBApplication.app().getDeviceManager().getSelectedDevice();
        if(connectedDevice == null || connectedDevice.getType() != DeviceType.FOSSILQHYBRID || connectedDevice.getState() != GBDevice.State.INITIALIZED){
            return false;
        }
        return true;
    }

    @Override
    public int getAlarmSlotCount() {
        return this.supportsAlarmConfiguration() ? 5 : 0;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return false;
    }

    @Override
    public String getManufacturer() {
        return "Fossil";
    }

    @Override
    public boolean supportsAppsManagement() {
        return true;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return isHybridHR() ? HRConfigActivity.class : ConfigActivity.class;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
    }

    @Override
    public boolean supportsWeather() {
        return isHybridHR();
    }

    @Override
    public boolean supportsFindDevice() {
        GBDevice connectedDevice = GBApplication.app().getDeviceManager().getSelectedDevice();
        if(connectedDevice == null || connectedDevice.getType() != DeviceType.FOSSILQHYBRID){
            return true;
        }
        ItemWithDetails vibration = connectedDevice.getDeviceInfo(QHybridSupport.ITEM_EXTENDED_VIBRATION_SUPPORT);
        if(vibration == null){
            return true;
        }
        return vibration.getDetails().equals("true");
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        if (isHybridHR()) {
            return new int[]{
                    R.xml.devicesettings_fossilhybridhr,
                    R.xml.devicesettings_pairingkey,
                    R.xml.devicesettings_custom_deviceicon
            };
        }
        return new int[]{
                R.xml.devicesettings_pairingkey,
                R.xml.devicesettings_custom_deviceicon
        };
    }

    private boolean isHybridHR() {
        GBDevice connectedDevice = GBApplication.app().getDeviceManager().getSelectedDevice();
        if (connectedDevice != null) {
            return connectedDevice.getName().startsWith("Hybrid HR");
        }
        return false;
    }
}