package com.clover.influxdb.android;

import android.content.Context;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class InfluxDbConfig extends XmlResourceParser {
  private static InfluxDbConfig instance = null;

  static synchronized InfluxDbConfig instance(Context context) throws IOException, XmlPullParserException {
    if (instance == null) {
      instance = new InfluxDbConfig(context.getApplicationContext());
      instance.parse();
    }
    return instance;
  }

  private InfluxDbConfig(Context context) {
    super(context);
  }

  public void parse() throws XmlPullParserException, IOException {
    int id = context.getResources().getIdentifier("influxdb", "xml", context.getPackageName());
    if (id == -1) {
      throw new IllegalArgumentException("influxdb config not found");
    }
    super.parse(id);
  }

  String getDbName() {
    return getString("db_name", null);
  }

  String getUrl() {
    return getString("url", null);
  }

  String getUser() {
    return getString("user", null);
  }

  String getPassword() {
    return getString("password", null);
  }

  int getWriteBatchSize() {
    return getInt("write_batch_size", context.getResources().getInteger(R.integer.default_write_batch_size));
  }

  int getWriteAtLeastEvery() {
    return getInt("write_at_least_every", context.getResources().getInteger(R.integer.default_write_at_least_every));
  }

  int getWriteAtMostEvery() {
    return getInt("write_at_most_every", context.getResources().getInteger(R.integer.default_write_at_most_every));
  }
}
