package com.athalukita.privatechat.webrtc

import android.content.Context
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class WebRTCManager(
    private val context: Context,
    private val onIceCandidate: (IceCandidate) -> Unit,
    private val onLocalOffer: (SessionDescription) -> Unit,
    private val onLocalAnswer: (SessionDescription) -> Unit
) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let { onIceCandidate(it) }
        }

        @Deprecated("Deprecated in Java")
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
        override fun onAddStream(stream: org.webrtc.MediaStream?) = Unit
        override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit
        override fun onDataChannel(channel: org.webrtc.DataChannel?) = Unit

        @Deprecated("Deprecated in Java")
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, streams: Array<out org.webrtc.MediaStream>?) = Unit
        override fun onTrack(transceiver: org.webrtc.RtpTransceiver?) = Unit
        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) = Unit
        override fun onSelectedCandidatePairChanged(event: org.webrtc.CandidatePairChangeEvent?) = Unit
    }

    fun initialize() {
        if (peerConnectionFactory != null) return

        val options = PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(iceServers: List<PeerConnection.IceServer> = defaultIceServers()) {
        val factory = peerConnectionFactory ?: return
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peerConnection = factory.createPeerConnection(rtcConfig, peerConnectionObserver)
    }

    fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription ?: return
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sessionDescription)
                onLocalOffer(sessionDescription)
            }
        }, constraints)
    }

    fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription ?: return
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sessionDescription)
                onLocalAnswer(sessionDescription)
            }
        }, constraints)
    }

    fun setRemoteDescription(sessionDescription: SessionDescription, onComplete: () -> Unit = {}) {
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                onComplete()
            }
        }, sessionDescription)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun dispose() {
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
    }

    private fun defaultIceServers(): List<PeerConnection.IceServer> {
        return listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
    }

    private open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }
}
