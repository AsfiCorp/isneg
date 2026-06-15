package com.athalukita.privatechat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.athalukita.privatechat.data.model.ChatRoom
import com.athalukita.privatechat.databinding.ItemChatListBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class ChatListAdapter(
    private val onChatClick: (String) -> Unit
) : ListAdapter<ChatRoom, ChatListAdapter.ChatListViewHolder>(ChatRoomDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val binding = ItemChatListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatListViewHolder(
        private val binding: ItemChatListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatRoom: ChatRoom) {
            binding.tvName.text = chatRoom.otherUserName
            binding.tvLastMessage.text = chatRoom.lastMessage
            binding.tvTime.text = timeFormat.format(chatRoom.lastMessageTimestamp.toDate())

            if (!chatRoom.otherUserPhotoUrl.isNullOrBlank()) {
                Glide.with(binding.ivAvatar.context)
                    .load(chatRoom.otherUserPhotoUrl)
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
            }

            binding.root.setOnClickListener {
                onChatClick(chatRoom.chatId)
            }
        }
    }

    private class ChatRoomDiffCallback : DiffUtil.ItemCallback<ChatRoom>() {
        override fun areItemsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
            return oldItem.chatId == newItem.chatId
        }

        override fun areContentsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
            return oldItem == newItem
        }
    }
}
