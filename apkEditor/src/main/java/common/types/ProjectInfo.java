package common.types;

import java.io.Serializable;

public class ProjectInfo implements Serializable {

    // The original apk we are editing
    public String apkPath;

    public String decodeRootPath;
    public ActivityState state;
}
