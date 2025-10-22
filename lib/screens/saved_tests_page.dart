import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';

class SavedTestsPage extends StatelessWidget {
  const SavedTestsPage({Key? key}) : super(key: key);

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
          'Saved Tests',
          style: GoogleFonts.poppins(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: ThemeHelper.textPrimary(context),
          ),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Summary Card
            _buildSummaryCard(),
            
            const SizedBox(height: 24),
            
            // Filter Tabs
            _buildFilterTabs(context),
            
            const SizedBox(height: 20),
            
            // Saved Tests List
            Text(
              'Your Saved Tests',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: ThemeHelper.textPrimary(context),
              ),
            ),
            
            const SizedBox(height: 12),
            
            _buildSavedTestCard(
              context,
              'TNPSC Group 1 - Mock Test 15',
              'Tamil, History, Geography',
              '50 Questions',
              '60 Minutes',
              'Medium',
              Colors.orange,
              true,
            ),
            const SizedBox(height: 12),
            _buildSavedTestCard(
              context,
              'TNPSC Group 2 - Practice Set 8',
              'General Science, Aptitude',
              '40 Questions',
              '45 Minutes',
              'Easy',
              Colors.green,
              false,
            ),
            const SizedBox(height: 12),
            _buildSavedTestCard(
              context,
              'TNPSC Group 4 - Full Mock Test',
              'All Subjects',
              '100 Questions',
              '120 Minutes',
              'Hard',
              Colors.red,
              false,
            ),
            const SizedBox(height: 12),
            _buildSavedTestCard(
              context,
              'Current Affairs - Weekly Test',
              'Current Affairs, GK',
              '30 Questions',
              '30 Minutes',
              'Easy',
              Colors.green,
              true,
            ),
            const SizedBox(height: 12),
            _buildSavedTestCard(
              context,
              'Tamil Language - Grammar Test',
              'Tamil Grammar, Literature',
              '35 Questions',
              '40 Minutes',
              'Medium',
              Colors.orange,
              false,
            ),
            const SizedBox(height: 12),
            _buildSavedTestCard(
              context,
              'Aptitude & Reasoning - Set 3',
              'Logical Reasoning, Quantitative',
              '45 Questions',
              '50 Minutes',
              'Medium',
              Colors.orange,
              true,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSummaryCard() {
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
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _buildStatItem('12', 'Saved Tests', Icons.bookmark),
          Container(width: 1, height: 40, color: Colors.white24),
          _buildStatItem('8', 'Not Started', Icons.pending_outlined),
          Container(width: 1, height: 40, color: Colors.white24),
          _buildStatItem('4', 'In Progress', Icons.play_circle_outline),
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
            fontSize: 11,
            color: Colors.white.withOpacity(0.9),
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildFilterTabs(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: Row(
        children: [
          _buildFilterButton(context, 'All', true),
          _buildFilterButton(context, 'Not Started', false),
          _buildFilterButton(context, 'In Progress', false),
        ],
      ),
    );
  }

  Widget _buildFilterButton(BuildContext context, String label, bool isSelected) {
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
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: isSelected ? Colors.white : ThemeHelper.textSecondary(context),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSavedTestCard(
    BuildContext context,
    String title,
    String subjects,
    String questions,
    String duration,
    String difficulty,
    Color difficultyColor,
    bool isNew,
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
            children: [
              Expanded(
                child: Text(
                  title,
                  style: GoogleFonts.poppins(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: ThemeHelper.textPrimary(context),
                  ),
                ),
              ),
              if (isNew)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.success.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    'NEW',
                    style: GoogleFonts.poppins(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: AppColors.success,
                    ),
                  ),
                ),
              const SizedBox(width: 8),
              Icon(
                Icons.bookmark,
                color: AppColors.primary,
                size: 20,
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            subjects,
            style: GoogleFonts.poppins(
              fontSize: 13,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              _buildInfoChip(Icons.quiz_outlined, questions, AppColors.primary),
              const SizedBox(width: 8),
              _buildInfoChip(Icons.access_time, duration, AppColors.secondary),
              const SizedBox(width: 8),
              _buildInfoChip(Icons.signal_cellular_alt, difficulty, difficultyColor),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.bookmark_remove, size: 18),
                  label: Text(
                    'Remove',
                    style: GoogleFonts.poppins(fontSize: 13),
                  ),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.error,
                    side: const BorderSide(color: AppColors.error),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.play_arrow, size: 18),
                  label: Text(
                    'Start Test',
                    style: GoogleFonts.poppins(fontSize: 13),
                  ),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildInfoChip(IconData icon, String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 4),
          Text(
            label,
            style: GoogleFonts.poppins(
              fontSize: 11,
              fontWeight: FontWeight.w500,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}

