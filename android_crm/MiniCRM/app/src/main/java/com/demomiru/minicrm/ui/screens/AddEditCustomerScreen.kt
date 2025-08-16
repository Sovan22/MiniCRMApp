package com.demomiru.minicrm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.demomiru.minicrm.data.models.CustomerEntity
import com.demomiru.minicrm.data.models.SyncState
import com.demomiru.minicrm.ui.theme.MiniCRMTheme
import com.demomiru.minicrm.viewmodel.CustomerState
import com.demomiru.minicrm.viewmodel.CustomerViewModel
import kotlinx.coroutines.delay
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(
    initialCustomer: CustomerEntity? = null, // null → Add mode
    onNavigateBack: () -> Unit,
    onImportRandom: (() -> Unit)? = null,
    customerViewModel: CustomerViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val customerState by customerViewModel.customerState.collectAsStateWithLifecycle()
    val importedCustomer by customerViewModel.importedCustomer.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var email by remember { mutableStateOf(initialCustomer?.email ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var company by remember { mutableStateOf(initialCustomer?.company ?: "") }

    var isNameError by remember { mutableStateOf(false) }
    var isEmailError by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        customerViewModel.resetState()
    }

    LaunchedEffect(importedCustomer) {
        importedCustomer?.let { customer ->
            name = customer.name
            email = customer.email
            phone = customer.phone
            company = customer.company
        }
    }

    // Handle state changes
    LaunchedEffect(customerState) {
        when (customerState) {
            is CustomerState.Success -> {
                snackbarMessage = (customerState as CustomerState.Success).message
                showSnackbar = true
                if ((customerState as CustomerState.Success).message.contains("saved")) {
                    // Navigate back after successful save
                    kotlinx.coroutines.delay(2000) // Show message briefly
                    onNavigateBack()
                }
            }
            is CustomerState.Error -> {
                snackbarMessage = (customerState as CustomerState.Error).message
                showSnackbar = true
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialCustomer == null) "Add Customer" else "Edit Customer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isNameError = name.isBlank()
                            isEmailError = email.isBlank() || !email.contains("@")

                            if (!isNameError && !isEmailError) {
                                val now = System.currentTimeMillis()
                                val customer = CustomerEntity(
                                    id = initialCustomer?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    email = email.trim(),
                                    phone = phone.trim(),
                                    company = company.trim(),
                                    createdAt = initialCustomer?.createdAt ?: now,
                                    updatedAt = now,
                                    isDeleted = false,
                                    syncState = SyncState.PENDING
                                )
                                customerViewModel.saveCustomer(customer)
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
                LaunchedEffect(Unit) {
                    delay(3000)
                    showSnackbar = false
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
                                text = "Saving customer...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Form fields with consistent spacing
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isNameError = false
                    },
                    label = { Text("Name *") },
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) Text("Name is required")
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    enabled = customerState !is CustomerState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        isEmailError = false
                    },
                    label = { Text("Email *") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    isError = isEmailError,
                    supportingText = {
                        if (isEmailError) Text("Enter a valid email")
                    },
                    enabled = customerState !is CustomerState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    enabled = customerState !is CustomerState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    enabled = customerState !is CustomerState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Import button with consistent spacing
                if (onImportRandom != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onImportRandom,
                        enabled = customerState !is CustomerState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Random Company")
                    }
                }

                // Bottom spacer to ensure content doesn't get cut off
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Reset snackbar when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            customerViewModel.resetState()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditPreviewScreen() {
    MiniCRMTheme {
        AddEditCustomerScreen(
            initialCustomer = null,
            onNavigateBack = {},
            onImportRandom = {},
            customerViewModel = viewModel()
        )
    }
}