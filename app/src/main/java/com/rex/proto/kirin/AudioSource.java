package com.rex.proto.kirin;

public interface AudioSource {

    boolean start(int sampleRate, int sampleBits, int samplePerFrame, int numChannels);
    boolean stop();

    AudioSource setOutput(AudioSink sink);

    interface Factory {
        AudioSource create();
    }
}
