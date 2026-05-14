package com.localvault.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.localvault.app.R

fun copyToClipboardWithTimeout(context: Context, label: String, value: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, R.string.clipboard_copied_warning, Toast.LENGTH_LONG).show()
    Handler(Looper.getMainLooper()).postDelayed({
        try {
            cm.clearPrimaryClip()
        } catch (_: Exception) {
        }
    }, 45_000L)
}
