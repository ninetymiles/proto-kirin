package com.rex.proto.kirin;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class AudioSinkPlayer implements AudioSink {

    private static final Logger sLogger = LoggerFactory.getLogger(AudioSinkPlayer.class);

    private AudioTrack mTrack;
    private AudioSessionProvider mProvider;
    private final boolean mVoice;

    public AudioSinkPlayer() {
        this(false);
    }

    public AudioSinkPlayer(boolean voiceCall) {
        sLogger.trace("voice:{}", voiceCall);
        mVoice = voiceCall;
    }

    public AudioSinkPlayer setSessionProvider(AudioSessionProvider provider) {
        sLogger.trace("provider:{}", provider);
        mProvider = provider;
        return this;
    }

    @Override // AudioSink
    public void onStart(int sampleRate, int sampleBits, int frameSize, int numChannels) {
        sLogger.trace("sampleRate={} sampleBits={} frameSize={} numChannels={}", sampleRate, sampleBits, frameSize, numChannels);
        int channelConfig = (numChannels == 2) ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
        switch (sampleBits) {
        case 16:
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            break;
        case 32:
            audioFormat = AudioFormat.ENCODING_PCM_FLOAT;
            break;
        }

        // e.g. 48000/16/2
        // SM-G9700 min 15392, 3848 samples
        // Amlogic MBOX min 16416, 4104 samples
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        if (minBufferSize <= 0) { // ERROR or ERROR_BAD_VALUE
            int minFrameSize = Math.max(frameSize, 480);
            minBufferSize = sampleBits / Byte.SIZE * minFrameSize * numChannels * 8; // at least 8 frames, about 3840 samples for 480 frame size
        }
        int bufferSize = minBufferSize * 2;
        try {
            AudioTrack.Builder builder = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(mVoice ? AudioAttributes.USAGE_VOICE_COMMUNICATION : AudioAttributes.USAGE_MEDIA)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(audioFormat)
                            .setChannelMask(channelConfig)
                            .build())
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM);
            int sessionId = (mProvider != null) ? mProvider.get() : 0; // AudioManager.AUDIO_SESSION_ID_GENERATE
            if (sessionId > 0) {
                sLogger.debug("Player audioSessionId={}", sessionId);
                builder.setSessionId(sessionId);
            }
            mTrack = builder.build();

            sLogger.trace("audioSessionId={} bufferSizeInFrames={}",
                    mTrack.getAudioSessionId(),
                    mTrack.getBufferSizeInFrames());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API-24
                sLogger.trace("bufferCapacityInFrames={}", mTrack.getBufferCapacityInFrames());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API-29
                AudioAttributes attributes = mTrack.getAudioAttributes();
                sLogger.trace("contentType={} usage={} flags={}",
                        attributes.getContentType(),
                        attributes.getUsage(),
                        attributes.getFlags());
            }
        } catch (IllegalArgumentException ex) {
            sLogger.warn("Failed to create AudioTrack (" + sampleRate + "," + sampleBits + "," + frameSize + "," + numChannels + "," + bufferSize + ") - {}", ex.getMessage());
        }
    }

    @Override // AudioSink
    public void onData(@NonNull ByteBuffer buffer, int offset, int size, long timestamp) {
        //sLogger.trace("buffer={} offset={} size={} timestamp={}", buffer, offset, size, timestamp);
        if (size <= 0) {
            sLogger.warn("Buffer empty");
            return;
        }
        try {
            buffer.position(offset);
            mTrack.play();
            mTrack.write(buffer, size, AudioTrack.WRITE_BLOCKING);
        } catch (Exception ex) {
            sLogger.warn("Failed to play data - {}", ex.getMessage());
        }
    }

    @Override // AudioSink
    public void onStop() {
        sLogger.trace("");
        try {
            if (mTrack != null) {
                mTrack.stop();
                mTrack.release();
            }
        } catch (Exception ex) {
            sLogger.warn("Failed to stop player - {}", ex.getMessage());
        }
    }

    public int getSessionId() {
        return (mTrack != null) ? mTrack.getAudioSessionId() : 0;
    }
}
