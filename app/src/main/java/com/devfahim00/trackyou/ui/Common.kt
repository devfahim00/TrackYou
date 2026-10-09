package com.devfahim00.trackyou.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.TxEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val expenseCategories = listOf("Food", "Transport", "Shopping", "Bills", "Rent", "Health", "Education", "Entertainment", "Other")
val incomeCategories = listOf("Salary", "Business", "Freelance", "Gift", "Other")

fun fmt(c: Currency, v: Double): String = c.symbol + String.format(Locale.US, "%,.2f", v)

fun fmtShort(c: Currency, v: Double): String = c.symbol + String.format(Locale.US, "%,.0f", v)

fun dateText(ms: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))

fun timeText(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

// ---------------- Category visual metadata ----------------

private val categoryMeta: Map<String, Pair<ImageVector, Color>> = mapOf(
    "Food" to (Icons.Rounded.Restaurant to Color(0xFFF59E0B)),
    "Transport" to (Icons.Rounded.DirectionsBus to Color(0xFF0EA5E9)),
    "Shopping" to (Icons.Rounded.ShoppingBag to Color(0xFFEC4899)),
    "Bills" to (Icons.Rounded.Receipt to Color(0xFF8B5CF6)),
    "Rent" to (Icons.Rounded.Home to Color(0xFF64748B)),
    "Health" to (Icons.Rounded.LocalHospital to Color(0xFFEF4444)),
    "Education" to (Icons.Rounded.School to Color(0xFF6366F1)),
    "Entertainment" to (Icons.Rounded.Movie to Color(0xFFD946EF)),
    "Other" to (Icons.Rounded.MoreHoriz to Color(0xFF94A3B8)),
    "Salary" to (Icons.Rounded.Payments to Color(0xFF10B981)),
    "Business" to (Icons.Rounded.BusinessCenter to Color(0xFF14B8A6)),
    "Freelance" to (Icons.Rounded.Work to Color(0xFF06B6D4)),
    "Gift" to (Icons.Rounded.CardGiftcard to Color(0xFFF472B6))
)

fun categoryIcon(name: String): ImageVector = categoryMeta[name]?.first ?: Icons.Rounded.MoreHoriz

fun categoryTint(name: String): Color = categoryMeta[name]?.second ?: Color(0xFF94A3B8)

// ---------------- Date / month helpers ----------------

data class MonthSel(val year: Int, val month: Int) { // month is 0-based (Calendar style)
    fun index(): Int = year * 12 + month
    fun start(): Long {
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, month, 1)
        return c.timeInMillis
    }
    fun end(): Long = next().start()
    fun next(): MonthSel = if (month == 11) MonthSel(year + 1, 0) else MonthSel(year, month + 1)
    fun prev(): MonthSel = if (month == 0) MonthSel(year - 1, 11) else MonthSel(year, month - 1)
    fun back(n: Int): MonthSel {
        val i = index() - n
        return MonthSel(i / 12, i % 12)
    }
    fun title(): String {
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, month, 1)
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(c.time)
    }
    fun shortLabel(): String {
        val c = Calendar.getInstance()
        c.clear()
        c.set(year, month, 1)
        return SimpleDateFormat("MMM", Locale.getDefault()).format(c.time)
    }
}

fun currentMonth(): MonthSel {
    val c = Calendar.getInstance()
    return MonthSel(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
}

fun monthStart(): Long = currentMonth().start()

/** "Today", "Yesterday" or "Sat, 12 Oct". */
fun dayLabel(ms: Long): String {
    val key = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val today = key.format(Date())
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = key.format(cal.time)
    return when (key.format(Date(ms))) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(ms))
    }
}

data class DayGroup(val label: String, val txs: List<TxEntity>, val dayNet: Double)

fun groupByDay(txs: List<TxEntity>): List<DayGroup> {
    val key = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return txs
        .groupBy { key.format(Date(it.date)) }
        .toSortedMap(compareByDescending { it })
        .map { (_, list) ->
            DayGroup(
                label = dayLabel(list.first().date),
                txs = list,
                dayNet = list.sumOf { if (it.type == com.devfahim00.trackyou.data.TxType.INCOME) it.amount else -it.amount }
            )
        }
}

/**
 * DatePicker returns UTC-midnight millis; merge the chosen calendar date
 * with the current local time so stored dates stay in local time.
 */
fun mergeDateToNow(utcMillis: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    utc.timeInMillis = utcMillis
    val local = Calendar.getInstance()
    local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    return local.timeInMillis
}
