package com.mcal.neweditor.editor2.smali;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.neweditor.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Popup window helper
public class SmaliMethodsDialogs {
    private final WeakReference<ISmaliMethodClicked> callbackRef;

    private String methodComputedFrom; // Record the method is from which file

    public SmaliMethodsDialogs(ISmaliMethodClicked callback) {
        callbackRef = new WeakReference<>(callback);
    }

    public String getFile() {
        return methodComputedFrom;
    }

    private void createPopWindow(@NonNull Activity activity, String smaliFile,
                                 final List<SmaliMethodInfo> methodList) {
        this.methodComputedFrom = smaliFile;

        final View layout = LayoutInflater.from(activity).inflate(R.layout.dialog_methods_list, null);
        final ListView methodLv = layout.findViewById(R.id.methods);

        final SmaliMethodAdapter adapter = new SmaliMethodAdapter(activity.getApplicationContext(), methodList);
        methodLv.setAdapter(adapter);

        final AlertDialog materialDialog = new MaterialAlertDialogBuilder(activity)
                .setView(layout)
                .create();
        materialDialog.show();

        methodLv.setOnItemClickListener((adapterView, view, position, id) -> {
            if (position < methodList.size()) {
                SmaliMethodInfo info = methodList.get(position);
                if (callbackRef.get() != null) {
                    callbackRef.get().gotoLine(info.lineIndex);
                }
                materialDialog.dismiss();
            }
        });
    }

    public void asyncShowPopup(Activity activity, String filePath, String text) {
        new MethodAsyncLoader(activity, filePath, text).execute();
    }

    public interface ISmaliMethodClicked {
        void gotoLine(int lineNO);
    }

    // Use to async load all the methods in smali file
    @SuppressLint("StaticFieldLeak")
    private class MethodAsyncLoader extends AsyncTask<Void, Integer, Boolean> {
        private final int FILE_TYPE_SMALI = 0;
        private final int FILE_TYPE_JAVA = 1;
        private final WeakReference<Activity> activityRef;
        private final String smaliFile;
        private final String content;
        private final List<SmaliMethodInfo> methodList;
        private int fileType = -1;

        @SuppressWarnings("deprecation")
        MethodAsyncLoader(Activity activity, @NonNull String filePath, String fileContent) {
            activityRef = new WeakReference<>(activity);
            smaliFile = filePath;
            content = fileContent;
            methodList = new ArrayList<>();

            if (filePath.endsWith(".smali")) {
                fileType = FILE_TYPE_SMALI;
            } else if (filePath.endsWith(".java")) {
                fileType = FILE_TYPE_JAVA;
            }
        }

        @Nullable
        @Override
        public Boolean doInBackground(Void... params) {
            final BufferedReader br = new BufferedReader(new StringReader(content));
            String line;
            try {
                int lineIndex = 0;
                switch (fileType) {
                    case FILE_TYPE_SMALI:
                        while ((line = br.readLine()) != null) {
                            parseSmaliMethod(lineIndex, line);
                            lineIndex += 1;
                        }
                        break;
                    case FILE_TYPE_JAVA:
                        while ((line = br.readLine()) != null) {
                            parseJavaMethod(lineIndex, line);
                            lineIndex += 1;
                        }
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void parseJavaMethod(int lineIndex, @NonNull String line) {
            final String mLine = line.trim();
            // For 'public' 'private' 'protected'
            if (mLine.length() > 6 && mLine.charAt(0) == 'p' && (mLine.charAt(1) == 'u' || mLine.charAt(1) == 'r')) {
                final Matcher matcher = Pattern.compile("(public|protected|private|static|\\s) +[\\w<>\\[\\]]+\\s+(\\w+) *\\([^)]*\\) *(\\{?|[^;])").matcher(mLine);
                //Matcher matcher = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *").matcher(line);
                if (matcher.matches()) {
                    String prototype = matcher.group(0);
                    if (prototype != null && prototype.endsWith("{")) {
                        prototype = prototype.substring(0, prototype.length() - 1);
                        prototype = prototype.trim();
                        methodList.add(new SmaliMethodInfo(lineIndex, prototype));
                    }
                }
            }
        }

        private void parseSmaliMethod(int lineIndex, @NonNull String line) {
            if (line.startsWith(".method ")) {
                final String prototype = line.substring(8);
                methodList.add(new SmaliMethodInfo(lineIndex, prototype));
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            createPopWindow(activityRef.get(), smaliFile, methodList);
        }
    }
}