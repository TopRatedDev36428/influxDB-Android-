package com.clover.influxdb.android;

import android.provider.BaseColumns;

public abstract class InfluxDbContract {
  public static final class Points implements BaseColumns {
    public static final String CONTENT_DIRECTORY = "points";

    public static final String POINT = "point";
    public static final String CREATED_TIME = "created_time";
  }
}
