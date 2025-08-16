package com.demomiru.minicrm.data.models

import android.R.attr.order
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.*
import com.google.firebase.firestore.PropertyName
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val company: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId")]
)
data class OrderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String = "",
    val orderTitle: String = "",
    val orderAmount: Double = 0.0,
    val orderDate: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun convertToDate() : String{
        val formatter = DateTimeFormatter.ofPattern("E, dd MMM yyyy, hh:mm a")
            // 2. Convert the Long timestamp to a LocalDateTime object
        val instant = Instant.ofEpochMilli(orderDate)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return localDateTime.format(formatter)
    }
}

enum class SyncState { PENDING, IN_SYNC, SYNCED}

data class CustomerWithOrders(
    @Embedded val customer: CustomerEntity,
    @Relation(parentColumn = "id", entityColumn = "customerId")
    val orders: List<OrderEntity>
)

val customer1 = CustomerEntity(
    name = "Alice Johnson",
    email = "alice.j@example.com",
    phone = "555-0101",
    company = "Innovate Inc."
)

val customer2 = CustomerEntity(
    name = "Bob Smith",
    email = "bob.smith@webmail.com",
    phone = "555-0102",
    company = "Solutions Co."
)

val customer3 = CustomerEntity(
    name = "Charlie Brown",
    email = "charlie@enterprise.net",
    phone = "555-0103",
    company = "Tech Systems"
)

val customer4 = CustomerEntity(
    name = "Diana Prince",
    email = "diana.p@corp.io",
    phone = "555-0104",
    company = "Innovate Inc."
)

val customer5 = CustomerEntity(
    name = "Ethan Hunt",
    email = "ethan.hunt@mission.org",
    phone = "555-0105",
    company = "Global Dynamics"
)

// You can then put them in a list
val sampleCustomers = listOf(customer1, customer2, customer3, customer4, customer5)
const val ONE_DAY_IN_MS = 24 * 60 * 60 * 1000L
val now = System.currentTimeMillis()
val sampleOrders: List<OrderEntity> = listOf(
    OrderEntity(
        customerId = "CUST-101",
        orderTitle = "MacBook Pro 16-inch",
        orderAmount = 2499.99,
        orderDate = now - (1 * ONE_DAY_IN_MS), // 1 day ago
        syncState = SyncState.SYNCED
    ),
    OrderEntity(
        customerId = "CUST-205",
        orderTitle = "Annual Software Subscription",
        orderAmount = 149.50,
        orderDate = now - (3 * ONE_DAY_IN_MS), // 3 days ago
        syncState = SyncState.PENDING // Default state
    ),
    OrderEntity(
        customerId = "CUST-101", // Same customer, second order
        orderTitle = "Magic Mouse & Keyboard",
        orderAmount = 199.00,
        orderDate = now - (5 * ONE_DAY_IN_MS), // 5 days ago
        isDeleted = true, // This order is marked as deleted
        syncState = SyncState.PENDING
    ),
    OrderEntity(
        customerId = "CUST-310",
        orderTitle = "Office Coffee Supplies (Bulk)",
        orderAmount = 85.75,
        orderDate = now - (10 * ONE_DAY_IN_MS), // 10 days ago
        syncState = SyncState.PENDING
    ),
    OrderEntity(
        customerId = "CUST-404",
        orderTitle = "Ergonomic Office Chair",
        orderAmount = 350.00,
        orderDate = now - (12 * ONE_DAY_IN_MS), // 12 days ago
        syncState = SyncState.SYNCED
    )
)