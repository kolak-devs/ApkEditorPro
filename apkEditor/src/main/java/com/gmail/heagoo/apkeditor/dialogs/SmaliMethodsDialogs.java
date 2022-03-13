package com.gmail.heagoo.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gmail.heagoo.apkeditor.SmaliMethodAdapter;
import com.gmail.heagoo.apkeditor.SmaliMethodInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.svolf.melissa.sheet.ViewDialog;

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

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500);
        LinearLayout ll = new LinearLayout(activity);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(layoutParams);

        ListView methodLv = new ListView(activity);
        methodLv.setNestedScrollingEnabled(true);
        ll.addView(methodLv);

        SmaliMethodAdapter adapter = new SmaliMethodAdapter(
                activity.getApplicationContext(), methodList);
        methodLv.setAdapter(adapter);

        ViewDialog dialog = new ViewDialog(activity);
        dialog.setTitle("Methods");
        dialog.setView(ll);
        dialog.setPositive("Ok", null);
        dialog.show();

        methodLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                if (position < methodList.size()) {
                    SmaliMethodInfo info = methodList.get(position);
                    if (callbackRef.get() != null) {
                        callbackRef.get().gotoLine(info.lineIndex + 1);
                    }
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }
        });
    }

    public void asyncShowPopup(Activity activity, String filePath, String text) {
        new MethodAsyncLoader(activity, filePath, text).execute();
    }

    public interface ISmaliMethodClicked {
        // lineNO starts at 1
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

        MethodAsyncLoader(Activity activity, @NonNull String filePath, String fileContent) {
            activityRef = new WeakReference<>(activity);
            this.smaliFile = filePath;
            this.content = fileContent;
            this.methodList = new ArrayList<>();

            if (filePath.endsWith(".smali")) {
                fileType = FILE_TYPE_SMALI;
            } else if (filePath.endsWith(".java")) {
                fileType = FILE_TYPE_JAVA;
            }
        }

        @Nullable
        @Override
        public Boolean doInBackground(Void... params) {
            BufferedReader br = new BufferedReader(new StringReader(content));
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
            } catch (IOException ignored) {
            }
            return null;
        }

        private void parseJavaMethod(int lineIndex, String line) {
            line = line.trim();
            // For 'public' 'private' 'protected'
            if (line.length() > 6 && line.charAt(0) == 'p' && (line.charAt(1) == 'u' || line.charAt(1) == 'r')) {
                Matcher matcher = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])").matcher(line);
                //Matcher matcher = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *").matcher(line);
                if (matcher.matches()) {
                    String prototype = matcher.group(0);
                    if (prototype.endsWith("{")) {
                        prototype = prototype.substring(0, prototype.length() - 1);
                        prototype = prototype.trim();
                    }
                    methodList.add(new SmaliMethodInfo(lineIndex, prototype));
                }
            }
        }

        private void parseSmaliMethod(int lineIndex, @NonNull String line) {
            if (line.startsWith(".method ")) {
                String prototype = line.substring(8);
                methodList.add(new SmaliMethodInfo(lineIndex, prototype));
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            createPopWindow(activityRef.get(), smaliFile, methodList);
        }
    }
}
