package com.athalukita.privatechat.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.athalukita.privatechat.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        setupToolbar()
        loadCurrentProfile()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun loadCurrentProfile() {
        val user = auth.currentUser ?: return
        binding.etName.setText(user.displayName)

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val status = document.getString("status")
                    if (status != null && status != "online") {
                        binding.etStatus.setText(status)
                    }
                }
            }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun saveProfile() {
        val user = auth.currentUser ?: return
        val name = binding.etName.text.toString().trim()
        val status = binding.etStatus.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        val profileUpdates = userProfileChangeRequest { displayName = name }

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userData = hashMapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "displayName" to name,
                    "status" to if (status.isEmpty()) "online" else status
                )
                db.collection("users").document(user.uid)
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        binding.btnSave.isEnabled = true
                        finish() 
                    }
            } else {
                Toast.makeText(this, "Gagal update profil", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}