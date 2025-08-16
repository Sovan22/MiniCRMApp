package com.demomiru.minicrm

import android.R.attr.label
import android.R.attr.name
import android.R.attr.onClick
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.demomiru.minicrm.data.CRMDatabase
import com.demomiru.minicrm.data.MockApiServer
import com.demomiru.minicrm.data.api
import com.demomiru.minicrm.ui.screens.LoginScreen
import com.demomiru.minicrm.ui.theme.MiniCRMTheme
import com.demomiru.minicrm.viewmodel.CustomerViewModel
import com.demomiru.minicrm.viewmodel.CustomerViewModelFactory
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CustomerViewModel
    private var server: MockApiServer? = null
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        val database = CRMDatabase.getDatabase(this)
        val repository =
            RepositoryFactory.createCustomerRepository(
                database.customerDao(),
                database.orderDao()
            )

        viewModel = ViewModelProvider(
            this,
            CustomerViewModelFactory(repository)
        )[CustomerViewModel::class.java]
        server = MockApiServer(this)
        server?.start()
        setContent {
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    val customers = api.getCustomers()
                    Log.d("customer api", customers[0].toString())
                }
            }
            MiniCRMTheme {
                    MiniCRMApp(customerViewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
    }
}


