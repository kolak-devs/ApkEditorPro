import java.io.*;
import android.widget.*;
import com.mcal.bshengine.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final BshEngineActivity activity = XActivity;

final File smaliDir = XStorage.getSmali();

Main() {
    public void encryptSmaliStringsRegex(File smaliFile, String className, String methodName) {
        StringBuilder out = new StringBuilder();
        String register;
        String startTextLine;
        String encrypted;
        String call;
        String moveResult;
        for (String line : XFileHelper.readFileAsLines(smaliFile)) {
            if (line.contains("const-string")) {
                final Matcher matcher = Pattern.compile("const-string ([vp]\\d{1,2}), \"(.*)\"").matcher(line);
                if (matcher.find()) {
                    final String text = matcher.group(2);
                    if (text == null || text.length() <= 0) {
                        out.append(line).append("\n");
                        continue;
                    }
                    register = matcher.group(1);
                    startTextLine = "    const-string $register, \"";
                    encrypted = XCipher.encodeBase64(XString.unescapeUnicode(text));
                    if (Integer.parseInt(register.substring(1)) > 15 && register.startsWith("v")) {
                        call = "    invoke-static/range {" + register + " .. " + register + "}, " + className + "->" + methodName + "(" + "Ljava/lang/String;)Ljava/lang/String;\n";
                    } else if (register.startsWith("v") || (register.startsWith("p") && Integer.parseInt(register.substring(1)) < 10)) {
                        call = "    invoke-static {" + register + "}, " + className + "->" + methodName + "(" + "Ljava/lang/String;)Ljava/lang/String;\n";
                    } else {
                        out.append(line).append("\n");
                        continue;
                    }
                    moveResult = "    move-result-object $register\n";
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

    String stringerClassName = "Lcom/mcal/Stringer";
    String stringerMethodName = "Lcom/mcal/Stringer";

    public void patchSmali() {
        for(File f: smaliDir.listFiles()) {
            if(f.isFile() && f.getName().endsWith(".smali")) {
                XLog.info(f.getPath());
                encryptSmaliStringsRegex(f, stringerClassName, stringerMethodName);
            }
        }
    }

    public void onCreate() {
        patchSmali();
    }
    return this;
}

main = Main();
main.onCreate();