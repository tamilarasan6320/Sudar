import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import 'test_page.dart';

class TestsPage extends StatefulWidget {
  const TestsPage({Key? key}) : super(key: key);

  @override
  State<TestsPage> createState() => _TestsPageState();
}

class _TestsPageState extends State<TestsPage> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  bool _isLoading = true;
  List<Map<String, dynamic>> _allSessions = [];
  List<Map<String, dynamic>> _categories = [];
  
  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    
    try {
      // Load test categories
      final categoriesResponse = await ApiService.getTestCategories();
      
      // Load all question sessions
      final sessionsResponse = await ApiService.getQuestionSessions();
      
      if (mounted) {
        setState(() {
          if (categoriesResponse['success'] == true) {
            _categories = List<Map<String, dynamic>>.from(categoriesResponse['categories'] ?? []);
            // Add "All" category at the beginning
            _categories.insert(0, {'id': 0, 'name': 'All'});
          } else {
            _categories = [{'id': 0, 'name': 'All'}];
          }
          
          if (sessionsResponse['success'] == true) {
            _allSessions = List<Map<String, dynamic>>.from(sessionsResponse['sessions'] ?? []);
          }
          
          _isLoading = false;
          // Initialize tab controller after categories are loaded
          _tabController = TabController(length: _categories.length, vsync: this);
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _categories = [{'id': 0, 'name': 'All'}];
          _tabController = TabController(length: 1, vsync: this);
        });
      }
    }
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return Scaffold(
        backgroundColor: AppColors.background,
        appBar: AppBar(
          backgroundColor: Colors.white,
          elevation: 1,
          title: Text(
            'All Tests',
            style: GoogleFonts.poppins(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimary,
            ),
          ),
        ),
        body: const Center(
          child: CircularProgressIndicator(color: AppColors.primary),
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 1,
        title: Text(
          'All Tests',
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: AppColors.textPrimary,
          ),
        ),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(50),
          child: Container(
            color: Colors.white,
            child: TabBar(
              controller: _tabController,
              isScrollable: true,
              indicatorColor: AppColors.primary,
              indicatorWeight: 3,
              labelColor: AppColors.primary,
              unselectedLabelColor: AppColors.textSecondary,
              labelStyle: GoogleFonts.poppins(
                fontWeight: FontWeight.w600,
                fontSize: 14,
              ),
              unselectedLabelStyle: GoogleFonts.poppins(
                fontWeight: FontWeight.w500,
                fontSize: 14,
              ),
              tabs: _categories.map((category) => Tab(text: category['name'])).toList(),
            ),
          ),
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: _categories.map((category) => _buildTestList(category)).toList(),
      ),
    );
  }

  Widget _buildTestList(Map<String, dynamic> category) {
    // Filter sessions by category
    List<Map<String, dynamic>> filteredSessions;
    if (category['id'] == 0) {
      // Show all sessions
      filteredSessions = _allSessions;
    } else {
      // Filter by category
      filteredSessions = _allSessions
          .where((session) => session['test_category_id'] == category['id'])
          .toList();
    }

    if (filteredSessions.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.assignment_outlined, size: 64, color: AppColors.textLight),
            const SizedBox(height: 16),
            Text(
              'No tests available',
              style: GoogleFonts.poppins(
                fontSize: 16,
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: filteredSessions.length,
      separatorBuilder: (context, index) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final session = filteredSessions[index];
        return _buildTestCard(session);
      },
    );
  }

  Widget _buildTestCard(Map<String, dynamic> session) {
    final title = session['name'] ?? 'Unnamed Test';
    final description = session['description'] ?? '';
    final totalQuestions = session['actual_question_count'] ?? session['total_questions'] ?? 0;
    final duration = session['duration'] ?? 60;
    final difficulty = (session['difficulty'] ?? 'medium').toString();
    final categoryName = session['category_name'] ?? '';
    final sessionId = session['id'];
    
    // Determine difficulty color
    Color difficultyColor;
    String difficultyText;
    switch (difficulty.toLowerCase()) {
      case 'easy':
        difficultyColor = AppColors.success;
        difficultyText = 'Easy';
        break;
      case 'hard':
        difficultyColor = AppColors.error;
        difficultyText = 'Hard';
        break;
      case 'medium':
      default:
        difficultyColor = AppColors.warning;
        difficultyText = 'Medium';
        break;
    }

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
            if (totalQuestions > 0) {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => TestPage(
                    testTitle: title,
                    category: categoryName,
                    sessionId: sessionId,
                  ),
                ),
              );
            } else {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(
                    'This test has no questions yet',
                    style: GoogleFonts.poppins(),
                  ),
                  backgroundColor: AppColors.error,
                ),
              );
            }
          },
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
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
                          if (description.isNotEmpty) ...[
                            const SizedBox(height: 4),
                            Text(
                              description,
                              style: GoogleFonts.poppins(
                                fontSize: 13,
                                color: AppColors.textSecondary,
                              ),
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                          const SizedBox(height: 8),
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                decoration: BoxDecoration(
                                  color: difficultyColor.withOpacity(0.1),
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: Text(
                                  difficultyText,
                                  style: GoogleFonts.poppins(
                                    fontSize: 11,
                                    fontWeight: FontWeight.w600,
                                    color: difficultyColor,
                                  ),
                                ),
                              ),
                              if (categoryName.isNotEmpty) ...[
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: AppColors.primary.withOpacity(0.1),
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    categoryName,
                                    style: GoogleFonts.poppins(
                                      fontSize: 11,
                                      fontWeight: FontWeight.w600,
                                      color: AppColors.primary,
                                    ),
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: AppColors.primary.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(
                        Icons.assignment_outlined,
                        color: AppColors.primary,
                        size: 24,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                const Divider(height: 1, color: AppColors.borderLight),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Icon(Icons.quiz_outlined, size: 16, color: AppColors.textSecondary),
                    const SizedBox(width: 6),
                    Text(
                      '$totalQuestions Questions',
                      style: GoogleFonts.poppins(
                        fontSize: 13,
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Icon(Icons.timer_outlined, size: 16, color: AppColors.textSecondary),
                    const SizedBox(width: 6),
                    Text(
                      '$duration min',
                      style: GoogleFonts.poppins(
                        fontSize: 13,
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const Spacer(),
                    Icon(Icons.arrow_forward_ios_rounded, size: 14, color: AppColors.primary),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
