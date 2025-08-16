package com.demomiru.minicrm

import AuthViewModel
import android.R.attr.order
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demomiru.minicrm.data.models.sampleOrders
import com.demomiru.minicrm.ui.Destinations
import com.demomiru.minicrm.ui.screens.AddEditCustomerScreen
import com.demomiru.minicrm.ui.screens.AddEditOrderScreenWrapper
import com.demomiru.minicrm.ui.screens.CustomerDetailsScreen
import com.demomiru.minicrm.ui.screens.CustomerListScreen
import com.demomiru.minicrm.ui.screens.LoginScreen
import com.demomiru.minicrm.viewmodel.CustomerViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MiniCRMApp(customerViewModel: CustomerViewModel) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
//    val customerViewModel : CustomerViewModel = viewModel()
    // Observe auth state to handle navigation
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser = FirebaseAuth.getInstance().currentUser
    // Determine start destination based on auth state
    val startDestination = if (currentUser != null) {
        Destinations.CustomerList.route
    } else {
        Destinations.Login.route
    }
    Log.d("current User", currentUser?.email.toString())

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        MiniCRMNavHost(
            currentUser = currentUser,
            navController = navController,
            startDestination = startDestination,
            authViewModel = authViewModel,
            customerViewModel = customerViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }

    // Handle navigation based on auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                // Navigate to dashboard and clear back stack
                navController.navigate(Destinations.CustomerList.route) {
                    popUpTo(Destinations.Login.route) {
                        inclusive = true
                    }
                }
            }
            is AuthState.Idle -> {
                // User signed out, navigate to login
                if (navController.currentDestination?.route != Destinations.Login.route) {
                    navController.navigate(Destinations.Login.route) {
                        popUpTo(0) // Clear entire back stack
                    }
                }
            }
            else -> {
                // Loading or Error states - stay on current screen
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MiniCRMNavHost(
    currentUser : FirebaseUser?,
    navController: NavHostController,
    startDestination: String,
    authViewModel: AuthViewModel,
    customerViewModel: CustomerViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Login Screen
        composable(Destinations.Login.route) {
            LoginScreen(
                modifier = Modifier,
                viewModel = authViewModel,
                onNavigateToSingUp = { TODO() }
            )
        }

        // Customer List Screen
        composable(Destinations.CustomerList.route) {
            CustomerListScreen(
                userName = currentUser?.displayName?:"John Doe",
                onNavigateToAddCustomer = {
                    navController.navigate(Destinations.AddCustomer.route)
                },
                onNavigateToCustomerDetail = { customerId ->
                    navController.navigate(Destinations.CustomerDetail.createRoute(customerId))
                },
                onNavigateToEditCustomer = { customerId->
                    navController.navigate(Destinations.EditCustomer.createRoute(customerId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogOut = {
                    authViewModel.signOut()
                },
                viewModel = customerViewModel
            )
        }

        // Add Customer Screen
        composable(Destinations.AddCustomer.route) {
            AddEditCustomerScreen(
                customerViewModel= customerViewModel,
                onImportRandom = {
                    customerViewModel.importRandomCustomer()
                },
                onNavigateBack = {
//                    customerViewModel.loadCustomers()
                    navController.popBackStack()
                }

//                onSave = { it->
//                    authViewModel.saveInStore()
//                    navController.popBackStack()
//                }
            )
        }

        // Customer Detail Screen
        composable(Destinations.CustomerDetail.route) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                CustomerDetailsScreen(
                    customerId = customerId,
                    onEditClick = { id ->
                        navController.navigate(Destinations.EditCustomer.createRoute(id))
                    },
                    onAddOrderClick = { id->
                        navController.navigate(Destinations.AddOrder.createRoute(id))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    customerViewModel = customerViewModel
                )

        }

        // Edit Customer Screen
        composable(Destinations.EditCustomer.route) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            customerViewModel.loadCustomer(customerId)
            val customer by customerViewModel.selectedCustomer.collectAsStateWithLifecycle()
            AddEditCustomerScreen(
                initialCustomer = customer,
                onNavigateBack = {
                    navController.popBackStack()
                },
                customerViewModel = customerViewModel
            )
        }

        composable(Destinations.AddOrder.route) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""

            AddEditOrderScreenWrapper(
                customerId = customerId,
                orderId = null, // Add mode
                onNavigateBack = {
//                    customerViewModel.loadCustomerOrders(customerId)
                    navController.popBackStack()
                },
                customerViewModel = customerViewModel
            )
        }

        composable(Destinations.EditOrder.route) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""

            AddEditOrderScreenWrapper(
                customerId = customerId,
                customerViewModel = customerViewModel,
                orderId = orderId, // Edit mode
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}



//@Composable
//fun CustomerDetailScreen(
//    customerId: String,
//    onNavigateToEdit: (String) -> Unit,
//    onNavigateBack: () -> Unit
//) {
//    // Your customer detail implementation here
//}

@Composable
fun EditCustomerScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    onCustomerUpdated: () -> Unit
) {
    // Your edit customer implementation here
}

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit
) {
    // Your profile implementation here
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    // Your settings implementation here
}

// Navigation Extensions (Optional helper functions)
object NavigationHelper {

    fun NavHostController.navigateToCustomerDetail(customerId: String) {
        navigate(Destinations.CustomerDetail.createRoute(customerId))
    }

    fun NavHostController.navigateToEditCustomer(customerId: String) {
        navigate(Destinations.EditCustomer.createRoute(customerId))
    }

    fun NavHostController.navigateAndClearBackStack(destination: String) {
        navigate(destination) {
            popUpTo(0)
        }
    }

    fun NavHostController.navigateWithSingleTop(destination: String) {
        navigate(destination) {
            launchSingleTop = true
        }
    }
}