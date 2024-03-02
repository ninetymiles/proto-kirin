package com.rex.proto.kirin;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtoApp extends Application {

    private static final Logger sLogger = LoggerFactory.getLogger(ProtoApp.class);

    private ProtoPlayManager mRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        sLogger.trace("{}", Integer.toHexString(hashCode()));

        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults(); // Filter logcat with tag 'StrictMode' on debug level to watch results
        }

        mRepository = new ProtoPlayManager();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        sLogger.trace("{}", Integer.toHexString(hashCode()));
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        sLogger.info("Low memory");
    }

    public ProtoPlayManager getPlayManager() {
        return mRepository;
    }

    public SharedPreferences getSharedPreferences() {
        return getSharedPreferences("ProtoCompat", Context.MODE_PRIVATE);
    }
}
