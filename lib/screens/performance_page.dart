import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/app_colors.dart';

class PerformancePage extends StatelessWidget {
  const PerformancePage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 1,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.textPrimary),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Performance Analytics',
          style: GoogleFonts.poppins(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
      ),
      body: SingleChildScrollView(
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
                color: AppColors.textPrimary,
              ),
            ),
            
            const SizedBox(height: 12),
            
            _buildSubjectPerformance('Tamil Language', 0.85, 42, 50, AppColors.primary),
            const SizedBox(height: 12),
            _buildSubjectPerformance('General Science', 0.72, 36, 50, const Color(0xFF00BCD4)),
            const SizedBox(height: 12),
            _buildSubjectPerformance('Social Science', 0.68, 34, 50, const Color(0xFF4CAF50)),
            const SizedBox(height: 12),
            _buildSubjectPerformance('Aptitude', 0.78, 39, 50, AppColors.secondary),
            const SizedBox(height: 12),
            _buildSubjectPerformance('Current Affairs', 0.60, 30, 50, const Color(0xFFFF5722)),
            
            const SizedBox(height: 24),
            
            // Strengths & Weaknesses
            Text(
              'Analysis',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: AppColors.textPrimary,
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
                color: AppColors.textPrimary,
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
            '78.5%',
            style: GoogleFonts.poppins(
              fontSize: 48,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '↑ 5.2% from last week',
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
              _buildStatItem('Tests', '24', Icons.quiz_outlined),
              _buildStatItem('Hours', '18.5', Icons.access_time),
              _buildStatItem('Rank', '#142', Icons.emoji_events_outlined),
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
          _buildPeriodButton('7D', true),
          _buildPeriodButton('1M', false),
          _buildPeriodButton('3M', false),
          _buildPeriodButton('1Y', false),
        ],
      ),
    );
  }

  Widget _buildPeriodButton(String label, bool isSelected) {
    return Expanded(
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
              color: isSelected ? Colors.white : AppColors.textSecondary,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPerformanceChart() {
    return Container(
      height: 200,
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
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Score Trend',
            style: GoogleFonts.poppins(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 20),
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                _buildChartBar('Mon', 0.65),
                _buildChartBar('Tue', 0.72),
                _buildChartBar('Wed', 0.68),
                _buildChartBar('Thu', 0.80),
                _buildChartBar('Fri', 0.75),
                _buildChartBar('Sat', 0.85),
                _buildChartBar('Sun', 0.78),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildChartBar(String day, double value) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Text(
          '${(value * 100).toInt()}',
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

  Widget _buildSubjectPerformance(String subject, double progress, int correct, int total, Color color) {
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
                  color: AppColors.textPrimary,
                ),
              ),
              Text(
                '${(progress * 100).toInt()}%',
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
                    value: progress,
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
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
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
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 12),
          _buildTag('Tamil', AppColors.success),
          const SizedBox(height: 6),
          _buildTag('Aptitude', AppColors.success),
          const SizedBox(height: 6),
          _buildTag('History', AppColors.success),
        ],
      ),
    );
  }

  Widget _buildWeaknessesCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
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
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 12),
          _buildTag('Current Affairs', AppColors.warning),
          const SizedBox(height: 6),
          _buildTag('Geography', AppColors.warning),
          const SizedBox(height: 6),
          _buildTag('Economics', AppColors.warning),
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
      child: Column(
        children: [
          _buildTimelineItem('Completed Tamil Test 12', '85% Score', '2 hours ago', AppColors.success),
          const SizedBox(height: 16),
          _buildTimelineItem('Completed Science Test 8', '72% Score', '5 hours ago', AppColors.success),
          const SizedBox(height: 16),
          _buildTimelineItem('Completed Aptitude Test 5', '68% Score', 'Yesterday', AppColors.warning),
          const SizedBox(height: 16),
          _buildTimelineItem('Started Current Affairs', 'In Progress', '2 days ago', AppColors.primary),
        ],
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
                  color: AppColors.textPrimary,
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

