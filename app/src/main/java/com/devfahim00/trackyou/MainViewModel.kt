package com.devfahim00.trackyou

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.devfahim00.trackyou.util.Backup
import com.devfahim00.trackyou.util.Exporter
import com.devfahim00.trackyou.util.PdfReport
import com.devfahim00.trackyou.util.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // Backup & restore state
    var backupDirName by mutableStateOf(prefs.backupDirName)
        private set
    var lastBackupAt by mutableStateOf(prefs.lastBackupAt)
        private set
    var autoBackup by mutableStateOf(prefs.autoBackup)
        private set

    val transactions: StateFlow<List<TxEntity>> =
        db.txDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts: StateFlow<List<DebtEntity>> =
        db.debtDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val goals: StateFlow<List<GoalEntity>> =
        db.goalDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val payments: StateFlow<List<PaymentEntity>> =
        db.paymentDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Backup right after a fresh app open if the last backup is older than 12h.
        if (prefs.autoBackup && System.currentTimeMillis() - prefs.lastBackupAt > 12 * 60 * 60 * 1000L) {
            viewModelScope.launch(Dispatchers.IO) { backupSilent() }
        }
        // Auto backup shortly after any data change (Room invalidation), debounced.
        viewModelScope.launch {
            combine(transactions, debts, goals, payments) { t, d, g, p ->
                t.size + d.size + g.size + p.size
            }.drop(1) // skip the initial emission - nothing changed yet
                .collect { scheduleAutoBackup() }
        }
    }

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

    fun updateBiometricUnlock(enabled: Boolean) {
        prefs.biometricUnlock = enabled
        biometricUnlock = enabled
    }

    fun checkPin(pin: String): Boolean = prefs.checkPin(pin)

    fun pinLength(): Int = prefs.pinLength()

    // ---------------- Reminders ----------------

    fun toggleDailyReminder(enabled: Boolean) {
        prefs.dailyReminder = enabled
        dailyReminder = enabled
        Reminders.sync(getApplication())
    }

    fun toggleDueReminder(enabled: Boolean) {
        prefs.dueReminder = enabled
        dueReminder = enabled
        Reminders.sync(getApplication())
    }

    // ---------------- Backup & restore ----------------

    private var autoBackupJob: Job? = null

    /** Debounced: waits 10s after the last change before backing up. */
    private fun scheduleAutoBackup() {
        if (!prefs.autoBackup) return
        autoBackupJob?.cancel()
        autoBackupJob = viewModelScope.launch {
            delay(10_000)
            backupSilent()
        }
    }

    private suspend fun backupSilent() {
        runCatching {
            val app = getApplication<Application>()
            val json = Backup.snapshot(
                transactions.value, debts.value, payments.value, goals.value,
                userName, currency?.code ?: "", themeMode.name
            )
            Backup.writeLocal(app, json)
            val tree = prefs.backupDirUri
            if (tree.isNotBlank()) Backup.writeToTree(app, Uri.parse(tree), json)
            prefs.lastBackupAt = System.currentTimeMillis()
            lastBackupAt = prefs.lastBackupAt
        }
    }

    /** Remembers the user-picked backup folder (e.g. a Google Drive folder) with persistable access. */
    fun setBackupFolder(context: Context, uri: Uri?) {
        if (uri == null) return
        val old = prefs.backupDirUri
        if (old.isNotBlank()) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(old),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val name = Backup.folderDisplayName(context, uri)
            prefs.backupDirUri = uri.toString()
            prefs.backupDirName = name
            backupDirName = name
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        prefs.autoBackup = enabled
        autoBackup = enabled
        if (enabled) scheduleAutoBackup()
    }

    /** Manual backup from settings. [onDone] receives the destination description or the error. */
    fun backupNow(onDone: (Result<String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = runCatching {
                val app = getApplication<Application>()
                val json = Backup.snapshot(
                    transactions.value, debts.value, payments.value, goals.value,
                    userName, currency?.code ?: "", themeMode.name
                )
                Backup.writeLocal(app, json)
                var where = "device storage (app folder)"
                if (prefs.backupDirUri.isNotBlank() &&
                    Backup.writeToTree(app, Uri.parse(prefs.backupDirUri), json) != null
                ) {
                    where = "the \"${prefs.backupDirName}\" folder"
                }
                prefs.lastBackupAt = System.currentTimeMillis()
                where
            }
            withContext(Dispatchers.Main) {
                lastBackupAt = prefs.lastBackupAt
                onDone(res)
            }
        }
    }

    /** Replaces the whole database with the picked backup file. */
    fun restoreBackup(context: Context, uri: Uri, onDone: (Result<Backup.RestoreResult>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var restored: Backup.RestoreResult? = null
            val res = runCatching {
                val json = Backup.readText(context, uri)
                val r = Backup.restoreInto(json, db)
                restored = r
                r.profileName?.takeIf { it.isNotBlank() }?.let { prefs.name = it }
                r.profileCurrency?.takeIf { it.isNotBlank() }?.let { prefs.currencyCode = it }
                r
            }
            withContext(Dispatchers.Main) {
                restored?.let { r ->
                    r.profileName?.takeIf { it.isNotBlank() }?.let { n -> userName = n }
                    r.profileCurrency?.takeIf { it.isNotBlank() }?.let { c ->
                        currency = currencies.firstOrNull { it.code == c }
                    }
                }
                onDone(res)
            }
        }
    }
}
