package com.huawei.geofencetest;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.huawei.hms.location.Geofence;
import com.huawei.hms.location.GeofenceData;

import java.util.ArrayList;

public class GeofenceIntentService extends JobIntentService {

    private static final int JOB_ID = 573;
    private static final String CHANNEL_ID = "channel_01";

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        GeofenceData geofenceData = GeofenceData.getDataFromIntent(intent);
        if(geofenceData!=null){
            int conversion = geofenceData.getConversion();
            ArrayList<Geofence> geofenceTransition =(ArrayList<Geofence>) geofenceData.getConvertingGeofenceList();
            String geofenceTransitionDetails = getGeofenceTransitionDetails(conversion,
                    geofenceTransition);
            sendNotification(geofenceTransitionDetails);
            Log.i("GeofenceTest", geofenceTransitionDetails);
        }
    }

    private String getGeofenceTransitionDetails(int conversion, ArrayList<Geofence> triggeringGeofences) {
            String geofenceConversion = getConversionString(conversion);
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for(Geofence geofence : triggeringGeofences){
            triggeringGeofencesIdsList.add(geofence.getUniqueId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);
        return geofenceConversion + ": " + triggeringGeofencesIdsString;
    }


    private void sendNotification(String notificationDetails) {
        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }

        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_marker)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_marker))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Issue the notification
        assert mNotificationManager != null;
        mNotificationManager.notify(0, builder.build());

    }

    private String getConversionString(int transitionType) {
        switch (transitionType) {
            case Geofence.ENTER_GEOFENCE_CONVERSION:
                return getString(R.string.geofence_transition_entered);
            case Geofence.EXIT_GEOFENCE_CONVERSION:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }

}
