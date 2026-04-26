package com.example.gapfix;

import android.Manifest;
import android.content.Intent;
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

    private String appId, channelName, bookingId, tutorId;
    private RtcEngine mRtcEngine;

    private FrameLayout localContainer, remoteContainer;
    private CardView localVideoCard, timerCard;
    private LinearLayout layoutCalling, layoutControls;
    private FloatingActionButton btnMute, btnHangUp, btnFinishLesson, btnSwitchCamera;
    private FloatingActionButton btnAnswer, btnDecline;
    private TextView tvCallerName, tvTimer;

    private boolean isMuted = false;
    private boolean isTimerFinished = false;
    private boolean isRemoteUserJoined = false;
    private CountDownTimer classTimer;
    private DatabaseReference callRef;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                Log.d(TAG, "Joined Channel: " + channel);
                if (!getIntent().getBooleanExtra("IS_INCOMING", false)) {
                    notifyOtherParty();
                }
                startClassTimer();
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                Log.d(TAG, "Remote User Joined: " + uid);
                isRemoteUserJoined = true;
                setupRemoteVideo(uid);
                enterActiveCallMode();
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                Log.d(TAG, "Remote User Offline: " + uid);
                Toast.makeText(VideoCallActivity.this, "Partner disconnected. Waiting...", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onError(int err) { Log.e(TAG, "Agora Error: " + err); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        appId = getString(R.string.appIdAgora);
        bookingId = intent.getStringExtra("BOOKING_ID");
        if (bookingId == null) bookingId = "test_room";
        channelName = bookingId;

        initUI();
        callRef = FirebaseDatabase.getInstance().getReference("Calls").child(bookingId);
        listenForCallState();
        fetchTutorIdAndStatus();

        if (checkSelfPermission()) {
            startCallingFlow();
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
    }

    private void fetchTutorIdAndStatus() {
        FirebaseDatabase.getInstance().getReference("Bookings").child(bookingId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tutorId = snapshot.child("tutorId").getValue(String.class);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initUI() {
        localContainer = findViewById(R.id.local_video_view_container);
        remoteContainer = findViewById(R.id.remote_video_view_container);
        localVideoCard = findViewById(R.id.local_video_card);
        timerCard = findViewById(R.id.timer_card);
        layoutCalling = findViewById(R.id.layout_calling);
        layoutControls = findViewById(R.id.layout_controls);
        
        btnMute = findViewById(R.id.btn_mute);
        btnHangUp = findViewById(R.id.btn_hang_up);
        btnFinishLesson = findViewById(R.id.btn_finish_lesson);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        
        btnAnswer = findViewById(R.id.btn_answer);
        btnDecline = findViewById(R.id.btn_decline);
        tvCallerName = findViewById(R.id.tv_caller_name);
        tvTimer = findViewById(R.id.tv_timer);

        // Reset visibility
        localVideoCard.setVisibility(View.GONE);
        timerCard.setVisibility(View.GONE);
        layoutControls.setVisibility(View.GONE);
        layoutCalling.setVisibility(View.GONE);

        btnHangUp.setOnClickListener(v -> hangUp());
        btnFinishLesson.setOnClickListener(v -> finishLessonAction());
        btnMute.setOnClickListener(v -> toggleMute());
        btnSwitchCamera.setOnClickListener(v -> { if (mRtcEngine != null) mRtcEngine.switchCamera(); });
        btnAnswer.setOnClickListener(v -> answerCall());
        btnDecline.setOnClickListener(v -> hangUp());
    }

    private void startCallingFlow() {
        boolean isIncoming = getIntent().getBooleanExtra("IS_INCOMING", false);
        initAgoraEngine();
        
        if (isIncoming) {
            layoutCalling.setVisibility(View.VISIBLE);
            layoutControls.setVisibility(View.GONE);
            tvCallerName.setText("Incoming Lesson Call...");
            setupLocalVideo(false);
        } else {
            layoutCalling.setVisibility(View.GONE);
            layoutControls.setVisibility(View.VISIBLE);
            updateCallState("calling");
            joinChannel();
            setupLocalVideo(false);
        }
    }

    private void initAgoraEngine() {
        if (mRtcEngine != null) return;
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = this;
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);
            mRtcEngine.enableVideo();
            mRtcEngine.startPreview();
        } catch (Exception e) { Log.e(TAG, "Agora Initialization Failed", e); }
    }

    private void setupLocalVideo(boolean isOverlay) {
        if (mRtcEngine == null) return;
        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.setZOrderMediaOverlay(isOverlay);
        if (isOverlay) {
            localContainer.removeAllViews();
            localContainer.addView(surfaceView);
            localVideoCard.setVisibility(View.VISIBLE);
        } else {
            remoteContainer.removeAllViews();
            remoteContainer.addView(surfaceView);
        }
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void setupRemoteVideo(int uid) {
        SurfaceView surfaceView = new SurfaceView(this);
        remoteContainer.removeAllViews();
        remoteContainer.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private void joinChannel() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;
        options.publishMicrophoneTrack = true;
        options.publishCameraTrack = true;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        String joinToken = null;
        int resId = getResources().getIdentifier("tokenAgora", "string", getPackageName());
        if (resId != 0) joinToken = getString(resId);
        mRtcEngine.joinChannel(joinToken, channelName, 0, options);
    }

    private void enterActiveCallMode() {
        layoutCalling.setVisibility(View.GONE);
        layoutControls.setVisibility(View.VISIBLE);
        timerCard.setVisibility(View.VISIBLE);
        setupLocalVideo(true);
    }

    private void answerCall() {
        layoutCalling.setVisibility(View.GONE);
        layoutControls.setVisibility(View.VISIBLE);
        updateCallState("answered");
        joinChannel();
    }

    private void toggleMute() {
        if (mRtcEngine == null) return;
        isMuted = !isMuted;
        mRtcEngine.muteLocalAudioStream(isMuted);
        btnMute.setImageResource(isMuted ? R.drawable.baseline_mic_off_24 : R.drawable.baseline_mic_24);
    }

    private void hangUp() {
        if (isTimerFinished) {
            finalizeCall();
        } else {
            updateCallState("offline");
            finish();
        }
    }

    private void finishLessonAction() {
        String uid = FirebaseAuth.getInstance().getUid();
        callRef.child("finishRequests").child(uid).setValue(true);
        Toast.makeText(this, "Finish request sent. Waiting for partner...", Toast.LENGTH_SHORT).show();
    }

    private void finalizeCall() {
        if (isRemoteUserJoined) {
            updateCallState("ended");
            updateBookingStatus();
            checkIfStudentAndOpenReview();
        } else {
            updateCallState("cancelled");
            cancelBooking();
        }
    }

    private void updateBookingStatus() {
        if (bookingId == null || bookingId.equals("test_room")) return;
        DatabaseReference bookingRef = FirebaseDatabase.getInstance().getReference("Bookings").child(bookingId);
        bookingRef.child("status").setValue("finished");
        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isFree = snapshot.child("isFree").getValue(Boolean.class);
                    String studentId = snapshot.child("studentId").getValue(String.class);
                    if (isFree != null && isFree && studentId != null && tutorId != null) {
                        FirebaseDatabase.getInstance().getReference("FreeLessonsUsed").child(studentId).child(tutorId).setValue(true);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cancelBooking() {
        if (bookingId == null || bookingId.equals("test_room")) {
            finish();
            return;
        }
        FirebaseDatabase.getInstance().getReference("Bookings").child(bookingId)
                .child("status").setValue("cancelled");
        finish();
    }

    private void checkIfStudentAndOpenReview() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        FirebaseDatabase.getInstance().getReference("Users").child("Student").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Intent intent = new Intent(VideoCallActivity.this, ReviewWritingActivity.class);
                    intent.putExtra("BOOKING_ID", bookingId);
                    intent.putExtra("TUTOR_ID", tutorId);
                    startActivity(intent);
                }
                finish();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { finish(); }
        });
    }

    private void updateCallState(String state) {
        Map<String, Object> map = new HashMap<>();
        map.put("state", state);
        map.put("callerId", FirebaseAuth.getInstance().getUid());
        callRef.updateChildren(map);
    }

    private void listenForCallState() {
        callRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String state = snapshot.child("state").getValue(String.class);
                if ("ended".equals(state)) {
                    isTimerFinished = true;
                    updateBookingStatus();
                    checkIfStudentAndOpenReview();
                } else if ("cancelled".equals(state)) {
                    cancelBooking();
                } else if ("offline".equals(state)) {
                    Toast.makeText(VideoCallActivity.this, "Partner left the call", Toast.LENGTH_SHORT).show();
                }

                DataSnapshot finishReqs = snapshot.child("finishRequests");
                if (finishReqs.getChildrenCount() >= 2) {
                    isTimerFinished = true;
                    finalizeCall();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startClassTimer() {
        if (classTimer != null) return;
        FirebaseDatabase.getInstance().getReference("Bookings").child(bookingId)
                .child("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    startCountdown(System.currentTimeMillis());
                    return;
                }
                long scheduledStartTime = snapshot.getValue(Long.class);
                startCountdown(scheduledStartTime);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startCountdown(long scheduledStartTime) {
        long totalDuration = 30 * 60 * 1000;
        long scheduledEndTime = scheduledStartTime + totalDuration;
        long remaining = scheduledEndTime - System.currentTimeMillis();

        if (remaining <= 0) {
            isTimerFinished = true;
            finalizeCall();
            return;
        }

        classTimer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long ms) {
                if (layoutCalling.getVisibility() == View.GONE) {
                    timerCard.setVisibility(View.VISIBLE);
                }
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", (ms/60000), (ms%60000/1000)));
            }
            @Override public void onFinish() {
                isTimerFinished = true;
                finalizeCall();
            }
        }.start();
    }

    private void notifyOtherParty() {
        String targetUserId = getIntent().getStringExtra("TARGET_USER_ID");
        if (targetUserId == null) {
            FirebaseDatabase.getInstance().getReference("Bookings").child(bookingId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String sId = snapshot.child("studentId").getValue(String.class);
                        String tId = snapshot.child("tutorId").getValue(String.class);
                        String curr = FirebaseAuth.getInstance().getUid();
                        sendNotification(curr.equals(sId) ? tId : sId);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else {
            sendNotification(targetUserId);
        }
    }

    private void sendNotification(String targetUid) {
        if (targetUid == null) return;
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(targetUid).push();
        Map<String, Object> data = new HashMap<>();
        data.put("isCall", true);
        data.put("title", "Incoming Video Call");
        data.put("message", "A user is calling you...");
        data.put("bookingId", bookingId);
        data.put("timestamp", System.currentTimeMillis());
        notifRef.setValue(data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkSelfPermission()) { startCallingFlow(); }
        else { Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show(); finish(); }
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
            mRtcEngine = null;
        }
    }
}
