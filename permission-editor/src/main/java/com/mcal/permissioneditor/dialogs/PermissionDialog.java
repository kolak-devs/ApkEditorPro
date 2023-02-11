package com.mcal.permissioneditor.dialogs;

import android.annotation.SuppressLint;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.permissioneditor.R;
import com.mcal.permissioneditor.adapter.ManifestAdapter;
import com.mcal.permissioneditor.adapter.SearchAdapter;
import com.mcal.permissioneditor.model.Permission;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PermissionDialog {
    private static List<Permission> permissionList;
    private AppCompatActivity activity;
    private SearchAdapter adapter;
    private AlertDialog dialog;
    private ListView mv;
    private EditText searchView;
    private List<Permission> selectedList;

    public PermissionDialog(AppCompatActivity activity, @NonNull List<Permission> list, Listener listener) {
        this.activity = activity;
        selectedList = new ArrayList<>();
        selectedList.addAll(list);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(1);
        searchView = new EditText(activity);
        searchView.setHint(R.string.enter_search);
        linearLayout.setPadding(ManifestAdapter.dp2px((float) 16), ManifestAdapter.dp2px((float) 13), ManifestAdapter.dp2px((float) 16), ManifestAdapter.dp2px((float) 13));
        linearLayout.addView(searchView, -1, -2);
        mv = new ListView(activity);
        mv.setFastScrollEnabled(true);
        linearLayout.addView(mv, -1, -1);
        dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.add_permission)
                .setView(linearLayout)
                .setPositiveButton(R.string.add, (dialogInterface, i2) -> listener.onAdd(selectedList))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        if (permissionList == null) {
            try {
                permissionList = new ArrayList<>();
                try {
                    Field[] declaredFields = Class.forName("android.Manifest$permission").getDeclaredFields();
                    for (Field field : declaredFields) {
                        try {
                            field.setAccessible(true);
                            Object obj = field.get(null);
                            if (obj != null && (obj instanceof String)) {
                                permissionList.add(new Permission(activity, activity.getPackageManager(), (String) obj));
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                    Collections.sort(permissionList);
                } catch (ClassNotFoundException e) {
                    throw new NoClassDefFoundError(e.getMessage());
                }
            } catch (Throwable th2) {
                th2.printStackTrace();
            }
        }
        adapter = new SearchAdapter(activity, permissionList);
        adapter.bind(searchView);
        adapter.setListener(new ManifestAdapter.Listener() {
            @Override
            public void onDataSetChanged() {
            }

            @Override
            public void onSelection(Permission permission, boolean z) {
                updateDialogTitle();
            }
        });
        adapter.setSelectMode(true);
        mv.setAdapter(adapter);
        mv.setOnItemClickListener((adapterView, view, i2, j) -> {
            Permission item = adapter.getItem(i2);
            boolean invertSelection = ((ManifestAdapter.ItemView) view).holder.invertSelection();
            adapter.setSelected(item, invertSelection);
            if (invertSelection) {
                selectedList.add(item);
            } else {
                ManifestAdapter.remove(selectedList, item);
            }
            adapter.notifyDataSetChanged();
        });
        mv.setOnItemLongClickListener((adapterView, view, i2, j) -> {
            Permission item = adapter.getItem(i2);
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(item.getLabel())
                    .setMessage(item.getDescribe())
                    .setPositiveButton(adapter.isSelected(item.getName()) ? R.string.unselect : R.string.select, (dialogInterface, i3) -> {
                        Permission item2 = adapter.getItem(i3);
                        boolean invertSelection = ((ManifestAdapter.ItemView) view).holder.invertSelection();
                        adapter.setSelected(item2, invertSelection);
                        if (invertSelection) {
                            selectedList.add(item2);
                        } else {
                            ManifestAdapter.remove(selectedList, item2);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });
        int i2 = -1;
        for (int i3 = 0; i3 < permissionList.size(); i3++) {
            for (int i4 = 0; i4 < list.size(); i4++) {
                if (permissionList.get(i3).getName().equals(list.get(i4).getName())) {
                    adapter.setSelected(permissionList.get(i3), true);
                    if (i2 < 0) {
                        i2 = i3;
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
        if (i2 > 0) {
            mv.setSelection(i2);
        }
        updateDialogTitle();
    }

    @SuppressLint("DefaultLocale")
    public void updateDialogTitle() {
        dialog.setTitle(String.format("Add permission (%d selected items)", adapter.getSelected().size()));
    }

    @SuppressLint("WrongConstant")
    public void show() {
        dialog.show();
    }

    public interface Listener {
        void onAdd(List<Permission> list);
    }
}