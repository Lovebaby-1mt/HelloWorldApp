package com.example.helloworldapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabaseHelper extends SQLiteOpenHelper{

    private static final String DB_NAME = "user.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_USER = "user";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";

    private static final String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USERNAME + " TEXT NOT NULL, " +
            COLUMN_PASSWORD + " TEXT NOT NULL)";

    public MyDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);

        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, "admin");
        values.put(COLUMN_PASSWORD, "123456");
        db.insert(TABLE_USER, null, values);
    }

    public boolean checkLogin(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();

        String selection = COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = new String[]{username, password};

        android.database.Cursor cursor = db.query(
                TABLE_USER,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        boolean isLoginSuccess = false;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                isLoginSuccess = true;
            }
            cursor.close();
        }

        return isLoginSuccess;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }

}
