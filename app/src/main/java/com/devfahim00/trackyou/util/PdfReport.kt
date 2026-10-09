package com.devfahim00.trackyou.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.DebtEntity
import com.devfahim00.trackyou.data.DebtType
import com.devfahim00.trackyou.data.TxEntity
import com.devfahim00.trackyou.data.TxType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Monthly PDF report built with the platform PdfDocument API (no external
 * dependencies). Produces a clean multi-page A4 document: header, monthly
 * summary, expense breakdown by category, full transaction list and the
 * active dena-paona position.
 */
object PdfReport {

    private const val PAGE_W = 595 // A4 @72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 42f

    private val indigo = Color.rgb(79, 70, 229)
    private val violet = Color.rgb(124, 58, 237)
    private val incomeC = Color.rgb(5, 150, 105)
    private val expenseC = Color.rgb(220, 38, 38)
    private val dark = Color.rgb(15, 23, 42)
    private val gray = Color.rgb(100, 116, 139)
    private val lightBg = Color.rgb(241, 245, 249)
    private val lineC = Color.rgb(226, 232, 240)

    private fun money(cur: Currency, v: Double): String =
        cur.symbol + String.format(Locale.US, "%,.2f", v)

    fun generate(
        context: Context,
        monthTitle: String,
        from: Long,
        to: Long,
        txs: List<TxEntity>,
        debts: List<DebtEntity>,
        cur: Currency,
        userName: String
    ): File {
        val inMonth = txs.filter { it.date >= from && it.date < to }.sortedByDescending { it.date }
        val income = inMonth.filter { it.type == TxType.INCOME }.sumOf { it.amount }
        val expense = inMonth.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
        val byCat = inMonth.filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
        val active = debts.filter { (it.amount - it.paid) >= 0.005 }

        val doc = PdfDocument()
        val w = Writer(doc)

        // ---- cover header ----
        w.fillGradientHeader(monthTitle, userName)

        // ---- summary ----
        w.sectionTitle("Monthly summary")
        val boxW = (PAGE_W - 2 * MARGIN - 20) / 3f
        val boxY = w.y
        listOf(
            Triple("Income", money(cur, income), incomeC),
            Triple("Expense", money(cur, expense), expenseC),
            Triple("Net saved", money(cur, income - expense), if (income - expense >= 0) incomeC else expenseC)
        ).forEachIndexed { i, (label, value, color) ->
            val x = MARGIN + i * (boxW + 10)
            w.roundRect(x, boxY, boxW, 46f, 8f, lightBg)
            w.text(label, x + 10, boxY + 16, 8f, gray, bold = false)
            w.text(value, x + 10, boxY + 34, 11f, color, bold = true, maxChars = 16)
        }
        w.y = boxY + 60

        // ---- expense by category ----
        if (byCat.isNotEmpty()) {
            w.sectionTitle("Expenses by category")
            byCat.take(10).forEach { (cat, amt) ->
                if (w.needsSpace(26f)) w.newPage(monthTitle)
                val pct = if (expense > 0) (amt / expense).toFloat() else 0f
                w.text(cat, MARGIN, w.y + 11, 10f, dark, bold = true)
                val amtStr = money(cur, amt)
                w.textRight(amtStr, PAGE_W - MARGIN - 120f, w.y + 11, 10f, dark)
                w.textRight(
                    String.format(Locale.US, "%.0f%%", pct * 100),
                    PAGE_W - MARGIN, w.y + 11, 10f, gray
                )
                w.rect(MARGIN, w.y + 16, (PAGE_W - 2 * MARGIN) * pct, 6f, expenseC)
                w.rect(MARGIN + (PAGE_W - 2 * MARGIN) * pct, w.y + 16, (PAGE_W - 2 * MARGIN) * (1 - pct), 6f, lineC)
                w.y += 28
            }
            w.y += 8
        }

        // ---- transactions ----
        w.sectionTitle("Transactions (${inMonth.size})")
        if (inMonth.isEmpty()) {
            w.text("No transactions recorded this month.", MARGIN, w.y + 12, 10f, gray)
            w.y += 30
        } else {
            val dateFmt = SimpleDateFormat("dd MMM", Locale.US)
            var lastDay = ""
            inMonth.forEach { t ->
                if (w.needsSpace(24f)) w.newPage(monthTitle)
                val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(t.date))
                if (day != lastDay) {
                    w.text(dateFmt.format(Date(t.date)), MARGIN, w.y + 11, 10f, gray, bold = true)
                    lastDay = day
                } else {
                    w.text("", MARGIN, w.y + 11, 10f, gray)
                }
                val label = if (t.note.isBlank()) t.category else "${t.category} - ${t.note}"
                w.text(label, MARGIN + 60, w.y + 11, 10f, dark, maxChars = 52)
                val sign = if (t.type == TxType.INCOME) "+" else "-"
                w.textRight(
                    sign + money(cur, t.amount).let { if (it.length > 18) it.take(18) else it },
                    PAGE_W - MARGIN, w.y + 11, 10f,
                    if (t.type == TxType.INCOME) incomeC else expenseC
                )
                w.hairline(w.y + 16)
                w.y += 24
            }
        }

        // ---- dena-paona position ----
        if (active.isNotEmpty()) {
            w.y += 6
            if (w.needsSpace(40f)) w.newPage(monthTitle)
            w.sectionTitle("Active dena-paona (as of report date)")
            val dateFmt2 = SimpleDateFormat("dd MMM yyyy", Locale.US)
            active.take(20).forEach { d ->
                if (w.needsSpace(24f)) w.newPage(monthTitle)
                val lent = d.type == DebtType.LENT
                val rem = d.amount - d.paid
                w.text(
                    d.person, MARGIN, w.y + 11, 10f, dark, bold = true, maxChars = 24
                )
                val sub = (if (lent) "Paona (you'll get)" else "Dena (you owe)") +
                    (d.dueDate?.let { " - due ${dateFmt2.format(Date(it))}" } ?: "")
                w.text(sub, MARGIN + 150, w.y + 11, 9f, gray, maxChars = 34)
                w.textRight(money(cur, rem).let { if (it.length > 16) it.take(16) else it }, PAGE_W - MARGIN, w.y + 11, 10f, if (lent) incomeC else expenseC)
                w.hairline(w.y + 16)
                w.y += 24
            }
        }

        w.finish(monthTitle)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = monthTitle.replace(Regex("[^A-Za-z0-9]+"), "_")
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val f = File(dir, "TrackYou_Report_${safe}_$stamp.pdf")
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
        return f
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "TrackYou monthly report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share monthly report"))
    }

    // ---------------- low-level drawing ----------------

    private class Writer(val doc: PdfDocument) {
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f
        var pageNo = 0

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun needsSpace(h: Float): Boolean = page == null || y + h > PAGE_H - 64

        fun newPage(monthTitle: String) {
            stampFooter()
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            page = p
            canvas = p.canvas
            // slim continuation header
            fillRect(0f, 0f, PAGE_W.toFloat(), 34f, indigo)
            text("TrackYou - $monthTitle (cont.)", MARGIN, 22f, 11f, Color.WHITE, bold = true)
            y = 56f
        }

        fun finish(monthTitle: String) {
            stampFooter()
            page?.let { doc.finishPage(it) }
            page = null
            canvas = null
        }

        private fun stampFooter() {
            val c = canvas ?: return
            val p = paint
            p.reset(); p.isAntiAlias = true
            p.color = gray; p.textSize = 8f
            c.drawText("Generated by TrackYou - offline expense tracker", MARGIN, PAGE_H - 28f, p)
            p.textAlign = Paint.Align.RIGHT
            c.drawText("Page $pageNo", PAGE_W - MARGIN, PAGE_H - 28f, p)
            p.textAlign = Paint.Align.LEFT
        }

        fun fillGradientHeader(monthTitle: String, userName: String) {
            newPageSilent()
            fillRect(0f, 0f, PAGE_W.toFloat(), 96f, indigo)
            fillRect(0f, 96f, PAGE_W.toFloat(), 5f, violet)
            text("TrackYou", MARGIN, 40f, 20f, Color.WHITE, bold = true)
            text("Monthly Report - $monthTitle", MARGIN, 64f, 13f, Color.WHITE)
            val who = if (userName.isBlank()) "" else "for $userName - "
            textRight(
                who + SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date()),
                PAGE_W - MARGIN, 64f, 9f, Color.WHITE
            )
            y = 124f
        }

        private fun newPageSilent() {
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            page = p
            canvas = p.canvas
        }

        fun sectionTitle(s: String) {
            if (needsSpace(40f)) newPage("")
            paint.reset(); paint.isAntiAlias = true
            paint.color = indigo; paint.textSize = 13f; paint.isFakeBoldText = true
            canvas?.drawText(s, MARGIN, y + 6, paint)
            paint.color = lineC
            canvas?.drawRect(MARGIN, y + 12, PAGE_W - MARGIN, y + 13, paint)
            y += 28f
        }

        fun text(
            s: String, x: Float, y: Float, size: Float, color: Int,
            bold: Boolean = false, maxChars: Int = Int.MAX_VALUE
        ) {
            val c = canvas ?: return
            val shown = if (s.length > maxChars) s.take(maxChars - 1) + "..." else s
            paint.reset(); paint.isAntiAlias = true
            paint.color = color; paint.textSize = size; paint.isFakeBoldText = bold
            c.drawText(shown, x, y, paint)
        }

        fun textRight(s: String, xRight: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
            val c = canvas ?: return
            paint.reset(); paint.isAntiAlias = true
            paint.color = color; paint.textSize = size; paint.isFakeBoldText = bold
            paint.textAlign = Paint.Align.RIGHT
            c.drawText(s, xRight, y, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        fun fillRect(l: Float, t: Float, r: Float, b: Float, color: Int) {
            val c = canvas ?: return
            paint.reset(); paint.isAntiAlias = true
            paint.color = color
            c.drawRect(l, t, r, b, paint)
        }

        fun rect(l: Float, t: Float, w: Float, h: Float, color: Int) {
            val c = canvas ?: return
            paint.reset(); paint.isAntiAlias = true
            paint.color = color
            c.drawRect(l, t, l + w, t + h, paint)
        }

        fun roundRect(l: Float, t: Float, w: Float, h: Float, r: Float, color: Int) {
            val c = canvas ?: return
            paint.reset(); paint.isAntiAlias = true
            paint.color = color
            c.drawRoundRect(RectF(l, t, l + w, t + h), r, r, paint)
        }

        fun hairline(yBelow: Float) {
            paint.reset(); paint.isAntiAlias = true
            paint.color = lineC
            canvas?.drawRect(MARGIN, yBelow, PAGE_W - MARGIN, yBelow + 0.7f, paint)
        }
    }
}
