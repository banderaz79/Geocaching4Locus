package com.arcao.geocaching4locus.live_map;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.arcao.geocaching4locus.base.constants.AppConstants;
import com.arcao.geocaching4locus.base.util.ServiceUtil;
import com.arcao.geocaching4locus.live_map.task.LiveMapDownloadTask;
import com.arcao.geocaching4locus.live_map.util.LiveMapNotificationManager;

public class LiveMapService extends Service {
    public static final String PARAM_LATITUDE = "LATITUDE";
    public static final String PARAM_LONGITUDE = "LONGITUDE";
    public static final String PARAM_TOP_LEFT_LATITUDE = "TOP_LEFT_LATITUDE";
    public static final String PARAM_TOP_LEFT_LONGITUDE = "TOP_LEFT_LONGITUDE";
    public static final String PARAM_BOTTOM_RIGHT_LATITUDE = "BOTTOM_RIGHT_LATITUDE";
    public static final String PARAM_BOTTOM_RIGHT_LONGITUDE = "BOTTOM_RIGHT_LONGITUDE";
    private static final String ACTION_START = LiveMapService.class.getCanonicalName() + ".START";
    private static final String ACTION_STOP = LiveMapService.class.getCanonicalName() + ".STOP";

    private LiveMapDownloadTask downloadTask;
    private LiveMapNotificationManager notificationManager;

    public static void stop(Context context) {
        context.stopService(new Intent(context, LiveMapService.class).setAction(ACTION_STOP));
    }

    public static ComponentName start(Context context, double latitude, double longitude, double topLeftLatitude, double topLeftLongitude, double bottomRightLatitude, double bottomRightLongitude) {
        Intent intent = new Intent(context, LiveMapService.class);

        intent.setAction(ACTION_START);
        intent.putExtra(PARAM_LATITUDE, latitude);
        intent.putExtra(PARAM_LONGITUDE, longitude);
        intent.putExtra(PARAM_TOP_LEFT_LATITUDE, topLeftLatitude);
        intent.putExtra(PARAM_TOP_LEFT_LONGITUDE, topLeftLongitude);
        intent.putExtra(PARAM_BOTTOM_RIGHT_LATITUDE, bottomRightLatitude);
        intent.putExtra(PARAM_BOTTOM_RIGHT_LONGITUDE, bottomRightLongitude);

        return ServiceUtil.startWakefulForegroundService(context, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = LiveMapNotificationManager.get(this);
        startForeground(AppConstants.NOTIFICATION_ID_LIVEMAP, notificationManager.createNotification().build());

        downloadTask = new LiveMapDownloadTask(this, notificationManager) {
            @Override
            public void onTaskFinished(Intent task) {
                ServiceUtil.completeWakefulIntent(task);
            }
        };
        downloadTask.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // in case the service is already running, this must be called after each startForegroundService
        startForeground(AppConstants.NOTIFICATION_ID_LIVEMAP, notificationManager.createNotification().build());

        if (intent != null) {
            if (ACTION_START.equals(intent.getAction())) {
                downloadTask.addTask(intent);
            } else if (ACTION_STOP.equals(intent.getAction())) {
                cancelTasks();
                stopSelf(startId);
            }
        }

        return START_STICKY;
    }

    private void cancelTasks() {
        downloadTask.cancel();
        stopForeground(true);
        ServiceUtil.releaseAllWakeLocks(new ComponentName(this, LiveMapService.class));
    }

    @Override
    public void onDestroy() {
        cancelTasks();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
