package com.mcal.sqliteutil;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.adapters.MySimpleAdapter;
import com.mcal.common.utilsOld.ActivityUtils;
import com.mcal.common.utilsOld.RootCommand;
import com.mcal.common.utilsOld.SDCard;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Show one line record of a table
 *
 * @author phe3
 */
public class SqliteRowViewActivity extends CustomizedLangActivity implements
        OnItemClickListener, OnClickListener {

    // All versions now editable
    private final boolean editable = true;
    private String originDbFilePath;
    private String dbFilePath;
    private String tableName;
    private ArrayList<String> columnNames;
    private ArrayList<String> columnTypes;
    private ArrayList<String> columnIsPKs;
    private ArrayList<String> rowData;
    private ListView valueListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();

        setContentView(R.layout.sql_activity_rowview);

        this.originDbFilePath = ActivityUtils.getParam(intent,
                "originDbFilePath");
        this.dbFilePath = ActivityUtils.getParam(intent, "dbFilePath");
        this.tableName = ActivityUtils.getParam(intent, "tableName");
        this.columnNames = ActivityUtils.getStringArray(intent, "columnNames");
        this.columnTypes = ActivityUtils.getStringArray(intent, "columnTypes");
        this.columnIsPKs = ActivityUtils.getStringArray(intent, "columnIsPKs");
        this.rowData = ActivityUtils.getStringArray(intent, "rowData");

        initListView();
        initButton();

        // Initially not modified
        this.setResult(0);
    }

    private void initListView() {
        this.valueListView = (ListView) this.findViewById(R.id.tableRowList);

        SimpleAdapter adapter = createListAdapter();
        valueListView.setAdapter(adapter);

        valueListView.setCacheColorHint(Color.TRANSPARENT);
        valueListView.setOnItemClickListener(this);
    }

    private void initButton() {
        Button delBtn = (Button) this.findViewById(R.id.btn_delete);
        if (!this.editable) {
            delBtn.setVisibility(View.GONE);
        } else {
            delBtn.setOnClickListener(this);
        }

        Button closeBtn = (Button) this.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
        TableRecordDialog dlg = new TableRecordDialog(this, columnTypes,
                columnNames, columnIsPKs, rowData, position, this.editable);
        dlg.setTableInfo(dbFilePath, tableName);
        dlg.show();
    }

    // Copy temporary DB file to the original place
    private void copyDbFile() throws Exception {
        // DO NOT need to copy
        // For non-root mode, will reach this code
        if (dbFilePath.equals(originDbFilePath)) {
            return;
        }

        String sdDir = SDCard.getRootDirectory();
        if (!sdDir.endsWith("/")) {
            sdDir += "/";
        }
        String workingDir = sdDir + "HackAppData/tmp/";
        this.dbFilePath = workingDir + "tmp.db";

        RootCommand rc = new RootCommand();
        boolean copyRet = rc.runRootCommand(
                String.format("cat %s > %s", dbFilePath, originDbFilePath),
                null, 2000);
        // Copy file failed, use the original file
        if (!copyRet) {
            throw new Exception("Can not write to DB file.");
        }
    }

    // Save the data base
    // index - modified column index
    public void saveValue(int index, Object newValue) throws Exception {
        String colName = columnNames.get(index);
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFilePath, null,
                SQLiteDatabase.OPEN_READWRITE);
        if (db == null) {
            throw new Exception("Can not open database.");
        }

        try {
            updateDB(db, colName, newValue);
        } catch (Exception e) {
            throw e;
        } finally {
            db.close();
        }

        // Copy to origin DB file
        copyDbFile();

        // When succeed, update the data and the list view
        rowData.set(index, newValue.toString());
        SimpleAdapter adapter = createListAdapter();
        valueListView.setAdapter(adapter);

        // Also set the result to notify the previous activity
        this.setResult(1);
    }

    @NonNull
    @Contract(" -> new")
    private MySimpleAdapter createListAdapter() {
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        for (int i = 0; i < columnNames.size(); i++) {
            Map<String, String> map1 = new HashMap<String, String>();
            map1.put("NAME", columnNames.get(i));
            map1.put("VALUE", rowData.get(i));
            data.add(map1);
        }
        return new MySimpleAdapter(this, data,
                android.R.layout.simple_list_item_2, new String[]{"NAME",
                "VALUE"}, new int[]{android.R.id.text1,
                android.R.id.text2});
    }

    private void updateDB(SQLiteDatabase db, String colName, Object newValue)
            throws Exception {
        ContentValues values = new ContentValues();
        if (newValue instanceof String) {
            values.put(colName, (String) newValue);
        } else if (newValue instanceof Boolean) {
            values.put(colName, (Boolean) newValue);
        } else if (newValue instanceof Integer) {
            values.put(colName, (Integer) newValue);
        } else if (newValue instanceof Long) {
            values.put(colName, (Long) newValue);
        } else if (newValue instanceof Short) {
            values.put(colName, (Short) newValue);
        } else if (newValue instanceof Double) {
            values.put(colName, (Double) newValue);
        } else if (newValue instanceof Float) {
            values.put(colName, (Float) newValue);
        } else if (newValue instanceof Byte) {
            values.put(colName, (Byte) newValue);
        } else {
            throw new Exception("Unrecognized value type!");
        }

        List<String> valueList = new ArrayList<String>();
        String whereClause = buildCondition(valueList);

        // Log.d("DEBUG", "value: " + values.get(colName));
        // Log.d("DEBUG", "whereClause: " + whereClause);
        // Log.d("DEBUG", "valueList: " + valueList);
        int ret = db.update(tableName, values, whereClause,
                valueList.toArray(new String[valueList.size()]));
        if (ret <= 0) {
            throw new Exception("Failed or no change detected!");
        }
    }

    private String buildCondition(List<String> valueList) {
        String whereClause = "";

        for (int i = 0; i < columnIsPKs.size(); i++) {
            if ("1".equals(columnIsPKs.get(i))) {
                valueList.add(rowData.get(i));
                if ("".equals(whereClause)) {
                    whereClause = columnNames.get(i) + "=?";
                } else {
                    whereClause += " AND " + columnNames.get(i) + "=?";
                }
            }
        }

        // There is no primary key
        if ("".equals(whereClause)) {
            for (int i = 0; i < columnNames.size(); i++) {
                valueList.add(rowData.get(i));
                if ("".equals(whereClause)) {
                    whereClause = columnNames.get(i) + "=?";
                } else {
                    whereClause += " AND " + columnNames.get(i) + "=?";
                }
            }
        }

        return whereClause;
    }

    private void deleteRecord() throws Exception {

        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFilePath, null,
                SQLiteDatabase.OPEN_READWRITE);
        if (db == null) {
            throw new Exception("Can not open database.");
        }

        try {
            List<String> valueList = new ArrayList<String>();
            String whereClause = buildCondition(valueList);

            db.delete(tableName, whereClause,
                    valueList.toArray(new String[valueList.size()]));
        } catch (Exception e) {
            throw e;
        } finally {
            db.close();
        }

        // Copy to origin DB file
        copyDbFile();

        // Set the result to notify the previous activity
        this.setResult(1);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_delete) {
            showDeleteDialog();
        } else if (id == R.id.btn_close) {
            this.finish();
        }
    }

    @SuppressLint("NewApi")
    private void showDeleteDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        Dialog alertDialog = builder
                .setTitle("Sure to Delete?")
                .setMessage("Are you sure to delete the record?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("YES",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                try {
                                    deleteRecord();
                                    SqliteRowViewActivity.this.finish();
                                } catch (Exception e) {
                                    Toast.makeText(
                                            SqliteRowViewActivity.this,
                                            e.getClass().getSimpleName() + ": "
                                                    + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // dismiss();
                    }
                }).create();
        alertDialog.show();
    }
}
