import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';

class LanguageSettingsPage extends StatefulWidget {
  const LanguageSettingsPage({Key? key}) : super(key: key);

  @override
  State<LanguageSettingsPage> createState() => _LanguageSettingsPageState();
}

class _LanguageSettingsPageState extends State<LanguageSettingsPage> {
  String _selectedLanguage = 'en';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      appBar: AppBar(
        title: Text(
          'Language Settings',
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
            Text(
              'Select your preferred language',
              style: GoogleFonts.poppins(
                fontSize: 14,
                color: ThemeHelper.textSecondary(context),
              ),
            ),
            const SizedBox(height: 30),
            // Language Options
            Container(
              decoration: BoxDecoration(
                color: ThemeHelper.cardColor(context),
                borderRadius: BorderRadius.circular(16),
                boxShadow: ThemeHelper.cardShadow(context),
              ),
              child: Column(
                children: [
                  _buildLanguageOption(
                    flag: '🇺🇸',
                    name: 'English',
                    code: 'en',
                    nativeName: 'English',
                  ),
                  const Divider(height: 1),
                  _buildLanguageOption(
                    flag: '🇮🇳',
                    name: 'Tamil',
                    code: 'ta',
                    nativeName: 'தமிழ்',
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLanguageOption({
    required String flag,
    required String name,
    required String code,
    required String nativeName,
  }) {
    final isSelected = _selectedLanguage == code;
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      leading: Text(
        flag,
        style: const TextStyle(fontSize: 32),
      ),
      title: Text(
        name,
        style: GoogleFonts.poppins(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: ThemeHelper.textPrimary(context),
        ),
      ),
      subtitle: Text(
        nativeName,
        style: GoogleFonts.poppins(
          fontSize: 14,
          color: ThemeHelper.textSecondary(context),
        ),
      ),
      trailing: isSelected
          ? Icon(
              Icons.check_circle,
              color: AppColors.primary,
            )
          : const Icon(
              Icons.radio_button_unchecked,
              color: Colors.grey,
            ),
      onTap: () {
        setState(() => _selectedLanguage = code);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              code == 'en' ? 'Language set to English' : 'Tamil language coming soon!',
              style: GoogleFonts.poppins(),
            ),
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            duration: const Duration(seconds: 2),
          ),
        );
      },
    );
  }
}

