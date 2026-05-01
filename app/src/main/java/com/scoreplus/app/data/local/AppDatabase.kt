package com.scoreplus.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.scoreplus.app.data.local.dao.CategoryDao
import com.scoreplus.app.data.local.dao.ExpenseDao
import com.scoreplus.app.data.local.dao.IncomeDao
import com.scoreplus.app.data.local.dao.SavingsDao
import com.scoreplus.app.data.local.entity.CategoryEntity
import com.scoreplus.app.data.local.entity.ExpenseEntity
import com.scoreplus.app.data.local.entity.IncomeItemEntity
import com.scoreplus.app.data.local.entity.SavingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CategoryEntity::class, ExpenseEntity::class, IncomeItemEntity::class, SavingsEntity::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingsDao(): SavingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val defaultCategories = listOf(
            CategoryEntity(name = "Kira", icon = "🏠", isDefault = true),
            CategoryEntity(name = "Aidat", icon = "🏢", isDefault = true),
            CategoryEntity(name = "Elektrik", icon = "⚡", isDefault = true),
            CategoryEntity(name = "Su", icon = "💧", isDefault = true),
            CategoryEntity(name = "Doğalgaz", icon = "🔥", isDefault = true),
            CategoryEntity(name = "İnternet", icon = "📶", isDefault = true),
            CategoryEntity(name = "Okul Masrafı", icon = "📚", isDefault = true),
            CategoryEntity(name = "Market", icon = "🛒", isDefault = true),
            CategoryEntity(name = "Manav", icon = "🥦", isDefault = true),
            CategoryEntity(name = "Dijital Abonelik", icon = "📱", isDefault = true),
            CategoryEntity(name = "Kredi Kartı", icon = "💳", isDefault = true),
            CategoryEntity(name = "Banka Kredisi", icon = "🏦", isDefault = true),
            CategoryEntity(name = "Yemek", icon = "🍽️", isDefault = true),
            CategoryEntity(name = "Sigorta", icon = "🛡️", isDefault = true),
            CategoryEntity(name = "Vergi", icon = "🧾", isDefault = true),
            CategoryEntity(name = "Kasko", icon = "🚗", isDefault = true),
            CategoryEntity(name = "Kurs", icon = "🎓", isDefault = true),
            CategoryEntity(name = "Yakıt", icon = "⛽", isDefault = true),
            CategoryEntity(name = "HGS", icon = "🛣️", isDefault = true),
            CategoryEntity(name = "Eğlence", icon = "🎉", isDefault = true),
            CategoryEntity(name = "Hobi", icon = "🎨", isDefault = true),
            CategoryEntity(name = "Diğer", icon = "📌", isDefault = true),
        )

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("INSERT INTO categories (name, icon, isDefault) VALUES ('Kredi Kartı', '💳', 1)")
                database.execSQL("INSERT INTO categories (name, icon, isDefault) VALUES ('Kredi', '🏦', 1)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE FROM categories WHERE name = 'Kredi'")
                database.execSQL("DELETE FROM categories WHERE name = 'Kredi Kartı' AND id NOT IN (SELECT MIN(id) FROM categories WHERE name = 'Kredi Kartı')")
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'Banka Kredisi', '🏦', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Banka Kredisi')")
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'Diğer', '📌', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Diğer')")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Yeni income_items tablosunu oluştur
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `income_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `date` INTEGER NOT NULL DEFAULT 0,
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL
                    )"""
                )
                // Eski incomes tablosundaki verileri taşı
                database.execSQL(
                    """INSERT INTO income_items (amount, description, date, month, year)
                       SELECT amount, COALESCE(note, ''), strftime('%s','now') * 1000, month, year
                       FROM incomes"""
                )
                // Eski tabloyu sil
                database.execSQL("DROP TABLE IF EXISTS incomes")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'Eğlence', '🎉', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Eğlence')")
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'Hobi', '🎨', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Hobi')")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'Yemek', '🍽️', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Yemek')")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN serverId INTEGER")
                database.execSQL("ALTER TABLE expenses ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE expenses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE income_items ADD COLUMN serverId INTEGER")
                database.execSQL("ALTER TABLE income_items ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE income_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE categories ADD COLUMN serverId INTEGER")
                database.execSQL("ALTER TABLE categories ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE savings ADD COLUMN serverId INTEGER")
                database.execSQL("ALTER TABLE savings ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'Sigorta', '🛡️', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Sigorta')")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'Vergi', '🧾', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Vergi')")
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'Kasko', '🚗', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Kasko')")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'Kurs', '🎓', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Kurs')")
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'Yakıt', '⛽', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Yakıt')")
                database.execSQL("INSERT INTO categories (name, icon, isDefault) SELECT 'HGS', '🛣️', 1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'HGS')")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `savings` (
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        PRIMARY KEY(`month`, `year`)
                    )"""
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scoreplus_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                database.categoryDao().insertAll(defaultCategories)
                            }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
