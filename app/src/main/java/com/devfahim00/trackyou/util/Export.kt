package com.devfahim00.trackyou.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.devfahim00.trackyou.data.DebtEntity
import com.devfahim00.trackyou.data.DebtType
import com.devfahim00.trackyou.data.GoalEntity
import com.devfahim00.trackyou.data.TxEntity
import com.devfahim00.trackyou.data.TxType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    private fun esc(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n')) "\"${s.replace("\"", "\"\"")}\"" else s

    private fun dateText(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ms))

    fun buildCsv(txs: List<TxEntity>, debts: List<DebtEntity>, goals: List<GoalEntity>): String {
        val sb = StringBuilder()
        sb.append("TrackYou Export\n\n")
        sb.append("TRANSACTIONS\n")
        sb.append("Date,Type,Category,Amount,Note\n")
        txs.forEach { t ->
            sb.append(dateText(t.date)).append(',')
                .append(if (t.type == TxType.INCOME) "Income" else "Expense").append(',')
                .append(esc(t.category)).append(',')
                .append(String.format(Locale.US, "%.2f", t.amount)).append(',')
                .append(esc(t.note)).append('\n')
        }
        sb.append('\n')
        sb.append("DENA-PAONA (LENT/BORROWED)\n")
        sb.append("Date,Type,Person,Amount,Paid,Remaining,Due,Note\n")
        debts.forEach { d ->
            sb.append(dateText(d.date)).append(',')
                .append(if (d.type == DebtType.LENT) "Paona (to get)" else "Dena (to pay)").append(',')
                .append(esc(d.person)).append(',')
                .append(String.format(Locale.US, "%.2f", d.amount)).append(',')
                .append(String.format(Locale.US, "%.2f", d.paid)).append(',')
                .append(String.format(Locale.US, "%.2f", (d.amount - d.paid).coerceAtLeast(0.0))).append(',')
                .append(d.dueDate?.let { dateText(it) } ?: "").append(',')
                .append(esc(d.note)).append('\n')
        }
        sb.append('\n')
        sb.append("SAVINGS GOALS\n")
        sb.append("Created,Name,Target,Saved\n")
        goals.forEach { g ->
            sb.append(dateText(g.createdAt)).append(',')
                .append(esc(g.name)).append(',')
                .append(String.format(Locale.US, "%.2f", g.target)).append(',')
                .append(String.format(Locale.US, "%.2f", g.saved)).append('\n')
        }
        return sb.toString()
    }

    fun write(context: Context, content: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val f = File(dir, "TrackYou_export_$stamp.csv")
        f.writeText(content)
        return f
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "TrackYou data export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share TrackYou export"))
    }
}
