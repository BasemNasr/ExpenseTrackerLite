package com.bn.bassemexpensetrackerlite.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Long)

    @Query(
        "SELECT * FROM expenses WHERE (:start IS NULL OR dateEpochMillis >= :start) AND (:end IS NULL OR dateEpochMillis < :end) ORDER BY dateEpochMillis DESC LIMIT :limit OFFSET :offset"
    )
    fun getExpensesPaged(start: Long?, end: Long?, limit: Int, offset: Int): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT SUM(CASE WHEN isIncome THEN amountUsd ELSE 0 END) FROM expenses WHERE (:start IS NULL OR dateEpochMillis >= :start) AND (:end IS NULL OR dateEpochMillis < :end)"
    )
    fun totalIncomeUsd(start: Long?, end: Long?): Flow<Double?>

    @Query(
        "SELECT SUM(CASE WHEN isIncome THEN 0 ELSE amountUsd END) FROM expenses WHERE (:start IS NULL OR dateEpochMillis >= :start) AND (:end IS NULL OR dateEpochMillis < :end)"
    )
    fun totalExpenseUsd(start: Long?, end: Long?): Flow<Double?>
}


