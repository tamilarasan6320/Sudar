import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';
import '../services/theme_service.dart';

class ThemeSettingsPage extends StatelessWidget {
  const ThemeSettingsPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final themeService = Provider.of<ThemeService>(context);
    final currentMode = themeService.themeMode;

    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      appBar: AppBar(
        title: Text(
          'Theme Settings',
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
              'Choose your preferred theme',
              style: GoogleFonts.poppins(
                fontSize: 14,
                color: ThemeHelper.textSecondary(context),
              ),
            ),
            const SizedBox(height: 30),
            // Theme Options
            Container(
              decoration: BoxDecoration(
                color: ThemeHelper.cardColor(context),
                borderRadius: BorderRadius.circular(16),
                boxShadow: ThemeHelper.cardShadow(context),
              ),
              child: Column(
                children: [
                  _buildThemeOption(
                    context: context,
                    themeService: themeService,
                    mode: ThemeMode.light,
                    icon: Icons.light_mode,
                    title: 'Light Mode',
                    subtitle: 'Bright and clean interface',
                    isSelected: currentMode == ThemeMode.light,
                  ),
                  const Divider(height: 1),
                  _buildThemeOption(
                    context: context,
                    themeService: themeService,
                    mode: ThemeMode.dark,
                    icon: Icons.dark_mode,
                    title: 'Dark Mode',
                    subtitle: 'Easy on the eyes',
                    isSelected: currentMode == ThemeMode.dark,
                  ),
                  const Divider(height: 1),
                  _buildThemeOption(
                    context: context,
                    themeService: themeService,
                    mode: ThemeMode.system,
                    icon: Icons.brightness_auto,
                    title: 'System Default',
                    subtitle: 'Follow device settings',
                    isSelected: currentMode == ThemeMode.system,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildThemeOption({
    required BuildContext context,
    required ThemeService themeService,
    required ThemeMode mode,
    required IconData icon,
    required String title,
    required String subtitle,
    required bool isSelected,
  }) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      leading: Icon(
        icon,
        color: AppColors.primary,
        size: 28,
      ),
      title: Text(
        title,
        style: GoogleFonts.poppins(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: ThemeHelper.textPrimary(context),
        ),
      ),
      subtitle: Text(
        subtitle,
        style: GoogleFonts.poppins(
          fontSize: 13,
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
        themeService.setThemeMode(mode);
        String message = '';
        if (mode == ThemeMode.light) {
          message = 'Light mode enabled';
        } else if (mode == ThemeMode.dark) {
          message = 'Dark mode enabled';
        } else {
          message = 'System default enabled';
        }
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(message, style: GoogleFonts.poppins()),
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            duration: const Duration(seconds: 2),
          ),
        );
      },
    );
  }
}

