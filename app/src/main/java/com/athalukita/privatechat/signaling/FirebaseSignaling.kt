package com.athalukita.privatechat.signaling

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class FirebaseSignaling(
    private val callId: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val callRef = firestore.collection("calls").document(callId)
    private val candidatesRef = callRef.collection("candidates")

    fun sendOffer(sdp: SessionDescription) {
        callRef.set(
            mapOf(
                "offer" to mapOf(
                    "type" to sdp.type.canonicalForm(),
                    "sdp" to sdp.description
                )
            )
        )
    }

    fun sendAnswer(sdp: SessionDescription) {
        callRef.set(
            mapOf(
                "answer" to mapOf(
                    "type" to sdp.type.canonicalForm(),
                    "sdp" to sdp.description
                )
            )
        )
    }

    fun sendIceCandidate(userId: String, candidate: IceCandidate) {
        candidatesRef.document(userId).collection("items").add(
            mapOf(
                "sdpMid" to candidate.sdpMid,
                "sdpMLineIndex" to candidate.sdpMLineIndex,
                "candidate" to candidate.sdp
            )
        )
    }

    fun observeSignaling(
        currentUserId: String,
        onOffer: (SessionDescription) -> Unit,
        onAnswer: (SessionDescription) -> Unit,
        onRemoteIceCandidate: (IceCandidate) -> Unit
    ): ListenerRegistration {
        val callListener = callRef.addSnapshotListener { snapshot, _ ->
            val data = snapshot?.data ?: return@addSnapshotListener

            (data["offer"] as? Map<*, *>)?.let { offer ->
                val type = offer["type"] as? String ?: return@let
                val sdp = offer["sdp"] as? String ?: return@let
                onOffer(SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp))
            }

            (data["answer"] as? Map<*, *>)?.let { answer ->
                val type = answer["type"] as? String ?: return@let
                val sdp = answer["sdp"] as? String ?: return@let
                onAnswer(SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp))
            }
        }

        candidatesRef.addSnapshotListener { snapshot, _ ->
            snapshot?.documents?.forEach { userDoc ->
                if (userDoc.id == currentUserId) return@forEach
                userDoc.reference.collection("items")
                    .addSnapshotListener { candidateSnapshot, _ ->
                        candidateSnapshot?.documents?.forEach { doc ->
                            val sdpMid = doc.getString("sdpMid") ?: return@forEach
                            val sdpMLineIndex = doc.getLong("sdpMLineIndex")?.toInt() ?: return@forEach
                            val candidate = doc.getString("candidate") ?: return@forEach
                            onRemoteIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
                        }
                    }
            }
        }

        return callListener
    }

    fun clearCall() {
        callRef.delete()
    }
}
