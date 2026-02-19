package com.sudar.tnpscapp.nativeapp.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.core.network.model.TicketDto
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailsScreen(
    ticketId: Int,
    onBack: () -> Unit = {},
    viewModel: TicketDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(ticketId) {
        viewModel.load(ticketId)
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ticket #$ticketId",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(uiState.error ?: "Failed to load ticket", color = AppColors.Error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.load(ticketId) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }

            else -> {
                val ticket = uiState.ticket
                if (ticket == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ticket not found", color = AppColors.TextSecondary)
                    }
                    return@Scaffold
                }

                TicketDetailsContent(
                    ticket = ticket,
                    isClosing = uiState.isClosing,
                    onClose = { viewModel.close(ticketId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
private fun TicketDetailsContent(
    ticket: TicketDto,
    isClosing: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = (ticket.status ?: "pending").lowercase(Locale.getDefault())
    val statusColor = when (status) {
        "resolved" -> Color(0xFF2ECC71)
        "reviewed" -> Color(0xFF4ECDC4)
        "closed" -> Color(0xFF95A5A6)
        else -> Color(0xFFFF6B6B)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppColors.CardBackground
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = ticket.subject?.ifBlank { "Ticket #${ticket.id}" } ?: "Ticket #${ticket.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    val category = ticket.category?.trim().orEmpty()
                    if (category.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = AppColors.Primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = category.replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Primary
                            )
                        }
                    }
                }

                val created = formatTicketDate(ticket.createdAt)
                if (created.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Created: $created",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }

        MessageCard(
            title = "Your Message",
            icon = Icons.Outlined.Person,
            gradient = Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight)),
            body = ticket.message.orEmpty(),
        )

        val adminReply = ticket.adminResponse?.trim().orEmpty()
        if (adminReply.isNotBlank()) {
            MessageCard(
                title = "Admin Reply",
                icon = Icons.Outlined.AdminPanelSettings,
                gradient = Brush.linearGradient(listOf(Color(0xFF2ECC71), Color(0xFF4ECDC4))),
                body = adminReply,
                footer = ticket.adminResponseAt?.let { at ->
                    val d = formatTicketDate(at)
                    if (d.isBlank()) null else "Replied: $d"
                }
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.CardBackground
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = AppColors.TextLight
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("No reply yet", fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Your ticket is in review. We will respond soon.",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
        }

        if (status != "closed") {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClosing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF95A5A6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isClosing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Close Ticket", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MessageCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    body: String,
    footer: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.CardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(38.dp)
                        .background(gradient, RoundedCornerShape(10.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = body.ifBlank { "-" },
                color = AppColors.TextSecondary,
                lineHeight = 20.sp
            )

            if (!footer.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = footer,
                    fontSize = 12.sp,
                    color = AppColors.TextLight
                )
            }
        }
    }
}

private fun formatTicketDate(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val output = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val d = input.parse(value)
        if (d != null) output.format(d) else ""
    } catch (_: Exception) {
        ""
    }
}

