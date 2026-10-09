package com.devfahim00.trackyou.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devfahim00.trackyou.MainViewModel
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.DebtEntity
import com.devfahim00.trackyou.data.DebtType

private enum class DebtFilter { ALL, TO_GET, TO_PAY }

@Composable
fun DebtScreen(vm: MainViewModel, cur: Currency) {
    val debts by vm.debts.collectAsState()
    var filter by remember { mutableStateOf(DebtFilter.ALL) }
    var payFor by remember { mutableStateOf<DebtEntity?>(null) }
    var toDelete by remember { mutableStateOf<DebtEntity?>(null) }

    val active = debts.filter { (it.amount - it.paid) >= 0.005 }
    val toGet = active.filter { it.type == DebtType.LENT }.sumOf { it.amount - it.paid }
    val toPay = active.filter { it.type == DebtType.BORROWED }.sumOf { it.amount - it.paid }
    val shown = when (filter) {
        DebtFilter.ALL -> debts
        DebtFilter.TO_GET -> debts.filter { it.type == DebtType.LENT }
        DebtFilter.TO_PAY -> debts.filter { it.type == DebtType.BORROWED }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Dena-Paona",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Net position card
        item {
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Paona (you'll get)", fmtShort(cur, toGet), Icons.Rounded.ArrowUpward, incomeColor(), Modifier.weight(1f))
                    StatCard("Dena (you owe)", fmtShort(cur, toPay), Icons.Rounded.ArrowDownward, expenseColor(), Modifier.weight(1f))
                }
            }
        }

        // Filter chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == DebtFilter.ALL, onClick = { filter = DebtFilter.ALL }, label = { Text("All") })
                FilterChip(selected = filter == DebtFilter.TO_GET, onClick = { filter = DebtFilter.TO_GET }, label = { Text("Paona") })
                FilterChip(selected = filter == DebtFilter.TO_PAY, onClick = { filter = DebtFilter.TO_PAY }, label = { Text("Dena") })
            }
        }

        if (shown.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.SwapHoriz,
                    title = "No records",
                    subtitle = "Keep track of money you lent or borrowed. Tap + to add one."
                )
            }
        }

        items(shown, key = { it.id }) { d ->
            DebtCard(d, cur, onPay = { payFor = d }, onDelete = { toDelete = d })
        }
    }

    payFor?.let { d ->
        PayDebtSheet(d, (d.amount - d.paid).coerceAtLeast(0.0), { payFor = null }) { amt ->
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

@Composable
private fun DebtCard(d: DebtEntity, cur: Currency, onPay: () -> Unit, onDelete: () -> Unit) {
    val remaining = (d.amount - d.paid).coerceAtLeast(0.0)
    val settled = remaining < 0.005
    val lent = d.type == DebtType.LENT
    val tint = if (lent) incomeColor() else expenseColor()

    ModernCard {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(d.person, size = 42)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        d.person,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (lent) "Paona - you'll get" else "Dena - you owe",
                        color = tint,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    fmtShort(cur, remaining),
                    color = tint,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (d.note.isNotBlank()) {
                Text(
                    d.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                dateText(d.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (d.paid / d.amount).toFloat().coerceIn(0f, 1f) },
                color = tint,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Total ${fmtShort(cur, d.amount)}  |  Left ${fmtShort(cur, remaining)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (settled) {
                    Text("Settled", color = incomeColor(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                } else {
                    TextButton(onClick = onPay) {
                        Text(if (lent) "Received" else "Paid")
                    }
                }
            }
        }
    }
}
