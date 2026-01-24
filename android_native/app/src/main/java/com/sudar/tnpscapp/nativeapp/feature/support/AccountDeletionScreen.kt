package com.sudar.tnpscapp.nativeapp.feature.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDeletionScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val email = "sudar.tnpscapp@gmail.com"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Account Deletion Request",
                        fontWeight = FontWeight.Bold,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        containerColor = AppColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AppColors.CardBackground
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AppColors.Error.copy(alpha = 0.1f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.DeleteForever,
                                contentDescription = null,
                                tint = AppColors.Error,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Account Deletion Request",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Sudar TNPSC App",
                        fontSize = 16.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // What Data Will Be Deleted
            InfoSection(
                icon = Icons.Outlined.Info,
                title = "What Data Will Be Deleted",
                content = {
                    Text(
                        "When you submit an account deletion request, we will permanently delete the following data linked to your account:",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary,
                        lineHeight = 21.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DataItem(Icons.Outlined.Phone, "Mobile number")
                    DataItem(Icons.Outlined.Person, "User profile (if provided)")
                    DataItem(Icons.Outlined.History, "Test history")
                    DataItem(Icons.Outlined.Assessment, "Test results and scores")
                    DataItem(Icons.Outlined.Settings, "Saved preferences")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // How Long Does It Take
            InfoSection(
                icon = Icons.Outlined.Schedule,
                title = "How Long Does It Take?",
                content = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Once you submit the deletion request, your account and all associated data will be deleted within 7 working days.",
                                fontSize = 14.sp,
                                color = AppColors.TextPrimary,
                                lineHeight = 21.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // How to Request
            InfoSection(
                icon = Icons.Outlined.Email,
                title = "How to Request Account Deletion",
                content = {
                    Text(
                        "You can delete your account by contacting us through the email below:",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary,
                        lineHeight = 21.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Email",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    email,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { sendEmail(context, email) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Send Email Request",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Warning Notice
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AppColors.Error.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "This action cannot be undone. Once your account is deleted, all your data will be permanently removed.",
                        fontSize = 13.sp,
                        color = AppColors.Error,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoSection(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun DataItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AppColors.TextPrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            fontSize = 14.sp,
            color = AppColors.TextPrimary
        )
    }
}

private fun sendEmail(context: Context, email: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, "Account Deletion Request - Sudar TNPSC App")
        putExtra(Intent.EXTRA_TEXT, "Please delete my account and all associated data.\n\nMobile Number: [Your Mobile Number]\n\nThank you.")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Email app not available. Email: $email", Toast.LENGTH_LONG).show()
    }
}
