import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import 'test_page.dart';
import 'tests_page.dart';
import 'progress_page.dart';
import 'profile_page.dart';
import 'performance_page.dart';

class HomePage extends StatefulWidget {
  final String? selectedExam;
  
  const HomePage({Key? key, this.selectedExam}) : super(key: key);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _selectedIndex = 0;
  late String _currentExam;
  bool _isLoadingStats = true;
  bool _isLoadingCategories = true;
  int _testsTaken = 0;
  double _avgScore = 0.0;
  int _userRank = 0;
  List<Map<String, dynamic>> _recentTests = [];
  List<Map<String, dynamic>> _testCategories = [];
  
  final List<String> _examOptions = [
    'TNPSC Group 1',
    'TNPSC Group 2',
    'TNPSC Group 2A',
    'TNPSC Group 4',
    'TNPSC VAO',
    'TNPSC Police',
  ];

  @override
  void initState() {
    super.initState();
    _currentExam = widget.selectedExam ?? 'TNPSC Group 1';
    _loadUserStats();
  }

  Future<void> _loadUserStats() async {
    setState(() {
      _isLoadingStats = true;
      _isLoadingCategories = true;
    });
    
    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      
      // Load test categories with session counts
      final categoriesResponse = await ApiService.getTestCategories();
      if (categoriesResponse['success'] == true) {
        final categories = List<Map<String, dynamic>>.from(categoriesResponse['categories'] ?? []);
        _testCategories = categories;
      }
      
      if (userId != null) {
        // Load test history
        final historyResponse = await ApiService.getTestHistory(userId: userId);
        
        if (historyResponse['success'] == true) {
          final history = List<Map<String, dynamic>>.from(historyResponse['history'] ?? []);
          
          // Calculate stats
          _testsTaken = history.length;
          if (history.isNotEmpty) {
            double totalScore = 0;
            for (var test in history) {
              totalScore += (test['percentage'] ?? 0).toDouble();
            }
            _avgScore = totalScore / history.length;
          }
          _recentTests = history.take(3).toList();
        }
        
        // Load rankings to get user rank
        final rankingsResponse = await ApiService.getRankings();
        if (rankingsResponse['success'] == true) {
          final rankings = List<Map<String, dynamic>>.from(rankingsResponse['rankings'] ?? []);
          final userRanking = rankings.indexWhere((r) => r['user_id'] == userId);
          if (userRanking != -1) {
            _userRank = userRanking + 1;
          }
        }
      }
      
      if (mounted) {
        setState(() {
          _isLoadingStats = false;
          _isLoadingCategories = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoadingStats = false;
          _isLoadingCategories = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final isMobile = size.width < 600;
    
    // Page list for navigation
    final List<Widget> _pages = [
      _buildHomePage(),
      const TestsPage(),
      const ProgressPage(),
      const ProfilePage(),
    ];
    
    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      body: Center(
        child: Container(
          constraints: const BoxConstraints(maxWidth: 450),
          child: _pages[_selectedIndex],
        ),
      ),
      bottomNavigationBar: _buildBottomNavigationBar(),
    );
  }
  
  Widget _buildHomePage() {
    return CustomScrollView(
      slivers: [
        // App Bar
        _buildAppBar(),
        
        // Body Content
        SliverPadding(
          padding: const EdgeInsets.all(20),
          sliver: SliverList(
            delegate: SliverChildListDelegate([
              // Stats Cards
              _buildStatsSection(),
              
              const SizedBox(height: 24),
              
              // Quick Actions
              _buildQuickActionsSection(),
              
              const SizedBox(height: 24),
              
              // Test Categories
              _buildTestCategoriesSection(),
              
              const SizedBox(height: 24),
              
              // Recent Tests
              _buildRecentTestsSection(),
              
              const SizedBox(height: 80),
            ]),
          ),
        ),
      ],
    );
  }

  Widget _buildAppBar() {
    return SliverAppBar(
      expandedHeight: 60,
      floating: false,
      pinned: true,
      backgroundColor: ThemeHelper.cardColor(context),
      elevation: 1,
      shadowColor: Colors.black.withOpacity(0.1),
      title: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            'Mock Test',
            style: GoogleFonts.poppins(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: AppColors.primary,
            ),
          ),
          const SizedBox(width: 20),
          InkWell(
            onTap: _showExamSelector,
            borderRadius: BorderRadius.circular(8),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    _currentExam,
                    style: GoogleFonts.poppins(
                      fontSize: 15,
                      fontWeight: FontWeight.w500,
                      color: ThemeHelper.textPrimary(context),
                    ),
                  ),
                  const SizedBox(width: 4),
                  Icon(
                    Icons.keyboard_arrow_down,
                    color: ThemeHelper.textPrimary(context),
                    size: 20,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
      centerTitle: false,
      actions: [
        IconButton(
          icon: Icon(
            Icons.notifications_outlined,
            color: ThemeHelper.textPrimary(context),
            size: 26,
          ),
          onPressed: () {},
        ),
        const SizedBox(width: 8),
      ],
    );
  }

  void _showExamSelector() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return DraggableScrollableSheet(
          initialChildSize: 0.6,
          minChildSize: 0.4,
          maxChildSize: 0.9,
          expand: false,
          builder: (context, scrollController) {
            return Container(
              padding: const EdgeInsets.all(20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Select Your Exam',
                        style: GoogleFonts.poppins(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: ThemeHelper.textPrimary(context),
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.close),
                        onPressed: () => Navigator.pop(context),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Expanded(
                    child: ListView.builder(
                      controller: scrollController,
                      itemCount: _examOptions.length,
                      itemBuilder: (context, index) {
                        final exam = _examOptions[index];
                        final isSelected = exam == _currentExam;
                        return InkWell(
                          onTap: () {
                            setState(() {
                              _currentExam = exam;
                            });
                            Navigator.pop(context);
                          },
                          child: Container(
                            padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 16),
                            margin: const EdgeInsets.only(bottom: 8),
                            decoration: BoxDecoration(
                              color: isSelected ? AppColors.primary.withOpacity(0.1) : Colors.transparent,
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(
                                color: isSelected ? AppColors.primary : AppColors.border,
                                width: isSelected ? 2 : 1,
                              ),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  exam,
                                  style: GoogleFonts.poppins(
                                    fontSize: 15,
                                    fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
                                    color: isSelected ? AppColors.primary : AppColors.textPrimary,
                                  ),
                                ),
                                if (isSelected)
                                  const Icon(
                                    Icons.check_circle,
                                    color: AppColors.primary,
                                    size: 22,
                                  ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildStatsSection() {
    return Row(
      children: [
        Expanded(
          child: _buildStatCard(
            icon: Icons.quiz_rounded,
            label: 'Tests Taken',
            value: _isLoadingStats ? '...' : '$_testsTaken',
            color: AppColors.primary,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildStatCard(
            icon: Icons.trending_up_rounded,
            label: 'Avg Score',
            value: _isLoadingStats ? '...' : '${_avgScore.toStringAsFixed(0)}%',
            color: AppColors.success,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildStatCard(
            icon: Icons.emoji_events_rounded,
            label: 'Rank',
            value: _isLoadingStats ? '...' : (_userRank > 0 ? '#$_userRank' : '--'),
            color: AppColors.warning,
          ),
        ),
      ],
    );
  }

  Widget _buildStatCard({
    required IconData icon,
    required String label,
    required String value,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(icon, color: color, size: 24),
          ),
          const SizedBox(height: 8),
          Text(
            value,
            style: GoogleFonts.poppins(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: ThemeHelper.textPrimary(context),
            ),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: GoogleFonts.poppins(
              fontSize: 11,
              color: AppColors.textLight,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildQuickActionsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Quick Actions',
          style: GoogleFonts.poppins(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: ThemeHelper.textPrimary(context),
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _buildQuickActionButton(
                icon: Icons.play_circle_filled_rounded,
                label: 'Start Test',
                color: AppColors.primary,
                onTap: () {
                  // Navigate to Tests page to select a test
                  setState(() => _selectedIndex = 1);
                },
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildQuickActionButton(
                icon: Icons.analytics_rounded,
                label: 'Performance',
                color: AppColors.secondary,
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const PerformancePage(),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildQuickActionButton({
    required IconData icon,
    required String label,
    required Color color,
    VoidCallback? onTap,
  }) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [color, color.withOpacity(0.8)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: color.withOpacity(0.3),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 20),
            child: Column(
              children: [
                Icon(icon, color: Colors.white, size: 32),
                const SizedBox(height: 8),
                Text(
                  label,
                  style: GoogleFonts.poppins(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildTestCategoriesSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Test Categories',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: ThemeHelper.textPrimary(context),
              ),
            ),
            TextButton(
              onPressed: () {
                setState(() => _selectedIndex = 1); // Go to Tests page
              },
              child: Text(
                'View All',
                style: GoogleFonts.poppins(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: AppColors.primary,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        if (_isLoadingCategories)
          const Center(
            child: Padding(
              padding: EdgeInsets.all(32.0),
              child: CircularProgressIndicator(color: AppColors.primary),
            ),
          )
        else if (_testCategories.isEmpty)
          Center(
            child: Padding(
              padding: const EdgeInsets.all(32.0),
              child: Text(
                'No test categories available',
                style: GoogleFonts.poppins(
                  fontSize: 14,
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          )
        else
          ..._testCategories.take(5).map((category) {
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _buildCategoryCardFromData(category),
            );
          }).toList(),
      ],
    );
  }

  Widget _buildCategoryCardFromData(Map<String, dynamic> category) {
    final name = category['name'] ?? 'Unknown';
    final sessionsCount = category['sessions_count'] ?? 0;
    final completedCount = category['completed_count'] ?? 0;
    final progress = sessionsCount > 0 ? completedCount / sessionsCount : 0.0;
    
    // Assign icon and color based on category name
    IconData icon;
    Color color;
    
    if (name.contains('Tamil')) {
      icon = Icons.translate_rounded;
      color = const Color(0xFF9C27B0);
    } else if (name.contains('Science')) {
      icon = Icons.science_rounded;
      color = const Color(0xFF00BCD4);
    } else if (name.contains('Social') || name.contains('History')) {
      icon = Icons.public_rounded;
      color = const Color(0xFF4CAF50);
    } else if (name.contains('Aptitude') || name.contains('Mental')) {
      icon = Icons.psychology_rounded;
      color = AppColors.secondary;
    } else if (name.contains('Current')) {
      icon = Icons.newspaper_rounded;
      color = const Color(0xFFFF5722);
    } else if (name.contains('Previous') || name.contains('Year')) {
      icon = Icons.history_edu_rounded;
      color = AppColors.primary;
    } else {
      icon = Icons.book_outlined;
      color = AppColors.primary;
    }
    
    return _buildCategoryCard(
      title: name,
      subtitle: '$sessionsCount Tests Available',
      icon: icon,
      color: color,
      progress: progress,
      completed: completedCount,
      total: sessionsCount,
    );
  }

  Widget _buildCategoryCard({
    required String title,
    required String subtitle,
    required IconData icon,
    required Color color,
    required double progress,
    required int completed,
    required int total,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            // Navigate to Tests page to see all tests in this category
            setState(() => _selectedIndex = 1);
          },
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    color: color.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(icon, color: color, size: 28),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: GoogleFonts.poppins(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: ThemeHelper.textPrimary(context),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        subtitle,
                        style: GoogleFonts.poppins(
                          fontSize: 12,
                          color: AppColors.textLight,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Expanded(
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(4),
                              child: LinearProgressIndicator(
                                value: progress,
                                backgroundColor: color.withOpacity(0.1),
                                valueColor: AlwaysStoppedAnimation<Color>(color),
                                minHeight: 6,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            '$completed/$total',
                            style: GoogleFonts.poppins(
                              fontSize: 11,
                              fontWeight: FontWeight.w600,
                              color: AppColors.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                const Icon(
                  Icons.arrow_forward_ios_rounded,
                  size: 16,
                  color: AppColors.textLight,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildRecentTestsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Recent Tests',
          style: GoogleFonts.poppins(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: ThemeHelper.textPrimary(context),
          ),
        ),
        const SizedBox(height: 12),
        if (_isLoadingStats)
          const Center(
            child: Padding(
              padding: EdgeInsets.all(32.0),
              child: CircularProgressIndicator(color: AppColors.primary),
            ),
          )
        else if (_recentTests.isEmpty)
          Center(
            child: Padding(
              padding: const EdgeInsets.all(32.0),
              child: Column(
                children: [
                  Icon(Icons.assignment_outlined, size: 48, color: AppColors.textLight),
                  const SizedBox(height: 8),
                  Text(
                    'No tests taken yet',
                    style: GoogleFonts.poppins(
                      fontSize: 14,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextButton(
                    onPressed: () {
                      setState(() => _selectedIndex = 1); // Go to Tests page
                    },
                    child: Text(
                      'Start Your First Test',
                      style: GoogleFonts.poppins(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          )
        else
          ..._recentTests.map((test) {
            final percentage = (test['percentage'] ?? 0).toDouble();
            final isPassed = percentage >= 50;
            
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _buildRecentTestCard(
                title: test['test_name'] ?? 'Test',
                date: 'Completed on ${_formatDate(test['submitted_at'])}',
                score: test['correct'] ?? 0,
                totalQuestions: test['total_questions'] ?? 0,
                status: isPassed ? 'Passed' : 'Failed',
                statusColor: isPassed ? AppColors.success : AppColors.error,
              ),
            );
          }).toList(),
      ],
    );
  }

  String _formatDate(String? dateStr) {
    if (dateStr == null) return 'Unknown';
    try {
      final date = DateTime.parse(dateStr);
      final months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 
                      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      return '${months[date.month - 1]} ${date.day}, ${date.year}';
    } catch (e) {
      return dateStr;
    }
  }

  Widget _buildRecentTestCard({
    required String title,
    required String date,
    required int score,
    required int totalQuestions,
    required String status,
    required Color statusColor,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  title,
                  style: GoogleFonts.poppins(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: ThemeHelper.textPrimary(context),
                  ),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: statusColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  status,
                  style: GoogleFonts.poppins(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: statusColor,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Icon(
                Icons.calendar_today_rounded,
                size: 14,
                color: AppColors.textLight,
              ),
              const SizedBox(width: 6),
              Text(
                date,
                style: GoogleFonts.poppins(
                  fontSize: 12,
                  color: AppColors.textLight,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.check_circle_outline_rounded,
                    size: 16,
                    color: AppColors.textSecondary,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    'Score: $score/$totalQuestions',
                    style: GoogleFonts.poppins(
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
              Text(
                '${(score * 100 / totalQuestions).toStringAsFixed(0)}%',
                style: GoogleFonts.poppins(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: statusColor,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildBottomNavigationBar() {
    return Container(
      constraints: const BoxConstraints(maxWidth: 450),
      margin: EdgeInsets.zero,
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 20,
            offset: const Offset(0, -4),
          ),
        ],
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildNavItem(0, Icons.home_rounded, 'Home'),
              _buildNavItem(1, Icons.library_books_rounded, 'Tests'),
              _buildNavItem(2, Icons.bar_chart_rounded, 'Progress'),
              _buildNavItem(3, Icons.person_rounded, 'Profile'),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildNavItem(int index, IconData icon, String label) {
    final isSelected = _selectedIndex == index;
    
    return InkWell(
      onTap: () {
        setState(() {
          _selectedIndex = index;
        });
      },
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected 
              ? AppColors.primary.withOpacity(0.1) 
              : Colors.transparent,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              color: isSelected ? AppColors.primary : AppColors.textLight,
              size: 24,
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: GoogleFonts.poppins(
                fontSize: 11,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
                color: isSelected ? AppColors.primary : AppColors.textLight,
              ),
            ),
          ],
        ),
      ),
    );
  }
}


