package com.devfahim00.trackyou.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
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
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY date DESC, id DESC")
    fun all(): Flow<List<DebtEntity>>
    @Insert suspend fun insert(d: DebtEntity)
    @Update suspend fun update(d: DebtEntity)
    @Delete suspend fun delete(d: DebtEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC, id DESC")
    fun all(): Flow<List<GoalEntity>>
    @Insert suspend fun insert(g: GoalEntity)
    @Update suspend fun update(g: GoalEntity)
    @Delete suspend fun delete(g: GoalEntity)
}

@Database(
    entities = [TxEntity::class, DebtEntity::class, GoalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun txDao(): TxDao
    abstract fun debtDao(): DebtDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "trackyou.db"
            ).build().also { instance = it }
        }
    }
}
