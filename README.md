# influxdb-android
InfluxDB SDK for Android OS. This is a WIP. This SDK saves measurements and sends them efficiently in batches, considering network connectivity and other factors (described below). This library is for data collection only, and hence only supports `/write`.

This is an Android AAR library, not a JAR. You must use the Gradle build system to include an AAR. It is not currently published to Maven Central or any other repository. To use it, install it locally using gradle (`gradle install`), and and reference it from your dependencies.

```
dependencies {
  compile 'com.clover.influxdb.android:influxdb-android-lib:3-SNAPSHOT'
}
```

You must provide a config file named `influxdb.xml` in `res/xml`. E.g.,

```
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <!--
  REQUIRED: Influx DB name.
  -->
  <string name="db_name">metrics</string>
  <!--
  REQUIRED: Influx DB URL.
  -->
  <string name="url">http://influxdb.test.com</string>
  <!--
  REQUIRED: Influx DB user name.
  -->
  <string name="user">testuser</string>
  <!--
  REQUIRED: Influx DB password.
  -->
  <string name="password">testpassword</string>
  <!--
  OPTIONAL: Will wait until this many measurements are queued before sending,
  except when max write delay is exceeded (see below). Default value is 100.
  -->
  <integer name="write_batch_size">10</integer>
  <!--
  OPTIONAL: Send values to server at least every this number of seconds, irrespective of batch size.
  Default value is 300 (5 minutes).
  -->
  <integer name="write_at_least_every">60</integer>
  <!--
  OPTIONAL: Send values to server at most every this number of seconds, irrespective of batch size.
  Default value is 20.
  -->
  <integer name="write_at_most_every">30</integer>
</resources>
```

Then simply use the SDK class `InfluxDb`,

```
InfluxDb db = new InfluxDb(context);
Point p = Point.measurement("test2")
  .tag("serial", Build.SERIAL)
  .tag("model", Build.MODEL)
  .field("sine", Math.sin(value++))
  .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
  .build();
db.write(p);
```

Since `InfluxDb.write()` performs a database operation, it should not be called on the UI thread (run it in an `AsyncTask`).

## Write Configuration

The three write configuration settings, `write_batch_size`, `write_at_least_every`, and `write_at_most_every` need some explanation. These all control when queued measurements will be sent to the server. If the time since the last send is below `write_at_most_every`, then no data will be sent regardless of the number of queued points. This is to keep an app that generates many measurements from calling the server very frequently. If the time since the last send is above `write_at_least_every`, then measurements will be sent to the server, regardless of the number of queued points. This is to ensure that all points will be sent to the server in a timely manner. If neither of those apply, and the number of queued measurements is over `write_batch_size`, then the points will be sent to the server, otherwise, they will not.

### Batching 

In short, disregarding `write_at_least_every` and `write_at_most_every`, we only send things in batches of `write_batch_size`.

Disregarding `write_at_least_every` and `write_at_most_every` the SDK will get up to the oldest `write_batch_size` points from the DB. If the count is below `write_batch_size`, no points are sent. If it's equal, those points are sent, and up to the next `write_batch_size` measurements are fetched from the DB. Again, the same check is performed. The points are only sent if the count is equal to `write_batch_size`. 

## Why not influxdb-java?

influxdb-java is a pure Java SDK for InfluxDB and can of course be used on Android.
https://github.com/influxdb/influxdb-java

It didn't work for me, since it does not ensure reliable delivery, and has no way to integrate network connectivity awareness into the SDK. On mobile, that's expecially important as you don't want code banging on the network wasting battery when you know it's going to fail.

It also has extremely large transitive dependencies, including Guava and OkHttp. This is a real problem on Android where we are limited to 64k methods (unless we use multi-DEX, which is a problem in itself). 
