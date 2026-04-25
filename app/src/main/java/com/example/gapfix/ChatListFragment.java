package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatListFragment extends Fragment {

    private RecyclerView rvConversations;
    private ConversationAdapter adapter;
    private List<FirestoreConversation> conversationList = new ArrayList<>();
    private TextView tvNoChats;
    private FloatingActionButton fabNewChat;
    private String currentUserId;
    private ListenerRegistration chatListener;

    public ChatListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        rvConversations = view.findViewById(R.id.rvConversations);
        tvNoChats = view.findViewById(R.id.tvNoChats);
        fabNewChat = view.findViewById(R.id.fabNewChat);
        currentUserId = FirebaseAuth.getInstance().getUid();

        setupRecyclerView();
        startListeningForChats();

        fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getContext(), NewChatActivity.class))
        );

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ConversationAdapter(conversationList, conversation -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("CHAT_USER_ID", conversation.otherUserId);
            intent.putExtra("CHAT_USER_NAME", conversation.otherUserName);
            startActivity(intent);
        });
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        rvConversations.setAdapter(adapter);
    }

    private void startListeningForChats() {
        if (currentUserId == null) return;

        // CRUCIAL: Connect to the 'gapfix' database instance
        FirebaseFirestore.getInstance("gapfix")
                .collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    conversationList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        FirestoreConversation conv = doc.toObject(FirestoreConversation.class);
                        
                        // 1. Identify other user
                        List<String> participants = (List<String>) doc.get("participants");
                        if (participants != null) {
                            for (String id : participants) {
                                if (!id.equals(currentUserId)) {
                                    conv.otherUserId = id;
                                }
                            }
                        }
                        
                        // 2. Fetch other user's name from metadata Map
                        Object namesObj = doc.get("participantNames");
                        if (namesObj instanceof Map && conv.otherUserId != null) {
                            Map<String, String> namesMap = (Map<String, String>) namesObj;
                            conv.otherUserName = namesMap.get(conv.otherUserId);
                        }

                        // 3. Fetch other user's image from metadata Map
                        Object imagesObj = doc.get("participantImages");
                        if (imagesObj instanceof Map && conv.otherUserId != null) {
                            Map<String, String> imagesMap = (Map<String, String>) imagesObj;
                            conv.otherUserImage = imagesMap.get(conv.otherUserId);
                        }
                        
                        conversationList.add(conv);
                    }
                    
                    tvNoChats.setVisibility(conversationList.isEmpty() ? View.VISIBLE : View.GONE);
                    rvConversations.setVisibility(conversationList.isEmpty() ? View.GONE : View.VISIBLE);
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatListener != null) chatListener.remove();
    }
}
