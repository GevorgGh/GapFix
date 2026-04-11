package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.agora.chat.ChatClient;
import io.agora.chat.Conversation;

public class ChatListFragment extends Fragment {

    private RecyclerView rvConversations;
    private ConversationAdapter adapter;
    private List<Conversation> conversationList = new ArrayList<>();
    private TextView tvNoChats;
    private FloatingActionButton fabNewChat;

    public ChatListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        rvConversations = view.findViewById(R.id.rvConversations);
        tvNoChats = view.findViewById(R.id.tvNoChats);
        fabNewChat = view.findViewById(R.id.fabNewChat);

        setupRecyclerView();
        loadConversations();

        fabNewChat.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NewChatActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ConversationAdapter(conversationList, conversation -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("CHAT_USER_ID", conversation.conversationId());
            intent.putExtra("CHAT_USER_NAME", conversation.conversationId());
            startActivity(intent);
        });
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        rvConversations.setAdapter(adapter);
    }

    private void loadConversations() {
        Map<String, Conversation> conversations = ChatClient.getInstance().chatManager().getAllConversations();
        conversationList.clear();
        if (conversations != null && !conversations.isEmpty()) {
            conversationList.addAll(conversations.values());
            tvNoChats.setVisibility(View.GONE);
            rvConversations.setVisibility(View.VISIBLE);
        } else {
            tvNoChats.setVisibility(View.VISIBLE);
            rvConversations.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadConversations();
    }
}
