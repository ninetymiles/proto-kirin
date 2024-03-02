package com.rex.proto.kirin;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

// Hold LiveData, focus on serving UI
public class ProtoViewModel extends AndroidViewModel {

    private static final Logger sLogger = LoggerFactory.getLogger(ProtoViewModel.class);

    private final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private final MutableLiveData<State> mState = new MutableLiveData<>();
    private final ProtoPlayManager mManager;

    public enum State { STARTING, START, STOPPING, STOP }

    public ProtoViewModel(Application application) {
        super(application);
        sLogger.trace("");

        mManager = ((ProtoApp) application).getPlayManager();
        mState.setValue(State.STOP);
    }

    public void handleStart() {
        sLogger.trace("");
        if (State.START.equals(mState.getValue())) {
            sLogger.warn("Already start");
            return;
        }
        submit(new Runnable() {
            @Override
            public void run() {
                sLogger.trace("onHandleStart+");
                mState.postValue(State.STARTING);
                mManager.start();
                mState.postValue(State.START);
                sLogger.trace("onHandleStart-");
            }
        });
    }

    public void handleStop() {
        sLogger.trace("");
        if (State.STOP.equals(mState.getValue())) {
            sLogger.warn("Already stop");
            return;
        }
        submit(new Runnable() {
            @Override
            public void run() {
                sLogger.trace("onHandleStop+");
                mState.postValue(State.STOPPING);
                mManager.stop();
                mState.postValue(State.STOP);
                sLogger.trace("onHandleStop-");
            }
        });
    }

    private void submit(Runnable runnable) {
        mExecutor.execute(() -> {
            try {
                runnable.run();
            } catch (Exception ex) {
                sLogger.warn("Failed to execute - {}", ex.getMessage());
            }
        });
    }

    public LiveData<State> getState() {
        return mState;
    }
}
