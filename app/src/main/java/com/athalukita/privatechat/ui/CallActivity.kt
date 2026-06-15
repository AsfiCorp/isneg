package com.athalukita.privatechat.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.athalukita.privatechat.databinding.ActivityCallBinding
import com.athalukita.privatechat.signaling.FirebaseSignaling
import com.athalukita.privatechat.ui.base.BaseSecureActivity
import com.athalukita.privatechat.webrtc.WebRTCManager
import com.google.firebase.auth.FirebaseAuth
import org.webrtc.EglBase

class CallActivity : BaseSecureActivity() {

    private lateinit var binding: ActivityCallBinding
    private var webRTCManager: WebRTCManager? = null
    private var firebaseSignaling: FirebaseSignaling? = null
    private var eglBase: EglBase? = null
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: run {
            finish()
            return
        }
        val isCaller = intent.getBooleanExtra(EXTRA_IS_CALLER, false)
        val currentUserId = auth.currentUser?.uid ?: run {
            finish()
            return
        }

        eglBase = EglBase.create()
        binding.localVideoView.init(eglBase?.eglBaseContext, null)
        binding.remoteVideoView.init(eglBase?.eglBaseContext, null)
        binding.localVideoView.setMirror(true)

        firebaseSignaling = FirebaseSignaling(chatId)
        webRTCManager = WebRTCManager(
            context = this,
            onIceCandidate = { candidate ->
                firebaseSignaling?.sendIceCandidate(currentUserId, candidate)
            },
            onLocalOffer = { offer ->
                firebaseSignaling?.sendOffer(offer)
                binding.tvCallStatus.text = getString(com.athalukita.privatechat.R.string.call_waiting_answer)
            },
            onLocalAnswer = { answer ->
                firebaseSignaling?.sendAnswer(answer)
                binding.tvCallStatus.text = getString(com.athalukita.privatechat.R.string.call_connected)
            }
        )

        webRTCManager?.initialize()
        webRTCManager?.createPeerConnection()

        firebaseSignaling?.observeSignaling(
            currentUserId = currentUserId,
            onOffer = { offer ->
                if (!isCaller) {
                    webRTCManager?.setRemoteDescription(offer) {
                        webRTCManager?.createAnswer()
                    }
                }
            },
            onAnswer = { answer ->
                if (isCaller) {
                    webRTCManager?.setRemoteDescription(answer)
                    binding.tvCallStatus.text = getString(com.athalukita.privatechat.R.string.call_connected)
                }
            },
            onRemoteIceCandidate = { candidate ->
                webRTCManager?.addIceCandidate(candidate)
            }
        )

        if (isCaller) {
            binding.tvCallStatus.text = getString(com.athalukita.privatechat.R.string.call_calling)
            webRTCManager?.createOffer()
        } else {
            binding.tvCallStatus.text = getString(com.athalukita.privatechat.R.string.call_incoming)
        }

        binding.btnEndCall.setOnClickListener { endCall() }
        binding.btnToggleMic.setOnClickListener {
            binding.tvCallStatus.text = getString(com.athalukita.privatechat.R.string.call_mic_toggled)
        }
        binding.btnToggleCamera.setOnClickListener {
            binding.tvCallStatus.text = getString(com.athalukita.privatechat.R.string.call_camera_toggled)
        }
    }

    private fun endCall() {
        firebaseSignaling?.clearCall()
        finish()
    }

    override fun onDestroy() {
        binding.localVideoView.release()
        binding.remoteVideoView.release()
        eglBase?.release()
        webRTCManager?.dispose()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_CHAT_ID = "extra_chat_id"
        private const val EXTRA_IS_CALLER = "extra_is_caller"

        fun createIntent(context: Context, chatId: String, isCaller: Boolean): Intent {
            return Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_CHAT_ID, chatId)
                putExtra(EXTRA_IS_CALLER, isCaller)
            }
        }
    }
}
