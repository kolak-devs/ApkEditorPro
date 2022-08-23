package com.mcal.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.ce.IApkMaking;
import com.mcal.apkeditor.ce.IDescriptionUpdate;
import com.mcal.apkeditor.pro.DexEncoder;
import com.mcal.apkeditor.smali.ISmaliAssembleCallback;
import com.mcal.apkeditor.utils.AssetsInstaller;
import com.mcal.apkeditor.utils.FileUtils;
import com.mcal.apksigner.ApkSigner;
import com.mcal.common.data.Preferences;
import com.mcal.common.fastzip.FastZip;
import com.mcal.common.utilsOld.CommandRunner;
import com.mcal.common.utilsOld.ITaskCallback;
import com.mcal.common.utilsOld.ITaskCallback.TaskStepInfo;
import com.mcal.common.utilsOld.LOGGER;
import com.mcal.common.utilsOld.SDCard;
import com.mcal.common.utils.ScopedStorage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ApkComposeThread extends ComposeThread implements ISmaliAssembleCallback {
    public static String sysArch;
    private final Context ctx;
    private final String binRootPath;
    private final String aaptPath;
    private final String aaptPath2;
    private final String androidJarPath;
    private final String decodedFilePath;
    private final String srcApkPath;
    private final String targetApkPath; // Target APK path
    // Record all the dex file replaces
    private final Map<String, String> dexReplaces = new HashMap<>();
    private final TaskStepInfo stepInfo;
    // Last time updating the smali assemble info
    private final long lastUpdateAssembleTime = 0;
    // merge result
    private String tempApkPath; // if res modified, store the intermediate
    // Indicate succeed or not
    private boolean succeed;
    private String errMessage;
    // String resource modified or not
    private boolean stringModified;
    // Manifest modified or not
    private boolean manifestModified;
    // resource file added/deleted
    private boolean resFileModified;
    // Samli file modified or not
    private List<String> modifiedSmaliFolders;
    // Add/Delete/Modify files
    private Map<String, String> addedFiles;
    private Map<String, String> replacedFiles;
    private Set<String> deletedFiles;
    private Map<String, String> fileEntry2ZipEntry;
    private ITaskCallback taskCallback;
    // Flag to control run or not
    private boolean stopFlag = false;
    private boolean bSignApk;
    private IApkMaking extraMaker;


    /**
     * @param ctx             Context
     * @param decodedFilePath path store all the decoded files
     * @param srcApkPath      where the source file is from
     * @param apkPath         target apk path
     */
    public ApkComposeThread(@NonNull Context ctx, String decodedFilePath,
                            String srcApkPath, String apkPath) {
        this.ctx = ctx;

        File fileDir = ctx.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        this.binRootPath = rootDirectory + "/bin";
        this.aaptPath = binRootPath + "/aapt";
        this.aaptPath2 = binRootPath + "/aapt2";
        this.androidJarPath = binRootPath + "/android-framework.jar";
        // this.androidJarPath = SDCard.getRootDirectory() + "/android-framework.jar";
        this.decodedFilePath = decodedFilePath;
        this.srcApkPath = srcApkPath;
        this.targetApkPath = apkPath;
        this.tempApkPath = srcApkPath;

        LOGGER.info("aaptPath: " + this.aaptPath);
        LOGGER.info("androidJarPath: " + this.androidJarPath);
        LOGGER.info("decodedFilePath: " + this.decodedFilePath);

        this.stepInfo = new ITaskCallback.TaskStepInfo();
    }

    // This method will extract the necessary files
    public static boolean prepare(@NonNull Context ctx) throws Exception {
        String curVersion = null;
        try {
            PackageInfo pInfo = ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            curVersion = pInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        // Prepare file
        SharedPreferences sp = ctx.getSharedPreferences("info", 0);
        boolean inited = sp.getBoolean("initialized", false);
        String lastVersion = sp.getString("version", "");
        if (!inited || !lastVersion.equals(curVersion)) {
            if (copyFiles(ctx)) {
                Editor editor = sp.edit();
                editor.putBoolean("initialized", true);
                editor.putString("version", curVersion);
                editor.apply();
                return true;
            }

            return false;
        }
        // Already inited before
        else {
            return true;
        }
    }

    // Copy aapt & android-framework.jar
    private static boolean copyFiles(Context ctx) throws Exception {
        try {
            try {
                new AssetsInstaller(ctx).install();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Can not copy file: " + e.getMessage());
        }
    }

    // resReplaces contains non-xml replaces
    // resFileModified means res added/deleted, or xml changed
    @Override
    public void setModification(boolean strModified, boolean manifestModified,
                                boolean resFileModified, List<String> modifiedSmaliFolders,
                                Map<String, String> addedFiles, Map<String, String> replacedFiles,
                                Set<String> deletedFiles, Map<String, String> fileEntry2ZipEntry,
                                boolean bSignApk) {

        this.stringModified = strModified;
        this.manifestModified = manifestModified;
        this.resFileModified = resFileModified;
        this.modifiedSmaliFolders = modifiedSmaliFolders;
        this.addedFiles = addedFiles;
        this.deletedFiles = deletedFiles;
        this.replacedFiles = replacedFiles;
        this.fileEntry2ZipEntry = fileEntry2ZipEntry;
        this.bSignApk = bSignApk;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        boolean rebuildResNeeded = isResourceModified();
        // Sign & Cleanup
        if (BuildConfig.WITH_SIGN) {
            this.stepInfo.stepTotal = (bSignApk ? 2 : 1);
        } else {
            this.stepInfo.stepTotal = 1;
        }
        // Need to compile the resource
        if (rebuildResNeeded)
            stepInfo.stepTotal += 2;
        // For DEX assembling
        if (this.modifiedSmaliFolders != null
                && !this.modifiedSmaliFolders.isEmpty())
            stepInfo.stepTotal += this.modifiedSmaliFolders.size();

        do {
            // XML/String/Manifest modified, requires re-compiling
            if (rebuildResNeeded) {
                if (stopFlag) {
                    this.errMessage = "User request to stop";
                    break;
                }
                setNextStep(ctx.getString(R.string.compose));

                if (!prepare()) {
                    break;
                }

                // Compose resource and extract files
                if (stopFlag) {
                    this.errMessage = "User request to stop";
                    break;
                }
                if (!composeResource()) {
                    break;
                }
            }
            // Assemble DEX files
            if (this.modifiedSmaliFolders != null
                    && !this.modifiedSmaliFolders.isEmpty()) {
                boolean assembleError = false;
                for (String smaliFolder : modifiedSmaliFolders) {
                    String smaliPath = decodedFilePath + "/" + smaliFolder;
                    String dexName = getDexNameBySmaliFolder(smaliFolder);
                    setNextStep(ctx.getString(R.string.assemble_dex_file) + ": " + dexName);
                    try {
                        // Log.d("DEBUG", "Assemble " + smaliPath + " to " +
                        // dexName);
                        assembleSmali(smaliPath, dexName);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        this.errMessage = e.getMessage();
                        assembleError = true;
                        break;
                    }

                    if (stopFlag) {
                        this.errMessage = "User request to stop";
                        assembleError = true;
                        break;
                    }
                }

                if (assembleError)
                    break;
            }

            if (rebuildResNeeded) {
                if (stopFlag) {
                    this.errMessage = "User request to stop";
                    break;
                }
                setNextStep(ctx.getString(R.string.merge));

                try {
                    // if (isProAndNoModification()) {
                    long start = System.currentTimeMillis();
                    mergeApk();
                    //Log.e("DEBUG", "Merge Time: " + (System.currentTimeMillis() - start));
                    // } else {
                    // mergeApkFreeVersion();
                    // }
                    // Must applied, otherwise may replace it again when sign
                    removeResChanges();
                } catch (Exception e) {
                    e.printStackTrace();
                    this.errMessage = ctx.getString(R.string.merge) + ": " + e.getMessage();
                    break;
                }
            }
            // Do not need to rebuild resource, thus need to translate the
            // modification
            else {
                translate2OriginEntry();
            }

            // Sign or not
            if (BuildConfig.WITH_SIGN && bSignApk) {
                if (stopFlag) {
                    this.errMessage = "User request to stop";
                    break;
                }
                setNextStep(ctx.getString(R.string.sign));
                if (!signApk()) {
                    break;
                }
            }/* else {
                notSignApk();
            }*/

            // Clean up
            if (stopFlag) {
                this.errMessage = "User request to stop";
                break;
            }
            setNextStep(ctx.getString(R.string.cleanup));
            cleanup();

            // For free version, make sure it longer enough, so that ad could be loaded
            if (!BuildConfig.IS_PRO) {
                long curTime = System.currentTimeMillis();
                if (curTime - startTime < 7500) {
                    try {
                        Thread.sleep(7500 - (curTime - startTime));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            this.succeed = true;
        } while (false);

        if (!stopFlag) {
            if (succeed) {
                taskCallback.taskSucceed();
            } else {
                taskCallback.taskFailed(errMessage);
            }
        }
    }

    // When add/delete/modify some general images, rebuild is not need
    // But in sign step, we need to do the modification
    // Here we must translate it back to the original entry name
    private void translate2OriginEntry() {
        // Do not need to translate
        if (fileEntry2ZipEntry == null || fileEntry2ZipEntry.isEmpty()) {
            return;
        }

        // Translate add records
        if (!this.addedFiles.isEmpty()) {
            Map<String, String> newAdded = new HashMap<>();
            for (Map.Entry<String, String> entry : this.addedFiles.entrySet()) {
                String newKey = fileEntry2ZipEntry.get(entry.getKey());
                if (newKey != null) {
                    newAdded.put(newKey, entry.getValue());
                } else {
                    newAdded.put(entry.getKey(), entry.getValue());
                }
            }
            this.addedFiles = newAdded;
        }

        // Translate replace records
        if (!this.replacedFiles.isEmpty()) {
            Map<String, String> newReplace = new HashMap<>();
            for (Map.Entry<String, String> entry : this.replacedFiles
                    .entrySet()) {
                String newKey = fileEntry2ZipEntry.get(entry.getKey());
                if (newKey != null) {
                    newReplace.put(newKey, entry.getValue());
                } else {
                    newReplace.put(entry.getKey(), entry.getValue());
                }
            }
            this.replacedFiles = newReplace;
        }

        // Translate delete records
        if (!this.deletedFiles.isEmpty()) {
            Set<String> newDelete = new HashSet<String>();
            for (String entry : deletedFiles) {
                String newEntry = fileEntry2ZipEntry.get(entry);
                if (newEntry != null) {
                    newDelete.add(newEntry);
                } else {
                    newDelete.add(entry);
                }
            }
            this.deletedFiles = newDelete;
        }
    }

    // Delete resource changes, as the resource change is already applied
    private void removeResChanges() {
        removeResInMap(this.addedFiles);
        removeResInMap(this.replacedFiles);

        this.deletedFiles.removeIf(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.startsWith("res/");
            }
        });
    }

    private void removeResInMap(@NonNull Map<String, String> data) {
        data.entrySet().removeIf(new Predicate<Map.Entry<String, String>>() {
            @Override
            public boolean test(Map.Entry<String, String> entry) {
                return entry.getKey().startsWith("res/");
            }
        });
    }

    @NonNull
    private String getDexNameBySmaliFolder(String smaliFolder) {
        if ("smali".equals(smaliFolder)) {
            return "classes.dex";
        }
        if (smaliFolder.startsWith("smali_")) {
            return smaliFolder.substring("smali_".length()) + ".dex";
        }
        return smaliFolder + ".dex";
    }

    private void setNextStep(String description) {
        stepInfo.stepIndex += 1;
        stepInfo.stepDescription = description;
        taskCallback.setTaskStepInfo(stepInfo);
    }

    // Assemble smali to DEX
    private void assembleSmali(String smaliFilePath, String dexFileName)
            throws Throwable {
        String dexFilePath = getPathInSameDirectory(this.targetApkPath,
                dexFileName);

        // Log.d("DEBUG", "assemble " + smaliFilePath + " to " + dexFilePath +
        // ", dexFileName=" + dexFileName);

        // Invoke DexEncoder.smali2Dex
        try {
            long start = System.currentTimeMillis();
            /*Class<?> obj_class = Class
                    .forName("com.mcal.apkeditor.pro.DexEncoder");
            Method method = obj_class.getMethod("smali2Dex", String.class, String.class, ISmaliAssembleCallback.class);
            method.invoke(null, smaliFilePath, dexFilePath, this);*/

            DexEncoder.smali2Dex(smaliFilePath, dexFilePath, this);
            Log.i("DEBUG", "Encode time=" + (System.currentTimeMillis() - start));

            // Record the replace
            this.dexReplaces.put(dexFileName, dexFilePath);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    @NonNull
    private String getPathInSameDirectory(@NonNull String path, String name) {
        int position = path.lastIndexOf('/');
        return path.substring(0, position + 1) + name;
    }

    private void cleanup() {
        // Delete the intermediate file
        if (!tempApkPath.equals(srcApkPath)) {
            File f = new File(tempApkPath);
            f.delete();
        }

        // Delete all the decoded files
        // Do not delete the res directory any more, as the project must keep it
        CommandRunner cr = new CommandRunner();
//        cr.runCommand("rm -rf " + decodedFilePath + "/res", null, 10000);

        // Clean /sdcard/ApkEditor/tmp
        try {
            String tmpDir = SDCard.getRootDirectory() + "/ApkEditor/tmp";
            cr.runCommand("rm -rf " + tmpDir, null, 10000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Log.d("DEBUG", "decodedFilePath" + decodedFilePath);
    }

    protected void mergeApk() {
        // Before merge, call the extra maker
        Map<String, String> extraReplaces = new HashMap<>();
        if (this.extraMaker != null) {
            try {
                extraMaker.prepareReplaces(ctx, tempApkPath, extraReplaces,
                        // Note: currently not support description update
                        new IDescriptionUpdate() {
                            @Override
                            public void updateDescription(String strDesc) {
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int mappingLen = 0;
        StringBuilder sb1 = new StringBuilder();
        for (Map.Entry<String, String> entry : fileEntry2ZipEntry.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb1.append(key);
            sb1.append('\n');
            sb1.append(value);
            sb1.append('\n');
            mappingLen += key.getBytes().length + value.getBytes().length + 2;
        }

        int replaceLen = 0;
        StringBuilder sb2 = new StringBuilder();
        for (Map.Entry<String, String> entry : addedFiles.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // For xml files, not replace it, as already compiled it into AXML
            if (key.startsWith("res/") && key.endsWith(".xml")) {
                continue;
            }
            sb2.append(key);
            sb2.append('\n');
            sb2.append(value);
            sb2.append('\n');
            replaceLen += key.getBytes().length + value.getBytes().length + 2;
        }
        for (Map.Entry<String, String> entry : replacedFiles.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith("res/")) {
                // For xml files, not replace it, as already compiled it into AXML
                if (key.endsWith(".xml")) {
                    continue;
                }
                // For 9.png files, not replace it, as already it is already compiled
                if (key.endsWith(".9.png")) {
                    continue;
                }
            }

            sb2.append(key);
            sb2.append('\n');
            sb2.append(value);
            sb2.append('\n');
            replaceLen += key.getBytes().length + value.getBytes().length + 2;
        }
        for (Map.Entry<String, String> entry : extraReplaces.entrySet()) {
            // The extra one must be replaced
//            String key = entry.getKey();
//            if (key.startsWith("res/") && key.endsWith(".xml")) {
//                continue;
//            }
            String key = entry.getKey();
            String value = entry.getValue();
            sb2.append(entry.getKey());
            sb2.append('\n');
            sb2.append(entry.getValue());
            sb2.append('\n');
            replaceLen += key.getBytes().length + value.getBytes().length + 2;
        }

       /* MainActivity.mg(tempApkPath, srcApkPath,
                sb2.toString(), replaceLen,
                sb1.toString(), mappingLen
        );*/
        try {
            FastZip.repack(srcApkPath, ScopedStorage.getApkEditorDirectory() + File.separator + "gen_unsigned.apk", replacedFiles, addedFiles, deletedFiles);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isResourceModified() {
        return (this.stringModified || this.manifestModified
                || this.resFileModified);
    }

    private boolean signApk() {

        // When smali code is edited, also replace classes.dex
        replacedFiles.putAll(this.dexReplaces);

        try {
            new ApkSigner().signApk(ScopedStorage.getApkEditorDirectory() + File.separator + "gen_unsigned.apk", ScopedStorage.getApkEditorDirectory() + File.separator + "gen_signed.apk");
            return true;
        } catch (Exception e) {
            String strHeader = ctx.getResources()
                    .getString(R.string.sign_error);
            this.errMessage = strHeader + e.getMessage();
        }

        return false;
    }

    private boolean composeResource() {
        this.tempApkPath = targetApkPath + ".in";
        try {
            return Preferences.isAapt2(ctx) ? aapt2() : aapt();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean aapt() {
        boolean noVersionVectorOption = Preferences.getNoVersionVectorOption(aaptPath);

        List<String> paramList = new ArrayList<>();
        paramList.add(aaptPath);
        paramList.add("package");
        paramList.add("-f");
        paramList.add("-I");
        paramList.add(androidJarPath);
        paramList.add("-S");
        paramList.add(decodedFilePath + "/res");
        paramList.add("-M");
        paramList.add(decodedFilePath + "/AndroidManifest.xml");
        paramList.add("-F");
        paramList.add(tempApkPath);
        if (noVersionVectorOption) {
            paramList.add("--no-version-vectors");
        }

        long startTime = System.currentTimeMillis();
        CommandRunner cr = new CommandRunner();
        boolean ret = cr.runCommand(paramList.toArray(new String[paramList.size()]),
                null, null, 300 * 1000, true);
        Log.e("DEBUG", "aapt Time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");

        LOGGER.info("stdout: " + cr.getStdOut() + ", ret=" + ret);
        if (!ret) {
            LOGGER.info("stderr: " + cr.getStdError());
            this.errMessage = cr.getStdError();
            return false;
        }
        return true;
    }

    public boolean aapt2() throws IOException {
        boolean noVersionVectorOption = Preferences.getNoVersionVectorOption(aaptPath2);

        ArrayList<String> args = new ArrayList<>();
        //compile resources

        args.add(aaptPath2);
        args.add("compile");
        args.add("--dir");
        args.add(decodedFilePath + "/res");
        args.add("-o");

        File resPath = new File(decodedFilePath, "build");
        resPath.mkdir();

        File outputPath = FileUtils.createNewFile(resPath, "resources.zip");

        args.add(outputPath.getAbsolutePath());


        long startTime = System.currentTimeMillis();
        CommandRunner cr = new CommandRunner();
        boolean ret = cr.runCommand(args.toArray(new String[args.size()]),
                null, null, 300 * 1000, true);

        LOGGER.info("stdout: " + cr.getStdOut() + ", ret=" + ret);
        if (!ret) {
            LOGGER.info("stderr: " + cr.getStdError());
            this.errMessage = cr.getStdError();
            return false;
        }

        args.clear();

        //link resources

        args.add(aaptPath2);
        args.add("link");
        args.add("--allow-reserved-package-id");
        if (noVersionVectorOption) {
            args.add("--no-version-vectors");
        }
        args.add("--no-version-transitions");
        args.add("--auto-add-overlay");

        args.add("-I");
        args.add(androidJarPath);

        //add compiled resources
        File[] resources = resPath.listFiles();
        if (resources != null) {
            for (File file : resources) {
                if (file.isDirectory() || file.getName().equals("resources.zip")) {
                    continue;
                }
                args.add("-R");
                args.add(file.getAbsolutePath());
            }
        }

        File projectZip = new File(resPath, "resources.zip");
        if (projectZip.exists()) {
            args.add("-R");
            args.add(projectZip.getAbsolutePath());
        }

        args.add("--manifest");
        args.add(decodedFilePath + "/AndroidManifest.xml");

        args.add("-o");
        args.add(tempApkPath);

        CommandRunner cr2 = new CommandRunner();
        boolean ret2 = cr2.runCommand(args.toArray(new String[args.size()]),
                null, null, 300 * 1000, true);
        Log.e("DEBUG", "aapt Time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");

        LOGGER.info("stdout: " + cr2.getStdOut() + ", ret=" + ret2);
        if (!ret2) {
            LOGGER.info("stderr: " + cr2.getStdError());
            this.errMessage = cr.getStdError();
            return false;
        }
        return true;
    }

    public boolean prepare() {
        try {
            return prepare(ctx);
        } catch (Exception e) {
            this.errMessage = e.getMessage();
            return false;
        }
    }

    @Override
    public void setTaskCallback(ITaskCallback taskCallback) {
        this.taskCallback = taskCallback;
    }

    @Override
    public void updateAssembledFiles(int assembledFiles, int totalFiles) {
        long curTime = System.currentTimeMillis();
        if (curTime > this.lastUpdateAssembleTime + 500) {
            String fmt = ctx.getString(R.string.assemble_dex_detail);
            stepInfo.stepDescription = String.format(fmt, assembledFiles,
                    totalFiles);
            taskCallback.setTaskStepInfo(stepInfo);
        }
    }

    @Override
    public void stopRunning() {
        this.stopFlag = true;
        this.interrupt();
    }

    @Override
    public void setExtraMaker(IApkMaking extraMaker) {
        this.extraMaker = extraMaker;
    }

    public String getErrMessage() {
        return errMessage;
    }
}
