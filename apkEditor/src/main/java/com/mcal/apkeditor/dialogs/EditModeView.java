package com.mcal.apkeditor.dialogs;

import android.content.Context;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.R;
import com.mcal.apksigner.ApkSigner;

import java.lang.ref.WeakReference;

public class EditModeView {
    public static final int FULL_EDIT = 0;
    public static final int SIMPLE_EDIT = 1;
    public static final int COMMON_EDIT = 2;
    public static final int DATA_EDIT = 3;
    //private final boolean isProVersion;
    public static final int XML_FILE_EDIT = 4;
    private final WeakReference<Context> context;
    private final IEditModeSelected callback;
    private final String apkPath;
    private final String packageName;

    public EditModeView(Context context, IEditModeSelected callback, String apkPath,
                        String packageName) {
        this.context = new WeakReference<>(context);
        this.callback = callback;
        this.apkPath = apkPath;
        this.packageName = packageName;
        //this.isProVersion = BuildConfig.IS_PRO;
    }

    public void showFileEditDialog(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context.get());
        dialog.setTitle("Menu");
        dialog.setItems(new String[]{context.get().getString(R.string.full_edit),
                context.get().getString(R.string.simple_edit),
                context.get().getString(R.string.common_edit),
                context.get().getString(R.string.xml_file_edit),
                context.get().getString(R.string.sign_apk)}, (p112, p2) -> {
            switch (p2) {
                case 0:
                    callback.editModeSelected(FULL_EDIT, apkPath);
                    p112.dismiss();
                    break;
                case 1:
                    callback.editModeSelected(SIMPLE_EDIT, apkPath);
                    p112.dismiss();
                    break;
                case 2:
                    callback.editModeSelected(COMMON_EDIT, apkPath);
                    p112.dismiss();
                    break;
                case 3:
                    if (BuildConfig.IS_PRO) {
                        callback.editModeSelected(XML_FILE_EDIT, apkPath);
                    }/* else {
                        Context ctx = context.get();
                        if (ctx != null) {
                            ManifestListAdapter.showPromoteDialog(context.get());
                        }
                    }*/
                    p112.dismiss();
                    break;
                case 4:
                    new ApkSigner().signApk(apkPath, apkPath.replace(".apk", "_sign.apk"));
                    p112.dismiss();
                    break;
            }
        });
        dialog.create().show();
        /*PopupMenu popupMenu = new PopupMenu(context.get(), view, Gravity.CENTER);
        popupMenu.inflate(R.menu.menu_editmode);
        popupMenu.setGravity(Gravity.CENTER);
        popupMenu.setForceShowIcon(true);
        if (packageName == null || !isProVersion) {
            popupMenu.getMenu().findItem(R.id.data_edit).setVisible(false);
        }

        popupMenu
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.full_edit) {
                            callback.editModeSelected(FULL_EDIT, apkPath);
                            return true;
                        } else if (itemId == R.id.simple_edit) {
                            callback.editModeSelected(SIMPLE_EDIT, apkPath);
                            return true;
                        } else if (itemId == R.id.common_edit) {
                            callback.editModeSelected(COMMON_EDIT, apkPath);
                            return true;
                        } else if (itemId == R.id.xml_edit) {
                            if (BuildConfig.IS_PRO) {
                                callback.editModeSelected(XML_FILE_EDIT, apkPath);
                            } else {
                                Context ctx = context.get();
                                if (ctx != null) {
                                    ManifestListAdapter.showPromoteDialog(context.get());
                                }
                            }
                            return true;
                        } else if (itemId == R.id.data_edit) {
                            callback.editModeSelected(DATA_EDIT, packageName);
                            return true;
                        }
                        return false;
                    }
                });

        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
            }
        });
        popupMenu.show();*/
    }

    public void showAppEditDialog(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context.get());
        dialog.setTitle("Menu");
        dialog.setItems(new String[]{context.get().getString(R.string.full_edit),
                context.get().getString(R.string.simple_edit),
                context.get().getString(R.string.common_edit),
                context.get().getString(R.string.xml_file_edit),
                context.get().getString(R.string.edit_data_root)}, (p112, p2) -> {
            switch (p2) {
                case 0:
                    callback.editModeSelected(FULL_EDIT, apkPath);
                    break;
                case 1:
                    callback.editModeSelected(SIMPLE_EDIT, apkPath);
                    break;
                case 2:
                    callback.editModeSelected(COMMON_EDIT, apkPath);
                    break;
                case 3:
                    if (BuildConfig.IS_PRO) {
                        callback.editModeSelected(XML_FILE_EDIT, apkPath);
                    }/* else {
                        Context ctx = context.get();
                        if (ctx != null) {
                            ManifestListAdapter.showPromoteDialog(context.get());
                        }
                    }*/
                    break;
                case 4:
                    callback.editModeSelected(DATA_EDIT, packageName);
                    break;
            }
        });
        dialog.create().show();
        /*PopupMenu popupMenu = new PopupMenu(context.get(), view, Gravity.CENTER);
        popupMenu.inflate(R.menu.menu_editmode);
        popupMenu.setGravity(Gravity.CENTER);
        popupMenu.setForceShowIcon(true);
        if (packageName == null || !isProVersion) {
            popupMenu.getMenu().findItem(R.id.data_edit).setVisible(false);
        }

        popupMenu
                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.full_edit) {
                            callback.editModeSelected(FULL_EDIT, apkPath);
                            return true;
                        } else if (itemId == R.id.simple_edit) {
                            callback.editModeSelected(SIMPLE_EDIT, apkPath);
                            return true;
                        } else if (itemId == R.id.common_edit) {
                            callback.editModeSelected(COMMON_EDIT, apkPath);
                            return true;
                        } else if (itemId == R.id.xml_edit) {
                            if (BuildConfig.IS_PRO) {
                                callback.editModeSelected(XML_FILE_EDIT, apkPath);
                            } else {
                                Context ctx = context.get();
                                if (ctx != null) {
                                    ManifestListAdapter.showPromoteDialog(context.get());
                                }
                            }
                            return true;
                        } else if (itemId == R.id.data_edit) {
                            callback.editModeSelected(DATA_EDIT, packageName);
                            return true;
                        }
                        return false;
                    }
                });

        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
            }
        });
        popupMenu.show();*/
    }

    public interface IEditModeSelected {
        void editModeSelected(int mode, String extraStr);
    }
}
