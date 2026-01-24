import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import '../services/truecaller_service.dart';
import '../services/onesignal_service.dart';
import 'otp_verification_page.dart';
import 'guest_login_page.dart';
import 'profile_setup_page.dart';
import 'exam_selection_page.dart';
import 'home_page.dart';
import 'subscription_offer_page.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({Key? key}) : super(key: key);

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  // WhatsApp support number
  static const String _whatsappNumber = '918300321814';
  static const String _displayNumber = '+91 83003 21814';

  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController();
  bool _isLoading = false;
  // Guest Login should be hidden by default and only shown when enabled via API setting.
  bool _guestLoginEnabled = false;
  
  // Truecaller state
  bool _truecallerUsable = false;
  bool _truecallerLoading = false;
  
  // Slow connection indicator
  bool _showSlowConnection = false;
  Timer? _slowConnectionTimer;

  @override
  void initState() {
    super.initState();
    _loadGuestLoginEnabled();
    _initTruecaller();
  }

  @override
  void dispose() {
    _phoneController.dispose();
    _slowConnectionTimer?.cancel();
    super.dispose();
  }

  /// Initialize Truecaller - check if usable and auto-trigger bottom sheet
  Future<void> _initTruecaller() async {
    try {
      final isUsable = await TruecallerService.isUsable();
      if (!mounted) return;
      
      setState(() => _truecallerUsable = isUsable);
      print('LoginPage: Truecaller isUsable = $isUsable');
      
      // Auto-trigger Truecaller bottom sheet if available
      if (isUsable) {
        // Small delay to let the UI settle
        await Future.delayed(const Duration(milliseconds: 500));
        if (mounted) {
          _handleTruecallerLogin();
        }
      }
    } catch (e) {
      print('LoginPage: Error initializing Truecaller - $e');
    }
  }

  /// Handle Truecaller login flow (separate from OTP)
  Future<void> _handleTruecallerLogin() async {
    if (_truecallerLoading || _isLoading) return;
    
    setState(() => _truecallerLoading = true);
    
    try {
      final result = await TruecallerService.requestProfile();
      
      if (!mounted) return;
      
      setState(() => _truecallerLoading = false);
      
      if (result.isSuccess && result.accessToken != null) {
        // Got access token from Truecaller - call backend to verify and login
        print('LoginPage: Truecaller success, calling backend login...');
        await _processTruecallerLogin(result.accessToken!);
      } else if (result.isCancelled) {
        // User cancelled or closed Truecaller - stay on login page
        print('LoginPage: Truecaller cancelled/closed');
      } else {
        // Other error - show message
        print('LoginPage: Truecaller error: ${result.message}');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Row(
                children: [
                  const Icon(Icons.error_outline, color: Colors.white, size: 20),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      result.message ?? 'Truecaller verification failed',
                      style: GoogleFonts.poppins(),
                    ),
                  ),
                ],
              ),
              backgroundColor: AppColors.error,
              behavior: SnackBarBehavior.floating,
              margin: const EdgeInsets.all(16),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
          );
        }
      }
    } catch (e) {
      print('LoginPage: Exception in Truecaller login - $e');
      if (mounted) {
        setState(() => _truecallerLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Truecaller verification failed. Please try again.',
              style: GoogleFonts.poppins(),
            ),
            backgroundColor: AppColors.error,
            behavior: SnackBarBehavior.floating,
            margin: const EdgeInsets.all(16),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          ),
        );
      }
    }
  }

  /// Cancel the current login attempt
  void _cancelLogin() {
    _slowConnectionTimer?.cancel();
    setState(() {
      _isLoading = false;
      _truecallerLoading = false;
      _showSlowConnection = false;
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Login cancelled', style: GoogleFonts.poppins()),
        backgroundColor: AppColors.textSecondary,
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    );
  }

  /// Process Truecaller login with backend verification
  Future<void> _processTruecallerLogin(String accessToken) async {
    setState(() {
      _isLoading = true;
      _showSlowConnection = false;
    });
    
    // Start slow connection timer (show message after 8 seconds)
    _slowConnectionTimer?.cancel();
    _slowConnectionTimer = Timer(const Duration(seconds: 8), () {
      if (mounted && _isLoading) {
        setState(() => _showSlowConnection = true);
      }
    });
    
    try {
      final response = await ApiService.truecallerLogin(accessToken: accessToken);
      
      _slowConnectionTimer?.cancel();
      if (!mounted) return;
      
      setState(() {
        _isLoading = false;
        _showSlowConnection = false;
      });
      
      if (response['success'] == true) {
        final isNewUser = response['is_new_user'] == true;
        final token = response['token'];
        final mobile = response['mobile'] ?? '';
        
        // Show success message
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.check_circle, color: Colors.white),
                const SizedBox(width: 12),
                Text(
                  'Truecaller verified successfully!',
                  style: GoogleFonts.poppins(fontWeight: FontWeight.w500),
                ),
              ],
            ),
            backgroundColor: AppColors.success,
            behavior: SnackBarBehavior.floating,
            margin: const EdgeInsets.all(16),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          ),
        );
        
        if (isNewUser) {
          // New user - go to profile setup (registration)
          Navigator.pushAndRemoveUntil(
            context,
            MaterialPageRoute(
              builder: (context) => ProfileSetupPage(
                mobileNumber: mobile,
                isNewUser: true,
                token: token,
                verificationMethod: 'truecaller', // User verified via Truecaller
              ),
            ),
            (route) => false,
          );
        } else {
          // Existing user - save data and navigate
          final prefs = await SharedPreferences.getInstance();
          final user = response['user'];
          
          // Convert id to int (handles both String and int types)
          final userId = user['id'] is int ? user['id'] : int.parse(user['id'].toString());
          
          await prefs.setInt('userId', userId);
          await prefs.setString('userName', user['name'] ?? '');
          await prefs.setString('userMobile', user['mobile'] ?? '');
          await prefs.setString('token', token ?? '');
          await prefs.setBool('isLoggedIn', true);
          
          // Set OneSignal user ID for push notifications
          await OneSignalService.setUserId(userId.toString());
          
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
        }
      } else {
        // Backend verification failed
        final errorMessage = response['message'] ?? 'Truecaller login failed';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.error_outline, color: Colors.white, size: 20),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    errorMessage,
                    style: GoogleFonts.poppins(),
                  ),
                ),
              ],
            ),
            backgroundColor: AppColors.error,
            behavior: SnackBarBehavior.floating,
            margin: const EdgeInsets.all(16),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          ),
        );
      }
    } catch (e) {
      print('LoginPage: Error in Truecaller backend login - $e');
      _slowConnectionTimer?.cancel();
      if (mounted) {
        setState(() {
          _isLoading = false;
          _showSlowConnection = false;
        });
        
        // Check if it's a timeout error
        final isTimeout = e.toString().contains('TimeoutException') || 
                          e.toString().contains('timeout');
        
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                Icon(
                  isTimeout ? Icons.wifi_off : Icons.error_outline,
                  color: Colors.white,
                  size: 20,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    isTimeout 
                        ? 'Connection timeout. Please check your internet and try again.'
                        : 'Login failed. Please try again.',
                    style: GoogleFonts.poppins(),
                  ),
                ),
              ],
            ),
            backgroundColor: AppColors.error,
            behavior: SnackBarBehavior.floating,
            margin: const EdgeInsets.all(16),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            duration: const Duration(seconds: 4),
            action: SnackBarAction(
              label: 'RETRY',
              textColor: Colors.white,
              onPressed: () => _processTruecallerLogin(accessToken),
            ),
          ),
        );
      }
    }
  }

  /// Check premium status and navigate accordingly (same logic as OTPVerificationPage)
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

  Future<void> _loadGuestLoginEnabled() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final cached = prefs.getString('guest_login_enabled');
      bool? cachedEnabled;
      if (cached != null) {
        final v = cached.trim().toLowerCase();
        cachedEnabled = (v == '1' || v == 'true' || v == 'yes' || v == 'on');
      }

      // Apply cached value immediately so UI doesn't wait for the API call.
      if (cachedEnabled != null && mounted) {
        setState(() => _guestLoginEnabled = cachedEnabled!);
      }

      final res = await ApiService.getPublicSettings(key: 'guest_login_enabled');
      if (!mounted) return;

      if (res['success'] == true) {
        final v = (res['value'] ?? '').toString().trim().toLowerCase();
        final enabled = (v == '1' || v == 'true' || v == 'yes' || v == 'on');
        await prefs.setString('guest_login_enabled', enabled ? '1' : '0');
        setState(() => _guestLoginEnabled = enabled);
      }
    } catch (_) {
      // ignore - keep default
    }
  }

  /// Opens WhatsApp with the support number
  Future<void> _openWhatsAppSupport() async {
    final whatsappUrl = Uri.parse(
      'https://wa.me/$_whatsappNumber?text=${Uri.encodeComponent("Hi, I need help with login/OTP in the Sudar TNPSC App.")}',
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
            Expanded(
              child: Text(
                'WhatsApp Not Found',
                style: GoogleFonts.poppins(fontWeight: FontWeight.bold, fontSize: 18),
              ),
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
              if (context.mounted) {
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Number copied to clipboard!', style: GoogleFonts.poppins()),
                    backgroundColor: AppColors.success,
                    behavior: SnackBarBehavior.floating,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  ),
                );
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

  /// Handle mobile number OTP login (original flow)
  void _handleLogin() async {
    if (_formKey.currentState!.validate()) {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => OTPVerificationPage(
            phoneNumber: _phoneController.text.trim(),
            // Send OTP only after OTP screen opens (ensures auto-fill listeners are started).
            sendOtpOnInit: true,
          ),
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
        backgroundColor: Colors.transparent,
        elevation: 0,
        automaticallyImplyLeading: false,
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
      body: Stack(
        children: [
          // Main content
          Center(
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
                    // Logo and Title Section
                    _buildHeader(),
                    
                    const SizedBox(height: 48),
                    
                    // Welcome Text
                    _buildWelcomeText(),
                    
                    const SizedBox(height: 32),
                    
                    // Login Form (Mobile OTP)
                    _buildLoginForm(),
                    
                    const SizedBox(height: 24),
                    
                    // Mobile OTP Login Button
                    _buildLoginButton(),
                    
                    // Guest Login Button (Temporary for Google Reviews)
                    if (_guestLoginEnabled) ...[
                      const SizedBox(height: 16),
                      _buildGuestLoginButton(),
                    ],
                  ],
                ),
              ),
            ),
          ),
          
          // Loading overlay for Truecaller/Login verification
          if (_truecallerLoading || _isLoading)
            Container(
              color: Colors.black.withOpacity(0.5),
              child: Center(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 40, vertical: 24),
                  margin: const EdgeInsets.all(24),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.15),
                        blurRadius: 20,
                        spreadRadius: 5,
                      ),
                    ],
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const SizedBox(
                        width: 40,
                        height: 40,
                        child: CircularProgressIndicator(
                          strokeWidth: 3,
                          valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        _truecallerLoading ? 'Verifying...' : 'Logging in...',
                        style: GoogleFonts.poppins(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _showSlowConnection 
                            ? 'Slow connection, please wait...'
                            : 'Please wait',
                        style: GoogleFonts.poppins(
                          fontSize: 13,
                          color: _showSlowConnection ? AppColors.warning : AppColors.textSecondary,
                        ),
                      ),
                      // Show cancel button when loading takes too long
                      if (_isLoading && !_truecallerLoading) ...[
                        const SizedBox(height: 16),
                        TextButton(
                          onPressed: _cancelLogin,
                          child: Text(
                            'Cancel',
                            style: GoogleFonts.poppins(
                              fontSize: 14,
                              fontWeight: FontWeight.w500,
                              color: AppColors.error,
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      children: [
        const SizedBox(height: 20),
        
        // Main Logo with elegant glow effect
        Container(
          width: 140,
          height: 140,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(28),
            boxShadow: [
              BoxShadow(
                color: AppColors.primary.withOpacity(0.25),
                blurRadius: 40,
                spreadRadius: 5,
                offset: const Offset(0, 15),
              ),
              BoxShadow(
                color: Colors.white,
                blurRadius: 20,
                spreadRadius: -5,
              ),
            ],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(28),
            child: Image.asset(
              'assets/images/logo.png',
              fit: BoxFit.contain,
            ),
          ),
        ),
        const SizedBox(height: 28),
        
        // Title with gradient effect
        ShaderMask(
          shaderCallback: (bounds) => const LinearGradient(
            colors: [AppColors.primary, Color(0xFFFF6B6B)],
          ).createShader(bounds),
          child: Text(
            'SUDAR',
            style: GoogleFonts.poppins(
              fontSize: 36,
              fontWeight: FontWeight.w800,
              color: Colors.white,
              letterSpacing: 4,
            ),
          ),
        ),
        const SizedBox(height: 8),
        
        // Subtitle with badge style
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          decoration: BoxDecoration(
            color: AppColors.primary.withOpacity(0.08),
            borderRadius: BorderRadius.circular(20),
          ),
          child: Text(
            'TNPSC Mock Test App',
            style: GoogleFonts.poppins(
              fontSize: 14,
              color: AppColors.primary,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.5,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildWelcomeText() {
    return Column(
      children: [
        const Divider(color: AppColors.border, height: 1),
        const SizedBox(height: 24),
        Text(
          'Log in/Sign up',
          style: GoogleFonts.poppins(
            fontSize: 16,
            color: AppColors.textSecondary,
            fontWeight: FontWeight.w400,
          ),
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
          Text(
            'Mobile Number',
            style: GoogleFonts.poppins(
              fontSize: 13,
              fontWeight: FontWeight.w500,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _phoneController,
            keyboardType: TextInputType.phone,
            maxLength: 10,
            style: GoogleFonts.poppins(fontSize: 15),
            decoration: InputDecoration(
              hintText: 'Enter Your 10 digit Mobile no.',
              hintStyle: GoogleFonts.poppins(
                color: AppColors.textLight,
                fontSize: 14,
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
              counterText: '',
            ),
            onChanged: (value) {
              setState(() {}); // Rebuild to update button color
            },
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter your mobile number';
              }
              if (value.length != 10) {
                return 'Please enter a valid 10-digit mobile number';
              }
              if (!RegExp(r'^[0-9]+$').hasMatch(value)) {
                return 'Please enter only numbers';
              }
              return null;
            },
          ),
        ],
      ),
    );
  }

  Widget _buildLoginButton() {
    bool isValid = _phoneController.text.length == 10;
    
    return ElevatedButton(
      onPressed: (_isLoading || _truecallerLoading) ? null : _handleLogin,
      style: ElevatedButton.styleFrom(
        backgroundColor: isValid ? AppColors.primary : const Color(0xFFBDBDBD),
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
                const Icon(Icons.sms_outlined, size: 20),
                const SizedBox(width: 8),
                Text(
                  'CONTINUE WITH OTP',
                  style: GoogleFonts.poppins(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.5,
                  ),
                ),
              ],
            ),
    );
  }

  Widget _buildGuestLoginButton() {
    return OutlinedButton(
      onPressed: (_isLoading || _truecallerLoading) ? null : () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => const GuestLoginPage(),
          ),
        );
      },
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.primary,
        side: const BorderSide(color: AppColors.primary, width: 1.5),
        padding: const EdgeInsets.symmetric(vertical: 16),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.person_outline, size: 20),
          const SizedBox(width: 8),
          Text(
            'Guest Login (U015)',
            style: GoogleFonts.poppins(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.5,
            ),
          ),
        ],
      ),
    );
  }

}
