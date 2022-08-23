package com.mcal.common.utilsOld;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUtils {
    // Extract a file from jar to the target path
    public static void extractFile(String jarFilePath, String fileInJar,
                                   String targetPath) throws IOException {
        JarFile inputJar = new JarFile(new File(jarFilePath), false); // Don't
        // verify.

        JarEntry inEntry = inputJar.getJarEntry(fileInJar);
        InputStream in = inputJar.getInputStream(inEntry);
        FileOutputStream out = new FileOutputStream(targetPath);
        IOUtils.copy(in, out);
        out.close();
        inputJar.close();
    }
}
