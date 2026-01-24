import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/app_colors.dart';
import '../services/api_service.dart';

class PrivacyPolicyPage extends StatefulWidget {
  const PrivacyPolicyPage({Key? key}) : super(key: key);

  @override
  State<PrivacyPolicyPage> createState() => _PrivacyPolicyPageState();
}

class _PrivacyPolicyPageState extends State<PrivacyPolicyPage> {
  bool _isLoading = true;
  String _privacyText = 'Privacy policy not available.';

  @override
  void initState() {
    super.initState();
    _loadPrivacyPolicy();
  }

  Future<void> _loadPrivacyPolicy() async {
    setState(() => _isLoading = true);
    
    try {
      final response = await ApiService.getPublicSettings(key: 'privacy_policy');
      
      if (response['success'] == true && response['value'] != null) {
        final text = response['value'].toString();
        if (mounted) {
          setState(() {
            _privacyText = text.isNotEmpty ? text : 'Privacy policy not available.';
            _isLoading = false;
          });
        }
        return;
      }
    } catch (e) {
      print('Error loading privacy policy: $e');
    }
    
    if (mounted) {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: false,
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Text(
          'Privacy Policy',
          style: GoogleFonts.poppins(
            fontWeight: FontWeight.bold,
            color: Theme.of(context).brightness == Brightness.dark
                ? Colors.white
                : AppColors.textPrimary,
          ),
        ),
        backgroundColor: Theme.of(context).brightness == Brightness.dark
            ? AppColors.darkCardBackground
            : Colors.white,
        elevation: 0,
        iconTheme: IconThemeData(
          color: Theme.of(context).brightness == Brightness.dark
              ? Colors.white
              : AppColors.textPrimary,
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Theme.of(context).brightness == Brightness.dark
                      ? Colors.grey[900]
                      : Colors.grey[50],
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Text(
                  _privacyText,
                  style: GoogleFonts.poppins(
                    fontSize: 15,
                    color: AppColors.textPrimary,
                    height: 1.8,
                  ),
                ),
              ),
            ),
    );
  }
}

