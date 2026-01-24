import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';

class ProgressPage extends StatefulWidget {
  const ProgressPage({Key? key}) : super(key: key);

  @override
  State<ProgressPage> createState() => _ProgressPageState();
}

class _ProgressPageState extends State<ProgressPage> {
  bool _isLoading = true;
  int _testsTaken = 0;
  double _avgScore = 0.0;
  int _rank = 0;
  int _streak = 0;
  List<Map<String, dynamic>> _testHistory = [];
  List<Map<String, dynamic>> _performanceTrend = [];
  List<Map<String, dynamic>> _strengths = [];
  List<Map<String, dynamic>> _weaknesses = [];
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadProgressData();
  }

  Future<void> _loadProgressData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    
    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      
      print('ProgressPage: Loading data for userId: $userId');
      
      if (userId == null) {
        if (mounted) {
          setState(() {
            _isLoading = false;
            _errorMessage = 'User not logged in. Please login again.';
          });
        }
        return;
      }
      
      // Load analytics data
      final analyticsResponse = await ApiService.getProgressAnalytics(userId: userId);
      print('ProgressPage: Analytics response: ${analyticsResponse['success']}');
      print('ProgressPage: Analytics data: $analyticsResponse');
      
      // Load test history
      final historyResponse = await ApiService.getTestHistory(userId: userId);
      print('ProgressPage: History response: ${historyResponse['success']}');
      print('ProgressPage: History count: ${historyResponse['count'] ?? 0}');
      
      if (mounted) {
        if (analyticsResponse['success'] == true) {
          final analytics = analyticsResponse;
          final overallStats = analytics['overall_stats'] ?? {};
          print('ProgressPage: Overall stats: $overallStats');
          
          setState(() {
            _testsTaken = (overallStats['total_tests'] ?? 0) as int;
            _avgScore = ((overallStats['avg_score'] ?? 0) as num).toDouble();
            _rank = (analytics['rank'] ?? 0) as int;
            _streak = (analytics['streak'] ?? 0) as int;
            
            print('ProgressPage: Tests taken: $_testsTaken, Avg score: $_avgScore, Rank: $_rank, Streak: $_streak');
            
            final trendList = analytics['performance_trend'] ?? [];
            _performanceTrend = trendList is List 
              ? List<Map<String, dynamic>>.from(trendList.map((e) => e as Map<String, dynamic>))
              : [];
            
            final strengthsList = analytics['strengths'] ?? [];
            _strengths = strengthsList is List
              ? List<Map<String, dynamic>>.from(strengthsList.map((e) => e as Map<String, dynamic>))
              : [];
            
            final weaknessesList = analytics['weaknesses'] ?? [];
            _weaknesses = weaknessesList is List
              ? List<Map<String, dynamic>>.from(weaknessesList.map((e) => e as Map<String, dynamic>))
              : [];
            
            print('ProgressPage: Trend items: ${_performanceTrend.length}, Strengths: ${_strengths.length}, Weaknesses: ${_weaknesses.length}');
            _errorMessage = null;
          });
        } else {
          final errorMsg = analyticsResponse['message'] ?? 'Failed to load analytics';
          print('ProgressPage: Analytics failed: $errorMsg');
          setState(() {
            _errorMessage = 'API Error: $errorMsg';
          });
        }
        
        if (historyResponse['success'] == true) {
          final historyList = historyResponse['history'] ?? [];
          final List<Map<String, dynamic>> history = historyList is List
            ? List<Map<String, dynamic>>.from(historyList.map((e) => e as Map<String, dynamic>))
            : <Map<String, dynamic>>[];
          print('ProgressPage: Loaded ${history.length} test history items');
          setState(() {
            _testHistory = history;
          });
        } else {
          final errorMsg = historyResponse['message'] ?? 'Failed to load history';
          print('ProgressPage: History failed: $errorMsg');
          if (_errorMessage == null) {
            setState(() {
              _errorMessage = 'History Error: $errorMsg';
            });
          }
        }
        
        setState(() => _isLoading = false);
      }
    } catch (e) {
      print('ProgressPage: Error loading data: $e');
      if (mounted) {
        setState(() {
          _isLoading = false;
          _errorMessage = 'Connection error: $e\n\nMake sure:\n1. XAMPP Apache is running\n2. API is accessible at http://localhost/MockTest/api';
        });
      }
    }
  }

  String _formatDate(String? dateStr) {
    if (dateStr == null) return 'Unknown date';
    try {
      final date = DateTime.parse(dateStr);
      final now = DateTime.now();
      final difference = now.difference(date);
      
      if (difference.inDays == 0) {
        return 'Today';
      } else if (difference.inDays == 1) {
        return 'Yesterday';
      } else if (difference.inDays < 7) {
        return '${difference.inDays} days ago';
      } else {
        return '${date.day}/${date.month}/${date.year}';
      }
    } catch (e) {
      return dateStr;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      appBar: AppBar(
        backgroundColor: ThemeHelper.cardColor(context),
        elevation: 1,
        title: Text(
          'Your Progress',
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: ThemeHelper.textPrimary(context),
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadProgressData,
            tooltip: 'Refresh',
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Error Message
            if (_errorMessage != null)
              Container(
                margin: const EdgeInsets.only(bottom: 16),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.error.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.error.withOpacity(0.3)),
                ),
                child: Row(
                  children: [
                    Icon(Icons.error_outline, color: AppColors.error),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        _errorMessage!,
                        style: GoogleFonts.poppins(
                          fontSize: 13,
                          color: AppColors.error,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            // Overall Stats Card
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [AppColors.primary, AppColors.primary.withOpacity(0.8)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.primary.withOpacity(0.3),
                    blurRadius: 10,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Column(
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: _buildStatItem(context,
                          icon: Icons.assignment_turned_in_rounded,
                          label: 'Tests Taken',
                          value: _isLoading ? '...' : '$_testsTaken',
                          isWhite: true,
                        ),
                      ),
                      Container(width: 1, height: 40, color: Colors.white.withOpacity(0.3)),
                      Expanded(
                        child: _buildStatItem(context,
                          icon: Icons.trending_up_rounded,
                          label: 'Avg Score',
                          value: _isLoading ? '...' : '${_avgScore.toStringAsFixed(0)}%',
                          isWhite: true,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  const Divider(color: Colors.white24, height: 1),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: _buildStatItem(context,
                          icon: Icons.emoji_events_rounded,
                          label: 'Rank',
                          value: _isLoading ? '...' : _rank > 0 ? '#$_rank' : '--',
                          isWhite: true,
                        ),
                      ),
                      Container(width: 1, height: 40, color: Colors.white.withOpacity(0.3)),
                      Expanded(
                        child: _buildStatItem(context,
                          icon: Icons.local_fire_department_rounded,
                          label: 'Streak',
                          value: _isLoading ? '...' : '$_streak ${_streak == 1 ? 'day' : 'days'}',
                          isWhite: true,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Performance Chart Section
            Text(
              'Performance Overview',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: ThemeHelper.textPrimary(context),
              ),
            ),
            
            const SizedBox(height: 12),
            
            Container(
              height: 200,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 10,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _performanceTrend.isEmpty
                  ? Center(
                      child: Text(
                        'No performance data yet',
                        style: GoogleFonts.poppins(color: Colors.grey[600]),
                      ),
                    )
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          'Performance Trend',
                          style: GoogleFonts.poppins(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                            color: ThemeHelper.textPrimary(context),
                          ),
                        ),
                        const SizedBox(height: 12),
                        Expanded(
                          child: LayoutBuilder(
                            builder: (context, constraints) {
                              // Limit to last 7 items to prevent overflow
                              final displayTrends = _performanceTrend.length > 7 
                                ? _performanceTrend.sublist(_performanceTrend.length - 7)
                                : _performanceTrend;
                              
                              return Row(
                                mainAxisAlignment: MainAxisAlignment.spaceAround,
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: List<Widget>.generate(displayTrends.length, (index) {
                                  final item = displayTrends[index];
                                  final score = ((item['score'] ?? 0) as num).toDouble();
                                  final height = (score / 100).clamp(0.0, 1.0);
                                  final label = index < 7 ? ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'][index] : 'T${index + 1}';
                                  return _buildChartBar(label, height, score, constraints.maxHeight);
                                }),
                              );
                            },
                          ),
                        ),
                      ],
                    ),
            ),
            
            const SizedBox(height: 24),
            
            // Recent Tests
            Text(
              'Recent Tests',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: ThemeHelper.textPrimary(context),
              ),
            ),
            
            const SizedBox(height: 12),
            
            // Show test history from API
            if (_isLoading)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(40),
                  child: CircularProgressIndicator(),
                ),
              )
            else if (_testHistory.isEmpty)
              Container(
                padding: const EdgeInsets.all(40),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Center(
                  child: Column(
                    children: [
                      Icon(Icons.assignment_outlined, size: 48, color: Colors.grey[400]),
                      const SizedBox(height: 16),
                      Text(
                        'No test history yet',
                        style: GoogleFonts.poppins(
                          fontSize: 16,
                          color: Colors.grey[600],
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Start taking tests to see your progress',
                        style: GoogleFonts.poppins(
                          fontSize: 14,
                          color: Colors.grey[500],
                        ),
                      ),
                    ],
                  ),
                ),
              )
            else
              ...List<Widget>.generate(_testHistory.length, (index) {
                final test = _testHistory[index];
                final percentage = ((test['percentage'] ?? 0) as num).toDouble();
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: _buildActivityCard(context,
                    title: (test['session_name'] ?? test['test_name'] ?? 'Test').toString(),
                    date: _formatDate(test['completed_at']?.toString() ?? test['submitted_at']?.toString()),
                    score: percentage.toInt(),
                    questions: (test['total_questions'] ?? 0) as int,
                    icon: percentage >= 50 ? Icons.check_circle : Icons.cancel,
                    iconColor: percentage >= 75 ? AppColors.success : percentage >= 50 ? AppColors.warning : AppColors.error,
                  ),
                );
              }),
            
            if (!_isLoading && _testHistory.isNotEmpty)
              const SizedBox(height: 12),
            
            if (false) // This is a placeholder - remove completely
              _buildActivityCard(context,
              title: 'Placeholder',
              date: 'Never',
              score: 0,
              questions: 0,
              icon: Icons.check_circle,
              iconColor: AppColors.success,
            ),
            
            const SizedBox(height: 24),
            
            // Strengths & Weaknesses
            Text(
              'Strengths & Weaknesses',
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
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppColors.success.withOpacity(0.3)),
                    ),
                    child: Column(
                      children: [
                        Icon(Icons.thumb_up_rounded, color: AppColors.success, size: 32),
                        const SizedBox(height: 8),
                        Text(
                          'Strengths',
                          style: GoogleFonts.poppins(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: ThemeHelper.textPrimary(context),
                          ),
                        ),
                        const SizedBox(height: 8),
                        if (_strengths.isEmpty)
                          Text(
                            'No strengths yet',
                            style: GoogleFonts.poppins(
                              fontSize: 12,
                              color: AppColors.textSecondary,
                            ),
                          )
                        else
                          ...List<Widget>.generate(_strengths.length, (index) {
                            final strength = _strengths[index];
                            final name = (strength['name'] ?? 'Unknown').toString();
                            final avgScore = ((strength['avg_score'] ?? 0) as num).toDouble();
                            return Padding(
                              padding: const EdgeInsets.only(bottom: 6),
                              child: _buildTag(
                                '$name (${avgScore.toStringAsFixed(0)}%)',
                                AppColors.success,
                              ),
                            );
                          }),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppColors.error.withOpacity(0.3)),
                    ),
                    child: Column(
                      children: [
                        Icon(Icons.thumb_down_rounded, color: AppColors.error, size: 32),
                        const SizedBox(height: 8),
                        Text(
                          'Needs Work',
                          style: GoogleFonts.poppins(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: ThemeHelper.textPrimary(context),
                          ),
                        ),
                        const SizedBox(height: 8),
                        if (_weaknesses.isEmpty)
                          Text(
                            'No areas to improve',
                            style: GoogleFonts.poppins(
                              fontSize: 12,
                              color: AppColors.textSecondary,
                            ),
                          )
                        else
                          ...List<Widget>.generate(_weaknesses.length, (index) {
                            final weakness = _weaknesses[index];
                            final name = (weakness['name'] ?? 'Unknown').toString();
                            final avgScore = ((weakness['avg_score'] ?? 0) as num).toDouble();
                            return Padding(
                              padding: const EdgeInsets.only(bottom: 6),
                              child: _buildTag(
                                '$name (${avgScore.toStringAsFixed(0)}%)',
                                AppColors.error,
                              ),
                            );
                          }),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatItem(BuildContext context, {
    required IconData icon,
    required String label,
    required String value,
    bool isWhite = false,
  }) {
    return Column(
      children: [
        Icon(
          icon,
          color: isWhite ? Colors.white : AppColors.primary,
          size: 28,
        ),
        const SizedBox(height: 8),
        Text(
          value,
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: isWhite ? Colors.white : AppColors.textPrimary,
          ),
        ),
        Text(
          label,
          style: GoogleFonts.poppins(
            fontSize: 12,
            color: isWhite ? Colors.white.withOpacity(0.9) : AppColors.textSecondary,
          ),
        ),
      ],
    );
  }

  Widget _buildPerformanceBar(BuildContext context, String subject, double progress, Color color) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              subject,
              style: GoogleFonts.poppins(
                fontSize: 14,
                fontWeight: FontWeight.w500,
                color: ThemeHelper.textPrimary(context),
              ),
            ),
            Text(
              '${(progress * 100).toInt()}%',
              style: GoogleFonts.poppins(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: color,
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: progress,
            backgroundColor: color.withOpacity(0.1),
            valueColor: AlwaysStoppedAnimation<Color>(color),
            minHeight: 8,
          ),
        ),
      ],
    );
  }

  Widget _buildActivityCard(BuildContext context, {
    required String title,
    required String date,
    required int score,
    required int questions,
    required IconData icon,
    required Color iconColor,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: iconColor.withOpacity(0.1),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: iconColor, size: 24),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: GoogleFonts.poppins(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: ThemeHelper.textPrimary(context),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  date,
                  style: GoogleFonts.poppins(
                    fontSize: 12,
                    color: AppColors.textLight,
                  ),
                ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                '$score%',
                style: GoogleFonts.poppins(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: iconColor,
                ),
              ),
              Text(
                '$questions Qs',
                style: GoogleFonts.poppins(
                  fontSize: 11,
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTag(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        text,
        style: GoogleFonts.poppins(
          fontSize: 12,
          fontWeight: FontWeight.w500,
          color: color,
        ),
      ),
    );
  }

  Widget _buildChartBar(String label, double value, double score, double maxHeight) {
    // Calculate available height for bar (maxHeight - text heights - spacing)
    // Score text (~14px) + spacing (4px) + label text (~15px) + spacing (8px) = ~41px
    final availableHeight = maxHeight - 50;
    final barHeight = (availableHeight * value).clamp(0.0, availableHeight);
    
    return Flexible(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.end,
        mainAxisSize: MainAxisSize.min,
        children: [
          Flexible(
            child: Text(
              score.toStringAsFixed(0),
              style: GoogleFonts.poppins(
                fontSize: 10,
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          const SizedBox(height: 4),
          Container(
            width: 28,
            height: barHeight,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [AppColors.primary, AppColors.primaryLight],
                begin: Alignment.bottomCenter,
                end: Alignment.topCenter,
              ),
              borderRadius: BorderRadius.circular(8),
            ),
          ),
          const SizedBox(height: 8),
          Flexible(
            child: Text(
              label,
              style: GoogleFonts.poppins(
                fontSize: 11,
                color: AppColors.textLight,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}

