package com.devfahim00.trackyou.data

import android.content.Context
import java.security.MessageDigest

data class Currency(val code: String, val symbol: String, val name: String)

val currencies = listOf(
    Currency("BDT", "৳", "Bangladeshi Taka"),
    Currency("USD", "$", "US Dollar"),
    Currency("INR", "₹", "Indian Rupee"),
    Currency("EUR", "€", "Euro"),
    Currency("GBP", "£", "British Pound"),
    Currency("SAR", "SAR ", "Saudi Riyal"),
    Currency("AED", "AED ", "UAE Dirham"),
    Currency("MYR", "RM ", "Malaysian Ringgit"),
    Currency("PKR", "Rs ", "Pakistani Rupee"),
    Currency("JPY", "¥", "Japanese Yen"),
    Currency("AUD", "A$", "Australian Dollar"),
    Currency("CAD", "C$", "Canadian Dollar")
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("trackyou_prefs", Context.MODE_PRIVATE)

    var name: String
        get() = sp.getString("name", "") ?: ""
        set(v) { sp.edit().putString("name", v).apply() }

    var currencyCode: String
        get() = sp.getString("currency", "") ?: ""
        set(v) { sp.edit().putString("currency", v).apply() }

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(sp.getString("theme", "SYSTEM") ?: "SYSTEM")
        }.getOrDefault(ThemeMode.SYSTEM)
        set(v) { sp.edit().putString("theme", v.name).apply() }

    // ---------------- App lock ----------------

    var appLockEnabled: Boolean
        get() = sp.getBoolean("app_lock", false)
        set(v) { sp.edit().putBoolean("app_lock", v).apply() }

    var biometricUnlock: Boolean
        get() = sp.getBoolean("biometric_unlock", true)
        set(v) { sp.edit().putBoolean("biometric_unlock", v).apply() }

    private var pinHash: String
        get() = sp.getString("pin_hash", "") ?: ""
        set(v) { sp.edit().putString("pin_hash", v).apply() }

    private var pinLen: Int
        get() = sp.getInt("pin_len", 0)
        set(v) { sp.edit().putInt("pin_len", v).apply() }

    val hasPin: Boolean get() = pinHash.isNotBlank() && pinLen > 0

    fun pinLength(): Int = pinLen

    fun setPin(pin: String) {
        pinHash = hashPin(pin)
        pinLen = pin.length
    }

    fun clearPin() {
        pinHash = ""
        pinLen = 0
        appLockEnabled = false
    }

    fun checkPin(pin: String): Boolean = hasPin && hashPin(pin) == pinHash

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest("trackyou:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    // ---------------- Reminders ----------------

    /** Nightly "aj er hishab likhecho?" reminder at ~9 PM (default on). */
    var dailyReminder: Boolean
        get() = sp.getBoolean("daily_reminder", true)
        set(v) { sp.edit().putBoolean("daily_reminder", v).apply() }

    /** Morning reminder for debts that are due / overdue (default on). */
    var dueReminder: Boolean
        get() = sp.getBoolean("due_reminder", true)
        set(v) { sp.edit().putBoolean("due_reminder", v).apply() }

    /** Whether the POST_NOTIFICATIONS runtime permission was already asked once. */
    var notifPermAsked: Boolean
        get() = sp.getBoolean("notif_perm_asked", false)
        set(v) { sp.edit().putBoolean("notif_perm_asked", v).apply() }
}
