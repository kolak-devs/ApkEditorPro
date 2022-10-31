package com.mcal.sqliteutil.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class PaddingTable implements OnClickListener, OnTouchListener {

    private final Context mContext;
    private final TableLayout mTableView;
    // Callback function
    private final ITableRowClicked mRowClickInterface;
    private ArrayList<String> mColumnNames;
    private List<ArrayList<String>> mTableData;
    // View and its height
    private TableRow mHeaderRow;
    private LayoutParams mTableLayoutParam;
    private android.widget.TableRow.LayoutParams mRowLayoutParam;
    private TableRow[] mTableRows;
    private View[] mSeperateLines;
    private boolean bShowWholeTable;

    public PaddingTable(Context ctx, TableLayout tableView, ITableRowClicked rowClickInterface) {
        mContext = ctx;
        mTableView = tableView;
        mRowClickInterface = rowClickInterface;
    }

    public void setTableHeaderNames(ArrayList<String> columnNames) {
        mColumnNames = columnNames;
    }

    public void setTableData(List<ArrayList<String>> tableData) {
        mTableData = tableData;
    }

    // Prepare table rows
    public void prepareTable() {
        mTableLayoutParam = new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);
        mRowLayoutParam = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        mRowLayoutParam.setMargins(8, 0, 8, 0);

        // Create table rows
        int rowNum = mTableData.size();
        int colNum = mColumnNames.size();
        mTableRows = new TableRow[rowNum];
        TextView[] textTvs = new TextView[colNum];
        mSeperateLines = new View[rowNum];

        for (int i = 0; i < rowNum; i++) {
            List<String> rowData = mTableData.get(i);

            mTableRows[i] = new TableRow(mContext);
            mTableRows[i].setId(i);

            for (int j = 0; j < colNum; j++) {
                textTvs[j] = new TextView(mContext);
                textTvs[j].setText(rowData.get(j));
            }
            for (int j = 0; j < colNum; j++) {
                mTableRows[i].addView(textTvs[j], j, mRowLayoutParam);
            }

            // Seperate line
            mSeperateLines[i] = new View(mContext);
        }

    }

    public void showSearchResult(@NonNull List<ArrayList<String>> data) {
        int rowNum = data.size();
        int colNum = mColumnNames.size();
        for (int i = 0; i < rowNum; i++) {
            ArrayList<String> rowData = data.get(i);
            for (int col = 0; col < colNum; col++) {
                TextView tv = (TextView) mTableRows[i].getChildAt(col);
                tv.setText(rowData.get(col));
            }
            mTableRows[i].setVisibility(View.VISIBLE);
            mSeperateLines[i].setVisibility(View.VISIBLE);
        }

        // Make other rows invisible
        for (int i = rowNum; i < this.mTableData.size(); i++) {
            mTableRows[i].setVisibility(View.GONE);
            mSeperateLines[i].setVisibility(View.GONE);
        }

        bShowWholeTable = false;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void drawTable() {
        // debugTime("start");
        mTableView.removeAllViews();
        bShowWholeTable = true;

        // Add header
        mHeaderRow = new TableRow(mContext);
        for (int j = 0; j < mColumnNames.size(); j++) {
            TextView textTv = new TextView(mContext);
            textTv.setText(mColumnNames.get(j));
            mHeaderRow.addView(textTv, mRowLayoutParam);
        }

        mTableView.addView(mHeaderRow, mTableLayoutParam);

        // Add data rows
        TableRow.LayoutParams rowParam = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams lineLayout = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1);
        for (int i = 0; i < mTableData.size(); i++) {
            mTableView.addView(mTableRows[i], 2 * i + 1, rowParam);
            mTableRows[i].setOnClickListener(this);
            mTableRows[i].setOnTouchListener(this);
            mTableView.addView(mSeperateLines[i], 2 * i + 2, lineLayout);
        }
    }

    // Click on some item
    @Override
    public void onClick(@NonNull View v) {
        int index = v.getId();
        if (mRowClickInterface != null) {
            mRowClickInterface.tableRowClicked(index, bShowWholeTable);
        }
    }

    @Override
    public boolean onTouch(View v, @NonNull MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            v.setAlpha(0.15f);
        } else if (action == MotionEvent.ACTION_UP) {
            v.setAlpha(0.30f);
            v.performClick();
        } else if (((action & MotionEvent.ACTION_UP) != 0)
                || ((action & MotionEvent.ACTION_OUTSIDE) != 0)) {
            v.setAlpha(0.45f);
        }

        return true;
    }

    public interface ITableRowClicked {
        void tableRowClicked(int index, boolean bShowWholeTable);
    }
}
