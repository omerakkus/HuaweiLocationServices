package com.huawei.geofencetest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.huawei.hms.location.ActivityConversionData;
import com.huawei.hms.location.ActivityConversionResponse;
import com.huawei.hms.location.ActivityIdentificationData;
import com.huawei.hms.location.ActivityIdentificationResponse;

import java.util.List;

public class RecognitionBroadcastReceiver extends BroadcastReceiver {
    public static boolean isListenActivityConversion = false;
    public static boolean isListenActivityIdentification = false;
    private static final String TAG = "LocationReceiver";
    public static final String ACTION_PROCESS_LOCATION = "com.huawei.hms.location.ACTION_PROCESS_LOCATION";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String messageBack = "";
            final String action = intent.getAction();
            if (ACTION_PROCESS_LOCATION.equals(action)) {
                ActivityConversionResponse activityTransitionResult = ActivityConversionResponse.getDataFromIntent(intent);
                if(activityTransitionResult!=null && isListenActivityConversion){
                    List<ActivityConversionData> list =activityTransitionResult.getActivityConversionDatas();
                    for (int i = 0; i < list.size(); i++) {
                        Log.i(TAG, "activityTransitionEvent[" + i + "]" + list.get(i));
                        messageBack += list.get(i) + "\n";
                    }
                    Log.d(TAG, messageBack);
                }

                ActivityIdentificationResponse activityRecognitionResult = ActivityIdentificationResponse.getDataFromIntent(intent);
                    if (activityRecognitionResult != null && isListenActivityIdentification){
                        Log.i(TAG, "activityRecognitionResult:" + activityRecognitionResult);
                        List<ActivityIdentificationData> list = activityRecognitionResult.getActivityIdentificationDatas();
                        MainActivity.sendData(list);
                }

            }
        }
    }

    public static void addConversionListener() {
        isListenActivityConversion = true;
    }

    public static void addIdentificationListener() {
        isListenActivityIdentification = true;
    }
}
