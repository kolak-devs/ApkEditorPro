#include <jni.h>

#ifndef MAIN_H
#define MAIN_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL Java_com_mcal_apkeditor_utils_Native_getSignature(
        JNIEnv *env,
        jobject /* this */,
        jobject context);
JNIEXPORT jstring JNICALL Java_com_mcal_apkeditor_utils_Native_getSignatureSalted(
        JNIEnv *env,
        jobject /* this */,
        jobject context,
        jstring saltStr);

#ifdef __cplusplus
}
#endif

jbyteArray cstr2jbyteArray( JNIEnv *env, const char *nativeStr);

#endif //MAIN_H
