import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/app_colors.dart';
import 'test_page.dart';

class TestsPage extends StatefulWidget {
  const TestsPage({Key? key}) : super(key: key);

  @override
  State<TestsPage> createState() => _TestsPageState();
}

class _TestsPageState extends State<TestsPage> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  
  final List<String> _categories = [
    'All',
    'Previous Year',
    'Tamil',
    'Science',
    'Social',
    'Aptitude',
    'Current Affairs',
  ];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: _categories.length, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
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
              tabs: _categories.map((category) => Tab(text: category)).toList(),
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

  Widget _buildTestList(String category) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _buildTestCard(
          title: 'TNPSC Group 1 - Full Mock Test',
          questions: 200,
          duration: '180 min',
          difficulty: 'Hard',
          difficultyColor: AppColors.error,
          category: 'Previous Year Papers',
        ),
        const SizedBox(height: 12),
        _buildTestCard(
          title: 'Tamil Grammar - Practice Test',
          questions: 50,
          duration: '45 min',
          difficulty: 'Medium',
          difficultyColor: AppColors.warning,
          category: 'Tamil Language',
        ),
        const SizedBox(height: 12),
        _buildTestCard(
          title: 'General Science - Mock Test',
          questions: 75,
          duration: '60 min',
          difficulty: 'Medium',
          difficultyColor: AppColors.warning,
          category: 'General Science',
        ),
        const SizedBox(height: 12),
        _buildTestCard(
          title: 'Indian History - Quick Test',
          questions: 30,
          duration: '30 min',
          difficulty: 'Easy',
          difficultyColor: AppColors.success,
          category: 'Social Science',
        ),
        const SizedBox(height: 12),
        _buildTestCard(
          title: 'Aptitude & Reasoning - Practice',
          questions: 40,
          duration: '40 min',
          difficulty: 'Medium',
          difficultyColor: AppColors.warning,
          category: 'Aptitude',
        ),
        const SizedBox(height: 12),
        _buildTestCard(
          title: 'Current Affairs - 2024',
          questions: 25,
          duration: '25 min',
          difficulty: 'Easy',
          difficultyColor: AppColors.success,
          category: 'Current Affairs',
        ),
        const SizedBox(height: 12),
        _buildTestCard(
          title: 'TNPSC Group 2 - Full Mock Test',
          questions: 200,
          duration: '180 min',
          difficulty: 'Hard',
          difficultyColor: AppColors.error,
          category: 'Previous Year Papers',
        ),
        const SizedBox(height: 12),
        _buildTestCard(
          title: 'Tamil Literature - Advanced',
          questions: 60,
          duration: '50 min',
          difficulty: 'Hard',
          difficultyColor: AppColors.error,
          category: 'Tamil Language',
        ),
      ],
    );
  }

  Widget _buildTestCard({
    required String title,
    required int questions,
    required String duration,
    required String difficulty,
    required Color difficultyColor,
    required String category,
  }) {
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
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => TestPage(
                  testTitle: title,
                  category: category,
                ),
              ),
            );
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
                              color: AppColors.textPrimary,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: difficultyColor.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(
                              difficulty,
                              style: GoogleFonts.poppins(
                                fontSize: 11,
                                fontWeight: FontWeight.w600,
                                color: difficultyColor,
                              ),
                            ),
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
                      '$questions Questions',
                      style: GoogleFonts.poppins(
                        fontSize: 13,
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Icon(Icons.timer_outlined, size: 16, color: AppColors.textSecondary),
                    const SizedBox(width: 6),
                    Text(
                      duration,
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

