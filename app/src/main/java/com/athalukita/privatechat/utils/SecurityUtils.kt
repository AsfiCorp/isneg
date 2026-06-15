package com.athalukita.privatechat.utils

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    
    fun hashEmail(email: String): String {
        return MessageDigest.getInstance("SHA-256").run {
            update(email.lowercase().toByteArray())
            Base64.encodeToString(digest(), Base64.NO_WRAP)
        }
    }
    
    fun encryptMessage(plainText: String, key: ByteArray): String {
        val cipher = Cipher.getInstance(AES_GCM)
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
    
    fun decryptMessage(encryptedText: String, key: ByteArray): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        val iv = combined.sliceArray(0..11)
        val encrypted = combined.sliceArray(12 until combined.size)
        val cipher = Cipher.getInstance(AES_GCM)
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
    
    fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(password.toByteArray())
        digest.update(salt)
        return digest.digest()
    }
    
    fun getSecurePrefs(context: Context): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
}
