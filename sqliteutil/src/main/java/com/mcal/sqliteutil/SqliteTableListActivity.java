package com.mcal.sqliteutil;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.utils.FileHelperKt;
import com.mcal.common.utils.ScopedStorage;
import com.mcal.common.utils.ActivityHelper;
import com.mcal.common.utils.RootCommand;

import java.io.File;
import java.util.ArrayList;

/**
 * Show all the table name in the DB
 *
 * @author phe3
 */
public class SqliteTableListActivity extends CustomizedLangActivity {

    private String originDbFilePath;
    private String dbFilePath;
    private ArrayList<String> tableList;

    private boolean isRootMode;

    private int textColor = 0xff333333;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.textColor = 0xffcccccc;
        setContentView(R.layout.sql_activity_tablelist);

        Intent intent = getIntent();
        this.originDbFilePath = ActivityHelper
                .getParam(intent, "dbFilePath");
        String strRootMode = ActivityHelper.getParam(intent, "isRootMode");
        // This is the default value
        isRootMode = !"false".equalsIgnoreCase(strRootMode);

        try {
            prepareAccessibleFile();
            initData();
            initView();
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    private void prepareAccessibleFile() throws Exception {
        // For non-root mode, just directly use the origin file
        if (!isRootMode) {
            this.dbFilePath = originDbFilePath;
            return;
        }

        // Following code is for root mode
        if (!FileHelperKt.exist()) {
            throw new Exception("Can not find SD Card!");
        }
        String sdDir = ScopedStorage.getStorageDirectory().getPath();
        if (!sdDir.endsWith("/")) {
            sdDir += "/";
        }

        File f = new File(getFilesDir(), "work.db");
        this.dbFilePath = f.getPath();

        RootCommand rc = new RootCommand();
        String strCmd = "cp";
        File bin = new File(getFilesDir(), "mycp");
        if (bin.exists()) {
            strCmd = bin.getPath();
        }
        boolean copyRet = rc.runRootCommand(String.format(strCmd + " \"%s\" %s", originDbFilePath, dbFilePath), null, 2000);

        // Copy file failed, use the original file
        if (!copyRet) {
            dbFilePath = originDbFilePath;
        }
    }

    private void initView() {
        // Title
        TextView tv = (TextView) this.findViewById(R.id.title);
        tv.setText(getResources().getString(R.string.tables_of) + " " + getFileName(originDbFilePath));

        // List
        ListView tableLv = (ListView) this.findViewById(R.id.tableList);
        tableLv.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, tableList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position,
                        convertView, parent);
                textView.setTextColor(textColor);
                return textView;
            }
        });
        tableLv.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            String tableName = tableList.get(position);
            Intent intent = new Intent(SqliteTableListActivity.this,
                    SqliteTableViewActivity.class);
            ActivityHelper.attachParam(intent, "originDbFilePath",
                    originDbFilePath);
            ActivityHelper.attachParam(intent, "dbFilePath", dbFilePath);
            ActivityHelper.attachParam(intent, "tableName", tableName);
            startActivity(intent);
        });
    }

    @NonNull
    private String getFileName(@NonNull String filePath) {
        int pos = filePath.lastIndexOf('/');
        return filePath.substring(pos + 1);
    }

    @SuppressLint("Range")
    private void initData() {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFilePath, null,
                SQLiteDatabase.OPEN_READONLY);

        this.tableList = new ArrayList<>();
        @SuppressLint("Recycle") Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                tableList.add(c.getString(c.getColumnIndex("name")));
                c.moveToNext();
            }
        }

        db.close();
    }

}
