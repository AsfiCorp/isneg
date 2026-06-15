package com.athalukita.privatechat.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.athalukita.privatechat.adapter.ChatAdapter
import com.athalukita.privatechat.adapter.ChatListAdapter
import com.athalukita.privatechat.data.model.MessageType
import com.athalukita.privatechat.databinding.ActivityMainBinding
import com.athalukita.privatechat.ui.base.BaseSecureActivity
import com.athalukita.privatechat.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import android.widget.TextView
import android.view.View
import com.athalukita.privatechat.R
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

class MainActivity : BaseSecureActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ChatViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatListAdapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fabNewChat.setOnClickListener { showNewChatDialog() }

        setupToolbar()
        setupAdapters()
        setupInputActions()
        observeViewModel()
        setupSidebarHeader()
    }

    private fun showNewChatDialog() {
    val input = EditText(this)
    input.hint = "Email (misal: lukitaamelia1804@gmail.com)"
    input.setPadding(48, 32, 48, 32)

    AlertDialog.Builder(this)
        .setTitle("Mulai Chat Baru")
        .setMessage("Masukkan email yang ingin kamu hubungi:")
        .setView(input)
        .setPositiveButton("Mulai") { _, _ ->
            val targetEmail = input.text.toString().trim()
            if (targetEmail.isNotEmpty()) {
                createChatRoom(targetEmail)
            }
        }
        .setNegativeButton("Batal", null)
        .show()
}

private fun createChatRoom(targetEmail: String) {
    val currentUser = auth.currentUser ?: return
    if (targetEmail == currentUser.email) {
        android.widget.Toast.makeText(this, "Tidak bisa chat dengan diri sendiri", android.widget.Toast.LENGTH_SHORT).show()
        return
    }

    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    // Cari UID user target berdasarkan email
    db.collection("users").whereEqualTo("email", targetEmail).get()
        .addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                android.widget.Toast.makeText(this, "Email belum terdaftar. Pastikan dia sudah Register!", android.widget.Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val targetUid = documents.documents[0].getString("uid") ?: return@addOnSuccessListener

            // Buat ruangan chat
            val roomRef = db.collection("chatRooms").document()
            val chatRoomData = hashMapOf(
                "participants" to listOf(currentUser.uid, targetUid),
                "lastMessageTime" to System.currentTimeMillis()
            )

            roomRef.set(chatRoomData).addOnSuccessListener {
                android.widget.Toast.makeText(this, "Berhasil! Silakan buka Sidebar untuk melihat chat.", android.widget.Toast.LENGTH_LONG).show()
                viewModel.loadUserChats()
            }
        }
}

    private fun setupSidebarHeader() {
    // Mengambil header dari navigation_view
    val headerView = binding.navigationView.getHeaderView(0)
    val tvName = headerView.findViewById<TextView>(R.id.tv_profile_name)
    val tvEmail = headerView.findViewById<TextView>(R.id.tv_profile_email)
    
    // Menampilkan data user
    val currentUser = auth.currentUser
    if (currentUser != null) {
        tvName.text = if (!currentUser.displayName.isNullOrEmpty()) currentUser.displayName else "Nama Pengguna"
        tvEmail.text = currentUser.email
    }

    // Membuat header bisa diklik
    headerView.setOnClickListener {
        startActivity(Intent(this, ProfileActivity::class.java))
        binding.drawerLayout.closeDrawers()
    }
}

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupAdapters() {
        val currentUserId = auth.currentUser?.uid ?: return

        chatAdapter = ChatAdapter(
            currentUserId = currentUserId,
            onViewOnceClick = { message ->
                val chatId = viewModel.currentChatId.value ?: return@ChatAdapter
                if (message.type == MessageType.IMAGE && !message.mediaUrl.isNullOrBlank()) {
                    startActivity(
                        ViewOnceActivity.createIntent(
                            context = this,
                            chatId = chatId,
                            messageId = message.id,
                            mediaUrl = message.mediaUrl
                        )
                    )
                }
            }
        )

        chatListAdapter = ChatListAdapter { chatId ->
            viewModel.selectChat(chatId)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        binding.rvChatList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chatListAdapter
        }

        binding.navigationView.getHeaderView(0)?.let { header ->
            header.findViewById<android.widget.TextView>(
                com.athalukita.privatechat.R.id.tv_profile_name
            )?.text = auth.currentUser?.displayName ?: "User"
            header.findViewById<android.widget.TextView>(
                com.athalukita.privatechat.R.id.tv_profile_email
            )?.text = auth.currentUser?.email ?: ""
        }
    }

    private fun setupInputActions() {
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isEmpty()) return@setOnClickListener
            if (viewModel.currentChatId.value == null) {
                Toast.makeText(this, com.athalukita.privatechat.R.string.select_chat_first, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.sendMessage(message)
            binding.etMessage.text?.clear()
        }

        binding.btnCall.setOnClickListener {
            val chatId = viewModel.currentChatId.value
            if (chatId == null) {
                Toast.makeText(this, com.athalukita.privatechat.R.string.select_chat_first, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(
                CallActivity.createIntent(
                    context = this,
                    chatId = chatId,
                    isCaller = true
                )
            )
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messages ->
                        chatAdapter.submitList(messages) {
                            if (messages.isNotEmpty()) {
                                binding.rvChat.scrollToPosition(messages.size - 1)
                            }
                        }
                    }
                }
                launch {
                    viewModel.chatRooms.collect { rooms ->
                        chatListAdapter.submitList(rooms)
                    }
                }
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
