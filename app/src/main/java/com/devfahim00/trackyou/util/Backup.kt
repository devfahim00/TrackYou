package com.devfahim00.trackyou.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.devfahim00.trackyou.data.AppDatabase
import com.devfahim00.trackyou.data.DebtEntity
import com.devfahim00.trackyou.data.DebtType
import com.devfahim00.trackyou.data.GoalEntity
import com.devfahim00.trackyou.data.PaymentEntity
import com.devfahim00.trackyou.data.TxEntity
import com.devfahim00.trackyou.data.TxType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-app JSON backup.
 *
 * Backups are written to two places:
 *  1. The user-selected SAF folder - which can be a Google Drive folder (Drive
 *     app installed) or any device folder - so the file syncs to the cloud.
 *  2. A local fallback folder inside the app's external files dir, so a recent
 *     copy always exists on-device even when no folder was chosen.
 *
 * Restore reads any backup .json file (picked via SAF, Drive or local) and
 * replaces the whole database in one Room transaction.
 */
object Backup {

    private const val PREFIX = "TrackYou-backup-"
    private const val KEEP = 10

    data class RestoreResult(
        val txCount: Int,
        val debtCount: Int,
        val paymentCount: Int,
        val goalCount: Int,
        val profileName: String?,
        val profileCurrency: String?
    )

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    // ---------------- Export ----------------

    fun snapshot(
        txs: List<TxEntity>,
        debts: List<DebtEntity>,
        payments: List<PaymentEntity>,
        goals: List<GoalEntity>,
        name: String,
        currencyCode: String,
        themeName: String
    ): String {
        val root = JSONObject()
        root.put("app", "TrackYou")
        root.put("schema", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val profile = JSONObject()
        profile.put("name", name)
        profile.put("currency", currencyCode)
        profile.put("theme", themeName)
        root.put("profile", profile)

        val txArr = JSONArray()
        txs.forEach { t ->
            val o = JSONObject()
            o.put("id", t.id)
            o.put("type", t.type.name)
            o.put("amount", t.amount)
            o.put("category", t.category)
            o.put("note", t.note)
            o.put("date", t.date)
            txArr.put(o)
        }
        root.put("transactions", txArr)

        val debtArr = JSONArray()
        debts.forEach { d ->
            val o = JSONObject()
            o.put("id", d.id)
            o.put("type", d.type.name)
            o.put("person", d.person)
            o.put("amount", d.amount)
            o.put("paid", d.paid)
            o.put("note", d.note)
            o.put("date", d.date)
            val dd = d.dueDate
            if (dd == null) o.put("dueDate", JSONObject.NULL) else o.put("dueDate", dd)
            debtArr.put(o)
        }
        root.put("debts", debtArr)

        val payArr = JSONArray()
        payments.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("debtId", p.debtId)
            o.put("amount", p.amount)
            o.put("note", p.note)
            o.put("date", p.date)
            payArr.put(o)
        }
        root.put("payments", payArr)

        val goalArr = JSONArray()
        goals.forEach { g ->
            val o = JSONObject()
            o.put("id", g.id)
            o.put("name", g.name)
            o.put("target", g.target)
            o.put("saved", g.saved)
            o.put("createdAt", g.createdAt)
            goalArr.put(o)
        }
        root.put("goals", goalArr)

        return root.toString(2)
    }

    // ---------------- Write destinations ----------------

    /** Writes the backup into the selected SAF folder (e.g. Google Drive). Returns the file name or null. */
    fun writeToTree(context: Context, treeUri: Uri, json: String): String? = runCatching {
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val name = "$PREFIX${stamp()}.json"
        val file = dir.createFile("application/json", name) ?: return null
        val out = context.contentResolver.openOutputStream(file.uri, "w") ?: return null
        out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        pruneTree(dir)
        name
    }.getOrNull()

    /** Always-on local fallback copy inside the app's own external files dir. */
    fun writeLocal(context: Context, json: String): String? = runCatching {
        val dir = localDir(context) ?: return null
        if (!dir.exists()) dir.mkdirs()
        val name = "$PREFIX${stamp()}.json"
        File(dir, name).writeText(json)
        File(dir, "latest.json").writeText(json)
        dir.listFiles { f -> f.name.startsWith(PREFIX) }
            ?.sortedByDescending { it.name }
            ?.drop(KEEP)
            ?.forEach { runCatching { it.delete() } }
        name
    }.getOrNull()

    /** Directory that always holds the latest local backups (no permission needed). */
    fun localDir(context: Context): File? =
        context.getExternalFilesDir(null)?.let { File(it, "backups") }

    private fun pruneTree(dir: DocumentFile) {
        runCatching {
            dir.listFiles()
                .filter { it.name?.startsWith(PREFIX) == true }
                .sortedByDescending { it.name ?: "" }
                .drop(KEEP)
                .forEach { f -> runCatching { f.delete() } }
        }
    }

    /** Friendly name of the selected folder, shown in settings. */
    fun folderDisplayName(context: Context, treeUri: Uri): String =
        runCatching { DocumentFile.fromTreeUri(context, treeUri)?.name }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Backup folder"

    // ---------------- Restore ----------------

    /** Reads the picked backup file into a JSON string. */
    fun readText(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open the selected file")
        return stream.use { it.bufferedReader().readText() }
    }

    /**
     * Replaces all database content with the backup, in a single transaction so
     * the DB is never left half-restored. Returns counts + profile info.
     */
    suspend fun restoreInto(json: String, db: AppDatabase): RestoreResult {
        val root = JSONObject(json)
        if (root.optString("app") != "TrackYou") {
            throw IllegalArgumentException("This is not a TrackYou backup file")
        }

        val txs = root.optJSONArray("transactions").mapObjects { o ->
            TxEntity(
                id = o.getLong("id"),
                type = TxType.valueOf(o.getString("type")),
                amount = o.getDouble("amount"),
                category = o.getString("category"),
                note = o.optString("note", ""),
                date = o.getLong("date")
            )
        }
        val debts = root.optJSONArray("debts").mapObjects { o ->
            DebtEntity(
                id = o.getLong("id"),
                type = DebtType.valueOf(o.getString("type")),
                person = o.getString("person"),
                amount = o.getDouble("amount"),
                paid = o.optDouble("paid", 0.0),
                note = o.optString("note", ""),
                date = o.getLong("date"),
                dueDate = if (o.isNull("dueDate")) null else o.optLong("dueDate")
            )
        }
        val payments = root.optJSONArray("payments").mapObjects { o ->
            PaymentEntity(
                id = o.getLong("id"),
                debtId = o.getLong("debtId"),
                amount = o.getDouble("amount"),
                note = o.optString("note", ""),
                date = o.getLong("date")
            )
        }
        val goals = root.optJSONArray("goals").mapObjects { o ->
            GoalEntity(
                id = o.getLong("id"),
                name = o.getString("name"),
                target = o.getDouble("target"),
                saved = o.getDouble("saved"),
                createdAt = o.getLong("createdAt")
            )
        }

        db.withTransaction {
            db.txDao().clear()
            db.debtDao().clear()
            db.paymentDao().clear()
            db.goalDao().clear()
            if (txs.isNotEmpty()) db.txDao().insertAll(txs)
            if (debts.isNotEmpty()) db.debtDao().insertAll(debts)
            if (payments.isNotEmpty()) db.paymentDao().insertAll(payments)
            if (goals.isNotEmpty()) db.goalDao().insertAll(goals)
        }

        val prof = root.optJSONObject("profile")
        fun str(key: String): String? = prof?.optString(key)?.takeIf { it.isNotBlank() && it != "null" }
        return RestoreResult(
            txCount = txs.size,
            debtCount = debts.size,
            paymentCount = payments.size,
            goalCount = goals.size,
            profileName = str("name"),
            profileCurrency = str("currency")
        )
    }

    /** Maps a nullable JSONArray of JSONObjects, skipping corrupt entries. */
    private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            runCatching { out.add(transform(o)) }
        }
        return out
    }
}
