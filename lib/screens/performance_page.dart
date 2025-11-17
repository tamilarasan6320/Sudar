import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';

class PerformancePage extends StatefulWidget {
  const PerformancePage({Key? key}) : super(key: key);

  @override
  State<PerformancePage> createState() => _PerformancePageState();
}

class _PerformancePageState extends State<PerformancePage> {
  bool _isLoading = true;
  String _selectedPeriod = 'all'; // week, month, year, all
  Map<String, dynamic>? _performanceData;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadPerformanceData();
  }

  Future<void> _loadPerformanceData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');

      if (userId == null) {
        setState(() {
          _errorMessage = 'User not logged in';
          _isLoading = false;
        });
        return;
      }

      final response = await ApiService.getPerformance(
        userId: userId,
        period: _selectedPeriod,
      );

      if (response['success'] == true) {
        setState(() {
          _performanceData = response;
          _isLoading = false;
        });
      } else {
        setState(() {
          _errorMessage = response['message'] ?? 'Failed to load performance data';
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() {
        _errorMessage = 'Error: $e';
        _isLoading = false;
      });
    }
  }

  void _changePeriod(String period) {
    if (_selectedPeriod != period) {
      setState(() {
        _selectedPeriod = period;
      });
      _loadPerformanceData();
    }
  }

  String _formatDate(String? dateStr) {
    if (dateStr == null) return '';
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

  Color _getCategoryColor(String categoryName) {
    if (categoryName.contains('Tamil')) return AppColors.primary;
    if (categoryName.contains('Science')) return const Color(0xFF00BCD4);
    if (categoryName.contains('Social')) return const Color(0xFF4CAF50);
    if (categoryName.contains('Aptitude')) return AppColors.secondary;
    if (categoryName.contains('Current')) return const Color(0xFFFF5722);
    return AppColors.primary;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      appBar: AppBar(
        backgroundColor: ThemeHelper.cardColor(context),
        elevation: 1,
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: ThemeHelper.textPrimary(context)),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Performance Analytics',
          style: GoogleFonts.poppins(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: ThemeHelper.textPrimary(context),
          ),
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
          : _errorMessage != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.error_outline, size: 64, color: AppColors.error),
                      const SizedBox(height: 16),
                      Text(
                        _errorMessage!,
                        style: GoogleFonts.poppins(color: AppColors.error),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: _loadPerformanceData,
                        child: const Text('Retry'),
                      ),
                    ],
                  ),
                )
              : _performanceData == null
                  ? const Center(child: Text('No data available'))
                  : SingleChildScrollView(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Overall Performance Card
                          _buildOverallPerformanceCard(),
                          
                          const SizedBox(height: 20),
                          
                          // Time Period Selector
                          _buildTimePeriodSelector(),
                          
                          const SizedBox(height: 20),
                          
                          // Performance Chart
                          _buildPerformanceChart(),
                          
                          const SizedBox(height: 24),
                          
                          // Subject-wise Performance
                          Text(
                            'Subject-wise Performance',
                            style: GoogleFonts.poppins(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: ThemeHelper.textPrimary(context),
                            ),
                          ),
                          
                          const SizedBox(height: 12),
                          
                          _buildSubjectPerformanceList(),
                          
                          const SizedBox(height: 24),
                          
                          // Strengths & Weaknesses
                          Text(
                            'Analysis',
                            style: GoogleFonts.poppins(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: ThemeHelper.textPrimary(context),
                            ),
                          ),
                          
                          const SizedBox(height: 12),
                          
                          Row(
                            children: [
                              Expanded(child: _buildStrengthsCard()),
                              const SizedBox(width: 12),
                              Expanded(child: _buildWeaknessesCard()),
                            ],
                          ),
                          
                          const SizedBox(height: 24),
                          
                          // Recent Activity Timeline
                          Text(
                            'Recent Activity',
                            style: GoogleFonts.poppins(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: ThemeHelper.textPrimary(context),
                            ),
                          ),
                          
                          const SizedBox(height: 12),
                          
                          _buildActivityTimeline(),
                        ],
                      ),
                    ),
    );
  }

  Widget _buildOverallPerformanceCard() {
    final overall = _performanceData?['overall'] ?? {};
    final avgScore = (overall['avg_score'] ?? 0.0).toDouble();
    final totalTests = overall['total_tests'] ?? 0;
    final avgTime = (overall['avg_time_taken'] ?? 0.0).toDouble();
    final hours = (avgTime / 60).toStringAsFixed(1);
    final passed = overall['passed'] ?? 0;
    final failed = overall['failed'] ?? 0;

    return Container(
      padding: const EdgeInsets.all(24),
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
            blurRadius: 12,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        children: [
          Text(
            'Overall Score',
            style: GoogleFonts.poppins(
              fontSize: 14,
              color: Colors.white.withOpacity(0.9),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '${avgScore.toStringAsFixed(1)}%',
            style: GoogleFonts.poppins(
              fontSize: 48,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '$passed passed, $failed failed',
            style: GoogleFonts.poppins(
              fontSize: 13,
              color: Colors.white.withOpacity(0.9),
            ),
          ),
          const SizedBox(height: 20),
          const Divider(color: Colors.white24, height: 1),
          const SizedBox(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildStatItem('Tests', totalTests.toString(), Icons.quiz_outlined),
              _buildStatItem('Hours', hours, Icons.access_time),
              _buildStatItem('Best', '${(overall['best_score'] ?? 0.0).toStringAsFixed(0)}%', Icons.emoji_events_outlined),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatItem(String label, String value, IconData icon) {
    return Column(
      children: [
        Icon(icon, color: Colors.white, size: 24),
        const SizedBox(height: 8),
        Text(
          value,
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
        Text(
          label,
          style: GoogleFonts.poppins(
            fontSize: 12,
            color: Colors.white.withOpacity(0.9),
          ),
        ),
      ],
    );
  }

  Widget _buildTimePeriodSelector() {
    return Container(
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: Row(
        children: [
          _buildPeriodButton('Week', 'week'),
          _buildPeriodButton('Month', 'month'),
          _buildPeriodButton('Year', 'year'),
          _buildPeriodButton('All', 'all'),
        ],
      ),
    );
  }

  Widget _buildPeriodButton(String label, String period) {
    final isSelected = _selectedPeriod == period;
    return Expanded(
      child: GestureDetector(
        onTap: () => _changePeriod(period),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            color: isSelected ? AppColors.primary : Colors.transparent,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Center(
            child: Text(
              label,
              style: GoogleFonts.poppins(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: isSelected ? Colors.white : ThemeHelper.textSecondary(context),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPerformanceChart() {
    final trendsList = _performanceData?['trends'] ?? [];
    final List<dynamic> trends = trendsList is List ? List<dynamic>.from(trendsList) : [];
    
    if (trends.isEmpty) {
      return Container(
        height: 200,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: ThemeHelper.cardColor(context),
          borderRadius: BorderRadius.circular(12),
          boxShadow: ThemeHelper.cardShadow(context),
        ),
        child: Center(
          child: Text(
            'No trend data available',
            style: GoogleFonts.poppins(
              color: AppColors.textSecondary,
            ),
          ),
        ),
      );
    }

    // Get last 7 items for chart
    final chartData = trends.length > 7 ? trends.sublist(trends.length - 7) : trends;
    if (chartData.isEmpty) {
      return Container(
        height: 200,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: ThemeHelper.cardColor(context),
          borderRadius: BorderRadius.circular(12),
          boxShadow: ThemeHelper.cardShadow(context),
        ),
        child: Center(
          child: Text(
            'No trend data available',
            style: GoogleFonts.poppins(
              color: AppColors.textSecondary,
            ),
          ),
        ),
      );
    }
    double maxScore = 0.0;
    for (var item in chartData) {
      final itemMap = item as Map<String, dynamic>;
      final score = (itemMap['score'] ?? 0.0).toDouble();
      if (score > maxScore) maxScore = score;
    }
    final maxHeight = maxScore > 0 ? maxScore : 100.0;

    return Container(
      height: 200,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Score Trend',
            style: GoogleFonts.poppins(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: ThemeHelper.textPrimary(context),
            ),
          ),
          const SizedBox(height: 20),
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: List<Widget>.generate(chartData.length, (index) {
                final item = chartData[index] as Map<String, dynamic>;
                final score = (item['score'] ?? 0.0).toDouble();
                final height = maxHeight > 0 ? (score / maxHeight) : 0.0;
                final day = index < 7 ? ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'][index] : 'Day ${index + 1}';
                return _buildChartBar(day, height.clamp(0.0, 1.0), score);
              }),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildChartBar(String day, double value, double score) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Text(
          score.toStringAsFixed(0),
          style: GoogleFonts.poppins(
            fontSize: 10,
            fontWeight: FontWeight.w600,
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(height: 4),
        Container(
          width: 32,
          height: 100 * value,
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
        Text(
          day,
          style: GoogleFonts.poppins(
            fontSize: 11,
            color: AppColors.textLight,
          ),
        ),
      ],
    );
  }

  Widget _buildSubjectPerformanceList() {
    final subjectsList = _performanceData?['subjects'] ?? [];
    final List<dynamic> subjects = subjectsList is List ? List<dynamic>.from(subjectsList) : [];
    
    if (subjects.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: ThemeHelper.cardColor(context),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Center(
          child: Text(
            'No subject data available',
            style: GoogleFonts.poppins(color: AppColors.textSecondary),
          ),
        ),
      );
    }

    final List<Widget> subjectWidgets = [];
    for (var subject in subjects) {
      final subjectMap = subject as Map<String, dynamic>;
      final name = (subjectMap['category_name'] ?? 'Unknown').toString();
      final avgScore = (subjectMap['avg_score'] ?? 0.0).toDouble();
      final progress = avgScore / 100.0;
      final passed = (subjectMap['passed'] ?? 0) as int;
      final total = (subjectMap['test_count'] ?? 0) as int;
      final color = _getCategoryColor(name);
      
      subjectWidgets.add(
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: _buildSubjectPerformance(name, progress, passed, total, color),
        ),
      );
    }
    return Column(children: subjectWidgets);
  }

  Widget _buildSubjectPerformance(String subject, double progress, int correct, int total, Color color) {
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
              Text(
                subject,
                style: GoogleFonts.poppins(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  color: ThemeHelper.textPrimary(context),
                ),
              ),
              Text(
                '${(progress * 100).toStringAsFixed(1)}%',
                style: GoogleFonts.poppins(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: color,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: LinearProgressIndicator(
                    value: progress.clamp(0.0, 1.0),
                    backgroundColor: color.withOpacity(0.1),
                    valueColor: AlwaysStoppedAnimation<Color>(color),
                    minHeight: 8,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Text(
                '$correct/$total',
                style: GoogleFonts.poppins(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStrengthsCard() {
    final strengthsList = _performanceData?['strengths'] ?? [];
    final List<dynamic> strengths = strengthsList is List ? List<dynamic>.from(strengthsList) : [];
    
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.success.withOpacity(0.3)),
        boxShadow: [
          BoxShadow(
            color: AppColors.success.withOpacity(0.1),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Icon(Icons.trending_up_rounded, color: AppColors.success, size: 32),
          const SizedBox(height: 8),
          Text(
            'Strengths',
            style: GoogleFonts.poppins(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: ThemeHelper.textPrimary(context),
            ),
          ),
          const SizedBox(height: 12),
          if (strengths.isEmpty)
            Text(
              'No strengths yet',
              style: GoogleFonts.poppins(
                fontSize: 12,
                color: AppColors.textSecondary,
              ),
            )
          else
            ...List<Widget>.generate(strengths.length, (index) {
              final strength = strengths[index] as Map<String, dynamic>;
              return Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: _buildTag(
                  '${strength['category'] ?? 'Unknown'} (${(strength['score'] ?? 0.0).toStringAsFixed(0)}%)',
                  AppColors.success,
                ),
              );
            }),
        ],
      ),
    );
  }

  Widget _buildWeaknessesCard() {
    final weaknessesList = _performanceData?['weaknesses'] ?? [];
    final List<dynamic> weaknesses = weaknessesList is List ? List<dynamic>.from(weaknessesList) : [];
    
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.warning.withOpacity(0.3)),
        boxShadow: [
          BoxShadow(
            color: AppColors.warning.withOpacity(0.1),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Icon(Icons.trending_down_rounded, color: AppColors.warning, size: 32),
          const SizedBox(height: 8),
          Text(
            'Improve',
            style: GoogleFonts.poppins(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: ThemeHelper.textPrimary(context),
            ),
          ),
          const SizedBox(height: 12),
          if (weaknesses.isEmpty)
            Text(
              'No areas to improve',
              style: GoogleFonts.poppins(
                fontSize: 12,
                color: AppColors.textSecondary,
              ),
            )
          else
            ...List<Widget>.generate(weaknesses.length, (index) {
              final weakness = weaknesses[index] as Map<String, dynamic>;
              return Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: _buildTag(
                  '${weakness['category'] ?? 'Unknown'} (${(weakness['score'] ?? 0.0).toStringAsFixed(0)}%)',
                  AppColors.warning,
                ),
              );
            }),
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

  Widget _buildActivityTimeline() {
    final trendsList = _performanceData?['trends'] ?? [];
    final List<dynamic> trends = trendsList is List ? List<dynamic>.from(trendsList) : [];
    
    if (trends.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: ThemeHelper.cardColor(context),
          borderRadius: BorderRadius.circular(12),
          boxShadow: ThemeHelper.cardShadow(context),
        ),
        child: Center(
          child: Text(
            'No recent activity',
            style: GoogleFonts.poppins(color: AppColors.textSecondary),
          ),
        ),
      );
    }

    // Get last 5 items
    final recentActivity = trends.length > 5 ? trends.sublist(trends.length - 5) : trends;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: Column(
        children: List<Widget>.generate(recentActivity.length, (index) {
          final item = recentActivity[index] as Map<String, dynamic>;
          final score = (item['score'] ?? 0.0).toDouble();
          final color = score >= 50 ? AppColors.success : AppColors.warning;
          
          return Padding(
            padding: EdgeInsets.only(bottom: index < recentActivity.length - 1 ? 16 : 0),
            child: _buildTimelineItem(
              (item['test_name'] ?? 'Test').toString(),
              '${score.toStringAsFixed(1)}% Score',
              _formatDate(item['date']?.toString()),
              color,
            ),
          );
        }),
      ),
    );
  }

  Widget _buildTimelineItem(String title, String subtitle, String time, Color color) {
    return Row(
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: GoogleFonts.poppins(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: ThemeHelper.textPrimary(context),
                ),
              ),
              const SizedBox(height: 2),
              Text(
                subtitle,
                style: GoogleFonts.poppins(
                  fontSize: 12,
                  color: color,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
        Text(
          time,
          style: GoogleFonts.poppins(
            fontSize: 11,
            color: AppColors.textLight,
          ),
        ),
      ],
    );
  }
}
