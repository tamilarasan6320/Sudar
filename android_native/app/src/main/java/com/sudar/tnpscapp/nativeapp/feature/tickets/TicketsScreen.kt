package com.sudar.tnpscapp.nativeapp.feature.tickets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.core.network.model.TicketDto
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(
    onBack: () -> Unit = {},
    onRaiseTicket: () -> Unit = {},
    onOpenTicket: (Int) -> Unit = {},
    viewModel: TicketsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load(refresh = true)
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Tickets",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load(refresh = true) }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRaiseTicket,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Raise Ticket") },
                containerColor = AppColors.Primary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }

            uiState.error != null -> {
                ErrorState(
                    message = uiState.error ?: "Something went wrong",
                    onRetry = { viewModel.load(refresh = true) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TicketsStatsCard(
                            total = uiState.stats?.total ?: uiState.tickets.size,
                            pending = uiState.stats?.pending ?: 0,
                            reviewed = uiState.stats?.reviewed ?: 0,
                            resolved = uiState.stats?.resolved ?: 0,
                            closed = uiState.stats?.closed ?: 0,
                        )
                    }

                    if (uiState.tickets.isEmpty()) {
                        item {
                            EmptyTicketsCard(onRaiseTicket = onRaiseTicket)
                        }
                    } else {
                        items(uiState.tickets, key = { it.id }) { ticket ->
                            TicketRow(
                                ticket = ticket,
                                onClick = { onOpenTicket(ticket.id) },
                            )
                        }
                    }

                    // Space above bottom nav / FAB
                    item { Spacer(modifier = Modifier.height(96.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TicketsStatsCard(
    total: Int,
    pending: Int,
    reviewed: Int,
    resolved: Int,
    closed: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overview",
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatChip(label = "Total", value = total.toString(), color = AppColors.Primary)
                StatChip(label = "Pending", value = pending.toString(), color = Color(0xFFFF6B6B))
                StatChip(label = "Resolved", value = resolved.toString(), color = Color(0xFF2ECC71))
                StatChip(label = "Closed", value = closed.toString(), color = Color(0xFF95A5A6))
            }
            if (reviewed > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Reviewed: $reviewed",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = AppColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TicketRow(
    ticket: TicketDto,
    onClick: () -> Unit,
) {
    val status = (ticket.status ?: "pending").lowercase(Locale.getDefault())
    val statusColor = when (status) {
        "resolved" -> Color(0xFF2ECC71)
        "reviewed" -> Color(0xFF4ECDC4)
        "closed" -> Color(0xFF95A5A6)
        else -> Color(0xFFFF6B6B) // pending
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppColors.Primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Feedback,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.subject?.ifBlank { "Ticket #${ticket.id}" } ?: "Ticket #${ticket.id}",
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = ticket.message?.trim().orEmpty()
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            val meta = buildString {
                val category = ticket.category?.trim().orEmpty()
                if (category.isNotBlank()) append(category.replaceFirstChar { it.uppercase() })
                val created = formatTicketDate(ticket.createdAt)
                if (created.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(created)
                }
            }

            if (meta.isNotBlank() || !ticket.adminResponse.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            fontSize = 11.sp,
                            color = AppColors.TextLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (!ticket.adminResponse.isNullOrBlank()) {
                        Text(
                            text = "Replied",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2ECC71)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTicketsCard(onRaiseTicket: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.CardBackground,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No tickets yet",
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Raise a ticket if you need help with the app.",
                fontSize = 13.sp,
                color = AppColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRaiseTicket,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Raise Ticket", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = AppColors.Error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) {
            Text("Retry", color = Color.White)
        }
    }
}

private fun formatTicketDate(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val output = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val d = input.parse(createdAt)
        if (d != null) output.format(d) else ""
    } catch (_: Exception) {
        ""
    }
}

