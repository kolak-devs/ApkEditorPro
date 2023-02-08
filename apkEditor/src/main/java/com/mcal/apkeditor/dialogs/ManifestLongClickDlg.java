package com.mcal.apkeditor.dialogs;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.common.utils.OpenFiles;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.activities.ApkInfoActivity;
import com.mcal.apkeditor.adapters.IManifestChangeCallback;
import com.mcal.apkeditor.adapters.LineRecord;
import com.mcal.apkeditor.dialogs.FileSelectDialog.IFileSelection;
import com.mcal.editor.TextEditor;
import com.mcal.patchview.ui.CodeText;

import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.Map;

class ManifestDesc {
    private static final Map<String, String> tag2Desc = new HashMap<>();

    static {
        tag2Desc.put("action", "Adds an action to an intent filter.");
        tag2Desc.put(
                "activity",
                "Declares an activity that implements part of the application's visual user interface.");
        tag2Desc.put("activity-alias", "An alias for an activity.");
        tag2Desc.put(
                "application",
                "The declaration of the application. This element contains subelements that declare each of the application's components and has attributes that can affect all the components.");
        tag2Desc.put("category", "Adds a category name to an intent filter.");
        tag2Desc.put("compatible-screens",
                "Specifies each screen configuration with which the application is compatible.");
        tag2Desc.put("data", "Adds a data specification to an intent filter.");
        tag2Desc.put(
                "grant-uri-permission",
                "Specifies which data subsets of the parent content provider permission can be granted for.");
        tag2Desc.put(
                "instrumentation",
                "Declares an Instrumentation class that enables you to monitor an application's interaction with the system.");
        tag2Desc.put(
                "intent-filter",
                "Specifies the types of intents that an activity, service, or broadcast receiver can respond to. An intent filter declares the capabilities of its parent component -- what an activity or service can do and what types of broadcasts a receiver can handle.");
        tag2Desc.put("manifest",
                "The root element of the AndroidManifest.xml file.");
        tag2Desc.put(
                "meta-data",
                "A name-value pair for an item of additional, arbitrary data that can be supplied to the parent component.");
        tag2Desc.put(
                "path-permission",
                "Defines the path and required permissions for a specific subset of data within a content provider.");
        tag2Desc.put(
                "permission",
                "Declares a security permission that can be used to limit access to specific components or features of this or other applications.");
        tag2Desc.put("permission-group",
                "Declares a name for a logical grouping of related permissions.");
        tag2Desc.put("permission-tree",
                "Declares the base name for a tree of permissions.");
        tag2Desc.put(
                "provider",
                "Declares a content provider component. A content provider supplies structured access to data managed by the application.");
        tag2Desc.put(
                "receiver",
                "Declares a broadcast receiver as one of the application's components. Broadcast receivers enable applications to receive intents that are broadcast by the system or by other applications, even when other components of the application are not running.");
        tag2Desc.put(
                "service",
                "Declares a service as one of the application's components. Unlike activities, services lack a visual user interface. They're used to implement long-running background operations");
        tag2Desc.put(
                "supports-gl-texture",
                "Declares a single GL texture compression format that is supported by the application.");
        tag2Desc.put(
                "supports-screens",
                "Lets you specify the screen sizes your application supports and enable screen compatibility mode for screens larger than what your application supports.");
        tag2Desc.put("uses-configuration",
                "Indicates what hardware and software features the application requires.");
        tag2Desc.put(
                "uses-feature",
                "Declares a single hardware or software feature that is used by the application.");
        tag2Desc.put("uses-library",
                "Specifies a shared library that the application must be linked against.");
        tag2Desc.put(
                "uses-permission",
                "Requests a permission that the application must be granted in order for it to operate correctly.");
        tag2Desc.put(
                "uses-sdk",
                "Lets you express an application's compatibility with one or more versions of the Android platform, by means of an API Level integer.");
    }

    static String getDescription(String tag) {
        return tag2Desc.get(tag);
    }
}

public class ManifestLongClickDlg {
    private final LineRecord lineRec;

    public ManifestLongClickDlg(@NonNull Activity activity, String xmlPath,
                                @NonNull LineRecord lineRec, IManifestChangeCallback callback) {
        this.lineRec = lineRec;

        View view = activity.getLayoutInflater().inflate(
                R.layout.dialog_manifestline, null, false);
        CodeText contentTv = view.findViewById(R.id.content);
        contentTv.setText(lineRec.lineData);
        TextView descTv = view.findViewById(R.id.description);
        String desc = getDescription();
        descTv.setText(desc != null ? desc : "");

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(activity);
        dialog.setView(view);
        dialog.setItems(new String[]{activity.getString(R.string.delete_this_line),
                activity.getString(R.string.extract),
                activity.getString(R.string.replace),
                activity.getString(R.string.open_in_new_window)}, (p112, p2) -> {
            switch (p2) {
                case 0:
                    String errMsg = callback.tryToDeleteSection(lineRec);
                    if (errMsg == null) {
                        p112.dismiss();
                    } else {
                        // Prompt the reason of the fail
                        if (!errMsg.equals("")) {
                            Toast.makeText(activity, errMsg, Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case 1:
                    // Select a target folder to extract
                    String dlgTitle = activity.getString(R.string.select_folder);
                    IFileSelection deleteCallback = new IFileSelection() {
                        @Override
                        // filePath is the target directory
                        // extraStr is the source file/directory
                        public void fileSelectedInDialog(
                                String filePath, String extraStr, boolean openFile) {
                            new FileCopyDialog(activity, xmlPath, filePath, null, null, null, 0);
                        }

                        @Override
                        public boolean isInterestedFile(String filename, String extraStr) {
                            return true;
                        }

                        @Nullable
                        @Contract(pure = true)
                        @Override
                        public String getConfirmMessage(String filePath, String extraStr) {
                            return null;
                        }
                    };

                    new FileSelectDialog(activity, deleteCallback, null, null,
                            dlgTitle, true, false, false, null);

                    p112.dismiss();
                    break;
                case 2:
                    new FileSelectDialog(
                            activity,
                            new IFileSelection() {
                                @Override
                                public void fileSelectedInDialog(
                                        String filePath, String extraStr, boolean openFile) {
                                    if (openFile) {
                                        ((ApkInfoActivity) activity).saveParams(
                                                filePath, extraStr, null);
                                        OpenFiles.openFile(activity, filePath,
                                                ApkInfoActivity.RC_OPEN_BEFORE_REPLACE);
                                    } else {
                                        ((ApkInfoActivity) activity).replaceFile(extraStr, filePath);
                                        ((ApkInfoActivity) activity).setManifestModified(true);
                                    }
                                }

                                @Override
                                public boolean isInterestedFile(
                                        String filename, String extraStr) {
                                    return filename.endsWith(".xml");
                                }

                                @Override
                                public String getConfirmMessage(String filePath, String extraStr) {
                                    return null;
                                }
                            }, ".xml", xmlPath, null, false, false, true, null);
                    p112.dismiss();
                    break;
                case 3:
                    Intent intent = TextEditor.getSoraEditor(activity.getApplicationContext(), xmlPath, null, 0, null);
                    activity.startActivityForResult(intent, 2);
                    p112.dismiss();
                    break;
            }
        });
        dialog.show();
    }

    // Tag like activity, manifest, uses-permission, etc

    // Get the string specified by android:name
    @NonNull
    private String getSearchKeywords() {
        String name = lineRec.getName();
        if (name == null || name.startsWith(".")) {
            name = "AndroidManifest " + lineRec.getSectionTag();
        }

        return name;
    }

    // Get description of section tag
    @Nullable
    private String getDescription() {
        String tag = lineRec.getSectionTag();
        if (tag != null) {
            return ManifestDesc.getDescription(tag);
        } else {
            return null;
        }
    }
}
