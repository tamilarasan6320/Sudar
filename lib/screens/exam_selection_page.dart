import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import 'home_page.dart';

class ExamSelectionPage extends StatefulWidget {
  const ExamSelectionPage({Key? key}) : super(key: key);

  @override
  State<ExamSelectionPage> createState() => _ExamSelectionPageState();
}

class _ExamSelectionPageState extends State<ExamSelectionPage> {
  String? _selectedExam;
  bool _isLoading = true;
  List<Map<String, dynamic>> _examCategories = [];

  @override
  void initState() {
    super.initState();
    _loadExamCategories();
  }

  Future<void> _loadExamCategories() async {
    setState(() => _isLoading = true);
    
    try {
      final response = await ApiService.getExamCategories();
      
      if (response['success'] == true) {
        final categories = List<Map<String, dynamic>>.from(response['categories'] ?? []);
        
        if (mounted) {
          setState(() {
            _examCategories = categories;
            // Auto-select TNPSC Group 4 if available
            final group4 = categories.firstWhere(
              (cat) => cat['name'].toString().contains('Group 4'),
              orElse: () => categories.isNotEmpty ? categories.first : {},
            );
            _selectedExam = group4.isNotEmpty ? group4['name'] : null;
            _isLoading = false;
          });
        }
      } else {
        if (mounted) {
          setState(() => _isLoading = false);
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  String _getIconText(String examName) {
    if (examName.contains('Group 1')) return '1';
    if (examName.contains('Group 2A')) return '2A';
    if (examName.contains('Group 2')) return '2';
    if (examName.contains('Group 4')) return '4';
    if (examName.contains('TNUSRB')) return 'PO';
    if (examName.contains('VAO')) return 'VAO';
    return examName.substring(0, 1).toUpperCase();
  }

  void _handleContinue() async {
    if (_selectedExam != null) {
      // Find the selected exam's ID
      final selectedExamData = _examCategories.firstWhere(
        (exam) => exam['name'] == _selectedExam,
        orElse: () => {},
      );
      
      // Save selected exam to SharedPreferences
      if (selectedExamData.isNotEmpty) {
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('selectedExam', _selectedExam!);
        await prefs.setInt('selectedExamId', selectedExamData['id']);
      }
      
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(
          builder: (context) => HomePage(selectedExam: _selectedExam),
        ),
        (route) => false,
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Please select an exam to continue',
            style: GoogleFonts.poppins(),
          ),
          backgroundColor: AppColors.error,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final isMobile = size.width < 600;

    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      appBar: AppBar(
        title: Text(
          'Select Exam',
          style: GoogleFonts.poppins(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
        centerTitle: false,
        elevation: 0,
        backgroundColor: Colors.white,
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: AppColors.primary),
            )
          : _examCategories.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.inbox_outlined, size: 64, color: AppColors.textLight),
                      const SizedBox(height: 16),
                      Text(
                        'No exam categories available',
                        style: GoogleFonts.poppins(
                          fontSize: 16,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                )
              : Column(
                  children: [
                    Expanded(
                      child: SingleChildScrollView(
                        child: Container(
                          constraints: const BoxConstraints(maxWidth: 600),
                          margin: EdgeInsets.symmetric(
                            horizontal: isMobile ? 16 : 40,
                            vertical: 24,
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              // Header Section
                              _buildHeader(),

                              const SizedBox(height: 32),

                              // Exam List
                              _buildExamList(),
                            ],
                          ),
                        ),
                      ),
                    ),

                    // Continue Button (Fixed at bottom)
                    _buildBottomButton(),
                  ],
                ),
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Select Your TNPSC Exam',
          style: GoogleFonts.poppins(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Choose the exam you are preparing for. Group 4 is our primary focus with comprehensive content.',
          style: GoogleFonts.poppins(
            fontSize: 14,
            color: AppColors.textSecondary,
            height: 1.5,
          ),
        ),
      ],
    );
  }

  Widget _buildExamList() {
    return Column(
      children: _examCategories.map((exam) {
        final title = exam['name'] ?? 'Unknown Exam';
        final description = exam['description'] ?? '';
        final isSelected = _selectedExam == title;
        
        return _buildExamCard(
          title: title,
          subtitle: description,
          icon: _getIconText(title),
          isSelected: isSelected,
          onTap: () {
            setState(() {
              _selectedExam = title;
            });
          },
        );
      }).toList(),
    );
  }

  Widget _buildExamCard({
    required String title,
    required String subtitle,
    required String icon,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: isSelected ? AppColors.primary : AppColors.border,
              width: isSelected ? 2 : 1,
            ),
            boxShadow: [
              BoxShadow(
                color: isSelected
                    ? AppColors.primary.withOpacity(0.1)
                    : Colors.black.withOpacity(0.03),
                blurRadius: 8,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          child: Row(
            children: [
              // Icon
              Container(
                width: 56,
                height: 56,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: isSelected
                        ? [AppColors.primary, AppColors.primaryLight]
                        : [
                            AppColors.border.withOpacity(0.3),
                            AppColors.border.withOpacity(0.5),
                          ],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Center(
                  child: Text(
                    icon,
                    style: GoogleFonts.poppins(
                      fontSize: icon.length > 2 ? 14 : 20,
                      fontWeight: FontWeight.bold,
                      color: isSelected ? Colors.white : AppColors.textSecondary,
                    ),
                  ),
                ),
              ),

              const SizedBox(width: 16),

              // Text Content
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
                    const SizedBox(height: 4),
                    Text(
                      subtitle,
                      style: GoogleFonts.poppins(
                        fontSize: 13,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),

              // Radio Button
              Container(
                width: 24,
                height: 24,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: isSelected ? AppColors.primary : AppColors.border,
                    width: 2,
                  ),
                ),
                child: isSelected
                    ? Center(
                        child: Container(
                          width: 12,
                          height: 12,
                          decoration: const BoxDecoration(
                            shape: BoxShape.circle,
                            color: AppColors.primary,
                          ),
                        ),
                      )
                    : null,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBottomButton() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: SafeArea(
        child: SizedBox(
          width: double.infinity,
          child: ElevatedButton(
            onPressed: _handleContinue,
            style: ElevatedButton.styleFrom(
              backgroundColor: _selectedExam != null
                  ? AppColors.primary
                  : const Color(0xFFBDBDBD),
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 18),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
              elevation: 0,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'CONTINUE',
                  style: GoogleFonts.poppins(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.5,
                  ),
                ),
                const SizedBox(width: 8),
                const Icon(Icons.arrow_forward, size: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

