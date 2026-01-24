import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../utils/app_colors.dart';
import '../utils/theme_helper.dart';

class NotificationsSettingsPage extends StatefulWidget {
  const NotificationsSettingsPage({Key? key}) : super(key: key);

  @override
  State<NotificationsSettingsPage> createState() => _NotificationsSettingsPageState();
}

class _NotificationsSettingsPageState extends State<NotificationsSettingsPage> {
  bool _testReminders = true;
  bool _performanceUpdates = false;
  bool _newContent = true;
  bool _dailyReports = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: ThemeHelper.backgroundColor(context),
      appBar: AppBar(
        title: Text(
          'Notifications',
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
              'Manage your notification preferences',
              style: GoogleFonts.poppins(
                fontSize: 14,
                color: ThemeHelper.textSecondary(context),
              ),
            ),
            const SizedBox(height: 30),
            // Notification Options
            Container(
              decoration: BoxDecoration(
                color: ThemeHelper.cardColor(context),
                borderRadius: BorderRadius.circular(16),
                boxShadow: ThemeHelper.cardShadow(context),
              ),
              child: Column(
                children: [
                  _buildNotificationTile(
                    icon: Icons.quiz_outlined,
                    title: 'Test Reminders',
                    subtitle: 'Get notified about upcoming tests',
                    value: _testReminders,
                    onChanged: (value) {
                      setState(() => _testReminders = value);
                      _showMessage('Test reminders ${value ? "enabled" : "disabled"}');
                    },
                  ),
                  const Divider(height: 1),
                  _buildNotificationTile(
                    icon: Icons.trending_up_outlined,
                    title: 'Performance Updates',
                    subtitle: 'Receive performance insights',
                    value: _performanceUpdates,
                    onChanged: (value) {
                      setState(() => _performanceUpdates = value);
                      _showMessage('Performance updates ${value ? "enabled" : "disabled"}');
                    },
                  ),
                  const Divider(height: 1),
                  _buildNotificationTile(
                    icon: Icons.new_releases_outlined,
                    title: 'New Content',
                    subtitle: 'Notify when new tests are added',
                    value: _newContent,
                    onChanged: (value) {
                      setState(() => _newContent = value);
                      _showMessage('New content notifications ${value ? "enabled" : "disabled"}');
                    },
                  ),
                  const Divider(height: 1),
                  _buildNotificationTile(
                    icon: Icons.assessment_outlined,
                    title: 'Daily Reports',
                    subtitle: 'Get daily progress summaries',
                    value: _dailyReports,
                    onChanged: (value) {
                      setState(() => _dailyReports = value);
                      _showMessage('Daily reports ${value ? "enabled" : "disabled"}');
                    },
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildNotificationTile({
    required IconData icon,
    required String title,
    required String subtitle,
    required bool value,
    required Function(bool) onChanged,
  }) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
      secondary: Icon(
        icon,
        color: AppColors.primary,
        size: 24,
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
      value: value,
      onChanged: onChanged,
      activeColor: AppColors.primary,
    );
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message, style: GoogleFonts.poppins()),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        duration: const Duration(seconds: 2),
      ),
    );
  }
}

