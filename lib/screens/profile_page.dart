import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/theme_service.dart';
import '../services/api_service.dart';
import '../services/onesignal_service.dart';
import 'performance_page.dart';
import 'login_page.dart';
// import 'saved_tests_page.dart'; // Removed - will implement in later phase
import 'test_history_page.dart';
import 'about_page.dart';
import 'privacy_policy_page.dart';
import 'edit_profile_page.dart';
import 'notifications_settings_page.dart';
import 'language_settings_page.dart';
// import 'theme_settings_page.dart'; // Removed - will implement in later phase
import 'help_faq_page.dart';
import 'feedback_page.dart';
import 'account_deletion_page.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({Key? key}) : super(key: key);

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  // WhatsApp support number
  static const String _whatsappNumber = '918300321814';
  static const String _displayNumber = '+91 83003 21814';

  String _userName = 'Loading...';
  String _userMobile = '';
  int _testsTaken = 0;
  int _userRank = 0;
  double _avgScore = 0;
  // int _savedTestsCount = 0; // Removed - will implement in later phase
  bool _isLoadingStats = true;
  bool _isPremium = false;
  
  @override
  void initState() {
    super.initState();
    _loadUserData();
    _loadStats();
    _loadPremiumStatus();
    // _loadSavedTestsCount(); // Removed - will implement in later phase
  }

  Future<void> _loadPremiumStatus() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _isPremium = prefs.getBool('is_premium') ?? false;
    });
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

  // Removed - will implement in later phase
  // Future<void> _loadSavedTestsCount() async {
  //   try {
  //     final response = await ApiService.getQuestionSessions();
  //     if (response['success'] == true) {
  //       final sessions = List<Map<String, dynamic>>.from(response['sessions'] ?? []);
  //       if (mounted) {
  //         setState(() {
  //           _savedTestsCount = sessions.length;
  //         });
  //       }
  //     }
  //   } catch (e) {
  //     print('Error loading saved tests count: $e');
  //   }
  // }

  // Removed - Profile Picture edit option removed
  // void _editProfilePicture() {
  //   // Profile photo change/edit option removed
  // }

  // Removed - will implement in later phase
  // void _openSavedTests() {
  //   Navigator.push(
  //     context,
  //     MaterialPageRoute(builder: (context) => const SavedTestsPage()),
  //   );
  // }

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

  // Removed - will implement in later phase
  // void _changeTheme() {
  //   Navigator.push(
  //     context,
  //     MaterialPageRoute(builder: (context) => const ThemeSettingsPage()),
  //   );
  // }

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

  void _openAccountDeletion() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const AccountDeletionPage()),
    );
  }

  // Logout function
  void _handleLogout() async {
    // Remove OneSignal user ID
    await OneSignalService.removeUserId();
    
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

  /// Opens WhatsApp with the support number
  Future<void> _openWhatsAppSupport() async {
    final whatsappUrl = Uri.parse(
      'https://wa.me/$_whatsappNumber?text=${Uri.encodeComponent("Hi, I need help with the Sudar TNPSC App.")}',
    );

    try {
      final launched = await launchUrl(
        whatsappUrl,
        mode: LaunchMode.externalApplication,
      );

      if (!launched && mounted) {
        _showWhatsAppError();
      }
    } catch (e) {
      if (mounted) {
        _showWhatsAppError();
      }
    }
  }

  /// Shows error dialog with option to copy number
  void _showWhatsAppError() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Row(
          children: [
            Icon(Icons.error_outline, color: AppColors.error),
            const SizedBox(width: 8),
            Text(
              'WhatsApp Not Found',
              style: GoogleFonts.poppins(fontWeight: FontWeight.bold, fontSize: 18),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Could not open WhatsApp. Contact us at:',
              style: GoogleFonts.poppins(fontSize: 14),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.primary.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  const Icon(Icons.phone, color: AppColors.primary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _displayNumber,
                      style: GoogleFonts.poppins(
                        fontWeight: FontWeight.w600,
                        fontSize: 16,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Close', style: GoogleFonts.poppins()),
          ),
          ElevatedButton.icon(
            onPressed: () async {
              await Clipboard.setData(ClipboardData(text: _displayNumber));
              if (mounted) {
                Navigator.pop(context);
                _showMessage('Number copied to clipboard!');
              }
            },
            icon: const Icon(Icons.copy, size: 18),
            label: Text('Copy Number', style: GoogleFonts.poppins()),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
          ),
        ],
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
        actions: [
          InkWell(
            onTap: _openWhatsAppSupport,
            borderRadius: BorderRadius.circular(4),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Center(
                child: Text(
                  'Support',
                  style: GoogleFonts.poppins(
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: AppColors.primary,
                  ),
                ),
              ),
            ),
          ),
        ],
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
                      // Removed - Profile photo edit button removed
                    ],
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        _userName,
                        style: GoogleFonts.poppins(
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                          color: textPrimary,
                        ),
                      ),
                      if (_isPremium) ...[
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            gradient: const LinearGradient(
                              colors: [Color(0xFFFFD700), Color(0xFFFFA500)],
                            ),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.workspace_premium, color: Colors.white, size: 14),
                              const SizedBox(width: 4),
                              Text(
                                'PRO',
                                style: GoogleFonts.poppins(
                                  fontSize: 10,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ],
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
                  // Removed Saved Tests - will implement in later phase
                  // const SizedBox(height: 16),
                  // _buildMenuItem(
                  //   icon: Icons.bookmark_outline,
                  //   title: 'Saved Tests',
                  //   subtitle: '$_savedTestsCount tests saved',
                  //   onTap: _openSavedTests,
                  // ),
                  const SizedBox(height: 16),
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
                  // Temporarily removed - Notifications
                  // const Divider(height: 24),
                  // _buildMenuItem(
                  //   icon: Icons.notifications_outlined,
                  //   title: 'Notifications',
                  //   subtitle: 'Manage notification preferences',
                  //   onTap: _openNotifications,
                  // ),
                  // Temporarily removed - Language
                  // const Divider(height: 24),
                  // _buildMenuItem(
                  //   icon: Icons.language_outlined,
                  //   title: 'Language',
                  //   subtitle: 'English',
                  //   onTap: _changeLanguage,
                  // ),
                  // Removed Theme - will implement in later phase
                  // const Divider(height: 24),
                  // Consumer<ThemeService>(
                  //   builder: (context, themeService, child) {
                  //     String themeText = 'Light mode';
                  //     if (themeService.themeMode == ThemeMode.dark) {
                  //       themeText = 'Dark mode';
                  //     } else if (themeService.themeMode == ThemeMode.system) {
                  //       themeText = 'System default';
                  //     }
                  //     return _buildMenuItem(
                  //       icon: Icons.dark_mode_outlined,
                  //       title: 'Theme',
                  //       subtitle: themeText,
                  //       onTap: _changeTheme,
                  //     );
                  //   },
                  // ),
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
                  const Divider(height: 24),
                  _buildMenuItem(
                    icon: Icons.delete_forever_outlined,
                    title: 'Delete Account',
                    subtitle: 'Request account deletion',
                    onTap: _openAccountDeletion,
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

