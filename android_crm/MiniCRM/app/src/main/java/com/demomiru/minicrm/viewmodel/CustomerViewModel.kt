package com.demomiru.minicrm.viewmodel

import CustomerRepository
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.demomiru.minicrm.data.api
import com.demomiru.minicrm.data.models.CustomerEntity
import com.demomiru.minicrm.data.models.CustomerWithOrders
import com.demomiru.minicrm.data.models.OrderEntity
import com.demomiru.minicrm.data.models.SyncState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.collections.filter

class CustomerViewModel(
    private val repository: CustomerRepository
) : ViewModel() {

    private val _customerState = MutableStateFlow<CustomerState>(CustomerState.Idle)
    val customerState: StateFlow<CustomerState> = _customerState.asStateFlow()

    private val _customers = MutableStateFlow<List<CustomerEntity>>(emptyList())
    val customers: StateFlow<List<CustomerEntity>> = _customers.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _importedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val importedCustomer: StateFlow<CustomerEntity?> = _importedCustomer

    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders.asStateFlow()

    private val _customerWithOrders = MutableStateFlow<CustomerWithOrders?>(null)
    val customerWithOrders: StateFlow<CustomerWithOrders?> = _customerWithOrders.asStateFlow()

    // CUSTOMER OPERATIONS

    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.saveCustomer(customer)
                .onSuccess { message ->
                    _customerState.value = CustomerState.Success(message)
//                    loadCustomers() // Refresh the list
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Unknown error")
                }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading
            repository.getAllCustomers()
                .onSuccess { customerList ->
                    _customers.value = customerList
                    _customerState.value = CustomerState.Success("Customers loaded successfully")
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Failed to load customers")
                }
        }
    }

    fun loadCustomer(customerId: String) {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.getCustomerById(customerId)
                .onSuccess { customer ->
                    _selectedCustomer.value = customer
                    _customerState.value = CustomerState.Success("Customer loaded successfully")
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Customer not found")
                }
        }
    }

    fun importRandomCustomer() {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading
            try {
                val customers = api.getCustomers()
                if (customers.isNotEmpty()) {
                    _customerState.value = CustomerState.Success("Imported Random Data")
                    _importedCustomer.value = customers.random()
                }
            } catch (e: Exception) {
                _customerState.value = CustomerState.Error("Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.deleteCustomer(customerId)
                .onSuccess { message ->
                    _customerState.value = CustomerState.Success(message)
                    loadCustomers() // Refresh the list
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Failed to delete customer")
                }
        }
    }

    fun searchCustomers(query: String) {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.searchCustomers(query)
                .onSuccess { customerList ->
                    _customers.value = customerList
                    _customerState.value = CustomerState.Success("Search completed")
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Search failed")
                }
        }
    }

    fun syncAllPendingCustomers() {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.syncAllPendingCustomers()
                .onSuccess { message ->
                    _customerState.value = CustomerState.Success(message)
                    loadCustomers() // Refresh the list
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Failed to sync customers")
                }
        }
    }

    // ORDER OPERATIONS

    fun saveOrder(order: OrderEntity) {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.saveOrder(order)
                .onSuccess { message ->
                    _customerState.value = CustomerState.Success(message)
//                    loadCustomerOrders(order.customerId) // Refresh orders
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Failed to save order")
                }
        }
    }

    fun loadCustomerOrders(customerId: String) {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.getOrdersByCustomer(customerId)
                .onSuccess { orderList ->
                    _orders.value = orderList
                    _customerState.value = CustomerState.Success("Orders loaded successfully")
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Failed to load orders")
                }
        }
    }

    fun loadCustomerWithOrders(customerId: String) {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.getCustomerWithOrders(customerId)
                .onSuccess { customerWithOrdersData ->
                    _customerWithOrders.value = customerWithOrdersData
                    _selectedCustomer.value = customerWithOrdersData.customer
                    _orders.value = customerWithOrdersData.orders
                    _customerState.value = CustomerState.Success("Customer and orders loaded successfully")
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Failed to load customer details")
                }
        }
    }

    fun deleteOrder(customerId: String, orderId: String) {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.deleteOrder(customerId, orderId)
                .onSuccess { message ->
                    _customerState.value = CustomerState.Success(message)
                    loadCustomerOrders(customerId) // Refresh orders
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Failed to delete order")
                }
        }
    }

    fun searchCustomerOrders(customerId: String, query: String) {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.searchOrdersByCustomer(customerId, query)
                .onSuccess { orderList ->
                    _orders.value = orderList
                    _customerState.value = CustomerState.Success("Order search completed")
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Order search failed")
                }
        }
    }

    fun syncAllPendingOrders(customerId: String) {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.syncAllPendingOrders(customerId)
                .onSuccess { message ->
                    _customerState.value = CustomerState.Success(message)
                    loadCustomerOrders(customerId) // Refresh orders
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Failed to sync orders")
                }
        }
    }

    fun syncAllPendingData() {
        viewModelScope.launch {
            _customerState.value = CustomerState.Loading

            repository.syncAllPendingData()
                .onSuccess { message ->
                    _customerState.value = CustomerState.Success(message)
                    loadCustomers() // Refresh data
                }
                .onFailure { exception ->
                    _customerState.value = CustomerState.Error(exception.message ?: "Failed to sync all data")
                }
        }
    }

    // UTILITY FUNCTIONS

    fun getCustomerStats(customerId: String): CustomerStats {
        return repository.getCustomerStats(customerId, _orders.value)
    }

    fun getCustomerCount(): Int = _customers.value.size

    fun resetState() {
        _customerState.value = CustomerState.Idle
    }

    fun clearSelectedCustomer() {
        _selectedCustomer.value = null
    }

    fun clearOrders() {
        _orders.value = emptyList()
    }

    fun clearCustomerWithOrders() {
        _customerWithOrders.value = null
    }
}

class CustomerViewModelFactory(
    private val repository: CustomerRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
            return CustomerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// CustomerState.kt
sealed class CustomerState {
    object Idle : CustomerState()
    object Loading : CustomerState()
    data class Success(val message: String) : CustomerState()
    data class Error(val message: String) : CustomerState()
}

data class CustomerStats(
    val orderCount: Int = 0,
    val totalSpent: Double = 0.0,
    val lastOrderDate: Long? = null
)