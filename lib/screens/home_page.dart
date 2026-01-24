import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import '../services/update_service.dart';
import '../services/onesignal_service.dart';
import '../services/firebase_service.dart';
import 'tests_page.dart';
import 'progress_page.dart';
import 'profile_page.dart';
import 'performance_page.dart';
import 'annual_planner_page.dart';

class HomePage extends StatefulWidget {
  final String? selectedExam;
  
  const HomePage({Key? key, this.selectedExam}) : super(key: key);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  static const String _notifPromptedKey = 'notification_permission_prompted';
  static const String _notifDismissedUntilKey = 'notification_permission_dismissed_until';

  int _selectedIndex = 0;
  // Must have a safe default before async SharedPreferences load finishes.
  String _currentExam = 'Select Exam';
  
  // Double back press to exit
  DateTime? _lastBackPressTime;
  bool _isLoadingStats = true;
  bool _isLoadingCategories = true;
  bool _isLoadingExams = true;
  int _testsTaken = 0;
  double _avgScore = 0.0;
  int _userRank = 0;
  List<Map<String, dynamic>> _recentTests = [];
  List<Map<String, dynamic>> _testCategories = [];
  List<Map<String, dynamic>> _examCategories = [];
  int? _selectedExamId;
  bool _isPremium = false;

  @override
  void initState() {
    super.initState();
    print('🚀 HomePage initState called');
    // Ensure _currentExam is initialized synchronously to avoid LateInitializationError
    // during the first build.
    _currentExam = widget.selectedExam ?? _currentExam;
    _initializeData();
    _loadPremiumStatus();
    
    // Check for app updates after widget is built
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await _checkForAppUpdate();
      await _maybeShowNotificationPermissionDialog();
    });
  }

  Future<void> _loadPremiumStatus() async {
    final prefs = await SharedPreferences.getInstance();
    if (mounted) {
      setState(() {
        _isPremium = prefs.getBool('is_premium') ?? false;
      });
    }
  }
  
  /// Check for app updates from Play Store
  Future<void> _checkForAppUpdate() async {
    await UpdateService().checkForUpdate(context);
  }

  Future<void> _maybeShowNotificationPermissionDialog() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final prompted = prefs.getBool(_notifPromptedKey) ?? false;
      if (prompted) return;

      final dismissedUntil = prefs.getInt(_notifDismissedUntilKey);
      final nowMs = DateTime.now().millisecondsSinceEpoch;
      if (dismissedUntil != null && nowMs < dismissedUntil) return;

      final shouldShow = await OneSignalService.shouldShowPermissionDialog();
      if (!shouldShow) return;
      if (!mounted) return;

      final action = await showDialog<String>(
        context: context,
        barrierDismissible: true,
        builder: (ctx) {
          return AlertDialog(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            title: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: AppColors.primary.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.notifications_active_rounded,
                    color: AppColors.primary,
                    size: 24,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    'Enable Notifications?',
                    style: GoogleFonts.poppins(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: ThemeHelper.textPrimary(ctx),
                    ),
                  ),
                ),
              ],
            ),
            content: Text(
              'Allow notifications to get updates about new tests, results, and announcements.',
              style: GoogleFonts.poppins(
                fontSize: 14,
                color: ThemeHelper.textSecondary(ctx),
                height: 1.3,
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx, 'later'),
                child: Text(
                  'Not now',
                  style: GoogleFonts.poppins(
                    fontWeight: FontWeight.w600,
                    color: Colors.grey.shade700,
                  ),
                ),
              ),
              ElevatedButton(
                onPressed: () => Navigator.pop(ctx, 'allow'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: Colors.white,
                ),
                child: Text(
                  'Allow',
                  style: GoogleFonts.poppins(fontWeight: FontWeight.w700),
                ),
              ),
            ],
          );
        },
      );

      if (action == 'allow') {
        await prefs.setBool(_notifPromptedKey, true);
        await _requestNotificationPermission();
      } else {
        // Later (or dismissed)
        await prefs.setInt(
          _notifDismissedUntilKey,
          DateTime.now().add(const Duration(days: 7)).millisecondsSinceEpoch,
        );
      }
    } catch (e) {
      print('HomePage: Error showing notification permission dialog: $e');
    }
  }

  Future<void> _requestNotificationPermission() async {
    try {
      await OneSignalService.requestPermission();
      await FirebaseService.requestPermission();
    } catch (e) {
      print('HomePage: Error requesting notification permission: $e');
    }
  }
  
  @override
  void didUpdateWidget(HomePage oldWidget) {
    super.didUpdateWidget(oldWidget);
    print('🔄 HomePage didUpdateWidget called');
    // Reload data when navigating back to this page
    _loadUserStats();
  }
  
  Future<void> _initializeData() async {
    await _loadSavedExam();
    await _loadExamCategories();
    // Load user stats after exam is loaded (so categories are filtered correctly)
    _loadUserStats();
  }
  
  Future<void> _loadSavedExam() async {
    final prefs = await SharedPreferences.getInstance();
    final savedExam = prefs.getString('selectedExam');
    if (savedExam != null) {
      setState(() {
        _currentExam = savedExam;
      });
    } else {
      setState(() {
        _currentExam = widget.selectedExam ?? 'Select Exam';
      });
    }
  }
  
  Future<void> _loadExamCategories() async {
    setState(() => _isLoadingExams = true);
    
    try {
      final response = await ApiService.getExamCategories();
      
      if (response['success'] == true) {
        final categories = List<Map<String, dynamic>>.from(response['categories'] ?? []);
        final prefs = await SharedPreferences.getInstance();
        final savedExam = prefs.getString('selectedExam');
        final savedExamId = prefs.getInt('selectedExamId');
        
        if (mounted) {
          setState(() {
            _examCategories = categories;
            
            if (savedExam != null && savedExamId != null) {
              // Verify the saved exam still exists
              final examExists = categories.any((cat) => cat['id'] == savedExamId);
              if (examExists) {
                _currentExam = savedExam;
                _selectedExamId = savedExamId;
              } else if (categories.isNotEmpty) {
                // Saved exam doesn't exist, select first exam
                final firstExam = categories.first;
                _currentExam = firstExam['name'] ?? 'Select Exam';
                _selectedExamId = firstExam['id'];
                _saveSelectedExam(firstExam['name'], firstExam['id']);
              }
            } else if (categories.isNotEmpty) {
              // Auto-select first exam if no saved exam
              final firstExam = categories.first;
              _currentExam = firstExam['name'] ?? 'Select Exam';
              _selectedExamId = firstExam['id'];
              _saveSelectedExam(firstExam['name'], firstExam['id']);
            }
            
            _isLoadingExams = false;
          });
        }
      } else {
        if (mounted) {
          setState(() => _isLoadingExams = false);
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoadingExams = false);
      }
    }
  }
  
  Future<void> _saveSelectedExam(String examName, int examId) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('selectedExam', examName);
    await prefs.setInt('selectedExamId', examId);
    
    // Reload test categories for the new exam
    await _loadTestCategories(examId);
    
    // Also update user's exam in database if user is logged in
    final userId = prefs.getInt('userId');
    if (userId != null) {
      // Note: This would require adding exam_category_id to users table
      // For now, we just save to SharedPreferences
    }
  }
  
  Future<void> _loadTestCategories(int? examId) async {
    setState(() => _isLoadingCategories = true);
    
    try {
      // Load test categories filtered by selected exam
      final categoriesResponse = await ApiService.getTestCategories(examId: examId);
      if (categoriesResponse['success'] == true) {
        final categories = List<Map<String, dynamic>>.from(categoriesResponse['categories'] ?? []);
        
        if (mounted) {
          setState(() {
            _testCategories = categories;
            _isLoadingCategories = false;
          });
        }
      } else {
        if (mounted) {
          setState(() => _isLoadingCategories = false);
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoadingCategories = false);
      }
    }
  }

  Future<void> _loadUserStats() async {
    setState(() {
      _isLoadingStats = true;
      _isLoadingCategories = true;
    });
    
    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      
      // Get selected exam ID to filter test categories
      final selectedExamId = prefs.getInt('selectedExamId');
      
      // Load test categories filtered by selected exam
      print('🔍 Loading test categories for exam: $selectedExamId, user: $userId');
      final categoriesResponse = await ApiService.getTestCategories(examId: selectedExamId, userId: userId);
      print('📥 Categories response: $categoriesResponse');
      
      if (categoriesResponse['success'] == true) {
        final categories = List<Map<String, dynamic>>.from(categoriesResponse['categories'] ?? []);
        print('✅ Loaded ${categories.length} categories');
        
        // Log each category's data
        for (var cat in categories) {
          print('  📁 ${cat['name']}: sessions=${cat['sessions_count'] ?? 0}, completed=${cat['completed_count'] ?? 0}');
        }
        
        _testCategories = categories;
      } else {
        print('❌ Failed to load categories: ${categoriesResponse['message']}');
      }
      
      if (userId != null) {
        print('🔍 Loading test history for user: $userId');
        
        // Load test history
        final historyResponse = await ApiService.getTestHistory(userId: userId);
        
        print('📥 Test history response: $historyResponse');
        
        if (historyResponse['success'] == true) {
          final history = List<Map<String, dynamic>>.from(historyResponse['history'] ?? []);
          
          print('✅ Loaded ${history.length} test results');
          
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
          
          print('📊 Stats:');
          print('  Tests taken: $_testsTaken');
          print('  Average score: $_avgScore');
          print('  Recent tests: ${_recentTests.length}');
        } else {
          print('❌ Failed to load test history: ${historyResponse['message']}');
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
        print('✅ State updated - Stats: $_testsTaken, Categories: ${_testCategories.length}, Recent: ${_recentTests.length}');
      }
    } catch (e) {
      print('❌ Error loading user stats: $e');
      if (mounted) {
        setState(() {
          _isLoadingStats = false;
          _isLoadingCategories = false;
        });
      }
    }
    
    if (mounted) {
      print('✅ Final state: Tests taken=$_testsTaken, Recent tests=${_recentTests.length}');
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
    
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) return;
        
        // Double back press to exit
        final now = DateTime.now();
        if (_lastBackPressTime == null || 
            now.difference(_lastBackPressTime!) > const Duration(seconds: 2)) {
          _lastBackPressTime = now;
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                'Press back again to exit',
                style: GoogleFonts.poppins(fontSize: 14),
              ),
              duration: const Duration(seconds: 2),
              backgroundColor: AppColors.textSecondary,
              behavior: SnackBarBehavior.floating,
              margin: const EdgeInsets.all(16),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
          );
        } else {
          // Exit the app
          Navigator.of(context).pop();
        }
      },
      child: Scaffold(
        backgroundColor: ThemeHelper.backgroundColor(context),
        body: Center(
          child: Container(
            constraints: const BoxConstraints(maxWidth: 450),
            child: _pages[_selectedIndex],
          ),
        ),
        bottomNavigationBar: _buildBottomNavigationBar(),
      ),
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
              
              // Annual Planner 2026
              _buildAnnualPlannerSection(),
              
              const SizedBox(height: 24),
              
              // Test Categories
              _buildTestCategoriesSection(),
              
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
            'SUDAR App',
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
      // Show premium badge if user is premium
      actions: [
        if (_isPremium)
          Container(
            margin: const EdgeInsets.only(right: 16),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFFFFD700), Color(0xFFFFA500)],
              ),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.workspace_premium, color: Colors.white, size: 16),
                const SizedBox(width: 4),
                Text(
                  'PRO',
                  style: GoogleFonts.poppins(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
              ],
            ),
          ),
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
                  if (_isLoadingExams)
                    const Center(
                      child: Padding(
                        padding: EdgeInsets.all(32.0),
                        child: CircularProgressIndicator(color: AppColors.primary),
                      ),
                    )
                  else if (_examCategories.isEmpty)
                    Center(
                      child: Padding(
                        padding: const EdgeInsets.all(32.0),
                        child: Column(
                          children: [
                            Icon(Icons.inbox_outlined, size: 48, color: AppColors.textLight),
                            const SizedBox(height: 16),
                            Text(
                              'No exams available',
                              style: GoogleFonts.poppins(
                                fontSize: 14,
                                color: AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ),
                    )
                  else
                    Expanded(
                      child: ListView.builder(
                        controller: scrollController,
                        itemCount: _examCategories.length,
                        itemBuilder: (context, index) {
                          final exam = _examCategories[index];
                          final examName = exam['name'] ?? 'Unknown';
                          final examId = exam['id'];
                          final isSelected = examId == _selectedExamId;
                          
                          return InkWell(
                            onTap: () async {
                              setState(() {
                                _currentExam = examName;
                                _selectedExamId = examId;
                              });
                              Navigator.pop(context);
                              
                              // Save and reload categories for new exam
                              await _saveSelectedExam(examName, examId);
                              
                              // Show success message
                              if (mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                    content: Text(
                                      'Exam set to: $examName',
                                      style: GoogleFonts.poppins(),
                                    ),
                                    backgroundColor: AppColors.success,
                                    duration: const Duration(seconds: 2),
                                  ),
                                );
                              }
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
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          examName,
                                          style: GoogleFonts.poppins(
                                            fontSize: 15,
                                            fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
                                            color: isSelected ? AppColors.primary : AppColors.textPrimary,
                                          ),
                                        ),
                                        if (exam['description'] != null && exam['description'].toString().isNotEmpty)
                                          Padding(
                                            padding: const EdgeInsets.only(top: 4),
                                            child: Text(
                                              exam['description'],
                                              style: GoogleFonts.poppins(
                                                fontSize: 12,
                                                color: AppColors.textSecondary,
                                              ),
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ),
                                      ],
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

  Widget _buildAnnualPlannerSection() {
    return Container(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Color(0xFF1E3A5F),
            Color(0xFF0F172A),
          ],
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF1E3A5F).withOpacity(0.3),
            blurRadius: 15,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => const AnnualPlannerPage(),
              ),
            );
          },
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: Colors.white.withOpacity(0.2)),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(
                            Icons.calendar_month_rounded,
                            color: Color(0xFF5EEAD4),
                            size: 16,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            'TNPSC Official Schedule',
                            style: GoogleFonts.poppins(
                              fontSize: 11,
                              fontWeight: FontWeight.w600,
                              color: const Color(0xFF5EEAD4),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const Spacer(),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [Color(0xFFF97316), Color(0xFFEA580C)],
                        ),
                        borderRadius: BorderRadius.circular(8),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFFF97316).withOpacity(0.4),
                            blurRadius: 10,
                            offset: const Offset(0, 3),
                          ),
                        ],
                      ),
                      child: Text(
                        '2026',
                        style: GoogleFonts.poppins(
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                          color: Colors.white,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Annual Planner',
                            style: GoogleFonts.poppins(
                              fontSize: 22,
                              fontWeight: FontWeight.w800,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Programme of Examinations',
                            style: GoogleFonts.poppins(
                              fontSize: 13,
                              fontWeight: FontWeight.w400,
                              color: Colors.white.withOpacity(0.7),
                            ),
                          ),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Icon(
                        Icons.arrow_forward_ios_rounded,
                        color: Colors.white,
                        size: 18,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                // Quick info row
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: [
                      _buildPlannerStat('6', 'Exams'),
                      Container(
                        height: 30,
                        width: 1,
                        color: Colors.white.withOpacity(0.2),
                      ),
                      _buildPlannerStat('21', 'Days of Exam'),
                      Container(
                        height: 30,
                        width: 1,
                        color: Colors.white.withOpacity(0.2),
                      ),
                      _buildPlannerStat('Latest', 'Updated'),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPlannerStat(String value, String label) {
    return Column(
      children: [
        Text(
          value,
          style: GoogleFonts.poppins(
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: Colors.white,
          ),
        ),
        Text(
          label,
          style: GoogleFonts.poppins(
            fontSize: 10,
            color: Colors.white.withOpacity(0.6),
          ),
        ),
      ],
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
            TextButton.icon(
              onPressed: () {
                setState(() => _selectedIndex = 1); // Go to Tests page
              },
              icon: Icon(Icons.grid_view_rounded, size: 16, color: AppColors.primary),
              label: Text(
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
        const SizedBox(height: 16),
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
              child: Column(
                children: [
                  Icon(Icons.folder_open_rounded, size: 48, color: AppColors.textLight),
                  const SizedBox(height: 12),
                  Text(
                    'No test categories available',
                    style: GoogleFonts.poppins(
                      fontSize: 14,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
          )
        else
          _buildCategoryGrid(),
      ],
    );
  }

  Widget _buildCategoryGrid() {
    final categories = _testCategories.take(6).toList();
    final List<Widget> rows = [];
    
    for (int i = 0; i < categories.length; i += 2) {
      final List<Widget> rowItems = [];
      
      rowItems.add(Expanded(child: _buildGridCategoryCard(categories[i], i)));
      
      if (i + 1 < categories.length) {
        rowItems.add(const SizedBox(width: 12));
        rowItems.add(Expanded(child: _buildGridCategoryCard(categories[i + 1], i + 1)));
      } else {
        rowItems.add(const SizedBox(width: 12));
        rowItems.add(const Expanded(child: SizedBox()));
      }
      
      rows.add(
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Row(children: rowItems),
        ),
      );
    }
    
    return Column(children: rows);
  }

  Widget _buildGridCategoryCard(Map<String, dynamic> category, int index) {
    final name = category['name'] ?? 'Unknown';
    final sessionsCount = (category['sessions_count'] ?? 0) is int 
        ? category['sessions_count'] 
        : int.tryParse(category['sessions_count']?.toString() ?? '0') ?? 0;
    final completedCount = (category['completed_count'] ?? 0) is int
        ? category['completed_count']
        : int.tryParse(category['completed_count']?.toString() ?? '0') ?? 0;
    final progress = sessionsCount > 0 ? (completedCount / sessionsCount).clamp(0.0, 1.0) : 0.0;
    
    // Clean accent colors
    final List<Color> colors = [
      const Color(0xFF6366F1), // Indigo
      const Color(0xFF10B981), // Emerald
      const Color(0xFFF59E0B), // Amber
      const Color(0xFFEF4444), // Red
      const Color(0xFF8B5CF6), // Violet
      const Color(0xFF06B6D4), // Cyan
    ];
    
    // Assign icon based on category name
    IconData icon;
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
    } else if (name.contains('Economy') || name.contains('Economic')) {
      icon = Icons.account_balance_rounded;
    } else if (name.contains('Polity') || name.contains('Constitution')) {
      icon = Icons.gavel_rounded;
    } else if (name.contains('Geography')) {
      icon = Icons.terrain_rounded;
    } else {
      icon = Icons.menu_book_rounded;
    }
    
    final color = colors[index % colors.length];
    
    return Container(
      height: 140,
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(14),
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
            setState(() => _selectedIndex = 1);
          },
          borderRadius: BorderRadius.circular(14),
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
                        '$sessionsCount',
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

  Widget _buildCategoryCardFromData(Map<String, dynamic> category) {
    final name = category['name'] ?? 'Unknown';
    final sessionsCount = (category['sessions_count'] ?? 0) is int 
        ? category['sessions_count'] 
        : int.tryParse(category['sessions_count']?.toString() ?? '0') ?? 0;
    final completedCount = (category['completed_count'] ?? 0) is int
        ? category['completed_count']
        : int.tryParse(category['completed_count']?.toString() ?? '0') ?? 0;
    final progress = sessionsCount > 0 ? (completedCount / sessionsCount).clamp(0.0, 1.0) : 0.0;
    
    print('📊 Category card: $name - Sessions: $sessionsCount, Completed: $completedCount, Progress: ${(progress * 100).toStringAsFixed(0)}%');
    
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
    print('📊 Building recent tests section: ${_recentTests.length} tests, loading: $_isLoadingStats');
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Recent Tests',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: ThemeHelper.textPrimary(context),
              ),
            ),
            IconButton(
              icon: Icon(Icons.refresh, color: AppColors.primary),
              onPressed: () {
                print('🔄 Manual refresh triggered');
                _loadUserStats();
              },
              tooltip: 'Refresh',
            ),
          ],
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


