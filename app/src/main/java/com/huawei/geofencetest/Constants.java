/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.geofencetest;

import com.huawei.hms.maps.model.LatLng;
import java.util.HashMap;

/**
 * Constants used in this sample.
 */

final class Constants {

    private Constants() {
    }

    /**
     * Used to set an expiration time for a geofence. After this amount of time Location Services
     * stops tracking the geofence.
     */
    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    static final float GEOFENCE_RADIUS_IN_METERS = 1000;

    static final HashMap<String, LatLng> BAY_AREA_LANDMARKS = new HashMap<>();
    static {
        BAY_AREA_LANDMARKS.put("HOME", new LatLng(40.9988764,28.8470277));
        BAY_AREA_LANDMARKS.put("OFFICE", new LatLng(41.043912,29.1432343));
    }
}
