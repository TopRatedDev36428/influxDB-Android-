package com.clover.influxdb.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class InfluxDbDatabase  {

  private static final int DATABASE_VERSION = 1;
  private static final String DATABASE_NAME = "influxdb.db";

  private DatabaseHelper helper = null;

  private synchronized DatabaseHelper getDbHelper() {
    if (helper == null) {
      helper = new DatabaseHelper(context, DATABASE_NAME);
    }

    return helper;
  }

  private static class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context, String name) {
      super(context, name, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      String cmd = "CREATE TABLE IF NOT EXISTS " + InfluxDbContract.Points.CONTENT_DIRECTORY + " ("
          + InfluxDbContract.Points._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
          + InfluxDbContract.Points.POINT + " TEXT NOT NULL,"
          + InfluxDbContract.Points.CREATED_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL"
          + ");";
      db.execSQL(cmd);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.i(InfluxDb.TAG, "upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
      db.execSQL("DROP TABLE IF EXISTS " + InfluxDbContract.Points.CONTENT_DIRECTORY);
      onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      onUpgrade(db, oldVersion, newVersion);
    }
  }
  
  private static final Map<String, String> projection = new HashMap<String, String>();

  private final Context context;

  InfluxDbDatabase(Context context) {
    this.context = context;

    projection.put(InfluxDbContract.Points._ID, InfluxDbContract.Points._ID);
    projection.put(InfluxDbContract.Points.POINT, InfluxDbContract.Points.POINT);
    projection.put(InfluxDbContract.Points.CREATED_TIME, InfluxDbContract.Points.CREATED_TIME);
  }

  int delete(String whereClause, String[] whereArgs) {
    DatabaseHelper dbHelper = getDbHelper();
    if (dbHelper == null) {
      return 0;
    }

    SQLiteDatabase db = dbHelper.getWritableDatabase();

    int count = 0;

    db.beginTransaction();
    try {
      count = db.delete(InfluxDbContract.Points.CONTENT_DIRECTORY, whereClause, whereArgs);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }

    return count;
  }

  long insert(ContentValues values) {
    DatabaseHelper dbHelper = getDbHelper();
    if (dbHelper == null) {
      return -1;
    }

    SQLiteDatabase db = dbHelper.getWritableDatabase();

    long rowId = -1;
    db.beginTransaction();
    try {
      rowId = db.insertWithOnConflict(InfluxDbContract.Points.CONTENT_DIRECTORY, null, values, SQLiteDatabase.CONFLICT_FAIL);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }


    trim();
    
    return rowId;
  }

  Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    long id = -1;

    DatabaseHelper dbHelper = getDbHelper();
    if (dbHelper == null) {
      return null;
    }

    qb.setTables(InfluxDbContract.Points.CONTENT_DIRECTORY);
    qb.setProjectionMap(InfluxDbDatabase.projection);

    SQLiteDatabase db = dbHelper.getWritableDatabase();
    return qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
  }

  int update(ContentValues values, String selection, String[] selectionArgs) {
    throw new UnsupportedOperationException();
  }

  void shutdown() {
    synchronized (this) {
      if (helper != null) {
        helper.close();
      }
    }
  }

  int bulkInsert(ContentValues[] values){
    int numInserted = 0;

    SQLiteDatabase db = getDbHelper().getWritableDatabase();
    db.beginTransaction();
    try {
      for (ContentValues cv : values) {
        long newId = db.insertWithOnConflict(InfluxDbContract.Points.CONTENT_DIRECTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (newId <= 0) {
          throw new SQLException("failed to insert row");
        }
      }
      db.setTransactionSuccessful();
      numInserted = values.length;
    } finally {
      db.endTransaction();
    }

    trim();

    return numInserted;
  }

  static final int TRIM_SIZE = 10000;

  public boolean trim() {
    SQLiteDatabase db = getDbHelper().getWritableDatabase();

    try {
      String sql = String.format(
          "DELETE FROM %s WHERE %s IN (SELECT %s FROM %s ORDER BY %s DESC LIMIT -1 OFFSET %d)",
          InfluxDbContract.Points.CONTENT_DIRECTORY,
          InfluxDbContract.Points._ID,
          InfluxDbContract.Points._ID,
          InfluxDbContract.Points.CONTENT_DIRECTORY,
          InfluxDbContract.Points._ID,
          TRIM_SIZE
      );
      db.execSQL(sql);
      return true;
    } catch (Exception e) {
      Log.e(InfluxDb.TAG, "error trimming database", e);
    }

    return false;
  }
}
