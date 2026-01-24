import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';

class TestHistoryPage extends StatefulWidget {
  const TestHistoryPage({Key? key}) : super(key: key);

  @override
  State<TestHistoryPage> createState() => _TestHistoryPageState();
}

class _TestHistoryPageState extends State<TestHistoryPage> {
  bool _isLoading = true;
  List<Map<String, dynamic>> _allTestHistory = [];
  List<Map<String, dynamic>> _filteredTestHistory = [];
  double _avgScore = 0;
  int _totalTests = 0;
  int _passedTests = 0;
  double _totalTime = 0;

  String _selectedPeriod = 'All'; // All | This Week | This Month

  @override
  void initState() {
    super.initState();
    _loadTestHistory();
  }

  Future<void> _loadTestHistory() async {
    setState(() => _isLoading = true);
    
    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      
      if (userId != null) {
        final response = await ApiService.getTestHistory(userId: userId);
        
        if (response['success'] == true) {
          final history = List<Map<String, dynamic>>.from(response['history'] ?? []);
          
          if (mounted) {
            setState(() {
              _allTestHistory = history;
              _applyPeriodFilter();
              _isLoading = false;
            });
          }
        } else {
          if (mounted) {
            setState(() => _isLoading = false);
          }
        }
      } else {
        if (mounted) {
          setState(() => _isLoading = false);
        }
      }
    } catch (e) {
      print('Error loading test history: $e');
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  DateTime? _parseCompletedAt(Map<String, dynamic> test) {
    final raw = test['completed_at'] ?? test['submitted_at'] ?? test['created_at'];
    if (raw == null) return null;
    try {
      return DateTime.parse(raw.toString());
    } catch (_) {
      return null;
    }
  }

  void _applyPeriodFilter() {
    final now = DateTime.now();

    bool include(Map<String, dynamic> test) {
      if (_selectedPeriod == 'All') return true;

      final dt = _parseCompletedAt(test);
      if (dt == null) return true; // don't hide items we can't parse

      if (_selectedPeriod == 'This Week') {
        return now.difference(dt).inDays < 7;
      }

      if (_selectedPeriod == 'This Month') {
        return dt.year == now.year && dt.month == now.month;
      }

      return true;
    }

    _filteredTestHistory = _allTestHistory.where(include).toList();

    // Recompute stats based on filtered list
    double totalScore = 0;
    int passed = 0;
    double totalTimeHours = 0;

    for (var test in _filteredTestHistory) {
      final percentage = (test['percentage'] ?? 0).toDouble();
      totalScore += percentage;
      if (percentage >= 50) passed++;

      // Calculate time if available (assuming time_taken in minutes)
      if (test['time_taken'] != null) {
        totalTimeHours += (test['time_taken'] / 60);
      }
    }

    _totalTests = _filteredTestHistory.length;
    _avgScore = _filteredTestHistory.isNotEmpty ? totalScore / _filteredTestHistory.length : 0;
    _passedTests = passed;
    _totalTime = totalTimeHours;
  }

  String _formatDate(String? dateStr) {
    if (dateStr == null) return 'Unknown date';
    try {
      final date = DateTime.parse(dateStr);
      final now = DateTime.now();
      final difference = now.difference(date);
      
      if (difference.inHours < 24) {
        return '${difference.inHours} hours ago';
      } else if (difference.inDays == 1) {
        return 'Yesterday';
      } else if (difference.inDays < 7) {
        return '${difference.inDays} days ago';
      } else if (difference.inDays < 14) {
        return '1 week ago';
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
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: ThemeHelper.textPrimary(context)),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Test History',
          style: GoogleFonts.poppins(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: ThemeHelper.textPrimary(context),
          ),
        ),
        actions: [
          IconButton(
            icon: Icon(Icons.refresh, color: ThemeHelper.textPrimary(context)),
            onPressed: _loadTestHistory,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Overall Stats Card
                  _buildOverallStatsCard(),
                  
                  const SizedBox(height: 24),
                  
                  // Time Period Selector
                  _buildTimePeriodSelector(context),
                  
                  const SizedBox(height: 20),
                  
                  // Recent Tests Header
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
                      Text(
                        '$_totalTests Total',
                        style: GoogleFonts.poppins(
                          fontSize: 14,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                  
                  const SizedBox(height: 12),
                  
                  // Test History Items - Dynamic from API
                  if (_filteredTestHistory.isEmpty)
                    Container(
                      padding: const EdgeInsets.all(40),
                      decoration: BoxDecoration(
                        color: ThemeHelper.cardColor(context),
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
                              'Start taking tests to see your history',
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
                    ..._filteredTestHistory.map((test) {
                      final percentage = (test['percentage'] ?? 0).toDouble();
                      final score = test['score'] ?? 0;
                      final totalQuestions = test['total_questions'] ?? 0;
                      final status = percentage >= 75 
                          ? 'Excellent' 
                          : percentage >= 50 
                              ? 'Passed' 
                              : 'Average';
                      final statusColor = percentage >= 75 
                          ? AppColors.success 
                          : percentage >= 50 
                              ? AppColors.warning 
                              : AppColors.error;
                      
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: _buildTestHistoryCard(
                          context,
                          test['session_name'] ?? 'Test',
                          '$score/$totalQuestions',
                          '${percentage.toStringAsFixed(0)}%',
                          status,
                          _formatDate(test['completed_at']),
                          statusColor,
                          percentage.toInt(),
                        ),
                      );
                    }).toList(),
                ],
              ),
            ),
    );
  }

  Widget _buildOverallStatsCard() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.secondary, AppColors.secondary.withOpacity(0.8)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: AppColors.secondary.withOpacity(0.3),
            blurRadius: 12,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        children: [
          Text(
            'Average Score',
            style: GoogleFonts.poppins(
              fontSize: 14,
              color: Colors.white.withOpacity(0.9),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '${_avgScore.toStringAsFixed(1)}%',
            style: GoogleFonts.poppins(
              fontSize: 48,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            _totalTests > 0 ? 'Based on $_totalTests tests' : 'No tests yet',
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
              _buildStatItem('$_totalTests', 'Tests', Icons.assignment_outlined),
              _buildStatItem('$_passedTests', 'Passed', Icons.check_circle_outline),
              _buildStatItem('${_totalTime.toStringAsFixed(1)}h', 'Time', Icons.access_time),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatItem(String value, String label, IconData icon) {
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

  Widget _buildTimePeriodSelector(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: Row(
        children: [
          _buildPeriodButton(context, 'All', _selectedPeriod == 'All'),
          _buildPeriodButton(context, 'This Week', _selectedPeriod == 'This Week'),
          _buildPeriodButton(context, 'This Month', _selectedPeriod == 'This Month'),
        ],
      ),
    );
  }

  Widget _buildPeriodButton(BuildContext context, String label, bool isSelected) {
    return Expanded(
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: () {
            if (_selectedPeriod == label) return;
            setState(() {
              _selectedPeriod = label;
              _applyPeriodFilter();
            });
          },
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 12),
            decoration: BoxDecoration(
              color: isSelected ? AppColors.secondary : Colors.transparent,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Center(
              child: Text(
                label,
                style: GoogleFonts.poppins(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: isSelected ? Colors.white : AppColors.textSecondary,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildTestHistoryCard(
    BuildContext context,
    String title,
    String score,
    String percentage,
    String status,
    String time,
    Color statusColor,
    int percentValue,
  ) {
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
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: statusColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  status,
                  style: GoogleFonts.poppins(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: statusColor,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Score',
                      style: GoogleFonts.poppins(
                        fontSize: 11,
                        color: AppColors.textLight,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      score,
                      style: GoogleFonts.poppins(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: ThemeHelper.textPrimary(context),
                      ),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Percentage',
                      style: GoogleFonts.poppins(
                        fontSize: 11,
                        color: AppColors.textLight,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      percentage,
                      style: GoogleFonts.poppins(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: statusColor,
                      ),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Time',
                      style: GoogleFonts.poppins(
                        fontSize: 11,
                        color: AppColors.textLight,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      time,
                      style: GoogleFonts.poppins(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: LinearProgressIndicator(
              value: percentValue / 100,
              backgroundColor: statusColor.withOpacity(0.1),
              valueColor: AlwaysStoppedAnimation<Color>(statusColor),
              minHeight: 8,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.visibility_outlined, size: 16),
                  label: Text(
                    'View Details',
                    style: GoogleFonts.poppins(fontSize: 12),
                  ),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.primary,
                    side: const BorderSide(color: AppColors.primary),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    padding: const EdgeInsets.symmetric(vertical: 8),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.replay, size: 16),
                  label: Text(
                    'Retry Test',
                    style: GoogleFonts.poppins(fontSize: 12),
                  ),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    padding: const EdgeInsets.symmetric(vertical: 8),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
