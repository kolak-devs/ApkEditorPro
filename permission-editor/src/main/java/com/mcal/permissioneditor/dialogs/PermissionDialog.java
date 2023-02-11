package com.mcal.permissioneditor.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

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
    private final Activity activity;
    private final SearchAdapter adapter;
    private final AlertDialog dialog;
    private final ListView mv;
    private final EditText searchView;
    private final List<Permission> selectedList;

    public PermissionDialog(Activity activity, @NonNull List<Permission> list, Listener listener) {
        this.activity = activity;
        selectedList = new ArrayList<>();
        selectedList.addAll(list);

        View contentView = LayoutInflater.from(activity).inflate(R.layout.dialog_permission, null, false);

        searchView = contentView.findViewById(R.id.searchview);
        mv = contentView.findViewById(R.id.permission_listview);

        dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.add_permission)
                .setView(contentView)
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

        int i = -1;
        for (int i3 = 0; i3 < permissionList.size(); i3++) {
            for (int i4 = 0; i4 < list.size(); i4++) {
                if (permissionList.get(i3).getName().equals(list.get(i4).getName())) {
                    adapter.setSelected(permissionList.get(i3), true);
                    if (i < 0) {
                        i = i3;
                    }
                }
            }
        }

        adapter.notifyDataSetChanged();
        if (i > 0) {
            mv.setSelection(i);
        }
        updateDialogTitle();
    }

    @SuppressLint("DefaultLocale")
    public void updateDialogTitle() {
        dialog.setTitle(String.format(activity.getString(R.string.dialog_add_permission), adapter.getSelected().size()));
    }

    @SuppressLint("WrongConstant")
    public void show() {
        dialog.show();
    }

    public interface Listener {
        void onAdd(List<Permission> list);
    }
}