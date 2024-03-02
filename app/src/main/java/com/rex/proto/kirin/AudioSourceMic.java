package com.rex.proto.kirin;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;

import androidx.annotation.RequiresPermission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AudioSourceMic implements AudioSource {

    private static final Logger sLogger = LoggerFactory.getLogger(AudioSourceMic.class);

    private final int mSource;
    private final List<AudioEffect> mEffectList = new ArrayList<>();
    private AudioSink mOutput;
    private AudioRecord mRecorder;
    private Thread mThread;

    public AudioSourceMic(int source) {
        sLogger.trace("source={}", source);
        mSource = source;
        try {
            AudioEffect.Descriptor[] descriptors = AudioEffect.queryEffects();
            sLogger.debug("AudioEffects={}", (descriptors != null) ? descriptors.length : 0);
            for (int i = 0; descriptors != null && i < descriptors.length; i++) {
                sLogger.debug("AudioEffects[" + i + "]=" + descriptors[i].name);
            }
        } catch (Exception ex) {
            sLogger.warn("Failed to query audio effects - {}", ex.getMessage());
        }
    }

    @Override // AudioSource
    public AudioSourceMic setOutput(AudioSink sink) {
        sLogger.trace("sink={}", sink);
        mOutput = sink;
        return this;
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Override // AudioSource
    public boolean start(int sampleRate, int sampleBits, int samplePerFrame, int numChannels) throws IllegalArgumentException, UnsupportedOperationException {
        sLogger.trace("sampleRate:{} sampleDepth:{} samplePerFrame:{} channelCount:{}", sampleRate, sampleBits, samplePerFrame, numChannels);
        int channelConfig = (numChannels == 2) ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_DEFAULT;
        switch (sampleBits) {
        case 8:
            audioFormat = AudioFormat.ENCODING_PCM_8BIT;
            break;
        case 16:
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            break;
        case 32:
            audioFormat = AudioFormat.ENCODING_PCM_FLOAT;
            break;
        }
        int minBuffSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        try {
            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build();
            mRecorder = new AudioRecord.Builder()
                    .setAudioSource(mSource)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(2 * minBuffSize)
                    .build();
            sLogger.trace("audioSessionId={} bufferSizeInFrames={}",
                    mRecorder.getAudioSessionId(),
                    mRecorder.getBufferSizeInFrames());
        } catch (UnsupportedOperationException ex) {
            sLogger.warn("Failed to build AudioRecord - {}", ex.getMessage());
        }

        if (AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler aec = AcousticEchoCanceler.create(mRecorder.getAudioSessionId());
            if (aec != null) {
                if (!aec.getEnabled()) {
                    int result = aec.setEnabled(true);
                    sLogger.info("Recorder AEC enable {}", (result == AudioEffect.SUCCESS) ? "success" : ("failed (" + result + ")"));
                } else {
                    sLogger.info("Recorder AEC enable by default");
                }
                mEffectList.add(aec);
            }
        } else {
            sLogger.warn("Recorder AEC not available");
        }

        if (AutomaticGainControl.isAvailable()) {
            AutomaticGainControl agc = AutomaticGainControl.create(mRecorder.getAudioSessionId());
            if (agc != null) {
                if (!agc.getEnabled()) {
                    int result = agc.setEnabled(true);
                    sLogger.info("Recorder AGC enable {}", (result == AudioEffect.SUCCESS) ? "success" : ("failed (" + result + ")"));
                } else {
                    sLogger.info("Recorder AGC enable by default");
                }
                mEffectList.add(agc);
            }
        } else {
            sLogger.warn("Recorder AGC not available");
        }

        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor ans = NoiseSuppressor.create(mRecorder.getAudioSessionId());
            if (ans != null) {
                if (!ans.getEnabled()) {
                    int result = ans.setEnabled(true);
                    sLogger.info("Recorder ANS enable {}", (result == AudioEffect.SUCCESS) ? "success" : ("failed (" + result + ")"));
                } else {
                    sLogger.info("Recorder ANS enable by default");
                }
                mEffectList.add(ans);
            }
        } else {
            sLogger.warn("Recorder ANS not available");
        }

        try {
            mRecorder.startRecording();
        } catch (IllegalStateException ex) {
            sLogger.warn("Failed to start recording - {}", ex.getMessage());
            return false;
        }

        int frameSize = samplePerFrame * numChannels * sampleBits / Byte.SIZE; // Buffer size in bytes
        sLogger.debug("minBuffSize:{} frameSize:{}", minBuffSize, frameSize);
        if (mOutput != null) {
            mOutput.onStart(sampleRate, sampleBits, frameSize, numChannels);
        }

        mThread = new AudioRecThread(frameSize);
        mThread.start();
        return (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING);
    }

    @Override // AudioSource
    public boolean stop() {
        sLogger.trace("");
        try {
            mRecorder.stop();
        } catch (IllegalStateException ex) {
            sLogger.warn("Failed to stop recording - {}", ex.getMessage());
        }

        if (mThread != null) {
            mThread.interrupt();
            try {
                mThread.join();
            } catch (InterruptedException ex) {
                sLogger.warn("Failed to join thread - {}", ex.getMessage());
            }
            mThread = null;
        }
        if (mOutput != null) {
            mOutput.onStop();
        }

        for (AudioEffect effect : mEffectList) {
            effect.release();
        }
        mEffectList.clear();

        mRecorder.release();
        mRecorder = null;
        return true;
    }

    public int getSessionId() {
        return (mRecorder != null) ? mRecorder.getAudioSessionId() : 0;
    }

    private class AudioRecThread extends Thread {
        private final int mFrameSize;
        public AudioRecThread(int frameSize) {
            super("AudioRec");
            mFrameSize = frameSize;
        }
        @Override
        public void run() {
            sLogger.debug("+");
            ByteBuffer buffer = ByteBuffer.allocateDirect(mFrameSize);
            try {
                while (!isInterrupted()) {
                    int size = mRecorder.read(buffer, mFrameSize);
                    if (size < 0) { // Failed
                        sLogger.warn("Failed to read from recorder - {}", size);
                        break;
                    } else if (size > 0) { // Success with valid data
                        long timestamp;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            AudioTimestamp ts = new AudioTimestamp();
                            mRecorder.getTimestamp(ts, AudioTimestamp.TIMEBASE_MONOTONIC);
                            timestamp = Math.round(ts.nanoTime / 1000.0);
                        } else {
                            timestamp = Math.round(System.nanoTime() / 1000.0);
                        }
                        //sLogger.trace("size:{} buffer:{} timestamp:{}", size, buffer, timestamp);
                        if (mOutput != null) {
                            mOutput.onData(buffer, 0, size, timestamp);
                        }
                    }
                    // size == 0, succeed but no data, continue read
                }
            } catch (Exception ex) {
                sLogger.warn("Failed to record audio - {}", ex.getMessage());
            }
            sLogger.debug("-");
        }
    }
}
