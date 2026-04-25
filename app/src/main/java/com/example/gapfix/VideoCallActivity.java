package com.example.gapfix;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class VideoCallActivity extends AppCompatActivity {

    private static final String TAG = "VideoCall_Debug";
    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private String appId, channelName, bookingId;
    private RtcEngine mRtcEngine;
    
    private FrameLayout localContainer, remoteContainer, localOverlayContainer;
    private CardView localVideoCard, timerCard;
    private LinearLayout layoutCallUI, layoutActiveControls;
    private FloatingActionButton btnMute, btnEndCallActive, btnSwitchCamera, btnAnswer, btnDecline;
    private TextView tvCallStatus, tvTimer;

    private boolean isMuted = false;
    private boolean isRemoteUserPresent = false;
    private boolean isCallAnswered = false;
    private CountDownTimer classTimer;
    private DatabaseReference callRef;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                if (!isRemoteUserPresent) notifyOtherPartyJoined();
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            isRemoteUserPresent = true;
            runOnUiThread(() -> enterActiveCallMode(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            isRemoteUserPresent = false;
            runOnUiThread(() -> {
                if (remoteContainer != null) remoteContainer.removeAllViews();
                endCall();
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        appId = getString(R.string.appIdAgora);
        bookingId = getIntent().getStringExtra("BOOKING_ID");
        if (bookingId == null) bookingId = "test_room";
        channelName = bookingId;

        initUI();
        
        callRef = FirebaseDatabase.getInstance().getReference("Calls").child(bookingId);
        listenForCallState();

        if (checkSelfPermission()) {
            startCallingFlow();
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
    }

    private void initUI() {
        localContainer = findViewById(R.id.local_video_view_container);
        remoteContainer = findViewById(R.id.remote_video_view_container);
        localOverlayContainer = findViewById(R.id.local_video_overlay_container);
        localVideoCard = findViewById(R.id.local_video_card);
        timerCard = findViewById(R.id.timer_card);
        layoutCallUI = findViewById(R.id.layout_call_ui);
        layoutActiveControls = findViewById(R.id.layout_active_controls);
        tvCallStatus = findViewById(R.id.tv_call_status);
        tvTimer = findViewById(R.id.tv_timer);
        btnMute = findViewById(R.id.btn_mute);
        btnEndCallActive = findViewById(R.id.btn_end_call_active);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        btnAnswer = findViewById(R.id.btn_answer);
        btnDecline = findViewById(R.id.btn_decline);

        btnMute.setOnClickListener(v -> toggleMute());
        btnEndCallActive.setOnClickListener(v -> endCall());
        btnSwitchCamera.setOnClickListener(v -> { if (mRtcEngine != null) mRtcEngine.switchCamera(); });
        btnAnswer.setOnClickListener(v -> answerCall());
        btnDecline.setOnClickListener(v -> endCall());
    }

    private void startCallingFlow() {
        boolean isIncoming = getIntent().getBooleanExtra("IS_INCOMING", false);
        initAgoraEngine();
        setupLocalVideo(false); // Full screen RINGING preview

        if (isIncoming) {
            tvCallStatus.setText("Incoming Call...");
            btnAnswer.setVisibility(View.VISIBLE);
        } else {
            tvCallStatus.setText("Ringing...");
            btnAnswer.setVisibility(View.GONE);
            joinChannel();
            updateCallState("calling");
        }
    }

    private void enterActiveCallMode(int remoteUid) {
        isCallAnswered = true;
        layoutCallUI.setVisibility(View.GONE);
        layoutActiveControls.setVisibility(View.VISIBLE);
        timerCard.setVisibility(View.VISIBLE);
        setupLocalVideo(true); // Shrink local video to overlay
        setupRemoteVideo(remoteUid);
        startClassTimer();
    }

    private void setupLocalVideo(boolean isOverlay) {
        if (mRtcEngine == null) return;
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(isOverlay);
        if (isOverlay) {
            localContainer.setVisibility(View.GONE);
            localVideoCard.setVisibility(View.VISIBLE);
            localOverlayContainer.removeAllViews();
            localOverlayContainer.addView(surfaceView);
        } else {
            localVideoCard.setVisibility(View.GONE);
            localContainer.setVisibility(View.VISIBLE);
            localContainer.removeAllViews();
            localContainer.addView(surfaceView);
        }
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void listenForCallState() {
        callRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String state = snapshot.child("state").getValue(String.class);
                if ("ended".equals(state)) finish();
                else if ("answered".equals(state) && mRtcEngine == null) joinChannel();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void answerCall() {
        updateCallState("answered");
        joinChannel();
    }

    private void updateCallState(String state) {
        Map<String, Object> map = new HashMap<>();
        map.put("state", state);
        map.put("timestamp", System.currentTimeMillis());
        callRef.updateChildren(map);
    }

    private void endCall() {
        updateCallState("ended");
        finish();
    }

    private void startClassTimer() {
        if (classTimer != null) return;
        classTimer = new CountDownTimer(30 * 60 * 1000, 1000) {
            @Override
            public void onTick(long ms) {
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", (ms/60000), (ms%60000/1000)));
            }
            @Override public void onFinish() { endCall(); }
        }.start();
    }

    private void initAgoraEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);
            mRtcEngine.enableVideo();
            mRtcEngine.startPreview();
        } catch (Exception e) { Log.e(TAG, "Agora init failed", e); }
    }

    private void joinChannel() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;
        options.publishMicrophoneTrack = true;
        options.publishCameraTrack = true;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        mRtcEngine.joinChannel(null, channelName, 0, options);
    }

    private void setupRemoteVideo(int uid) {
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

    private void notifyOtherPartyJoined() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child("Bookings").child(bookingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String targetId = FirebaseAuth.getInstance().getUid().equals(snapshot.child("studentId").getValue(String.class)) 
                                      ? snapshot.child("tutorId").getValue(String.class) : snapshot.child("studentId").getValue(String.class);
                    DatabaseReference notifRef = db.child("Notifications").child(targetId).push();
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", "Incoming Lesson Call");
                    data.put("message", "A user is calling you for a class.");
                    data.put("bookingId", bookingId);
                    data.put("isCall", true);
                    data.put("timestamp", System.currentTimeMillis());
                    notifRef.setValue(data);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean checkSelfPermission() {
        return ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (classTimer != null) classTimer.cancel();
        if (mRtcEngine != null) {
            mRtcEngine.stopPreview();
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
        }
    }
}
