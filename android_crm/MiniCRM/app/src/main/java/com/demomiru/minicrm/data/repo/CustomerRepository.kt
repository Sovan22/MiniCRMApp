import android.util.Log
import com.demomiru.minicrm.data.models.CustomerEntity
import com.demomiru.minicrm.data.models.CustomerWithOrders
import com.demomiru.minicrm.data.models.OrderEntity
import com.demomiru.minicrm.data.models.SyncState
import com.demomiru.minicrm.viewmodel.CustomerStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import kotlin.collections.filter

interface CustomerRepository {
    suspend fun saveCustomer(customer: CustomerEntity): Result<String>
    suspend fun getAllCustomers(): Result<List<CustomerEntity>>
    suspend fun getCustomerById(customerId: String): Result<CustomerEntity>
    suspend fun deleteCustomer(customerId: String): Result<String>
    suspend fun searchCustomers(query: String): Result<List<CustomerEntity>>
    suspend fun syncAllPendingCustomers(): Result<String>

    suspend fun saveOrder(order: OrderEntity): Result<String>
    suspend fun getOrdersByCustomer(customerId: String): Result<List<OrderEntity>>
    suspend fun getCustomerWithOrders(customerId: String): Result<CustomerWithOrders>
    suspend fun deleteOrder(customerId: String, orderId: String): Result<String>
    suspend fun searchOrdersByCustomer(customerId: String, query: String): Result<List<OrderEntity>>
    suspend fun syncAllPendingOrders(customerId: String): Result<String>
    suspend fun syncAllPendingData(): Result<String>

    fun getCustomerStats(customerId: String, orders: List<OrderEntity>): CustomerStats
}

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao,
    private val orderDao: OrderDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CustomerRepository {

    private fun getCustomersCollection() = firestore
        .collection("users")
        .document(getUserId() ?: "")
        .collection("customers")

    private fun getOrdersCollection(customerId: String) = firestore
        .collection("users")
        .document(getUserId() ?: "")
        .collection("customers")
        .document(customerId)
        .collection("orders")

    private fun getUserId(): String? = auth.currentUser?.uid

    // CUSTOMER OPERATIONS

    override suspend fun saveCustomer(customer: CustomerEntity): Result<String> {
        return try {
            // 1. Save to local database first
            val customerData = customer.copy(
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING
            )

            customerDao.insertOrUpdate(customerData)
            Log.d("CustomerRepository", "Customer saved to database: ${customer.id}")

            // 2. Try to sync to Firebase
            val userId = getUserId()
            if (userId != null) {
                try {
                    val syncedCustomer = customerData.copy(syncState = SyncState.SYNCED)

                    getCustomersCollection()
                        .document(customer.id)
                        .set(syncedCustomer)
                        .await()

                    // Update local database with synced state
                    customerDao.updateSyncState(customer.id, SyncState.SYNCED)
                    Log.d("CustomerRepository", "Customer synced to Firebase: ${customer.id}")

                    Result.success("Customer saved and synced successfully")
                } catch (e: Exception) {
                    Log.e("CustomerRepository", "Error syncing customer to Firebase: ${e.message}")
                    Result.success("Customer saved locally, will sync when online")
                }
            } else {
                Result.success("Customer saved locally, will sync when authenticated")
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error saving customer: ${e.message}")
            Result.failure(Exception("Failed to save customer: ${e.message}"))
        }
    }

    override suspend fun getAllCustomers(): Result<List<CustomerEntity>> {
        return try {
            // 1. Load from local database first
            val customers = customerDao.getAllCustomers()

            // 2. Try to sync from Firebase in background
            val userId = getUserId()
            if (userId != null) {
                try {
                    syncCustomersFromFirebase()
                } catch (e: Exception) {
                    Log.e("CustomerRepository", "Error syncing from Firebase: ${e.message}")
                }
            }

            Result.success(customers)
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error loading customers: ${e.message}")
            Result.failure(Exception("Failed to load customers: ${e.message}"))
        }
    }

    private suspend fun syncCustomersFromFirebase() {
        try {
            val querySnapshot = getCustomersCollection()
                .whereEqualTo("isDeleted", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val firebaseCustomers = querySnapshot.documents.mapNotNull { document ->
                document.toObject<CustomerEntity>()?.copy(syncState = SyncState.SYNCED)
            }

            if (firebaseCustomers.isNotEmpty()) {
                customerDao.insertOrUpdateAll(firebaseCustomers)
                Log.d("CustomerRepository", "Synced ${firebaseCustomers.size} customers from Firebase")
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error syncing customers from Firebase: ${e.message}")
        }
    }

    override suspend fun getCustomerById(customerId: String): Result<CustomerEntity> {
        return try {
            // Load from database first
            val customer = customerDao.getCustomerById(customerId)

            if (customer != null && !customer.isDeleted) {
                // Try to sync from Firebase in background
                val userId = getUserId()
                if (userId != null) {
                    try {
                        val documentSnapshot = getCustomersCollection()
                            .document(customerId)
                            .get()
                            .await()

                        val firebaseCustomer = documentSnapshot.toObject<CustomerEntity>()
                        if (firebaseCustomer != null && !firebaseCustomer.isDeleted) {
                            val syncedCustomer = firebaseCustomer.copy(syncState = SyncState.SYNCED)
                            customerDao.insertOrUpdate(syncedCustomer)
                            return Result.success(syncedCustomer)
                        }
                    } catch (e: Exception) {
                        Log.e("CustomerRepository", "Error syncing customer from Firebase: ${e.message}")
                    }
                }
                Result.success(customer)
            } else {
                Result.failure(Exception("Customer not found"))
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error loading customer: ${e.message}")
            Result.failure(Exception("Failed to load customer: ${e.message}"))
        }
    }

    override suspend fun deleteCustomer(customerId: String): Result<String> {
        return try {
            // 1. Soft delete in local database first
            customerDao.softDeleteCustomer(customerId)
            Log.d("CustomerRepository", "Customer soft deleted in database: $customerId")

            // 2. Try to sync deletion to Firebase
            val userId = getUserId()
            if (userId != null) {
                try {
                    val updates = mapOf(
                        "isDeleted" to true,
                        "updatedAt" to System.currentTimeMillis(),
                        "syncState" to SyncState.SYNCED.name
                    )

                    getCustomersCollection()
                        .document(customerId)
                        .update(updates)
                        .await()

                    customerDao.updateSyncState(customerId, SyncState.SYNCED)
                    Log.d("CustomerRepository", "Customer deletion synced to Firebase: $customerId")

                    Result.success("Customer deleted and synced successfully")
                } catch (e: Exception) {
                    Log.e("CustomerRepository", "Error syncing deletion to Firebase: ${e.message}")
                    Result.success("Customer deleted locally, will sync when online")
                }
            } else {
                Result.success("Customer deleted locally, will sync when authenticated")
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error deleting customer: ${e.message}")
            Result.failure(Exception("Failed to delete customer: ${e.message}"))
        }
    }

    override suspend fun searchCustomers(query: String): Result<List<CustomerEntity>> {
        return try {
            val customers = if (query.isBlank()) {
                customerDao.getAllCustomers()
            } else {
                customerDao.searchCustomers("%$query%")
            }
            Result.success(customers)
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error searching customers: ${e.message}")
            Result.failure(Exception("Search failed: ${e.message}"))
        }
    }

    override suspend fun syncAllPendingCustomers(): Result<String> {
        return try {
            val pendingCustomers = customerDao.getPendingCustomers()

            if (pendingCustomers.isEmpty()) {
                return Result.success("No customers to sync")
            }

            val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))

            val batch = firestore.batch()

            pendingCustomers.forEach { customer ->
                val docRef = getCustomersCollection().document(customer.id)
                val customerData = customer.copy(
                    syncState = SyncState.SYNCED,
                    updatedAt = System.currentTimeMillis()
                )
                batch.set(docRef, customerData)
            }

            batch.commit().await()

            // Update sync states in database
            pendingCustomers.forEach { customer ->
                customerDao.updateSyncState(customer.id, SyncState.SYNCED)
            }

            Log.d("CustomerRepository", "Synced ${pendingCustomers.size} customers to Firebase")
            Result.success("${pendingCustomers.size} customers synced successfully")

        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error syncing customers: ${e.message}")
            Result.failure(Exception("Failed to sync customers: ${e.message}"))
        }
    }

    // ORDER OPERATIONS

    override suspend fun saveOrder(order: OrderEntity): Result<String> {
        return try {
            // 1. Save to local database first
            val orderData = order.copy(
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING
            )

            orderDao.insertOrUpdate(orderData)
            Log.d("CustomerRepository", "Order saved to database: ${order.id}")

            // 2. Try to sync to Firebase
            val userId = getUserId()
            if (userId != null) {
                try {
                    val syncedOrder = orderData.copy(syncState = SyncState.SYNCED)

                    getOrdersCollection(order.customerId)
                        .document(order.id)
                        .set(syncedOrder)
                        .await()

                    orderDao.updateSyncState(order.id, SyncState.SYNCED)
                    Log.d("CustomerRepository", "Order synced to Firebase: ${order.id}")

                    Result.success("Order saved and synced successfully")
                } catch (e: Exception) {
                    Log.e("CustomerRepository", "Error syncing order to Firebase: ${e.message}")
                    Result.success("Order saved locally, will sync when online")
                }
            } else {
                Result.success("Order saved locally, will sync when authenticated")
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error saving order: ${e.message}")
            Result.failure(Exception("Failed to save order: ${e.message}"))
        }
    }

    override suspend fun getOrdersByCustomer(customerId: String): Result<List<OrderEntity>> {
        return try {
            // 1. Load from database first
            val orders = orderDao.getOrdersByCustomer(customerId)

            // 2. Try to sync from Firebase in background
            val userId = getUserId()
            if (userId != null) {
                try {
                    syncOrdersFromFirebase(customerId)
                } catch (e: Exception) {
                    Log.e("CustomerRepository", "Error syncing orders from Firebase: ${e.message}")
                }
            }

            Result.success(orders)
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error loading orders: ${e.message}")
            Result.failure(Exception("Failed to load orders: ${e.message}"))
        }
    }

    private suspend fun syncOrdersFromFirebase(customerId: String) {
        try {
            val querySnapshot = getOrdersCollection(customerId)
                .whereEqualTo("isDeleted", false)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .await()

            val firebaseOrders = querySnapshot.documents.mapNotNull { document ->
                document.toObject<OrderEntity>()?.copy(syncState = SyncState.SYNCED)
            }

            if (firebaseOrders.isNotEmpty()) {
                orderDao.insertOrUpdateAll(firebaseOrders)
                Log.d("CustomerRepository", "Synced ${firebaseOrders.size} orders from Firebase")
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error syncing orders from Firebase: ${e.message}")
        }
    }

    override suspend fun getCustomerWithOrders(customerId: String): Result<CustomerWithOrders> {
        return try {
            // Load from database
            val customerWithOrders = customerDao.getCustomerWithOrders(customerId)

            if (customerWithOrders == null || customerWithOrders.customer.isDeleted) {
                return Result.failure(Exception("Customer not found"))
            }

            // Sync from Firebase in background
            val userId = getUserId()
            if (userId != null) {
                try {
                    // Sync customer
                    val customerDoc = getCustomersCollection()
                        .document(customerId)
                        .get()
                        .await()

                    val firebaseCustomer = customerDoc.toObject<CustomerEntity>()
                    if (firebaseCustomer != null && !firebaseCustomer.isDeleted) {
                        val syncedCustomer = firebaseCustomer.copy(syncState = SyncState.SYNCED)
                        customerDao.insertOrUpdate(syncedCustomer)
                    }

                    // Sync orders
                    syncOrdersFromFirebase(customerId)

                    // Return updated data
                    val updatedCustomerWithOrders = customerDao.getCustomerWithOrders(customerId)
                    return Result.success(updatedCustomerWithOrders ?: customerWithOrders)

                } catch (e: Exception) {
                    Log.e("CustomerRepository", "Error syncing customer with orders from Firebase: ${e.message}")
                }
            }

            Result.success(customerWithOrders)

        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error loading customer with orders: ${e.message}")
            Result.failure(Exception("Failed to load customer details: ${e.message}"))
        }
    }

    override suspend fun deleteOrder(customerId: String, orderId: String): Result<String> {
        return try {
            // 1. Soft delete in database first
            orderDao.softDeleteOrder(orderId)
            Log.d("CustomerRepository", "Order soft deleted in database: $orderId")

            // 2. Try to sync deletion to Firebase
            val userId = getUserId()
            if (userId != null) {
                try {
                    val updates = mapOf(
                        "isDeleted" to true,
                        "updatedAt" to System.currentTimeMillis(),
                        "syncState" to SyncState.SYNCED.name
                    )

                    getOrdersCollection(customerId)
                        .document(orderId)
                        .update(updates)
                        .await()

                    orderDao.updateSyncState(orderId, SyncState.SYNCED)
                    Log.d("CustomerRepository", "Order deletion synced to Firebase: $orderId")

                    Result.success("Order deleted and synced successfully")
                } catch (e: Exception) {
                    Log.e("CustomerRepository", "Error syncing order deletion to Firebase: ${e.message}")
                    Result.success("Order deleted locally, will sync when online")
                }
            } else {
                Result.success("Order deleted locally, will sync when authenticated")
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error deleting order: ${e.message}")
            Result.failure(Exception("Failed to delete order: ${e.message}"))
        }
    }

    override suspend fun searchOrdersByCustomer(customerId: String, query: String): Result<List<OrderEntity>> {
        return try {
            val orders = if (query.isBlank()) {
                orderDao.getOrdersByCustomer(customerId)
            } else {
                orderDao.searchOrdersByCustomer(customerId, "%$query%")
            }
            Result.success(orders)
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error searching orders: ${e.message}")
            Result.failure(Exception("Order search failed: ${e.message}"))
        }
    }

    override suspend fun syncAllPendingOrders(customerId: String): Result<String> {
        return try {
            val pendingOrders = orderDao.getPendingOrdersByCustomer(customerId)

            if (pendingOrders.isEmpty()) {
                return Result.success("No orders to sync")
            }

            val userId = getUserId() ?: return Result.failure(Exception("User not authenticated"))

            val batch = firestore.batch()

            pendingOrders.forEach { order ->
                val docRef = getOrdersCollection(customerId).document(order.id)
                val orderData = order.copy(
                    syncState = SyncState.SYNCED,
                    updatedAt = System.currentTimeMillis()
                )
                batch.set(docRef, orderData)
            }

            batch.commit().await()

            // Update sync states in database
            pendingOrders.forEach { order ->
                orderDao.updateSyncState(order.id, SyncState.SYNCED)
            }

            Log.d("CustomerRepository", "Synced ${pendingOrders.size} orders to Firebase")
            Result.success("${pendingOrders.size} orders synced successfully")

        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error syncing orders: ${e.message}")
            Result.failure(Exception("Failed to sync orders: ${e.message}"))
        }
    }

    override suspend fun syncAllPendingData(): Result<String> {
        return try {
            // Sync customers first
            val customerSyncResult = syncAllPendingCustomers()

            // Then sync all pending orders
            val pendingOrders = orderDao.getAllPendingOrders()
            val customerIds = pendingOrders.map { it.customerId }.distinct()

            val orderSyncResults = customerIds.map { customerId ->
                syncAllPendingOrders(customerId)
            }

            val allSuccess = customerSyncResult.isSuccess && orderSyncResults.all { it.isSuccess }

            if (allSuccess) {
                Result.success("All data synced successfully")
            } else {
                Result.failure(Exception("Some data failed to sync"))
            }

        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error syncing all data: ${e.message}")
            Result.failure(Exception("Failed to sync all data: ${e.message}"))
        }
    }

    // UTILITY FUNCTIONS

    override fun getCustomerStats(customerId: String, orders: List<OrderEntity>): CustomerStats {
        val customerOrders = orders.filter { it.customerId == customerId }
        val totalAmount = customerOrders.sumOf { it.orderAmount }
        val orderCount = customerOrders.size
        val lastOrderDate = customerOrders.maxByOrNull { it.orderDate }?.orderDate

        return CustomerStats(
            orderCount = orderCount,
            totalSpent = totalAmount,
            lastOrderDate = lastOrderDate
        )
    }
}

class RepositoryFactory {
    companion object {
        fun createCustomerRepository(
            customerDao: CustomerDao,
            orderDao: OrderDao,
            firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
            auth: FirebaseAuth = FirebaseAuth.getInstance()
        ): CustomerRepository {
            return CustomerRepositoryImpl(customerDao, orderDao, firestore, auth)
        }
    }
}