package com.example.gapfix;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.agora.rtc2.Constants;
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
    private View localVideoCard;
    private LinearLayout waitingStatus;
    private FloatingActionButton btnMute, btnEndCall, btnSwitchCamera;

    private boolean isMuted = false;
    private String userRole; // "Tutor" or "Student"

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                Log.d("VideoCall", "Joined Channel: " + channel);
                // In a 1-on-1 call, we can hide the "waiting" status once the local user joins
                // but we keep it for Students until the Tutor (Remote User) actually joins.
                if ("Tutor".equals(userRole)) {
                    waitingStatus.setVisibility(View.GONE);
                }
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                waitingStatus.setVisibility(View.GONE);
                setupRemoteVideo(uid);
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                if (remoteContainer != null) {
                    remoteContainer.removeAllViews();
                }
                if ("Student".equals(userRole)) {
                    waitingStatus.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public void onError(int err) {
            Log.e("VideoCall", "Agora error: " + err);
            runOnUiThread(() -> {
                if (err == 101) {
                    Toast.makeText(VideoCallActivity.this, "Invalid App ID.", Toast.LENGTH_LONG).show();
                } else if (err == 110) {
                    Toast.makeText(VideoCallActivity.this, "Token expired.", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        appId = getString(R.string.appIdAgora);
        token = getString(R.string.tokenAgora);

        channelName = getIntent().getStringExtra("TARGET_USER_ID");
        if (channelName == null) channelName = "default_room";

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userRole = prefs.getString("user_role", "Student");

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
        localVideoCard = findViewById(R.id.cv_local_video);
        waitingStatus = findViewById(R.id.ll_waiting_status);
        btnMute = findViewById(R.id.btn_mute);
        btnEndCall = findViewById(R.id.btn_end_call);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);

        btnEndCall.setOnClickListener(v -> finish());
        
        btnMute.setOnClickListener(v -> {
            if (mRtcEngine != null) toggleMute();
        });
        
        btnSwitchCamera.setOnClickListener(v -> {
            if (mRtcEngine != null) mRtcEngine.switchCamera();
        });

        // Setup role-specific UI
        if ("Student".equals(userRole)) {
            // Students now see their own camera (small) so they know it's working
            localVideoCard.setVisibility(View.VISIBLE);
            waitingStatus.setVisibility(View.VISIBLE);
        } else {
            waitingStatus.setVisibility(View.GONE);
        }
    }

    private boolean checkSelfPermission() {
        return ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
            
            // IMPORTANT: Enable video module for everyone
            mRtcEngine.enableVideo();
        } catch (Exception e) {
            Log.e("VideoCall", "Agora initialization failed", e);
            return;
        }

        // Both use BROADCASTER role so they can see each other
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

        // Everyone starts their local camera
        setupLocalVideo();

        String joinToken = (token == null || token.isEmpty() || token.equals("null")) ? null : token;
        mRtcEngine.joinChannel(joinToken, channelName, null, 0);
    }

    private void setupLocalVideo() {
        if (mRtcEngine == null) return;
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        localContainer.removeAllViews();
        localContainer.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void setupRemoteVideo(int uid) {
        if (mRtcEngine == null) return;
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        remoteContainer.removeAllViews();
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
