package com.example.policemobiledirectory.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        EmployeeEntity::class,
        OfficerEntity::class,
        PendingRegistrationEntity::class,
        AppIconEntity::class,
        NotificationEntity::class,
        LeaveEntryEntity::class,
        LeaveBalanceEntity::class,
        LeaveCreditLogEntity::class,
        AdminEmployeeEntity::class
    ],
    version = 27,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun officerDao(): OfficerDao
    abstract fun pendingRegistrationDao(): PendingRegistrationDao
    abstract fun appIconDao(): AppIconDao
    abstract fun notificationDao(): NotificationDao
    abstract fun leaveDao(): LeaveDao
    abstract fun adminEmployeeDao(): AdminEmployeeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ✅ Migration 3 → 4: Add new column + new table
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add new column to Employee table if missing
                    database.execSQL(
                        "ALTER TABLE employees ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 1"
                    )
                } catch (e: Exception) {
                    // Column might already exist, ignore
                }

                // Create new table for AppIconEntity if not exists
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_icons (
                        packageName TEXT PRIMARY KEY NOT NULL,
                        iconUrl TEXT NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // ✅ Migration 4 → 5: Add updatedAt column to employees table
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add updatedAt column to Employee table
                    database.execSQL(
                        "ALTER TABLE employees ADD COLUMN updatedAt INTEGER"
                    )
                } catch (e: Exception) {
                    // Column might already exist, ignore
                }
            }
        }

        // ✅ Migration 5 → 6: Add indexes for better query performance
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Create indexes for frequently queried columns
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_email ON employees(email)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_name ON employees(name)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_station ON employees(station)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_district ON employees(district)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_rank ON employees(rank)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_mobile1 ON employees(mobile1)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_mobile2 ON employees(mobile2)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_metalNumber ON employees(metalNumber)")
                } catch (e: Exception) {
                    // Indexes might already exist, ignore
                }
            }
        }
        
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isRead INTEGER NOT NULL,
                        targetKgid TEXT
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_timestamp ON notifications(timestamp)")
            }
        }

        // ✅ Migration 7 → 8: Fix Schema Drift (add missing columns safely)
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Add 'unit' to 'employees'
                try {
                    database.execSQL("ALTER TABLE employees ADD COLUMN unit TEXT")
                } catch (e: Exception) {
                }

                // 2. Add 'unit' to 'pending_registrations'
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN unit TEXT")
                } catch (e: Exception) {
                }

                // 3. Add 'firebaseUid' to 'pending_registrations'
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN firebaseUid TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                }

                // 4. Add 'photoUrlFromGoogle' to 'pending_registrations'
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN photoUrlFromGoogle TEXT")
                } catch (e: Exception) {
                }
            }
        }

        // ✅ Migration 8 → 9: Power Search (add searchBlob and officers table)
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Add searchBlob to employees
                try {
                    database.execSQL("ALTER TABLE employees ADD COLUMN searchBlob TEXT NOT NULL DEFAULT ''")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_searchBlob ON employees(searchBlob)")
                } catch (e: Exception) {
                }

                // 2. Create officers table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS officers (
                        agid TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        email TEXT,
                        rank TEXT,
                        mobile TEXT,
                        landline TEXT,
                        station TEXT,
                        district TEXT,
                        unit TEXT,
                        photoUrl TEXT,
                        bloodGroup TEXT,
                        isHidden INTEGER NOT NULL DEFAULT 0,
                        searchBlob TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                
                // 3. Add indexes for officers
                database.execSQL("CREATE INDEX IF NOT EXISTS index_officers_name ON officers(name)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_officers_district ON officers(district)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_officers_rank ON officers(rank)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_officers_unit ON officers(unit)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_officers_searchBlob ON officers(searchBlob)")
            }
        }

        // ✅ Migration 10 → 11: Add gender and serviceStartDate to employees and pending_registrations
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add to employees table
                try {
                    database.execSQL("ALTER TABLE employees ADD COLUMN gender TEXT NOT NULL DEFAULT 'Male'")
                } catch (e: Exception) {}
                try {
                    database.execSQL("ALTER TABLE employees ADD COLUMN serviceStartDate INTEGER")
                } catch (e: Exception) {}
                
                // Add to pending_registrations table
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN gender TEXT NOT NULL DEFAULT 'Male'")
                } catch (e: Exception) {}
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN serviceStartDate INTEGER")
                } catch (e: Exception) {}
            }
        }

        // ✅ Migration 9 → 10: Add isManualStation column
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE employees ADD COLUMN isManualStation INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN isManualStation INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        // ✅ Migration 11 → 12: Add dateOfBirth, landline2, isManualSubSection, subSection
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // employees: add dateOfBirth
                try {
                    database.execSQL("ALTER TABLE employees ADD COLUMN dateOfBirth INTEGER")
                } catch (e: Exception) {}

                // pending_registrations: add dateOfBirth, landline2, isManualSubSection, subSection
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN dateOfBirth INTEGER")
                } catch (e: Exception) {}
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN landline2 TEXT")
                } catch (e: Exception) {}
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN isManualSubSection INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    database.execSQL("ALTER TABLE pending_registrations ADD COLUMN subSection TEXT")
                } catch (e: Exception) {}
            }
        }

        // ✅ Migration 12 → 13: Add LeaveBalance and LeaveEntry tables
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS leave_balances (
                        kgid TEXT PRIMARY KEY NOT NULL,
                        clYear INTEGER NOT NULL DEFAULT 0,
                        clAnnualLimit INTEGER NOT NULL DEFAULT 15,
                        clRemaining REAL NOT NULL DEFAULT 15.0,
                        elManualBalance REAL NOT NULL DEFAULT 0.0,
                        elBalance REAL NOT NULL DEFAULT 0.0,
                        hplBalance REAL NOT NULL DEFAULT 0.0,
                        cclUsed REAL NOT NULL DEFAULT 0.0,
                        maternityUsedCount INTEGER NOT NULL DEFAULT 0,
                        paternityUsedCount INTEGER NOT NULL DEFAULT 0,
                        mclUsedThisMonth INTEGER NOT NULL DEFAULT 0,
                        woUsedThisMonth INTEGER NOT NULL DEFAULT 0,
                        mclLastUsedMonth INTEGER NOT NULL DEFAULT 0,
                        mclLastUsedYear INTEGER NOT NULL DEFAULT 0,
                        lastResetYear INTEGER NOT NULL DEFAULT 0,
                        lastCreditDate TEXT NOT NULL DEFAULT '',
                        lastElHplCreditDate TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS leave_entries (
                        localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        id TEXT NOT NULL,
                        kgid TEXT NOT NULL,
                        dateFrom INTEGER,
                        dateTo INTEGER,
                        totalDays REAL NOT NULL,
                        leaveType TEXT NOT NULL,
                        remark TEXT,
                        createdAt INTEGER,
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        isHalfDay INTEGER NOT NULL,
                        isMcl INTEGER NOT NULL,
                        elEntryType TEXT NOT NULL,
                        hasMedicalCertificate INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // ✅ Migration 13 → 14: Add LeaveCreditLog table
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS leave_credit_logs (
                        localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        id TEXT NOT NULL,
                        kgid TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        date INTEGER,
                        year INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // ✅ Migration 14 → 15: Add hplManualBalance to leave_balances
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE leave_balances ADD COLUMN hplManualBalance REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }

        // ✅ Migration 15 → 16: Handle submittedAt type change (no SQL needed, INTEGER column matches)
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }
        
        // ✅ Migration 16 → 17: Add isAdmin, dutyRole and missing schema drift columns to pending_registrations
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. New fields for Web Admin Sync
                try {
                    db.execSQL("ALTER TABLE pending_registrations ADD COLUMN isAdmin INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE pending_registrations ADD COLUMN dutyRole TEXT")
                } catch (e: Exception) {}
                
                // 2. Schema Drift Fixes (Missing columns in previous migrations)
                try {
                    db.execSQL("ALTER TABLE pending_registrations ADD COLUMN landline TEXT")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE pending_registrations ADD COLUMN viewedByAdmin INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE pending_registrations ADD COLUMN rejectionReason TEXT")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE pending_registrations ADD COLUMN createdAt INTEGER")
                } catch (e: Exception) {}
            }
        }

        // ✅ Migration 17 → 18: Sync employees table with Web Admin / Pending features
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE employees ADD COLUMN dutyRole TEXT")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE employees ADD COLUMN subSection TEXT")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE employees ADD COLUMN isManualSubSection INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE employees ADD COLUMN isAdmin INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE employees ADD COLUMN landline2 TEXT")
                } catch (e: Exception) {}
            }
        }

        // ✅ Migration 18 → 19: Comprehensive Schema Healing
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Healing 'pending_registrations' table
                val pendingColumns = listOf(
                    "gender" to "TEXT NOT NULL DEFAULT 'Male'",
                    "isManualStation" to "INTEGER NOT NULL DEFAULT 0",
                    "isManualSubSection" to "INTEGER NOT NULL DEFAULT 0",
                    "isAdmin" to "INTEGER NOT NULL DEFAULT 0",
                    "viewedByAdmin" to "INTEGER NOT NULL DEFAULT 0",
                    "dutyRole" to "TEXT",
                    "subSection" to "TEXT",
                    "landline" to "TEXT",
                    "landline2" to "TEXT",
                    "dateOfBirth" to "INTEGER",
                    "serviceStartDate" to "INTEGER",
                    "rejectionReason" to "TEXT",
                    "photoUrlFromGoogle" to "TEXT",
                    "unit" to "TEXT"
                )
                
                pendingColumns.forEach { (name, type) ->
                    try {
                        db.execSQL("ALTER TABLE pending_registrations ADD COLUMN $name $type")
                    } catch (e: Exception) {
                        // Column might already exist
                    }
                }

                // Healing 'employees' table
                val employeeColumns = listOf(
                    "dutyRole" to "TEXT",
                    "subSection" to "TEXT",
                    "isManualSubSection" to "INTEGER NOT NULL DEFAULT 0",
                    "isManualStation" to "INTEGER NOT NULL DEFAULT 0",
                    "isAdmin" to "INTEGER NOT NULL DEFAULT 0",
                    "landline" to "TEXT",
                    "landline2" to "TEXT",
                    "gender" to "TEXT NOT NULL DEFAULT 'Male'",
                    "dateOfBirth" to "INTEGER",
                    "serviceStartDate" to "INTEGER"
                )

                employeeColumns.forEach { (name, type) ->
                    try {
                        db.execSQL("ALTER TABLE employees ADD COLUMN $name $type")
                    } catch (e: Exception) {
                        // Column might already exist
                    }
                }
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "employee_directory_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19) // ✅ Keep user data on update
                    .fallbackToDestructiveMigration() // ✅ Wipe data if migration fails
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
