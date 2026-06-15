package com.athalukita.privatechat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.athalukita.privatechat.data.model.Message
import com.athalukita.privatechat.data.model.MessageType
import com.athalukita.privatechat.databinding.ItemChatMeBinding
import com.athalukita.privatechat.databinding.ItemChatOtherBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(
    private val currentUserId: String,
    private val onViewOnceClick: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        const val VIEW_TYPE_ME = 1
        const val VIEW_TYPE_OTHER = 2
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val revealedViewOnceIds = mutableSetOf<String>()

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) VIEW_TYPE_ME else VIEW_TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ME -> MeViewHolder(
                ItemChatMeBinding.inflate(inflater, parent, false)
            )
            else -> OtherViewHolder(
                ItemChatOtherBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is MeViewHolder -> holder.bind(message, position)
            is OtherViewHolder -> holder.bind(message, position)
        }
    }

    private fun bindMessage(
        message: Message,
        position: Int,
        contentView: android.widget.TextView,
        imageView: android.widget.ImageView,
        viewOnceView: android.widget.TextView,
        deletedView: android.widget.TextView,
        timeView: android.widget.TextView
    ) {
        timeView.text = timeFormat.format(message.timestamp.toDate())

        when {
            message.isDeleted -> {
                contentView.visibility = View.GONE
                imageView.visibility = View.GONE
                viewOnceView.visibility = View.GONE
                deletedView.visibility = View.VISIBLE
            }
            message.isViewOnce && !revealedViewOnceIds.contains(message.id) -> {
                contentView.visibility = View.GONE
                imageView.visibility = View.GONE
                deletedView.visibility = View.GONE
                viewOnceView.visibility = View.VISIBLE
            }
            message.type == MessageType.IMAGE && !message.mediaUrl.isNullOrBlank() -> {
                contentView.visibility = View.GONE
                viewOnceView.visibility = View.GONE
                deletedView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                Glide.with(imageView.context)
                    .load(message.mediaUrl)
                    .centerCrop()
                    .into(imageView)
            }
            else -> {
                imageView.visibility = View.GONE
                viewOnceView.visibility = View.GONE
                deletedView.visibility = View.GONE
                contentView.visibility = View.VISIBLE
                (contentView as android.widget.TextView).text = message.content
            }
        }

        val clickable = message.isViewOnce && !message.isDeleted && !revealedViewOnceIds.contains(message.id)
        val clickListener = if (clickable) {
            View.OnClickListener {
                revealedViewOnceIds.add(message.id)
                onViewOnceClick(message)
                notifyItemChanged(position)
            }
        } else {
            null
        }

        contentView.setOnClickListener(clickListener)
        imageView.setOnClickListener(clickListener)
        viewOnceView.setOnClickListener(clickListener)
        deletedView.setOnClickListener(null)
    }

    inner class MeViewHolder(
        private val binding: ItemChatMeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, position: Int) {
            bindMessage(
                message = message,
                position = position,
                contentView = binding.tvContent,
                imageView = binding.ivImage,
                viewOnceView = binding.tvViewOnce,
                deletedView = binding.tvDeleted,
                timeView = binding.tvTime
            )
        }
    }

    inner class OtherViewHolder(
        private val binding: ItemChatOtherBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, position: Int) {
            bindMessage(
                message = message,
                position = position,
                contentView = binding.tvContent,
                imageView = binding.ivImage,
                viewOnceView = binding.tvViewOnce,
                deletedView = binding.tvDeleted,
                timeView = binding.tvTime
            )
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
