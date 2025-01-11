package org.amito4.floatfpsmeter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1234;
    private Button startButton;
    private Button stopButton;
    private Button exitButton;
    private boolean isMeterRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        exitButton = findViewById(R.id.exitButton);

        isMeterRunning = getIntent().getBooleanExtra("isMeterRunning", false);
        updateButtonStates();

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

    private void startFPSService() {
        Intent intent = new Intent(this, FPSService.class);
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