package com.demomiru.minicrm.ui
sealed class Destinations(val route: String) {
    object Login : Destinations("login")
    object Dashboard : Destinations("dashboard")
    object CustomerList : Destinations("customer_list")
    object AddCustomer : Destinations("add_customer")
    object CustomerDetail : Destinations("customer_detail/{customerId}") {
        fun createRoute(customerId: String) = "customer_detail/$customerId"
    }
    object EditCustomer : Destinations("edit_customer/{customerId}") {
        fun createRoute(customerId: String) = "edit_customer/$customerId"
    }
    object AddOrder : Destinations("add_order/{customerId}") {
        fun createRoute(customerId: String) = "add_order/$customerId"
    }
    object EditOrder : Destinations("edit_order/{customerId}/{orderId}") {
        fun createRoute(customerId: String, orderId: String) = "edit_order/$customerId/$orderId"
    }
    object Profile : Destinations("profile")
    object Settings : Destinations("settings")

    companion object {
        // Helper function to get all routes for navigation graph
        fun getAllRoutes(): List<String> = listOf(
            Login.route,
            Dashboard.route,
            CustomerList.route,
            AddCustomer.route,
            CustomerDetail.route,
            EditCustomer.route,
            Profile.route,
            Settings.route
        )
    }
}