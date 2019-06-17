//
// Created by NOSAE on 2019/6/10.
//

#include "Mp3Encoder.h"
#include "mp3_encoder.h"

Mp3Encoder *encoder = nullptr;

extern "C"
JNIEXPORT jint JNICALL Java_org_fmod_recobox_util_AudioUtilNative_init(
        JNIEnv *env,
        jobject,
        jstring pcmPathParam,
        jint audioChannelsParam,
        jint bitRateParam,
        jint sampleRateParam,
        jstring mp3PathParam
        ){
    const char* pcmPath = env->GetStringUTFChars(pcmPathParam,nullptr);
    const char* mp3Path = env->GetStringUTFChars(mp3PathParam, nullptr);
    encoder = new Mp3Encoder();
    int ret = encoder->lint(pcmPath,
                            mp3Path,
                            sampleRateParam,
                            audioChannelsParam,
                            bitRateParam);
    env->ReleaseStringUTFChars(mp3PathParam, mp3Path);
    env->ReleaseStringUTFChars(pcmPathParam, pcmPath);
    return ret;
}

extern "C"
JNIEXPORT void JNICALL Java_org_fmod_recobox_util_AudioUtilNative_encode
        (JNIEnv *, jobject){
    encoder->Encode();
}

extern "C"
JNIEXPORT void JNICALL Java_org_fmod_recobox_util_AudioUtilNative_destroy
        (JNIEnv *, jobject){
    encoder->Destory();
}