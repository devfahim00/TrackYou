package com.devfahim00.trackyou.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devfahim00.trackyou.MainViewModel
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.DebtEntity
import com.devfahim00.trackyou.data.DebtType
import com.devfahim00.trackyou.data.PaymentEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class DebtFilter { ALL, TO_GET, TO_PAY }

private val amber = Color(0xFFF59E0B)

private fun startOfDay(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private enum class DueStatus { OVERDUE, TODAY, UPCOMING, NONE }

private fun dueStatus(due: Long?): DueStatus {
    if (due == null) return DueStatus.NONE
    val day = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val today = day.format(Date())
    return when {
        due < startOfDay() -> DueStatus.OVERDUE
        day.format(Date(due)) == today -> DueStatus.TODAY
        else -> DueStatus.UPCOMING
    }
}

@Composable
fun DebtScreen(vm: MainViewModel, cur: Currency) {
    val debts by vm.debts.collectAsState()
    var filter by remember { mutableStateOf(DebtFilter.ALL) }
    var payFor by remember { mutableStateOf<DebtEntity?>(null) }
    var toDelete by remember { mutableStateOf<DebtEntity?>(null) }
    var personKey by remember { mutableStateOf<String?>(null) }

    if (personKey != null) {
        PersonHistoryScreen(vm, cur, personKey!!, onBack = { personKey = null })
        return
    }

    val active = debts.filter { (it.amount - it.paid) >= 0.005 }
    val toGet = active.filter { it.type == DebtType.LENT }.sumOf { it.amount - it.paid }
    val toPay = active.filter { it.type == DebtType.BORROWED }.sumOf { it.amount - it.paid }
    val shown = when (filter) {
        DebtFilter.ALL -> debts
        DebtFilter.TO_GET -> debts.filter { it.type == DebtType.LENT }
        DebtFilter.TO_PAY -> debts.filter { it.type == DebtType.BORROWED }
    }

    // person-wise summary of active records
    val byPerson = active
        .groupBy { it.person.trim().lowercase() }
        .map { (key, list) ->
            PersonSummary(
                key = key,
                display = list.first().person,
                records = list.size,
                lent = list.filter { it.type == DebtType.LENT }.sumOf { it.amount - it.paid },
                borrowed = list.filter { it.type == DebtType.BORROWED }.sumOf { it.amount - it.paid }
            )
        }
        .sortedByDescending { it.lent + it.borrowed }

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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Paona (you'll get)", animatedValueShort(cur, toGet), Icons.Rounded.ArrowUpward, incomeColor(), Modifier.weight(1f))
                StatCard("Dena (you owe)", animatedValueShort(cur, toPay), Icons.Rounded.ArrowDownward, expenseColor(), Modifier.weight(1f))
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

        if (byPerson.isNotEmpty()) {
            item { SectionHeader("By person") }
            items(byPerson, key = { it.key }) { p ->
                Box(Modifier.animateItem()) {
                    PersonCard(p, cur, onClick = { personKey = p.key })
                }
            }
        }

        item { SectionHeader("All records") }

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
            Box(Modifier.animateItem()) {
                DebtCard(d, cur, onPay = { payFor = d }, onDelete = { toDelete = d })
            }
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

private data class PersonSummary(
    val key: String,
    val display: String,
    val records: Int,
    val lent: Double,
    val borrowed: Double
)

@Composable
private fun PersonCard(p: PersonSummary, cur: Currency, onClick: () -> Unit) {
    val net = p.lent - p.borrowed
    ModernCard(onClick = onClick) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(p.display, size = 42)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.display,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$p.recordCountLabel - tap for full history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    fmtShort(cur, kotlin.math.abs(net)),
                    color = if (net >= 0) incomeColor() else expenseColor(),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (net >= 0) "to get" else "to pay",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private val PersonSummary.recordCountLabel: String
    get() = "$records active record" + if (records != 1) "s" else ""

// ---------------- Person full history ----------------

@Composable
private fun PersonHistoryScreen(vm: MainViewModel, cur: Currency, personKey: String, onBack: () -> Unit) {
    val debts by vm.debts.collectAsState()
    val payments by vm.payments.collectAsState()

    val personDebts = debts.filter { it.person.trim().equals(personKey, ignoreCase = true) }
    val display = personDebts.firstOrNull()?.person ?: personKey
    val debtById = personDebts.associateBy { it.id }

    val lent = personDebts.filter { it.type == DebtType.LENT }.sumOf { it.amount - it.paid }
    val borrowed = personDebts.filter { it.type == DebtType.BORROWED }.sumOf { it.amount - it.paid }
    val net = lent - borrowed

    // chronological timeline: debt events + repayment events
    data class HistRow(val date: Long, val debt: DebtEntity?, val payment: PaymentEntity?)

    val events = buildList {
        personDebts.forEach { add(HistRow(it.date, it, null)) }
        payments.forEach { p ->
            val d = debtById[p.debtId]
            if (d != null) add(HistRow(p.date, d, p))
        }
    }.sortedByDescending { it.date }

    var payFor by remember { mutableStateOf<DebtEntity?>(null) }

    BackHandler { onBack() }

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
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ChevronLeft, "Back")
                }
                InitialsAvatar(display, size = 38)
                Spacer(Modifier.width(10.dp))
                Text(
                    display,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Net position hero
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
                        .size(110.dp)
                        .background(Color.White.copy(alpha = 0.07f), CircleShape)
                        .align(Alignment.TopEnd)
                )
                Column {
                    Text(
                        "Net position",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        (if (net >= 0) "+" else "-") + animatedValue(cur, kotlin.math.abs(net)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (net >= 0) "${display.trim()} owes you this amount"
                        else "You owe ${display.trim()} this amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniHistStat("Paona (to get)", animatedValueShort(cur, lent), Modifier.weight(1f))
                        MiniHistStat("Dena (to pay)", animatedValueShort(cur, borrowed), Modifier.weight(1f))
                    }
                }
            }
        }

        item { SectionHeader("Full history (${events.size})") }

        if (events.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.SwapHoriz,
                    title = "Nothing recorded",
                    subtitle = "All records and payments with ${display.trim()} will appear here."
                )
            }
        }

        items(events, key = { row -> val p = row.payment; if (p != null) "p${p.id}" else "d${row.debt?.id}" }) { e ->
            Box(Modifier.animateItem()) {
                HistEventRow(
                    date = e.date,
                    debt = e.debt,
                    payment = e.payment,
                    cur = cur,
                    onPay = {
                        val d = e.debt
                        if (d != null && (d.amount - d.paid) >= 0.005) payFor = d
                    }
                )
            }
        }
    }

    payFor?.let { d ->
        PayDebtSheet(d, (d.amount - d.paid).coerceAtLeast(0.0), { payFor = null }) { amt ->
            vm.payDebt(d, amt)
            payFor = null
        }
    }
}

@Composable
private fun MiniHistStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(20.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (label.startsWith("Paona")) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    null, tint = Color.White, modifier = Modifier.size(11.dp)
                )
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
        }
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun HistEventRow(
    date: Long,
    debt: DebtEntity?,
    payment: PaymentEntity?,
    cur: Currency,
    onPay: () -> Unit
) {
    val d = debt ?: return
    val pay = payment
    val lent = d.type == DebtType.LENT
    val tint = if (lent) incomeColor() else expenseColor()
    val isPayment = pay != null
    val remaining = (d.amount - d.paid).coerceAtLeast(0.0)
    val clickable = !isPayment && remaining >= 0.005

    ModernCard(onClick = if (clickable) onPay else null) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(
                        if (isPayment) incomeColor().copy(alpha = 0.14f) else tint.copy(alpha = 0.14f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPayment) Icons.Rounded.Check else Icons.Rounded.SwapHoriz,
                    null,
                    tint = if (isPayment) incomeColor() else tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (pay != null) {
                    Text(
                        if (lent) "Received ${fmt(cur, pay.amount)}" else "Paid ${fmt(cur, pay.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        dateText(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        if (lent) "Lent ${fmt(cur, d.amount)}" else "Borrowed ${fmt(cur, d.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (d.note.isBlank()) dateText(date) else "${d.note} - ${dateText(date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    d.dueDate?.let {
                        Spacer(Modifier.height(4.dp))
                        DueChip(it)
                    }
                }
            }
            if (!isPayment) {
                if (remaining < 0.005) {
                    Text(
                        "Settled",
                        color = incomeColor(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            fmtShort(cur, remaining),
                            color = tint,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "left - tap to settle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------- Debt card ----------------

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    dateText(d.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                d.dueDate?.let { DueChip(it) }
            }
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

@Composable
private fun DueChip(due: Long) {
    val status = dueStatus(due)
    val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
    val (bg, fg, label) = when (status) {
        DueStatus.OVERDUE -> Triple(expenseColor().copy(alpha = 0.12f), expenseColor(), "Overdue ${fmt.format(Date(due))}")
        DueStatus.TODAY -> Triple(amber.copy(alpha = 0.15f), amber, "Due today")
        DueStatus.UPCOMING -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Due ${fmt.format(Date(due))}"
        )
        DueStatus.NONE -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "")
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(Icons.Rounded.Event, null, tint = fg, modifier = Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.SemiBold)
    }
}
