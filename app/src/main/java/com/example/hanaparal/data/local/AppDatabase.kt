package com.example.hanaparal.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * MODULE 1 — Room Flow Streaming + Conflict Resolution
 *
 * UserEntity: each field maps to a SQLite column.
 * updatedAt: timestamp used for Last-Write-Wins conflict resolution.
 *
 * UserDao.getAll() returns Flow<List<UserEntity>> — this is "observable".
 * Whenever a row changes, Room automatically emits a fresh list.
 * The UI never needs to manually refresh — it just collects the Flow.
 *
 * Comparison from the module:
 *   fun get(): List<User>        → one-shot, manual refresh needed
 *   fun get(): Flow<List<User>>  → subscribe once, gets updates forever
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val course: String,
    val avatar: String,
    val updatedAt: Long = System.currentTimeMillis()  // for conflict resolution
)

@Dao
interface UserDao {
    // Reactive Flow — emits a new list every time any row changes
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAll(): Flow<List<UserEntity>>

    // One-shot suspend query — used inside the SSOT sync function
    @Query("SELECT * FROM users ORDER BY name ASC")
    suspend fun getAllOnce(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getById(id: Int): Flow<UserEntity?>

    // REPLACE resolves primary-key conflicts by overwriting the old row
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}

@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "hanaparal.db")
                    .build().also { INSTANCE = it }
            }
    }
}
