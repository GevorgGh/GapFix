package com.example.gapfix;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
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
    public static String activeBookingId = null;
    private String appId, channelName, bookingId, tutorId, otherId;
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
    private boolean isEnding = false;
    private CountDownTimer classTimer;
    private DatabaseReference callRef;
    private ValueEventListener callStateListener;
    private DatabaseReference bookingRef;
    private ValueEventListener bookingListener;
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                if (!getIntent().getBooleanExtra("IS_INCOMING", false)) {
                    notifyOtherParty();
                }
                startClassTimer();
            });
        }
        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                isRemoteUserJoined = true;
                setupRemoteVideo(uid);
                enterActiveCallMode();
            });
        }
        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
            });
        }
        @Override
        public void onError(int err) { }
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
        String incomingBookingId = intent.getStringExtra("BOOKING_ID");
        if (incomingBookingId == null) incomingBookingId = "test_room";
        if (mRtcEngine != null && incomingBookingId.equals(bookingId)) {
            return;
        }
        isEnding = false;
        appId = getString(R.string.appIdAgora);
        bookingId = incomingBookingId;
        channelName = bookingId;
        activeBookingId = bookingId;
        initUI();
        if (callRef != null && callStateListener != null) callRef.removeEventListener(callStateListener);
        if (bookingRef != null && bookingListener != null) bookingRef.removeEventListener(bookingListener);
        callRef = FirebaseDatabase.getInstance().getReference("Calls").child(bookingId);
        bookingRef = FirebaseDatabase.getInstance().getReference("Bookings").child(bookingId);
        listenForCallState();
        listenForBookingChanges();
        fetchTutorId();
        if (checkSelfPermission()) {
            startCallingFlow();
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
    }
    private void fetchTutorId() {
        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
        } catch (Exception e) { }
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
            boolean isIncoming = getIntent().getBooleanExtra("IS_INCOMING", false);
            if (isIncoming && !isRemoteUserJoined) {
                updateCallState("declined");
            } else {
                updateCallState("offline");
            }
            finish();
        }
    }
    private void finishLessonAction() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        bookingRef.child("finishRequests").child(uid).setValue(true)
                .addOnSuccessListener(aVoid -> {
                    if (uid.equals(tutorId)){
                        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                otherId = snapshot.child("studentId").getValue(String.class);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (Boolean.FALSE.equals(snapshot.child("finishRequests").child(otherId).getValue(Boolean.class))){
                                    btnFinishLesson.setEnabled(false);
                                    btnFinishLesson.setAlpha(0.5f);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }
                    btnFinishLesson.setEnabled(false);
                    btnFinishLesson.setAlpha(0.5f);
                });
    }
    private void finalizeCall() {
        if (isEnding) return;
        isEnding = true;
        isTimerFinished = true;
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid != null && myUid.equals(tutorId)) {
            updateBookingStatusToFinished();
        }
        updateCallState("ended");
        checkIfStudentAndOpenReview();
        addLessonCount();
    }
    private void addLessonCount() {
        if (tutorId == null) return;
        DatabaseReference tutorRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child("Tutor")
                .child(tutorId);
        tutorRef.child("lessonsCount").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                int currentCount = 0;
                if (snapshot.exists() && snapshot.getValue() != null) {
                    currentCount = snapshot.getValue(Integer.class);
                }
                tutorRef.child("lessonsCount").setValue(currentCount + 1);
            } else {
                }
        });
    }
    private void updateBookingStatusToFinished() {
        if (bookingId == null || bookingId.equals("test_room")) return;
        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String studentId = snapshot.child("studentId").getValue(String.class);
                    String tId = snapshot.child("tutorId").getValue(String.class);
                    String subject = snapshot.child("subject").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);
                    Boolean isFree = snapshot.child("isFree").getValue(Boolean.class);
                    Object priceObj = snapshot.child("price").getValue();
                    double lessonPrice = 0.0;
                    if (priceObj instanceof Long) lessonPrice = ((Long) priceObj).doubleValue();
                    else if (priceObj instanceof Double) lessonPrice = (Double) priceObj;
                    else if (priceObj instanceof Integer) lessonPrice = ((Integer) priceObj).doubleValue();
                    boolean wasTrial = "free_trial_pending".equals(status) || (isFree != null && isFree);
                    bookingRef.child("status").setValue("finished");
                    if (wasTrial && studentId != null && tId != null && subject != null) {
                        FirebaseDatabase.getInstance().getReference("FreeLessonsUsed")
                                .child(studentId)
                                .child(tId)
                                .child(subject)
                                .setValue(true);
                    } else if (tId != null && lessonPrice > 0) {
                        awardTutorMoney(tId, lessonPrice);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void awardTutorMoney(String targetTutorId, double amount) {
        DatabaseReference tutorMoneyRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child("Tutor")
                .child(targetTutorId)
                .child("earnedMoney");
        tutorMoneyRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                double currentMoney = 0.0;
                if (currentData.getValue() != null) {
                    Object val = currentData.getValue();
                    if (val instanceof Long) currentMoney = ((Long) val).doubleValue();
                    else if (val instanceof Double) currentMoney = (Double) val;
                    else if (val instanceof Integer) currentMoney = ((Integer) val).doubleValue();
                }
                currentData.setValue(currentMoney + amount);
                return com.google.firebase.database.Transaction.success(currentData);
            }
            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null) {
                    } else {
                    }
            }
        });
    }
    private void cancelBooking() {
        if (bookingId == null || bookingId.equals("test_room")) {
            finish();
            return;
        }
        bookingRef.child("status").setValue("cancelled");
        finish();
    }
    private void checkIfStudentAndOpenReview() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            finish();
            return;
        }
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
        callStateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isEnding) return;
                String state = snapshot.child("state").getValue(String.class);
                if ("ended".equals(state)) {
                    finalizeCall();
                } else if ("cancelled".equals(state) || "declined".equals(state)) {
                    isEnding = true;
                    if ("cancelled".equals(state)) {
                        cancelBooking();
                    } else {
                        finish();
                    }
                } else if ("offline".equals(state)) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!isFinishing() && !isDestroyed()) finish();
                    }, 2000);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        callRef.addValueEventListener(callStateListener);
    }
    private void listenForBookingChanges() {
        bookingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isEnding) return;
                String status = snapshot.child("status").getValue(String.class);
                if ("finished".equals(status)) {
                    finalizeCall();
                    return;
                }
                DataSnapshot finishReqs = snapshot.child("finishRequests");
                if (finishReqs.getChildrenCount() >= 2) {
                    finalizeCall();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        bookingRef.addValueEventListener(bookingListener);
    }
    private void startClassTimer() {
        if (classTimer != null) return;
        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long scheduledStartTime = System.currentTimeMillis();
                int durationMins = 30; 
                if (snapshot.exists()) {
                    Long ts = snapshot.child("timestamp").getValue(Long.class);
                    if (ts != null) {
                        scheduledStartTime = ts;
                    }
                    Object durObj = snapshot.child("duration").getValue();
                    if (durObj instanceof Number) {
                        durationMins = ((Number) durObj).intValue();
                    }
                }
                startCountdown(scheduledStartTime, durationMins);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void startCountdown(long scheduledStartTime, int durationMins) {
        long totalDuration = (long) durationMins * 60 * 1000;
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
        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sId = snapshot.child("studentId").getValue(String.class);
                    String tId = snapshot.child("tutorId").getValue(String.class);
                    Long scheduledStart = snapshot.child("timestamp").getValue(Long.class);
                    String curr = FirebaseAuth.getInstance().getUid();
                    String target = curr.equals(sId) ? tId : sId;
                    if (scheduledStart != null) {
                        long now = System.currentTimeMillis();
                        if (curr.equals(tId) && now < scheduledStart) {
                            long delay = scheduledStart - now;
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    sendNotification(target);
                                }
                            }, delay);
                        } else {
                            sendNotification(target);
                        }
                    } else {
                        sendNotification(target);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        if (bookingId != null && bookingId.equals(activeBookingId)) {
            activeBookingId = null;
        }
        if (classTimer != null) classTimer.cancel();
        if (callRef != null && callStateListener != null) callRef.removeEventListener(callStateListener);
        if (bookingRef != null && bookingListener != null) bookingRef.removeEventListener(bookingListener);
        if (mRtcEngine != null) {
            mRtcEngine.stopPreview();
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }
    }
}
