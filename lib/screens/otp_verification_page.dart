import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/api_service.dart';
import 'profile_setup_page.dart';
import 'dart:async';

class OTPVerificationPage extends StatefulWidget {
  final String phoneNumber;
  
  const OTPVerificationPage({
    Key? key,
    required this.phoneNumber,
  }) : super(key: key);

  @override
  State<OTPVerificationPage> createState() => _OTPVerificationPageState();
}

class _OTPVerificationPageState extends State<OTPVerificationPage> {
  final List<TextEditingController> _otpControllers = List.generate(
    6,
    (index) => TextEditingController(),
  );
  final List<FocusNode> _focusNodes = List.generate(
    6,
    (index) => FocusNode(),
  );
  
  bool _isLoading = false;
  int _resendTimer = 30;
  bool _canResend = false;
  bool _isResending = false;

  @override
  void initState() {
    super.initState();
    _startResendTimer();
  }

  @override
  void dispose() {
    for (var controller in _otpControllers) {
      controller.dispose();
    }
    for (var node in _focusNodes) {
      node.dispose();
    }
    super.dispose();
  }

  void _startResendTimer() {
    setState(() {
      _resendTimer = 30;
      _canResend = false;
    });

    Timer.periodic(const Duration(seconds: 1), (timer) {
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

  Future<void> _handleVerify() async {
    String otp = _otpControllers.map((c) => c.text).join();

    if (otp.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Please enter complete OTP',
            style: GoogleFonts.poppins(),
          ),
          backgroundColor: AppColors.error,
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

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'OTP verified successfully!',
            style: GoogleFonts.poppins(),
          ),
          backgroundColor: AppColors.success,
        ),
      );

      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(
          builder: (context) => ProfileSetupPage(
            mobileNumber: widget.phoneNumber,
            isNewUser: isNewUser,
            token: token,
            userData: response['user'],
          ),
        ),
        (route) => false,
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            response['message'] ?? 'Invalid or expired OTP',
            style: GoogleFonts.poppins(),
          ),
          backgroundColor: AppColors.error,
        ),
      );
    }
  }

  Future<void> _handleResend() async {
    if (!_canResend || _isResending) return;

    setState(() => _isResending = true);

    final response = await ApiService.sendOTP(widget.phoneNumber);

    if (!mounted) return;

    setState(() => _isResending = false);

    if (response['success'] == true) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'OTP resent successfully!',
            style: GoogleFonts.poppins(),
          ),
          backgroundColor: AppColors.success,
        ),
      );
      _startResendTimer();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            response['message'] ?? 'Failed to resend OTP. Try again later.',
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
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(6, (index) {
        final bool hasValue = _otpControllers[index].text.isNotEmpty;
        
        return Container(
          width: 50,
          height: 60,
          margin: EdgeInsets.only(
            left: index == 0 ? 0 : 6,
            right: index == 5 ? 0 : 6,
          ),
          decoration: BoxDecoration(
            color: ThemeHelper.cardColor(context),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: hasValue ? AppColors.primary : const Color(0xFFE0E0E0),
              width: 2,
            ),
            boxShadow: [
              BoxShadow(
                color: hasValue 
                    ? AppColors.primary.withOpacity(0.2)
                    : Colors.black.withOpacity(0.05),
                blurRadius: hasValue ? 8 : 4,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          child: Center(
            child: TextFormField(
              controller: _otpControllers[index],
              focusNode: _focusNodes[index],
              textAlign: TextAlign.center,
              keyboardType: TextInputType.number,
              maxLength: 1,
              style: GoogleFonts.poppins(
                fontSize: 28,
                fontWeight: FontWeight.bold,
                color: ThemeHelper.textPrimary(context),
              ),
              decoration: const InputDecoration(
                counterText: '',
                border: InputBorder.none,
                contentPadding: EdgeInsets.zero,
              ),
              inputFormatters: [
                FilteringTextInputFormatter.digitsOnly,
              ],
              onChanged: (value) {
                setState(() {});
                if (value.isNotEmpty && index < 5) {
                  _focusNodes[index + 1].requestFocus();
                } else if (value.isEmpty && index > 0) {
                  _focusNodes[index - 1].requestFocus();
                }
                
                // Auto-verify when all fields are filled
                if (index == 5 && value.isNotEmpty) {
                  bool allFilled = _otpControllers.every((c) => c.text.isNotEmpty);
                  if (allFilled) {
                    FocusScope.of(context).unfocus();
                  }
                }
              },
            ),
          ),
        );
      }),
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


