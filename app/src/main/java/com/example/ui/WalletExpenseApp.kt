package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Expense
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletExpenseApp(viewModel: ExpenseViewModel) {
    val expenses by viewModel.expensesState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Initialize initial seed data if DB is empty to show beautiful analytics on first load!
    LaunchedEffect(expenses) {
        if (expenses.isEmpty()) {
            viewModel.syncGoogleWalletCards()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05), Color(0xFFEA4335))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = viewModel.translate("app_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Display currency pill
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = viewModel.selectedCurrency.name,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = viewModel.currentScreen == "dashboard",
                    onClick = { viewModel.currentScreen = "dashboard" },
                    label = { Text(viewModel.translate("nav_dashboard")) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    modifier = Modifier.testTag("nav_btn_dashboard")
                )
                NavigationBarItem(
                    selected = viewModel.currentScreen == "wallet",
                    onClick = { viewModel.currentScreen = "wallet" },
                    label = { Text(viewModel.translate("nav_wallet")) },
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = "Wallet") },
                    modifier = Modifier.testTag("nav_btn_wallet")
                )
                NavigationBarItem(
                    selected = viewModel.currentScreen == "insights",
                    onClick = { viewModel.currentScreen = "insights" },
                    label = { Text(viewModel.translate("nav_insights")) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Insights") },
                    modifier = Modifier.testTag("nav_btn_insights")
                )
                NavigationBarItem(
                    selected = viewModel.currentScreen == "settings",
                    onClick = { viewModel.currentScreen = "settings" },
                    label = { Text(viewModel.translate("nav_settings")) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    modifier = Modifier.testTag("nav_btn_settings")
                )
            }
        },
        floatingActionButton = {
            if (viewModel.currentScreen == "dashboard" || viewModel.currentScreen == "wallet") {
                FloatingActionButton(
                    onClick = { viewModel.showAddManualDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .testTag("fab_add_expense")
                        .padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (viewModel.currentScreen) {
                "dashboard" -> DashboardScreen(viewModel, expenses)
                "wallet" -> WalletScreen(viewModel)
                "insights" -> InsightsScreen(viewModel, expenses)
                "settings" -> SettingsScreen(viewModel)
            }

            // Dialog for adding manual expenses
            if (viewModel.showAddManualDialog) {
                AddExpenseDialog(viewModel)
            }

            // Dialog indicating custom pass generated successfully
            if (viewModel.showPassGeneratedDialog) {
                val details = viewModel.lastGeneratedPassDetails
                Dialog(onDismissRequest = { viewModel.showPassGeneratedDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF34A853).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = Color(0xFF34A853),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = viewModel.translate("pass_sent_title"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${viewModel.translate("pass_sent_desc")} ${viewModel.selectedCurrency.symbol}${String.format(Locale.US, "%.2f", details?.second ?: 0.0)} at ${details?.first ?: "Merchant"}.",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Immersive digital Google Wallet pass graphic inside the popup!
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Google Wallet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Icon(Icons.Filled.Nfc, contentDescription = "NFC", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(details?.first ?: "NFC Coupon Receipt", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(viewModel.translate("card_loyalty"), color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = Color(0xFF1E293B))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    // Custom drawn barcode
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(35.dp)
                                    ) {
                                        val barCount = 42
                                        val spaceWidth = size.width / barCount
                                        val random = Random(12345)
                                        for (i in 0 until barCount) {
                                            val isBar = random.nextBoolean()
                                            if (isBar) {
                                                drawRect(
                                                    color = Color.White,
                                                    topLeft = Offset(i * spaceWidth, 0f),
                                                    size = androidx.compose.ui.geometry.Size(spaceWidth * 0.7f, size.height)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.showPassGeneratedDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: ExpenseViewModel, expenses: List<Expense>) {
    var isPulsing by remember { mutableStateOf(true) }
    val pulseAlpha by animateFloatAsState(
        targetValue = if (isPulsing) 1f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfcPulse"
    )

    // Calculate aggregated sums
    val totalInPreferred = expenses.sumOf {
        viewModel.convertCurrency(it.amount, it.currency, viewModel.selectedCurrency)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("dashboard_scroll_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // NFC Status Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .drawBehind {
                                drawCircle(
                                    color = Color(0xFF34A853).copy(alpha = pulseAlpha),
                                    radius = size.minDimension / 2
                                )
                            }
                    )
                    Text(
                        text = viewModel.translate("wallet_status_active"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Aggregate Expense Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_total_expenses"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = viewModel.translate("total_expenses"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${viewModel.selectedCurrency.symbol}${String.format(Locale.US, "%,.2f", totalInPreferred)}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(viewModel.translate("data_retrieved"), expenses.size),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Row(
                            modifier = Modifier
                                .clickable { viewModel.syncGoogleWalletCards() }
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = viewModel.translate("sync_now"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // Category Breakdown Card & Custom Segmented horizontal Bar chart
        if (expenses.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = viewModel.translate("currency_distribution"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Calculations
                        val categories = listOf("cat_food", "cat_transport", "cat_shopping", "cat_entertainment", "cat_utilities", "cat_other")
                        val colors = listOf(
                            Color(0xFFEA4335), // Food
                            Color(0xFF4285F4), // Transport
                            Color(0xFFFBBC05), // Shopping
                            Color(0xFF34A853), // Entertainment
                            Color(0xFF8F00FF), // Utilities
                            Color(0xFF6B7280)  // Other
                        )

                        val categoryTotals = categories.map { cat ->
                            expenses.filter { it.category == cat }.sumOf {
                                viewModel.convertCurrency(it.amount, it.currency, viewModel.selectedCurrency)
                            }
                        }

                        val overallTotal = categoryTotals.sum()

                        // Segmented bar representing ratios
                        if (overallTotal > 0) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(CircleShape)
                            ) {
                                var accumulatedX = 0f
                                categoryTotals.forEachIndexed { index, sum ->
                                    if (sum > 0) {
                                        val widthRatio = (sum / overallTotal).toFloat()
                                        val segmentWidth = size.width * widthRatio
                                        drawRect(
                                            color = colors[index],
                                            topLeft = Offset(accumulatedX, 0f),
                                            size = androidx.compose.ui.geometry.Size(segmentWidth, size.height)
                                        )
                                        accumulatedX += segmentWidth
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Grid listing the category totals
                        categories.forEachIndexed { index, cat ->
                            val total = categoryTotals[index]
                            if (total > 0) {
                                val percentage = (total / overallTotal) * 100
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(colors[index], CircleShape)
                                        )
                                        Text(
                                            text = viewModel.translate(cat),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "${viewModel.selectedCurrency.symbol}${String.format(Locale.US, "%.2f", total)} (${String.format(Locale.US, "%.1f", percentage)}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ledger of recent Google Wallet Transactions
        item {
            Text(
                text = "Google Wallet Transactions",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (expenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.translate("empty_state_text"),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(expenses) { expense ->
                TransactionRow(viewModel, expense)
            }
        }
    }
}

@Composable
fun TransactionRow(viewModel: ExpenseViewModel, expense: Expense) {
    var expanded by remember { mutableStateOf(false) }

    // Map category string to specific dynamic indicator colors
    val categoryColor = when (expense.category) {
        "cat_food" -> Color(0xFFEA4335)
        "cat_transport" -> Color(0xFF4285F4)
        "cat_shopping" -> Color(0xFFFBBC05)
        "cat_entertainment" -> Color(0xFF34A853)
        "cat_utilities" -> Color(0xFF8F00FF)
        else -> Color(0xFF6B7280)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("expense_item_${expense.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category accent circle
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(categoryColor, CircleShape)
                    )
                    Column {
                        Text(
                            text = expense.merchant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = viewModel.translate(expense.walletSource),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Amount
                val preferredAmount = viewModel.convertCurrency(expense.amount, expense.currency, viewModel.selectedCurrency)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${viewModel.selectedCurrency.symbol}${String.format(Locale.US, "%.2f", preferredAmount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // If display currency is different than primary transaction coin, show original too
                    if (expense.currency != viewModel.selectedCurrency.code) {
                        val originalSymbol = AppCurrency.values().find { it.code == expense.currency }?.symbol ?: expense.currency
                        Text(
                            text = "$originalSymbol${String.format(Locale.US, "%.2f", expense.amount)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanding sub-details card
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                ) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Date: ${SimpleDateFormat("MMM dd, yyyy @ HH:mm", Locale.getDefault()).format(Date(expense.timestamp))}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Category: ${viewModel.translate(expense.category)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (expense.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Notes: ${expense.notes}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.deleteExpense(expense) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(viewModel.translate("delete"), fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun WalletScreen(viewModel: ExpenseViewModel) {
    // Elegant interface mimicking GPay Card Stack & Custom coupon emitter
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Google Wallet Cards",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Visual simulated Cards list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SimulatedGpayCard(
                brush = Brush.linearGradient(colors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))),
                cardName = "Google Wallet (Visa Premium)",
                endingNo = "7741",
                cardholder = "Andranik T."
            )
            SimulatedGpayCard(
                brush = Brush.linearGradient(colors = listOf(Color(0xFF374151), Color(0xFF111827))),
                cardName = "Google Wallet (Mastercard Black)",
                endingNo = "3329",
                cardholder = "Andranik T."
            )
            SimulatedGpayCard(
                brush = Brush.linearGradient(colors = listOf(Color(0xFF78350F), Color(0xFFF59E0B))),
                cardName = "Google Wallet (Amex Corporate)",
                endingNo = "9951",
                cardholder = "Andranik T."
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = viewModel.translate("wallet_pass_simulator"),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            var passMerchant by remember { mutableStateOf("Armenian Grapes Co.") }
            var passAmount by remember { mutableStateOf("45.00") }
            var passCategory by remember { mutableStateOf("cat_shopping") }
            var passCurrency by remember { mutableStateOf("USD") }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = viewModel.translate("pass_title"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = passMerchant,
                    onValueChange = { passMerchant = it },
                    label = { Text(viewModel.translate("merchant")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = passAmount,
                        onValueChange = { passAmount = it },
                        label = { Text(viewModel.translate("amount")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        var expandedCurrency by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = passCurrency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(viewModel.translate("currency")) },
                            trailingIcon = {
                                IconButton(onClick = { expandedCurrency = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            AppCurrency.values().forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text("${curr.symbol} - ${curr.code}") },
                                    onClick = {
                                        passCurrency = curr.code
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }
                }

                Box {
                    var expandedCat by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = viewModel.translate(passCategory),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(viewModel.translate("category")) },
                        trailingIcon = {
                            IconButton(onClick = { expandedCat = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false }
                    ) {
                        val categories = listOf("cat_food", "cat_transport", "cat_shopping", "cat_entertainment", "cat_utilities", "cat_other")
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(viewModel.translate(cat)) },
                                onClick = {
                                    passCategory = cat
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val amt = passAmount.toDoubleOrNull() ?: 0.0
                        if (passMerchant.isNotEmpty() && amt > 0) {
                            viewModel.simulateWalletPassAdd(passMerchant, amt, passCurrency, passCategory)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_gpay_pass_add")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(viewModel.translate("pass_btn"))
                }
            }
        }
    }
}

@Composable
fun SimulatedGpayCard(brush: Brush, cardName: String, endingNo: String, cardholder: String) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cardName,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Filled.Nfc,
                    contentDescription = "Contactless payment",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp, 26.dp)
                    .background(Color(0xFFE2E8F0).copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = cardholder,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•••• •••• •••• $endingNo",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GP",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun InsightsScreen(viewModel: ExpenseViewModel, expenses: List<Expense>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = viewModel.translate("ai_insights_title"),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "Gemini Budget Analytics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Analyze notification receipts, convert original currencies automatically, and query machine-learning recommendations custom tailored to your limits.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { viewModel.generateAiInsights() },
                        enabled = !viewModel.isAiLoading,
                        modifier = Modifier.fillMaxWidth().testTag("btn_trigger_gemini_insights")
                    ) {
                        if (viewModel.isAiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.translate("ai_generating"))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.translate("ai_btn_generate"))
                        }
                    }
                }
            }
        }

        if (viewModel.aiInsight.isNotEmpty() || viewModel.isAiLoading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (viewModel.lastAiAnalysisTime.isNotEmpty()) {
                            Text(
                                text = "${viewModel.translate("ai_last_analyzed")} ${viewModel.lastAiAnalysisTime}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (viewModel.isAiLoading) {
                            Text(
                                text = "Preparing wallet charts and calling Gemini...",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                style = LocalTextStyle.current.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            )
                        } else {
                            Text(
                                text = viewModel.aiInsight,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: ExpenseViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = viewModel.translate("nav_settings"),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Language block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = viewModel.translate("language"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AppLanguage.values()) { lang ->
                            val isSelected = viewModel.selectedLanguage == lang
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.selectedLanguage = lang }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Currency block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = viewModel.translate("pref_currency"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AppCurrency.values()) { curr ->
                            val isSelected = viewModel.selectedCurrency == curr
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.selectedCurrency = curr }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${curr.symbol} (${curr.code})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Danger Controls & Information
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = viewModel.translate("about_app"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = viewModel.translate("about_desc"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Button(
                        onClick = { viewModel.clearAll() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Trash", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(viewModel.translate("clear_all_data"))
                    }
                }
            }
        }
    }
}

// Dialog Component for manual addition of receipts
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(viewModel: ExpenseViewModel) {
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("cat_food") }
    var currencyCode by remember { mutableStateOf(viewModel.selectedCurrency.code) }
    var source by remember { mutableStateOf("manual") }
    var notes by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { viewModel.showAddManualDialog = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = viewModel.translate("add_manual"),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Merchant input
                item {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text(viewModel.translate("merchant")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Amount and Currency select
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text(viewModel.translate("amount")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            var expandedCur by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = currencyCode,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(viewModel.translate("currency")) },
                                trailingIcon = {
                                    IconButton(onClick = { expandedCur = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = expandedCur,
                                onDismissRequest = { expandedCur = false }
                            ) {
                                AppCurrency.values().forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text("${curr.symbol} (${curr.code})") },
                                        onClick = {
                                            currencyCode = curr.code
                                            expandedCur = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Category
                item {
                    Box {
                        var expandedCat by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = viewModel.translate(category),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(viewModel.translate("category")) },
                            trailingIcon = {
                                IconButton(onClick = { expandedCat = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false }
                        ) {
                            val categories = listOf("cat_food", "cat_transport", "cat_shopping", "cat_entertainment", "cat_utilities", "cat_other")
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(viewModel.translate(cat)) },
                                    onClick = {
                                        category = cat
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Wallet Card Source select
                item {
                    Box {
                        var expandedCard by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = viewModel.translate(source),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(viewModel.translate("source")) },
                            trailingIcon = {
                                IconButton(onClick = { expandedCard = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedCard,
                            onDismissRequest = { expandedCard = false }
                        ) {
                            val cards = listOf("card_visa", "card_master", "card_amex", "manual")
                            cards.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(viewModel.translate(c)) },
                                    onClick = {
                                        source = c
                                        expandedCard = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Notes input
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(viewModel.translate("notes")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (showError) {
                    item {
                        Text(
                            text = viewModel.translate("field_required"),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Cancel / Save Buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.showAddManualDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(viewModel.translate("cancel"))
                        }
                        Button(
                            onClick = {
                                val amtDouble = amount.toDoubleOrNull()
                                if (merchant.isNotEmpty() && amtDouble != null && amtDouble > 0) {
                                    viewModel.addExpense(merchant, amtDouble, currencyCode, category, source, notes)
                                    viewModel.showAddManualDialog = false
                                } else {
                                    showError = true
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_save_expense")
                        ) {
                            Text(viewModel.translate("save"))
                        }
                    }
                }
            }
        }
    }
}
