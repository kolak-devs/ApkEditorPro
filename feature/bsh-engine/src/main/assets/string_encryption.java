import java.io.*;
import com.mcal.bshengine.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

// Min API 2.4.6

Main() {
    public ArrayList<String> getWhiteList() {
        final ArrayList<String> whiteList = new ArrayList<String>();
        whiteList.add("android/");
        whiteList.add("androidx/");
        whiteList.add("anet/channel/");
        whiteList.add("anetwork/");
        whiteList.add("bolts/");
        whiteList.add("coil/");
        whiteList.add("com/afollestad/");
        whiteList.add("com/airbnb/");
        whiteList.add("com/alibaba/");
        whiteList.add("com/alipay/");
        whiteList.add("com/amap/");
        whiteList.add("com/amplitude/");
        whiteList.add("com/android/");
        whiteList.add("com/appsamurai/");
        whiteList.add("com/appsflyer/");
        whiteList.add("com/artifex/");
        whiteList.add("com/auth0/");
        whiteList.add("com/birbit/");
        whiteList.add("com/bumptech/");
        whiteList.add("com/bytedance/");
        whiteList.add("com/dropbox/");
        whiteList.add("com/facebook/");
        whiteList.add("com/fasterxml/");
        whiteList.add("com/flurry/");
        whiteList.add("com/github/");
        whiteList.add("com/google/");
        whiteList.add("com/huawei/");
        whiteList.add("com/iflytek/");
        whiteList.add("com/intercom/");
        whiteList.add("com/itextpdf/");
        whiteList.add("com/jakewharton/");
        whiteList.add("com/kenai/");
        whiteList.add("com/microsoft/");
        whiteList.add("com/mikepinz/");
        whiteList.add("com/millennialmedia/");
        whiteList.add("com/my/tracker/");
        whiteList.add("com/netplug/");
        whiteList.add("com/nuance/");
        whiteList.add("com/onesignal/");
        whiteList.add("com/samsung/");
        whiteList.add("com/squareup/");
        whiteList.add("com/taobao/");
        whiteList.add("com/tencent/");
        whiteList.add("com/thoughtworks/");
        whiteList.add("com/twitter/");
        whiteList.add("com/unisound/");
        whiteList.add("com/unity3d/");
        whiteList.add("com/vk/");
        whiteList.add("com/xiaomi/");
        whiteList.add("com/xuhao/");
        whiteList.add("dagger/");
        whiteList.add("dalvik/");
        whiteList.add("firebase/");
        whiteList.add("fusion/");
        whiteList.add("io/ktor/");
        whiteList.add("io/reactivex/");
        whiteList.add("java/");
        whiteList.add("javax/");
        whiteList.add("junit/");
        whiteList.add("kotlin/");
        whiteList.add("kotlinx/");
        whiteList.add("microsoft/");
        whiteList.add("mtopsdk/");
        whiteList.add("net/sqlcipher/");
        whiteList.add("okhttp3/");
        whiteList.add("okio/");
        whiteList.add("org/android/");
        whiteList.add("org/apache/");
        whiteList.add("org/bouncycastle/");
        whiteList.add("org/checkframework/");
        whiteList.add("org/chromium/");
        whiteList.add("org/codehaus/");
        whiteList.add("org/commonmark/");
        whiteList.add("org/fmod/");
        whiteList.add("org/greenrobot/");
        whiteList.add("org/hamcrest/");
        whiteList.add("org/intellij/");
        whiteList.add("org/jboss/");
        whiteList.add("org/jdom/");
        whiteList.add("org/jetbrains/");
        whiteList.add("org/jsoup/");
        whiteList.add("org/junit/");
        whiteList.add("org/koin/");
        whiteList.add("org/objectweb/");
        whiteList.add("org/qtproject/");
        whiteList.add("org/reactivestreams/");
        whiteList.add("org/spongecastle/");
        whiteList.add("org/tensorflow/");
        whiteList.add("proto/");
        whiteList.add("retrofit2/");
        whiteList.add("ru/ok/");
        whiteList.add("ru/rbs/");
        whiteList.add("ru/sberbank/");
        whiteList.add("ru/sberdevices/");
        whiteList.add("rx/");
        whiteList.add("sberid/");
        return whiteList;
    }

    public void createSmaliClassDecoder() {
        final String smaliStringer = ".class public Lcom/mcal/Stringer;\n" +
        ".super Ljava/lang/Object;\n" +
        ".source \"Stringer.java\"\n" +
        "\n" +
        "\n" +
        "# direct methods\n" +
        ".method public constructor <init>()V\n" +
        "    .registers 4\n" +
        "\n" +
        "    .prologue\n" +
        "    .line 8\n" +
        "    move-object v0, p0\n" +
        "\n" +
        "    move-object v2, v0\n" +
        "\n" +
        "    invoke-direct {v2}, Ljava/lang/Object;-><init>()V\n" +
        "\n" +
        "    return-void\n" +
        ".end method\n" +
        "\n" +
        ".method public static decode(Ljava/lang/String;)Ljava/lang/String;\n" +
        "    .registers 9\n" +
        "\n" +
        "    .prologue\n" +
        "    .line 6\n" +
        "    move-object v0, p0\n" +
        "\n" +
        "    invoke-static {}, Ljava/util/Base64;->getDecoder()Ljava/util/Base64$Decoder;\n" +
        "\n" +
        "    move-result-object v4\n" +
        "\n" +
        "    move-object v5, v0\n" +
        "\n" +
        "    invoke-virtual {v4, v5}, Ljava/util/Base64$Decoder;->decode(Ljava/lang/String;)[B\n" +
        "\n" +
        "    move-result-object v4\n" +
        "\n" +
        "    move-object v2, v4\n" +
        "\n" +
        "    .line 7\n" +
        "    new-instance v4, Ljava/lang/String;\n" +
        "\n" +
        "    move-object v7, v4\n" +
        "\n" +
        "    move-object v4, v7\n" +
        "\n" +
        "    move-object v5, v7\n" +
        "\n" +
        "    move-object v6, v2\n" +
        "\n" +
        "    invoke-direct {v5, v6}, Ljava/lang/String;-><init>([B)V\n" +
        "\n" +
        "    move-object v0, v4\n" +
        "\n" +
        "    return-object v0\n" +
        ".end method";
        final File stringerFilePath = new File(XStorage.getSmaliDir(), "Stringer.smali");
        XFileHelper.writeText(stringerFilePath, smaliStringer);
    }

    int countClasses = 0;
    int countStrings = 0;

    public void encryptSmaliStringsRegex(File smaliFile, String className, String methodName) {
        final StringBuilder out = new StringBuilder();
        String register;
        String startTextLine;
        String encrypted;
        String call;
        String moveResult;
        // Чтение файла построчно
        for (String line : XFileHelper.readFileAsLines(smaliFile)) {
            if (line.contains("const-string")) {
                // Ищем строки которые нужно зашифровать
                final Matcher matcher = Pattern.compile("const-string ([vp]\\d{1,2}), \"(.*)\"").matcher(line);
                if (matcher.find()) {
                    final String text = matcher.group(2);
                    if (text == null || text.length() <= 0) {
                        // Если строка нулл или пустая - пропускаем
                        out.append(line).append("\n");
                        continue;
                    }
                    countStrings++;
                    register = matcher.group(1);
                    startTextLine = "    const-string " + register + ", \"";
                    // Конвертация юникод симловов в буквы
                    final String unescapeText = XString.unescapeUnicode(text);
                    XLog.info("Original: " + unescapeText);
                    encrypted = XCipher.encodeBase64(unescapeText);
                    XLog.info("Encrypted: " + encrypted);
                    if (Integer.parseInt(register.substring(1)) > 15 && register.startsWith("v")) {
                        call = "    invoke-static/range {" + register + " .. " + register + "}, " + className + "->" + methodName + "(" + "Ljava/lang/String;)Ljava/lang/String;\n";
                    } else if (register.startsWith("v") || (register.startsWith("p") && Integer.parseInt(register.substring(1)) < 10)) {
                        call = "    invoke-static {" + register + "}, " + className + "->" + methodName + "(" + "Ljava/lang/String;)Ljava/lang/String;\n";
                    } else {
                        out.append(line).append("\n");
                        continue;
                    }
                    moveResult = "    move-result-object " + register + "\n";
                    out.append(startTextLine).append(encrypted).append("\"\n").append(call).append(moveResult);
                } else {
                    out.append(line).append("\n");
                }
            } else {
                out.append(line).append("\n");
            }
        }
        XFileHelper.writeText(smaliFile, out.toString());
    }

    public void patchSmali() {
        final String stringerClassName = "Lcom/mcal/Stringer;";
        final String stringerMethodName = "decode";
        // Получаем папки с smali кодом
        for(File smaliDirName: XStorage.getAllSmaliDirs()) {
            // Получаем список классов
            for(File f: XFileHelper.getFilterFiles(smaliDirName, getWhiteList())) {
                final String path = f.getPath();
                // Проверяем что это smali файл
                if(XFileHelper.isFile(f) && XFileHelper.isSmali(f)) {
                    countClasses++;
                    XLog.info(path, true);
                    encryptSmaliStringsRegex(f,stringerClassName,stringerMethodName);
                }
            }
        }
        createSmaliClassDecoder();
    }

    public void onCreate() {
        XLog.info("STARTING!");
        try {
            patchSmali();
        } catch (Exception e) {
            final String error = e.toString();
            if (error != null || error.length() > 0) {
                XLog.error(error);
            }
        }
        XAlert.show("Finished", "Encrypted classes: " + countClasses + "\nEncrypted strings: " + countStrings);
    }
    return this;
}

main = Main();
main.onCreate();