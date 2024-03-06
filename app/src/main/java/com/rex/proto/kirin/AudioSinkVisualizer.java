package com.rex.proto.kirin;

import android.media.audiofx.Visualizer;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Analyze PCM audio data, make the spectrum visual
 * Ref: <a href="https://developer.android.com/reference/android/media/audiofx/Visualizer?hl=en">...</a>
 */
public class AudioSinkVisualizer extends AudioSink.Wrapper {

    private static final Logger sLogger = LoggerFactory.getLogger(AudioSinkVisualizer.class);

    // Google Pixel4a max 20000
    // Samsung Galaxy Note10 (SM-N9700) max 20000
    private static final int DEFAULT_CAPTURE_RATE = 20000;

    private Visualizer mVisualizer;
    private AudioSessionProvider mProvider;
    private Callback mCallback;

    public interface Callback {
        int WAV = 1;
        int FFT = 2;
        void onData(int fmt, float[] data);
    }

    public AudioSinkVisualizer(AudioSink sink) {
        super(sink);
        sLogger.trace("sink:{}", sink);
    }

    public AudioSinkVisualizer setSessionProvider(AudioSessionProvider provider) {
        sLogger.trace("provider:{}", provider);
        mProvider = provider;
        return this;
    }

    public AudioSinkVisualizer setCallback(Callback callback) {
        sLogger.trace("callback:{}", callback);
        mCallback = callback;
        return this;
    }

    @Override // AudioSink
    public void onStart(int sampleRate, int sampleBits, int frameSize, int numChannels) {
        super.onStart(sampleRate, sampleBits, frameSize, numChannels);
        sLogger.trace("sampleRate:{} sampleBits:{} frameSize:{} numChannels:{}", sampleRate, sampleBits, frameSize, numChannels);

        int[] range = Visualizer.getCaptureSizeRange(); // [128, 1024]
        int size = range[0];
        sLogger.debug("Visualizer captureSize value:{} min:{} max:{}", size, range[0], range[1]);

        int rate = Math.min(DEFAULT_CAPTURE_RATE, Visualizer.getMaxCaptureRate()); // max 20000
        sLogger.debug("Visualizer captureRate value:{} max:{}", rate, Visualizer.getMaxCaptureRate());

        // The use of the visualizer requires the permission RECORD_AUDIO.
        // Creating a Visualizer on the output mix (audio session 0) requires permission MODIFY_AUDIO_SETTINGS
        int sessionId = (mProvider != null) ? mProvider.get() : 0;
        sLogger.debug("Visualizer audioSessionId={}", sessionId);
        try {
            mVisualizer = new Visualizer(sessionId);
            mVisualizer.setCaptureSize(size);
            mVisualizer.setDataCaptureListener(mListener, rate, true, true);
            mVisualizer.setEnabled(true);
        } catch (Exception ex) {
            sLogger.warn("Failed to initialize visualizer - {}", ex.getMessage());
        }
    }

    @Override
    public void onData(@NonNull ByteBuffer buffer, int offset, int size, long timestamp) {
        super.onData(buffer, offset, size, timestamp);
        //sLogger.trace("buffer:{} offset:{} size:{} timestamp:{}", buffer, offset, size, timestamp);
    }

    @Override // AudioSink
    public void onStop() {
        super.onStop();
        sLogger.trace("");

        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mVisualizer.release();
        }
    }

    // https://developer.android.com/reference/android/media/audiofx/Visualizer?hl=en#getFft(byte[])
    private final Visualizer.OnDataCaptureListener mListener = new Visualizer.OnDataCaptureListener() {
        @Override
        public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            //sLogger.trace("visualizer:{} waveform.length:{} samplingRate:{}", visualizer.hashCode(), waveform.length, samplingRate);
            //sLogger.trace("{}", StringUtils.byteArrayToHexString(waveform));
            int n = waveform.length;
            float[] wave = new float[n];
            for (int i = 0; i < n; i++) {
                wave[i] = (float) (waveform[i] + 128) / 0xFF; // Normalize to [0,1]
            }
            //sLogger.trace("wave:{} {}", wave.length, wave);

            if (mCallback != null) {
                mCallback.onData(Callback.WAV, wave);
            }
        }
        @Override
        public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
            //sLogger.trace("visualizer:{} fft.length:{} samplingRate:{}", visualizer.hashCode(), fft.length, samplingRate);
            //sLogger.trace("{}", StringUtils.byteArrayToHexString(fft));
            int n = fft.length;
            float[] magnitudes = new float[n / 2 + 1];
            //float[] phases = new float[n / 2 + 1];
            magnitudes[0] = (float) Math.abs(fft[0]);      // DC
            magnitudes[n / 2] = (float) Math.abs(fft[1]);  // Nyquist
            for (int k = 1; k < n / 2; k++) {
                int i = k * 2;
                magnitudes[k] = (float) Math.hypot(fft[i], fft[i + 1]);
                //phases[k] = (float) Math.atan2(fft[i + 1], fft[i]);
            }
            //sLogger.trace("magnitudes:{} {}", magnitudes.length, magnitudes);
            //sLogger.trace("phases:{}", phases);

            if (mCallback != null) {
                mCallback.onData(Callback.FFT, magnitudes);
            }
        }
    };
}