package com.example.nfc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "contactDb";
    public static final String TABLE_CONTACTS = "contacts";

    public static final String KEY_ID = "_id";
    public static final String KEY_PRODUCT_NAME = "name";
    public static final String KEY_PRODUCT_ID = "product_id";

    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //Вызывается при первом создании базы данных
    @Override
    public void onCreate(SQLiteDatabase db) {
        //реализация логики создания таблиц
        db.execSQL("create table " + TABLE_CONTACTS + "(" + KEY_ID + " integer primary key," + KEY_PRODUCT_NAME + " text," + KEY_PRODUCT_ID + " int" + ")");
    }

    //Вызывается при изменении базы данных
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Запрос на уничтожение таблицы, после чего создаем новую таблицу с обновленной структурой
        db.execSQL("drop table if exists " + TABLE_CONTACTS);

        onCreate(db);
    }
}
