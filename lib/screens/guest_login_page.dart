import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import '../services/onesignal_service.dart';
import 'exam_selection_page.dart';
import 'home_page.dart';
import 'subscription_offer_page.dart';

class GuestLoginPage extends StatefulWidget {
  const GuestLoginPage({Key? key}) : super(key: key);

  @override
  State<GuestLoginPage> createState() => _GuestLoginPageState();
}

class _GuestLoginPageState extends State<GuestLoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();

  /// Check premium status and navigate accordingly
  Future<void> _navigateAfterLogin(BuildContext context, SharedPreferences prefs, String selectedExam) async {
    bool isPremium = prefs.getBool('is_premium') ?? false;

    // Best-effort refresh from server
    if (!isPremium) {
      final userId = prefs.getInt('user_id') ?? prefs.getInt('userId');
      if (userId != null) {
        try {
          final url = '${ApiService.baseUrl}/subscriptions/status.php?user_id=$userId';
          final res = await http.get(Uri.parse(url)).timeout(const Duration(seconds: 10));
          if (res.statusCode == 200) {
            final data = jsonDecode(res.body);
            isPremium = data['is_premium'] == true;
            await prefs.setBool('is_premium', isPremium);
          }
        } catch (_) {
          // ignore network errors; use cached value
        }
      }
    }

    if (!isPremium) {
      // Show subscription offer first
      final result = await Navigator.push<bool>(
        context,
        MaterialPageRoute(builder: (_) => const SubscriptionOfferPage(useVideoHero: true)),
      );
      // Update premium status if subscribed
      if (result == true) {
        await prefs.setBool('is_premium', true);
      }
    }

    // Now go to home page
    if (context.mounted) {
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(
          builder: (context) => HomePage(selectedExam: selectedExam),
        ),
        (route) => false,
      );
    }
  }
  bool _isLoading = false;
  bool _obscurePassword = true;

  // Hardcoded guest credentials (no API, no admin)
  static const String guestUsername = 'U015';
  static const String guestPassword = 'guest123'; // You can change this password

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _handleGuestLogin() async {
    if (_formKey.currentState!.validate()) {
      final username = _usernameController.text.trim();
      final password = _passwordController.text.trim();

      // Simple hardcoded validation (no API)
      if (username == guestUsername && password == guestPassword) {
        setState(() => _isLoading = true);

        // Simulate a small delay
        await Future.delayed(const Duration(milliseconds: 500));

        if (!mounted) return;

        setState(() => _isLoading = false);

        // Save guest login data
        final prefs = await SharedPreferences.getInstance();
        await prefs.setInt('userId', 15); // U015 = user ID 15
        await prefs.setString('userName', 'Guest User');
        await prefs.setString('userMobile', '0000000000');
        await prefs.setString('token', 'guest_token_${DateTime.now().millisecondsSinceEpoch}');
        await prefs.setBool('isLoggedIn', true);
        await prefs.setBool('isGuestUser', true); // Mark as guest user
        
        // Set OneSignal user ID for push notifications
        await OneSignalService.setUserId('15');

        // Check if exam is already selected
        final selectedExam = prefs.getString('selectedExam');
        final selectedExamId = prefs.getInt('selectedExamId');

        if (selectedExam != null && selectedExamId != null) {
          // Exam already selected, check premium and navigate
          await _navigateAfterLogin(context, prefs, selectedExam);
        } else {
          // First time login, show exam selection
          Navigator.pushAndRemoveUntil(
            context,
            MaterialPageRoute(
              builder: (context) => const ExamSelectionPage(),
            ),
            (route) => false,
          );
        }
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Invalid username or password',
              style: GoogleFonts.poppins(),
            ),
            backgroundColor: AppColors.error,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final isMobile = size.width < 600;

    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, size: 24),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Guest Login',
          style: GoogleFonts.poppins(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
        centerTitle: false,
        elevation: 0,
        backgroundColor: ThemeHelper.cardColor(context),
      ),
      body: Center(
        child: SingleChildScrollView(
          child: Container(
            constraints: const BoxConstraints(maxWidth: 450),
            padding: EdgeInsets.symmetric(
              horizontal: isMobile ? 24 : 40,
              vertical: 32,
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Header Section
                _buildHeader(),

                const SizedBox(height: 48),

                // Login Form
                _buildLoginForm(),

                const SizedBox(height: 24),

                // Login Button
                _buildLoginButton(),

                const SizedBox(height: 16),

                // Info Text
                _buildInfoText(),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      children: [
        // Icon
        Container(
          width: 80,
          height: 80,
          decoration: BoxDecoration(
            color: AppColors.primary.withOpacity(0.1),
            shape: BoxShape.circle,
          ),
          child: Icon(
            Icons.person_outline,
            size: 40,
            color: AppColors.primary,
          ),
        ),
        const SizedBox(height: 24),

        // Title
        Text(
          'Guest Login',
          style: GoogleFonts.poppins(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 8),

        // Subtitle
        Text(
          'Enter your guest credentials to continue',
          style: GoogleFonts.poppins(
            fontSize: 14,
            color: AppColors.textSecondary,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildLoginForm() {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Username Field
          Text(
            'Username',
            style: GoogleFonts.poppins(
              fontSize: 13,
              fontWeight: FontWeight.w500,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _usernameController,
            keyboardType: TextInputType.text,
            textCapitalization: TextCapitalization.characters,
            style: GoogleFonts.poppins(fontSize: 15),
            decoration: InputDecoration(
              hintText: 'Enter Username (e.g., U015)',
              hintStyle: GoogleFonts.poppins(
                color: AppColors.textLight,
                fontSize: 14,
              ),
              prefixIcon: const Icon(Icons.person_outline, color: AppColors.textLight),
              filled: true,
              fillColor: const Color(0xFFF5F5F5),
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: BorderSide.none,
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: BorderSide.none,
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: const BorderSide(color: AppColors.primary, width: 1),
              ),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter username';
              }
              return null;
            },
          ),

          const SizedBox(height: 24),

          // Password Field
          Text(
            'Password',
            style: GoogleFonts.poppins(
              fontSize: 13,
              fontWeight: FontWeight.w500,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _passwordController,
            obscureText: _obscurePassword,
            style: GoogleFonts.poppins(fontSize: 15),
            decoration: InputDecoration(
              hintText: 'Enter Password',
              hintStyle: GoogleFonts.poppins(
                color: AppColors.textLight,
                fontSize: 14,
              ),
              prefixIcon: const Icon(Icons.lock_outline, color: AppColors.textLight),
              suffixIcon: IconButton(
                icon: Icon(
                  _obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                  color: AppColors.textLight,
                ),
                onPressed: () {
                  setState(() {
                    _obscurePassword = !_obscurePassword;
                  });
                },
              ),
              filled: true,
              fillColor: const Color(0xFFF5F5F5),
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: BorderSide.none,
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: BorderSide.none,
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: const BorderSide(color: AppColors.primary, width: 1),
              ),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter password';
              }
              return null;
            },
          ),
        ],
      ),
    );
  }

  Widget _buildLoginButton() {
    return ElevatedButton(
      onPressed: _isLoading ? null : _handleGuestLogin,
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(vertical: 18),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
        elevation: 0,
      ),
      child: _isLoading
          ? const SizedBox(
              height: 20,
              width: 20,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
              ),
            )
          : Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'LOGIN',
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
    );
  }

  Widget _buildInfoText() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.primary.withOpacity(0.1),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          Icon(
            Icons.info_outline,
            size: 20,
            color: AppColors.primary,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'Default credentials: Username: $guestUsername, Password: $guestPassword',
              style: GoogleFonts.poppins(
                fontSize: 12,
                color: AppColors.textSecondary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

