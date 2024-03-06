package com.rex.proto.kirin;

import android.media.MediaRecorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
public class ProtoPlayManager {

    private static final Logger sLogger = LoggerFactory.getLogger(ProtoPlayManager.class);

    private AudioSource.Factory mSourceFactory = () -> new AudioSourceMic(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
    private AudioSink.Factory mSinkFactory = () -> new AudioSinkPlayer(true);

    private AudioSource mSource;
    private AudioSink mSink;
    private Callback mCallback;

    public interface Callback {
        void onWavData(float[] data);
        void onFftData(float[] data);
    }

    public ProtoPlayManager() {
        sLogger.trace("");
    }

    public void setSourceFactory(AudioSource.Factory factory) {
        sLogger.trace("factory:{}", factory);
        mSourceFactory = factory;
    }

    public void setSink(AudioSink.Factory factory) {
        sLogger.trace("factory:{}", factory);
        mSinkFactory = factory;
    }

    public void setCallback(Callback cb) {
        sLogger.trace("cb:{}", cb);
        mCallback = cb;
    }

    public boolean start() {
        sLogger.trace("");
        mSource = (mSourceFactory != null) ? mSourceFactory.create() : null;
        mSink = (mSinkFactory != null) ? mSinkFactory.create() : null;
        if (mSink instanceof AudioSinkPlayer) {
            AudioSinkPlayer sinkPlayer = (AudioSinkPlayer) mSink;
            AudioSinkVisualizer visualizer = new AudioSinkVisualizer(mSink)
                    .setSessionProvider(() -> sinkPlayer.getSessionId())
                    .setCallback((fmt, data) -> {
                        sLogger.trace("fmt={} data.length={}", fmt, data.length);
                        if (mCallback != null) {
                            switch (fmt) {
                            case AudioSinkVisualizer.Callback.WAV: mCallback.onWavData(data); break;
                            case AudioSinkVisualizer.Callback.FFT: mCallback.onFftData(data); break;
                            }
                        }
                    });
            mSink = visualizer;
        }
        if (mSource != null) {
            mSource.setOutput(mSink);
            mSource.start(48000, 16, 480, 2);
        }
        return true;
    }

    public boolean stop() {
        sLogger.trace("");
        if (mSource != null) {
            mSource.stop();
        }
        mSink = null;
        return true;
    }
}
