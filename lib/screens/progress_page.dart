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
  List<Map<String, dynamic>> _testHistory = [];

  @override
  void initState() {
    super.initState();
    _loadProgressData();
  }

  Future<void> _loadProgressData() async {
    setState(() => _isLoading = true);
    
    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      
      if (userId != null) {
        final response = await ApiService.getTestHistory(userId: userId);
        
        if (response['success'] == true) {
          final history = List<Map<String, dynamic>>.from(response['history'] ?? []);
          
          double totalScore = 0;
          for (var test in history) {
            totalScore += (test['percentage'] ?? 0).toDouble();
          }
          
          if (mounted) {
            setState(() {
              _testHistory = history;
              _testsTaken = history.length;
              _avgScore = history.isNotEmpty ? totalScore / history.length : 0;
              _isLoading = false;
            });
          }
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
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
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
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
                          value: _isLoading ? '...' : '--',
                          isWhite: true,
                        ),
                      ),
                      Container(width: 1, height: 40, color: Colors.white.withOpacity(0.3)),
                      Expanded(
                        child: _buildStatItem(context,
                          icon: Icons.local_fire_department_rounded,
                          label: 'Streak',
                          value: _isLoading ? '...' : '0 days',
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
              padding: const EdgeInsets.all(20),
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
                : _testHistory.isEmpty
                  ? Center(
                      child: Padding(
                        padding: const EdgeInsets.all(20),
                        child: Text(
                          'No performance data yet',
                          style: GoogleFonts.poppins(color: Colors.grey[600]),
                        ),
                      ),
                    )
                  : Column(
                      children: [
                        Text(
                          'Performance data based on ${_testHistory.length} tests',
                          style: GoogleFonts.poppins(
                            fontSize: 14,
                            color: Colors.grey[600],
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
              ..._testHistory.map((test) {
                final percentage = (test['percentage'] ?? 0).toDouble();
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: _buildActivityCard(context,
                    title: test['session_name'] ?? 'Test',
                    date: _formatDate(test['completed_at']),
                    score: percentage.toInt(),
                    questions: test['total_questions'] ?? 0,
                    icon: percentage >= 50 ? Icons.check_circle : Icons.cancel,
                    iconColor: percentage >= 75 ? AppColors.success : percentage >= 50 ? AppColors.warning : AppColors.error,
                  ),
                );
              }).toList(),
            
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
                        _buildTag('Aptitude', AppColors.success),
                        const SizedBox(height: 6),
                        _buildTag('History', AppColors.success),
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
                        _buildTag('Current Affairs', AppColors.error),
                        const SizedBox(height: 6),
                        _buildTag('Science', AppColors.error),
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
}

