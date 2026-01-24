package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sudar.tnpscapp.nativeapp.core.network.UrlUtils
import com.sudar.tnpscapp.nativeapp.core.network.model.QuestionSessionDto
import com.sudar.tnpscapp.nativeapp.core.network.model.TestCategoryDto
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

/**
 * Tests Page - Exact Flutter replica
 * Shows categories in 3-column grid, tests in 2-column grid when category selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestCategoriesScreen(
    onBack: () -> Unit,
    onOpenCategory: (Int) -> Unit,
    onStartTest: (sessionId: Int, title: String, categoryName: String, duration: Int, totalQuestions: Int) -> Unit = { _, _, _, _, _ -> },
    viewModel: TestCategoriesViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState
    var selectedCategory by remember { mutableStateOf<TestCategoryDto?>(null) }

    // Handle back press
    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.Background,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button (only when category selected)
                    if (selectedCategory != null) {
                        IconButton(onClick = { selectedCategory = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = "Back",
                                tint = AppColors.TextPrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    // Title
                    Text(
                        text = selectedCategory?.name ?: "Test Categories",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    // Refresh button
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppColors.TextPrimary
                        )
                    }
                }
            }
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }
            selectedCategory != null -> {
                TestsGrid(
                    modifier = Modifier.padding(paddingValues),
                    sessions = uiState.sessions.filter { it.testCategoryId == selectedCategory!!.id },
                    completedTests = uiState.completedTests,
                    onTestClick = { session, isCompleted ->
                        // Handle test tap
                        if (isCompleted) {
                            // Show retake dialog
                            viewModel.showRetakeDialog(session)
                        } else {
                            // Start test directly
                            onStartTest(
                                session.id,
                                session.name ?: "Test",
                                selectedCategory!!.name,
                                session.duration ?: 60,
                                session.actualQuestionCount ?: session.totalQuestions ?: 0
                            )
                        }
                    }
                )
            }
            else -> {
                CategoriesGrid(
                    modifier = Modifier.padding(paddingValues),
                    categories = uiState.categories,
                    sessions = uiState.sessions,
                    completedTests = uiState.completedTests,
                    onCategoryClick = { category ->
                        selectedCategory = category
                    }
                )
            }
        }
    }

    // Retake Dialog
    uiState.retakeDialogSession?.let { session ->
        val completedTest = uiState.completedTests[session.id]
        val score = completedTest?.percentage?.toDouble() ?: 0.0
        val correctAnswers = completedTest?.correct ?: 0
        val totalAnswered = completedTest?.totalQuestions ?: 0
        val isPassed = score >= 50

        AlertDialog(
            onDismissRequest = { viewModel.dismissRetakeDialog() },
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPassed) AppColors.Success.copy(alpha = 0.1f) else AppColors.Warning.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPassed) Icons.Filled.EmojiEvents else Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = if (isPassed) AppColors.Success else AppColors.Warning,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Test Completed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    // Score Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPassed) AppColors.Success.copy(alpha = 0.1f) else AppColors.Warning.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${score.toInt()}%",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPassed) AppColors.Success else AppColors.Warning
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPassed) "🎉 Excellent!" else "💪 Keep Practicing!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center
                            ) {
                                StatItem(
                                    icon = Icons.Filled.CheckCircle,
                                    value = correctAnswers.toString(),
                                    label = "Correct",
                                    color = AppColors.Success
                                )
                                Spacer(modifier = Modifier.width(24.dp))
                                StatItem(
                                    icon = Icons.Filled.Cancel,
                                    value = (totalAnswered - correctAnswers).toString(),
                                    label = "Wrong",
                                    color = AppColors.Error
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Would you like to retake this test?",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissRetakeDialog()
                        // Navigate to test
                        onStartTest(
                            session.id,
                            session.name ?: "Test",
                            session.categoryName ?: selectedCategory?.name ?: "",
                            session.duration ?: 60,
                            session.actualQuestionCount ?: session.totalQuestions ?: 0
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retake", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRetakeDialog() }) {
                    Text("Cancel", color = AppColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun CategoriesGrid(
    modifier: Modifier = Modifier,
    categories: List<TestCategoryDto>,
    sessions: List<QuestionSessionDto>,
    completedTests: Map<Int, com.sudar.tnpscapp.nativeapp.core.network.model.TestHistoryItemDto>,
    onCategoryClick: (TestCategoryDto) -> Unit
) {
    if (categories.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    tint = AppColors.TextLight,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No categories available",
                    fontSize = 16.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
        return
    }

    val colors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Violet
        Color(0xFF06B6D4), // Cyan
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 3 cards per row
        categories.chunked(3).forEachIndexed { rowIndex, rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCategories.forEachIndexed { index, category ->
                    val colorIndex = rowIndex * 3 + index
                    val testsInCategory = sessions.count { it.testCategoryId == category.id }

                    CategoryCard(
                        modifier = Modifier.weight(1f),
                        category = category,
                        testsCount = testsInCategory,
                        color = colors[colorIndex % colors.size],
                        onClick = { onCategoryClick(category) }
                    )
                }
                // Fill empty space
                repeat(3 - rowCategories.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CategoryCard(
    modifier: Modifier = Modifier,
    category: TestCategoryDto,
    testsCount: Int,
    color: Color,
    onClick: () -> Unit
) {
    val icon = getCategoryIcon(category.name)

    // Check if has image and resolve URL
    val rawImagePath = category.imagePath ?: category.image
    val imageUrl = UrlUtils.resolveTestCategoryImageUrl(rawImagePath)
    val hasImage = imageUrl != null

    if (hasImage) {
        // Image card
        Card(
            modifier = modifier.height(180.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = category.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        // Icon card
        Card(
            modifier = modifier.height(180.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick)
                    .padding(14.dp)
            ) {
                // Top row: Icon and count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = testsCount.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Category name
                Text(
                    text = category.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar (placeholder - would need completed count)
                LinearProgressIndicator(
                    progress = { 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
private fun TestsGrid(
    modifier: Modifier = Modifier,
    sessions: List<QuestionSessionDto>,
    completedTests: Map<Int, com.sudar.tnpscapp.nativeapp.core.network.model.TestHistoryItemDto>,
    onTestClick: (QuestionSessionDto, Boolean) -> Unit
) {
    if (sessions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = AppColors.TextLight,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No tests available",
                    fontSize = 16.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 2 cards per row
        sessions.chunked(2).forEach { rowSessions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowSessions.forEach { session ->
                    val completedTest = completedTests[session.id]
                    val isCompleted = completedTest != null

                    TestCard(
                        modifier = Modifier.weight(1f),
                        session = session,
                        completedTest = completedTest,
                        onClick = { onTestClick(session, isCompleted) }
                    )
                }
                // Fill empty space
                if (rowSessions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// Theme colors for different test categories
private data class TestTheme(
    val icon: ImageVector,
    val primaryColor: Color
)

private fun getTestTheme(title: String): TestTheme {
    val t = title.lowercase()
    return when {
        // Biology / Botany / Zoology / Life Science
        t.contains("botany") || t.contains("zoology") || t.contains("biology") ||
        t.contains("organism") || t.contains("plant") || t.contains("animal") ||
        t.contains("cell") || t.contains("species") || t.contains("living") ||
        t.contains("life science") -> TestTheme(
            icon = Icons.Rounded.Biotech, primaryColor = Color(0xFF10B981) // Emerald
        )
        // Evolution
        t.contains("evolution") -> TestTheme(
            icon = Icons.Rounded.Spa, primaryColor = Color(0xFF06B6D4) // Cyan
        )
        // Genetics / DNA
        t.contains("genetic") || t.contains("dna") || t.contains("heredit") -> TestTheme(
            icon = Icons.Rounded.Fingerprint, primaryColor = Color(0xFF8B5CF6) // Purple
        )
        // Physiology / Human Body
        t.contains("physiology") || t.contains("human body") || t.contains("organ") ||
        t.contains("blood") || t.contains("heart") || t.contains("brain") ||
        t.contains("nerve") || t.contains("muscle") -> TestTheme(
            icon = Icons.Rounded.Favorite, primaryColor = Color(0xFFEC4899) // Pink
        )
        // Nutrition / Food / Diet
        t.contains("nutrition") || t.contains("food") || t.contains("diet") ||
        t.contains("vitamin") || t.contains("protein") || t.contains("carbohydrate") -> TestTheme(
            icon = Icons.Rounded.Restaurant, primaryColor = Color(0xFFF59E0B) // Amber
        )
        // Classification / Taxonomy
        t.contains("classification") || t.contains("taxonomy") -> TestTheme(
            icon = Icons.Rounded.Category, primaryColor = Color(0xFF3B82F6) // Blue
        )
        // Main Concepts / Concepts / Basics / Introduction
        t.contains("main concept") || t.contains("concept") || t.contains("basic") ||
        t.contains("fundamental") || t.contains("introduction") || t.contains("overview") -> TestTheme(
            icon = Icons.Rounded.Lightbulb, primaryColor = Color(0xFFFBBF24) // Yellow
        )
        // Chemistry
        t.contains("chemistry") || t.contains("chemical") || t.contains("atom") ||
        t.contains("molecule") || t.contains("element") || t.contains("compound") ||
        t.contains("reaction") || t.contains("acid") || t.contains("base") -> TestTheme(
            icon = Icons.Rounded.Science, primaryColor = Color(0xFFFF6B6B) // Coral
        )
        // Physics
        t.contains("physics") || t.contains("motion") || t.contains("force") ||
        t.contains("energy") || t.contains("light") || t.contains("sound") ||
        t.contains("electricity") || t.contains("magnet") || t.contains("wave") -> TestTheme(
            icon = Icons.Rounded.Bolt, primaryColor = Color(0xFF7C3AED) // Violet
        )
        // Environment / Ecology / Ecosystem
        t.contains("environment") || t.contains("ecology") || t.contains("ecosystem") ||
        t.contains("pollution") || t.contains("forest") || t.contains("wildlife") ||
        t.contains("biodiversity") || t.contains("climate change") -> TestTheme(
            icon = Icons.Rounded.Park, primaryColor = Color(0xFF22C55E) // Green
        )
        // Health / Disease / Medicine
        t.contains("health") || t.contains("disease") || t.contains("medicine") ||
        t.contains("medical") || t.contains("virus") || t.contains("bacteria") -> TestTheme(
            icon = Icons.Rounded.LocalHospital, primaryColor = Color(0xFFEF4444) // Red
        )
        // Math / Aptitude / Reasoning
        t.contains("math") || t.contains("aptitude") || t.contains("mental") ||
        t.contains("reasoning") || t.contains("arithmetic") || t.contains("number") ||
        t.contains("algebra") || t.contains("quantitative") || t.contains("calculation") -> TestTheme(
            icon = Icons.Rounded.Calculate, primaryColor = Color(0xFFF97316) // Orange
        )
        // Ancient History / பண்டைய வரலாறு
        t.contains("ancient") || t.contains("பண்டைய") || t.contains("indus") ||
        t.contains("harappa") || t.contains("vedic") || t.contains("maurya") ||
        t.contains("gupta") || t.contains("stone age") || t.contains("bronze") -> TestTheme(
            icon = Icons.Rounded.Explore, primaryColor = Color(0xFF92400E) // Brown
        )
        // Medieval History / இடைக்கால வரலாறு
        t.contains("medieval") || t.contains("இடைக்கால") || t.contains("sultanate") ||
        t.contains("mughal") || t.contains("delhi") || t.contains("vijayanagar") ||
        t.contains("bahmani") -> TestTheme(
            icon = Icons.Rounded.AccountBalance, primaryColor = Color(0xFF7C3AED) // Violet
        )
        // Modern History / நவீன வரலாறு
        t.contains("modern history") || t.contains("நவீன") || t.contains("british") ||
        t.contains("colonial") || t.contains("east india") || t.contains("company") -> TestTheme(
            icon = Icons.Rounded.HistoryEdu, primaryColor = Color(0xFF0891B2) // Cyan
        )
        // Freedom Movement / சுதந்திர போராட்டம்
        t.contains("freedom") || t.contains("independence") || t.contains("சுதந்திர") ||
        t.contains("revolt") || t.contains("movement") || t.contains("gandhi") ||
        t.contains("nehru") || t.contains("subhas") || t.contains("bhagat") ||
        t.contains("quit india") || t.contains("non-cooperation") || t.contains("civil disobedience") -> TestTheme(
            icon = Icons.Rounded.EmojiEvents, primaryColor = Color(0xFFEF4444) // Red
        )
        // Tamil Nadu History / தமிழ்நாடு வரலாறு
        t.contains("tamil nadu") || t.contains("தமிழ்நாடு") || t.contains("chola") ||
        t.contains("சோழ") || t.contains("pandya") || t.contains("பாண்டிய") ||
        t.contains("pallava") || t.contains("பல்லவ") || t.contains("chera") ||
        t.contains("சேர") || t.contains("nayak") || t.contains("நாயக்க") -> TestTheme(
            icon = Icons.Rounded.Place, primaryColor = Color(0xFFD97706) // Amber
        )
        // World History / உலக வரலாறு
        t.contains("world history") || t.contains("உலக வரலாறு") || t.contains("world war") ||
        t.contains("french revolution") || t.contains("american") || t.contains("russian") ||
        t.contains("renaissance") || t.contains("industrial") -> TestTheme(
            icon = Icons.Rounded.Public, primaryColor = Color(0xFF3B82F6) // Blue
        )
        // Indian History General / இந்திய வரலாறு
        t.contains("indian history") || t.contains("இந்திய வரலாறு") ||
        t.contains("india") -> TestTheme(
            icon = Icons.Rounded.LocationCity, primaryColor = Color(0xFFF59E0B) // Amber
        )
        // History General
        t.contains("history") || t.contains("வரலாறு") -> TestTheme(
            icon = Icons.Rounded.HistoryEdu, primaryColor = Color(0xFFD97706) // Brown
        )
        // Geography / Map
        t.contains("geography") || t.contains("map") || t.contains("climate") ||
        t.contains("river") || t.contains("mountain") || t.contains("ocean") ||
        t.contains("soil") || t.contains("mineral") || t.contains("terrain") -> TestTheme(
            icon = Icons.Rounded.Public, primaryColor = Color(0xFF0EA5E9) // Sky Blue
        )
        // Economy / Economics
        t.contains("economy") || t.contains("economic") || t.contains("finance") ||
        t.contains("banking") || t.contains("budget") || t.contains("tax") ||
        t.contains("gdp") || t.contains("rbi") || t.contains("inflation") -> TestTheme(
            icon = Icons.Rounded.AccountBalance, primaryColor = Color(0xFF14B8A6) // Teal
        )
        // Polity / Constitution / Law
        t.contains("polity") || t.contains("constitution") || t.contains("law") ||
        t.contains("governance") || t.contains("parliament") || t.contains("judiciary") ||
        t.contains("amendment") || t.contains("rights") || t.contains("duties") -> TestTheme(
            icon = Icons.Rounded.Gavel, primaryColor = Color(0xFF7C3AED) // Violet
        )
        // Current Affairs / News
        t.contains("current") || t.contains("affairs") || t.contains("news") ||
        t.contains("recent") || t.contains("latest") || t.contains("update") ||
        t.contains("2024") || t.contains("2025") || t.contains("2026") -> TestTheme(
            icon = Icons.Rounded.Newspaper, primaryColor = Color(0xFFEF4444) // Red
        )
        // Thirukkural / திருக்குறள்
        t.contains("thirukkural") || t.contains("kural") || t.contains("திருக்குறள்") ||
        t.contains("valluvar") || t.contains("வள்ளுவர்") -> TestTheme(
            icon = Icons.Rounded.AutoStories, primaryColor = Color(0xFFD97706) // Amber/Gold
        )
        // Tamil Grammar / இலக்கணம்
        t.contains("grammar") || t.contains("இலக்கணம்") || t.contains("ilakkanam") ||
        t.contains("எழுத்து") || t.contains("சொல்") || t.contains("வாக்கியம்") -> TestTheme(
            icon = Icons.Rounded.Spellcheck, primaryColor = Color(0xFF7C3AED) // Violet
        )
        // Tamil Literature / இலக்கியம் / Sangam
        t.contains("literature") || t.contains("இலக்கியம்") || t.contains("ilakkiyam") ||
        t.contains("sangam") || t.contains("சங்க") || t.contains("poet") ||
        t.contains("புலவர்") || t.contains("கவிதை") || t.contains("poetry") -> TestTheme(
            icon = Icons.Rounded.MenuBook, primaryColor = Color(0xFF059669) // Emerald
        )
        // Tamil Prose / உரைநடை
        t.contains("prose") || t.contains("உரைநடை") || t.contains("essay") ||
        t.contains("கட்டுரை") || t.contains("சிறுகதை") || t.contains("story") -> TestTheme(
            icon = Icons.Rounded.Article, primaryColor = Color(0xFF0891B2) // Cyan
        )
        // Synonyms / Antonyms / ஒத்த சொற்கள்
        t.contains("synonym") || t.contains("antonym") || t.contains("ஒத்த") ||
        t.contains("எதிர்") || t.contains("opposite") || t.contains("similar") -> TestTheme(
            icon = Icons.Rounded.SwapHoriz, primaryColor = Color(0xFFF59E0B) // Amber
        )
        // Comprehension / புரிதல்
        t.contains("comprehension") || t.contains("புரிதல்") || t.contains("passage") ||
        t.contains("reading") || t.contains("பத்தி") -> TestTheme(
            icon = Icons.Rounded.Search, primaryColor = Color(0xFF6366F1) // Indigo
        )
        // Vocabulary / சொற்களஞ்சியம்
        t.contains("vocabulary") || t.contains("சொற்களஞ்சியம்") || t.contains("word") ||
        t.contains("சொல்வளம்") -> TestTheme(
            icon = Icons.Rounded.TextFields, primaryColor = Color(0xFF10B981) // Emerald
        )
        // Tamil General / தமிழ்
        t.contains("tamil") || t.contains("தமிழ்") -> TestTheme(
            icon = Icons.Rounded.Translate, primaryColor = Color(0xFFE11D48) // Rose
        )
        // English
        t.contains("english") || t.contains("ஆங்கிலம்") -> TestTheme(
            icon = Icons.Rounded.Language, primaryColor = Color(0xFF3B82F6) // Blue
        )
        // Hindi
        t.contains("hindi") || t.contains("இந்தி") -> TestTheme(
            icon = Icons.Rounded.Translate, primaryColor = Color(0xFFEF4444) // Red
        )
        // Language General
        t.contains("language") -> TestTheme(
            icon = Icons.Rounded.Translate, primaryColor = Color(0xFFE11D48) // Rose
        )
        // Computer / IT / Technology
        t.contains("computer") || t.contains("technology") || t.contains("software") ||
        t.contains("internet") || t.contains("digital") || t.contains("cyber") ||
        t.contains("programming") || t.contains("hardware") -> TestTheme(
            icon = Icons.Rounded.Computer, primaryColor = Color(0xFF6366F1) // Indigo
        )
        // Sports / Games
        t.contains("sport") || t.contains("game") || t.contains("cricket") ||
        t.contains("olympic") || t.contains("player") || t.contains("tournament") ||
        t.contains("champion") || t.contains("medal") -> TestTheme(
            icon = Icons.Rounded.SportsScore, primaryColor = Color(0xFF059669) // Emerald Dark
        )
        // Art / Culture / Heritage
        t.contains("art") || t.contains("culture") || t.contains("heritage") ||
        t.contains("dance") || t.contains("music") || t.contains("temple") ||
        t.contains("festival") || t.contains("tradition") || t.contains("folk") -> TestTheme(
            icon = Icons.Rounded.Palette, primaryColor = Color(0xFFDB2777) // Fuchsia
        )
        // Agriculture / Farming
        t.contains("agriculture") || t.contains("farming") || t.contains("crop") ||
        t.contains("irrigation") || t.contains("harvest") || t.contains("seed") -> TestTheme(
            icon = Icons.Rounded.Grass, primaryColor = Color(0xFF65A30D) // Lime
        )
        // Social / Civics / Society
        t.contains("social") || t.contains("civic") || t.contains("society") ||
        t.contains("community") || t.contains("welfare") || t.contains("scheme") -> TestTheme(
            icon = Icons.Rounded.Groups, primaryColor = Color(0xFF0891B2) // Cyan Dark
        )
        // General Knowledge / GK
        t.contains("general knowledge") || t.contains("gk ") || t.contains("general studies") ||
        t.contains("general awareness") -> TestTheme(
            icon = Icons.Rounded.School, primaryColor = Color(0xFF4F46E5) // Indigo
        )
        // Previous Year / Mock / Model / Practice
        t.contains("previous year") || t.contains("previous") || t.contains("mock") ||
        t.contains("model") || t.contains("practice") || t.contains("sample") ||
        t.contains("test series") -> TestTheme(
            icon = Icons.Rounded.Assignment, primaryColor = Color(0xFF9333EA) // Purple
        )
        // Science (general fallback)
        t.contains("science") -> TestTheme(
            icon = Icons.Rounded.Science, primaryColor = Color(0xFF8B5CF6) // Purple
        )
        // Life (fallback)
        t.contains("life") -> TestTheme(
            icon = Icons.Rounded.Biotech, primaryColor = Color(0xFF10B981) // Emerald
        )
        // Default
        else -> TestTheme(
            icon = Icons.Rounded.Quiz, primaryColor = AppColors.Primary
        )
    }
}

@Composable
private fun TestCard(
    modifier: Modifier = Modifier,
    session: QuestionSessionDto,
    completedTest: com.sudar.tnpscapp.nativeapp.core.network.model.TestHistoryItemDto?,
    onClick: () -> Unit
) {
    val title = session.name ?: "Unnamed Test"
    val totalQuestions = session.actualQuestionCount ?: session.totalQuestions ?: 0
    val duration = session.duration ?: 60

    val isCompleted = completedTest != null
    val score = completedTest?.percentage?.toDouble() ?: 0.0
    val isPassed = score >= 50
    
    // Get theme based on title
    val theme = getTestTheme(title)
    
    val statusColor = when {
        isCompleted && isPassed -> AppColors.Success
        isCompleted -> AppColors.Warning
        else -> theme.primaryColor
    }

    // Card with colored left accent border
    Card(
        modifier = modifier.height(190.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            theme.primaryColor.copy(alpha = 0.25f),
                            Color(0xFF1A1A1A)
                        ),
                        startX = 0f,
                        endX = 200f
                    )
                )
                .clickable(onClick = onClick)
        ) {
            // Left colored accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(theme.primaryColor)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top row: Icon and Start badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Icon box
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.primaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = theme.icon,
                            contentDescription = null,
                            tint = theme.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Start/Done badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isCompleted && isPassed -> Icons.Filled.CheckCircle
                                    isCompleted -> Icons.Filled.Refresh
                                    else -> Icons.Filled.PlayArrow
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    isCompleted && isPassed -> "Done"
                                    isCompleted -> "Retry"
                                    else -> "Start"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Score for completed tests
                if (isCompleted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Score: ",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                        Text(
                            text = "${score.toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPassed) AppColors.Success else AppColors.Warning
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Test name
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stats row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Questions count
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2A2A2A)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Quiz,
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = totalQuestions.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE5E5E5)
                            )
                        }
                    }

                    // Duration
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2A2A2A)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${duration}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE5E5E5)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(name: String): ImageVector {
    return when {
        name.contains("Tamil", ignoreCase = true) -> Icons.Rounded.Translate
        name.contains("Science", ignoreCase = true) -> Icons.Rounded.Science
        name.contains("Social", ignoreCase = true) || name.contains("History", ignoreCase = true) -> Icons.Rounded.Public
        name.contains("Aptitude", ignoreCase = true) || name.contains("Mental", ignoreCase = true) -> Icons.Rounded.Psychology
        name.contains("Current", ignoreCase = true) -> Icons.Rounded.Newspaper
        name.contains("Previous", ignoreCase = true) || name.contains("Year", ignoreCase = true) -> Icons.Rounded.HistoryEdu
        name.contains("Economy", ignoreCase = true) || name.contains("Economic", ignoreCase = true) -> Icons.Rounded.AccountBalance
        name.contains("Polity", ignoreCase = true) || name.contains("Constitution", ignoreCase = true) -> Icons.Rounded.Gavel
        name.contains("Geography", ignoreCase = true) -> Icons.Rounded.Terrain
        else -> Icons.AutoMirrored.Filled.MenuBook
    }
}

@Composable
private fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)
    androidx.activity.compose.BackHandler(enabled = enabled) {
        currentOnBack()
    }
}
