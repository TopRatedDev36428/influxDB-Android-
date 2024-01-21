package com.clover.influxdb.android;

import android.content.Context;
import android.util.Log;
import org.apache.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class InfluxDbService {
  private final Context context;

  public InfluxDbService(Context context) {
    this.context = context;
  }

  boolean write(List<String> lineProtocols) {
    boolean success = false;

    PrintWriter writer = null;
    BufferedReader reader = null;

    HttpURLConnection conn = null;
    try {
      InfluxDbConfig config = InfluxDbConfig.instance(context);
      URL url = new URL(String.format("%s/write?db=%s&u=%s&p=%s", config.getUrl(), config.getDbName(), config.getUser(), config.getPassword()));
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestProperty("Accept-Encoding", "");
      conn.setRequestMethod("POST");
      writer = new PrintWriter(conn.getOutputStream());
      for (String lp : lineProtocols) {
        writer.write(lp);
        writer.write("\n");
      }
      writer.flush();

      int responseCode = conn.getResponseCode();
      Log.i(InfluxDb.TAG, "write response, status: " + responseCode);
      if (responseCode == HttpStatus.SC_NO_CONTENT) {
        success = true;
      } else {
        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = reader.readLine()) != null) {
          response.append(inputLine);
        }
        Log.i(InfluxDb.TAG, "write response, content: " + response.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
      if (writer != null) {
        writer.close();
      }
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }

    return success;
  }
}
