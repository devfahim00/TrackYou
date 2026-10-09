package com.devfahim00.trackyou.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devfahim00.trackyou.MainViewModel
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.DebtEntity
import com.devfahim00.trackyou.data.DebtType
import com.devfahim00.trackyou.data.GoalEntity
import com.devfahim00.trackyou.data.ThemeMode
import com.devfahim00.trackyou.data.TxEntity
import com.devfahim00.trackyou.data.TxType
import com.devfahim00.trackyou.util.Exporter
import com.devfahim00.trackyou.util.PdfReport
import com.devfahim00.trackyou.util.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun SheetHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AmountField(value: String, label: String = "Amount", onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d{0,2}$"))) onChange(v) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------------- Add / Edit transaction ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTxSheet(
    tx: TxEntity?,
    initialType: TxType,
    cur: Currency,
    onDismiss: () -> Unit,
    onSave: (TxType, Double, String, String, Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(tx?.type ?: initialType) }
    var amount by remember { mutableStateOf(tx?.let { String.format(Locale.US, "%.2f", it.amount) } ?: "") }
    var category by remember { mutableStateOf(tx?.category ?: expenseCategories[0]) }
    var note by remember { mutableStateOf(tx?.note ?: "") }
    var dateMillis by remember { mutableStateOf(tx?.date ?: System.currentTimeMillis()) }
    var showPicker by remember { mutableStateOf(false) }
    val cats = if (type == TxType.EXPENSE) expenseCategories else incomeCategories

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            SheetHeader(Icons.Rounded.CalendarMonth, if (tx == null) "Add transaction" else "Edit transaction")
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == TxType.EXPENSE,
                    onClick = {
                        type = TxType.EXPENSE
                        if (category !in expenseCategories) category = expenseCategories[0]
                    },
                    label = { Text("Expense") },
                    leadingIcon = { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = type == TxType.INCOME,
                    onClick = {
                        type = TxType.INCOME
                        if (category !in incomeCategories) category = incomeCategories[0]
                    },
                    label = { Text("Income") },
                    leadingIcon = { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                )
            }
            Spacer(Modifier.height(12.dp))
            AmountField(amount) { amount = it }
            Spacer(Modifier.height(12.dp))

            Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats) { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text(c) },
                        leadingIcon = {
                            Icon(categoryIcon(c), null, Modifier.size(16.dp), tint = categoryTint(c))
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showPicker = true }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateText(dateMillis), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val a = amount.toDoubleOrNull() ?: 0.0
                    if (a > 0) onSave(type, a, category, note.trim(), dateMillis)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text(if (tx == null) "Save transaction" else "Update transaction") }
        }
    }

    if (showPicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    dpState.selectedDateMillis?.let { dateMillis = mergeDateToNow(it) }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dpState)
        }
    }
}

// ---------------- Add debt ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtSheet(
    onDismiss: () -> Unit,
    onSave: (DebtType, String, Double, String, Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(DebtType.LENT) }
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueMillis by remember { mutableStateOf<Long?>(null) }
    var showDuePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            SheetHeader(Icons.Rounded.Settings, "Add dena / paona")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == DebtType.LENT,
                    onClick = { type = DebtType.LENT },
                    label = { Text("Paona (I'll get)") }
                )
                FilterChip(
                    selected = type == DebtType.BORROWED,
                    onClick = { type = DebtType.BORROWED },
                    label = { Text("Dena (I owe)") }
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = person, onValueChange = { person = it },
                label = { Text("Person name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            AmountField(amount) { amount = it }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Note (optional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Optional due date for reminder notifications
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showDuePicker = true }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Event, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(
                        "Due date (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        dueMillis?.let { dateText(it) } ?: "Not set - tap to add",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (dueMillis != null) {
                    TextButton(onClick = { dueMillis = null }) { Text("Clear") }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val a = amount.toDoubleOrNull() ?: 0.0
                    if (a > 0 && person.isNotBlank()) onSave(type, person.trim(), a, note.trim(), dueMillis)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Save") }
        }
    }

    if (showDuePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = dueMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDuePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDuePicker = false
                    dpState.selectedDateMillis?.let { dueMillis = mergeDateToNow(it) }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDuePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dpState)
        }
    }
}

// ---------------- Pay / settle debt ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayDebtSheet(debt: DebtEntity, remaining: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by remember { mutableStateOf(String.format(Locale.US, "%.2f", remaining)) }
    val received = debt.type == DebtType.LENT

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            SheetHeader(Icons.Rounded.Check, if (received) "Received from ${debt.person}" else "Paid to ${debt.person}")
            Spacer(Modifier.height(16.dp))
            AmountField(amount, if (received) "Amount received" else "Amount paid") { amount = it }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { amount = String.format(Locale.US, "%.2f", remaining) }) {
                Text("Use full amount (${String.format(Locale.US, "%,.2f", remaining)})")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val a = amount.toDoubleOrNull() ?: 0.0
                    if (a > 0) onConfirm(a)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Save") }
        }
    }
}

// ---------------- Add goal ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalSheet(onDismiss: () -> Unit, onSave: (String, Double, Double) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var initial by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            SheetHeader(Icons.Rounded.Check, "New savings goal")
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name (e.g. Emergency fund)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            AmountField(target, "Target (optional)") { target = it }
            Spacer(Modifier.height(12.dp))
            AmountField(initial, "Already saved (optional)") { initial = it }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(name.trim(), target.toDoubleOrNull() ?: 0.0, initial.toDoubleOrNull() ?: 0.0)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Create goal") }
        }
    }
}

// ---------------- Goal deposit / withdraw ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalAmountSheet(goal: GoalEntity, deposit: Boolean, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            SheetHeader(Icons.Rounded.Check, if (deposit) "Add to ${goal.name}" else "Withdraw from ${goal.name}")
            Spacer(Modifier.height(16.dp))
            AmountField(amount) { amount = it }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val a = amount.toDoubleOrNull() ?: 0.0
                    if (a > 0) onConfirm(a)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text(if (deposit) "Deposit" else "Withdraw") }
        }
    }
}

// ---------------- Settings ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(vm: MainViewModel, cur: Currency, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(vm.userName) }
    var picked by remember { mutableStateOf(vm.currency) }
    var currencyOpen by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var confirmLockOff by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    val bioAvailable = remember { canBiometric(context) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            vm.setBackupFolder(context, uri)
            vm.backupNow { res ->
                statusIsError = res.isFailure
                statusMsg = res.fold(
                    { "Backup saved to $it." },
                    { "Backup failed: ${it.message ?: "unknown error"}" }
                )
            }
        }
    }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            SheetHeader(Icons.Rounded.Settings, "Settings")
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Your name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Text("Currency", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            if (!currencyOpen) {
                OutlinedButton(
                    onClick = { currencyOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("${picked?.code ?: cur.code} - ${picked?.name ?: cur.name}")
                }
            } else {
                Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                    com.devfahim00.trackyou.data.currencies.forEach { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { picked = c }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = picked?.code == c.code, onClick = { picked = c })
                            Text("${c.code} - ${c.name}")
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("Appearance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeModeChip("System", Icons.Rounded.Settings, vm.themeMode == ThemeMode.SYSTEM) { vm.saveThemeMode(ThemeMode.SYSTEM) }
                ThemeModeChip("Light", Icons.Rounded.LightMode, vm.themeMode == ThemeMode.LIGHT) { vm.saveThemeMode(ThemeMode.LIGHT) }
                ThemeModeChip("Dark", Icons.Rounded.DarkMode, vm.themeMode == ThemeMode.DARK) { vm.saveThemeMode(ThemeMode.DARK) }
            }
            Spacer(Modifier.height(16.dp))

            // ---- Security / app lock ----
            Text("Security", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SettingsToggleRow(
                icon = Icons.Rounded.Lock,
                title = "App lock (PIN)",
                subtitle = "Ask for a PIN when the app opens",
                checked = vm.appLockEnabled,
                onChecked = { on ->
                    if (on) showPinSetup = true else confirmLockOff = true
                }
            )
            if (vm.appLockEnabled) {
                if (bioAvailable) {
                    SettingsToggleRow(
                        icon = Icons.Rounded.Fingerprint,
                        title = "Fingerprint unlock",
                        subtitle = "Also unlock with fingerprint / face",
                        checked = vm.biometricUnlock,
                        onChecked = { vm.updateBiometricUnlock(it) }
                    )
                }
                TextButton(onClick = { showPinSetup = true }) { Text("Change PIN") }
            }
            Spacer(Modifier.height(16.dp))

            // ---- Reminders ----
            Text("Reminders", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SettingsToggleRow(
                icon = Icons.Rounded.NotificationsActive,
                title = "Daily hishab reminder",
                subtitle = "Every night at ~9:00 PM, if nothing was recorded",
                checked = vm.dailyReminder,
                onChecked = { vm.toggleDailyReminder(it) }
            )
            SettingsToggleRow(
                icon = Icons.Rounded.Event,
                title = "Due date alerts",
                subtitle = "Morning alert when dena/paona is due or overdue",
                checked = vm.dueReminder,
                onChecked = { vm.toggleDueReminder(it) }
            )
            if (!Reminders.canPostNotifications(context)) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Notifications are blocked for TrackYou. Allow them from system settings to receive reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(16.dp))

            // ---- Backup & restore ----
            Text("Backup & restore", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val folderSet = vm.backupDirName.isNotBlank()
            val lastText = if (vm.lastBackupAt > 0)
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(vm.lastBackupAt))
            else "never"
            SettingsToggleRow(
                icon = if (folderSet) Icons.Rounded.CloudDone else Icons.Rounded.CloudUpload,
                title = "Auto backup",
                subtitle = if (folderSet) "\"${vm.backupDirName}\" - last backup: $lastText"
                else "Local only - pick a Google Drive folder to sync",
                checked = vm.autoBackup,
                onChecked = { vm.toggleAutoBackup(it) }
            )
            OutlinedButton(
                onClick = { pickFolder.launch(null) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (folderSet) "Change backup folder" else "Choose backup folder (Drive or device)")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    vm.backupNow { res ->
                        statusIsError = res.isFailure
                        statusMsg = res.fold(
                            { "Backup saved to $it." },
                            { "Backup failed: ${it.message ?: "unknown error"}" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Backup now")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { pickRestore.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Restore from backup file")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick a Google Drive folder (Drive app required) so backups sync to the cloud. A fresh copy is also kept on-device every time your data changes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // ---- Data ----
            OutlinedButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            val m = currentMonth()
                            val file = vm.exportPdf(context, m.title(), m.start(), m.end())
                            withContext(Dispatchers.Main) { PdfReport.share(context, file) }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Export PDF report (this month)")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    runCatching {
                        val file = vm.exportCsv(context)
                        Exporter.share(context, file)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Export all data (CSV)")
            }
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(
                    "TrackYou v2.2 - offline-first expense tracker",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val c = picked
                    if (name.isNotBlank() && c != null) {
                        vm.saveProfile(name, c)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Save settings") }
        }
    }

    if (showPinSetup) {
        PinSetupSheet(
            canBiometric = bioAvailable,
            onDismiss = { showPinSetup = false },
            onSet = { pin, bio ->
                vm.enableAppLock(pin, bio)
                showPinSetup = false
            }
        )
    }
    if (confirmLockOff) {
        ConfirmDeleteDialog("App lock", { confirmLockOff = false }) {
            vm.disableAppLock()
            confirmLockOff = false
        }
    }
    if (pendingRestore != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup?") },
            text = {
                Text("Current transactions, dena-paona, payments and goals will be replaced with this backup file. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingRestore
                    pendingRestore = null
                    if (uri != null) {
                        vm.restoreBackup(context, uri) { res ->
                            statusIsError = res.isFailure
                            statusMsg = res.fold(
                                { r ->
                                    "Restored ${r.txCount} transactions, ${r.debtCount} dena-paona records, " +
                                        "${r.paymentCount} repayments and ${r.goalCount} goals." +
                                        (r.profileName?.let { " Profile: $it." } ?: "")
                                },
                                { "Restore failed: ${it.message ?: "not a valid backup file"}" }
                            )
                        }
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } }
        )
    }
    if (statusMsg != null) {
        AlertDialog(
            onDismissRequest = { statusMsg = null },
            title = { Text(if (statusIsError) "Backup / Restore" else "Done") },
            text = { Text(statusMsg ?: "") },
            confirmButton = { TextButton(onClick = { statusMsg = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

// ---------------- PIN setup ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupSheet(
    canBiometric: Boolean,
    onDismiss: () -> Unit,
    onSet: (pin: String, biometric: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableStateOf(0) } // 0 = enter, 1 = confirm, 2 = biometric choice
    var entered by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val current = if (step == 0) entered else confirmed
    val validLength = current.length in 4..6

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SheetHeader(
                Icons.Rounded.Lock,
                when (step) {
                    0 -> "Set app PIN"
                    1 -> "Confirm PIN"
                    else -> "Fingerprint unlock"
                }
            )
            Spacer(Modifier.height(12.dp))

            when (step) {
                0, 1 -> {
                    Text(
                        if (step == 0) "Choose a 4-6 digit PIN to lock TrackYou."
                        else "Re-enter the same PIN to confirm.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    PinDots(
                        count = current.length,
                        total = maxOf(4, current.length),
                        error = error != null
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        error ?: " ",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (error != null) expenseColor() else androidx.compose.ui.graphics.Color.Transparent
                    )
                    Spacer(Modifier.height(14.dp))
                    PinKeypad(
                        onDigit = { d ->
                            error = null
                            if (step == 0) {
                                if (entered.length < 6) entered += d
                            } else {
                                if (confirmed.length < 6) confirmed += d
                            }
                        },
                        onBackspace = {
                            if (step == 0) {
                                if (entered.isNotEmpty()) entered = entered.dropLast(1)
                            } else {
                                if (confirmed.isNotEmpty()) confirmed = confirmed.dropLast(1)
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (step == 0) {
                                step = 1
                            } else {
                                if (confirmed == entered) {
                                    step = if (canBiometric) 2 else 3
                                } else {
                                    error = "PINs didn't match - start over"
                                    confirmed = ""
                                    step = 0
                                }
                            }
                        },
                        enabled = validLength,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(if (step == 0) "Continue" else "Confirm") }
                }
                2 -> {
                    Text(
                        "PIN saved. Also unlock with your fingerprint when available?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onSet(entered, true) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Fingerprint, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Yes, enable fingerprint")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onSet(entered, false) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("No, PIN only") }
                }
                else -> { /* step 3: no biometric available, finishes below */ }
            }
        }
    }

    // step 3: no biometric available -> done directly
    if (step == 3) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onSet(entered, false) }
    }
}

@Composable
private fun ThemeModeChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) }
    )
}

// ---------------- Confirm delete ----------------

@Composable
fun ConfirmDeleteDialog(what: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete?") },
        text = { Text("Delete $what? This cannot be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = expenseColor()) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
