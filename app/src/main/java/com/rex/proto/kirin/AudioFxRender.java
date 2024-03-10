package com.rex.proto.kirin;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioFxRender implements SurfaceHolder.Callback {

    private static final Logger sLogger = LoggerFactory.getLogger(AudioFxRender.class);

    private Surface mSurface;
    private RenderThread mThread;
    private boolean mDirty;
    private float[] mFftData;
    private float[] mWavData;

    private Bitmap mBgImage;
    private int mBgColor = Color.WHITE;

    public AudioFxRender() {
        sLogger.trace("");
    }

    public AudioFxRender setBackground(@ColorInt int color) {
        sLogger.trace("color={}", color);
        mBgColor = color;
        return this;
    }

    public AudioFxRender setBackground(Bitmap bitmap) {
        sLogger.trace("bitmap={} width={} height={}",
                bitmap,
                (bitmap != null) ? bitmap.getWidth() : 0,
                (bitmap != null) ? bitmap.getHeight() : 0);
        mBgImage = bitmap;
        return this;
    }

    @Override // SurfaceHolder.Callback
    synchronized public void surfaceCreated(@NonNull SurfaceHolder holder) {
        sLogger.trace("");
        mSurface = holder.getSurface();
        if (mThread == null) {
            mThread = new RenderThread();
            mThread.setName("Render");
            mThread.start();
        }
    }

    @Override // SurfaceHolder.Callback
    synchronized public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        sLogger.trace("format={} width={} height={}", format, width, height);
        mSurface = holder.getSurface();
    }

    @Override // SurfaceHolder.Callback
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        sLogger.trace("+");
        try {
            if (mThread != null) {
                mThread.interrupt();
                synchronized (AudioFxRender.this) {
                    notifyAll();
                }
                mThread.join();
                mThread = null;
            }
        } catch (InterruptedException ex) {
            sLogger.warn("interrupted - {}", ex.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted state
        }
        mSurface = null;
        sLogger.trace("-");
    }

    synchronized void updateFftData(float[] data) {
        //sLogger.trace("data={} {}", data.length, data);
        mFftData = data;
        mDirty = true;
        notifyAll();
    }

    synchronized void updateWavData(float[] data) {
        //sLogger.trace("data={} {}", data.length, data);
        mWavData = data;
        mDirty = true;
        notifyAll();
    }

    private class RenderThread extends Thread {
        final Rect mBounds = new Rect();
        final Paint mBorderPaint;
        final Paint mWavPaint;
        final Paint mFftPaint;
        public RenderThread() {
            sLogger.trace("");

            mBorderPaint = new Paint();
            mBorderPaint.setColor(Color.DKGRAY);
            mBorderPaint.setStrokeWidth(3f);

            mWavPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mWavPaint.setColor(Color.RED);
            mWavPaint.setStrokeWidth(3f);

            mFftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFftPaint.setColor(Color.BLUE);
            mFftPaint.setStrokeWidth(3f);
        }
        @Override
        public void run() {
            sLogger.trace("+");
            try {
                while (!Thread.interrupted()) {
                    synchronized (AudioFxRender.this) {
                        if (!mDirty) {
                            //sLogger.trace("wait");
                            try {
                                AudioFxRender.this.wait();
                            } catch (InterruptedException ex) {
                                sLogger.warn("interrupted - {}", ex.getMessage());
                                Thread.currentThread().interrupt(); // Restore interrupted state
                            }
                            //sLogger.trace("wakeup");
                            continue;
                        }

                        Canvas canvas;
                        try {
                            canvas = mSurface.lockHardwareCanvas();
                        } catch (Exception ex) {
                            sLogger.warn("failed to lockHardwareCanvas - {}", ex.getMessage());
                            canvas = mSurface.lockCanvas(null);
                        }
                        if (canvas == null) {
                            sLogger.warn("failed to lockCanvas");
                            return;
                        }
                        canvas.getWidth();
                        canvas.getClipBounds(mBounds);
                        //sLogger.trace("bounds={}", mBounds);

                        canvas.drawColor(mBgColor);
                        canvas.drawLine(0, 0, 0, mBounds.height(), mBorderPaint); // left
                        canvas.drawLine(0, mBounds.height(), mBounds.width(), mBounds.height(), mBorderPaint); // bottom
                        canvas.drawLine(mBounds.width(), mBounds.height(), mBounds.width(), 0, mBorderPaint); // right
                        canvas.drawLine(mBounds.width(), 0, 0, 0, mBorderPaint); // top

                        if (mBgImage != null) {
                            float ratioX = (float) mBounds.width() / mBgImage.getWidth();
                            float ratioY = (float) mBounds.height() / mBgImage.getHeight();
                            float ratio = Math.min(ratioX, ratioY);
                            int scaleWidth = (int) (mBgImage.getWidth() * ratio);
                            int scaleHeight = (int) (mBgImage.getHeight() * ratio);
                            int left = (mBounds.width() - scaleWidth) / 2;
                            int top = (mBounds.height() - scaleHeight) / 2;
                            int right = left + scaleWidth;
                            int bottom = top + scaleHeight;
                            canvas.drawBitmap(mBgImage,
                                    new Rect(0, 0, mBgImage.getWidth(), mBgImage.getHeight()),
                                    new Rect(left, top, right, bottom),
                                    null);
                        }

                        if (mFftData != null) {
                            mFftPaint.setStrokeWidth((float) mBounds.width() / mFftData.length);
                            float dc = mFftData[0];
                            float nyquist = mFftData[mFftData.length - 1];
                            for (int i = 1; i < mFftData.length - 1; i++) {
                                float startX = (float) mBounds.width() / mFftData.length * i;
                                float startY = mBounds.height();
                                float stopX  = startX;
                                float stopY  = mBounds.height() - (mBounds.height() * mFftData[i] / 128 / 2);
                                //sLogger.trace("start=({},{}) stop=({},{})", startX, startY, stopX, stopY);
                                canvas.drawLine(startX, startY, stopX, stopY, mFftPaint);
                            }
                        }

                        if (mWavData != null) {
                            for (int i = 0; i < mWavData.length - 1; i++) {
                                float startX = (float) mBounds.width() * i / (mWavData.length - 1);
                                float startY = (float) mBounds.height() / 2 - (mBounds.height() / 3f) * mWavData[i];
                                float stopX  = (float) mBounds.width() * (i + 1) / (mWavData.length - 1);
                                float stopY  = (float) mBounds.height() / 2 - (mBounds.height() / 3f) * mWavData[i + 1];
                                //sLogger.trace("start=({},{}) stop=({},{})", startX, startY, stopX, stopY);
                                canvas.drawLine(startX, startY, stopX, stopY, mWavPaint);
                            }
                        }
                        mDirty = false;
                        try {
                            mSurface.unlockCanvasAndPost(canvas);
                        } catch (Exception ex) {
                            sLogger.warn("failed to unlockCanvasAndPost - {}", ex.getMessage());
                        }
                    }
                }
            } catch (Exception ex) {
                sLogger.warn("failed to draw - {}", ex.getMessage());
            }
            sLogger.trace("-");
        }
    }
}
