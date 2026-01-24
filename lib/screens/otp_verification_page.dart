import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:sms_autofill/sms_autofill.dart';
import 'package:url_launcher/url_launcher.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import '../services/onesignal_service.dart';
import 'profile_setup_page.dart';
import 'exam_selection_page.dart';
import 'home_page.dart';
import 'subscription_offer_page.dart';

class OTPVerificationPage extends StatefulWidget {
  final String phoneNumber;
  final bool sendOtpOnInit;
  
  const OTPVerificationPage({
    Key? key,
    required this.phoneNumber,
    this.sendOtpOnInit = false,
  }) : super(key: key);

  @override
  State<OTPVerificationPage> createState() => _OTPVerificationPageState();
}

class _OTPVerificationPageState extends State<OTPVerificationPage> with CodeAutoFill {
  // WhatsApp support number
  static const String _whatsappNumber = '918300321814';
  static const String _displayNumber = '+91 83003 21814';
  static const MethodChannel _smsUserConsentChannel =
      MethodChannel('com.sudar.tnpscapp/sms_user_consent');

  final List<TextEditingController> _otpControllers = List.generate(
    6,
    (index) => TextEditingController(),
  );
  final List<FocusNode> _focusNodes = List.generate(
    6,
    (index) => FocusNode(),
  );
  
  bool _isLoading = false;
  bool _isSendingOtp = false;
  int _resendTimer = 60; // Match server cooldown (60 seconds)
  bool _canResend = false;
  bool _isResending = false;
  int _remainingAttempts = 5; // Track remaining attempts
  Timer? _countdownTimer;
  
  // SMS Retriever state
  String? _appSignature;
  bool _smsUserConsentInitialized = false;

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

  @override
  void initState() {
    super.initState();
    _startResendTimer();
    _initSmsRetriever();
    // Ensure listeners are started before requesting OTP.
    Future.microtask(() async {
      await _initSmsUserConsent();
      if (widget.sendOtpOnInit) {
        await _sendInitialOtp();
      }
    });
  }

  @override
  void dispose() {
    _countdownTimer?.cancel();
    // Stop SMS Retriever listener
    cancel();
    SmsAutoFill().unregisterListener();
    if (_smsUserConsentInitialized) {
      // Best-effort stop native SMS consent listener
      unawaited(_smsUserConsentChannel.invokeMethod('stop'));
      _smsUserConsentChannel.setMethodCallHandler(null);
    }
    for (final c in _otpControllers) {
      c.dispose();
    }
    for (final n in _focusNodes) {
      n.dispose();
    }
    super.dispose();
  }
  
  /// Initialize SMS Retriever to auto-read OTP
  Future<void> _initSmsRetriever() async {
    // Only available on Android
    if (kIsWeb || !Platform.isAndroid) {
      print('OTPVerificationPage: SMS Retriever not available on this platform');
      return;
    }
    
    try {
      // Get app signature for SMS template (needed by AuthKey.io)
      _appSignature = await SmsAutoFill().getAppSignature;
      print('OTPVerificationPage: App signature for SMS Retriever: $_appSignature');
      
      // Start listening for SMS code (returns void, not Future)
      listenForCode();
      print('OTPVerificationPage: SMS Retriever listener started');
    } catch (e) {
      print('OTPVerificationPage: Error initializing SMS Retriever - $e');
    }
  }

  /// Initialize Android SMS User Consent API (works even if you can't change SMS format).
  /// Shows a one-time system consent dialog, then returns the SMS text to the app.
  Future<void> _initSmsUserConsent() async {
    if (kIsWeb || !Platform.isAndroid) return;

    if (!_smsUserConsentInitialized) {
      _smsUserConsentChannel.setMethodCallHandler((call) async {
        if (!mounted) return;

        if (call.method == 'onSmsReceived') {
          final msg = call.arguments as String?;
          if (msg == null || msg.isEmpty) return;
          final otpMatch = RegExp(r'\d{6}').firstMatch(msg);
          if (otpMatch != null) {
            final otp = otpMatch.group(0)!;
            _setOtp(otp, autoSubmit: true);
          }
        }
      });
      _smsUserConsentInitialized = true;
    }

    try {
      await _smsUserConsentChannel.invokeMethod('start');
      print('OTPVerificationPage: SMS User Consent started');
    } catch (e) {
      // Not fatal; keyboard autofill may still work.
      print('OTPVerificationPage: SMS User Consent start failed - $e');
    }
  }
  
  /// Called when SMS code is received by SMS Retriever
  @override
  void codeUpdated() {
    print('OTPVerificationPage: codeUpdated called with code: $code');
    if (code != null && code!.isNotEmpty) {
      // Extract 6-digit OTP from the SMS
      final otpMatch = RegExp(r'\d{6}').firstMatch(code!);
      if (otpMatch != null) {
        final otp = otpMatch.group(0)!;
        print('OTPVerificationPage: Extracted OTP: $otp');
        _setOtp(otp, autoSubmit: true);
        
        // Show brief notification
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Row(
                children: [
                  const Icon(Icons.auto_awesome, color: Colors.white, size: 20),
                  const SizedBox(width: 12),
                  Text(
                    'OTP auto-filled',
                    style: GoogleFonts.poppins(fontWeight: FontWeight.w500),
                  ),
                ],
              ),
              backgroundColor: AppColors.success,
              behavior: SnackBarBehavior.floating,
              margin: const EdgeInsets.all(16),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              duration: const Duration(seconds: 2),
            ),
          );
        }
      }
    }
  }

  void _startResendTimer([int? customDuration]) {
    _countdownTimer?.cancel();
    
    setState(() {
      _resendTimer = customDuration ?? 60;
      _canResend = false;
    });

    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }
      if (_resendTimer > 0) {
        setState(() {
          _resendTimer--;
        });
      } else {
        timer.cancel();
        setState(() {
          _canResend = true;
        });
      }
    });
  }
  
  void _clearOTPFields() {
    for (final c in _otpControllers) {
      c.clear();
    }
    setState(() {});
    if (_focusNodes.isNotEmpty) {
      _focusNodes[0].requestFocus();
    }
  }

  void _setOtp(String otp, {bool autoSubmit = false}) {
    if (otp.length != 6) return;
    for (int i = 0; i < 6; i++) {
      _otpControllers[i].text = otp[i];
    }
    setState(() {});
    if (!mounted) return;
    FocusScope.of(context).unfocus();
    if (autoSubmit) {
      Future.microtask(() {
        if (mounted) _handleVerify();
      });
    }
  }

  Future<void> _handleVerify() async {
    final otp = _otpControllers.map((c) => c.text).join().replaceAll(RegExp(r'\\D'), '');

    if (otp.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Please enter complete 6-digit OTP',
            style: GoogleFonts.poppins(),
          ),
          backgroundColor: AppColors.error,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.all(16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        ),
      );
      return;
    }

    setState(() => _isLoading = true);

    final response = await ApiService.verifyOTP(widget.phoneNumber, otp);

    if (!mounted) return;

    setState(() => _isLoading = false);

    if (response['success'] == true) {
      final isNewUser = response['is_new_user'] == true;
      final token = response['token'];

      // Show success animation
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.check_circle, color: Colors.white),
              const SizedBox(width: 12),
              Text(
                'OTP verified successfully!',
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
        // New user - go to profile setup
        Navigator.pushAndRemoveUntil(
          context,
          MaterialPageRoute(
            builder: (context) => ProfileSetupPage(
              mobileNumber: widget.phoneNumber,
              isNewUser: isNewUser,
              token: token,
              userData: response['user'],
              verificationMethod: 'otp', // User verified via SMS OTP
            ),
          ),
          (route) => false,
        );
      } else {
        // Existing user - save data and check if exam is already selected
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
      // Handle different error types
      final errorCode = response['error_code'];
      String errorMessage = response['message'] ?? 'Invalid or expired OTP';
      IconData errorIcon = Icons.error_outline;
      
      if (errorCode == 'INVALID_OTP') {
        // Update remaining attempts
        final remaining = response['remaining_attempts'];
        if (remaining != null) {
          setState(() => _remainingAttempts = remaining);
          if (remaining <= 2) {
            errorMessage = '$errorMessage\n⚠️ Only $remaining attempts left!';
          }
        }
        errorIcon = Icons.lock_outline;
        _clearOTPFields(); // Clear fields on wrong OTP
      } else if (errorCode == 'MAX_ATTEMPTS_EXCEEDED') {
        errorIcon = Icons.block;
        _clearOTPFields();
      } else if (errorCode == 'NO_ACTIVE_OTP') {
        errorIcon = Icons.timer_off;
      }
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              Icon(errorIcon, color: Colors.white),
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
          duration: const Duration(seconds: 4),
        ),
      );
    }
  }

  Future<void> _sendInitialOtp() async {
    if (_isSendingOtp) return;
    setState(() => _isSendingOtp = true);

    final response = await ApiService.sendOTP(widget.phoneNumber);

    if (!mounted) return;
    setState(() => _isSendingOtp = false);

    if (response['success'] == true) {
      final maskedMobile = response['mobile'] ?? widget.phoneNumber;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.sms, color: Colors.white, size: 20),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  'OTP sent to $maskedMobile',
                  style: GoogleFonts.poppins(fontWeight: FontWeight.w500),
                ),
              ),
            ],
          ),
          backgroundColor: AppColors.success,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.all(16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        ),
      );
    } else {
      final errorMessage = response['message'] ?? 'Failed to send OTP';

      // If initial send failed, allow user to retry immediately.
      _countdownTimer?.cancel();
      setState(() {
        _resendTimer = 0;
        _canResend = true;
      });

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.white, size: 20),
              const SizedBox(width: 12),
              Expanded(child: Text(errorMessage, style: GoogleFonts.poppins())),
            ],
          ),
          backgroundColor: AppColors.error,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.all(16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          duration: const Duration(seconds: 4),
        ),
      );
    }
  }

  void _handleOtpChanged(int index, String value) {
    final digits = value.replaceAll(RegExp(r'\\D'), '');

    // Handle full-code autofill/paste (some keyboards insert the whole OTP at once,
    // and it might happen while any box is focused).
    if (digits.length >= 6) {
      _setOtp(digits.substring(0, 6), autoSubmit: true);
      return;
    }

    // Paste support: if user pasted multiple digits into a box.
    if (digits.length > 1) {
      final remainingSlots = 6 - index;
      final take = digits.length > remainingSlots ? remainingSlots : digits.length;
      for (int i = 0; i < take; i++) {
        _otpControllers[index + i].text = digits[i];
      }
      setState(() {});
      final nextIndex = (index + take).clamp(0, 5);
      _focusNodes[nextIndex].requestFocus();
      return;
    }

    if (value.isNotEmpty) {
      if (index < 5) {
        _focusNodes[index + 1].requestFocus();
      } else {
        FocusScope.of(context).unfocus();
      }
    } else {
      if (index > 0) {
        _focusNodes[index - 1].requestFocus();
      }
    }
    setState(() {});
  }

  Future<void> _handleResend() async {
    if (!_canResend || _isResending) return;

    setState(() => _isResending = true);
    // Restart SMS consent listener (it times out after a few minutes).
    await _initSmsUserConsent();

    // Use dedicated resend endpoint
    final response = await ApiService.resendOTP(widget.phoneNumber);

    if (!mounted) return;

    setState(() => _isResending = false);

    if (response['success'] == true) {
      // Reset attempts on successful resend
      setState(() => _remainingAttempts = 5);
      
      // Clear OTP fields for new entry
      _clearOTPFields();
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.sms, color: Colors.white),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  'New OTP sent to ${response['mobile'] ?? widget.phoneNumber}',
                  style: GoogleFonts.poppins(fontWeight: FontWeight.w500),
                ),
              ),
            ],
          ),
          backgroundColor: AppColors.success,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.all(16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        ),
      );
      
      // Start timer with server-provided cooldown or default 60s
      final resendIn = response['resend_in'] ?? 60;
      _startResendTimer(resendIn);
    } else {
      final errorCode = response['error_code'];
      String errorMessage = response['message'] ?? 'Failed to resend OTP';
      IconData errorIcon = Icons.error_outline;
      
      if (errorCode == 'COOLDOWN_ACTIVE' || errorCode == 'RATE_LIMITED') {
        errorIcon = Icons.timer;
        // Update timer with server-provided retry time
        final retryAfter = response['retry_after'];
        if (retryAfter != null && retryAfter > 0) {
          _startResendTimer(retryAfter);
        }
      } else if (errorCode == 'RESEND_LIMIT_EXCEEDED') {
        errorIcon = Icons.block;
        errorMessage = 'Maximum resend limit reached.\nPlease try again later.';
      }
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              Icon(errorIcon, color: Colors.white),
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
          duration: const Duration(seconds: 4),
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
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, size: 24),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Sign Up',
          style: GoogleFonts.poppins(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
        centerTitle: false,
        elevation: 0,
        backgroundColor: ThemeHelper.cardColor(context),
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
                
                // OTP Input Fields
                _buildOTPFields(),
                
                const SizedBox(height: 24),
                
                // Resend Section
                _buildResendSection(),
                
                const SizedBox(height: 32),
                
                // Verify Button
                _buildVerifyButton(),
                
                const SizedBox(height: 24),
                
                // Help Text
                _buildHelpText(),
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
        // Title
        Text(
          'OTP Verification',
          style: GoogleFonts.poppins(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 16),
        
        // Subtitle
        RichText(
          textAlign: TextAlign.center,
          text: TextSpan(
            text: 'Enter the code from the sms we sent to\n',
            style: GoogleFonts.poppins(
              fontSize: 14,
              color: AppColors.textSecondary,
            ),
            children: [
              TextSpan(
                text: '+91 ${widget.phoneNumber}',
                style: GoogleFonts.poppins(
                  fontWeight: FontWeight.w600,
                  color: ThemeHelper.textPrimary(context),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildOTPFields() {
    return AutofillGroup(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: List.generate(6, (index) {
          final hasValue = _otpControllers[index].text.isNotEmpty;
          return Container(
            width: 48,
            height: 56,
            margin: EdgeInsets.only(
              left: index == 0 ? 0 : 6,
              right: index == 5 ? 0 : 6,
            ),
            decoration: BoxDecoration(
              color: ThemeHelper.cardColor(context),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: hasValue ? AppColors.primary : const Color(0xFFE0E0E0),
                width: 1,
              ),
            ),
            alignment: Alignment.center,
            child: TextField(
              controller: _otpControllers[index],
              focusNode: _focusNodes[index],
              keyboardType: TextInputType.number,
              textAlign: TextAlign.center,
              maxLength: 1,
              // Allow iOS/Android keyboards to insert the full OTP into any box.
              // `_handleOtpChanged` splits it across all 6 fields.
              maxLengthEnforcement: MaxLengthEnforcement.none,
              autofillHints: const [AutofillHints.oneTimeCode],
              autofocus: index == 0,
              style: GoogleFonts.poppins(
                fontSize: 24,
                fontWeight: FontWeight.w700,
                color: ThemeHelper.textPrimary(context),
              ),
              decoration: const InputDecoration(
                counterText: '',
                border: InputBorder.none,
                contentPadding: EdgeInsets.zero,
              ),
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              onChanged: (v) => _handleOtpChanged(index, v),
            ),
          );
        }),
      ),
    );
  }

  Widget _buildResendSection() {
    return Center(
      child: InkWell(
        onTap: _handleResend,
        child: RichText(
          text: TextSpan(
            text: 'Didn\'t receive the OTP? ',
            style: GoogleFonts.poppins(
              fontSize: 13,
              color: AppColors.textSecondary,
            ),
            children: [
              TextSpan(
                text: _canResend ? (_isResending ? 'Sending...' : 'Resend') : 'Resend in ${_resendTimer}s',
                style: GoogleFonts.poppins(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: _canResend ? AppColors.primary : AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildVerifyButton() {
    bool isComplete = _otpControllers.every((c) => c.text.isNotEmpty);
    
    return ElevatedButton(
      onPressed: _isLoading ? null : _handleVerify,
      style: ElevatedButton.styleFrom(
        backgroundColor: isComplete ? AppColors.primary : const Color(0xFFBDBDBD),
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
                  'SUBMIT',
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

  /// Opens WhatsApp with the support number
  Future<void> _openWhatsAppSupport() async {
    final whatsappUrl = Uri.parse(
      'https://wa.me/$_whatsappNumber?text=${Uri.encodeComponent("Hi, I need help with OTP verification for mobile number ${widget.phoneNumber} in the Sudar TNPSC App.")}',
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

  Widget _buildHelpText() {
    return RichText(
      textAlign: TextAlign.center,
      text: TextSpan(
        text: 'By continuing, you agree with our\n',
        style: GoogleFonts.poppins(
          fontSize: 11,
          color: AppColors.textSecondary,
        ),
        children: [
          TextSpan(
            text: 'Terms of Use',
            style: GoogleFonts.poppins(
              color: AppColors.primary,
              fontWeight: FontWeight.w500,
            ),
          ),
          const TextSpan(text: ' and '),
          TextSpan(
            text: 'Privacy Policy',
            style: GoogleFonts.poppins(
              color: AppColors.primary,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}


