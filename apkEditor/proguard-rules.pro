#-dontobfuscate

#-keep class com.mcal.apkeditor.R$** { *; }
-keep class com.mcal.neweditor.Token { *; }
-keep class com.mcal.apkeditor.translate.** { *; }

# For project persistence
-keep class common.types.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class kellinwood.** { *; }
#-keep class org.spongycastle.** { *; }
#-keep class com.android.apksig.** { *; }
#-keep class com.android.apksigner.** { *; }

-keep class brut.** { *; }
-keep class jadx.** { *; }

#-keep class brut.androlib.meta.MetaInfo.** {*;}
#-keep class brut.androlib.meta.PackageInfo.** {*;}
#-keep class brut.androlib.meta.UsesFramework.** {*;}
#-keep class brut.androlib.meta.VersionInfo.** {*;}

#-keep class javax.annotation.** { *; }
#-keep class android.** { *; }
#-keep class com.google.** { *; }
#-keep class com.beust.** { *; }
#-keep class org.jf.** { *; }
#-keep class org.apache.** { *; }
#-keep class org.mozilla.** { *; }

#-keep class jadx.** { *; }
#-keep class jadx.plugins.** { *; }
#-keep class io.github.skylot.** { *; }

#-keepattributes LineNumberTable

#-obfuscationdictionary proguard-bin.txt
#-packageobfuscationdictionary proguard-bin.txt
#-classobfuscationdictionary proguard-bin.txt

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