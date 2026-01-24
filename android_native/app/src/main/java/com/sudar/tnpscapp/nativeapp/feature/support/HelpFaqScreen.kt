package com.sudar.tnpscapp.nativeapp.feature.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

data class FaqItem(
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFaqScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val whatsappNumber = "918300321814"
    val displayNumber = "+91 83003 21814"

    val faqs = listOf(
        FaqItem(
            "How do I take a test?",
            "Go to the Tests tab on the home screen and select any available test. Click \"Start Test\" to begin answering questions."
        ),
        FaqItem(
            "Can I review my answers?",
            "Yes! After completing a test, you can review all your answers, see which ones were correct, and read explanations."
        ),
        FaqItem(
            "How is my score calculated?",
            "Each correct answer gives you points based on the test's marking scheme. Check the test details for specific scoring information."
        ),
        FaqItem(
            "How do I track my progress?",
            "Visit the Progress tab to see detailed analytics including tests taken, average score, rank, and performance trends."
        ),
        FaqItem(
            "How do I change my exam category?",
            "Tap on the exam dropdown at the top of the home screen and select your preferred exam category."
        ),
        FaqItem(
            "What if I lose internet connection during a test?",
            "Your progress is saved locally. You can continue the test when you reconnect, but make sure to submit before the time limit."
        ),
        FaqItem(
            "How do I contact support?",
            "Tap the \"Chat on WhatsApp\" button below to reach our support team instantly. You can also find contact information in the About section of your profile."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Help & FAQ",
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
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(AppColors.Primary, AppColors.PrimaryLight)
                            )
                        )
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Help,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Need Help?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Find answers to common questions",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // FAQ List Header
            Text(
                "Frequently Asked Questions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // FAQ Items
            faqs.forEach { faq ->
                FaqCard(faq)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // WhatsApp Support Button
            Button(
                onClick = { openWhatsAppSupport(context, whatsappNumber) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366) // WhatsApp green
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Chat on WhatsApp",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Support Number
            Text(
                "Support: $displayNumber",
                fontSize = 13.sp,
                color = AppColors.TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FaqCard(faq: FaqItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.Primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Help,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = faq.question,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = faq.answer,
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                lineHeight = 21.sp,
                modifier = Modifier.padding(start = 48.dp)
            )
        }
    }
}

private fun openWhatsAppSupport(context: Context, whatsappNumber: String) {
    val whatsappUrl = "https://wa.me/$whatsappNumber?text=${Uri.encode("Hi, I need help with the Sudar TNPSC App.")}"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
    }
}
