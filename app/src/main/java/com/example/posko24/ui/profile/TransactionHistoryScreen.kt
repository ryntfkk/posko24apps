package com.example.posko24.ui.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posko24.ui.components.TransactionListItem

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    balance: Float,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val state by viewModel.transactionsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saldo & Riwayat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            BalanceCard(balance = balance.toDouble())
            Spacer(modifier = Modifier.height(16.dp))
            Text("Riwayat Transaksi", style = MaterialTheme.typography.titleMedium)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (val currentState = state) {
                    is TransactionsState.Loading -> CircularProgressIndicator()
                    is TransactionsState.Empty -> Text("Belum ada riwayat transaksi.")
                    is TransactionsState.Error -> Text(currentState.message)
                    is TransactionsState.Success -> {
                        LazyColumn {
                            items(currentState.transactions) { transaction ->
                                TransactionListItem(transaction = transaction)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceCard(balance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Saldo Anda Saat Ini", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Rp ${"%,d".format(balance.toInt())}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
