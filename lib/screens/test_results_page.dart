import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:flutter_html/flutter_html.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import 'test_page.dart';

class TestResultsPage extends StatefulWidget {
  final List<Question> questions;
  final Map<int, int> selectedAnswers;
  final int correctAnswers;
  final int totalQuestions;
  final String testTitle;
  final String category;
  
  const TestResultsPage({
    Key? key,
    required this.questions,
    required this.selectedAnswers,
    required this.correctAnswers,
    required this.totalQuestions,
    required this.testTitle,
    required this.category,
  }) : super(key: key);

  @override
  State<TestResultsPage> createState() => _TestResultsPageState();
}

class _TestResultsPageState extends State<TestResultsPage> {
  int _currentQuestionIndex = 0;

  @override
  Widget build(BuildContext context) {
    final currentQuestion = widget.questions[_currentQuestionIndex];
    final selectedAnswer = widget.selectedAnswers[_currentQuestionIndex];
    final isCorrect = selectedAnswer == currentQuestion.correctAnswer;
    
    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      appBar: AppBar(
        backgroundColor: ThemeHelper.cardColor(context),
        elevation: 1,
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: ThemeHelper.textPrimary(context)),
          onPressed: () => Navigator.pop(context),
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Test Results',
              style: GoogleFonts.poppins(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: ThemeHelper.textPrimary(context),
              ),
            ),
            Text(
              widget.testTitle,
              style: GoogleFonts.poppins(
                fontSize: 12,
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
        actions: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            margin: const EdgeInsets.only(right: 16),
            decoration: BoxDecoration(
              color: AppColors.success.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.check_circle,
                  size: 18,
                  color: AppColors.success,
                ),
                const SizedBox(width: 6),
                Text(
                  '${widget.correctAnswers}/${widget.totalQuestions}',
                  style: GoogleFonts.poppins(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppColors.success,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          // Progress Bar
          LinearProgressIndicator(
            value: (_currentQuestionIndex + 1) / widget.questions.length,
            backgroundColor: AppColors.border,
            valueColor: const AlwaysStoppedAnimation<Color>(AppColors.primary),
            minHeight: 4,
          ),
          
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Question Number and Status
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: AppColors.primary,
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          'Question ${_currentQuestionIndex + 1}/${widget.questions.length}',
                          style: GoogleFonts.poppins(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: Colors.white,
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: isCorrect ? AppColors.success : AppColors.error,
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Row(
                          children: [
                            Icon(
                              isCorrect ? Icons.check_circle : Icons.cancel,
                              size: 16,
                              color: Colors.white,
                            ),
                            const SizedBox(width: 4),
                            Text(
                              isCorrect ? 'Correct' : 'Incorrect',
                              style: GoogleFonts.poppins(
                                fontSize: 12,
                                fontWeight: FontWeight.w600,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  
                  const SizedBox(height: 20),
                  
                  // Question Text
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // English Version
                        if (currentQuestion.hasEnglish && currentQuestion.textEn != null) ...[
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: Colors.blue.shade50,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Text(
                              'English',
                              style: GoogleFonts.poppins(
                                fontSize: 10,
                                fontWeight: FontWeight.w600,
                                color: Colors.blue.shade700,
                              ),
                            ),
                          ),
                          const SizedBox(height: 8),
                          Html(
                            data: currentQuestion.textEn!,
                            style: {
                              "body": Style(
                                margin: Margins.zero,
                                padding: HtmlPaddings.zero,
                                fontSize: FontSize(16),
                                fontWeight: FontWeight.w500,
                                color: ThemeHelper.textPrimary(context),
                                lineHeight: const LineHeight(1.5),
                                fontFamily: GoogleFonts.poppins().fontFamily,
                              ),
                              "p": Style(
                                margin: Margins.zero,
                                padding: HtmlPaddings.zero,
                              ),
                              "img": Style(
                                width: Width(100, Unit.percent),
                                height: Height.auto(),
                                margin: Margins.only(top: 8, bottom: 8),
                                display: Display.block,
                              ),
                              "br": Style(
                                margin: Margins.only(bottom: 4),
                              ),
                            },
                            extensions: [
                              TagExtension(
                                tagsToExtend: {"img"},
                                builder: (extensionContext) {
                                  final src = extensionContext.attributes['src'] ?? '';
                                  print('🖼️ RENDERING IMAGE: $src');
                                  
                                  if (src.isEmpty) {
                                    return const SizedBox.shrink();
                                  }
                                  
                                  return Padding(
                                    padding: const EdgeInsets.symmetric(vertical: 8.0),
                                    child: Image.network(
                                      src,
                                      loadingBuilder: (context, child, loadingProgress) {
                                        if (loadingProgress == null) {
                                          print('✅ Image loaded: $src');
                                          return child;
                                        }
                                        return const Center(child: CircularProgressIndicator());
                                      },
                                      errorBuilder: (context, error, stackTrace) {
                                        print('❌ IMAGE ERROR: $error for $src');
                                        return Container(
                                          padding: const EdgeInsets.all(16),
                                          color: Colors.red.shade50,
                                          child: Text('❌ Image failed: $src\nError: $error'),
                                        );
                                      },
                                    ),
                                  );
                                },
                              ),
                            ],
                          ),
                          if (currentQuestion.hasTamil) ...[
                            const SizedBox(height: 20),
                            Divider(
                              color: Colors.grey.shade300,
                              thickness: 1,
                              height: 1,
                            ),
                            const SizedBox(height: 20),
                          ],
                        ],
                        
                        // Tamil Version
                        if (currentQuestion.hasTamil && currentQuestion.textTa != null) ...[
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: Colors.orange.shade50,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Text(
                              'தமிழ்',
                              style: GoogleFonts.poppins(
                                fontSize: 10,
                                fontWeight: FontWeight.w600,
                                color: Colors.orange.shade700,
                              ),
                            ),
                          ),
                          const SizedBox(height: 8),
                          Html(
                            data: currentQuestion.textTa!,
                            style: {
                              "body": Style(
                                margin: Margins.zero,
                                padding: HtmlPaddings.zero,
                                fontSize: FontSize(16),
                                fontWeight: FontWeight.w500,
                                color: ThemeHelper.textPrimary(context),
                                lineHeight: const LineHeight(1.5),
                                fontFamily: GoogleFonts.poppins().fontFamily,
                              ),
                              "p": Style(
                                margin: Margins.zero,
                                padding: HtmlPaddings.zero,
                              ),
                              "img": Style(
                                width: Width(100, Unit.percent),
                                height: Height.auto(),
                                margin: Margins.only(top: 8, bottom: 8),
                                display: Display.block,
                              ),
                              "br": Style(
                                margin: Margins.only(bottom: 4),
                              ),
                            },
                            extensions: [
                              TagExtension(
                                tagsToExtend: {"img"},
                                builder: (extensionContext) {
                                  final src = extensionContext.attributes['src'] ?? '';
                                  print('🖼️ RENDERING IMAGE (TA): $src');
                                  
                                  if (src.isEmpty) {
                                    return const SizedBox.shrink();
                                  }
                                  
                                  return Padding(
                                    padding: const EdgeInsets.symmetric(vertical: 8.0),
                                    child: Image.network(
                                      src,
                                      loadingBuilder: (context, child, loadingProgress) {
                                        if (loadingProgress == null) {
                                          print('✅ Image loaded (TA): $src');
                                          return child;
                                        }
                                        return const Center(child: CircularProgressIndicator());
                                      },
                                      errorBuilder: (context, error, stackTrace) {
                                        print('❌ IMAGE ERROR (TA): $error for $src');
                                        return Container(
                                          padding: const EdgeInsets.all(16),
                                          color: Colors.red.shade50,
                                          child: Text('❌ Image failed: $src\nError: $error'),
                                        );
                                      },
                                    ),
                                  );
                                },
                              ),
                            ],
                          ),
                        ],
                        
                        // Table Display (if exists)
                        if (currentQuestion.tableData != null) ...[
                          const SizedBox(height: 16),
                          Builder(
                            builder: (ctx) => _buildTableWidget(ctx, currentQuestion.tableData!),
                          ),
                        ],
                      ],
                    ),
                  ),
                  
                  const SizedBox(height: 24),
                  
                  // Options with Answer Status
                  ...List.generate(4, (index) {
                    final isSelected = selectedAnswer == index;
                    final isCorrectAnswer = index == currentQuestion.correctAnswer;
                    
                    Color backgroundColor;
                    Color borderColor;
                    Color textColor;
                    IconData? icon;
                    Color iconColor;
                    
                    if (isCorrectAnswer) {
                      // Correct answer - always green
                      backgroundColor = AppColors.success.withOpacity(0.1);
                      borderColor = AppColors.success;
                      textColor = AppColors.success;
                      icon = Icons.check_circle;
                      iconColor = AppColors.success;
                    } else if (isSelected && !isCorrectAnswer) {
                      // Wrong selected answer - red
                      backgroundColor = AppColors.error.withOpacity(0.1);
                      borderColor = AppColors.error;
                      textColor = AppColors.error;
                      icon = Icons.cancel;
                      iconColor = AppColors.error;
                    } else {
                      // Unselected option - normal
                      backgroundColor = Colors.white;
                      borderColor = AppColors.border;
                      textColor = AppColors.textPrimary;
                      icon = null;
                      iconColor = Colors.transparent;
                    }
                    
                    return Container(
                      width: double.infinity,
                      margin: const EdgeInsets.only(bottom: 12),
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: backgroundColor,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: borderColor,
                          width: 2,
                        ),
                      ),
                      child: Row(
                        children: [
                          Container(
                            width: 28,
                            height: 28,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              border: Border.all(
                                color: borderColor,
                                width: 2,
                              ),
                              color: isCorrectAnswer || (isSelected && !isCorrectAnswer) 
                                  ? borderColor 
                                  : Colors.transparent,
                            ),
                            child: icon != null
                                ? Icon(icon, size: 18, color: Colors.white)
                                : null,
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                // Label (A, B, C, D)
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                  margin: const EdgeInsets.only(top: 2),
                                  decoration: BoxDecoration(
                                    color: isCorrectAnswer || isSelected 
                                        ? borderColor.withOpacity(0.2) 
                                        : Colors.grey.shade100,
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                  child: Text(
                                    '${String.fromCharCode(65 + index)}.',
                                    style: GoogleFonts.poppins(
                                      fontSize: 14,
                                      fontWeight: FontWeight.bold,
                                      color: textColor,
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 12),
                                
                                // Text content (English + Tamil) - with HTML stripped
                                Expanded(
                                  child: Builder(
                                    builder: (context) {
                                      // Helper function to strip HTML tags
                                      String stripHtml(String html) {
                                        if (html.isEmpty) return '';
                                        return html
                                            .replaceAll(RegExp(r'<[^>]*>'), '')
                                            .replaceAll('&nbsp;', ' ')
                                            .replaceAll('&amp;', '&')
                                            .replaceAll('&lt;', '<')
                                            .replaceAll('&gt;', '>')
                                            .trim();
                                      }
                                      
                                      // Helper to check if text contains Tamil characters
                                      bool hasTamilChars(String text) {
                                        return RegExp(r'[\u0B80-\u0BFF]').hasMatch(text);
                                      }
                                      
                                      String englishText = '';
                                      String tamilText = '';
                                      
                                      if (currentQuestion.optionsEn != null && currentQuestion.optionsEn!.length > index) {
                                        englishText = stripHtml(currentQuestion.optionsEn![index]);
                                      }
                                      if (currentQuestion.optionsTa != null && currentQuestion.optionsTa!.length > index) {
                                        tamilText = stripHtml(currentQuestion.optionsTa![index]);
                                      }
                                      
                                      // Show Tamil line only if it contains Tamil characters
                                      bool showTamilLine = tamilText.isNotEmpty && hasTamilChars(tamilText);
                                      
                                      return Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          // English text (or Tamil if no English)
                                          Text(
                                            englishText.isNotEmpty ? englishText : tamilText,
                                            style: GoogleFonts.poppins(
                                              fontSize: 15,
                                              fontWeight: FontWeight.w600,
                                              color: textColor,
                                              height: 1.4,
                                            ),
                                          ),
                                          
                                          // Tamil text (only if it contains Tamil characters)
                                          if (showTamilLine) ...[
                                            const SizedBox(height: 6),
                                            Text(
                                              tamilText,
                                              style: GoogleFonts.poppins(
                                                fontSize: 14,
                                                fontWeight: FontWeight.w500,
                                                color: textColor.withOpacity(0.8),
                                                height: 1.4,
                                              ),
                                            ),
                                          ],
                                        ],
                                      );
                                    },
                                  ),
                                ),
                              ],
                            ),
                          ),
                          if (isCorrectAnswer)
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                              decoration: BoxDecoration(
                                color: AppColors.success,
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: Text(
                                'Correct Answer',
                                style: GoogleFonts.poppins(
                                  fontSize: 10,
                                  fontWeight: FontWeight.w600,
                                  color: Colors.white,
                                ),
                              ),
                            ),
                        ],
                      ),
                    );
                  }),
                  
                  const SizedBox(height: 24),
                  
                  // Explanation Section
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: AppColors.primary.withOpacity(0.05),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: AppColors.primary.withOpacity(0.2),
                        width: 1,
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(
                              Icons.lightbulb_outline,
                              color: AppColors.primary,
                              size: 20,
                            ),
                            const SizedBox(width: 8),
                            Text(
                              'Explanation',
                              style: GoogleFonts.poppins(
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                color: AppColors.primary,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Text(
                          _getExplanation(currentQuestion),
                          style: GoogleFonts.poppins(
                            fontSize: 14,
                            color: ThemeHelper.textPrimary(context),
                            height: 1.5,
                          ),
                        ),
                      ],
                    ),
                  ),
                  
                  const SizedBox(height: 80),
                ],
              ),
            ),
          ),
          
          // Bottom Navigation
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.05),
                  blurRadius: 10,
                  offset: const Offset(0, -2),
                ),
              ],
            ),
            child: SafeArea(
              child: Row(
                children: [
                  // Previous Button
                  Expanded(
                    child: OutlinedButton(
                      onPressed: _currentQuestionIndex > 0 ? _previousQuestion : null,
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        side: const BorderSide(color: AppColors.border),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8),
                        ),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.arrow_back, size: 18),
                          const SizedBox(width: 8),
                          Text(
                            'Previous',
                            style: GoogleFonts.poppins(fontWeight: FontWeight.w600),
                          ),
                        ],
                      ),
                    ),
                  ),
                  
                  const SizedBox(width: 12),
                  
                  // Question Navigator
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    decoration: BoxDecoration(
                      color: AppColors.primary.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      '${_currentQuestionIndex + 1} / ${widget.questions.length}',
                      style: GoogleFonts.poppins(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                  
                  const SizedBox(width: 12),
                  
                  // Next Button
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _currentQuestionIndex < widget.questions.length - 1
                          ? _nextQuestion
                          : null,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8),
                        ),
                        elevation: 0,
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            'Next',
                            style: GoogleFonts.poppins(
                              fontWeight: FontWeight.w600,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(width: 8),
                          const Icon(
                            Icons.arrow_forward,
                            size: 18,
                            color: Colors.white,
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _nextQuestion() {
    if (_currentQuestionIndex < widget.questions.length - 1) {
      setState(() {
        _currentQuestionIndex++;
      });
    }
  }

  void _previousQuestion() {
    if (_currentQuestionIndex > 0) {
      setState(() {
        _currentQuestionIndex--;
      });
    }
  }

  Widget _buildTableWidget(BuildContext context, Map<String, dynamic> tableData) {
    List<dynamic> columnA = tableData['column_A'] ?? [];
    List<dynamic> columnB = tableData['column_B'] ?? [];
    
    if (columnA.isEmpty && columnB.isEmpty) {
      return const SizedBox.shrink();
    }
    
    // Determine max rows
    int maxRows = columnA.length > columnB.length ? columnA.length : columnB.length;
    
    return Container(
      margin: const EdgeInsets.only(top: 12),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300, width: 1),
      ),
      child: Table(
        border: TableBorder(
          horizontalInside: BorderSide(color: Colors.grey.shade300, width: 1),
          verticalInside: BorderSide(color: Colors.grey.shade300, width: 1),
          top: BorderSide(color: Colors.grey.shade400, width: 1),
          bottom: BorderSide(color: Colors.grey.shade400, width: 1),
          left: BorderSide(color: Colors.grey.shade400, width: 1),
          right: BorderSide(color: Colors.grey.shade400, width: 1),
        ),
        columnWidths: const {
          0: FlexColumnWidth(1),
          1: FlexColumnWidth(1),
        },
        children: [
          // Header row
          TableRow(
            decoration: BoxDecoration(
              color: Colors.grey.shade200,
            ),
            children: [
              TableCell(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Text(
                    'பட்டியல் I',
                    style: GoogleFonts.poppins(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: ThemeHelper.textPrimary(context),
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
              TableCell(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Text(
                    'பட்டியல் II',
                    style: GoogleFonts.poppins(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: ThemeHelper.textPrimary(context),
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            ],
          ),
          // Data rows
          ...List.generate(maxRows, (index) {
            String cellA = '';
            String cellB = '';
            
            if (index < columnA.length) {
              if (columnA[index] is Map) {
                final key = columnA[index]['key']?.toString() ?? '';
                final value = columnA[index]['value']?.toString() ?? '';
                // Format: (a) value or a. value
                if (key.isNotEmpty && value.isNotEmpty) {
                  cellA = '($key) $value';
                } else if (value.isNotEmpty) {
                  cellA = value;
                } else {
                  cellA = key;
                }
              } else {
                cellA = columnA[index].toString();
              }
            }
            
            if (index < columnB.length) {
              if (columnB[index] is Map) {
                final key = columnB[index]['key']?.toString() ?? '';
                final value = columnB[index]['value']?.toString() ?? '';
                // Format: 1. value
                if (key.isNotEmpty && value.isNotEmpty) {
                  cellB = '$key. $value';
                } else if (value.isNotEmpty) {
                  cellB = value;
                } else {
                  cellB = key;
                }
              } else {
                cellB = columnB[index].toString();
              }
            }
            
            return TableRow(
              children: [
                TableCell(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Text(
                      cellA,
                      style: GoogleFonts.poppins(
                        fontSize: 13,
                        color: ThemeHelper.textPrimary(context),
                      ),
                    ),
                  ),
                ),
                TableCell(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Text(
                      cellB,
                      style: GoogleFonts.poppins(
                        fontSize: 13,
                        color: ThemeHelper.textPrimary(context),
                      ),
                    ),
                  ),
                ),
              ],
            );
          }),
        ],
      ),
    );
  }

  String _getExplanation(Question question) {
    // This would typically come from your question data
    // For now, providing sample explanations
    switch (question.id) {
      case 1:
        return 'Chennai is the capital city of Tamil Nadu and serves as the state\'s administrative center.';
      case 2:
        return 'Ilango Adigal is credited with writing the Tamil epic "Silappatikaram" (The Tale of the Anklet), one of the five great epics of Tamil literature.';
      case 3:
        return 'Water is composed of two hydrogen atoms and one oxygen atom, giving it the chemical formula H2O.';
      case 4:
        return 'India gained independence from British rule on August 15, 1947, after a long struggle for freedom.';
      case 5:
        return 'To calculate 15% of 200: 15/100 × 200 = 0.15 × 200 = 30.';
      default:
        return 'This is the correct answer based on the question requirements.';
    }
  }
}

