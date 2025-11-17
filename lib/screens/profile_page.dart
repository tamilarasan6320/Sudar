import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:provider/provider.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/theme_service.dart';
import '../services/api_service.dart';
import 'performance_page.dart';
import 'login_page.dart';
import 'saved_tests_page.dart';
import 'test_history_page.dart';
import 'about_page.dart';
import 'privacy_policy_page.dart';
import 'edit_profile_page.dart';
import 'notifications_settings_page.dart';
import 'language_settings_page.dart';
import 'theme_settings_page.dart';
import 'help_faq_page.dart';
import 'feedback_page.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({Key? key}) : super(key: key);

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  String _userName = 'Loading...';
  String _userMobile = '';
  int _testsTaken = 0;
  int _userRank = 0;
  double _avgScore = 0;
  int _savedTestsCount = 0;
  bool _isLoadingStats = true;
  
  @override
  void initState() {
    super.initState();
    _loadUserData();
    _loadStats();
    _loadSavedTestsCount();
  }
  
  Future<void> _loadUserData() async {
    final prefs = await SharedPreferences.getInstance();
    final userId = prefs.getInt('userId');
    
    // Try to load fresh data from API first
    if (userId != null) {
      try {
        final response = await ApiService.getUserProfile(userId);
        if (response['success'] == true && response['user'] != null) {
          final user = response['user'];
          setState(() {
            _userName = user['name'] ?? 'User';
            _userMobile = user['mobile'] ?? '';
          });
          // Update SharedPreferences with fresh data
          await prefs.setString('userName', _userName);
          await prefs.setString('userMobile', _userMobile);
          return;
        }
      } catch (e) {
        // If API fails, fall back to SharedPreferences
      }
    }
    
    // Fallback to SharedPreferences
    setState(() {
      _userName = prefs.getString('userName') ?? 'User';
      _userMobile = prefs.getString('userMobile') ?? '';
    });
  }

  Future<void> _loadStats() async {
    setState(() => _isLoadingStats = true);
    
    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      
      if (userId != null) {
        // Load test history
        final historyResponse = await ApiService.getTestHistory(userId: userId);
        if (historyResponse['success'] == true) {
          final history = List<Map<String, dynamic>>.from(historyResponse['history'] ?? []);
          double totalScore = 0;
          for (var test in history) {
            totalScore += (test['percentage'] ?? 0).toDouble();
          }
          
          setState(() {
            _testsTaken = history.length;
            _avgScore = history.isNotEmpty ? totalScore / history.length : 0;
          });
        }
        
        // Load rankings
        final rankingsResponse = await ApiService.getRankings();
        if (rankingsResponse['success'] == true) {
          final rankings = List<Map<String, dynamic>>.from(rankingsResponse['rankings'] ?? []);
          final userRanking = rankings.indexWhere((r) => r['user_id'] == userId);
          if (userRanking != -1) {
            setState(() {
              _userRank = userRanking + 1;
            });
          }
        }
      }
      
      if (mounted) {
        setState(() => _isLoadingStats = false);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoadingStats = false);
      }
    }
  }

  Future<void> _loadSavedTestsCount() async {
    try {
      final response = await ApiService.getQuestionSessions();
      if (response['success'] == true) {
        final sessions = List<Map<String, dynamic>>.from(response['sessions'] ?? []);
        if (mounted) {
          setState(() {
            _savedTestsCount = sessions.length;
          });
        }
      }
    } catch (e) {
      print('Error loading saved tests count: $e');
    }
  }

  // Edit Profile Picture function
  void _editProfilePicture() {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => Container(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Change Profile Picture',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: 20),
            ListTile(
              leading: const Icon(Icons.camera_alt_outlined, color: AppColors.primary),
              title: Text('Take Photo', style: GoogleFonts.poppins()),
              onTap: () {
                Navigator.pop(context);
                _showMessage('Camera feature coming soon!');
              },
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_outlined, color: AppColors.primary),
              title: Text('Choose from Gallery', style: GoogleFonts.poppins()),
              onTap: () {
                Navigator.pop(context);
                _showMessage('Gallery feature coming soon!');
              },
            ),
            ListTile(
              leading: const Icon(Icons.delete_outline, color: AppColors.error),
              title: Text('Remove Photo', style: GoogleFonts.poppins(color: AppColors.error)),
              onTap: () {
                Navigator.pop(context);
                _showMessage('Profile picture removed');
              },
            ),
          ],
        ),
      ),
    );
  }

  // Saved Tests function
  void _openSavedTests() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const SavedTestsPage()),
    );
  }

  // Test History function
  void _openTestHistory() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const TestHistoryPage()),
    );
  }

  // Performance Analytics function
  void _openPerformanceAnalytics() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const PerformancePage()),
    );
  }

  // Edit Profile function
  void _editProfile() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const EditProfilePage()),
    );
    if (result != null && mounted) {
      setState(() {
        _userName = result;
      });
    }
  }

  // Notifications function
  void _openNotifications() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const NotificationsSettingsPage()),
    );
  }

  // Language function
  void _changeLanguage() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const LanguageSettingsPage()),
    );
  }

  // Theme function
  void _changeTheme() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const ThemeSettingsPage()),
    );
  }

  // Help & FAQ function
  void _openHelp() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const HelpFAQPage()),
    );
  }

  // Send Feedback function
  void _sendFeedback() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const FeedbackPage()),
    );
  }

  // About function
  void _openAbout() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const AboutPage()),
    );
  }

  // Privacy Policy function
  void _openPrivacyPolicy() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const PrivacyPolicyPage()),
    );
  }

  // Logout function
  void _handleLogout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    if (!mounted) return;
    Navigator.pushAndRemoveUntil(
      context,
      MaterialPageRoute(builder: (context) => const LoginPage()),
      (route) => false,
    );
  }

  // Helper function to show messages
  void _showMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message, style: GoogleFonts.poppins()),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final backgroundColor = isDark ? AppColors.darkBackground : AppColors.background;
    final cardColor = isDark ? AppColors.darkCardBackground : Colors.white;
    final textPrimary = isDark ? AppColors.darkTextPrimary : AppColors.textPrimary;
    final textSecondary = isDark ? AppColors.darkTextSecondary : AppColors.textSecondary;
    final textLight = isDark ? AppColors.darkTextLight : AppColors.textLight;
    final borderColor = isDark ? AppColors.darkBorder : AppColors.border;
    
    return Scaffold(
      backgroundColor: backgroundColor,
      appBar: AppBar(
        backgroundColor: cardColor,
        elevation: 1,
        title: Text(
          'Profile',
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: textPrimary,
          ),
        ),
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            // Profile Header
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: cardColor,
              ),
              child: Column(
                children: [
                  Stack(
                    children: [
                      Container(
                        width: 100,
                        height: 100,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          gradient: LinearGradient(
                            colors: [AppColors.primary, AppColors.primaryDark],
                          ),
                        ),
                        child: Center(
                          child: Text(
                            _userName.isNotEmpty ? _userName[0].toUpperCase() : 'U',
                            style: GoogleFonts.poppins(
                              fontSize: 40,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ),
                      Positioned(
                        bottom: 0,
                        right: 0,
                        child: GestureDetector(
                          onTap: _editProfilePicture,
                          child: Container(
                            padding: const EdgeInsets.all(6),
                            decoration: const BoxDecoration(
                              color: AppColors.success,
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(
                              Icons.edit,
                              color: Colors.white,
                              size: 16,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    _userName,
                    style: GoogleFonts.poppins(
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                      color: textPrimary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _userMobile.isNotEmpty ? _userMobile : 'No mobile number',
                    style: GoogleFonts.poppins(
                      fontSize: 14,
                      color: textSecondary,
                    ),
                  ),
                  const SizedBox(height: 20),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      _buildProfileStat(
                        _isLoadingStats ? '...' : '$_testsTaken', 
                        'Tests', 
                        textPrimary, 
                        textSecondary
                      ),
                      Container(width: 1, height: 40, color: borderColor),
                      _buildProfileStat(
                        _isLoadingStats ? '...' : (_userRank > 0 ? '#$_userRank' : '--'), 
                        'Rank', 
                        textPrimary, 
                        textSecondary
                      ),
                      Container(width: 1, height: 40, color: borderColor),
                      _buildProfileStat(
                        _isLoadingStats ? '...' : '${_avgScore.toStringAsFixed(0)}%', 
                        'Avg Score', 
                        textPrimary, 
                        textSecondary
                      ),
                    ],
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Exam Preparation Section
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 16),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: cardColor,
                borderRadius: BorderRadius.circular(12),
                boxShadow: ThemeHelper.cardShadow(context),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Exam Preparation',
                    style: GoogleFonts.poppins(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: textPrimary,
                    ),
                  ),
                  const SizedBox(height: 16),
                  _buildMenuItem(
                    icon: Icons.bookmark_outline,
                    title: 'Saved Tests',
                    subtitle: '$_savedTestsCount tests saved',
                    onTap: _openSavedTests,
                  ),
                  const Divider(height: 24),
                  _buildMenuItem(
                    icon: Icons.history,
                    title: 'Test History',
                    subtitle: 'View all attempted tests',
                    onTap: _openTestHistory,
                  ),
                  const Divider(height: 24),
                  _buildMenuItem(
                    icon: Icons.trending_up,
                    title: 'Performance Analytics',
                    subtitle: 'Detailed performance insights',
                    onTap: _openPerformanceAnalytics,
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Account Settings Section
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 16),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: cardColor,
                borderRadius: BorderRadius.circular(12),
                boxShadow: ThemeHelper.cardShadow(context),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Account Settings',
                    style: GoogleFonts.poppins(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: textPrimary,
                    ),
                  ),
                  const SizedBox(height: 16),
                  _buildMenuItem(
                    icon: Icons.person_outline,
                    title: 'Edit Profile',
                    subtitle: 'Update your information',
                    onTap: _editProfile,
                  ),
                  const Divider(height: 24),
                  _buildMenuItem(
                    icon: Icons.notifications_outlined,
                    title: 'Notifications',
                    subtitle: 'Manage notification preferences',
                    onTap: _openNotifications,
                  ),
                  const Divider(height: 24),
                  _buildMenuItem(
                    icon: Icons.language_outlined,
                    title: 'Language',
                    subtitle: 'English',
                    onTap: _changeLanguage,
                  ),
                  const Divider(height: 24),
                  Consumer<ThemeService>(
                    builder: (context, themeService, child) {
                      String themeText = 'Light mode';
                      if (themeService.themeMode == ThemeMode.dark) {
                        themeText = 'Dark mode';
                      } else if (themeService.themeMode == ThemeMode.system) {
                        themeText = 'System default';
                      }
                      return _buildMenuItem(
                        icon: Icons.dark_mode_outlined,
                        title: 'Theme',
                        subtitle: themeText,
                        onTap: _changeTheme,
                      );
                    },
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Support Section
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 16),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: cardColor,
                borderRadius: BorderRadius.circular(12),
                boxShadow: ThemeHelper.cardShadow(context),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Support',
                    style: GoogleFonts.poppins(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: textPrimary,
                    ),
                  ),
                  const SizedBox(height: 16),
                  _buildMenuItem(
                    icon: Icons.help_outline,
                    title: 'Help & FAQ',
                    subtitle: 'Get help with the app',
                    onTap: _openHelp,
                  ),
                  const Divider(height: 24),
                  _buildMenuItem(
                    icon: Icons.feedback_outlined,
                    title: 'Send Feedback',
                    subtitle: 'Share your thoughts',
                    onTap: _sendFeedback,
                  ),
                  const Divider(height: 24),
                  _buildMenuItem(
                    icon: Icons.info_outline,
                    title: 'About',
                    subtitle: 'App information',
                    onTap: _openAbout,
                  ),
                  const Divider(height: 24),
                  _buildMenuItem(
                    icon: Icons.privacy_tip_outlined,
                    title: 'Privacy Policy',
                    subtitle: 'View our privacy policy',
                    onTap: _openPrivacyPolicy,
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Logout Button
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: () {
                    showDialog(
                      context: context,
                      builder: (context) => AlertDialog(
                        title: Text('Logout', style: GoogleFonts.poppins(fontWeight: FontWeight.bold)),
                        content: Text('Are you sure you want to logout?', style: GoogleFonts.poppins()),
                        actions: [
                          TextButton(
                            onPressed: () => Navigator.pop(context),
                            child: Text('Cancel', style: GoogleFonts.poppins()),
                          ),
                          ElevatedButton(
                            onPressed: () {
                              Navigator.pop(context);
                              _handleLogout();
                            },
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.error,
                            ),
                            child: Text('Logout', style: GoogleFonts.poppins(color: Colors.white)),
                          ),
                        ],
                      ),
                    );
                  },
                  icon: const Icon(Icons.logout, color: AppColors.error),
                  label: Text(
                    'Logout',
                    style: GoogleFonts.poppins(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: AppColors.error,
                    ),
                  ),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    side: const BorderSide(color: AppColors.error),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                ),
              ),
            ),
            
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _buildProfileStat(String value, String label, Color textPrimary, Color textSecondary) {
    return Column(
      children: [
        Text(
          value,
          style: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: AppColors.primary,
          ),
        ),
        Text(
          label,
          style: GoogleFonts.poppins(
            fontSize: 12,
            color: textSecondary,
          ),
        ),
      ],
    );
  }

  Widget _buildMenuItem({
    required IconData icon,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final textPrimary = isDark ? AppColors.darkTextPrimary : AppColors.textPrimary;
    final textLight = isDark ? AppColors.darkTextLight : AppColors.textLight;
    
    return InkWell(
      onTap: onTap,
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: AppColors.primary.withOpacity(0.1),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: AppColors.primary, size: 22),
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
                    color: textPrimary,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  style: GoogleFonts.poppins(
                    fontSize: 12,
                    color: textLight,
                  ),
                ),
              ],
            ),
          ),
          Icon(
            Icons.arrow_forward_ios_rounded,
            size: 16,
            color: textLight,
          ),
        ],
      ),
    );
  }
}

