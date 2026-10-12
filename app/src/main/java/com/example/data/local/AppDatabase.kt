package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.Item
import com.example.data.model.StockMovement
import com.example.data.model.Supplier
import com.example.data.model.Warehouse

@Database(
    entities = [
        Item::class,
        StockMovement::class,
        Invoice::class,
        Warehouse::class,
        Supplier::class,
        AuditLog::class,
        AppNotification::class,
        Client::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun supplierDao(): SupplierDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun notificationDao(): NotificationDao
    abstract fun clientDao(): ClientDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stockmaster_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
