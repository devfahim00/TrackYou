package com.devfahim00.trackyou.data

import android.content.Context

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
}
