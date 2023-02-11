package com.mcal.appdm.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private final DBHelper helper;
    private final SQLiteDatabase db;

    public DBManager(Context context) {
        helper = new DBHelper(context);
        db = helper.getWritableDatabase();
    }

    /**
     * add records
     *
     * @param recordList
     */
    public void add(@NonNull List<BackupInfo> recordList) {
        db.beginTransaction();
        try {
            for (BackupInfo record : recordList) {
                db.execSQL(
                        "INSERT INTO data_backups VALUES(null, ?, ?, ?, ?, ?, ?)",
                        new Object[]{record.backupName, record.backupTime,
                                record.backupPath, record.packageName,
                                record.comment, record.backupSize});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void add(@NonNull BackupInfo record) {
        db.execSQL("INSERT INTO data_backups VALUES(null, ?, ?, ?, ?, ?, ?)",
                new Object[]{record.backupName, record.backupTime,
                        record.backupPath, record.packageName, record.comment,
                        record.backupSize});
    }

    public boolean update(@NonNull BackupInfo record) {
        ContentValues cv = new ContentValues();
        cv.put("backup_name", record.backupName);
        cv.put("backup_time", record.backupTime);
        cv.put("package_name", record.packageName);
        cv.put("comment", record.comment);
        cv.put("size", record.backupSize);
        int rows = db.update("data_backups", cv, "_id = ?", new String[]{String.valueOf(record._id)});
        return rows > 0;
    }

    /**
     * delete old record
     */
    public void deleteOldRecord(@NonNull BackupInfo record) {
        db.delete("data_backups", "backup_time = ?", new String[]{record.backupTime});
    }

    /**
     * query all revised apps, return list
     */
    @SuppressLint("Range")
    public List<BackupInfo> query() {
        ArrayList<BackupInfo> recordList = new ArrayList<>();
        Cursor c = queryTheCursor();
        while (c.moveToNext()) {
            BackupInfo record = new BackupInfo();
            record._id = c.getInt(c.getColumnIndex("_id"));
            record.backupName = c.getString(c.getColumnIndex("backup_name"));
            record.backupTime = c.getString(c.getColumnIndex("backup_time"));
            record.backupPath = c.getString(c.getColumnIndex("backup_path"));
            record.packageName = c.getString(c.getColumnIndex("package_name"));
            record.comment = c.getString(c.getColumnIndex("comment"));
            record.backupSize = c.getLong(c.getColumnIndex("size"));
            recordList.add(record);
        }
        c.close();
        return recordList;
    }

    /**
     * query all records, return cursor
     *
     * @return Cursor
     */
    public Cursor queryTheCursor() {
        return db.rawQuery("SELECT * FROM data_backups ORDER BY backup_time DESC", null);
    }

    /**
     * close database
     */
    public void closeDB() {
        db.close();
    }
}
