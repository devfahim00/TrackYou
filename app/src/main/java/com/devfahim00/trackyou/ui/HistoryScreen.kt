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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devfahim00.trackyou.MainViewModel
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.TxEntity
import com.devfahim00.trackyou.data.TxType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vm: MainViewModel,
    cur: Currency,
    onDelete: (TxEntity) -> Unit,
    onEdit: (TxEntity) -> Unit
) {
    val txs by vm.transactions.collectAsState()
    var month by remember { mutableStateOf(currentMonth()) }
    var filter by remember { mutableStateOf<TxType?>(null) }
    var query by remember { mutableStateOf("") }

    val inMonth = txs.filter { it.date >= month.start() && it.date < month.end() }
    val mIncome = inMonth.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val mExpense = inMonth.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val shown = inMonth
        .filter { filter == null || it.type == filter }
        .filter {
            query.isBlank() || it.note.contains(query, true) || it.category.contains(query, true)
        }
    val groups = remember(shown) { groupByDay(shown) }
    val atCurrent = month.index() >= currentMonth().index()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { month = currentMonth() }) {
                Icon(Icons.Rounded.CalendarMonth, "Jump to current month")
            }
        }

        // Month selector
        Box(Modifier.padding(horizontal = 16.dp)) {
            MonthSelector(
                title = month.title(),
                nextEnabled = !atCurrent,
                onPrev = { month = month.prev() },
                onNext = { month = month.next() }
            )
        }
        Spacer(Modifier.height(10.dp))

        // Monthly summary
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Income", fmtShort(cur, mIncome), Icons.Rounded.ArrowUpward, incomeColor(), Modifier.weight(1f))
            StatCard("Expense", fmtShort(cur, mExpense), Icons.Rounded.ArrowDownward, expenseColor(), Modifier.weight(1f))
            StatCard(
                "Net",
                fmtShort(cur, mIncome - mExpense),
                Icons.Rounded.Paid,
                if (mIncome - mExpense >= 0) incomeColor() else expenseColor(),
                Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))

        // Search
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search note or category") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear search") }
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Type filter
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
            FilterChip(selected = filter == TxType.INCOME, onClick = { filter = TxType.INCOME }, label = { Text("Income") })
            FilterChip(selected = filter == TxType.EXPENSE, onClick = { filter = TxType.EXPENSE }, label = { Text("Expense") })
        }

        // Grouped list
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (groups.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "No transactions",
                        subtitle = if (query.isNotBlank()) "Nothing matches \"$query\" in ${month.title()}."
                        else "Nothing recorded in ${month.title()}. Add an entry from the Home tab."
                    )
                }
            } else {
                groups.forEach { g ->
                    item(key = "h_${g.label}_${g.txs.first().id}") {
                        DayHeader(g.label, g.dayNet, cur)
                    }
                    items(g.txs, key = { it.id }) { t ->
                        SwipeToDelete(onDelete = { onDelete(t) }) {
                            TxRow(t, cur, showTime = true, onClick = { onEdit(t) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 0.dp)
                    .background(expenseColor(), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Rounded.Delete, "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 20.dp).size(22.dp)
                )
            }
        }
    ) {
        content()
    }
}
