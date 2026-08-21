package com.example.staybuddy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

@Database(
    entities = [
        PgListingEntity::class,
        SearchHistoryEntity::class,
        FavoriteEntity::class,
        ChatRoomEntity::class,
        MessageEntity::class,
        RoommatePostEntity::class,
        NotificationEntity::class,
        UserEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StayBuddyDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun searchDao(): SearchDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun chatDao(): ChatDao
    abstract fun roommatePostDao(): RoommatePostDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "staybuddy_db"

        /**
         * v12 adds the city/area scope + priority columns for featured/boosted
         * promotions. The app previously dropped these fields in the Room cache;
         * nullable columns with a default so the migration validates against the
         * entity schema ([androidx.room.ColumnInfo] defaults on the entity).
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pg_listings ADD COLUMN featuredCity TEXT")
                db.execSQL("ALTER TABLE pg_listings ADD COLUMN featuredArea TEXT")
                db.execSQL("ALTER TABLE pg_listings ADD COLUMN boostedCity TEXT")
                db.execSQL("ALTER TABLE pg_listings ADD COLUMN boostedArea TEXT")
                db.execSQL("ALTER TABLE pg_listings ADD COLUMN featuredPriority INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE pg_listings ADD COLUMN boostPriority INTEGER NOT NULL DEFAULT 2")
            }
        }
    }
}
