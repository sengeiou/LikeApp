/*  Copyright (C) 2015-2020 Andreas Böhler, Andreas Shimokawa, Avamander,
    Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti, Daniel Hauck, Dikay900,
    Frank Slezak, ivanovlev, João Paulo Barraca, José Rebelo, Julien Pivotto,
    Kasha, keeshii, Martin, Matthieu Baerts, Nephiel, Sebastian Kranz, Sergey
    Trofimov, Steffen Liebergeld, Taavi Eomäe, Uwe Hermann

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
package org.likeapp.likeapp.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.likeapp.likeapp.externalevents.TinyWeatherForecastGermanyReceiver;
import org.likeapp.likeapp.service.receivers.AutoConnectIntervalReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import org.likeapp.likeapp.GBApplication;
import org.likeapp.likeapp.R;
import org.likeapp.likeapp.activities.HeartRateUtils;
import org.likeapp.likeapp.devices.DeviceCoordinator;
import org.likeapp.likeapp.externalevents.AlarmClockReceiver;
import org.likeapp.likeapp.externalevents.AlarmReceiver;
import org.likeapp.likeapp.externalevents.BluetoothConnectReceiver;
import org.likeapp.likeapp.externalevents.BluetoothPairingRequestReceiver;
import org.likeapp.likeapp.externalevents.CMWeatherReceiver;
import org.likeapp.likeapp.externalevents.CalendarReceiver;
import org.likeapp.likeapp.externalevents.LineageOsWeatherReceiver;
import org.likeapp.likeapp.externalevents.MusicPlaybackReceiver;
import org.likeapp.likeapp.externalevents.OmniJawsObserver;
import org.likeapp.likeapp.externalevents.PebbleReceiver;
import org.likeapp.likeapp.externalevents.PhoneCallReceiver;
import org.likeapp.likeapp.externalevents.SMSReceiver;
import org.likeapp.likeapp.externalevents.TimeChangeReceiver;
import org.likeapp.likeapp.impl.GBDevice;
import org.likeapp.likeapp.model.Alarm;
import org.likeapp.likeapp.model.CalendarEventSpec;
import org.likeapp.likeapp.model.CallSpec;
import org.likeapp.likeapp.model.CannedMessagesSpec;
import org.likeapp.likeapp.model.MusicSpec;
import org.likeapp.likeapp.model.MusicStateSpec;
import org.likeapp.likeapp.model.NotificationSpec;
import org.likeapp.likeapp.model.NotificationType;
import org.likeapp.likeapp.model.WeatherSpec;
import org.likeapp.likeapp.service.receivers.GBAutoFetchReceiver;
import org.likeapp.likeapp.util.DeviceHelper;
import org.likeapp.likeapp.util.EmojiConverter;
import org.likeapp.likeapp.util.GB;
import org.likeapp.likeapp.util.GBPrefs;
import org.likeapp.likeapp.util.Prefs;

import static org.likeapp.likeapp.model.DeviceService.ACTION_ADD_CALENDAREVENT;
import static org.likeapp.likeapp.model.DeviceService.ACTION_APP_CONFIGURE;
import static org.likeapp.likeapp.model.DeviceService.ACTION_APP_REORDER;
import static org.likeapp.likeapp.model.DeviceService.ACTION_CALLSTATE;
import static org.likeapp.likeapp.model.DeviceService.ACTION_CONNECT;
import static org.likeapp.likeapp.model.DeviceService.ACTION_DELETEAPP;
import static org.likeapp.likeapp.model.DeviceService.ACTION_DELETE_CALENDAREVENT;
import static org.likeapp.likeapp.model.DeviceService.ACTION_DELETE_NOTIFICATION;
import static org.likeapp.likeapp.model.DeviceService.ACTION_DISCONNECT;
import static org.likeapp.likeapp.model.DeviceService.ACTION_ENABLE_HEARTRATE_SLEEP_SUPPORT;
import static org.likeapp.likeapp.model.DeviceService.ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT;
import static org.likeapp.likeapp.model.DeviceService.ACTION_ENABLE_REALTIME_STEPS;
import static org.likeapp.likeapp.model.DeviceService.ACTION_FETCH_RECORDED_DATA;
import static org.likeapp.likeapp.model.DeviceService.ACTION_FIND_DEVICE;
import static org.likeapp.likeapp.model.DeviceService.ACTION_HEARTRATE_TEST;
import static org.likeapp.likeapp.model.DeviceService.ACTION_INSTALL;
import static org.likeapp.likeapp.model.DeviceService.ACTION_NOTIFICATION;
import static org.likeapp.likeapp.model.DeviceService.ACTION_READ_CONFIGURATION;
import static org.likeapp.likeapp.model.DeviceService.ACTION_REQUEST_APPINFO;
import static org.likeapp.likeapp.model.DeviceService.ACTION_REQUEST_DEVICEINFO;
import static org.likeapp.likeapp.model.DeviceService.ACTION_REQUEST_SCREENSHOT;
import static org.likeapp.likeapp.model.DeviceService.ACTION_RESET;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SEND_CONFIGURATION;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SEND_WEATHER;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SETCANNEDMESSAGES;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SETMUSICINFO;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SETMUSICSTATE;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SETTIME;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SET_ALARMS;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SET_CONSTANT_VIBRATION;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SET_FM_FREQUENCY;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SET_HEARTRATE_MEASUREMENT_INTERVAL;
import static org.likeapp.likeapp.model.DeviceService.ACTION_SET_LED_COLOR;
import static org.likeapp.likeapp.model.DeviceService.ACTION_START;
import static org.likeapp.likeapp.model.DeviceService.ACTION_STARTAPP;
import static org.likeapp.likeapp.model.DeviceService.ACTION_TEST_NEW_FUNCTION;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_ALARMS;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_APP_CONFIG;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_APP_CONFIG_ID;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_APP_START;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_APP_UUID;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_BOOLEAN_ENABLE;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CALENDAREVENT_DESCRIPTION;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CALENDAREVENT_DURATION;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CALENDAREVENT_ID;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CALENDAREVENT_LOCATION;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CALENDAREVENT_TIMESTAMP;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CALENDAREVENT_TITLE;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CALENDAREVENT_TYPE;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CALL_COMMAND;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CALL_DISPLAYNAME;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CALL_PHONENUMBER;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CANNEDMESSAGES;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CANNEDMESSAGES_TYPE;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CONFIG;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_CONNECT_FIRST_TIME;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_FIND_START;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_FM_FREQUENCY;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_INTERVAL_SECONDS;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_LED_COLOR;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_ALBUM;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_ARTIST;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_DURATION;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_POSITION;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_RATE;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_REPEAT;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_SHUFFLE;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_STATE;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_TRACK;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_TRACKCOUNT;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_MUSIC_TRACKNR;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_ACTIONS;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_BODY;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_FLAGS;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_ID;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_PEBBLE_COLOR;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_PHONENUMBER;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_SENDER;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_SOURCEAPPID;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_SOURCENAME;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_SUBJECT;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_TITLE;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_NOTIFICATION_TYPE;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_RECORDED_DATA_TYPES;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_RESET_FLAGS;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_URI;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_VIBRATION_INTENSITY;
import static org.likeapp.likeapp.model.DeviceService.EXTRA_WEATHER;

public class DeviceCommunicationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceCommunicationService.class);
    @SuppressLint("StaticFieldLeak") // only used for test cases
    private static DeviceSupportFactory DEVICE_SUPPORT_FACTORY = null;

    private boolean mStarted = false;

    private DeviceSupportFactory mFactory;
    private GBDevice mGBDevice = null;
    private DeviceSupport mDeviceSupport;
    private DeviceCoordinator mCoordinator = null;

    private PhoneCallReceiver mPhoneCallReceiver = null;
    private SMSReceiver mSMSReceiver = null;
    private PebbleReceiver mPebbleReceiver = null;
    private MusicPlaybackReceiver mMusicPlaybackReceiver = null;
    private TimeChangeReceiver mTimeChangeReceiver = null;
    private BluetoothConnectReceiver mBlueToothConnectReceiver = null;
    private BluetoothPairingRequestReceiver mBlueToothPairingRequestReceiver = null;
    private AlarmClockReceiver mAlarmClockReceiver = null;
    private GBAutoFetchReceiver mGBAutoFetchReceiver = null;
    private AutoConnectIntervalReceiver mAutoConnectIntervalReceiver = null;

    private AlarmReceiver mAlarmReceiver = null;
    private CalendarReceiver mCalendarReceiver = null;
    private CMWeatherReceiver mCMWeatherReceiver = null;
    private LineageOsWeatherReceiver mLineageOsWeatherReceiver = null;
    private TinyWeatherForecastGermanyReceiver mTinyWeatherForecastGermanyReceiver = null;
    private OmniJawsObserver mOmniJawsObserver = null;
    private Intent notificationSaved;

    private final String[] mMusicActions = {
            "com.android.music.metachanged",
            "com.android.music.playstatechanged",
            "com.android.music.queuechanged",
            "com.android.music.playbackcomplete",
            "net.sourceforge.subsonic.androidapp.EVENT_META_CHANGED",
            "com.maxmpz.audioplayer.TPOS_SYNC",
            "com.maxmpz.audioplayer.STATUS_CHANGED",
            "com.maxmpz.audioplayer.PLAYING_MODE_CHANGED",
            "com.spotify.music.metadatachanged",
            "com.spotify.music.playbackstatechanged"
    };

    /**
     * For testing!
     *
     * @param factory
     */
    public static void setDeviceSupportFactory(DeviceSupportFactory factory) {
        DEVICE_SUPPORT_FACTORY = factory;
    }

    public DeviceCommunicationService() {

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(action)) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (mGBDevice != null && mGBDevice.equals(device)) {
                    mGBDevice = device;
                    mCoordinator = DeviceHelper.getInstance().getCoordinator(device);
                    boolean enableReceivers = mDeviceSupport != null && (mDeviceSupport.useAutoConnect() || mGBDevice.isInitialized());
                    setReceiversEnableState(enableReceivers, mGBDevice.isInitialized(), mCoordinator);
                } else {
                    LOG.error("Got ACTION_DEVICE_CHANGED from unexpected device: " + device);
                }
            }
        }
    };

    private final BroadcastReceiver mBatteryLevelReceiver = new BroadcastReceiver ()
    {
        private int lastPercent;

        @Override
        public void onReceive (Context context, Intent intent)
        {
            Prefs prefs = GBApplication.getPrefs ();
            if (mGBDevice != null && mGBDevice.isInitialized ())
            {
                int level = intent.getIntExtra (BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra (BatteryManager.EXTRA_SCALE, -1);
                int status = intent.getIntExtra (BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);

                int percent = 100 * level / scale;

                if (lastPercent != percent)
                {
                    if (prefs.getBoolean ("notifications_battery_" + percent, false))
                    {
                        if (percent >= 80)
                        {
                            status = status == BatteryManager.BATTERY_STATUS_CHARGING ? BatteryManager.BATTERY_STATUS_FULL : 0;
                        }

                        if (status == BatteryManager.BATTERY_STATUS_FULL || status == BatteryManager.BATTERY_STATUS_DISCHARGING || status == BatteryManager.BATTERY_STATUS_NOT_CHARGING)
                        {
                            NotificationSpec notificationSpec = new NotificationSpec ();
                            notificationSpec.type = status == BatteryManager.BATTERY_STATUS_FULL ? NotificationType.BATTERY_FULL : NotificationType.BATTERY_LOW;
                            notificationSpec.sender = context.getString (R.string.notification_battery, percent);
                            notificationSpec.subject = context.getString (status == BatteryManager.BATTERY_STATUS_FULL ? R.string.notification_battery_full : R.string.notification_battery_low);
                            GBApplication.deviceService ().onNotification (notificationSpec);
                        }
                    }

                    lastPercent = percent;
                }
            }
        }
    };

    @Override
    public void onCreate() {
        LOG.debug("DeviceCommunicationService is being created");
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));
        mFactory = getDeviceSupportFactory();

        registerReceiver (mBatteryLevelReceiver, new IntentFilter (Intent.ACTION_BATTERY_CHANGED));

        if (hasPrefs()) {
            getPrefs().getPreferences().registerOnSharedPreferenceChangeListener(this);
        }
    }

    private DeviceSupportFactory getDeviceSupportFactory() {
        if (DEVICE_SUPPORT_FACTORY != null) {
            return DEVICE_SUPPORT_FACTORY;
        }
        return new DeviceSupportFactory(this);
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            LOG.info("no intent");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        boolean firstTime = intent.getBooleanExtra(EXTRA_CONNECT_FIRST_TIME, false);

        if (action == null) {
            LOG.info("no action");
            return START_NOT_STICKY;
        }

        LOG.debug("Service startcommand: " + action);

        if (!action.equals(ACTION_START) && !action.equals(ACTION_CONNECT)) {
            if (!mStarted) {
                // using the service before issuing ACTION_START
                LOG.info("Must start service with " + ACTION_START + " or " + ACTION_CONNECT + " before using it: " + action);
                return START_NOT_STICKY;
            }

            if (mDeviceSupport == null || (!isInitialized() && !action.equals(ACTION_DISCONNECT) && (!mDeviceSupport.useAutoConnect() || isConnected()))) {
                // trying to send notification without valid Bluetooth connection
                if (mGBDevice != null) {
                    // at least send back the current device state
                    mGBDevice.sendDeviceUpdateIntent(this);
                }
                else if (action.equals (ACTION_NOTIFICATION))
                {
                    // Сохранить уведомление
                    notificationSaved = intent;
                }
                return START_STICKY;
            }
        }

        // when we get past this, we should have valid mDeviceSupport and mGBDevice instances

        Prefs prefs = getPrefs();
        switch (action) {
            case ACTION_START:
                start();
                break;
            case ACTION_CONNECT:
                start(); // ensure started
                GBDevice gbDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                String btDeviceAddress = null;
                if (gbDevice == null) {
                    if (prefs != null) { // may be null in test cases
                        btDeviceAddress = prefs.getString("last_device_address", null);
                        if (btDeviceAddress != null) {
                            gbDevice = DeviceHelper.getInstance().findAvailableDevice(btDeviceAddress, this);
                        }
                    }
                } else {
                    btDeviceAddress = gbDevice.getAddress();
                }

                boolean autoReconnect = GBPrefs.AUTO_RECONNECT_DEFAULT;
                if (prefs != null && prefs.getPreferences() != null) {
                    prefs.getPreferences().edit().putString("last_device_address", btDeviceAddress).apply();
                    autoReconnect = getGBPrefs().getAutoReconnect();
                }

                if (gbDevice != null && !isConnecting() && !isConnected()) {
                    setDeviceSupport(null);
                    try {
                        DeviceSupport deviceSupport = mFactory.createDeviceSupport(gbDevice);
                        if (deviceSupport != null) {
                            setDeviceSupport(deviceSupport);
                            if (firstTime) {
                                deviceSupport.connectFirstTime();
                            } else {
                                deviceSupport.setAutoReconnect(autoReconnect);
                                deviceSupport.connect();
                            }
                        } else {
                            GB.toast(this, getString(R.string.cannot_connect, "Can't create device support"), Toast.LENGTH_SHORT, GB.ERROR);
                        }
                    } catch (Exception e) {
                        GB.toast(this, getString(R.string.cannot_connect, e.getMessage()), Toast.LENGTH_SHORT, GB.ERROR, e);
                        setDeviceSupport(null);
                    }
                } else if (mGBDevice != null) {
                    // send an update at least
                    mGBDevice.sendDeviceUpdateIntent(this);
                }
                break;
            default:
                if (mDeviceSupport == null || mGBDevice == null) {
                    LOG.warn("device support:" + mDeviceSupport + ", device: " + mGBDevice + ", aborting");
                } else {
                    try
                    {
                        handleAction(intent, action, prefs);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace ();
                    }
                }
                break;
        }
        return START_STICKY;
    }

    /**
     * @param text original text
     * @return 'text' or a new String without non supported chars like emoticons, etc.
     */
    private String sanitizeNotifText(String text) {
        if (text == null || text.length() == 0)
            return text;

        text = mDeviceSupport.customStringFilter(text);

        if (!mCoordinator.supportsUnicodeEmojis()) {
            return EmojiConverter.convertUnicodeEmojiToAscii(text, getApplicationContext());
        }

        return text;
    }

    private void handleAction(Intent intent, String action, Prefs prefs) {
        switch (action) {
            case ACTION_REQUEST_DEVICEINFO:
                mGBDevice.sendDeviceUpdateIntent(this);
                break;
            case ACTION_NOTIFICATION: {
                // Удалить сохранённое уведомление
                notificationSaved = null;

                int desiredId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
                NotificationSpec notificationSpec = new NotificationSpec(desiredId);
                notificationSpec.phoneNumber = intent.getStringExtra(EXTRA_NOTIFICATION_PHONENUMBER);
                notificationSpec.sender = sanitizeNotifText(intent.getStringExtra(EXTRA_NOTIFICATION_SENDER));
                notificationSpec.subject = sanitizeNotifText(intent.getStringExtra(EXTRA_NOTIFICATION_SUBJECT));
                notificationSpec.title = sanitizeNotifText(intent.getStringExtra(EXTRA_NOTIFICATION_TITLE));
                notificationSpec.body = sanitizeNotifText(intent.getStringExtra(EXTRA_NOTIFICATION_BODY));
                notificationSpec.sourceName = intent.getStringExtra(EXTRA_NOTIFICATION_SOURCENAME);
                notificationSpec.type = (NotificationType) intent.getSerializableExtra(EXTRA_NOTIFICATION_TYPE);
                notificationSpec.attachedActions = (ArrayList<NotificationSpec.Action>) intent.getSerializableExtra(EXTRA_NOTIFICATION_ACTIONS);
                notificationSpec.pebbleColor = (byte) intent.getSerializableExtra(EXTRA_NOTIFICATION_PEBBLE_COLOR);
                notificationSpec.flags = intent.getIntExtra(EXTRA_NOTIFICATION_FLAGS, 0);
                notificationSpec.sourceAppId = intent.getStringExtra(EXTRA_NOTIFICATION_SOURCEAPPID);

                if (notificationSpec.type == NotificationType.GENERIC_SMS && notificationSpec.phoneNumber != null) {
                    GBApplication.getIDSenderLookup().add(notificationSpec.getId(), notificationSpec.phoneNumber);
                }

                //TODO: check if at least one of the attached actions is a reply action instead?
                if ((notificationSpec.attachedActions != null && notificationSpec.attachedActions.size() > 0)
                        || (notificationSpec.type == NotificationType.GENERIC_SMS && notificationSpec.phoneNumber != null)) {
                    // NOTE: maybe not where it belongs
                    // I would rather like to save that as an array in SharedPreferences
                    // this would work but I dont know how to do the same in the Settings Activity's xml
                    ArrayList<String> replies = new ArrayList<>();
                    for (int i = 1; i <= 16; i++) {
                        String reply = prefs.getString("canned_reply_" + i, null);
                        if (reply != null && !reply.equals("")) {
                            replies.add(reply);
                        }
                    }
                    notificationSpec.cannedReplies = replies.toArray(new String[replies.size()]);
                }

                mDeviceSupport.onNotification(notificationSpec);
                break;
            }
            case ACTION_DELETE_NOTIFICATION: {
                mDeviceSupport.onDeleteNotification(intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1));
                break;
            }
            case ACTION_ADD_CALENDAREVENT: {
                CalendarEventSpec calendarEventSpec = new CalendarEventSpec();
                calendarEventSpec.id = intent.getLongExtra(EXTRA_CALENDAREVENT_ID, -1);
                calendarEventSpec.type = intent.getByteExtra(EXTRA_CALENDAREVENT_TYPE, (byte) -1);
                calendarEventSpec.timestamp = intent.getIntExtra(EXTRA_CALENDAREVENT_TIMESTAMP, -1);
                calendarEventSpec.durationInSeconds = intent.getIntExtra(EXTRA_CALENDAREVENT_DURATION, -1);
                calendarEventSpec.title = sanitizeNotifText(intent.getStringExtra(EXTRA_CALENDAREVENT_TITLE));
                calendarEventSpec.description = sanitizeNotifText(intent.getStringExtra(EXTRA_CALENDAREVENT_DESCRIPTION));
                calendarEventSpec.location = sanitizeNotifText(intent.getStringExtra(EXTRA_CALENDAREVENT_LOCATION));
                mDeviceSupport.onAddCalendarEvent(calendarEventSpec);
                break;
            }
            case ACTION_DELETE_CALENDAREVENT: {
                long id = intent.getLongExtra(EXTRA_CALENDAREVENT_ID, -1);
                byte type = intent.getByteExtra(EXTRA_CALENDAREVENT_TYPE, (byte) -1);
                mDeviceSupport.onDeleteCalendarEvent(type, id);
                break;
            }
            case ACTION_RESET: {
                int flags = intent.getIntExtra(EXTRA_RESET_FLAGS, 0);
                mDeviceSupport.onReset(flags);
                break;
            }
            case ACTION_HEARTRATE_TEST: {
                mDeviceSupport.onHeartRateTest();
                break;
            }
            case ACTION_FETCH_RECORDED_DATA: {
                int dataTypes = intent.getIntExtra(EXTRA_RECORDED_DATA_TYPES, 0);
                mDeviceSupport.onFetchRecordedData(dataTypes);
                break;
            }
            case ACTION_DISCONNECT: {
                mDeviceSupport.dispose();
                if (mGBDevice != null) {
                    mGBDevice.setState(GBDevice.State.NOT_CONNECTED);
                    mGBDevice.sendDeviceUpdateIntent(this);
                }
                setReceiversEnableState(false, false, null);
                mGBDevice = null;
                mDeviceSupport = null;
                mCoordinator = null;
                break;
            }
            case ACTION_FIND_DEVICE: {
                boolean start = intent.getBooleanExtra(EXTRA_FIND_START, false);
                mDeviceSupport.onFindDevice(start);
                break;
            }
            case ACTION_SET_CONSTANT_VIBRATION: {
                int intensity = intent.getIntExtra(EXTRA_VIBRATION_INTENSITY, 0);
                mDeviceSupport.onSetConstantVibration(intensity);
                break;
            }
            case ACTION_CALLSTATE:
                CallSpec callSpec = new CallSpec();
                callSpec.command = intent.getIntExtra(EXTRA_CALL_COMMAND, CallSpec.CALL_UNDEFINED);
                callSpec.number = intent.getStringExtra(EXTRA_CALL_PHONENUMBER);
                callSpec.name = sanitizeNotifText(intent.getStringExtra(EXTRA_CALL_DISPLAYNAME));
                mDeviceSupport.onSetCallState(callSpec);
                break;
            case ACTION_SETCANNEDMESSAGES:
                int type = intent.getIntExtra(EXTRA_CANNEDMESSAGES_TYPE, -1);
                String[] cannedMessages = intent.getStringArrayExtra(EXTRA_CANNEDMESSAGES);

                CannedMessagesSpec cannedMessagesSpec = new CannedMessagesSpec();
                cannedMessagesSpec.type = type;
                cannedMessagesSpec.cannedMessages = cannedMessages;
                mDeviceSupport.onSetCannedMessages(cannedMessagesSpec);
                break;
            case ACTION_SETTIME:
                mDeviceSupport.onSetTime();
                break;
            case ACTION_SETMUSICINFO:
                MusicSpec musicSpec = new MusicSpec();
                musicSpec.artist = sanitizeNotifText(intent.getStringExtra(EXTRA_MUSIC_ARTIST));
                musicSpec.album = sanitizeNotifText(intent.getStringExtra(EXTRA_MUSIC_ALBUM));
                musicSpec.track = sanitizeNotifText(intent.getStringExtra(EXTRA_MUSIC_TRACK));
                musicSpec.duration = intent.getIntExtra(EXTRA_MUSIC_DURATION, 0);
                musicSpec.trackCount = intent.getIntExtra(EXTRA_MUSIC_TRACKCOUNT, 0);
                musicSpec.trackNr = intent.getIntExtra(EXTRA_MUSIC_TRACKNR, 0);
                mDeviceSupport.onSetMusicInfo(musicSpec);
                break;
            case ACTION_SETMUSICSTATE:
                MusicStateSpec stateSpec = new MusicStateSpec();
                stateSpec.shuffle = intent.getByteExtra(EXTRA_MUSIC_SHUFFLE, (byte) 0);
                stateSpec.repeat = intent.getByteExtra(EXTRA_MUSIC_REPEAT, (byte) 0);
                stateSpec.position = intent.getIntExtra(EXTRA_MUSIC_POSITION, 0);
                stateSpec.playRate = intent.getIntExtra(EXTRA_MUSIC_RATE, 0);
                stateSpec.state = intent.getByteExtra(EXTRA_MUSIC_STATE, (byte) 0);
                mDeviceSupport.onSetMusicState(stateSpec);
                break;
            case ACTION_REQUEST_APPINFO:
                mDeviceSupport.onAppInfoReq();
                break;
            case ACTION_REQUEST_SCREENSHOT:
                mDeviceSupport.onScreenshotReq();
                break;
            case ACTION_STARTAPP: {
                UUID uuid = (UUID) intent.getSerializableExtra(EXTRA_APP_UUID);
                boolean start = intent.getBooleanExtra(EXTRA_APP_START, true);
                mDeviceSupport.onAppStart(uuid, start);
                break;
            }
            case ACTION_DELETEAPP: {
                UUID uuid = (UUID) intent.getSerializableExtra(EXTRA_APP_UUID);
                mDeviceSupport.onAppDelete(uuid);
                break;
            }
            case ACTION_APP_CONFIGURE: {
                UUID uuid = (UUID) intent.getSerializableExtra(EXTRA_APP_UUID);
                String config = intent.getStringExtra(EXTRA_APP_CONFIG);
                Integer id = null;
                if (intent.hasExtra(EXTRA_APP_CONFIG_ID)) {
                    id = intent.getIntExtra(EXTRA_APP_CONFIG_ID, 0);
                }
                mDeviceSupport.onAppConfiguration(uuid, config, id);
                break;
            }
            case ACTION_APP_REORDER: {
                UUID[] uuids = (UUID[]) intent.getSerializableExtra(EXTRA_APP_UUID);
                mDeviceSupport.onAppReorder(uuids);
                break;
            }
            case ACTION_INSTALL:
                Uri uri = intent.getParcelableExtra(EXTRA_URI);
                if (uri != null) {
                    LOG.info("will try to install app/fw");
                    mDeviceSupport.onInstallApp(uri);
                }
                break;
            case ACTION_SET_ALARMS:
                ArrayList<? extends Alarm> alarms = (ArrayList<? extends Alarm>) intent.getSerializableExtra(EXTRA_ALARMS);
                mDeviceSupport.onSetAlarms(alarms);
                break;
            case ACTION_ENABLE_REALTIME_STEPS: {
                boolean enable = intent.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                mDeviceSupport.onEnableRealtimeSteps(enable);
                break;
            }
            case ACTION_ENABLE_HEARTRATE_SLEEP_SUPPORT: {
                boolean enable = intent.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                mDeviceSupport.onEnableHeartRateSleepSupport(enable);
                break;
            }
            case ACTION_SET_HEARTRATE_MEASUREMENT_INTERVAL: {
                int seconds = intent.getIntExtra(EXTRA_INTERVAL_SECONDS, 0);
                mDeviceSupport.onSetHeartRateMeasurementInterval(seconds);
                break;
            }
            case ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT: {
                boolean enable = intent.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                mDeviceSupport.onEnableRealtimeHeartRateMeasurement(enable);
                break;
            }
            case ACTION_SEND_CONFIGURATION: {
                String config = intent.getStringExtra(EXTRA_CONFIG);
                mDeviceSupport.onSendConfiguration(config);
                break;
            }
            case ACTION_READ_CONFIGURATION: {
                String config = intent.getStringExtra(EXTRA_CONFIG);
                mDeviceSupport.onReadConfiguration(config);
                break;
            }
            case ACTION_TEST_NEW_FUNCTION: {
                mDeviceSupport.onTestNewFunction();
                break;
            }
            case ACTION_SEND_WEATHER: {
                WeatherSpec weatherSpec = intent.getParcelableExtra(EXTRA_WEATHER);
                if (weatherSpec != null) {
                    mDeviceSupport.onSendWeather(weatherSpec);
                }
                break;
            }
            case ACTION_SET_LED_COLOR:
                int color = intent.getIntExtra(EXTRA_LED_COLOR, 0);
                if (color != 0) {
                    mDeviceSupport.onSetLedColor(color);
                }
                break;
            case ACTION_SET_FM_FREQUENCY:
                float frequency = intent.getFloatExtra(EXTRA_FM_FREQUENCY, -1);
                if (frequency != -1) {
                    mDeviceSupport.onSetFmFrequency(frequency);
                }
                break;
        }
    }

    private void sendNotificationSaved ()
    {
        if (notificationSaved != null && mDeviceSupport != null && isInitialized ())
        {
            LOG.debug ("Send notification saved: " + notificationSaved);
            handleAction (notificationSaved, notificationSaved.getAction (), getPrefs ());
        }
    }

    /**
     * Disposes the current DeviceSupport instance (if any) and sets a new device support instance
     * (if not null).
     *
     * @param deviceSupport
     */
    private void setDeviceSupport(@Nullable DeviceSupport deviceSupport) {
        if (deviceSupport != mDeviceSupport && mDeviceSupport != null) {
            mDeviceSupport.dispose();
            mDeviceSupport = null;
            mGBDevice = null;
            mCoordinator = null;
        }
        mDeviceSupport = deviceSupport;
        mGBDevice = mDeviceSupport != null ? mDeviceSupport.getDevice() : null;
        mCoordinator = mGBDevice != null ? DeviceHelper.getInstance().getCoordinator(mGBDevice) : null;
    }

    private void start() {
        if (!mStarted) {
            startForeground(GB.NOTIFICATION_ID, GB.createNotification(getString(R.string.gadgetbridge_running), this));
            mStarted = true;
        }
    }

    public boolean isStarted() {
        return mStarted;
    }

    private boolean isConnected() {
        return mGBDevice != null && mGBDevice.isConnected();
    }

    private boolean isConnecting() {
        return mGBDevice != null && mGBDevice.isConnecting();
    }

    private boolean isInitialized() {
        return mGBDevice != null && mGBDevice.isInitialized();
    }


    private void setReceiversEnableState(boolean enable, boolean initialized, DeviceCoordinator coordinator) {
        LOG.info("Setting broadcast receivers to: " + enable);

        if (enable && initialized && coordinator != null && coordinator.supportsCalendarEvents()) {
            if (mCalendarReceiver == null && getPrefs().getBoolean("enable_calendar_sync", true)) {
                if (!(GBApplication.isRunningMarshmallowOrLater() && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_DENIED)) {
                    IntentFilter calendarIntentFilter = new IntentFilter();
                    calendarIntentFilter.addAction("android.intent.action.PROVIDER_CHANGED");
                    calendarIntentFilter.addDataScheme("content");
                    calendarIntentFilter.addDataAuthority("com.android.calendar", null);
                    mCalendarReceiver = new CalendarReceiver(mGBDevice);
                    registerReceiver(mCalendarReceiver, calendarIntentFilter);
                }
            }
            if (mAlarmReceiver == null) {
                mAlarmReceiver = new AlarmReceiver();
                registerReceiver(mAlarmReceiver, new IntentFilter("DAILY_ALARM"));
            }
        } else {
            if (mCalendarReceiver != null) {
                unregisterReceiver(mCalendarReceiver);
                mCalendarReceiver = null;
            }
            if (mAlarmReceiver != null) {
                unregisterReceiver(mAlarmReceiver);
                mAlarmReceiver = null;
            }
        }

        if (enable) {
            if (mPhoneCallReceiver == null) {
                mPhoneCallReceiver = new PhoneCallReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.PHONE_STATE");
                filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
                filter.addAction("org.likeapp.likeapp.MUTE_CALL");
                registerReceiver(mPhoneCallReceiver, filter);
            }
            if (mSMSReceiver == null) {
                mSMSReceiver = new SMSReceiver();
                registerReceiver(mSMSReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
            }
            if (mPebbleReceiver == null) {
                mPebbleReceiver = new PebbleReceiver();
                registerReceiver(mPebbleReceiver, new IntentFilter("com.getpebble.action.SEND_NOTIFICATION"));
            }
            if (mMusicPlaybackReceiver == null && coordinator != null && coordinator.supportsMusicInfo()) {
                mMusicPlaybackReceiver = new MusicPlaybackReceiver();
                IntentFilter filter = new IntentFilter();
                for (String action : mMusicActions) {
                    filter.addAction(action);
                }
                registerReceiver(mMusicPlaybackReceiver, filter);
            }
            if (mTimeChangeReceiver == null) {
                mTimeChangeReceiver = new TimeChangeReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.TIME_SET");
                filter.addAction("android.intent.action.TIMEZONE_CHANGED");
                registerReceiver(mTimeChangeReceiver, filter);
            }
            if (mBlueToothConnectReceiver == null) {
                mBlueToothConnectReceiver = new BluetoothConnectReceiver(this);
                registerReceiver(mBlueToothConnectReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
            }
            if (mBlueToothPairingRequestReceiver == null) {
                mBlueToothPairingRequestReceiver = new BluetoothPairingRequestReceiver(this);
                registerReceiver(mBlueToothPairingRequestReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));
            }
            if (mAlarmClockReceiver == null) {
                mAlarmClockReceiver = new AlarmClockReceiver();
                IntentFilter filter = new IntentFilter();

                for (String a : AlarmClockReceiver.ALARM_ALERT_ACTIONS)
                {
                    filter.addAction (a);
                }

                for (String a : AlarmClockReceiver.ALARM_DONE_ACTIONS)
                {
                    filter.addAction (a);
                }

                registerReceiver(mAlarmClockReceiver, filter);
            }

            // Weather receivers
            if ( coordinator != null && coordinator.supportsWeather()) {
                if (GBApplication.isRunningOreoOrLater()) {
                    if (mLineageOsWeatherReceiver == null) {
                        mLineageOsWeatherReceiver = new LineageOsWeatherReceiver();
                        registerReceiver(mLineageOsWeatherReceiver, new IntentFilter("GB_UPDATE_WEATHER"));
                    }
                }
                else {
                    if (mCMWeatherReceiver == null) {
                        mCMWeatherReceiver = new CMWeatherReceiver();
                        registerReceiver(mCMWeatherReceiver, new IntentFilter("GB_UPDATE_WEATHER"));
                    }
                }
                if (mTinyWeatherForecastGermanyReceiver == null) {
                    mTinyWeatherForecastGermanyReceiver = new TinyWeatherForecastGermanyReceiver();
                    registerReceiver(mTinyWeatherForecastGermanyReceiver, new IntentFilter("de.kaffeemitkoffein.broadcast.WEATHERDATA"));
                }
                if (mOmniJawsObserver == null) {
                    try {
                        mOmniJawsObserver = new OmniJawsObserver(new Handler());
                        getContentResolver().registerContentObserver(OmniJawsObserver.WEATHER_URI, true, mOmniJawsObserver);
                    } catch (PackageManager.NameNotFoundException e) {
                        //Nothing wrong, it just means we're not running on omnirom.
                    }
                }
            }

            if (GBApplication.getPrefs().getBoolean("auto_fetch_enabled", false) &&
                    coordinator != null && coordinator.supportsActivityDataFetching() && mGBAutoFetchReceiver == null) {
                mGBAutoFetchReceiver = new GBAutoFetchReceiver();
                registerReceiver(mGBAutoFetchReceiver, new IntentFilter("android.intent.action.USER_PRESENT"));
            }
            if (mAutoConnectIntervalReceiver == null) {
                mAutoConnectIntervalReceiver = new AutoConnectIntervalReceiver(this);
                registerReceiver(mAutoConnectIntervalReceiver, new IntentFilter("GB_RECONNECT"));
            }

            // Отправить сохранённое уведомление
            sendNotificationSaved ();
        } else {
            if (mPhoneCallReceiver != null) {
                unregisterReceiver(mPhoneCallReceiver);
                mPhoneCallReceiver = null;
            }
            if (mSMSReceiver != null) {
                unregisterReceiver(mSMSReceiver);
                mSMSReceiver = null;
            }
            if (mPebbleReceiver != null) {
                unregisterReceiver(mPebbleReceiver);
                mPebbleReceiver = null;
            }
            if (mMusicPlaybackReceiver != null) {
                unregisterReceiver(mMusicPlaybackReceiver);
                mMusicPlaybackReceiver = null;
            }
            if (mTimeChangeReceiver != null) {
                unregisterReceiver(mTimeChangeReceiver);
                mTimeChangeReceiver = null;
            }
            if (mBlueToothConnectReceiver != null) {
                unregisterReceiver(mBlueToothConnectReceiver);
                mBlueToothConnectReceiver = null;
            }

            if (mBlueToothPairingRequestReceiver != null) {
                unregisterReceiver(mBlueToothPairingRequestReceiver);
                mBlueToothPairingRequestReceiver = null;
            }
            if (mAlarmClockReceiver != null) {
                unregisterReceiver(mAlarmClockReceiver);
                mAlarmClockReceiver = null;
            }
            if (mCMWeatherReceiver != null) {
                unregisterReceiver(mCMWeatherReceiver);
                mCMWeatherReceiver = null;
            }
            if (mLineageOsWeatherReceiver != null) {
                unregisterReceiver(mLineageOsWeatherReceiver);
                mLineageOsWeatherReceiver = null;
            }
            if (mOmniJawsObserver != null) {
                getContentResolver().unregisterContentObserver(mOmniJawsObserver);
                mOmniJawsObserver = null;
            }
            if (mTinyWeatherForecastGermanyReceiver != null) {
                unregisterReceiver(mTinyWeatherForecastGermanyReceiver);
                mTinyWeatherForecastGermanyReceiver = null;
            }
            if (mGBAutoFetchReceiver != null) {
                unregisterReceiver(mGBAutoFetchReceiver);
                mGBAutoFetchReceiver = null;
            }
            if (mAutoConnectIntervalReceiver != null) {
                unregisterReceiver(mAutoConnectIntervalReceiver);
                mAutoConnectIntervalReceiver.destroy();
                mAutoConnectIntervalReceiver = null;
            }
        }
    }

    @Override
    public void onDestroy() {
        if (hasPrefs()) {
            getPrefs().getPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        LOG.debug("DeviceCommunicationService is being destroyed");
        super.onDestroy();

        unregisterReceiver (mBatteryLevelReceiver);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        setReceiversEnableState(false, false, null); // disable BroadcastReceivers

        setDeviceSupport(null);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(GB.NOTIFICATION_ID); // need to do this because the updated notification won't be cancelled when service stops
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (GBPrefs.AUTO_RECONNECT.equals(key)) {
            boolean autoReconnect = getGBPrefs().getAutoReconnect();
            if (mDeviceSupport != null) {
                mDeviceSupport.setAutoReconnect(autoReconnect);
            }
        }
        if (GBPrefs.CHART_MAX_HEART_RATE.equals(key) || GBPrefs.CHART_MIN_HEART_RATE.equals(key)) {
            HeartRateUtils.getInstance().updateCachedHeartRatePreferences();
        }
    }

    protected boolean hasPrefs() {
        return getPrefs().getPreferences() != null;
    }

    public Prefs getPrefs() {
        return GBApplication.getPrefs();
    }

    public GBPrefs getGBPrefs() {
        return GBApplication.getGBPrefs();
    }

    public GBDevice getGBDevice() {
        return mGBDevice;
    }
}
