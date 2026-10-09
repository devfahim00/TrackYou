package com.devfahim00.trackyou.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devfahim00.trackyou.MainViewModel
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.TxEntity
import com.devfahim00.trackyou.data.TxType
import com.devfahim00.trackyou.util.Exporter
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

private data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(vm: MainViewModel) {
    val cur = vm.currency ?: return
    val tabs = listOf(
        TabItem("Home", Icons.Rounded.Home),
        TabItem("History", Icons.Rounded.ReceiptLong),
        TabItem("Stats", Icons.Rounded.PieChart),
        TabItem("Dena-Paona", Icons.Rounded.SwapHoriz),
        TabItem("Savings", Icons.Rounded.Savings)
    )
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showTx by remember { mutableStateOf(false) }
    var editTx by remember { mutableStateOf<TxEntity?>(null) }
    var showDebt by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun deleteTxWithUndo(t: TxEntity) {
        vm.deleteTx(t)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Transaction deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) vm.restoreTx(t)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (tab) {
                        3 -> showDebt = true
                        4 -> showGoal = true
                        else -> showTx = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) { Icon(Icons.Rounded.Add, "Add") }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(vm, cur, onSeeAll = { tab = 1 }, onOpenSettings = { showSettings = true })
                1 -> HistoryScreen(vm, cur, onDelete = ::deleteTxWithUndo, onEdit = { editTx = it })
                2 -> StatsScreen(vm, cur)
                3 -> DebtScreen(vm, cur)
                else -> SavingsScreen(vm, cur)
            }
        }
    }

    if (showTx) {
        AddEditTxSheet(tx = null, initialType = TxType.EXPENSE, cur = cur, onDismiss = { showTx = false }) { type, amt, cat, note, date ->
            vm.addTx(type, amt, cat, note, date)
            showTx = false
        }
    }
    editTx?.let { e ->
        AddEditTxSheet(tx = e, initialType = e.type, cur = cur, onDismiss = { editTx = null }) { type, amt, cat, note, date ->
            vm.updateTx(e.copy(type = type, amount = amt, category = cat, note = note, date = date))
            editTx = null
        }
    }
    if (showDebt) {
        AddDebtSheet(onDismiss = { showDebt = false }) { type, person, amt, note ->
            vm.addDebt(type, person, amt, note)
            showDebt = false
        }
    }
    if (showGoal) {
        AddGoalSheet(onDismiss = { showGoal = false }) { name, target, initial ->
            vm.addGoal(name, target, initial)
            showGoal = false
        }
    }
    if (showSettings) {
        SettingsSheet(vm, cur) { showSettings = false }
    }
}

// ---------------- Home ----------------

@Composable
fun HomeScreen(
    vm: MainViewModel,
    cur: Currency,
    onSeeAll: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val txs by vm.transactions.collectAsState()
    var hideBalance by remember { mutableStateOf(false) }
    var addType by remember { mutableStateOf<TxType?>(null) }
    val context = LocalContext.current

    val income = txs.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val balance = income - expense
    val ms = remember { monthStart() }
    val mIncome = txs.filter { it.type == TxType.INCOME && it.date >= ms }.sumOf { it.amount }
    val mExpense = txs.filter { it.type == TxType.EXPENSE && it.date >= ms }.sumOf { it.amount }
    val lastMonth = remember { currentMonth().prev() }
    val lmExpense = txs.filter {
        it.type == TxType.EXPENSE && it.date >= lastMonth.start() && it.date < lastMonth.end()
    }.sumOf { it.amount }
    val deltaPct = if (lmExpense > 0) ((mExpense - lmExpense) / lmExpense) * 100 else null

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InitialsAvatar(vm.userName, size = 44)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        vm.userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Hero balance card
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(HeroBrush)
                    .padding(20.dp)
            ) {
                Box(
                    Modifier
                        .size(130.dp)
                        .background(Color.White.copy(alpha = 0.07f), CircleShape)
                        .align(Alignment.TopEnd)
                )
                Box(
                    Modifier
                        .size(60.dp)
                        .background(Color.White.copy(alpha = 0.09f), CircleShape)
                        .align(Alignment.BottomStart)
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AccountBalanceWallet, null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Total balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { hideBalance = !hideBalance }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (hideBalance) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                "Toggle balance visibility",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (hideBalance) "••••••" else fmt(cur, balance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniStat(
                            icon = Icons.Rounded.ArrowUpward,
                            label = "Income this month",
                            value = if (hideBalance) "•••" else fmt(cur, mIncome),
                            modifier = Modifier.weight(1f)
                        )
                        MiniStat(
                            icon = Icons.Rounded.ArrowDownward,
                            label = "Expense this month",
                            value = if (hideBalance) "•••" else fmt(cur, mExpense),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (deltaPct != null && mExpense > 0) {
                        Spacer(Modifier.height(10.dp))
                        val up = deltaPct > 0
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (up) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                String.format(
                                    java.util.Locale.US,
                                    "%.0f%% %s last month",
                                    abs(deltaPct),
                                    if (up) "more spending" else "less spending"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Quick actions
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Expense", Icons.Rounded.ArrowDownward, expenseColor(), Modifier.weight(1f)) { addType = TxType.EXPENSE }
                QuickAction("Income", Icons.Rounded.ArrowUpward, incomeColor(), Modifier.weight(1f)) { addType = TxType.INCOME }
                QuickAction("Export", Icons.Rounded.Download, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) {
                    runCatching {
                        val file = vm.exportCsv(context)
                        Exporter.share(context, file)
                    }
                }
            }
        }

        item {
            SectionHeader("Recent transactions") {
                TextButton(onClick = onSeeAll) {
                    Text("See all")
                    Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (txs.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "No transactions yet",
                    subtitle = "Tap the + button to record your first income or expense."
                )
            }
        } else {
            items(txs.take(5), key = { it.id }) { t ->
                TxRow(t, cur)
            }
        }
    }

    addType?.let { type ->
        AddEditTxSheet(tx = null, initialType = type, cur = cur, onDismiss = { addType = null }) { t, amt, cat, note, date ->
            vm.addTx(t, amt, cat, note, date)
            addType = null
        }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(22.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
        }
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
