package com.demomiru.minicrm.ui.screens

import com.demomiru.minicrm.viewmodel.CustomerState
import com.demomiru.minicrm.viewmodel.CustomerViewModel
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.demomiru.minicrm.data.models.OrderEntity
import com.demomiru.minicrm.data.models.SyncState
import com.demomiru.minicrm.ui.theme.MiniCRMTheme
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditOrderScreen(
    customerId: String,
    initialOrder: OrderEntity? = null, // null → Add mode
    customerName: String = "", // Display customer name in title
    onNavigateBack: () -> Unit,
    customerViewModel: CustomerViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val customerState by customerViewModel.customerState.collectAsStateWithLifecycle()

    // Form state
    var orderTitle by remember { mutableStateOf(initialOrder?.orderTitle ?: "") }
    var orderAmount by remember { mutableStateOf(
        if (initialOrder?.orderAmount != null && initialOrder.orderAmount > 0)
            initialOrder.orderAmount.toString()
        else ""
    ) }
    var orderDate by remember { mutableLongStateOf(initialOrder?.orderDate ?: System.currentTimeMillis()) }

    // Validation state
    var isTitleError by remember { mutableStateOf(false) }
    var isAmountError by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Handle state changes
    val state: CustomerState = customerState
    LaunchedEffect(state) {
        when (state) {
            is CustomerState.Success -> {
                if (state.message.contains("Order saved") || state.message.contains("saved successfully")) {
                    snackbarMessage = state.message
                    showSnackbar = true
                    // Navigate back after successful save
                    kotlinx.coroutines.delay(1000)
                    onNavigateBack()
                }
            }
            is CustomerState.Error -> {
                snackbarMessage = state.message
                showSnackbar = true
            }
            else -> {}
        }
    }

    // Date picker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = orderDate
    )

    // Format date for display
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val displayDate = dateFormatter.format(Date(orderDate))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (initialOrder == null) "Add Order" else "Edit Order")
                        if (customerName.isNotEmpty()) {
                            Text(
                                text = "for $customerName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Validation
                            isTitleError = orderTitle.isBlank()

                            val amountValue = orderAmount.toDoubleOrNull()
                            isAmountError = orderAmount.isBlank() || amountValue == null || amountValue <= 0

                            if (!isTitleError && !isAmountError) {
                                val now = System.currentTimeMillis()
                                val order = OrderEntity(
                                    id = initialOrder?.id ?: UUID.randomUUID().toString(),
                                    customerId = customerId,
                                    orderTitle = orderTitle.trim(),
                                    orderAmount = amountValue!!,
                                    orderDate = orderDate,
                                    updatedAt = now,
                                    isDeleted = false,
                                    syncState = SyncState.PENDING
                                )
                                customerViewModel.saveOrder(order)
                            }
                        },
                        enabled = customerState !is CustomerState.Loading
                    ) {
                        if (customerState is CustomerState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        },
        snackbarHost = {
            if (showSnackbar) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("Dismiss")
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status indicator
                if (customerState is CustomerState.Loading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Saving order...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Order Title Field
                OutlinedTextField(
                    value = orderTitle,
                    onValueChange = {
                        orderTitle = it
                        isTitleError = false
                    },
                    label = { Text("Order Title *") },
                    placeholder = { Text("e.g., MacBook Pro 16-inch") },
                    isError = isTitleError,
                    supportingText = {
                        if (isTitleError) Text("Order title is required")
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    enabled = customerState !is CustomerState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Order Amount Field
                OutlinedTextField(
                    value = orderAmount,
                    onValueChange = { newValue ->
                        // Allow only numbers and decimal point
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            orderAmount = newValue
                            isAmountError = false
                        }
                    },
                    label = { Text("Order Amount *") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = "$",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    isError = isAmountError,
                    supportingText = {
                        if (isAmountError) {
                            Text("Enter a valid amount greater than 0")
                        } else {
                            Text("Enter the total order amount")
                        }
                    },
                    enabled = customerState !is CustomerState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Order Date Field
                OutlinedTextField(
                    value = displayDate,
                    onValueChange = { }, // Read-only
                    label = { Text("Order Date") },
                    trailingIcon = {
                        IconButton(
                            onClick = { showDatePicker = true },
                            enabled = customerState !is CustomerState.Loading
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    readOnly = true,
                    enabled = customerState !is CustomerState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Order Preview Card
                if (orderTitle.isNotEmpty() && orderAmount.isNotEmpty()) {
                    val previewAmount = orderAmount.toDoubleOrNull()
                    if (previewAmount != null && previewAmount > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Order Preview",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Badge {
                                        Text(
                                            text = if (initialOrder == null) "NEW" else "UPDATED",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                Text(
                                    text = orderTitle,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "$${String.format("%.2f", previewAmount)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = displayDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Quick Amount Buttons
                if (initialOrder == null) { // Only show for new orders
                    Text(
                        text = "Quick Amounts",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickAmounts = listOf("50", "100", "250", "500")
                        quickAmounts.forEach { amount ->
                            OutlinedButton(
                                onClick = { orderAmount = amount },
                                enabled = customerState !is CustomerState.Loading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$$amount")
                            }
                        }
                    }
                }

                // Bottom spacer to ensure content doesn't get cut off
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            orderDate = selectedDate
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    // Reset state when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            customerViewModel.resetState()
        }
    }
}

// Extension function to add to your CustomerViewModel
fun CustomerViewModel.loadCustomerName(customerId: String, callback: (String) -> Unit) {
    // This would ideally be a separate StateFlow, but for simplicity:
    val customer = selectedCustomer.value
    if (customer?.id == customerId) {
        callback(customer.name)
    } else {
        // Load customer if not already loaded
        loadCustomer(customerId)
    }
}

// Usage in Navigation
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddEditOrderScreenWrapper(
    customerId: String,
    orderId: String? = null, // null for add mode
    onNavigateBack: () -> Unit,
    customerViewModel: CustomerViewModel = viewModel()
) {
    val selectedCustomer by customerViewModel.selectedCustomer.collectAsStateWithLifecycle()
    val orders by customerViewModel.orders.collectAsStateWithLifecycle()

    // Load customer if not already loaded
    LaunchedEffect(customerId) {
        if (selectedCustomer?.id != customerId) {
            customerViewModel.loadCustomer(customerId)
        }
        if (orderId != null) {
            customerViewModel.loadCustomerOrders(customerId)
        }
    }

    val initialOrder = if (orderId != null) {
        orders.find { it.id == orderId }
    } else null

    AddEditOrderScreen(
        customerId = customerId,
        initialOrder = initialOrder,
        customerName = selectedCustomer?.name ?: "",
        onNavigateBack = onNavigateBack,
        customerViewModel = customerViewModel
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun AddEditOrderPreview() {
    MiniCRMTheme {
        AddEditOrderScreen(
            customerId = "sample-customer-id",
            initialOrder = null,
            customerName = "John Doe",
            onNavigateBack = {}
        )
    }
}