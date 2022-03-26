# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontwarn android.support.v4.**
-dontwarn com.mcal.**

-dontwarn antlr.**
-dontwarn org.antlr.**
-dontwarn com.google.**
-dontwarn org.mozilla.**
-dontwarn org.objenesis.**
-dontwarn org.slf4j.**
-dontwarn com.polites.android.**
-dontwarn com.beust.**

-keep class com.mcal.apkeditor.R$** { *; }
-keep class com.mcal.seticon.** { *; }
-keep class com.mcal.apkeditor.pro.** { *; }
-keep class com.mcal.apkeditor.translate.TranslateDialog { *; }
-keep class com.mcal.neweditor.Token { *; }
-keep class com.mcal.appdm.util.FileCopyUtil { *; }
-keep class com.mcal.apkeditor.translate.** { *; }

# For project persistence
-keep class common.types.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class javax.annotation.** { *; }
-keep class android.** { *; }
#-keep class antlr.** { *; }
#-keep class org.antlr.** { *; }
-keep class com.google.** { *; }
-keep class com.beust.** { *; }
-keep class org.jf.** { *; }
-keep class org.apache.** { *; }
-keep class org.mozilla.** { *; }

-keep class jadx.** { *; }
-keep class jadx.plugins.** { *; }
-keep class io.github.skylot.** { *; }

#-keepattributes LineNumberTable

-obfuscationdictionary proguard-bin.txt
-packageobfuscationdictionary proguard-bin.txt
-classobfuscationdictionary proguard-bin.txt

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    public static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
    public static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    public static void checkNotNull(java.lang.Object);
    public static void checkNotNull(java.lang.Object, java.lang.String);
    public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
    public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
    public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    public static void throwUninitializedPropertyAccessException(java.lang.String);
}