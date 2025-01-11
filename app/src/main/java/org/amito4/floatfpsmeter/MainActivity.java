package org.amito4.floatfpsmeter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1234;
    private Button startButton;
    private Button stopButton;
    private Button exitButton;
    private boolean isMeterRunning = false;
    private RadioGroup fpsRadioGroup;
    private int currentFps = 60; // Default FPS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        exitButton = findViewById(R.id.exitButton);

        isMeterRunning = getIntent().getBooleanExtra("isMeterRunning", false);
        updateButtonStates();

        fpsRadioGroup = findViewById(R.id.fpsRadioGroup);
        setupFpsControls();

        setupButtons();
    }

    private void setupButtons() {
        startButton.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
            } else {
                startFPSService();
            }
        });

        stopButton.setOnClickListener(v -> {
            stopFPSService();
        });

        exitButton.setOnClickListener(v -> {
            exitApp();
        });
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName())
        );
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startFPSService();
            }
        }
    }

    private void setupFpsControls() {
        fpsRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.fpsNone) {
                setSystemFps(0);  // 0 indicates no target FPS
            } else if (checkedId == R.id.fps60) {
                setSystemFps(60);
            } else if (checkedId == R.id.fps120) {
                setSystemFps(120);
            } else if (checkedId == R.id.fps144) {
                setSystemFps(144);
            }
        });
    }

    private void setSystemFps(int fps) {
        currentFps = fps;
        // If the service is running, update it
        if (isMeterRunning) {
            Intent intent = new Intent(this, FPSService.class);
            intent.setAction("UPDATE_FPS");
            intent.putExtra("targetFps", fps);
            startService(intent);
        }
    }

    private void startFPSService() {
        Intent intent = new Intent(this, FPSService.class);
        intent.putExtra("targetFps", currentFps);
        startService(intent);
        isMeterRunning = true;
        updateButtonStates();
    }

    private void stopFPSService() {
        Intent intent = new Intent(this, FPSService.class);
        stopService(intent);
        isMeterRunning = false;
        updateButtonStates();
    }

    private void exitApp() {
        stopFPSService();
        finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    private void updateButtonStates() {
        startButton.setEnabled(!isMeterRunning);
        stopButton.setEnabled(isMeterRunning);
    }
}