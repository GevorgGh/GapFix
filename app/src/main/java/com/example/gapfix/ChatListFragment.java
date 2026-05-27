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
    private final List<FirestoreConversation> conversationList = new ArrayList<>();
    private TextView tvNoChats;
    private String currentUserId;
    private ListenerRegistration chatListener;

    public ChatListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        rvConversations = view.findViewById(R.id.rvConversations);
        tvNoChats = view.findViewById(R.id.tvNoChats);
        FloatingActionButton fabNewChat = view.findViewById(R.id.fabNewChat);
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
            intent.putExtra("CHAT_ID", conversation.chatId); 
            intent.putExtra("CHAT_USER_ID", conversation.otherUserId);
            intent.putExtra("CHAT_USER_NAME", conversation.otherUserName);
            startActivity(intent);
        });
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        rvConversations.setAdapter(adapter);
    }

    @SuppressWarnings("unchecked")
    private void startListeningForChats() {
        if (currentUserId == null) return;

        chatListener = com.google.firebase.firestore.FirebaseFirestore.getInstance("gapfix")
                .collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    if (snapshots == null) return;

                    conversationList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        FirestoreConversation conv = doc.toObject(FirestoreConversation.class);
                        conv.chatId = doc.getId(); 

                        
                        conv.unreadCount = (Map<String, Long>) doc.get("unreadCount");

                        
                        Object participantsObj = doc.get("participants");
                        if (participantsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> participants = (List<String>) participantsObj;
                            for (String id : participants) {
                                if (id != null && !id.equals(currentUserId)) {
                                    conv.otherUserId = id;
                                    break;
                                }
                            }
                        }

                        
                        if (conv.otherUserId == null) {
                            
                            Map<String, Object> data = doc.getData();
                            for (String key : data.keySet()) {
                                if (key.startsWith("user_") && !key.equals("user_" + currentUserId)) {
                                    conv.otherUserId = key.replace("user_", "");
                                }
                            }
                        }

                        
                        Object namesObj = doc.get("participantNames");
                        if (namesObj instanceof Map && conv.otherUserId != null) {
                            Map<String, String> namesMap = (Map<String, String>) namesObj;
                            conv.otherUserName = namesMap.get(conv.otherUserId);
                        }

                        
                        Object imagesObj = doc.get("participantImages");
                        if (imagesObj instanceof Map && conv.otherUserId != null) {
                            Map<String, String> imagesMap = (Map<String, String>) imagesObj;
                            conv.otherUserImage = imagesMap.get(conv.otherUserId);
                        }

                        conversationList.add(conv);
                    }

                    
                    conversationList.sort((c1, c2) -> {
                        long t1 = (c1.lastMessageTime != null) ? c1.lastMessageTime.toDate().getTime() : 0;
                        long t2 = (c2.lastMessageTime != null) ? c2.lastMessageTime.toDate().getTime() : 0;
                        
                        if (t1 == 0 && t2 == 0) return 0;
                        if (t1 == 0) return -1;
                        if (t2 == 0) return 1;
                        return Long.compare(t2, t1);
                    });

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