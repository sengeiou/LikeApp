/*  Copyright (C) 2020 Andreas Shimokawa

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

package org.likeapp.likeapp.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.likeapp.likeapp.GBApplication;
import org.likeapp.likeapp.model.Weather;
import org.likeapp.likeapp.model.WeatherSpec;

public class TinyWeatherForecastGermanyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                WeatherSpec weatherSpec = bundle.getParcelable("WeatherSpec");
                if (weatherSpec != null) {
                    Weather.getInstance().setWeatherSpec(weatherSpec);
                    GBApplication.deviceService().onSendWeather(weatherSpec);
                }
            }
        }
    }
}