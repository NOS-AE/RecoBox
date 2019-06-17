//
// Created by NOSAE on 2019/6/10.
//

#ifndef FINALJNI_MP3ENCODER_H
#define FINALJNI_MP3ENCODER_H


#include <jni.h>

#ifdef __cplusplus
extern "C"{
#endif

JNIEXPORT jint JNICALL Java_org_fmod_recobox_util_AudioUtilNative_init
        (JNIEnv *, jobject, jstring, jint, jint, jint, jstring);

/*
 * Class:     com_wyt_myapplication_Mp3Encoder
 * Method:    encode
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_fmod_recobox_util_AudioUtilNative_encode
(JNIEnv *, jobject);

/*
 * Class:     com_wyt_myapplication_Mp3Encoder
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_fmod_recobox_util_AudioUtilNative_destroy
(JNIEnv *, jobject);


#ifdef __cplusplus
};
#endif


#endif //FINALJNI_MP3ENCODER_H


