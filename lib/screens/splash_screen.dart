import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../utils/app_colors.dart';
import '../services/api_service.dart';
import '../services/meta_app_events_service.dart';
import '../services/google_analytics_service.dart';
import 'login_page.dart';
import 'exam_selection_page.dart';
import 'home_page.dart';
import 'subscription_offer_page.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({Key? key}) : super(key: key);

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  // NOTE: Purchase event is now logged when 7-day trial is activated
  // (in subscription_page.dart and subscription_offer_page.dart)
  // No need to log again when actual charge happens.

  @override
  void initState() {
    super.initState();
    // Log app activation for Meta tracking (helps with install attribution)
    MetaAppEventsService.logActivateApp();
    // Log app open for Google Analytics
    GoogleAnalyticsService.logAppOpen();
    _checkLoginStatus();
  }

  /// Check premium status and navigate accordingly
  Future<void> _navigateAfterLogin(BuildContext context, SharedPreferences prefs, String selectedExam) async {
    bool isPremium = prefs.getBool('is_premium') ?? false;

    // Best-effort refresh from server (also used to detect real subscription charges)
    final userId = prefs.getInt('user_id') ?? prefs.getInt('userId');
    if (userId != null) {
      try {
        final url = '${ApiService.baseUrl}/subscriptions/status.php?user_id=$userId';
        final res = await http.get(Uri.parse(url)).timeout(const Duration(seconds: 10));
        if (res.statusCode == 200) {
          final data = jsonDecode(res.body);
          if (data is Map<String, dynamic>) {
            isPremium = data['is_premium'] == true;
            await prefs.setBool('is_premium', isPremium);

            // Set user ID for tracking (Meta + Google Analytics)
            await MetaAppEventsService.setUserId(userId.toString());
            await GoogleAnalyticsService.setUserId(userId.toString());
          }
        }
      } catch (_) {
        // ignore network errors; use cached value
      }
    }

    if (!isPremium && context.mounted) {
      // Show subscription offer first
      final result = await Navigator.push<bool>(
        context,
        _fadeRoute<bool>(const SubscriptionOfferPage(useVideoHero: true)),
      );
      // Update premium status if subscribed
      if (result == true) {
        await prefs.setBool('is_premium', true);
      }
    }

    // Now go to home page
    if (context.mounted) {
      Navigator.pushReplacement(
        context,
        _fadeRoute(HomePage(selectedExam: selectedExam)),
      );
    }
  }

  // Smooth fade transition to avoid black screen
  Route<T> _fadeRoute<T>(Widget page) {
    return PageRouteBuilder<T>(
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        return FadeTransition(opacity: animation, child: child);
      },
      transitionDuration: const Duration(milliseconds: 300),
    );
  }

  Future<void> _checkLoginStatus() async {
    // No delay - navigate immediately
    final prefs = await SharedPreferences.getInstance();
    final isLoggedIn = prefs.getBool('isLoggedIn') ?? false;
    
    if (!mounted) return;
    
    if (isLoggedIn) {
      // Ensure device id exists (used for single-device session checks)
      await ApiService.getOrCreateDeviceId();

      final userId = prefs.getInt('userId');
      if (userId == null) {
        await prefs.setBool('isLoggedIn', false);
        Navigator.pushReplacement(
          context,
          _fadeRoute(const LoginPage()),
        );
        return;
      }

      // Validate session with server (will force logout if another device logged in)
      final sessionRes = await ApiService.checkSession(userId: userId);
      if (!mounted) return;
      if (sessionRes['success'] != true) {
        // Only force logout for real auth problems.
        // If it's a network/server temporary issue, keep user logged in.
        final errorCode = sessionRes['error_code']?.toString();
        final httpStatus = sessionRes['http_status'];
        final isAuthFailure = httpStatus == 401 ||
            errorCode == 'SESSION_REVOKED' ||
            errorCode == 'UNAUTHORIZED' ||
            errorCode == 'SESSION_NOT_INITIALIZED';

        if (isAuthFailure) {
          await prefs.setBool('isLoggedIn', false);
          await prefs.remove('token');
          await prefs.remove('userId');
          await prefs.remove('userName');
          await prefs.remove('userMobile');
          await prefs.remove('selectedExam');
          await prefs.remove('selectedExamId');
          Navigator.pushReplacement(
            context,
            _fadeRoute(const LoginPage()),
          );
          return;
        }
        // else: proceed (offline / server down)
      }

      // User is logged in, check if exam is already selected
      final selectedExam = prefs.getString('selectedExam');
      final selectedExamId = prefs.getInt('selectedExamId');
      
      if (selectedExam != null && selectedExamId != null) {
        // Exam already selected, check premium and navigate
        await _navigateAfterLogin(context, prefs, selectedExam);
      } else {
        // First time login, show exam selection
        Navigator.pushReplacement(
          context,
          _fadeRoute(const ExamSelectionPage()),
        );
      }
    } else {
      // User is not logged in, go to login page
      Navigator.pushReplacement(
        context,
        _fadeRoute(const LoginPage()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    // Show splash UI while async login/session checks run
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // App logo
            Image.asset(
              'assets/images/logo.png',
              width: 200,
              height: 200,
              fit: BoxFit.contain,
            ),
            const SizedBox(height: 24),
            // Loading indicator
            const SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(
                strokeWidth: 2.5,
                valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
