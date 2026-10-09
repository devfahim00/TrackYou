package com.devfahim00.trackyou.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.currencies
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val expenseCategories = listOf("Food", "Transport", "Shopping", "Bills", "Rent", "Health", "Education", "Entertainment", "Other")
val incomeCategories = listOf("Salary", "Business", "Freelance", "Gift", "Other")

fun fmt(c: Currency, v: Double): String = c.symbol + String.format(Locale.US, "%,.2f", v)

fun dateText(ms: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))

fun monthStart(): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.DAY_OF_MONTH, 1)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

@Composable
fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AmountField(value: String, label: String = "Amount", onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d{0,2}$"))) onChange(v) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun CurrencyList(selected: Currency?, onSelect: (Currency) -> Unit) {
    Column {
        currencies.forEach { c ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(c) }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected?.code == c.code, onClick = { onSelect(c) })
                Text("${c.symbol.trim()}  ${c.code} - ${c.name}")
            }
        }
    }
}
