import java.io.*;
import com.mcal.bshengine.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final BshEngineActivity activity = XActivity;

Main() {
    public void createSmaliClassDecoder() {
        String smaliStringer = ".class public Lcom/mcal/Stringer;\n" +
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
        File stringerFilePath = new File(XStorage.getSmaliDir(), "Stringer.smali");
        XFileHelper.writeText(stringerFilePath, smaliStringer);
    }

    public void encryptSmaliStringsRegex(File smaliFile, String className, String methodName) {
        StringBuilder out = new StringBuilder();
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
                    register = matcher.group(1);
                    startTextLine = "    const-string " + register + ", \"";
                    // Конвертация юникод симловов в буквы
                    String unescapeText = XString.unescapeUnicode(text);
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
        String stringerClassName = "Lcom/mcal/Stringer;";
        String stringerMethodName = "decode";
        // Получаем папки с smali кодом
        for(File smaliDirName: XStorage.getAllSmaliDirs()) {
            // Получаем список классов
            for(File f: XFileHelper.getFiles(smaliDirName)){
                // Проверяем что это smali файл
                if(XFileHelper.isFile(f) && XFileHelper.isSmali(f)){
                    XLog.info(f.getPath());
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
            String error = e.toString();
            if (error != null || error.length() > 0) {
                XLog.error(error);
            }
        }
        XLog.info("FINISHED!");
    }
    return this;
}

main = Main();
main.onCreate();