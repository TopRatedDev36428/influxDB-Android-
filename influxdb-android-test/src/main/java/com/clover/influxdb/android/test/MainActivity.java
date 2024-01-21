package com.clover.influxdb.android.test;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import com.clover.influxdb.android.InfluxDb;
import com.clover.influxdb.android.Point;

import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity {
  static double value = 0;


  private Button writePointButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    writePointButton = (Button) findViewById(R.id.button_write_point);
    writePointButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Point p = Point.measurement("test2")
            .tag("serial", Build.SERIAL)
            .tag("model", Build.MODEL)
            .field("sine", Math.sin(value++))
            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .build();
        InfluxDb.instance(MainActivity.this).write(p);
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
