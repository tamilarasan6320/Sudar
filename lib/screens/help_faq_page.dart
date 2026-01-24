import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:url_launcher/url_launcher.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';

class HelpFAQPage extends StatefulWidget {
  const HelpFAQPage({Key? key}) : super(key: key);

  @override
  State<HelpFAQPage> createState() => _HelpFAQPageState();
}

class _HelpFAQPageState extends State<HelpFAQPage> {
  // WhatsApp support number
  static const String _whatsappNumber = '918300321814';
  static const String _displayNumber = '+91 83003 21814';

  final List<Map<String, String>> _faqs = [
    {
      'question': 'How do I take a test?',
      'answer': 'Go to the Tests tab on the home screen and select any available test. Click "Start Test" to begin answering questions.',
    },
    {
      'question': 'Can I review my answers?',
      'answer': 'Yes! After completing a test, you can review all your answers, see which ones were correct, and read explanations.',
    },
    {
      'question': 'How is my score calculated?',
      'answer': 'Each correct answer gives you points based on the test\'s marking scheme. Check the test details for specific scoring information.',
    },
    {
      'question': 'How do I track my progress?',
      'answer': 'Visit the Progress tab to see detailed analytics including tests taken, average score, rank, and performance trends.',
    },
    // Removed Saved Tests FAQ - will implement in later phase
    // {
    //   'question': 'Can I save tests for later?',
    //   'answer': 'Yes! You can save tests and access them later from the "Saved Tests" section in your profile.',
    // },
    {
      'question': 'How do I change my exam category?',
      'answer': 'Tap on the exam dropdown at the top of the home screen and select your preferred exam category.',
    },
    {
      'question': 'What if I lose internet connection during a test?',
      'answer': 'Your progress is saved locally. You can continue the test when you reconnect, but make sure to submit before the time limit.',
    },
    {
      'question': 'How do I contact support?',
      'answer': 'Tap the "Chat on WhatsApp" button below to reach our support team instantly. You can also find contact information in the About section of your profile.',
    },
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      appBar: AppBar(
        title: Text(
          'Help & FAQ',
          style: GoogleFonts.poppins(
            fontWeight: FontWeight.bold,
            color: ThemeHelper.textPrimary(context),
          ),
        ),
        backgroundColor: ThemeHelper.cardColor(context),
        elevation: 0,
        iconTheme: IconThemeData(
          color: ThemeHelper.textPrimary(context),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 20),
            // Header
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [AppColors.primary, AppColors.primaryLight],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  const Icon(Icons.help_outline, color: Colors.white, size: 40),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Need Help?',
                          style: GoogleFonts.poppins(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'Find answers to common questions',
                          style: GoogleFonts.poppins(
                            fontSize: 14,
                            color: Colors.white.withOpacity(0.9),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 30),
            // FAQ List
            Text(
              'Frequently Asked Questions',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: ThemeHelper.textPrimary(context),
              ),
            ),
            const SizedBox(height: 16),
            ...List.generate(_faqs.length, (index) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _buildFAQItem(_faqs[index]),
              );
            }),
            const SizedBox(height: 20),
            // WhatsApp Support Button
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: _openWhatsAppSupport,
                icon: const Icon(Icons.chat, color: Colors.white),
                label: Text(
                  'Chat on WhatsApp',
                  style: GoogleFonts.poppins(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF25D366), // WhatsApp green
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 12),
            // Show number below button
            Center(
              child: Text(
                'Support: $_displayNumber',
                style: GoogleFonts.poppins(
                  fontSize: 13,
                  color: ThemeHelper.textSecondary(context),
                ),
              ),
            ),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }

  /// Opens WhatsApp with the support number
  Future<void> _openWhatsAppSupport() async {
    // WhatsApp URL with pre-filled message
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
              'Could not open WhatsApp. You can contact us directly at:',
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

  Widget _buildFAQItem(Map<String, String> faq) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: ThemeHelper.cardColor(context),
        borderRadius: BorderRadius.circular(12),
        boxShadow: ThemeHelper.cardShadow(context),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: AppColors.primary.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(
                  Icons.help_outline,
                  color: AppColors.primary,
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  faq['question'] ?? '',
                  style: GoogleFonts.poppins(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: ThemeHelper.textPrimary(context),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Padding(
            padding: const EdgeInsets.only(left: 40),
            child: Text(
              faq['answer'] ?? '',
              style: GoogleFonts.poppins(
                fontSize: 14,
                color: ThemeHelper.textSecondary(context),
                height: 1.5,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

