import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.demomiru.minicrm.data.models.CustomerEntity
import com.demomiru.minicrm.data.models.CustomerWithOrders
import com.demomiru.minicrm.data.models.OrderEntity
import com.demomiru.minicrm.data.models.SyncState
import com.demomiru.minicrm.viewmodel.CustomerViewModel

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :customerId AND isDeleted = 0")
    suspend fun getCustomerById(customerId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE syncState = 'PENDING'")
    suspend fun getPendingCustomers(): List<CustomerEntity>

    @Query("""
        SELECT * FROM customers 
        WHERE isDeleted = 0 
        AND (name LIKE :query 
             OR email LIKE :query 
             OR company LIKE :query 
             OR phone LIKE :query)
        ORDER BY createdAt DESC
    """)
    suspend fun searchCustomers(query: String): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(customers: List<CustomerEntity>)

    @Query("UPDATE customers SET isDeleted = 1, updatedAt = :timestamp WHERE id = :customerId")
    suspend fun softDeleteCustomer(customerId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET syncState = :syncState WHERE id = :customerId")
    suspend fun updateSyncState(customerId: String, syncState: SyncState)

    @Transaction
    @Query("SELECT * FROM customers WHERE id = :customerId AND isDeleted = 0")
    suspend fun getCustomerWithOrders(customerId: String): CustomerWithOrders?

    @Query("DELETE FROM customers WHERE isDeleted = 1")
    suspend fun deleteMarkedCustomers()
}

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders WHERE customerId = :customerId AND isDeleted = 0 ORDER BY orderDate DESC")
    suspend fun getOrdersByCustomer(customerId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :orderId AND isDeleted = 0")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE syncState = 'PENDING'")
    suspend fun getAllPendingOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE customerId = :customerId AND syncState = 'PENDING'")
    suspend fun getPendingOrdersByCustomer(customerId: String): List<OrderEntity>

    @Query("""
        SELECT * FROM orders 
        WHERE customerId = :customerId 
        AND isDeleted = 0 
        AND (orderTitle LIKE :query 
             OR CAST(orderAmount AS TEXT) LIKE :query 
             OR id LIKE :query)
        ORDER BY orderDate DESC
    """)
    suspend fun searchOrdersByCustomer(customerId: String, query: String): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(orders: List<OrderEntity>)

    @Query("UPDATE orders SET isDeleted = 1, updatedAt = :timestamp WHERE id = :orderId")
    suspend fun softDeleteOrder(orderId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET syncState = :syncState WHERE id = :orderId")
    suspend fun updateSyncState(orderId: String, syncState: SyncState)

    @Query("DELETE FROM orders WHERE isDeleted = 1")
    suspend fun deleteMarkedOrders()

    @Query("SELECT SUM(orderAmount) FROM orders WHERE customerId = :customerId AND isDeleted = 0")
    suspend fun getTotalAmountByCustomer(customerId: String): Double?

    @Query("SELECT COUNT(*) FROM orders WHERE customerId = :customerId AND isDeleted = 0")
    suspend fun getOrderCountByCustomer(customerId: String): Int
}