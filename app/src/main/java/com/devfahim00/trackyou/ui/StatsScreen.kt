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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devfahim00.trackyou.MainViewModel
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.TxType
import com.devfahim00.trackyou.util.PdfReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StatsScreen(vm: MainViewModel, cur: Currency) {
    val txs by vm.transactions.collectAsState()
    var month by remember { mutableStateOf(currentMonth()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun exportPdf() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val file = vm.exportPdf(context, month.title(), month.start(), month.end())
                withContext(Dispatchers.Main) { PdfReport.share(context, file) }
            }
        }
    }

    val inMonth = txs.filter { it.date >= month.start() && it.date < month.end() }
    val mIncome = inMonth.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val mExpense = inMonth.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val byCategory = inMonth
        .filter { it.type == TxType.EXPENSE }
        .groupBy { it.category }
        .map { (cat, list) -> cat to list.sumOf { it.amount } }
        .sortedByDescending { it.second }
    val totalExpense = byCategory.sumOf { it.second }
    val atCurrent = month.index() >= currentMonth().index()

    // last 6 months ending at selected month
    val months = (5 downTo 0).map { month.back(it) }
    val incByMonth = months.map { m ->
        txs.filter { it.type == TxType.INCOME && it.date >= m.start() && it.date < m.end() }.sumOf { it.amount }.toFloat()
    }
    val expByMonth = months.map { m ->
        txs.filter { it.type == TxType.EXPENSE && it.date >= m.start() && it.date < m.end() }.sumOf { it.amount }.toFloat()
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Stats",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { exportPdf() }) {
                    Icon(
                        Icons.Rounded.PictureAsPdf,
                        "Export PDF report for ${month.title()}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            MonthSelector(
                title = month.title(),
                nextEnabled = !atCurrent,
                onPrev = { month = month.prev() },
                onNext = { month = month.next() }
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Income", animatedValueShort(cur, mIncome), Icons.Rounded.ArrowUpward, incomeColor(), Modifier.weight(1f))
                StatCard("Expense", animatedValueShort(cur, mExpense), Icons.Rounded.ArrowDownward, expenseColor(), Modifier.weight(1f))
                StatCard(
                    "Net",
                    animatedValueShort(cur, mIncome - mExpense),
                    Icons.Rounded.Paid,
                    if (mIncome - mExpense >= 0) incomeColor() else expenseColor(),
                    Modifier.weight(1f)
                )
            }
        }

        // Expense by category - donut
        item {
            ModernCard {
                Column(Modifier.padding(18.dp)) {
                    Text("Expenses by category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    if (totalExpense <= 0.0) {
                        EmptyState(
                            icon = Icons.Rounded.PieChart,
                            title = "No expenses",
                            subtitle = "Record some expenses in ${month.title()} to see the breakdown."
                        )
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DonutChart(
                                segments = byCategory.map { (cat, amt) -> Pair(amt.toFloat(), categoryTint(cat)) },
                                modifier = Modifier.size(150.dp),
                                stroke = 22.dp
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        animatedValueShort(cur, totalExpense),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.size(14.dp))
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                byCategory.take(5).forEach { (cat, amt) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LegendDot(categoryTint(cat))
                                        Spacer(Modifier.size(8.dp))
                                        Text(
                                            cat,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            if (totalExpense > 0) "${(amt / totalExpense * 100).toInt()}%" else "0%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (byCategory.size > 5) {
                                    Text(
                                        "+${byCategory.size - 5} more",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        // Progress bars
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            byCategory.take(5).forEach { (cat, amt) ->
                                Column {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(cat, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                        Text(
                                            fmtShort(cur, amt),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = {
                                            if (totalExpense > 0) (amt / totalExpense).toFloat().coerceIn(0f, 1f) else 0f
                                        },
                                        color = categoryTint(cat),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth().height(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6-month trend
        item {
            ModernCard {
                Column(Modifier.padding(18.dp)) {
                    Text("Last 6 months", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    MonthBarsChart(
                        labels = months.map { it.shortLabel() },
                        income = incByMonth,
                        expense = expByMonth,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    ChartLegend()
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Income vs expense for the 6 months ending ${month.title()}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
