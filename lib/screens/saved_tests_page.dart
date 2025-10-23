import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import 'test_page.dart';

class SavedTestsPage extends StatefulWidget {
  const SavedTestsPage({Key? key}) : super(key: key);

  @override
  State<SavedTestsPage> createState() => _SavedTestsPageState();
}

class _SavedTestsPageState extends State<SavedTestsPage> {
  bool _isLoading = true;
  List<Map<String, dynamic>> _testSessions = [];

  @override
  void initState() {
    super.initState();
    _loadTestSessions();
  }

  Future<void> _loadTestSessions() async {
    setState(() => _isLoading = true);
    
    try {
      final response = await ApiService.getQuestionSessions();
      
      if (response['success'] == true) {
        final sessions = List<Map<String, dynamic>>.from(response['sessions'] ?? []);
        
        if (mounted) {
          setState(() {
            _testSessions = sessions;
            _isLoading = false;
          });
        }
      } else {
        if (mounted) {
          setState(() => _isLoading = false);
        }
      }
    } catch (e) {
      print('Error loading test sessions: $e');
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  String _getDifficulty(int? questionCount) {
    if (questionCount == null) return 'Medium';
    if (questionCount <= 20) return 'Easy';
    if (questionCount <= 50) return 'Medium';
    return 'Hard';
  }

  Color _getDifficultyColor(String difficulty) {
    switch (difficulty) {
      case 'Easy':
        return Colors.green;
      case 'Hard':
        return Colors.red;
      default:
        return Colors.orange;
    }
  }

  @override
  Widget build(BuildContext context) {
    final totalSessions = _testSessions.length;
    final notStarted = _testSessions.where((s) => (s['is_active'] ?? 1) == 1).length;
    final inProgress = 0; // We don't track progress yet
    
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
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Summary Card
                  _buildSummaryCard(totalSessions, notStarted, inProgress),
                  
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
                  
                  // Dynamic test sessions from API
                  if (_testSessions.isEmpty)
                    Container(
                      padding: const EdgeInsets.all(40),
                      decoration: BoxDecoration(
                        color: ThemeHelper.cardColor(context),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Center(
                        child: Column(
                          children: [
                            Icon(Icons.bookmark_outline, size: 48, color: Colors.grey[400]),
                            const SizedBox(height: 16),
                            Text(
                              'No tests available yet',
                              style: GoogleFonts.poppins(
                                fontSize: 16,
                                color: Colors.grey[600],
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'Check back later for new tests',
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
                    ..._testSessions.asMap().entries.map((entry) {
                      final index = entry.key;
                      final session = entry.value;
                      final questionCount = session['question_count'] ?? 0;
                      final difficulty = _getDifficulty(questionCount);
                      final difficultyColor = _getDifficultyColor(difficulty);
                      final isNew = index < 2; // Mark first 2 as new
                      
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: _buildSavedTestCard(
                          context,
                          session['name'] ?? 'Test',
                          session['description'] ?? 'Test Session',
                          '$questionCount Questions',
                          '${(questionCount * 1.2).toInt()} Minutes', // Estimate 1.2 min per question
                          difficulty,
                          difficultyColor,
                          isNew,
                          session['id'],
                        ),
                      );
                    }).toList(),
                ],
              ),
            ),
    );
  }

  Widget _buildSummaryCard(int total, int notStarted, int inProgress) {
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
          _buildStatItem('$total', 'Saved Tests', Icons.bookmark),
          Container(width: 1, height: 40, color: Colors.white24),
          _buildStatItem('$notStarted', 'Not Started', Icons.pending_outlined),
          Container(width: 1, height: 40, color: Colors.white24),
          _buildStatItem('$inProgress', 'In Progress', Icons.play_circle_outline),
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
    int sessionId,
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
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
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
                  onPressed: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Remove feature coming soon!')),
                    );
                  },
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
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => TestPage(
                          testTitle: title,
                          category: subjects,
                          sessionId: sessionId,
                        ),
                      ),
                    );
                  },
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
