package com.devfahim00.trackyou.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devfahim00.trackyou.MainViewModel
import com.devfahim00.trackyou.data.DebtEntity
import com.devfahim00.trackyou.data.DebtType
import com.devfahim00.trackyou.data.GoalEntity
import com.devfahim00.trackyou.data.TxType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTxDialog(onDismiss: () -> Unit, onSave: (TxType, Double, String, String) -> Unit) {
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(expenseCategories[0]) }
    var note by remember { mutableStateOf("") }
    val cats = if (type == TxType.EXPENSE) expenseCategories else incomeCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TxType.EXPENSE,
                        onClick = { type = TxType.EXPENSE; category = expenseCategories[0] },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == TxType.INCOME,
                        onClick = { type = TxType.INCOME; category = incomeCategories[0] },
                        label = { Text("Income") }
                    )
                }
                AmountField(amount) { amount = it }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cats) { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toDoubleOrNull() ?: 0.0
                if (a > 0) onSave(type, a, category, note.trim())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDialog(onDismiss: () -> Unit, onSave: (DebtType, String, Double, String) -> Unit) {
    var type by remember { mutableStateOf(DebtType.LENT) }
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add dena / paona") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == DebtType.LENT,
                        onClick = { type = DebtType.LENT },
                        label = { Text("Paona (I get)") }
                    )
                    FilterChip(
                        selected = type == DebtType.BORROWED,
                        onClick = { type = DebtType.BORROWED },
                        label = { Text("Dena (I owe)") }
                    )
                }
                OutlinedTextField(
                    value = person, onValueChange = { person = it },
                    label = { Text("Person name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AmountField(amount) { amount = it }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toDoubleOrNull() ?: 0.0
                if (a > 0 && person.isNotBlank()) onSave(type, person.trim(), a, note.trim())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PayDebtDialog(debt: DebtEntity, remaining: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", remaining)) }
    val received = debt.type == DebtType.LENT
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (received) "Received from ${debt.person}" else "Paid to ${debt.person}") },
        text = { AmountField(amount) { amount = it } },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toDoubleOrNull() ?: 0.0
                if (a > 0) onConfirm(a)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onSave: (String, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var initial by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New savings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name (e.g. Emergency fund)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AmountField(target, "Target (optional)") { target = it }
                AmountField(initial, "Already saved (optional)") { initial = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(name.trim(), target.toDoubleOrNull() ?: 0.0, initial.toDoubleOrNull() ?: 0.0)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun GoalAmountDialog(goal: GoalEntity, deposit: Boolean, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (deposit) "Add to ${goal.name}" else "Withdraw from ${goal.name}") },
        text = { AmountField(amount) { amount = it } },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toDoubleOrNull() ?: 0.0
                if (a > 0) onConfirm(a)
            }) { Text(if (deposit) "Deposit" else "Withdraw") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDeleteDialog(what: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete?") },
        text = { Text("Delete $what? This cannot be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProfileDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(vm.userName) }
    var cur by remember { mutableStateOf(vm.currency) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Your name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Currency")
                Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    CurrencyList(cur) { cur = it }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val c = cur
                if (name.isNotBlank() && c != null) {
                    vm.saveProfile(name, c)
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
