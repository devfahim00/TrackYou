package com.devfahim00.trackyou

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devfahim00.trackyou.data.AppDatabase
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.DebtEntity
import com.devfahim00.trackyou.data.DebtType
import com.devfahim00.trackyou.data.GoalEntity
import com.devfahim00.trackyou.data.PaymentEntity
import com.devfahim00.trackyou.data.Prefs
import com.devfahim00.trackyou.data.ThemeMode
import com.devfahim00.trackyou.data.TxEntity
import com.devfahim00.trackyou.data.TxType
import com.devfahim00.trackyou.data.currencies
import com.devfahim00.trackyou.util.Exporter
import com.devfahim00.trackyou.util.PdfReport
import com.devfahim00.trackyou.util.Reminders
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val prefs = Prefs(app)

    var userName by mutableStateOf(prefs.name)
        private set
    var currency by mutableStateOf<Currency?>(currencies.firstOrNull { it.code == prefs.currencyCode })
        private set
    var themeMode by mutableStateOf(prefs.themeMode)
        private set

    // App lock / security state
    var appLockEnabled by mutableStateOf(prefs.appLockEnabled && prefs.hasPin)
        private set
    var biometricUnlock by mutableStateOf(prefs.biometricUnlock)
        private set

    // Reminder state
    var dailyReminder by mutableStateOf(prefs.dailyReminder)
        private set
    var dueReminder by mutableStateOf(prefs.dueReminder)
        private set

    val transactions: StateFlow<List<TxEntity>> =
        db.txDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts: StateFlow<List<DebtEntity>> =
        db.debtDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val goals: StateFlow<List<GoalEntity>> =
        db.goalDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val payments: StateFlow<List<PaymentEntity>> =
        db.paymentDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveProfile(name: String, c: Currency) {
        prefs.name = name.trim()
        prefs.currencyCode = c.code
        userName = name.trim()
        currency = c
    }

    fun saveThemeMode(m: ThemeMode) {
        prefs.themeMode = m
        themeMode = m
    }

    fun addTx(type: TxType, amount: Double, category: String, note: String, date: Long) {
        viewModelScope.launch {
            db.txDao().insert(
                TxEntity(type = type, amount = amount, category = category, note = note, date = date)
            )
        }
    }

    fun updateTx(t: TxEntity) { viewModelScope.launch { db.txDao().update(t) } }

    fun deleteTx(t: TxEntity) { viewModelScope.launch { db.txDao().delete(t) } }

    /** Re-inserts a previously deleted transaction (undo). */
    fun restoreTx(t: TxEntity) { viewModelScope.launch { db.txDao().insert(t) } }

    fun addDebt(type: DebtType, person: String, amount: Double, note: String, dueDate: Long? = null) {
        viewModelScope.launch {
            db.debtDao().insert(
                DebtEntity(type = type, person = person, amount = amount, note = note, date = System.currentTimeMillis(), dueDate = dueDate)
            )
        }
    }

    fun payDebt(d: DebtEntity, amount: Double) {
        viewModelScope.launch {
            db.paymentDao().insert(
                PaymentEntity(debtId = d.id, amount = amount, date = System.currentTimeMillis())
            )
            db.debtDao().update(d.copy(paid = minOf(d.amount, d.paid + amount)))
        }
    }

    fun deleteDebt(d: DebtEntity) { viewModelScope.launch { db.debtDao().delete(d) } }

    fun addGoal(name: String, target: Double, initial: Double) {
        viewModelScope.launch {
            db.goalDao().insert(GoalEntity(name = name, target = target, saved = initial, createdAt = System.currentTimeMillis()))
        }
    }

    fun changeGoal(g: GoalEntity, delta: Double) {
        viewModelScope.launch {
            db.goalDao().update(g.copy(saved = maxOf(0.0, g.saved + delta)))
        }
    }

    fun deleteGoal(g: GoalEntity) { viewModelScope.launch { db.goalDao().delete(g) } }

    fun exportCsv(context: Context): File {
        val csv = Exporter.buildCsv(transactions.value, debts.value, goals.value)
        return Exporter.write(context, csv)
    }

    fun exportPdf(context: Context, monthTitle: String, from: Long, to: Long): File =
        PdfReport.generate(
            context, monthTitle, from, to,
            transactions.value, debts.value,
            currency ?: currencies.first(), userName
        )

    // ---------------- App lock ----------------

    fun enableAppLock(pin: String, biometric: Boolean) {
        prefs.setPin(pin)
        prefs.appLockEnabled = true
        prefs.biometricUnlock = biometric
        appLockEnabled = true
        biometricUnlock = biometric
    }

    fun disableAppLock() {
        prefs.clearPin()
        appLockEnabled = false
    }

    fun setBiometricUnlock(enabled: Boolean) {
        prefs.biometricUnlock = enabled
        biometricUnlock = enabled
    }

    fun checkPin(pin: String): Boolean = prefs.checkPin(pin)

    fun pinLength(): Int = prefs.pinLength()

    // ---------------- Reminders ----------------

    fun setDailyReminder(enabled: Boolean) {
        prefs.dailyReminder = enabled
        dailyReminder = enabled
        Reminders.sync(getApplication())
    }

    fun setDueReminder(enabled: Boolean) {
        prefs.dueReminder = enabled
        dueReminder = enabled
        Reminders.sync(getApplication())
    }
}
