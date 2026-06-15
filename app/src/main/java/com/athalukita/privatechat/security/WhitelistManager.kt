package com.athalukita.privatechat.security

import android.content.Context
import com.athalukita.privatechat.utils.SecurityUtils

class WhitelistManager(context: Context) {
    private val prefs = SecurityUtils.getSecurePrefs(context)
    private val whitelistKey = "whitelisted_emails"
    
    // Daftar email yang diizinkan masuk
    private val allowedEmails = setOf(
        SecurityUtils.hashEmail("athaillahsfi@gmail.com"),
        SecurityUtils.hashEmail("lukitaamelia1804@gmail.com")
    )
    
    fun isEmailAllowed(email: String): Boolean {
        val hashedEmail = SecurityUtils.hashEmail(email)
        return allowedEmails.contains(hashedEmail) || getAllowedEmails().contains(hashedEmail)
    }
    
    fun addAllowedEmail(email: String) {
        val currentList = getAllowedEmails().toMutableSet()
        currentList.add(SecurityUtils.hashEmail(email))
        prefs.edit().putStringSet(whitelistKey, currentList).apply()
    }
    
    fun getAllowedEmails(): Set<String> {
        return prefs.getStringSet(whitelistKey, allowedEmails) ?: emptySet()
    }
}