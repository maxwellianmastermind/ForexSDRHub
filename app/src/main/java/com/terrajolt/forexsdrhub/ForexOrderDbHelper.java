package com.terrajolt.forexsdrhub;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by William on 3/26/2016.
 */
public class ForexOrderDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ForexOrders.db";
    private static final String TABLE_NAME = "orderbook";
    private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" +
            ForexOrdersContract.OrderEntry._ID + " INTEGER PRIMARY KEY," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_ENTRY_ID + " INTEGER," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_EXEC_TIME + " TEXT," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_OPTION_TYPE + " TEXT," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_CURRENCY1 + " TEXT," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_CURRENCY2 + " TEXT," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_NOTIONAL1 + " INTEGER," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_NOTIONAL2 + " INTEGER," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_STRIKE_PRICE + " FLOAT," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_OPTION_CURR + " TEXT," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_PREMIUM + " FLOAT," +
            ForexOrdersContract.OrderEntry.COLUMN_NAME_EXPIRATION + " TEXT" + ")";

    private static final String TABLE_DELETE = "DROP TABLE IF EXISTS " +
            ForexOrdersContract.OrderEntry.TABLE_NAME;

    ForexOrderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(TABLE_DELETE);
    }
}
