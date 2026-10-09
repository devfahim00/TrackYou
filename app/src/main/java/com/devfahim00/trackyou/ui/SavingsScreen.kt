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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.devfahim00.trackyou.data.GoalEntity

@Composable
fun SavingsScreen(vm: MainViewModel, cur: Currency) {
    val goals by vm.goals.collectAsState()
    var amountFor by remember { mutableStateOf<Pair<GoalEntity, Boolean>?>(null) }
    var toDelete by remember { mutableStateOf<GoalEntity?>(null) }
    val total = goals.sumOf { it.saved }

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
                    "Savings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Total savings hero
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(HeroBrushSoft)
                    .padding(20.dp)
            ) {
                Box(
                    Modifier
                        .size(110.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .align(Alignment.TopEnd)
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Savings, null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Total saved",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        animatedValue(cur, total),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${goals.size} goal${if (goals.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Savings,
                    title = "No savings goals",
                    subtitle = "Create a goal like \"Emergency fund\" and grow it with deposits."
                )
            }
        }

        items(goals, key = { it.id }) { g ->
            Box(Modifier.animateItem()) {
                GoalCard(g, cur, onChange = { deposit -> amountFor = Pair(g, deposit) }, onDelete = { toDelete = g })
            }
        }
    }

    amountFor?.let { (g, deposit) ->
        GoalAmountSheet(g, deposit, { amountFor = null }) { amt ->
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

@Composable
private fun GoalCard(
    g: GoalEntity,
    cur: Currency,
    onChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (g.target > 0) (g.saved / g.target).toFloat().coerceIn(0f, 1f) else 0f

    ModernCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Savings, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        g.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (g.target > 0) {
                        Text(
                            "${(progress * 100).toInt()}% of ${fmtShort(cur, g.target)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "No target set",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    fmtShort(cur, g.saved),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (g.target > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onChange(true) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Deposit", style = MaterialTheme.typography.labelMedium) }
                OutlinedButton(
                    onClick = { onChange(false) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Withdraw", style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}
