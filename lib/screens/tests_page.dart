import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import 'test_page.dart';
import 'subscription_offer_page.dart';

class TestsPage extends StatefulWidget {
  const TestsPage({Key? key}) : super(key: key);

  @override
  State<TestsPage> createState() => _TestsPageState();
}

class _TestsPageState extends State<TestsPage> {
  bool _isLoading = true;
  List<Map<String, dynamic>> _allSessions = [];
  List<Map<String, dynamic>> _categories = [];
  Map<int, Map<String, dynamic>> _completedTests = {};
  int? _userId;
  
  // Selected category (null = show categories list)
  Map<String, dynamic>? _selectedCategory;
  
  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    
    try {
      final prefs = await SharedPreferences.getInstance();
      _userId = prefs.getInt('userId');
      
      final categoriesResponse = await ApiService.getTestCategories();
      final sessionsResponse = await ApiService.getQuestionSessions();
      
      if (_userId != null) {
        final historyResponse = await ApiService.getTestHistory(userId: _userId!);
        
        if (historyResponse['success'] == true) {
          final history = List<Map<String, dynamic>>.from(historyResponse['history'] ?? []);
          
          _completedTests.clear();
          for (var test in history) {
            final sessionIdRaw = test['session_id'];
            final sessionId = sessionIdRaw is int ? sessionIdRaw : int.tryParse(sessionIdRaw?.toString() ?? '');
            
            if (sessionId != null) {
              final existingScore = _completedTests[sessionId]?['percentage'] ?? 0;
              final newScore = (test['percentage'] ?? 0) is num 
                  ? (test['percentage'] as num).toDouble() 
                  : double.tryParse(test['percentage']?.toString() ?? '0') ?? 0;
              
              if (!_completedTests.containsKey(sessionId) || newScore > existingScore) {
                _completedTests[sessionId] = test;
              }
            }
          }
        }
      }
      
      if (mounted) {
        setState(() {
          if (categoriesResponse['success'] == true) {
            _categories = List<Map<String, dynamic>>.from(categoriesResponse['categories'] ?? []);
          } else {
            _categories = [];
          }
          
          if (sessionsResponse['success'] == true) {
            _allSessions = List<Map<String, dynamic>>.from(sessionsResponse['sessions'] ?? []);
          }
          
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _categories = [];
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: _selectedCategory == null,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop && _selectedCategory != null) {
          setState(() {
            _selectedCategory = null;
          });
        }
      },
      child: Scaffold(
        backgroundColor: ThemeHelper.backgroundColor(context),
        appBar: AppBar(
          backgroundColor: ThemeHelper.cardColor(context),
          elevation: 1,
          leading: _selectedCategory != null
              ? IconButton(
                  icon: Icon(Icons.arrow_back_ios_rounded, color: ThemeHelper.textPrimary(context)),
                  onPressed: () {
                    setState(() {
                      _selectedCategory = null;
                    });
                  },
                )
              : null,
          title: Text(
            _selectedCategory != null 
                ? _selectedCategory!['name'] ?? 'Tests'
                : 'Test Categories',
            style: GoogleFonts.poppins(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: ThemeHelper.textPrimary(context),
            ),
          ),
          actions: [
            IconButton(
              icon: Icon(Icons.refresh, color: ThemeHelper.textPrimary(context)),
              onPressed: _loadData,
              tooltip: 'Refresh',
            ),
          ],
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
            : _selectedCategory != null
                ? _buildTestsGrid()
                : _buildCategoriesGrid(),
      ),
    );
  }

  // ==================== CATEGORIES GRID ====================
  
  Widget _buildCategoriesGrid() {
    if (_categories.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.folder_open_rounded, size: 64, color: AppColors.textLight),
            const SizedBox(height: 16),
            Text(
              'No categories available',
              style: GoogleFonts.poppins(fontSize: 16, color: AppColors.textSecondary),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadData,
      color: AppColors.primary,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        child: _buildCategoryGridLayout(),
      ),
    );
  }

  Widget _buildCategoryGridLayout() {
    final List<Widget> rows = [];
    
    // 3 cards per row
    for (int i = 0; i < _categories.length; i += 3) {
      final List<Widget> rowItems = [];
      
      // First item
      rowItems.add(Expanded(child: _buildCategoryCard(_categories[i], i)));
      
      // Second item
      if (i + 1 < _categories.length) {
        rowItems.add(const SizedBox(width: 8));
        rowItems.add(Expanded(child: _buildCategoryCard(_categories[i + 1], i + 1)));
      } else {
        rowItems.add(const SizedBox(width: 8));
        rowItems.add(const Expanded(child: SizedBox()));
      }
      
      // Third item
      if (i + 2 < _categories.length) {
        rowItems.add(const SizedBox(width: 8));
        rowItems.add(Expanded(child: _buildCategoryCard(_categories[i + 2], i + 2)));
      } else {
        rowItems.add(const SizedBox(width: 8));
        rowItems.add(const Expanded(child: SizedBox()));
      }
      
      rows.add(
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: rowItems,
          ),
        ),
      );
    }
    
    return Column(children: rows);
  }

  Widget _buildCategoryCard(Map<String, dynamic> category, int index) {
    final name = category['name'] ?? 'Unknown';
    final categoryId = category['id'];
    
    // Count tests in this category
    final testsInCategory = _allSessions.where((s) => s['test_category_id'] == categoryId).length;
    
    // Count completed tests in this category
    int completedInCategory = 0;
    for (var session in _allSessions) {
      if (session['test_category_id'] == categoryId) {
        final sessionId = session['id'] is int ? session['id'] : int.tryParse(session['id']?.toString() ?? '');
        if (sessionId != null && _completedTests.containsKey(sessionId)) {
          completedInCategory++;
        }
      }
    }
    
    final progress = testsInCategory > 0 ? (completedInCategory / testsInCategory).clamp(0.0, 1.0) : 0.0;
    
    // Check for category image
    final imagePath = category['image_path'] ?? category['image'];
    final imageUrl = ApiService.resolveTestCategoryImageUrl(imagePath?.toString());
    
    // If image exists, render full-cover image card
    if (imageUrl != null) {
      return _buildImageCategoryCard(
        imageUrl: imageUrl,
        name: name,
        onTap: () {
          setState(() {
            _selectedCategory = category;
          });
        },
      );
    }
    
    // Fallback: icon-based card
    // Clean accent colors (same as home page)
    final List<Color> colors = [
      const Color(0xFF6366F1), // Indigo
      const Color(0xFF10B981), // Emerald
      const Color(0xFFF59E0B), // Amber
      const Color(0xFFEF4444), // Red
      const Color(0xFF8B5CF6), // Violet
      const Color(0xFF06B6D4), // Cyan
    ];
    
    // Assign icon based on category name
    IconData icon = Icons.menu_book_rounded;
    if (name.contains('Tamil')) {
      icon = Icons.translate_rounded;
    } else if (name.contains('Science')) {
      icon = Icons.science_rounded;
    } else if (name.contains('Social') || name.contains('History')) {
      icon = Icons.public_rounded;
    } else if (name.contains('Aptitude') || name.contains('Mental')) {
      icon = Icons.psychology_rounded;
    } else if (name.contains('Current')) {
      icon = Icons.newspaper_rounded;
    } else if (name.contains('Previous') || name.contains('Year')) {
      icon = Icons.history_edu_rounded;
    } else if (name.contains('Economy')) {
      icon = Icons.account_balance_rounded;
    } else if (name.contains('Polity')) {
      icon = Icons.gavel_rounded;
    } else if (name.contains('Geography')) {
      icon = Icons.terrain_rounded;
    }
    
    final color = colors[index % colors.length];

    return Container(
      height: 180, // Height for 3-column grid
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: color.withOpacity(0.15),
          width: 1,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            setState(() {
              _selectedCategory = category;
            });
          },
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Top row: Icon and count
                Row(
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        color: color.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Icon(icon, color: color, size: 20),
                    ),
                    const Spacer(),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: color.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        '$testsInCategory',
                        style: GoogleFonts.poppins(
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          color: color,
                        ),
                      ),
                    ),
                  ],
                ),
                const Spacer(),
                // Category name
                Text(
                  name,
                  style: GoogleFonts.poppins(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: ThemeHelper.textPrimary(context),
                    height: 1.2,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 8),
                // Progress bar
                ClipRRect(
                  borderRadius: BorderRadius.circular(3),
                  child: LinearProgressIndicator(
                    value: progress,
                    backgroundColor: color.withOpacity(0.1),
                    valueColor: AlwaysStoppedAnimation<Color>(color),
                    minHeight: 5,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Full-cover image card for categories with uploaded images
  Widget _buildImageCategoryCard({
    required String imageUrl,
    required String name,
    required VoidCallback onTap,
  }) {
    return Container(
      height: 180, // Height for 3-column grid
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.15),
            blurRadius: 8,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: Stack(
          fit: StackFit.expand,
          children: [
            // Full-cover cached image (loads once, cached locally)
            CachedNetworkImage(
              imageUrl: imageUrl,
              fit: BoxFit.cover,
              filterQuality: FilterQuality.high,
              // Simple placeholder - no spinner to avoid "loading" look on cached images
              placeholder: (context, url) => Container(
                color: ThemeHelper.cardColor(context),
              ),
              errorWidget: (context, url, error) {
                // On error, show placeholder with category initial
                return Container(
                  color: AppColors.primary.withOpacity(0.1),
                  child: Center(
                    child: Text(
                      name.isNotEmpty ? name[0].toUpperCase() : '?',
                      style: GoogleFonts.poppins(
                        fontSize: 40,
                        fontWeight: FontWeight.bold,
                        color: AppColors.primary.withOpacity(0.3),
                      ),
                    ),
                  ),
                );
              },
            ),
            // Ripple effect overlay
            Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: onTap,
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // ==================== TESTS GRID ====================
  
  Widget _buildTestsGrid() {
    final filteredSessions = _allSessions
        .where((session) => session['test_category_id'] == _selectedCategory!['id'])
        .toList();

    if (filteredSessions.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.assignment_outlined, size: 64, color: AppColors.textLight),
            const SizedBox(height: 16),
            Text(
              'No tests available',
              style: GoogleFonts.poppins(fontSize: 16, color: AppColors.textSecondary),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadData,
      color: AppColors.primary,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        child: _buildTestGridLayout(filteredSessions),
      ),
    );
  }

  Widget _buildTestGridLayout(List<Map<String, dynamic>> sessions) {
    final List<Widget> rows = [];
    
    for (int i = 0; i < sessions.length; i += 2) {
      final List<Widget> rowItems = [];
      
      rowItems.add(Expanded(child: _buildTestCard(sessions[i])));
      
      if (i + 1 < sessions.length) {
        rowItems.add(const SizedBox(width: 12));
        rowItems.add(Expanded(child: _buildTestCard(sessions[i + 1])));
      } else {
        rowItems.add(const SizedBox(width: 12));
        rowItems.add(const Expanded(child: SizedBox()));
      }
      
      rows.add(
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: rowItems,
          ),
        ),
      );
    }
    
    return Column(children: rows);
  }

  Widget _buildTestCard(Map<String, dynamic> session) {
    final title = session['name'] ?? 'Unnamed Test';
    final totalQuestions = session['actual_question_count'] ?? session['total_questions'] ?? 0;
    final duration = session['duration'] ?? 60;
    
    final sessionIdRaw = session['id'];
    final sessionId = sessionIdRaw is int ? sessionIdRaw : int.tryParse(sessionIdRaw?.toString() ?? '') ?? 0;
    
    final completedTest = _completedTests[sessionId];
    final isCompleted = completedTest != null;
    
    double score = 0.0;
    if (isCompleted) {
      final percentageRaw = completedTest['percentage'];
      score = percentageRaw is num ? percentageRaw.toDouble() : double.tryParse(percentageRaw?.toString() ?? '0') ?? 0.0;
    }
    
    final isPassed = score >= 50;
    
    // Card color based on status
    final Color cardColor = isCompleted 
        ? (isPassed ? AppColors.success : AppColors.warning)
        : AppColors.primary;

    return Container(
      height: 180, // Height for 3-column grid
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: cardColor.withOpacity(0.15),
          width: 1,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () => _handleTestTap(session, isCompleted),
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Top row: Icon/Score and badge
                Row(
                  children: [
                    if (isCompleted)
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: cardColor.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Center(
                          child: Text(
                            '${score.toStringAsFixed(0)}%',
                            style: GoogleFonts.poppins(
                              fontSize: 12,
                              fontWeight: FontWeight.w700,
                              color: cardColor,
                            ),
                          ),
                        ),
                      )
                    else
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: cardColor.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Icon(
                          Icons.assignment_outlined,
                          color: cardColor,
                          size: 20,
                        ),
                      ),
                    const Spacer(),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: cardColor.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            isCompleted 
                                ? (isPassed ? Icons.check_circle : Icons.refresh)
                                : Icons.play_arrow_rounded,
                            size: 12,
                            color: cardColor,
                          ),
                          const SizedBox(width: 3),
                          Text(
                            isCompleted ? (isPassed ? 'Done' : 'Retry') : 'Start',
                            style: GoogleFonts.poppins(
                              fontSize: 10,
                              fontWeight: FontWeight.w600,
                              color: cardColor,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const Spacer(),
                // Test name
                Text(
                  title,
                  style: GoogleFonts.poppins(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: ThemeHelper.textPrimary(context),
                    height: 1.2,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 8),
                // Stats row with icons
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: ThemeHelper.backgroundColor(context),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.quiz_outlined, size: 11, color: AppColors.textSecondary),
                          const SizedBox(width: 3),
                          Text(
                            '$totalQuestions',
                            style: GoogleFonts.poppins(fontSize: 10, fontWeight: FontWeight.w500, color: AppColors.textSecondary),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: ThemeHelper.backgroundColor(context),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.timer_outlined, size: 11, color: AppColors.textSecondary),
                          const SizedBox(width: 3),
                          Text(
                            '${duration}m',
                            style: GoogleFonts.poppins(fontSize: 10, fontWeight: FontWeight.w500, color: AppColors.textSecondary),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  // ==================== TEST ACTIONS ====================

  void _handleTestTap(Map<String, dynamic> session, bool isCompleted) {
    final totalQuestions = session['actual_question_count'] ?? session['total_questions'] ?? 0;

    if (totalQuestions <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('This test has no questions yet', style: GoogleFonts.poppins()),
          backgroundColor: AppColors.error,
        ),
      );
      return;
    }

    if (isCompleted) {
      _showRetakeDialog(session);
    } else {
      _startTest(session);
    }
  }

  void _showRetakeDialog(Map<String, dynamic> session) {
    final sessionIdRaw = session['id'];
    final sessionId = sessionIdRaw is int ? sessionIdRaw : int.tryParse(sessionIdRaw?.toString() ?? '') ?? 0;
    
    final completedTest = _completedTests[sessionId];
    
    final percentageRaw = completedTest?['percentage'];
    final score = percentageRaw is num ? percentageRaw.toDouble() : double.tryParse(percentageRaw?.toString() ?? '0') ?? 0.0;
    
    final correctRaw = completedTest?['correct'];
    final correctAnswers = correctRaw is int ? correctRaw : int.tryParse(correctRaw?.toString() ?? '0') ?? 0;
    
    final totalRaw = completedTest?['total_questions'];
    final totalAnswered = totalRaw is int ? totalRaw : int.tryParse(totalRaw?.toString() ?? '0') ?? 0;
    
    final isPassed = score >= 50;
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: (isPassed ? AppColors.success : AppColors.warning).withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(
                isPassed ? Icons.emoji_events : Icons.refresh,
                color: isPassed ? AppColors.success : AppColors.warning,
                size: 24,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                'Test Completed',
                style: GoogleFonts.poppins(fontSize: 18, fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: isPassed 
                      ? [AppColors.success.withOpacity(0.1), AppColors.success.withOpacity(0.05)]
                      : [AppColors.warning.withOpacity(0.1), AppColors.warning.withOpacity(0.05)],
                ),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                children: [
                  Text(
                    '${score.toStringAsFixed(0)}%',
                    style: GoogleFonts.poppins(
                      fontSize: 40,
                      fontWeight: FontWeight.bold,
                      color: isPassed ? AppColors.success : AppColors.warning,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    isPassed ? '🎉 Excellent!' : '💪 Keep Practicing!',
                    style: GoogleFonts.poppins(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: ThemeHelper.textPrimary(context),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      _buildStatItem(Icons.check_circle, '$correctAnswers', 'Correct', AppColors.success),
                      const SizedBox(width: 24),
                      _buildStatItem(Icons.cancel, '${totalAnswered - correctAnswers}', 'Wrong', AppColors.error),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Would you like to retake this test?',
              style: GoogleFonts.poppins(fontSize: 14, color: AppColors.textSecondary),
              textAlign: TextAlign.center,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(
              'Cancel',
              style: GoogleFonts.poppins(color: AppColors.textSecondary, fontWeight: FontWeight.w600),
            ),
          ),
          ElevatedButton.icon(
            onPressed: () {
              Navigator.pop(context);
              _startTest(session);
            },
            icon: const Icon(Icons.replay, size: 18),
            label: Text('Retake', style: GoogleFonts.poppins(fontWeight: FontWeight.w600)),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatItem(IconData icon, String value, String label, Color color) {
    return Column(
      children: [
        Icon(icon, color: color, size: 20),
        const SizedBox(height: 4),
        Text(
          value,
          style: GoogleFonts.poppins(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: ThemeHelper.textPrimary(context),
          ),
        ),
        Text(
          label,
          style: GoogleFonts.poppins(fontSize: 10, color: AppColors.textSecondary),
        ),
      ],
    );
  }

  Future<void> _startTest(Map<String, dynamic> session) async {
    final prefs = await SharedPreferences.getInstance();
    bool isPremium = prefs.getBool('is_premium') ?? false;

    if (!isPremium) {
      final userId = prefs.getInt('user_id') ?? prefs.getInt('userId');
      if (userId != null) {
        try {
          final url = '${ApiService.baseUrl}/subscriptions/status.php?user_id=$userId';
          final res = await http.get(Uri.parse(url)).timeout(const Duration(seconds: 10));
          if (res.statusCode == 200) {
            final data = jsonDecode(res.body);
            isPremium = data['is_premium'] == true;
            await prefs.setBool('is_premium', isPremium);
          }
        } catch (_) {}
      }
    }

    if (!isPremium && mounted) {
      final result = await Navigator.push<bool>(
        context,
        MaterialPageRoute(builder: (_) => const SubscriptionOfferPage()),
      );
      if (result == true) {
        await prefs.setBool('is_premium', true);
        isPremium = true;
      } else {
        return;
      }
    }

    if (mounted && isPremium) {
      final rawSessionId = session['id'];
      final int? sessionId = rawSessionId is int ? rawSessionId : int.tryParse(rawSessionId.toString());
      if (sessionId == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Invalid session. Please try again.',
              style: GoogleFonts.poppins(),
            ),
            backgroundColor: AppColors.error,
          ),
        );
        return;
      }

      final rawDuration = session['duration'];
      int durationMinutes = 60;
      if (rawDuration is int) {
        durationMinutes = rawDuration;
      } else if (rawDuration is double) {
        durationMinutes = rawDuration.toInt();
      } else if (rawDuration is String) {
        durationMinutes = int.tryParse(rawDuration) ??
            int.tryParse(rawDuration.split('.').first) ??
            60;
      }
      if (durationMinutes <= 0) durationMinutes = 60;

      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => TestPage(
            testTitle: session['name'] ?? 'Test',
            category: session['category_name'] ?? '',
            sessionId: sessionId,
            duration: durationMinutes,
          ),
        ),
      ).then((_) => _loadData());
    }
  }
}
