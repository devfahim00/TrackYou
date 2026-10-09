package com.devfahim00.trackyou.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

enum class TxType { INCOME, EXPENSE }

/** LENT = paona (you will get), BORROWED = dena (you owe) */
enum class DebtType { LENT, BORROWED }

@Entity(tableName = "transactions")
data class TxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amount: Double,
    val category: String,
    val note: String,
    val date: Long
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: DebtType,
    val person: String,
    val amount: Double,
    val paid: Double = 0.0,
    val note: String,
    val date: Long,
    val dueDate: Long? = null
)

/** A single repayment against a debt (person history timeline). */
@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Double,
    val note: String = "",
    val date: Long
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Double,
    val saved: Double,
    val createdAt: Long
)

@Dao
interface TxDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun all(): Flow<List<TxEntity>>
    @Insert suspend fun insert(t: TxEntity)
    @Update suspend fun update(t: TxEntity)
    @Delete suspend fun delete(t: TxEntity)
    @Query("SELECT COUNT(*) FROM transactions WHERE date >= :from AND date < :to")
    suspend fun countBetween(from: Long, to: Long): Int
    @Query("DELETE FROM transactions")
    suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TxEntity>)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY date DESC, id DESC")
    fun all(): Flow<List<DebtEntity>>
    @Insert suspend fun insert(d: DebtEntity)
    @Update suspend fun update(d: DebtEntity)
    @Delete suspend fun delete(d: DebtEntity)
    @Query("SELECT * FROM debts")
    suspend fun allOnce(): List<DebtEntity>
    @Query("DELETE FROM debts")
    suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<DebtEntity>)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC")
    fun all(): Flow<List<PaymentEntity>>
    @Insert suspend fun insert(p: PaymentEntity)
    @Delete suspend fun delete(p: PaymentEntity)
    @Query("DELETE FROM payments")
    suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<PaymentEntity>)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC, id DESC")
    fun all(): Flow<List<GoalEntity>>
    @Insert suspend fun insert(g: GoalEntity)
    @Update suspend fun update(g: GoalEntity)
    @Delete suspend fun delete(g: GoalEntity)
    @Query("DELETE FROM goals")
    suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<GoalEntity>)
}

@Database(
    entities = [TxEntity::class, DebtEntity::class, GoalEntity::class, PaymentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun txDao(): TxDao
    abstract fun debtDao(): DebtDao
    abstract fun goalDao(): GoalDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v1 -> v2: debts.dueDate column + payments table. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN dueDate INTEGER DEFAULT NULL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `debtId` INTEGER NOT NULL, `amount` REAL NOT NULL, `note` TEXT NOT NULL, `date` INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "trackyou.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
