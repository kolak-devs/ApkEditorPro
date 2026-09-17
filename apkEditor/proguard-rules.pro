-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute
-allowaccessmodification

#-dontobfuscate

-keep class com.mcal.apkeditor.translate.** { *; }
-keep class com.mcal.uidesigner.** { *; }
-keep class com.mcal.bshengine.api.** { *; }

# For project persistence
-keep class common.types.** { *; }

-keep class kellinwood.** { *; }
#-keep class org.spongycastle.** { *; }
#-keep class com.android.apksig.** { *; }
#-keep class com.android.apksigner.** { *; }

#-keep class com.mcal.common.activities.CustomizedLangActivity.** { *; }
-keep class android.content.** { *; } # todo
#-keep class android.support.** { *; }
-keep class android.util.** { *; } # todo
-keep class androidx.appcompat.app.** { *; }
#-keep class com.google.protobuf.** { *; }
-keep class com.google.android.material.color.** { *; }
#-keep class java.** { *; }
-keep class javax.xml.** { *; }
#-keep class kotlin.** { *; }
#-keep class kotlinx.** { *; }
-keep class javax.xml.** { *; }
-keep class org.apache.** { *; }#.batik.** { *; }
-keep class org.w3c.css.sac.** { *; }
-keep class org.xml.sax.** { *; }
-keep class org.xmlpull.v1.** { *; }
-keep class com.blankj.utilcode.util.** { *; } # todo
-keep class io.github.rosemoe.** { *; }

-keep class com.mcal.apkeditor.utils.Native.** { *; }
-keep class brut.** { *; }
-keep class jadx.** { *; }
-keep class com.google.common.collect.** { *; }
-keep class apkeditor.Utils.** { *; }

# UI Designer
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }

-obfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt

#-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
#    public static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
#    public static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
#    public static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
#    public static void checkNotNull(java.lang.Object);
#    public static void checkNotNull(java.lang.Object, java.lang.String);
#    public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
#    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
#    public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
#    public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
#    public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
#    public static void throwUninitializedPropertyAccessException(java.lang.String);
#}

# dontwarn - classes not available on Android
-dontwarn com.mcal.apksigner.ApkSigner
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn org.xmlpull.v1.wrapper.**
-dontwarn org.xmlpull.v1.wrapper.classic.**

# bsh service impl (hilang saat shrink)
-dontwarn bsh.engine.BshScriptEngineFactory
# xpp3 service impl dalam jar filter (tidak dipaketkan)
-dontwarn org.xmlpull.mxp1.**

# TextMate (tm4e) & Jackson — dipakai sora editor untuk syntax highlighting.
# tm4e menggunakan reflection dan state static; tanpa keep ini release crash
# (ExceptionInInitializerError saat TextMateAnalyzer.tokenizeLine).
-dontwarn org.eclipse.tm4e.**
-keep class org.eclipse.tm4e.** { *; }
-dontwarn com.fasterxml.jackson.**
-keep class com.fasterxml.jackson.** { *; }

# JRuby JOni & JCodings — engine regex TextMate (dipakai org.eclipse.tm4e.core
# melalui org.joni.Regex). Tanpa keep ini R8 mengobfusasi/membuang kelasnya,
# membuat OnigRegExp gagal init saat release (ExceptionInInitializerError : NPE
# di <clinit>), lihat Rosemoe/sora-editor#398.
-dontwarn org.joni.**
-keep class org.joni.** { *; }
-dontwarn org.jcodings.**
-keep class org.jcodings.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod