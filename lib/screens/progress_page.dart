import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/app_colors.dart';

class ProgressPage extends StatelessWidget {
  const ProgressPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 1,
        title: Text(
          'Your Progress',
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: AppColors.textPrimary,
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
                        child: _buildStatItem(
                          icon: Icons.assignment_turned_in_rounded,
                          label: 'Tests Taken',
                          value: '24',
                          isWhite: true,
                        ),
                      ),
                      Container(width: 1, height: 40, color: Colors.white.withOpacity(0.3)),
                      Expanded(
                        child: _buildStatItem(
                          icon: Icons.trending_up_rounded,
                          label: 'Avg Score',
                          value: '78%',
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
                        child: _buildStatItem(
                          icon: Icons.emoji_events_rounded,
                          label: 'Rank',
                          value: '#142',
                          isWhite: true,
                        ),
                      ),
                      Container(width: 1, height: 40, color: Colors.white.withOpacity(0.3)),
                      Expanded(
                        child: _buildStatItem(
                          icon: Icons.local_fire_department_rounded,
                          label: 'Streak',
                          value: '7 days',
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
                color: AppColors.textPrimary,
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
              child: Column(
                children: [
                  _buildPerformanceBar('Previous Year Papers', 0.85, AppColors.primary),
                  const SizedBox(height: 16),
                  _buildPerformanceBar('Tamil Language', 0.72, const Color(0xFF9C27B0)),
                  const SizedBox(height: 16),
                  _buildPerformanceBar('General Science', 0.68, const Color(0xFF00BCD4)),
                  const SizedBox(height: 16),
                  _buildPerformanceBar('Social Science', 0.75, const Color(0xFF4CAF50)),
                  const SizedBox(height: 16),
                  _buildPerformanceBar('Aptitude', 0.80, AppColors.secondary),
                  const SizedBox(height: 16),
                  _buildPerformanceBar('Current Affairs', 0.65, const Color(0xFFFF5722)),
                ],
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Recent Activity
            Text(
              'Recent Activity',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: AppColors.textPrimary,
              ),
            ),
            
            const SizedBox(height: 12),
            
            _buildActivityCard(
              title: 'TNPSC Group 1 - Mock Test',
              date: 'Today, 10:30 AM',
              score: 85,
              questions: 50,
              icon: Icons.check_circle,
              iconColor: AppColors.success,
            ),
            
            const SizedBox(height: 12),
            
            _buildActivityCard(
              title: 'Tamil Grammar Practice',
              date: 'Yesterday, 3:45 PM',
              score: 72,
              questions: 30,
              icon: Icons.check_circle,
              iconColor: AppColors.success,
            ),
            
            const SizedBox(height: 12),
            
            _buildActivityCard(
              title: 'General Science Quiz',
              date: '2 days ago',
              score: 68,
              questions: 40,
              icon: Icons.check_circle,
              iconColor: AppColors.warning,
            ),
            
            const SizedBox(height: 12),
            
            _buildActivityCard(
              title: 'Current Affairs - 2024',
              date: '3 days ago',
              score: 78,
              questions: 25,
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
                color: AppColors.textPrimary,
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
                            color: AppColors.textPrimary,
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
                            color: AppColors.textPrimary,
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

  Widget _buildStatItem({
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

  Widget _buildPerformanceBar(String subject, double progress, Color color) {
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
                color: AppColors.textPrimary,
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

  Widget _buildActivityCard({
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
                    color: AppColors.textPrimary,
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

