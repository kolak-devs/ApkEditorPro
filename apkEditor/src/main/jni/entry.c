#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <sys/ptrace.h>
#include "util.h"

#include "libzip/config.h"

#ifdef HAVE_UNISTD_H

#include <unistd.h>

#endif

#ifndef HAVE_GETOPT
#include "getopt.h"
#endif

#include "libzip/zip.h"
#include "libzip/compat.h"

__attribute__((section (".mtext")))
void zip_modify(JNIEnv *env, jobject thiz, jstring _tname, jstring _sname,
                jstring _added, int len1, jstring _removed, int len2, jstring _replaced, int len3);

JNIEXPORT void JNICALL
Java_com_mcal_apkeditor_MainActivity_md(JNIEnv *env, jclass clazz, jstring target,
                                                jstring source, jstring added, jint len1,
                                                jstring removed, jint len2, jstring replaced,
                                                jint len3) {
    zip_modify;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;

    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    return JNI_VERSION_1_6;
}