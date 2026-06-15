package com.athalukita.privatechat.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.athalukita.privatechat.databinding.ActivityLoginBinding
import com.athalukita.privatechat.security.WhitelistManager

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var whitelistManager: WhitelistManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = Firebase.auth
        whitelistManager = WhitelistManager(this)
        
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        setupListeners()
    }
    
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateInput(email, password)) loginUser(email, password)
        }
        
        // INI YANG BIKIN TOMBOL REGISTER HIDUP
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateInput(email, password)) registerUser(email, password)
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = "Email dan password wajib diisi!"
            return false
        }
        if (!whitelistManager.isEmailAllowed(email)) {
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = "Akses Ditolak: Email tidak diizinkan."
            return false
        }
        binding.tvError.visibility = View.GONE
        return true
    }
    
    private fun loginUser(email: String, password: String) {
        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = "Login Gagal: Periksa password."
                }
            }
    }
    
    private fun registerUser(email: String, password: String) {
        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Akun Dibuat!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = "Gagal: ${task.exception?.message}"
                }
            }
    }
    
    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnRegister.isEnabled = !isLoading
    }
}