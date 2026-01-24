import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../services/api_service.dart';
import '../services/meta_app_events_service.dart';
import '../services/google_analytics_service.dart';
import '../utils/app_colors.dart';
import 'login_page.dart';
import 'exam_selection_page.dart';
import 'home_page.dart';
import 'subscription_offer_page.dart';

/// A startup router that shows a smooth loading spinner while checking login/session.
/// Android native splash shows logo, this shows just spinner while loading data.
class AppStartupRouter extends StatefulWidget {
  const AppStartupRouter({Key? key}) : super(key: key);

  @override
  State<AppStartupRouter> createState() => _AppStartupRouterState();
}

class _AppStartupRouterState extends State<AppStartupRouter> {
  bool _hasNavigated = false;

  @override
  void initState() {
    super.initState();
    // Log app activation for Meta tracking (helps with install/open attribution)
    MetaAppEventsService.logActivateApp();
    // Log app open for Google Analytics
    GoogleAnalyticsService.logAppOpen();
    // Run startup logic after first frame
    SchedulerBinding.instance.addPostFrameCallback((_) {
      _runStartupLogic();
    });
  }

  // NOTE: Purchase event is now logged when 7-day trial is activated
  // (in subscription_page.dart and subscription_offer_page.dart)
  // No need to log again when actual charge happens.

  Future<void> _runStartupLogic() async {
    final prefs = await SharedPreferences.getInstance();
    final isLoggedIn = prefs.getBool('isLoggedIn') ?? false;

    if (!mounted) return;

    if (isLoggedIn) {
      // Ensure device id exists
      await ApiService.getOrCreateDeviceId();

      final userId = prefs.getInt('userId');
      if (userId == null) {
        await prefs.setBool('isLoggedIn', false);
        _navigateToFirstScreen(const LoginPage());
        return;
      }

      // Validate session with server
      final sessionRes = await ApiService.checkSession(userId: userId);
      if (!mounted) return;

      if (sessionRes['success'] != true) {
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
          _navigateToFirstScreen(const LoginPage());
          return;
        }
      }

      // User is logged in, check if exam is already selected
      final selectedExam = prefs.getString('selectedExam');
      final selectedExamId = prefs.getInt('selectedExamId');

      if (selectedExam != null && selectedExamId != null) {
        // Exam already selected, check premium and navigate
        await _navigateAfterLogin(prefs, selectedExam);
      } else {
        // First time login, show exam selection
        _navigateToFirstScreen(const ExamSelectionPage());
      }
    } else {
      // User is not logged in, go to login page
      _navigateToFirstScreen(const LoginPage());
    }
  }

  Future<void> _navigateAfterLogin(SharedPreferences prefs, String selectedExam) async {
    bool isPremium = prefs.getBool('is_premium') ?? false;

    // Best-effort refresh from server
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

    if (!isPremium && mounted) {
      // Show subscription offer first (use push, not pushReplacement, so we can navigate after)
      final result = await Navigator.push<bool?>(
        context,
        _fadeRoute<bool?>(const SubscriptionOfferPage(useVideoHero: true)),
      );
      
      if (!mounted) return;
      
      // Update premium status if subscribed
      if (result == true) {
        await prefs.setBool('is_premium', true);
      }
      
      // After subscription page, go to home
      _hasNavigated = true;
      Navigator.pushReplacement(
        context,
        _fadeRoute(HomePage(selectedExam: selectedExam)),
      );
    } else {
      // Premium user - go directly to home
      _navigateToFirstScreen(HomePage(selectedExam: selectedExam));
    }
  }

  void _navigateToFirstScreen(Widget screen) {
    if (!mounted || _hasNavigated) return;
    _hasNavigated = true;
    
    Navigator.pushReplacement(
      context,
      _fadeRoute(screen),
    );
  }

  // Fade transition to avoid black screen flash
  Route<T> _fadeRoute<T>(Widget page) {
    return PageRouteBuilder<T>(
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        return FadeTransition(opacity: animation, child: child);
      },
      transitionDuration: const Duration(milliseconds: 200),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Plain white screen with smooth animated spinner while loading data
    return const Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: _SmoothSpinner(),
      ),
    );
  }
}

/// Smooth animated spinner with fade-in and pulse effect
class _SmoothSpinner extends StatefulWidget {
  const _SmoothSpinner();

  @override
  State<_SmoothSpinner> createState() => _SmoothSpinnerState();
}

class _SmoothSpinnerState extends State<_SmoothSpinner>
    with TickerProviderStateMixin {
  late AnimationController _fadeController;
  late AnimationController _pulseController;
  late Animation<double> _fadeAnimation;
  late Animation<double> _pulseAnimation;

  @override
  void initState() {
    super.initState();
    
    // Fade in animation
    _fadeController = AnimationController(
      duration: const Duration(milliseconds: 400),
      vsync: this,
    );
    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _fadeController, curve: Curves.easeOut),
    );
    
    // Subtle pulse animation
    _pulseController = AnimationController(
      duration: const Duration(milliseconds: 1200),
      vsync: this,
    );
    _pulseAnimation = Tween<double>(begin: 0.9, end: 1.1).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
    
    // Start animations
    _fadeController.forward();
    _pulseController.repeat(reverse: true);
  }

  @override
  void dispose() {
    _fadeController.dispose();
    _pulseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _fadeAnimation,
      child: ScaleTransition(
        scale: _pulseAnimation,
        child: Container(
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(24),
            boxShadow: [
              BoxShadow(
                color: AppColors.primary.withOpacity(0.15),
                blurRadius: 20,
                spreadRadius: 2,
              ),
            ],
          ),
          child: const Padding(
            padding: EdgeInsets.all(8),
            child: CircularProgressIndicator(
              strokeWidth: 3,
              valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
            ),
          ),
        ),
      ),
    );
  }
}

