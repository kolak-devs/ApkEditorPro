package com.mcal.common.utils;

import androidx.annotation.NonNull;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RootCommand implements CommandInterface {
    private static final String[] SU_LOCATIONS = {"/data/bin/su", "/system/bin/su", "/system/xbin/su"};
    private final String[] outputs = new String[2];

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check whether a process is still alive. We use this as a naive way to
     * implement timeouts.
     */
    public static boolean isProcessAlive(@NonNull Process p) {
        try {
            p.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    @NonNull
    static String readStream(InputStream stream) throws IOException {
        final char[] buffer = new char[8192];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
        int read;
        do {
            read = in.read(buffer, 0, buffer.length);
            if (read > 0)
                out.append(buffer, 0, read);
        } while (read >= 0);
        return out.toString();
    }

    static String getSuPath() {
        String path = "su";
        for (String p : SU_LOCATIONS) {
            File su = new File(p);
            if (su.exists()) {
                path = p;
            }
        }
        return path;
    }

    public static Process runWithEnv(String command, String[] env)
            throws IOException {
        Map<String, String> environment = System.getenv();
        String[] envArray = new String[environment.size()
                + (env != null ? env.length : 0)];
        int idx = 0;
        for (Map.Entry<String, String> entry : environment.entrySet())
            envArray[idx++] = entry.getKey() + "=" + entry.getValue();
        if (env != null)
            for (String entry : env)
                envArray[idx++] = entry;
        return Runtime.getRuntime().exec(command, envArray);
    }

    public boolean runRootCommand(String command, String[] env, Integer timeout) {
        return runRootCommand(command, env, timeout, false);
    }

    public boolean runRootCommand(String command, String[] env, Integer timeout, boolean readWhileExec) {
        return runRootCommand(command, env, timeout, null, readWhileExec);
    }

    public boolean runRootCommand(String command, String[] env, Integer timeout, String curDir, boolean readWhileExec) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = runWithEnv(getSuPath(), env);
            os = new DataOutputStream(process.getOutputStream());
            if (curDir != null) {
                os.writeBytes("cd " + curDir + "\n");
            }
            os.writeBytes(command + "\n");
            os.writeBytes("echo \"rc:\" $?\n");
            os.writeBytes("exit\n");
            os.flush();

            // Read stdout and stderr in another 2 threads
            InputStream outStream = process.getInputStream();
            InputStream errStream = process.getErrorStream();
            StreamReadThread thread0 = null;
            StreamReadThread thread1 = null;
            if (readWhileExec) {
                thread0 = new StreamReadThread(outStream, outputs, 0);
                thread0.start();
                thread1 = new StreamReadThread(errStream, outputs, 1);
                thread1.start();
            }

            // Handle a requested timeout, or just use waitFor() otherwise.
            if (timeout != null) {
                long finish = System.currentTimeMillis() + timeout;
                while (isProcessAlive(process)) {
                    if (System.currentTimeMillis() > finish) {
                        return true;
                    }
                }
            } else
                process.waitFor();

            if (readWhileExec) {
                thread0.close();
                thread1.close();
            } else {
                this.outputs[0] = readStream(outStream);
                this.outputs[1] = readStream(errStream);
                closeQuietly(outStream);
                closeQuietly(errStream);
            }
            return process.exitValue() == 0;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (os != null)
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (process != null) {
                try {
                    process.exitValue();
                } catch (IllegalThreadStateException e) {
                    process.destroy();
                }
            }
        }
    }

    @Override
    public boolean runCommand(String command, String[] env, Integer timeout) {
        return runRootCommand(command, env, timeout);
    }

    @Override
    public String getStdOut() {
        return outputs[0];
    }

    @Override
    public String getStdError() {
        return outputs[1];
    }

    private void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean runCommand(String command, String[] env, Integer timeout, boolean readWhileExec) {
        return runRootCommand(command, env, timeout, readWhileExec);
    }

    private static class StreamReadThread extends Thread {
        private final InputStream input;
        private final String[] outputs;
        private final int index;

        public StreamReadThread(InputStream input, String[] outputs,
                                int index) {
            this.input = input;
            this.outputs = outputs;
            this.index = index;
        }

        @Override
        public void run() {
            final char[] buffer = new char[128];
            StringBuilder out = new StringBuilder();
            try {
                Reader in = new InputStreamReader(input, StandardCharsets.UTF_8);
                int read;
                do {
                    read = in.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        out.append(buffer, 0, read);
                    }
                } while (read >= 0);
            } catch (Exception e) {
                e.printStackTrace();
            }

            outputs[index] = out.toString();
        }

        public void close() {
            this.interrupt();
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
