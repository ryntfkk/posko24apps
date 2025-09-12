package com.example.posko24.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.posko24.data.model.Order
import com.example.posko24.data.model.OrderStatus
import com.example.posko24.ui.components.OrderCard

@Composable
fun MyOrderItem(
    order: Order,
    activeRole: String,
    onOrderClick: (String) -> Unit,
    onReviewClick: (String) -> Unit,
    onClaim: (String) -> Unit,
    onAccept: (String) -> Unit,
    onStart: (String) -> Unit,
    onFinish: (String) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onOrderClick(order.id) }) {
        OrderCard(order = order)
        Spacer(modifier = Modifier.height(8.dp))
        if (activeRole == "customer" && order.status == OrderStatus.COMPLETED.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { onReviewClick(order.id) }) {
                    Text("Review")
                }
            }
        }
        if (activeRole == "provider") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (order.status) {
                    OrderStatus.SEARCHING_PROVIDER.value -> {
                        Button(onClick = { onClaim(order.id) }) {
                            Text("Claim")
                        }
                    }
                    OrderStatus.PENDING.value -> {
                        Button(onClick = { onAccept(order.id) }) {
                            Text("Accept")
                        }
                    }
                    OrderStatus.ACCEPTED.value -> {
                        Button(onClick = { onStart(order.id) }) {
                            Text("Start")
                        }
                    }
                    OrderStatus.ONGOING.value -> {
                        Button(onClick = { onFinish(order.id) }) {
                            Text("Finish")
                        }
                    }
                }
            }
        }
    }
}