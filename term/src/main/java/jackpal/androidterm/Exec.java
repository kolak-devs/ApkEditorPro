package jackpal.androidterm;

import java.io.IOException;

/**
 * Utility methods for managing a pty file descriptor.
 */
public class Exec {
    // Warning: bump the library revision, when an incompatible change happens
    static {
        System.loadLibrary("jackpal-androidterm5");
    }

    static native void setPtyWindowSizeInternal(int fd, int row, int col, int xpixel, int ypixel) throws IOException;

    static native void setPtyUTF8ModeInternal(int fd, boolean utf8Mode) throws IOException;
}

