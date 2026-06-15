package com.athalukita.privatechat.security

import android.os.Build
import java.io.File

object RootDetector {
    fun isDeviceRooted(): Boolean {
        return checkTestKeys() || checkSuperUserApk() || checkSuBinary() || checkBusyBoxBinary() || checkMagisk()
    }
    private fun checkTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    private fun checkSuperUserApk(): Boolean = File("/system/app/Superuser.apk").exists()
    private fun checkSuBinary(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su")
        return paths.any { File(it).exists() }
    }
    private fun checkBusyBoxBinary(): Boolean {
        val paths = arrayOf("/system/bin/busybox", "/system/xbin/busybox")
        return paths.any { File(it).exists() }
    }
    private fun checkMagisk(): Boolean = File("/sbin/.magisk").exists() || File("/data/adb/magisk").exists()
}
