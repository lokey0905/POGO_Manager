/*
 * The native Magisk detection portions of this file are derived from DetectMagiskHide:
 * https://github.com/darvincisec/DetectMagiskHide
 * Compared upstream source:
 * https://github.com/darvincisec/DetectMagiskHide/blob/dfecbb5ef6c92916bd5e96f22251e07362d47be9/app/src/main/c/native-lib.c
 * Copyright [2020] [Darvin]
 *
 * Modified for POGO Manager: adapted the JNI package name and declaration,
 * and added integration comments. The exact original import revision is unknown.
 * The following license applies to the upstream-derived portions.
 * See repository-root LICENSES/Apache-2.0.txt and THIRD_PARTY_NOTICES.md.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <unistd.h>
#include <stdio.h>
#include <malloc.h>
#include <android/log.h>
#include <stdbool.h>
#include <fcntl.h>
#include <string.h>

static inline bool is_mountpaths_detected();
static inline bool is_supath_detected();

static const char *TAG = "DetectMagiskNative";

static char *blacklistedMountPaths[] = {
        "magisk",
        "core/mirror",
        "core/img"
};

static const char *suPaths[] = {
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/bin/su",
        "/system/bin/.ext/su",
        "/system/bin/failsafe/su",
        "/system/sd/xbin/su",
        "/system/usr/we-need-root/su",
        "/system/xbin/su",
        "/cache/su",
        "/data/su",
        "/dev/su"
};

JNIEXPORT jboolean JNICALL
Java_app_lokey0905_location_Native_isMagiskPresentNative(JNIEnv *env,
 //                                                        jclass clazz) {
    // TODO: implement isMagiskPresentNative()
//}
//JNIEXPORT jboolean
//Java_app_lokey0905_location_Native_isMagiskPresentNativ(
//        JNIEnv *env,
        jobject this) {
    bool bRet = false;
    do {
        bRet = is_supath_detected();
        if (bRet)
            break;
        bRet = is_mountpaths_detected();
        if (bRet)
            break;
    } while (false);

    if(bRet)
        return JNI_TRUE;
    else
        return JNI_FALSE;
}

__attribute__((always_inline))
static inline bool is_mountpaths_detected() {
    int len = sizeof(blacklistedMountPaths) / sizeof(blacklistedMountPaths[0]);

    bool bRet = false;

    FILE *fp = fopen("/proc/self/mounts", "r");
    if (fp == NULL)
        goto exit;

    fseek(fp, 0L, SEEK_END);
    long size = ftell(fp);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Opening Mount file size: %ld", size);
    /* For some reason size comes as zero */
    if (size == 0)
        size = 20000;  /*This will differ for different devices */
    char *buffer = calloc(size, sizeof(char));
    if (buffer == NULL)
        goto exit;

    size_t read = fread(buffer, 1, size, fp);
    int count = 0;
    for (int i = 0; i < len; i++) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Checking Mount Path  :%s", blacklistedMountPaths[i]);
        char *rem = strstr(buffer, blacklistedMountPaths[i]);
        if (rem != NULL) {
            count++;
            __android_log_print(ANDROID_LOG_INFO, TAG, "Found Mount Path :%s", blacklistedMountPaths[i]);
            break;
        }
    }
    if (count > 0)
        bRet = true;

    exit:

    if (buffer != NULL)
        free(buffer);
    if (fp != NULL)
        fclose(fp);

    return bRet;
}


__attribute__((always_inline))
static inline bool is_supath_detected() {
    int len = sizeof(suPaths) / sizeof(suPaths[0]);

    bool bRet = false;
    for (int i = 0; i < len; i++) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Checking SU Path  :%s", suPaths[i]);
        if (open(suPaths[i], O_RDONLY) >= 0) {
            __android_log_print(ANDROID_LOG_INFO, TAG, "Found SU Path :%s", suPaths[i]);
            bRet = true;
            break;
        }
        if (0 == access(suPaths[i], R_OK)) {
            __android_log_print(ANDROID_LOG_INFO, TAG, "Found SU Path :%s", suPaths[i]);
            bRet = true;
            break;
        }
    }

    return bRet;
}

