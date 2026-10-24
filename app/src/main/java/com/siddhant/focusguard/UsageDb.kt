package com.siddhant.focusguard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

data class UsageDaily(
    val epochDay: Long,
    val pkg: String,
    val durationMs: Long
)

class UsageDb private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS usage_daily (
                epochDay INTEGER NOT NULL,
                pkg TEXT NOT NULL,
                durationMs INTEGER NOT NULL,
                PRIMARY KEY (epochDay, pkg)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS usage_daily")
        onCreate(db)
    }

    fun recordUsage(epochDay: Long, pkg: String, deltaMs: Long) {
        if (deltaMs <= 0L) return
        val db = writableDatabase
        db.execSQL(
            """
            INSERT INTO usage_daily (epochDay, pkg, durationMs)
            VALUES (?, ?, ?)
            ON CONFLICT(epochDay, pkg) DO UPDATE SET durationMs = durationMs + ?
            """.trimIndent(),
            arrayOf<Any>(epochDay, pkg, deltaMs, deltaMs)
        )
    }

    fun getUsageForLastNDays(days: Int): List<UsageDaily> {
        val today = LocalDate.now().toEpochDay()
        val startDay = today - days + 1
        val list = mutableListOf<UsageDaily>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT epochDay, pkg, durationMs
            FROM usage_daily
            WHERE epochDay >= ?
            ORDER BY epochDay DESC, durationMs DESC
            """.trimIndent(),
            arrayOf(startDay.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                val day = it.getLong(0)
                val pkg = it.getString(1)
                val duration = it.getLong(2)
                list.add(UsageDaily(day, pkg, duration))
            }
        }
        return list
    }

    fun pruneOlderThan(daysToKeep: Int = 30) {
        val cutoff = LocalDate.now().toEpochDay() - daysToKeep
        val db = writableDatabase
        db.delete("usage_daily", "epochDay < ?", arrayOf(cutoff.toString()))
    }

    companion object {
        private const val DATABASE_NAME = "focusguard_usage.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: UsageDb? = null

        fun getInstance(context: Context): UsageDb {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UsageDb(context).also { INSTANCE = it }
            }
        }
    }
}
