package com.mcal.apkeditor.ce;

import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mcal.apkeditor.ce.e.MyInputStream;
import com.mcal.apkeditor.ce.e.ResAttrIdChunk;
import com.mcal.apkeditor.ce.e.ResStringChunk;

import org.jetbrains.annotations.Contract;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

interface IAttributeCallback {
    void onAttribute(String tag, int attrNsIdx, int attrNameIdx,
                     int attrValIdx, int type, int attrValIdx2);
}

class AxmlBodyChunk {
    public static int endDocTag = 0x00100101;
    public static int startTag = 0x00100102;
    public static int endTag = 0x00100103;
    public static int namespaceTag = 0x00100100;
    public static int cdataTag = 0x00100104;
    // index in the string table ("android" & "manifest")
    ResStringChunk stringChunk;
    private byte[] rawChunkData;
    private final int stringCount; // stringCount is already added by 1
    private int androidIndex;

    // addedAttrPosition = the position of added attribute name in string table
    public AxmlBodyChunk(ResStringChunk strChunk) {
        this.stringCount = strChunk.getStringCount();
        this.stringChunk = strChunk;

        for (int i = 0; i < stringCount; i++) {
            if ("android".equals(strChunk.getStringByIndex(i))) {
                androidIndex = i;
            }
        }
    }

    // Check the attribute value type whether refer to the string table
    public static boolean isTypeRefToStrTable(int type) {
        if (type == TypedValue.TYPE_STRING) {
            return true;
        }
        if (type == TypedValue.TYPE_ATTRIBUTE) {
            return false;
        }
        if (type == TypedValue.TYPE_REFERENCE) {
            return false;
        }
        if (type == TypedValue.TYPE_FLOAT) {
            return false;
        }
        if (type == TypedValue.TYPE_INT_HEX) {
            return false;
        }
        if (type == TypedValue.TYPE_INT_BOOLEAN) {
            return false;
        }
        if (type == TypedValue.TYPE_DIMENSION) {
            return false;
        }
        if (type == TypedValue.TYPE_FRACTION) {
            return false;
        }
        if (type >= TypedValue.TYPE_FIRST_COLOR_INT
                && type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return false;
        }
        if (type >= TypedValue.TYPE_FIRST_INT
                && type <= TypedValue.TYPE_LAST_INT) {
            return false;
        }
        return false;
    }

    private boolean isInterestedTag(int tagIdx) {
        String name = stringChunk.getStringByIndex(tagIdx);
        return "uses-permission".equals(name) || "manifest".equals(name)
                || "application".equals(name) || "activity".equals(name)
                || "service".equals(name) || "receiver".equals(name)
                || "provider".equals(name) || "activity-alias".equals(name)
                || "category".equals(name) || "permission".equals(name)
                || "uses-sdk".equals(name);
    }

    public byte[] getRawChunkData() {
        return rawChunkData;
    }

    public int parseNext(@NonNull MyInputStream is, IAttributeCallback callback)
            throws IOException {
        int chunkTag = is.readInt();
        int chunkSize = is.readInt();

        rawChunkData = new byte[chunkSize];
        ManifestParser.setInt(rawChunkData, 0, chunkTag);
        ManifestParser.setInt(rawChunkData, 4, chunkSize);
        if (chunkSize > 8) {
            is.readFully(rawChunkData, 8, chunkSize - 8);
        }

        if (chunkTag == startTag) { // XML START TAG
            // int lineNo = AddAttribute.getInt(rawChunkData, 2 * 4);
            // int tag3 = AddAttribute.getInt(rawChunkData, 3 * 4);
            int nameNsSi = ManifestParser.getInt(rawChunkData, 4 * 4);
            int nameSi = ManifestParser.getInt(rawChunkData, 5 * 4);
            // Expected to be 14001400
            // int tag6 = AddAttribute.getInt(rawChunkData, 6 * 4);
            // Number of Attributes to follow
            int attrNum = ManifestParser.getInt(rawChunkData, 7 * 4);
            // Expected to be 00000000
            // int tag8 = AddAttribute.getInt(rawChunkData, 8 * 4);

            // Look for the Attributes
            // 0th word: StringIndex of Attribute Name's Namespace, or FFFFFFFF
            // 1st word: StringIndex of Attribute Name
            // 2nd word: StringIndex of Attribute Value, or FFFFFFF if
            // ResourceId
            // used
            // 3rd word: Flags? (This is type)
            // 4th word: str ind of attr value again, or ResourceId of value
            if (isInterestedTag(nameSi)) {
                for (int ii = 0; ii < attrNum; ii++) {
                    int attrNsIdx = ManifestParser.getInt(rawChunkData,
                            36 + ii * 20);
                    int attrNameIdx = ManifestParser.getInt(rawChunkData,
                            36 + ii * 20 + 4);
                    int attrValIdx = ManifestParser.getInt(rawChunkData,
                            36 + ii * 20 + 8);
                    int type = ManifestParser.getInt(rawChunkData,
                            36 + ii * 20 + 12) >> 16;
                    type = ((type & 0xff00) >> 8) | ((type & 0x00ff) << 8);
                    int attrValIdx2 = ManifestParser.getInt(rawChunkData,
                            36 + ii * 20 + 16);
                    ManifestParser
                            .log("%s=%s, type = 0X%x, attrValIdx=%d, attrValIdx2=%d",
                                    stringChunk.getStringByIndex(attrNameIdx),
                                    stringChunk.getStringByIndex(attrValIdx),
                                    type, attrValIdx, attrValIdx2);
                    callback.onAttribute(stringChunk.getStringByIndex(nameSi),
                            attrNsIdx, attrNameIdx, attrValIdx, type,
                            attrValIdx2);
                }
            }

        } else if (chunkTag == endTag) { // XML END TAG
            int nameNsSi = ManifestParser.getInt(rawChunkData, 4 * 4);
            int nameSi = ManifestParser.getInt(rawChunkData, 5 * 4);
        } else if (chunkTag == endDocTag) { // END OF XML DOC TAG
            int prefix = ManifestParser.getInt(rawChunkData, 4 * 4);
            int uri = ManifestParser.getInt(rawChunkData, 5 * 4);
        } else if (chunkTag == namespaceTag) {
            int prefix = ManifestParser.getInt(rawChunkData, 4 * 4);
            int uri = ManifestParser.getInt(rawChunkData, 5 * 4);
        } else if (chunkTag == cdataTag) {

        }
        return chunkTag;
    }
}

// Designed to add installLocation Attribute to AndroidManifest binary file
public class ManifestParser implements IAttributeCallback {

    private final MyInputStream input;

    private ResStringChunk strChunk;
    private ResAttrIdChunk attrIdChunk;

    // Value we concerned
    private final ManifestInfo result;

    // Temp variable to record the string index of last activity[-alias]
    private int lastActivityNameIdx = -1;

    // Record provider info during manifest parsing
    private ManifestInfo.ProviderInfo unfinishedProvider;

    public ManifestParser(InputStream is) {
        input = new MyInputStream(is);
        result = new ManifestInfo();
    }

    public static void main(@NonNull String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: AddInstallLocationAttr.jar inputFile outputFile");
            return;
        }

        final String inputFile = args[0];
        final String outputFile = args[1];
//        final String inputFile = "D:\\Android\\apk\\AndroidManifest.xml";
//        final String outputFile = "D:\\Android\\apk\\out.xml";

        final ManifestParser ama = new ManifestParser(new FileInputStream(inputFile));
        ama.parse();
    }

    @Contract(pure = true)
    protected static int getInt(@NonNull byte[] buf, int offset) {
        return ((int) buf[offset] & 0xff)
                | (((int) buf[offset + 1] & 0xff) << 8)
                | (((int) buf[offset + 2] & 0xff) << 16)
                | (((int) buf[offset + 3] & 0xff) << 24);
    }

    @Contract(pure = true)
    protected static int getShort(@NonNull byte[] buf, int offset) {
        return ((int) buf[offset] & 0xff)
                | (((int) buf[offset + 1] & 0xff) << 8);
    }

    protected static void setInt(@NonNull byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
        buf[offset + 2] = (byte) ((value >> 16) & 0xff);
        buf[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    protected static void setShort(@NonNull byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    protected static void log(String format, Object... arguments) {
        // String msg = String.format(format, arguments);
        // System.out.println(msg);
    }

    // If the string inside string pool is NULL/empty, get it from attr table
    @Nullable
    @Contract(pure = true)
    public static String getNameFromAttr(int attr) {
        switch (attr) {
            case 0x01010000:
                return "theme";
            case 0x01010001:
                return "label";
            case 0x01010002:
                return "icon";
            case 0x01010003:
                return "name";
            case 0x01010018:
                return "authorities";
            case 0x0101021c:
                return "versionName";
            case 0x0101021b:
                return "versionCode";
        }
        return null;
    }

    public ManifestInfo getManifestInfo() {
        return result;
    }

    public void parse() throws Exception {
        // Header
        int headTag = input.readInt();
        int fileSize = input.readInt();

        // String table
        strChunk = new ResStringChunk();
        strChunk.parse(input);
        log("ChunkSize of Resource String: %d", strChunk.chunkSize);
        log("String Count: %d", strChunk.getStringCount());
        //log("String Offset: 0x%x", strChunk.stringOffset);
        // if (strChunk.styleCount != 0 || strChunk.styleOffset != 0) {
        // throw new Exception(
        // "Detected style information, not supported yet!");
        // }
        for (int i = 0; i < strChunk.getStringCount(); ++i) {
            result.strings.add(strChunk.getStringByIndex(i));
        }

        // Resource Attribute ID table
        attrIdChunk = new ResAttrIdChunk();
        attrIdChunk.parse(input);

        // Body
        AxmlBodyChunk body = new AxmlBodyChunk(strChunk);
        int tag = 0;
        do {
            tag = body.parseNext(input, this);
        } while (tag != AxmlBodyChunk.endDocTag);
        fileSize += 20;
    }

    @Override
    public void onAttribute(String tag, int attrNsIdx, int attrNameIdx,
                            int attrValIdx, int type, int attrValIdx2) {
        String attrName = strChunk.getStringByIndex(attrNameIdx);
        if (attrName == null || attrName.equals("")) {
            if (attrNameIdx < attrIdChunk.getCount()) {
                attrName = getNameFromAttr(attrIdChunk.attrIdArray[attrNameIdx]);
                //Log.d("DEBUG", "getNameFromAttr return " + attrName);
            }
        }
        int idx = (attrValIdx2 >= 0 ? attrValIdx2 : attrValIdx);

        if ("uses-permission".equals(tag)) {
            if ("name".equals(attrName)) {
                result.permissions.add(strChunk.getStringByIndex(idx));
            }
        } else if ("manifest".equals(tag)) {
            if ("versionCode".equals(attrName)) {
                result.versionCode = idx;
            } else if ("versionName".equals(attrName)
                    && AxmlBodyChunk.isTypeRefToStrTable(type)) {
                result.verNameIdx = idx;
                result.versionName = strChunk.getStringByIndex(idx);
            } else if ("installLocation".equals(attrName)) {
                result.installLocation = idx;
            } else if ("package".equals(attrName)
                    && AxmlBodyChunk.isTypeRefToStrTable(type)) {
                result.pkgNameIdx = idx;
                result.packageName = strChunk.getStringByIndex(idx);
            }
        } else if ("application".equals(tag)) {
            if ("label".equals(attrName)) {
                if (AxmlBodyChunk.isTypeRefToStrTable(type)) {
                    result.appNameIdx = idx;
                    result.appName = strChunk.getStringByIndex(idx);
                } else { // resource id
                    result.appNameId = idx;
                }
            } else if ("name".equals(attrName)) {
                result.compNameIdxs.add(idx);
                result.applicationCls = strChunk.getStringByIndex(idx);
            } else if ("icon".equals(attrName)) {
                result.launcherId = idx;
            }
        } else if ("activity".equals(tag) || "service".equals(tag)
                || "receiver".equals(tag) || "provider".equals(tag)) {
            if ("name".equals(attrName)
                    && AxmlBodyChunk.isTypeRefToStrTable(type)) {
                result.compNameIdxs.add(idx);
                switch (tag) {
                    case "activity":
                        result.compNameIdx2Type.put(idx, 0);
                        break;
                    case "service":
                        result.compNameIdx2Type.put(idx, 1);
                        break;
                    case "receiver":
                        result.compNameIdx2Type.put(idx, 2);
                        break;
                    case "provider":
                        result.compNameIdx2Type.put(idx, 3);
                        break;
                }
            }

            if ("activity".equals(tag) && "name".equals(attrName)) {
                lastActivityNameIdx = idx;
            }
            if ("provider".equals(tag)
                    && AxmlBodyChunk.isTypeRefToStrTable(type)) {
                if (unfinishedProvider == null) {
                    unfinishedProvider = new ManifestInfo.ProviderInfo();
                }
                if ("authorities".equals(attrName)) {
                    unfinishedProvider.authorityIdx = idx;
                    unfinishedProvider.authority = strChunk
                            .getStringByIndex(idx);
                } else if ("name".equals(attrName)) {
                    unfinishedProvider.nameIdx = idx;
                    unfinishedProvider.name = strChunk.getStringByIndex(idx);
                }
                if (unfinishedProvider.authority != null
                        && unfinishedProvider.name != null) {
                    result.providerInfoList.add(unfinishedProvider);
                    unfinishedProvider = null;
                }
            }
        } else if ("activity-alias".equals(tag)) {
            if ("name".equals(attrName)) {
                lastActivityNameIdx = idx;
            } else if ("targetActivity".equals(attrName)) {
                result.targetActivityIdxs.put(lastActivityNameIdx, idx);
            }
        }
        // To detect the launcher activity
        else if ("category".equals(tag)) {
            // android:name=android.intent.category.LAUNCHER
            if ("name".equals(attrName)) {
                if ("android.intent.category.LAUNCHER".equals(strChunk
                        .getStringByIndex(idx))) {
                    if (lastActivityNameIdx != -1) {
                        result.launcherActIdxs.add(lastActivityNameIdx);
                    }
                }
            }
        }
        // Self defined permissions
        else if ("permission".equals(tag)) {
            if ("name".equals(attrName)) {
                result.permissionIdx2Name.put(idx,
                        strChunk.getStringByIndex(idx));
            }
        }
        // SDK version
        else if ("uses-sdk".equals(tag)) {
            if ("minSdkVersion".equals(attrName)) {
                result.minSdkVersion = idx;
            } else if ("targetSdkVersion".equals(attrName)) {
                result.targetSdkVersion = idx;
            } else if ("maxSdkVersion".equals(attrName)) {
                result.maxSdkVersion = idx;
            }
        }
    }
}
