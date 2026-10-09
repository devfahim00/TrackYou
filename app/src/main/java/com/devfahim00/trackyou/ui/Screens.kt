package com.devfahim00.trackyou.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devfahim00.trackyou.MainViewModel
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.DebtEntity
import com.devfahim00.trackyou.data.DebtType
import com.devfahim00.trackyou.data.GoalEntity
import com.devfahim00.trackyou.data.TxEntity
import com.devfahim00.trackyou.data.TxType
import com.devfahim00.trackyou.data.currencies

// ---------------- Onboarding ----------------

@Composable
fun OnboardingScreen(vm: MainViewModel) {
    var name by remember { mutableStateOf("") }
    var cur by remember { mutableStateOf<Currency?>(currencies.first()) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Welcome to TrackYou", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Let's set things up. You can change these later.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Your name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Text("Choose your currency", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CurrencyList(cur) { cur = it }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { cur?.let { vm.saveProfile(name, it) } },
            enabled = name.isNotBlank() && cur != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Get Started") }
    }
}

// ---------------- Main shell ----------------

private data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(vm: MainViewModel) {
    val cur = vm.currency ?: return
    val tabs = listOf(
        TabItem("Home", Icons.Filled.Home),
        TabItem("History", Icons.Filled.ReceiptLong),
        TabItem("Dena-Paona", Icons.Filled.SwapHoriz),
        TabItem("Savings", Icons.Filled.Savings)
    )
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showTx by remember { mutableStateOf(false) }
    var showDebt by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (tab == 0 || tab == 1) {
                    showTx = true
                } else if (tab == 2) {
                    showDebt = true
                } else {
                    showGoal = true
                }
            }) { Icon(Icons.Filled.Add, contentDescription = "Add") }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(vm, cur)
                1 -> TransactionsScreen(vm, cur)
                2 -> DebtScreen(vm, cur)
                else -> SavingsScreen(vm, cur)
            }
        }
    }

    if (showTx) {
        AddTxDialog(onDismiss = { showTx = false }) { type, amt, cat, note ->
            vm.addTx(type, amt, cat, note)
            showTx = false
        }
    }
    if (showDebt) {
        AddDebtDialog(onDismiss = { showDebt = false }) { type, person, amt, note ->
            vm.addDebt(type, person, amt, note)
            showDebt = false
        }
    }
    if (showGoal) {
        AddGoalDialog(onDismiss = { showGoal = false }) { name, target, initial ->
            vm.addGoal(name, target, initial)
            showGoal = false
        }
    }
}

// ---------------- Home ----------------

@Composable
fun HomeScreen(vm: MainViewModel, cur: Currency) {
    val txs by vm.transactions.collectAsState()
    val debts by vm.debts.collectAsState()
    val goals by vm.goals.collectAsState()
    var showProfile by remember { mutableStateOf(false) }

    val income = txs.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val saved = goals.sumOf { it.saved }
    val balance = income - expense - saved
    val ms = remember { monthStart() }
    val mIncome = txs.filter { it.type == TxType.INCOME && it.date >= ms }.sumOf { it.amount }
    val mExpense = txs.filter { it.type == TxType.EXPENSE && it.date >= ms }.sumOf { it.amount }
    val toGet = debts.filter { it.type == DebtType.LENT }.sumOf { it.amount - it.paid }
    val toPay = debts.filter { it.type == DebtType.BORROWED }.sumOf { it.amount - it.paid }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hello,", style = MaterialTheme.typography.bodyMedium)
                    Text(vm.userName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { showProfile = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Available balance", style = MaterialTheme.typography.labelLarge)
                    Text(fmt(cur, balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Income - Expense - Savings", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Income (this month)", fmt(cur, mIncome), IncomeGreen, Modifier.weight(1f))
                SummaryCard("Expense (this month)", fmt(cur, mExpense), ExpenseRed, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Paona (you'll get)", fmt(cur, toGet), IncomeGreen, Modifier.weight(1f))
                SummaryCard("Dena (you owe)", fmt(cur, toPay), ExpenseRed, Modifier.weight(1f))
            }
        }
        item { SummaryCard("Total savings", fmt(cur, saved), MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth()) }
        item { Text("Recent transactions", style = MaterialTheme.typography.titleMedium) }
        if (txs.isEmpty()) {
            item { Text("No transactions yet. Tap + to add one.") }
        }
        items(txs.take(5), key = { it.id }) { TxRow(it, cur, null) }
    }

    if (showProfile) ProfileDialog(vm) { showProfile = false }
}

@Composable
fun TxRow(t: TxEntity, cur: Currency, onDelete: (() -> Unit)?) {
    val income = t.type == TxType.INCOME
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t.category, fontWeight = FontWeight.SemiBold)
                if (t.note.isNotBlank()) Text(t.note, style = MaterialTheme.typography.bodySmall)
                Text(dateText(t.date), style = MaterialTheme.typography.labelSmall)
            }
            Text(
                (if (income) "+" else "-") + fmt(cur, t.amount),
                color = if (income) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.Bold
            )
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        }
    }
}

// ---------------- Transactions ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(vm: MainViewModel, cur: Currency) {
    val txs by vm.transactions.collectAsState()
    var filter by remember { mutableStateOf<TxType?>(null) }
    var toDelete by remember { mutableStateOf<TxEntity?>(null) }
    val shown = txs.filter { filter == null || it.type == filter }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
            FilterChip(selected = filter == TxType.INCOME, onClick = { filter = TxType.INCOME }, label = { Text("Income") })
            FilterChip(selected = filter == TxType.EXPENSE, onClick = { filter = TxType.EXPENSE }, label = { Text("Expense") })
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (shown.isEmpty()) item { Text("Nothing here yet. Tap + to add.") }
            items(shown, key = { it.id }) { t -> TxRow(t, cur) { toDelete = t } }
        }
    }

    toDelete?.let { t ->
        ConfirmDeleteDialog("this transaction", { toDelete = null }) {
            vm.deleteTx(t)
            toDelete = null
        }
    }
}

// ---------------- Dena-Paona ----------------

@Composable
fun DebtScreen(vm: MainViewModel, cur: Currency) {
    val debts by vm.debts.collectAsState()
    var payFor by remember { mutableStateOf<DebtEntity?>(null) }
    var toDelete by remember { mutableStateOf<DebtEntity?>(null) }
    val toGet = debts.filter { it.type == DebtType.LENT }.sumOf { it.amount - it.paid }
    val toPay = debts.filter { it.type == DebtType.BORROWED }.sumOf { it.amount - it.paid }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Paona (you'll get)", fmt(cur, toGet), IncomeGreen, Modifier.weight(1f))
                SummaryCard("Dena (you owe)", fmt(cur, toPay), ExpenseRed, Modifier.weight(1f))
            }
        }
        if (debts.isEmpty()) item { Text("No dena-paona yet. Tap + to add.") }
        items(debts, key = { it.id }) { d ->
            val remaining = (d.amount - d.paid).coerceAtLeast(0.0)
            val settled = remaining < 0.005
            val lent = d.type == DebtType.LENT
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.person, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (lent) "Paona - you'll get" else "Dena - you owe",
                                color = if (lent) IncomeGreen else ExpenseRed,
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (d.note.isNotBlank()) Text(d.note, style = MaterialTheme.typography.bodySmall)
                            Text(dateText(d.date), style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { toDelete = d }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (d.paid / d.amount).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Total ${fmt(cur, d.amount)}  |  Paid ${fmt(cur, d.paid)}  |  Left ${fmt(cur, remaining)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (settled) {
                        Text("Settled", color = IncomeGreen, fontWeight = FontWeight.Bold)
                    } else {
                        TextButton(onClick = { payFor = d }) { Text(if (lent) "Record received" else "Record payment") }
                    }
                }
            }
        }
    }

    payFor?.let { d ->
        PayDebtDialog(d, (d.amount - d.paid).coerceAtLeast(0.0), { payFor = null }) { amt ->
            vm.payDebt(d, amt)
            payFor = null
        }
    }
    toDelete?.let { d ->
        ConfirmDeleteDialog("${d.person}'s record", { toDelete = null }) {
            vm.deleteDebt(d)
            toDelete = null
        }
    }
}

// ---------------- Savings ----------------

@Composable
fun SavingsScreen(vm: MainViewModel, cur: Currency) {
    val goals by vm.goals.collectAsState()
    var amountFor by remember { mutableStateOf<Pair<GoalEntity, Boolean>?>(null) }
    var toDelete by remember { mutableStateOf<GoalEntity?>(null) }
    val total = goals.sumOf { it.saved }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { SummaryCard("Total savings", fmt(cur, total), MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth()) }
        if (goals.isEmpty()) item { Text("No savings yet. Tap + to create one.") }
        items(goals, key = { it.id }) { g ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(g.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { toDelete = g }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    }
                    if (g.target > 0) {
                        LinearProgressIndicator(
                            progress = { (g.saved / g.target).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Saved ${fmt(cur, g.saved)} of ${fmt(cur, g.target)}", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Saved ${fmt(cur, g.saved)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row {
                        TextButton(onClick = { amountFor = Pair(g, true) }) { Text("Deposit") }
                        TextButton(onClick = { amountFor = Pair(g, false) }) { Text("Withdraw") }
                    }
                }
            }
        }
    }

    amountFor?.let { (g, deposit) ->
        GoalAmountDialog(g, deposit, { amountFor = null }) { amt ->
            vm.changeGoal(g, if (deposit) amt else -amt)
            amountFor = null
        }
    }
    toDelete?.let { g ->
        ConfirmDeleteDialog("\"${g.name}\"", { toDelete = null }) {
            vm.deleteGoal(g)
            toDelete = null
        }
    }
}
