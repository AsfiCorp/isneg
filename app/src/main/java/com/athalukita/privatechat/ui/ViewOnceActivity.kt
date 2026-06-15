package com.athalukita.privatechat.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.lifecycle.lifecycleScope
import com.athalukita.privatechat.data.repository.ChatRepository
import com.athalukita.privatechat.databinding.ActivityViewOnceBinding
import com.athalukita.privatechat.ui.base.BaseSecureActivity
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class ViewOnceActivity : BaseSecureActivity() {

    private lateinit var binding: ActivityViewOnceBinding
    private val repository = ChatRepository()
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewOnceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: run {
            finish()
            return
        }
        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: run {
            finish()
            return
        }
        val mediaUrl = intent.getStringExtra(EXTRA_MEDIA_URL) ?: run {
            finish()
            return
        }

        Glide.with(this)
            .load(mediaUrl)
            .fitCenter()
            .into(binding.ivViewOnce)

        startCountdown(chatId, messageId)
    }

    private fun startCountdown(chatId: String, messageId: String) {
        countDownTimer = object : CountDownTimer(COUNTDOWN_MS, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvCountdown.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                lifecycleScope.launch {
                    repository.deleteViewOnceMessage(chatId, messageId)
                    finish()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_CHAT_ID = "extra_chat_id"
        private const val EXTRA_MESSAGE_ID = "extra_message_id"
        private const val EXTRA_MEDIA_URL = "extra_media_url"
        private const val COUNTDOWN_MS = 10_000L

        fun createIntent(
            context: Context,
            chatId: String,
            messageId: String,
            mediaUrl: String
        ): Intent {
            return Intent(context, ViewOnceActivity::class.java).apply {
                putExtra(EXTRA_CHAT_ID, chatId)
                putExtra(EXTRA_MESSAGE_ID, messageId)
                putExtra(EXTRA_MEDIA_URL, mediaUrl)
            }
        }
    }
}
