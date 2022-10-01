package com.mcal.common.utilsOld;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mcal.common.utils.RootCommand;

import org.jetbrains.annotations.Contract;

import java.io.File;

public class FileCopyUtils {

    // Support root mode and non-root mode
    @NonNull
    @Contract("_ -> new")
    private static CommandInterface createCommandRunner(boolean isRootMode) {
        if (isRootMode) {
            return new RootCommand();
        } else {
            return new CommandRunner();
        }
    }

    public static void copyBack(@NonNull Context ctx, String path, String realPath, boolean isRootMode) throws Exception {
        CommandInterface rc = createCommandRunner(isRootMode);
        String strCmd = "cp";
        File bin = new File(ctx.getFilesDir(), "mycp");
        if (bin.exists()) {
            strCmd = bin.getPath();
        }
        boolean copyRet = rc.runCommand(String.format(strCmd + " %s \"%s\"", path, realPath), null, 3000);

        // Copy file failed, use the original file
        if (!copyRet) {
            throw new Exception("Can not write file to " + realPath);
        }
    }
}