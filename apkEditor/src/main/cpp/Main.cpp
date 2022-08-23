#include <string>
#include <jni.h>
#include "Main.h"

static jclass class_String;
static jmethodID mid_getBytes;

// Signature check
extern "C" JNIEXPORT jstring JNICALL Java_com_mcal_apkeditor_utils_Native_getSignature(
        JNIEnv *env,
        jobject obj,
        jobject context) {
    return Java_com_mcal_apkeditor_utils_Native_getSignatureSalted(env, obj, context, NULL);
}

extern "C" JNIEXPORT jstring JNICALL Java_com_mcal_apkeditor_utils_Native_getSignatureSalted(
        JNIEnv *env,
        jobject /* this */,
        jobject context,
        jstring saltStr) {

    jstring packageName;
    jobject packageManagerObj;
    jobject packageInfoObj;
    jclass contextClass =  env->GetObjectClass( context);
    jmethodID getPackageNameMid = env->GetMethodID( contextClass, "getPackageName", "()Ljava/lang/String;");
    jmethodID getPackageManager =  env->GetMethodID( contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");

    jclass packageManagerClass = env->FindClass("android/content/pm/PackageManager");
    jmethodID getPackageInfo = env->GetMethodID( packageManagerClass, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");

    jclass packageInfoClass = env->FindClass("android/content/pm/PackageInfo");
    jfieldID signaturesFid = env->GetFieldID( packageInfoClass, "signatures", "[Landroid/content/pm/Signature;");

    jclass signatureClass = env->FindClass("android/content/pm/Signature");
    jmethodID signatureToByteArrayMid = env->GetMethodID( signatureClass, "toByteArray", "()[B");

    jclass messageDigestClass = env->FindClass("java/security/MessageDigest");
    jmethodID messageDigestUpdateMid = env->GetMethodID( messageDigestClass, "update", "([B)V");
    jmethodID getMessageDigestInstanceMid  = env->GetStaticMethodID( messageDigestClass, "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;");
    jmethodID digestMid = env->GetMethodID( messageDigestClass,"digest", "()[B");

    jclass base64Class = env->FindClass("android/util/Base64");
    jmethodID encodeToStringMid = env->GetStaticMethodID( base64Class,"encodeToString", "([BI)Ljava/lang/String;");

    packageName =  (jstring)env->CallObjectMethod( context, getPackageNameMid);

    packageManagerObj = env->CallObjectMethod(context, getPackageManager);
    // PackageManager.GET_SIGNATURES = 0x40
    packageInfoObj = env->CallObjectMethod( packageManagerObj,getPackageInfo, packageName, 0x40);
    jobjectArray signatures = (jobjectArray)env->GetObjectField( packageInfoObj, signaturesFid);
    //int signatureLength =  env->GetArrayLength(signatures);
    jobject signatureObj = env->GetObjectArrayElement(signatures, 0);

    jobject messageDigestObj  = env->CallStaticObjectMethod(messageDigestClass, getMessageDigestInstanceMid, env->NewStringUTF("SHA1"));
    env->CallVoidMethod(messageDigestObj, messageDigestUpdateMid, env->CallObjectMethod( signatureObj,signatureToByteArrayMid));

    //Add salt to app signature and update.
    if(saltStr != NULL)
    {
        const char *nSaltStr = env->GetStringUTFChars(saltStr, 0);
        jbyteArray saltBytes = cstr2jbyteArray(env, nSaltStr);
        env->CallVoidMethod(messageDigestObj, messageDigestUpdateMid, saltBytes);
    }
    jstring signatureHash = (jstring)env->CallStaticObjectMethod( base64Class, encodeToStringMid,env->CallObjectMethod( messageDigestObj, digestMid, signatureObj), 0);
    return signatureHash;
}

jbyteArray cstr2jbyteArray( JNIEnv *env, const char *nativeStr)
{
    jbyteArray javaBytes;
    int len = strlen( nativeStr );
    javaBytes = env->NewByteArray(len);
    env->SetByteArrayRegion(javaBytes, 0, len, (jbyte *) nativeStr );
    return javaBytes;
}

//extern "C" JNIEXPORT jstring
//JNICALL
//Java_com_mcal_apkeditor_utils_Native_test(
//        JNIEnv *env,
//        jobject /* this */) {
//    std::string hello = "3372957";
//    return env->NewStringUTF(hello.c_str());
//}