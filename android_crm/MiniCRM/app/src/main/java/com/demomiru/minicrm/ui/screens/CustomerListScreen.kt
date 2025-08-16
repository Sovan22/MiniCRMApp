package com.demomiru.minicrm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.demomiru.minicrm.data.models.CustomerEntity
import com.demomiru.minicrm.data.models.SyncState
import com.demomiru.minicrm.data.models.sampleCustomers
import com.demomiru.minicrm.ui.Destinations
import com.demomiru.minicrm.ui.theme.MiniCRMTheme
import com.demomiru.minicrm.viewmodel.CustomerState
import com.demomiru.minicrm.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    modifier: Modifier = Modifier,
    userName : String = "John Doe",
    onNavigateToAddCustomer: () -> Unit,
    onNavigateToEditCustomer: (String) -> Unit,
    onNavigateToCustomerDetail : (String) -> Unit,
    onLogOut: ()-> Unit,
    viewModel: CustomerViewModel = viewModel(),
    onNavigateBack :() -> Unit
){
    var query by remember { mutableStateOf("") }
    val customerState by viewModel.customerState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadCustomers()
    }
    val customersList = viewModel.customers.collectAsStateWithLifecycle()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Hey, $userName") },
                actions = {
                    TextButton(onClick = onLogOut) {
                        Text(color = Color.Red, text = "Log Out", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

            )
        },
        floatingActionButton = { FloatingActionButton(onClick= onNavigateToAddCustomer) { Icon(Icons.Filled.Add, contentDescription = null) } }
    ) {paddingValues ->
        Column(
            Modifier
                .padding(
                    paddingValues
                )
        ) {
//            Text("Padding: top=${paddingValues.calculateTopPadding()}, bottom=${paddingValues.calculateBottomPadding()}")
            OutlinedTextField(modifier= Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp), value=query, onValueChange={ value -> query = value}, label={Text("Search")})
            Divider(Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Customers", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 20.sp)
                IconButton(onClick = {viewModel.loadCustomers()}) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
            }
            LazyColumn {
                items(customersList.value) { customer ->
                    CustomerCard(customer, onClick = {onNavigateToCustomerDetail(customer.id)}, {onNavigateToEditCustomer(customer.id)})
                }
            }
        }
    }

}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    onClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: initials badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(16.dp))

            // Middle content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = customer.name.ifBlank { customer.company },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = customer.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = customer.company,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            // Sync status icon
            when (customer.syncState) {
                SyncState.SYNCED -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Synced",
                    tint = Color(0xFF4CAF50) // Green
                )
                else -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Pending Sync",
                    tint = Color(0xFFFF9800) // Orange
                )
            }

            // Menu button
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}



@Composable
fun ShimmerCustomerCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
            )

            Spacer(Modifier.width(16.dp))

            // Placeholder text lines
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "",
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.6f)
                        .clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = "",
                    modifier = Modifier
                        .height(12.dp)
                        .fillMaxWidth(0.4f)
                        .clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = "",
                    modifier = Modifier
                        .height(12.dp)
                        .fillMaxWidth(0.5f)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun CustomerListPreview(){
//    MiniCRMTheme {
//        CustomerListScreen(modifier = Modifier,"John Doe", {}, {}, {}, {},{})
//    }
//}