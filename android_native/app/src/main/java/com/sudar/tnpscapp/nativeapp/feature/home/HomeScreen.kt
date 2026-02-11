package com.sudar.tnpscapp.nativeapp.feature.home

import androidx.compose.animation.core.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.sudar.tnpscapp.nativeapp.core.network.model.ExamCategoryDto
import com.sudar.tnpscapp.nativeapp.core.network.model.TestCategoryDto
import com.sudar.tnpscapp.nativeapp.core.network.UrlUtils
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

/**
 * Home Screen - Exact Flutter replica
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onChangeExam: () -> Unit,
    onTests: () -> Unit,
    onOpenTestCategory: (categoryId: Int, categoryName: String) -> Unit,
    onHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExamSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // App Bar
        HomeAppBar(
            currentExam = uiState.currentExam,
            isPremium = uiState.isPremium,
            onExamClick = { showExamSelector = true }
        )

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Banner Slider at top
            BannerSlider()
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Content with padding
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                // Stats Section
                StatsSection(
                    testsTaken = uiState.testsTaken,
                    avgScore = uiState.avgScore,
                    userRank = uiState.userRank,
                    isLoading = uiState.isLoadingStats
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Horizontal Category Carousel with Scale Animation
                if (uiState.testCategories.isNotEmpty()) {
                    CategoryCarousel(
                        categories = uiState.testCategories,
                        onCategoryClick = { category -> onOpenTestCategory(category.id, category.name) }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }

                HomeProudCard(
                    metricValueText = "5000K",
                    metricLabelText = "Daily Practice",
                    subtitle = "We are proud to help thousands of students in securing their dream job"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom: Privacy Policy (expand/collapse like your reference screenshot)
                HomeExpandableInfoCard(
                    title = "Privacy Policy",
                    bodyText = "We are committed to protecting your personal information and your right to privacy. We may collect details like your mobile number (for login), app usage, and test progress to improve your learning experience. We follow strong privacy and security standards while doing this."
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Exam Selector Bottom Sheet
    if (showExamSelector) {
        ExamSelectorBottomSheet(
            examCategories = uiState.examCategories,
            selectedExamId = uiState.selectedExamId,
            isLoading = uiState.isLoadingExams,
            onExamSelected = { exam ->
                viewModel.selectExam(exam.id, exam.name)
                showExamSelector = false
            },
            onDismiss = { showExamSelector = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAppBar(
    currentExam: String,
    isPremium: Boolean,
    onExamClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.Background, // Match background color
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title
            Text(
                text = "SUDAR App",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )

            Spacer(modifier = Modifier.width(20.dp))

            // Exam Selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onExamClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentExam,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Premium Badge
            if (isPremium) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PRO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Banner Slider with auto-scroll animation
 */
@Composable
private fun BannerSlider() {
    // Banner images from assets
    val bannerImages = listOf(
        "file:///android_asset/banner1.webp",
        "file:///android_asset/banner2.webp"
    )
    
    val pagerState = rememberPagerState(pageCount = { bannerImages.size })
    
    // Auto-scroll effect
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % bannerImages.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
    }
    
    Column {
        // Pager - height adjusted for 3:2 image ratio
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                // Taller banner to better fit 1024x1024 creatives (less cropping)
                .height(260.dp)
                .padding(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { page ->
            // Calculate scale for current page
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val scale = 1f - (0.1f * abs(pageOffset)).coerceIn(0f, 0.1f)
            
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                AsyncImage(
                    model = bannerImages[page],
                    contentDescription = "Banner ${page + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Page indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(bannerImages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(
                            width = if (isSelected) 24.dp else 8.dp,
                            height = 8.dp
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSelected) AppColors.Primary
                            else AppColors.Primary.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun StatsSection(
    testsTaken: Int,
    avgScore: Double,
    userRank: Int,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Quiz,
            label = "Tests Taken",
            value = if (isLoading) "..." else testsTaken.toString(),
            color = AppColors.Primary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.TrendingUp,
            label = "Avg Score",
            value = if (isLoading) "..." else "${avgScore.toInt()}%",
            color = AppColors.Success
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.EmojiEvents,
            label = "Rank",
            value = if (isLoading) "..." else if (userRank > 0) "#$userRank" else "--",
            color = AppColors.Warning
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = AppColors.TextLight
            )
        }
    }
}

/**
 * Horizontal Category Carousel with Scale Animation
 * Features:
 * - LazyRow for horizontal scrolling
 * - Center item scaling effect
 * - Snap to center behavior
 */
@Composable
private fun CategoryCarousel(
    categories: List<TestCategoryDto>,
    onCategoryClick: (TestCategoryDto) -> Unit
) {
    val listState = rememberLazyListState()
    
    val colors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Violet
        Color(0xFF06B6D4), // Cyan
        Color(0xFFEC4899), // Pink
        Color(0xFF14B8A6), // Teal
    )

    Column {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Popular Categories",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        }

        // Horizontal Carousel with scale animation
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories.size) { index ->
                val category = categories[index]
                val color = colors[index % colors.size]
                
                // Calculate scale based on distance from center
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val itemInfo = visibleItems.find { it.index == index }
                
                val scale = if (itemInfo != null) {
                    val center = layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset / 2
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    val distance = abs(center - itemCenter).toFloat()
                    val maxDistance = layoutInfo.viewportEndOffset / 2f
                    
                    // Scale from 0.85 to 1.0 based on distance from center
                    val scaleFactor = 1f - (distance / maxDistance).coerceIn(0f, 1f) * 0.15f
                    scaleFactor.coerceIn(0.85f, 1f)
                } else {
                    0.85f
                }

                CarouselCategoryCard(
                    category = category,
                    color = color,
                    scale = scale,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@Composable
private fun CarouselCategoryCard(
    category: TestCategoryDto,
    color: Color,
    scale: Float,
    onClick: () -> Unit
) {
    val sessionsCount = category.sessionsCount ?: 0
    val icon = getCategoryIcon(category.name)
    
    // Check if has image
    val rawImagePath = category.imagePath ?: category.image
    val imageUrl = UrlUtils.resolveTestCategoryImageUrl(rawImagePath)
    val hasImage = imageUrl != null

    // Popular category image card size (bigger, more prominent)
    Card(
        modifier = Modifier
            .width(135.dp)
            .height(210.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = 0.75f + (scale - 0.85f) * 1.67f
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
        ) {
            if (hasImage) {
                // Full image cover - professional fit
                AsyncImage(
                    model = imageUrl,
                    contentDescription = category.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // No overlay - image only
            } else {
                // Fallback: Gradient with icon (no image)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(color, color.copy(alpha = 0.7f))
                            )
                        )
                )
                
                // Icon centered
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TestCategoriesSection(
    categories: List<TestCategoryDto>,
    isLoading: Boolean,
    onViewAll: () -> Unit,
    onCategoryClick: (TestCategoryDto) -> Unit
) {
    Column {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Test Categories",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            TextButton(onClick = onViewAll) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }
            categories.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = AppColors.TextLight,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No test categories available",
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
            else -> {
                CategoryGrid(
                    categories = categories, // Show ALL categories
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<TestCategoryDto>,
    onCategoryClick: (TestCategoryDto) -> Unit
) {
    val colors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Violet
        Color(0xFF06B6D4), // Cyan
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.chunked(2).forEachIndexed { rowIndex, rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCategories.forEachIndexed { index, category ->
                    val colorIndex = rowIndex * 2 + index
                    CategoryCard(
                        modifier = Modifier.weight(1f),
                        category = category,
                        color = colors[colorIndex % colors.size],
                        onClick = { onCategoryClick(category) }
                    )
                }
                // Fill empty space if odd number of items
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    modifier: Modifier = Modifier,
    category: TestCategoryDto,
    color: Color,
    onClick: () -> Unit
) {
    val sessionsCount = category.sessionsCount ?: 0
    val completedCount = category.completedCount ?: 0
    val progress = if (sessionsCount > 0) (completedCount.toFloat() / sessionsCount).coerceIn(0f, 1f) else 0f

    val icon = getCategoryIcon(category.name)

    // Match Flutter design: Card with subtle border
    Surface(
        modifier = modifier
            .height(140.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.15f))
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
                        .background(color.copy(alpha = 0.1f)),
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
                        .background(color.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = sessionsCount.toString(),
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

            // Progress bar - match Flutter style
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
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
private fun HomeProudCard(
    metricValueText: String,
    metricLabelText: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val borderBrush = Brush.linearGradient(listOf(AppColors.Primary, AppColors.Secondary))
    val innerBg = Color(0xFF1A1A1A)

    // Modern "highlight" card: gradient border + clean content
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(borderBrush, RoundedCornerShape(18.dp))
            .padding(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(innerBg)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Small header chip
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = AppColors.SurfaceLight
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        tint = AppColors.PrimaryLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SUDAR COMMUNITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = metricValueText,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.PrimaryLight,
            )
            Text(
                text = metricLabelText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            HomeAvatarRow(
                count = 6,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun HomeAvatarRow(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val size = 32.dp
    val overlap = 10.dp
    val step = size - overlap
    val initials = listOf("S", "U", "D", "A", "R", "T", "N", "P", "C")
    val gradients = listOf(
        listOf(AppColors.Primary, AppColors.PrimaryDark),
        listOf(AppColors.Secondary, AppColors.SecondaryDark),
        listOf(Color(0xFF10B981), Color(0xFF047857)), // emerald
        listOf(Color(0xFFF59E0B), Color(0xFFB45309)), // amber
        listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)), // violet
        listOf(Color(0xFF06B6D4), Color(0xFF0E7490)), // cyan
    )

    Box(
        modifier = modifier
            .height(size)
            .width(size + step * (count - 1))
    ) {
        repeat(count) { index ->
            val initial = initials.getOrElse(index) { "S" }
            val gradient = gradients[index % gradients.size]

            Box(
                modifier = Modifier
                    .size(size)
                    .offset(x = step * index)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF0F0F0F).copy(alpha = 0.25f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .padding(0.dp)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Border ring on top (to separate overlaps)
            Box(
                modifier = Modifier
                    .size(size)
                    .offset(x = step * index)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .padding(0.dp)
            ) {
                // Using border via background stroke is not available on Box, so we overlay a Surface ring
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.Background),
                    shadowElevation = 0.dp,
                    content = {}
                )
            }
        }
    }
}

@Composable
private fun HomeExpandableInfoCard(
    title: String,
    bodyText: String,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accent bar (matches reference style)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppColors.Secondary)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppColors.TextSecondary
                )
            }

            if (expanded) {
                Text(
                    text = bodyText,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamSelectorBottomSheet(
    examCategories: List<ExamCategoryDto>,
    selectedExamId: Int?,
    isLoading: Boolean,
    onExamSelected: (ExamCategoryDto) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Your Exam",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                }
                examCategories.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.InboxCustom,
                                contentDescription = null,
                                tint = AppColors.TextLight,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No exams available",
                                fontSize = 14.sp,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(examCategories) { exam ->
                            val isSelected = exam.id == selectedExamId

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onExamSelected(exam) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    brush = if (isSelected) {
                                        Brush.linearGradient(listOf(AppColors.Primary, AppColors.Primary))
                                    } else {
                                        Brush.linearGradient(listOf(AppColors.Border, AppColors.Border))
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exam.name,
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) AppColors.Primary else AppColors.TextPrimary
                                        )
                                        exam.description?.let { desc ->
                                            if (desc.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = desc,
                                                    fontSize = 12.sp,
                                                    color = AppColors.TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AppColors.Primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Extension for custom inbox icon if not available
private val Icons.Rounded.InboxCustom: ImageVector
    get() = Icons.Rounded.Inbox
