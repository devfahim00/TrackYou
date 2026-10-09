package com.devfahim00.trackyou

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devfahim00.trackyou.data.AppDatabase
import com.devfahim00.trackyou.data.Currency
import com.devfahim00.trackyou.data.DebtEntity
import com.devfahim00.trackyou.data.DebtType
import com.devfahim00.trackyou.data.GoalEntity
import com.devfahim00.trackyou.data.Prefs
import com.devfahim00.trackyou.data.TxEntity
import com.devfahim00.trackyou.data.TxType
import com.devfahim00.trackyou.data.currencies
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val prefs = Prefs(app)

    var userName by mutableStateOf(prefs.name)
        private set
    var currency by mutableStateOf<Currency?>(currencies.firstOrNull { it.code == prefs.currencyCode })
        private set

    val transactions: StateFlow<List<TxEntity>> =
        db.txDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts: StateFlow<List<DebtEntity>> =
        db.debtDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val goals: StateFlow<List<GoalEntity>> =
        db.goalDao().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveProfile(name: String, c: Currency) {
        prefs.name = name.trim()
        prefs.currencyCode = c.code
        userName = name.trim()
        currency = c
    }

    fun addTx(type: TxType, amount: Double, category: String, note: String) {
        viewModelScope.launch {
            db.txDao().insert(TxEntity(type = type, amount = amount, category = category, note = note, date = System.currentTimeMillis()))
        }
    }

    fun deleteTx(t: TxEntity) { viewModelScope.launch { db.txDao().delete(t) } }

    fun addDebt(type: DebtType, person: String, amount: Double, note: String) {
        viewModelScope.launch {
            db.debtDao().insert(DebtEntity(type = type, person = person, amount = amount, note = note, date = System.currentTimeMillis()))
        }
    }

    fun payDebt(d: DebtEntity, amount: Double) {
        viewModelScope.launch {
            db.debtDao().update(d.copy(paid = minOf(d.amount, d.paid + amount)))
        }
    }

    fun deleteDebt(d: DebtEntity) { viewModelScope.launch { db.debtDao().delete(d) } }

    fun addGoal(name: String, target: Double, initial: Double) {
        viewModelScope.launch {
            db.goalDao().insert(GoalEntity(name = name, target = target, saved = initial, createdAt = System.currentTimeMillis()))
        }
    }

    fun changeGoal(g: GoalEntity, delta: Double) {
        viewModelScope.launch {
            db.goalDao().update(g.copy(saved = maxOf(0.0, g.saved + delta)))
        }
    }

    fun deleteGoal(g: GoalEntity) { viewModelScope.launch { db.goalDao().delete(g) } }
}
