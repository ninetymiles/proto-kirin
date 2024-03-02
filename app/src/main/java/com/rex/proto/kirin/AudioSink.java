package com.rex.proto.kirin;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public interface AudioSink {

    void onStart(int sampleRate, int sampleBits, int frameSize, int numChannels);
    void onData(@NonNull ByteBuffer buffer, int offset, int size, long timestamp);
    void onStop();

    class Wrapper implements AudioSink {
        protected final AudioSink mSink;
        public Wrapper(AudioSink sink) {
            mSink = sink;
        }
        @Override // AudioSink
        public void onStart(int sampleRate, int sampleBits, int frameSize, int numChannels) {
            if (mSink != null) {
                mSink.onStart(sampleRate, sampleBits, frameSize, numChannels);
            }
        }
        @Override // AudioSink
        public void onData(@NonNull ByteBuffer buffer, int offset, int size, long timestamp) {
            if (mSink != null) {
                mSink.onData(buffer, offset, size, timestamp);
            }
        }
        @Override // AudioSink
        public void onStop() {
            if (mSink != null) {
                mSink.onStop();
            }
        }
    }

    interface Factory {
        AudioSink create();
    }
}
