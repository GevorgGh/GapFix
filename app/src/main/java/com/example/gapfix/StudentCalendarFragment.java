package com.example.gapfix;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StudentCalendarFragment extends Fragment implements CalendarHomeworkAdapter.OnHomeworkActionListener {

    private static final String TAG = "StudentCalendar";
    private RecyclerView rvCalendar;
    private TextView tvMonthYear;
    private TextView badgeSessions;
    
    private RecyclerView rvBookings, rvHomework;
    private View tvNoClasses, tvHomeworkLabel;
    private BookingAdapter bookingAdapter;
    private final List<Booking> bookingList = new ArrayList<>();
    
    private CalendarHomeworkAdapter homeworkAdapter;
    private final List<FirestoreMessage> homeworkList = new ArrayList<>();
    
    private final Map<String, List<FirestoreMessage>> homeworkByChat = new HashMap<>();

    private String currentStudentId;

    private Calendar currentCalendar;
    private Date selectedDate;
    private final Set<String> bookingDates = new HashSet<>();
    private final Set<String> homeworkDates = new HashSet<>();
    private final Set<String> combinedDates = new HashSet<>();
    private FirebaseFirestore db;

    private final List<ListenerRegistration> chatListeners = new ArrayList<>();
    private ListenerRegistration mainChatsListener;

    private ActivityResultLauncher<String[]> solutionPickerLauncher;
    private FirestoreMessage activeSolvingMessage = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        solutionPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null && activeSolvingMessage != null) {
                        uploadSolutionImage(uri, activeSolvingMessage);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_calendar, container, false);

        currentStudentId = FirebaseAuth.getInstance().getUid();
        
        try {
            db = com.google.firebase.firestore.FirebaseFirestore.getInstance("gapfix");
        } catch (Exception e) {
            db = com.google.firebase.firestore.FirebaseFirestore.getInstance("gapfix");
        }

        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        ImageButton btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        ImageButton btnNextMonth = view.findViewById(R.id.btnNextMonth);
        rvCalendar = view.findViewById(R.id.rvCalendar);
        badgeSessions = view.findViewById(R.id.badge_sessions);
        
        rvBookings = view.findViewById(R.id.rv_bookings);
        rvHomework = view.findViewById(R.id.rv_homework);
        tvHomeworkLabel = view.findViewById(R.id.tv_homework_label);
        tvNoClasses = view.findViewById(R.id.tv_no_classes_container);
        
        view.findViewById(R.id.btn_sessions).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SessionsActivity.class);
            intent.putExtra("role", "Student");
            startActivity(intent);
        });

        currentCalendar = Calendar.getInstance();
        selectedDate = new Date();

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        bookingAdapter = new BookingAdapter(requireContext(), bookingList, true);
        if (rvBookings != null) {
            rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
            rvBookings.setAdapter(bookingAdapter);
        }

        homeworkAdapter = new CalendarHomeworkAdapter(requireContext(), homeworkList, this);
        if (rvHomework != null) {
            rvHomework.setLayoutManager(new LinearLayoutManager(getContext()));
            rvHomework.setAdapter(homeworkAdapter);
        }

        fetchAllLessonDates();
        loadSessionBadgeCount();
        updateCalendar();
        
        return view;
    }

    private void loadSessionBadgeCount() {
        if (currentStudentId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("studentId").equalTo(currentStudentId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                int pendingCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Booking b = ds.getValue(Booking.class);
                    if (b == null) continue;
                    String s = b.getStatus() != null ? b.getStatus().toLowerCase() : "";
                    
                    if (s.equals("suggestion_pending")) {
                        pendingCount++;
                    }
                }
                updateBadge(badgeSessions, pendingCount);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateBadge(TextView badge, int count) {
        if (badge == null) return;
        if (count > 0) {
            badge.setText(String.valueOf(count));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void fetchAllLessonDates() {
        if (currentStudentId == null) return;

        
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("studentId").equalTo(currentStudentId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingDates.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Booking b = ds.getValue(Booking.class);
                    if (b != null && !"cancelled".equalsIgnoreCase(b.getStatus())) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(b.getTimestamp());
                        bookingDates.add(getDateKey(cal));
                    }
                }
                refreshCombinedDates();
                loadDataForDate(selectedDate);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        
        if (mainChatsListener != null) mainChatsListener.remove();
        cleanupChatListeners();
        
        mainChatsListener = db.collection("chats")
            .whereArrayContains("participants", currentStudentId)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    final String chatId = doc.getId();
                    if (!homeworkByChat.containsKey(chatId)) {
                        ListenerRegistration l = doc.getReference().collection("messages")
                            .whereEqualTo("type", "homework")
                            .addSnapshotListener((msgSnapshots, error) -> {
                                if (msgSnapshots == null) return;
                                
                                List<FirestoreMessage> list = new ArrayList<>();
                                for (DocumentSnapshot mDoc : msgSnapshots.getDocuments()) {
                                    FirestoreMessage hm = mDoc.toObject(FirestoreMessage.class);
                                    if (hm != null) {
                                        hm.documentId = mDoc.getId();
                                        hm.chatId = chatId;
                                        list.add(hm);
                                    }
                                }
                                homeworkByChat.put(chatId, list);
                                rebuildHomeworkState();
                            });
                        chatListeners.add(l);
                    }
                }
            });
    }

    private void rebuildHomeworkState() {
        homeworkDates.clear();
        for (List<FirestoreMessage> list : homeworkByChat.values()) {
            for (FirestoreMessage hm : list) {
                long ts = hm.lessonTimestamp > 0 ? hm.lessonTimestamp : 
                         (hm.timestamp != null ? hm.timestamp.toDate().getTime() : 0);
                
                if (ts > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(ts);
                    homeworkDates.add(getDateKey(cal));
                }
            }
        }
        refreshCombinedDates();
        loadDataForDate(selectedDate);
    }

    private String getDateKey(Calendar cal) {
        return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
    }

    private void refreshCombinedDates() {
        combinedDates.clear();
        combinedDates.addAll(bookingDates);
        combinedDates.addAll(homeworkDates);
        updateCalendar();
    }

    private void updateCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentCalendar.getTime()));

        List<Date> days = new ArrayList<>();
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysBefore = (firstDayOfWeek + 5) % 7; 
        cal.add(Calendar.DAY_OF_MONTH, -daysBefore);

        for (int gridIdx = 0; gridIdx < 42; gridIdx++) {
            days.add(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        CalendarMonthAdapter adapter = new CalendarMonthAdapter(days, currentCalendar.getTime(), selectedDate, combinedDates, date -> {
            selectedDate = date;
            loadDataForDate(date);
        });

        rvCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvCalendar.setAdapter(adapter);
    }

    private void loadDataForDate(Date date) {
        if (!isAdded()) return;
        loadBookingsForDate(date);
        filterHomeworkLocally(date);
    }

    private void filterHomeworkLocally(Date date) {
        if (date == null) return;
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(date);
        String targetKey = getDateKey(targetCal);

        homeworkList.clear();
        for (List<FirestoreMessage> list : homeworkByChat.values()) {
            for (FirestoreMessage hm : list) {
                long ts = hm.lessonTimestamp > 0 ? hm.lessonTimestamp : 
                         (hm.timestamp != null ? hm.timestamp.toDate().getTime() : 0);
                         
                if (ts > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(ts);
                    if (targetKey.equals(getDateKey(cal))) {
                        homeworkList.add(hm);
                    }
                }
            }
        }

        if (homeworkList.isEmpty()) {
            if (tvHomeworkLabel != null) tvHomeworkLabel.setVisibility(View.GONE);
            if (rvHomework != null) rvHomework.setVisibility(View.GONE);
        } else {
            if (tvHomeworkLabel != null) tvHomeworkLabel.setVisibility(View.VISIBLE);
            if (rvHomework != null) rvHomework.setVisibility(View.VISIBLE);
        }
        if (homeworkAdapter != null) homeworkAdapter.notifyDataSetChanged();
    }

    private void loadBookingsForDate(Date date) {
        if (currentStudentId == null) return;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endOfDay = cal.getTimeInMillis();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        Query query = ref.orderByChild("studentId").equalTo(currentStudentId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                bookingList.clear();
                Set<String> seenPackageIds = new HashSet<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Booking booking = data.getValue(Booking.class);
                    if (booking != null) {
                        booking.setBookingId(data.getKey());
                        long ts = booking.getTimestamp();
                        if (ts >= startOfDay && ts <= endOfDay) {
                            if (booking.isPackage() && booking.getPackageId() != null) {
                                if (!seenPackageIds.contains(booking.getPackageId())) {
                                    seenPackageIds.add(booking.getPackageId());
                                    bookingList.add(booking);
                                }
                            } else {
                                bookingList.add(booking);
                            }
                        }
                    }
                }

                if (bookingList.isEmpty()) {
                    if (tvNoClasses != null) tvNoClasses.setVisibility(View.VISIBLE);
                    if (rvBookings != null) rvBookings.setVisibility(View.GONE);
                } else {
                    if (tvNoClasses != null) tvNoClasses.setVisibility(View.GONE);
                    if (rvBookings != null) rvBookings.setVisibility(View.VISIBLE);
                }

                if (bookingAdapter != null) bookingAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onViewFile(String url) {
        if (url == null) return;

        if (url.toLowerCase().contains(".pdf")) {
            PdfHelper.openPdf(requireContext(), url);
            return;
        }

        android.app.Dialog viewDialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        viewDialog.setContentView(R.layout.dialog_view_image);
        
        if (viewDialog.getWindow() != null) {
            viewDialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        }

        ImageView ivFull = viewDialog.findViewById(R.id.ivFullImage);
        ImageButton btnClose = viewDialog.findViewById(R.id.btnClose);
        if (ivFull != null) Glide.with(this).load(url).into(ivFull);
        if (btnClose != null) btnClose.setOnClickListener(v -> viewDialog.dismiss());
        viewDialog.show();
    }

    @Override
    public void onUploadSolution(FirestoreMessage homework) {
        activeSolvingMessage = homework;
        solutionPickerLauncher.launch(new String[]{"image/*", "application/pdf"});
    }

    @Override
    public void onArchiveHomework(FirestoreMessage msg) {
        if (msg.fileUrl == null) return;
        
        String subject = msg.subject;
        if (subject == null || subject.isEmpty()) {
            subject = "General";
        }

        String title = msg.text != null ? msg.text : "Archived Homework";
        String safeTitle = title.replaceAll("[.#$\\[\\]/]", "_");
        String safeSubject = subject.replaceAll("[.#$\\[\\]/]", "_");
        
        ArchiveItem archiveItem = new ArchiveItem(
                msg.documentId,
                currentStudentId,
                safeSubject,
                msg.fileUrl,
                safeTitle,
                System.currentTimeMillis()
        );

        DatabaseReference archiveRef = FirebaseDatabase.getInstance().getReference("Users").child("Student").child(currentStudentId)
                .child("Archives").child(safeSubject).child(safeTitle);
        
        archiveRef.setValue(archiveItem).addOnSuccessListener(a -> {
            if (isAdded()) Toast.makeText(getContext(), "Added to Archive", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onCouldnDoIt(FirestoreMessage homework) {
        final String chatId = homework.chatId;
        if (chatId == null) {
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("homeworkStatus", "failed");
        db.collection("chats").document(chatId).collection("messages").document(homework.documentId).update(updates)
                .addOnSuccessListener(a -> {
                    if (isAdded()) Toast.makeText(getContext(), "Status updated", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadSolutionImage(Uri uri, final FirestoreMessage msg) {
        final String chatId = msg.chatId;
        if (chatId == null) {
            return;
        }
        
        Toast.makeText(getContext(), "Uploading solution...", Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(uri).unsigned("ml_default").option("folder", "Solutions/" + chatId).callback(new UploadCallback() {
            @Override public void onSuccess(String requestId, Map resultData) {
                final String url = (String) resultData.get("secure_url");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("solutionUrl", url);
                        updates.put("homeworkStatus", "done");
                        db.collection("chats").document(chatId).collection("messages").document(msg.documentId).update(updates)
                                .addOnSuccessListener(a -> {
                                    if (isAdded()) Toast.makeText(getContext(), "Solution uploaded", Toast.LENGTH_SHORT).show();
                                    activeSolvingMessage = null;
                                });
                    });
                }
            }
            @Override public void onError(String r, ErrorInfo e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Upload failed: " + e.getDescription(), Toast.LENGTH_SHORT).show());
            }
            @Override public void onStart(String r) {}
            @Override public void onProgress(String r, long b, long t) {}
            @Override public void onReschedule(String r, ErrorInfo e) {}
        }).dispatch();
    }

    private void cleanupChatListeners() {
        for (ListenerRegistration l : chatListeners) l.remove();
        chatListeners.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanupChatListeners();
        if (mainChatsListener != null) mainChatsListener.remove();
    }
}
