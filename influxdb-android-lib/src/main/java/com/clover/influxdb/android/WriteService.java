package com.clover.influxdb.android;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WriteService extends IntentService {
  private static final String PREF_LAST_WRITE = WriteService.class.getName() + ".lastWrite";

  private SharedPreferences prefs;

  public WriteService() {
    super(WriteService.class.getName());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (!isNetworkConnected(this)) {
      Log.i(InfluxDb.TAG, "network not connected, aborting");
      return;
    }

    InfluxDbService service = new InfluxDbService(this);
    InfluxDbConfig config = null;
    try {
      config = InfluxDbConfig.instance(this);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    long now = System.currentTimeMillis();
    long lastWrite = prefs.getLong(PREF_LAST_WRITE, -1);
    if (lastWrite == 1) {
      lastWrite = System.currentTimeMillis();
      prefs.edit().putLong(PREF_LAST_WRITE, lastWrite).apply();
    }

    long writeAtLeastEvery = config.getWriteAtMostEvery() * 1000;
    boolean writeAtLeast = lastWrite + writeAtLeastEvery > now;

    long writeAtMostEvery = config.getWriteAtLeastEvery() * 1000;
    boolean writeAtMost = lastWrite + writeAtMostEvery <= now;

    // proceeding with an attempted write, so cancel any scheduled operations
    WriteReceiver.cancel(this);

    while (true) {
      Cursor c = null;
      try {
        c = InfluxDb.instance(this).database.query(null, null, null, "_id ASC LIMIT " + config.getWriteBatchSize());
        if (c == null || !c.moveToFirst()) {
          Log.i(InfluxDb.TAG, "no (more) points, aborting");
          break;
        }
        Log.i(InfluxDb.TAG, "processing points, count: " + c.getCount());

        if (writeAtMost) {
          Log.i(InfluxDb.TAG, "write at most exceeded, ignoring batch size");
        } else if (writeAtLeast) {
          Log.i(InfluxDb.TAG, "below write at least, aborting");

          // schedule a retry when max write delay is expected to be expired
          long delay = (lastWrite + writeAtLeastEvery) - now;
          Log.i(InfluxDb.TAG, String.format("scheduling retry in ~%dms", delay));
          WriteReceiver.schedule(this, delay);

          break;
        } else {
          Log.i(InfluxDb.TAG, "max write delay not exceeded, checking batch size ...");
          if (c.getCount() < config.getWriteBatchSize()) {
            Log.i(InfluxDb.TAG, "count < batch size, aborting");

            // schedule a retry when max write delay is expected to be expired
            long delay = (lastWrite + writeAtMostEvery) - now;
            Log.i(InfluxDb.TAG, String.format("scheduling retry in ~%dms", delay));
            WriteReceiver.schedule(this, delay);

            break;
          }
        }

        final List<String> lineProtocols = new ArrayList<String>();
        long maxId = 0;
        final int idColumn = c.getColumnIndex(InfluxDbContract.Points._ID);
        final int pointColumn = c.getColumnIndex(InfluxDbContract.Points.POINT);

        while (!c.isAfterLast()) {
          maxId = c.getInt(idColumn);
          lineProtocols.add(c.getString(pointColumn));

          c.moveToNext();
        }

        Log.i(InfluxDb.TAG, "writing line protocols ...");
        if (service.write(lineProtocols)) {
          InfluxDb.instance(this).database.delete(InfluxDbContract.Points._ID + "<=" + maxId, null);
          prefs.edit().putLong(PREF_LAST_WRITE, now).apply();
          WriteReceiver.cancel(this);
          Log.i(InfluxDb.TAG, "done writing line protocols");
        } else {
          Log.w(InfluxDb.TAG, "failed writing line protocols");
          break;
        }
      } finally {
        if (c != null) {
          c.close();
        }
      }
    }
  }

  static boolean isNetworkConnected(Context context) {
    ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo active = connManager.getActiveNetworkInfo();

    return active != null && active.isConnected();
  }
}
