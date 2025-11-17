import 'dart:async';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import 'test_results_page.dart';

class TestPage extends StatefulWidget {
  final String testTitle;
  final String category;
  final int sessionId;
  
  const TestPage({
    Key? key,
    required this.testTitle,
    required this.category,
    required this.sessionId,
  }) : super(key: key);

  @override
  State<TestPage> createState() => _TestPageState();
}

class _TestPageState extends State<TestPage> {
  int _currentQuestionIndex = 0;
  int _timeRemaining = 3600; // 60 minutes in seconds
  Timer? _timer;
  bool _isLoading = true;
  String? _errorMessage;
  
  List<Question> _questions = [];
  
  final Map<int, int> _selectedAnswers = {};
  final Set<int> _markedForReview = {};

  @override
  void initState() {
    super.initState();
    _loadQuestions();
  }

  Future<void> _loadQuestions() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final response = await ApiService.getQuestions(sessionId: widget.sessionId);
      
      if (mounted) {
        if (response['success'] == true) {
          final questionsData = response['questions'] as List;
          
          if (questionsData.isEmpty) {
            setState(() {
              _isLoading = false;
              _errorMessage = 'This test has no questions yet. Please contact the administrator.';
            });
            return;
          }

          setState(() {
            _questions = questionsData.map((q) {
              // Parse English options
              List<String>? optionsEn;
              if (q['option_a_en'] != null || q['option_b_en'] != null) {
                optionsEn = [
                  q['option_a_en']?.toString() ?? '',
                  q['option_b_en']?.toString() ?? '',
                  q['option_c_en']?.toString() ?? '',
                  q['option_d_en']?.toString() ?? '',
                ];
              }
              
              // Parse Tamil options
              List<String>? optionsTa;
              if (q['option_a_ta'] != null || q['option_b_ta'] != null) {
                optionsTa = [
                  q['option_a_ta']?.toString() ?? '',
                  q['option_b_ta']?.toString() ?? '',
                  q['option_c_ta']?.toString() ?? '',
                  q['option_d_ta']?.toString() ?? '',
                ];
              }
              
              // Get question texts
              String? questionEn = q['question_en']?.toString();
              String? questionTa = q['question_ta']?.toString();
              
              // Determine correct answer index
              String correctOption = (q['correct_answer'] ?? 'A').toString().toUpperCase();
              int correctIndex = {'A': 0, 'B': 1, 'C': 2, 'D': 3}[correctOption] ?? 0;
              
              bool hasEn = q['has_english'] == true;
              bool hasTa = q['has_tamil'] == true;
              
              print('📝 Question ${q['id']}: EN="${hasEn ? 'Yes' : 'No'}", TA="${hasTa ? 'Yes' : 'No'}", correct=$correctOption');
              
              return Question(
                id: q['id'] is int ? q['id'] : int.parse(q['id'].toString()),
                textEn: questionEn,
                textTa: questionTa,
                optionsEn: optionsEn,
                optionsTa: optionsTa,
                correctAnswer: correctIndex,
                explanationEn: q['explanation_en']?.toString(),
                explanationTa: q['explanation_ta']?.toString(),
                hasEnglish: hasEn,
                hasTamil: hasTa,
              );
            }).where((q) {
              // Only include questions that have at least one language
              return q.hasEnglish || q.hasTamil;
            }).toList();
            
            print('✅ Loaded ${_questions.length} questions out of ${questionsData.length} total');
            
            _isLoading = false;
            // Start timer after questions are loaded
            _startTimer();
          });
        } else {
          setState(() {
            _isLoading = false;
            _errorMessage = response['message'] ?? 'Failed to load questions';
          });
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _errorMessage = 'Error loading questions: $e';
        });
      }
    }
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_timeRemaining > 0) {
        setState(() {
          _timeRemaining--;
        });
      } else {
        _timer?.cancel();
        _submitTest();
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  String _formatTime(int seconds) {
    int hours = seconds ~/ 3600;
    int minutes = (seconds % 3600) ~/ 60;
    int secs = seconds % 60;
    return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${secs.toString().padLeft(2, '0')}';
  }

  void _selectAnswer(int answerIndex) {
    setState(() {
      _selectedAnswers[_currentQuestionIndex] = answerIndex;
    });
  }

  void _nextQuestion() {
    if (_currentQuestionIndex < _questions.length - 1) {
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

  void _toggleMarkForReview() {
    setState(() {
      if (_markedForReview.contains(_currentQuestionIndex)) {
        _markedForReview.remove(_currentQuestionIndex);
      } else {
        _markedForReview.add(_currentQuestionIndex);
      }
    });
  }

  Future<void> _submitTest() async {
    _timer?.cancel();
    
    // Show loading dialog
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => Center(
        child: Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const CircularProgressIndicator(color: AppColors.primary),
              const SizedBox(height: 16),
              Text(
                'Submitting your test...',
                style: GoogleFonts.poppins(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ),
    );
    
    try {
      // Get user ID from SharedPreferences
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      
      if (userId == null) {
        Navigator.pop(context); // Close loading dialog
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error: User not logged in', style: GoogleFonts.poppins()),
            backgroundColor: AppColors.error,
          ),
        );
        return;
      }
      
      // Prepare answers for API
      final answers = _selectedAnswers.entries.map((entry) {
        final questionIndex = entry.key;
        final answerIndex = entry.value;
        final question = _questions[questionIndex];
        
        // Convert answer index (0,1,2,3) to letter (A,B,C,D)
        final answerLetter = String.fromCharCode(65 + answerIndex); // 65 is 'A'
        
        return {
          'question_id': question.id,
          'answer': answerLetter,
          'time_spent': 0,
          'marked_for_review': _markedForReview.contains(questionIndex),
        };
      }).toList();
      
      // Calculate time taken in seconds
      final timeTaken = 3600 - _timeRemaining;
      
      // Get started time (current time minus time taken)
      final now = DateTime.now();
      final startedAt = now.subtract(Duration(seconds: timeTaken)).toIso8601String();
      
      print('📤 Submitting test:');
      print('  User ID: $userId');
      print('  Session ID: ${widget.sessionId}');
      print('  Answers: ${answers.length}');
      print('  Time taken: $timeTaken seconds');
      
      // Submit to API
      final response = await ApiService.submitTestResult(
        userId: userId,
        sessionId: widget.sessionId,
        startedAt: startedAt,
        timeTaken: timeTaken,
        answers: answers,
      );
      
      print('📥 Submit response: $response');
      
      Navigator.pop(context); // Close loading dialog
      
      if (response['success'] == true) {
        // Calculate correctAnswers for results page
        int correctAnswers = 0;
        _selectedAnswers.forEach((questionIndex, selectedAnswer) {
          if (_questions[questionIndex].correctAnswer == selectedAnswer) {
            correctAnswers++;
          }
        });
        
        // Show success message
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Test submitted successfully!',
              style: GoogleFonts.poppins(),
            ),
            backgroundColor: AppColors.success,
          ),
        );
        
        // Navigate to results page
        Navigator.pushReplacement(
          context,
          MaterialPageRoute(
            builder: (context) => TestResultsPage(
              questions: _questions,
              selectedAnswers: _selectedAnswers,
              correctAnswers: correctAnswers,
              totalQuestions: _questions.length,
              testTitle: widget.testTitle,
              category: widget.category,
            ),
          ),
        );
      } else {
        // Show error message
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              response['message'] ?? 'Failed to submit test',
              style: GoogleFonts.poppins(),
            ),
            backgroundColor: AppColors.error,
          ),
        );
      }
    } catch (e) {
      Navigator.pop(context); // Close loading dialog
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Error submitting test: $e',
            style: GoogleFonts.poppins(),
          ),
          backgroundColor: AppColors.error,
        ),
      );
    }
  }

  void _showSubmitConfirmation() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          'Submit Test?',
          style: GoogleFonts.poppins(fontWeight: FontWeight.bold),
        ),
        content: Text(
          'Are you sure you want to submit the test? You have answered ${_selectedAnswers.length} out of ${_questions.length} questions.',
          style: GoogleFonts.poppins(),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(
              'Cancel',
              style: GoogleFonts.poppins(color: AppColors.textSecondary),
            ),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              _submitTest();
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
            ),
            child: Text(
              'Submit',
              style: GoogleFonts.poppins(color: Colors.white),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Show loading state
    if (_isLoading) {
      return Scaffold(
        backgroundColor: ThemeHelper.backgroundColor(context),
        appBar: AppBar(
          backgroundColor: ThemeHelper.cardColor(context),
          elevation: 1,
          title: Text(
            'Loading Test...',
            style: GoogleFonts.poppins(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: ThemeHelper.textPrimary(context),
            ),
          ),
        ),
        body: const Center(
          child: CircularProgressIndicator(color: AppColors.primary),
        ),
      );
    }

    // Show error state
    if (_errorMessage != null) {
      return Scaffold(
        backgroundColor: ThemeHelper.backgroundColor(context),
        appBar: AppBar(
          backgroundColor: ThemeHelper.cardColor(context),
          elevation: 1,
          title: Text(
            widget.testTitle,
            style: GoogleFonts.poppins(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: ThemeHelper.textPrimary(context),
            ),
          ),
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.error_outline, size: 64, color: AppColors.error),
                const SizedBox(height: 16),
                Text(
                  _errorMessage!,
                  style: GoogleFonts.poppins(
                    fontSize: 16,
                    color: AppColors.textSecondary,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: () => Navigator.pop(context),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 12),
                  ),
                  child: Text(
                    'Go Back',
                    style: GoogleFonts.poppins(color: Colors.white),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    // Check if questions list is empty
    if (_questions.isEmpty) {
      return Scaffold(
        backgroundColor: ThemeHelper.backgroundColor(context),
        appBar: AppBar(
          backgroundColor: ThemeHelper.cardColor(context),
          elevation: 1,
          title: Text(
            widget.testTitle,
            style: GoogleFonts.poppins(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: ThemeHelper.textPrimary(context),
            ),
          ),
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.quiz_outlined, size: 64, color: AppColors.textSecondary),
                const SizedBox(height: 16),
                Text(
                  'No valid questions found in this test.\nSome questions may have empty content.',
                  style: GoogleFonts.poppins(
                    fontSize: 16,
                    color: AppColors.textSecondary,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: () => Navigator.pop(context),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 12),
                  ),
                  child: Text(
                    'Go Back',
                    style: GoogleFonts.poppins(color: Colors.white),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    // Show test with questions
    final currentQuestion = _questions[_currentQuestionIndex];
    
    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      appBar: AppBar(
        backgroundColor: ThemeHelper.cardColor(context),
        elevation: 1,
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: ThemeHelper.textPrimary(context)),
          onPressed: () {
            showDialog(
              context: context,
              builder: (context) => AlertDialog(
                title: Text('Exit Test?', style: GoogleFonts.poppins(fontWeight: FontWeight.bold)),
                content: Text('Your progress will be lost.', style: GoogleFonts.poppins()),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: Text('Cancel', style: GoogleFonts.poppins()),
                  ),
                  TextButton(
                    onPressed: () {
                      Navigator.pop(context);
                      Navigator.pop(context);
                    },
                    child: Text('Exit', style: GoogleFonts.poppins(color: AppColors.error)),
                  ),
                ],
              ),
            );
          },
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widget.testTitle,
              style: GoogleFonts.poppins(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: ThemeHelper.textPrimary(context),
              ),
            ),
            Text(
              widget.category,
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
              color: _timeRemaining < 300 ? AppColors.error.withOpacity(0.1) : AppColors.primary.withOpacity(0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.timer_outlined,
                  size: 18,
                  color: _timeRemaining < 300 ? AppColors.error : AppColors.primary,
                ),
                const SizedBox(width: 6),
                Text(
                  _formatTime(_timeRemaining),
                  style: GoogleFonts.poppins(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _timeRemaining < 300 ? AppColors.error : AppColors.primary,
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
            value: (_currentQuestionIndex + 1) / _questions.length,
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
                  // Question Number
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppColors.primary,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      'Question ${_currentQuestionIndex + 1}/${_questions.length}',
                      style: GoogleFonts.poppins(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                      ),
                    ),
                  ),
                  
                  const SizedBox(height: 20),
                  
                  // Question Text - Bilingual
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
                          Text(
                            currentQuestion.textEn!,
                            style: GoogleFonts.poppins(
                              fontSize: 16,
                              fontWeight: FontWeight.w500,
                              color: ThemeHelper.textPrimary(context),
                              height: 1.5,
                            ),
                          ),
                          if (currentQuestion.hasTamil) const SizedBox(height: 16),
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
                          Text(
                            currentQuestion.textTa!,
                            style: GoogleFonts.poppins(
                              fontSize: 16,
                              fontWeight: FontWeight.w500,
                              color: ThemeHelper.textPrimary(context),
                              height: 1.5,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  
                  const SizedBox(height: 24),
                  
                  // Options - Bilingual
                  ...List.generate(4, (index) {
                    final isSelected = _selectedAnswers[_currentQuestionIndex] == index;
                    
                    return GestureDetector(
                      onTap: () => _selectAnswer(index),
                      child: Container(
                        width: double.infinity,
                        margin: const EdgeInsets.only(bottom: 12),
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: isSelected ? AppColors.primary.withOpacity(0.1) : Colors.white,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(
                            color: isSelected ? AppColors.primary : AppColors.border,
                            width: isSelected ? 2 : 1,
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
                                  color: isSelected ? AppColors.primary : AppColors.border,
                                  width: 2,
                                ),
                                color: isSelected ? AppColors.primary : Colors.transparent,
                              ),
                              child: isSelected
                                  ? const Icon(Icons.check, size: 18, color: Colors.white)
                                  : null,
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  // English Option
                                  if (currentQuestion.hasEnglish && currentQuestion.optionsEn != null)
                                    Text(
                                      '${String.fromCharCode(65 + index)}) ${currentQuestion.optionsEn![index]}',
                                      style: GoogleFonts.poppins(
                                        fontSize: 15,
                                        fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
                                        color: isSelected ? AppColors.primary : AppColors.textPrimary,
                                      ),
                                    ),
                                  
                                  // Spacing between languages
                                  if (currentQuestion.hasEnglish && currentQuestion.hasTamil && 
                                      currentQuestion.optionsEn != null && currentQuestion.optionsTa != null)
                                    const SizedBox(height: 4),
                                  
                                  // Tamil Option
                                  if (currentQuestion.hasTamil && currentQuestion.optionsTa != null)
                                    Text(
                                      '${['அ', 'ஆ', 'இ', 'ஈ'][index]}) ${currentQuestion.optionsTa![index]}',
                                      style: GoogleFonts.poppins(
                                        fontSize: 15,
                                        fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
                                        color: isSelected ? AppColors.primary : AppColors.textPrimary,
                                      ),
                                    ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  }),
                  
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
                  
                  // Mark for Review
                  OutlinedButton(
                    onPressed: _toggleMarkForReview,
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 16),
                      side: BorderSide(
                        color: _markedForReview.contains(_currentQuestionIndex) 
                            ? AppColors.warning 
                            : AppColors.border,
                      ),
                      backgroundColor: _markedForReview.contains(_currentQuestionIndex)
                          ? AppColors.warning.withOpacity(0.1)
                          : Colors.transparent,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    child: Icon(
                      _markedForReview.contains(_currentQuestionIndex) 
                          ? Icons.bookmark 
                          : Icons.bookmark_border,
                      color: _markedForReview.contains(_currentQuestionIndex) 
                          ? AppColors.warning 
                          : AppColors.textSecondary,
                    ),
                  ),
                  
                  const SizedBox(width: 12),
                  
                  // Next/Submit Button
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _currentQuestionIndex < _questions.length - 1
                          ? _nextQuestion
                          : _showSubmitConfirmation,
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
                            _currentQuestionIndex < _questions.length - 1 ? 'Next' : 'Submit',
                            style: GoogleFonts.poppins(
                              fontWeight: FontWeight.w600,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Icon(
                            _currentQuestionIndex < _questions.length - 1
                                ? Icons.arrow_forward
                                : Icons.check_circle,
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
}

class Question {
  final int id;
  final String? textEn;
  final String? textTa;
  final List<String>? optionsEn;
  final List<String>? optionsTa;
  final int correctAnswer;
  final String? explanationEn;
  final String? explanationTa;
  final bool hasEnglish;
  final bool hasTamil;

  Question({
    required this.id,
    this.textEn,
    this.textTa,
    this.optionsEn,
    this.optionsTa,
    required this.correctAnswer,
    this.explanationEn,
    this.explanationTa,
    required this.hasEnglish,
    required this.hasTamil,
  });
}

