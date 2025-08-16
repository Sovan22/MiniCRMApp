package com.demomiru.minicrm.data

import CustomerDao
import OrderDao
import android.content.Context
import com.demomiru.minicrm.data.models.CustomerEntity
import com.demomiru.minicrm.data.models.OrderEntity
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.demomiru.minicrm.data.models.SyncState
import kotlin.jvm.java

class Converters {
    @TypeConverter
    fun fromSyncState(syncState: SyncState): String {
        return syncState.name
    }

    @TypeConverter
    fun toSyncState(syncState: String): SyncState {
        return SyncState.valueOf(syncState)
    }
}

@Database(
    entities = [CustomerEntity::class, OrderEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CRMDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: CRMDatabase? = null

        fun getDatabase(context: Context): CRMDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CRMDatabase::class.java,
                    "customer_database"
                )
                    .fallbackToDestructiveMigration() // For development only
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

//@Database(
//    entities = [CustomerEntity::class, OrderEntity::class],
//    version = 1,
//    exportSchema = false
//)
//abstract class CrmDatabase : RoomDatabase() {
//    abstract fun customerDao(): CustomerDao
//    abstract fun orderDao(): OrderDao
//}
