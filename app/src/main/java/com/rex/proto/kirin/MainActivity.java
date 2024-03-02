package com.rex.proto.kirin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.rex.proto.kirin.databinding.ActivityMainBinding;
import com.rex.proto.kirin.preference.PreferenceViewActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final Logger sLogger = LoggerFactory.getLogger(MainActivity.class);

    private ActivityMainBinding mBinding;
    private ActivityResultLauncher<String[]> mPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sLogger.trace("");

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.toolbar);

        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            sLogger.debug("Receive permission results");

            for (String perm : result.keySet()) {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, perm);
                sLogger.trace("permission:<{}> granted:{} showRationale:{}", perm, result.get(perm), showRationale);
                if (Boolean.TRUE.equals(result.get(perm))) continue;
                if (!showRationale) continue;

                String message = getString(R.string.toast_permission_required);
                if (Manifest.permission.RECORD_AUDIO.equalsIgnoreCase(perm)) {
                    message = getString(R.string.toast_record_audio_required);
                }
                Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                sLogger.trace("requesting <{}>", perm);
                                mPermissionLauncher.launch(new String[] { perm });
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sLogger.trace("");
    }

    @Override
    protected void onStart() {
        super.onStart();
        sLogger.trace("");

        List<String> allList = new ArrayList<>();
        allList.add(Manifest.permission.RECORD_AUDIO);

        List<String> requestList = new ArrayList<>();
        for (String perm : allList) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                sLogger.debug("<{}> not granted", perm);
                requestList.add(perm);
            } else {
                sLogger.debug("<{}> already granted", perm);
            }
        }
        if (!requestList.isEmpty()) {
            mPermissionLauncher.launch(requestList.toArray(new String[0]));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sLogger.trace("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, PreferenceViewActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}