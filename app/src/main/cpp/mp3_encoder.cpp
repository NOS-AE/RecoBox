//
// Created by NOSAE on 2019/6/10.
//

#include "mp3_encoder.h"
extern "C"

Mp3Encoder::Mp3Encoder()=default;

Mp3Encoder::~Mp3Encoder()=default;

int Mp3Encoder::lint(const char *pcmFilePath,
                     const char *mp3FilePath,
                     int sampleRate,
                     int channels,
                     int bitRate) {
    int ret = 1;
    pcmFIle = fopen(pcmFilePath, "rb");
    if (pcmFIle) {
        mp3File = fopen(mp3FilePath, "wb");
        if (mp3File) {
            //初始化lame相关参数，输入/输出采样率、音频声道数、码率
            lameClient = lame_init();
            lame_set_in_samplerate(lameClient, sampleRate);
            lame_set_out_samplerate(lameClient, sampleRate);
            lame_set_num_channels(lameClient, channels);
            lame_set_brate(lameClient, 128);
            lame_init_params(lameClient);
            ret = 0;
        }
    }
    return ret;
}

void Mp3Encoder::Encode() {
    int bufferSize = 1024 * 256;
    auto *buffer = new short[bufferSize / 2];
    auto *leftChannelBuffer = new short[bufferSize / 4];//左声道
    auto *rightChannelBuffer = new short[bufferSize / 4];//右声道
    auto *mp3_buffer = new unsigned char[bufferSize];
    size_t readBufferSize = 0;
    while ((readBufferSize = fread(buffer, 2, size_t (bufferSize / 2), pcmFIle)) > 0) {
        for (int i = 0; i < readBufferSize; i++) {
            if (i % 2 == 0) {
                leftChannelBuffer[i / 2] = buffer[i];
            } else {
                rightChannelBuffer[i / 2] = buffer[i];
            }
        }
        auto writeSize = (size_t)lame_encode_buffer(
                lameClient,
                leftChannelBuffer,
                rightChannelBuffer,
                (int) (readBufferSize / 2),
                mp3_buffer,
                bufferSize);
        fwrite(mp3_buffer, 1, writeSize, mp3File);
    }
    delete [] buffer;
    delete [] leftChannelBuffer;
    delete [] rightChannelBuffer;
    delete [] mp3_buffer;
}

void Mp3Encoder::Destory() {
    if (pcmFIle){
        fclose(pcmFIle);
    }
    if (mp3File){
        fclose(mp3File);
        lame_close(lameClient);
    }
}