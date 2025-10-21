package com.example.posko24.ui.profile

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.posko24.MainActivity
import com.example.posko24.R
import com.example.posko24.data.model.User
import com.example.posko24.ui.main.MainScreenStateHolder
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import com.example.posko24.util.APP_TIME_ZONE
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

// Data class untuk merepresentasikan setiap item menu di halaman profil
data class ProfileMenuItem(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    mainViewModel: MainScreenStateHolder,
    onNavigateToTransactions: (Float) -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    onNavigateToAddressSettings: () -> Unit,
    onNavigateToProviderOnboarding: () -> Unit
) {
    val state by viewModel.profileState.collectAsState()
    val activeRole by mainViewModel.activeRole.collectAsState()
    val availability by viewModel.availability.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.availabilityMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                is ProfileState.Loading -> CircularProgressIndicator()
                is ProfileState.Error -> Text(currentState.message)
                is ProfileState.Success -> {
                    val user = currentState.user
                    val providerProfile = currentState.providerProfile
                    var showAvailabilitySheet by rememberSaveable { mutableStateOf(false) }

                    // Daftar menu untuk customer
                    val customerMenuItems = listOf(
                        MenuSection(
                            "Akun", listOf(
                                ProfileMenuItem(Icons.Default.AccountBalanceWallet, "Saldo & Riwayat") { onNavigateToTransactions(user.balance.toFloat()) },
                                ProfileMenuItem(Icons.Default.AccountCircle, "Pengaturan Akun") { onNavigateToAccountSettings() },
                                ProfileMenuItem(Icons.Default.LocationOn, "Pengaturan Alamat") { onNavigateToAddressSettings() },
                            )
                        ),
                        MenuSection(
                            "Aplikasi", listOf(
                                ProfileMenuItem(Icons.Default.Notifications, "Notifikasi") { /* TODO: Navigasi ke halaman notifikasi */ },
                                ProfileMenuItem(Icons.Default.Security, "Keamanan") { /* TODO: Navigasi ke halaman keamanan */ },
                                ProfileMenuItem(Icons.AutoMirrored.Filled.HelpOutline, "Pusat Bantuan") { /* TODO: Navigasi ke halaman bantuan */ },
                            )
                        ),
                        MenuSection(
                            "Lainnya", listOf(
                                ProfileMenuItem(Icons.AutoMirrored.Filled.Notes, "Syarat & Ketentuan") { /* TODO: Tampilkan S&K */ },
                                ProfileMenuItem(Icons.AutoMirrored.Filled.Notes, "Kebijakan Privasi") { /* TODO: Tampilkan Kebijakan Privasi */ },
                            )
                        )
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { ProfileCard(user = user) }

                        if (user.roles.contains("provider")) {
                            item {
                                RoleSwitcher(
                                    activeRole = activeRole,
                                    onRoleChange = mainViewModel::setActiveRole,
                                    availabilityDates = availability,
                                    onManageAvailability = { showAvailabilitySheet = true }
                                )
                            }
                        }

                        customerMenuItems.forEach { section ->
                            item {
                                MenuSection(title = section.title) {
                                    section.items.forEach { menuItem ->
                                        MenuItem(
                                            icon = menuItem.icon,
                                            title = menuItem.title,
                                            onClick = menuItem.onClick
                                        )
                                    }
                                }
                            }
                        }

                        if (!user.roles.contains("provider")) {
                            item {
                                UpgradeProviderButton(onClick = onNavigateToProviderOnboarding)
                            }
                        }

                        item {
                            LogoutButton {
                                viewModel.logout()
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                                (context as? Activity)?.finish()
                            }
                        }
                    }

                    if (showAvailabilitySheet) {
                        AvailabilityBottomSheet(
                            initialSelection = availability,
                            onDismiss = { showAvailabilitySheet = false },
                            onSave = { selectedDates ->
                                val isoDates = selectedDates.map { it.toString() }
                                viewModel.saveAvailability(isoDates)
                                showAvailabilitySheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// Composable untuk header profil
@Composable
fun ProfileCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.profilePictureUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto profil ${user.fullName}",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.ic_launcher_foreground)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.email.ifBlank { user.phoneNumber },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Composable untuk bagian menu
data class MenuSection(val title: String, val items: List<ProfileMenuItem>)

@Composable
fun MenuSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(content = content)
        }
    }
}

// Composable untuk satu item menu
@Composable
fun MenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "Lanjutkan",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Composable untuk tombol ganti peran
@Composable
fun RoleSwitcher(
    activeRole: String,
    onRoleChange: (String) -> Unit,
    availabilityDates: List<LocalDate>,
    onManageAvailability: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mode Aplikasi", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                SegmentedButton(
                    items = listOf("Customer", "Provider"),
                    selectedIndex = if (activeRole == "provider") 1 else 0,
                    onItemSelected = { index ->
                        onRoleChange(if (index == 1) "provider" else "customer")
                    }
                )
            }
            if (activeRole == "provider") {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Jadwal Tersedia",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (availabilityDates.isEmpty()) {
                        Text(
                            text = "Belum ada jadwal dipilih.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val sortedDates = availabilityDates.sorted()
                        val maxVisible = 4
                        val visibleDates = sortedDates.take(maxVisible)
                        val remainingCount = (sortedDates.size - maxVisible).coerceAtLeast(0)

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(visibleDates) { date ->
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = {
                                        Text(date.toSummaryLabel())
                                    }
                                )
                            }
                            if (remainingCount > 0) {
                                item {
                                    AssistChip(
                                        onClick = {},
                                        enabled = false,
                                        label = { Text("+${remainingCount}") }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = onManageAvailability) {
                        Text("Atur Jadwal")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityBottomSheet(
    initialSelection: List<LocalDate>,
    onDismiss: () -> Unit,
    onSave: (List<LocalDate>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val selectedDates = remember { mutableStateListOf<LocalDate>() }

    LaunchedEffect(initialSelection) {
        selectedDates.clear()
        selectedDates.addAll(initialSelection)
    }

    val today = remember {
        Clock.System.now().toLocalDateTime(APP_TIME_ZONE).date
    }
    val availableDates = remember(today) {
        (0 until 60).map { today.plus(DatePeriod(days = it)) }
    }

    ModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Atur Jadwal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Pilih tanggal ketersediaan untuk 60 hari ke depan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                val groupedDates = availableDates.groupBy { it.year to it.monthNumber }
                groupedDates.forEach { (yearMonth, datesInMonth) ->
                    val (year, monthNumber) = yearMonth
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = monthYearLabel(year, monthNumber),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(datesInMonth, key = { it.toString() }) { date ->
                        AvailabilityDateItem(
                            date = date,
                            isSelected = selectedDates.contains(date),
                            onClick = {
                                if (selectedDates.contains(date)) {
                                    selectedDates.remove(date)
                                } else {
                                    selectedDates.add(date)
                                }
                            }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            onSave(selectedDates.sorted())
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}

@Composable
private fun AvailabilityDateItem(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.aspectRatio(1f),
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButton(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow {
        items.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                onClick = { onItemSelected(index) },
                selected = index == selectedIndex
            ) {
                Text(label, fontSize = 12.sp)
            }
        }
    }
}

// Composable untuk tombol "Daftar sebagai Provider"
@Composable
fun UpgradeProviderButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Daftar sebagai Provider")
    }
}

// Composable untuk tombol logout
@Composable
fun LogoutButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Keluar")
    }
}

// Fungsi pembantu untuk format mata uang
private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amount).replace("Rp", "Rp ")
}

private val monthNamesShort = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
private val monthNamesFull = listOf(
    "Januari",
    "Februari",
    "Maret",
    "April",
    "Mei",
    "Juni",
    "Juli",
    "Agustus",
    "September",
    "Oktober",
    "November",
    "Desember"
)

private fun LocalDate.toSummaryLabel(): String {
    val day = dayOfMonth.toString().padStart(2, '0')
    val month = monthNamesShort[monthNumber - 1]
    return "$day $month"
}

private fun monthYearLabel(year: Int, monthNumber: Int): String {
    val month = monthNamesFull[monthNumber - 1]
    return "$month $year"
}