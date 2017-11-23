package com.example.android.waitlist.data;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.waitlist.data.WaitlistContract.*;

public class WaitlistDbHelper extends SQLiteOpenHelper {

    // The database name
    private static final String DATABASE_NAME = "waitlist.db";

    // If you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 2;

    // Constructor
    public WaitlistDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        // Create a table to hold waitlist data
        final String SQL_CREATE_WAITLIST_TABLE = "CREATE TABLE " + WaitlistEntry.TABLE_NAME + " (" +
                WaitlistEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                WaitlistEntry.COLUMN_GUEST_NAME + " TEXT NOT NULL, " +
                WaitlistEntry.COLUMN_PARTY_SIZE + " INTEGER NOT NULL, " +
                WaitlistEntry.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                WaitlistEntry.COLUMN_MOBILE_NUMBER + " TEXT" +
                "); ";

        sqLiteDatabase.execSQL(SQL_CREATE_WAITLIST_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        // change onUpgrade() so the Database updates for all users
        switch (oldVersion) {
            case 1:
                String SQL_CREATE_WAITLIST_TABLE = "ALTER TABLE "
                        + WaitlistEntry.TABLE_NAME
                        + " ADD COLUMN "
                        + WaitlistEntry.COLUMN_MOBILE_NUMBER
                        + " TEXT";
                sqLiteDatabase.execSQL(SQL_CREATE_WAITLIST_TABLE);
        }
        onCreate(sqLiteDatabase);
    }
}