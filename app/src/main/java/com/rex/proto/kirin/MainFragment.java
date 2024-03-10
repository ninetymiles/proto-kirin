package com.rex.proto.kirin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.rex.proto.kirin.databinding.FragmentMainBinding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainFragment extends Fragment {

    private static final Logger sLogger = LoggerFactory.getLogger(MainFragment.class);

    private FragmentMainBinding mBinding;
    private ProtoViewModel mViewModel;
    private ProtoViewModel.State mState;
    private AudioFxRender mFxRender;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sLogger.trace("");
        mFxRender = new AudioFxRender();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        sLogger.trace("");
        mBinding = FragmentMainBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sLogger.trace("");

        mViewModel = new ViewModelProvider(this).get(ProtoViewModel.class);
        mViewModel.getState().observe(getViewLifecycleOwner(), new Observer<ProtoViewModel.State>() {
            @Override
            public void onChanged(ProtoViewModel.State state) {
                sLogger.trace("state=<{}>", state);
                mState = state;
                switch (state) {
                case START:
                    mBinding.mainButton.setEnabled(true);
                    mBinding.mainButton.setText(getString(R.string.main_button_stop));
                    break;
                case STOP:
                    mBinding.mainButton.setEnabled(true);
                    mBinding.mainButton.setText(getString(R.string.main_button_start));
                    break;
                }
                mBinding.mainStatus.setText(String.valueOf(state));
            }
        });
        mViewModel.getWavData().observe(getViewLifecycleOwner(), new Observer<float[]>() {
            @Override
            public void onChanged(float[] data) {
                //sLogger.trace("data.length=<{}>", data.length);
                if (mFxRender != null) {
                    mFxRender.updateWavData(data);
                }
            }
        });
        mViewModel.getFftData().observe(getViewLifecycleOwner(), new Observer<float[]>() {
            @Override
            public void onChanged(float[] data) {
                //sLogger.trace("data.length=<{}>", data.length);
                if (mFxRender != null) {
                    mFxRender.updateFftData(data);
                }
            }
        });

        mBinding.mainVisualizer.getHolder().addCallback(mFxRender);
        mBinding.mainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sLogger.trace("current state={}", mState);
                v.setEnabled(false);
                switch (mState) {
                case START:
                    mViewModel.handleStop();
                    break;
                case STOP:
                    mViewModel.handleStart();
                    break;
                }
            }
        });

        mState = mViewModel.getState().getValue();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sLogger.trace("");
        mBinding.mainVisualizer.getHolder().removeCallback(mFxRender);
        mBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sLogger.trace("");
        mFxRender = null;
    }
}
