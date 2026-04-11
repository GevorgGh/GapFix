package com.example.gapfix;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class VideoCallActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private String appId;
    private String channelName = "test_channel";
    private String token;

    private RtcEngine mRtcEngine;
    private FrameLayout localContainer;
    private FrameLayout remoteContainer;
    private FloatingActionButton btnMute, btnEndCall, btnSwitchCamera;

    private boolean isMuted = false;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> Log.d("VideoCall", "Joined Channel: " + channel));
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> setupRemoteVideo(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                if (remoteContainer != null) {
                    remoteContainer.removeAllViews();
                }
            });
        }

        @Override
        public void onError(int err) {
            Log.e("VideoCall", "Agora error: " + err);
            runOnUiThread(() -> {
                if (err == 101) {
                    Toast.makeText(VideoCallActivity.this, "Invalid App ID. Please check your Agora config.", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        // Correct way to get string resources
        appId = getString(R.string.appIdAgora);
        token = getString(R.string.tokenAgora);

        channelName = getIntent().getStringExtra("TARGET_USER_ID");
        if (channelName == null) channelName = "default_room";

        initUI();

        if (checkSelfPermission()) {
            initAgoraEngineAndJoinChannel();
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
    }

    private void initUI() {
        localContainer = findViewById(R.id.local_video_view_container);
        remoteContainer = findViewById(R.id.remote_video_view_container);
        btnMute = findViewById(R.id.btn_mute);
        btnEndCall = findViewById(R.id.btn_end_call);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);

        btnEndCall.setOnClickListener(v -> finish());
        
        btnMute.setOnClickListener(v -> {
            if (mRtcEngine != null) {
                toggleMute();
            }
        });
        
        btnSwitchCamera.setOnClickListener(v -> {
            if (mRtcEngine != null) {
                mRtcEngine.switchCamera();
            }
        });
    }

    private boolean checkSelfPermission() {
        return ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                initAgoraEngineAndJoinChannel();
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initAgoraEngineAndJoinChannel() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            Log.e("VideoCall", "Agora initialization failed", e);
            return;
        }

        setupLocalVideo();
        // If your project doesn't use a token, you can pass null here.
        // But since you have a tokenAgora string, we use it.
        mRtcEngine.joinChannel(token, channelName, null, 0);
    }

    private void setupLocalVideo() {
        if (mRtcEngine == null) return;
        mRtcEngine.enableVideo();
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        localContainer.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void setupRemoteVideo(int uid) {
        if (mRtcEngine == null) return;
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        remoteContainer.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private void toggleMute() {
        isMuted = !isMuted;
        mRtcEngine.muteLocalAudioStream(isMuted);
        btnMute.setImageResource(isMuted ? R.drawable.baseline_mic_off_24 : R.drawable.baseline_mic_24);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }
    }
}
