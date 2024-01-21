package com.clover.influxdb.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class WriteReceiver extends BroadcastReceiver {
  private static final String PREF_IS_CONNECTED = WriteReceiver.class.getName() + ".connected";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
      // if we transition from not connected to connected, attempt to process points

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      boolean connected = WriteService.isNetworkConnected(context);
      boolean lastConnected = prefs.getBoolean(PREF_IS_CONNECTED, connected);

      Log.i(InfluxDb.TAG, "connectivity change: connected? " + connected + ", last connected? " + lastConnected);

      if (connected && !lastConnected) {
        context.startService(new Intent(context, WriteService.class));
      }

      prefs.edit().putBoolean(PREF_IS_CONNECTED, connected).apply();
    } else {
      context.startService(new Intent(context, WriteService.class));
    }
  }

  static void schedule(Context context, long delay) {
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, getAlarmIntent(context));
  }

  static void cancel(Context context) {
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    am.cancel(getAlarmIntent(context));
  }

  private static PendingIntent getAlarmIntent(Context context) {
    Intent intent = new Intent(context, WriteReceiver.class);
    return PendingIntent.getBroadcast(context, 0, intent, 0);
  }
}
