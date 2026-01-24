import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:in_app_update/in_app_update.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import 'api_service.dart';

class _ParsedVersion {
  final int major;
  final int minor;
  final int patch;
  final int build;
  const _ParsedVersion(this.major, this.minor, this.patch, this.build);
}

/// Service to handle in-app updates
/// Uses Google Play's in-app update API for Android
class UpdateService {
  static final UpdateService _instance = UpdateService._internal();
  factory UpdateService() => _instance;
  UpdateService._internal();

  AppUpdateInfo? _updateInfo;
  bool _isUpdateAvailable = false;

  static const String _defaultPlayStoreUrl =
      'https://play.google.com/store/apps/details?id=com.sudar.tnpscapp';
  static const String _dismissedVersionKey = 'dismissed_update_version';
  
  /// Check for updates.
  ///
  /// Priority:
  /// 1) Admin-controlled version check (shows bottom sheet on Home) - DISABLED
  /// 2) Play Store in-app update check (Android only)
  Future<void> checkForUpdate(BuildContext context) async {
    // Admin-controlled update check - COMMENTED OUT
    // Uncomment below lines to enable admin-controlled updates from your server
    // final shown = await _checkAdminControlledUpdate(context);
    // if (shown) return;

    try {
      print('🔄 Checking for app updates...');
      
      _updateInfo = await InAppUpdate.checkForUpdate();
      
      _isUpdateAvailable = _updateInfo?.updateAvailability == 
          UpdateAvailability.updateAvailable;
      
      if (_isUpdateAvailable) {
        print('✅ Update available!');
        print('   Available version code: ${_updateInfo?.availableVersionCode}');
        print('   Update priority: ${_updateInfo?.updatePriority}');
        
        // FORCE IMMEDIATE UPDATE (Priority 5)
        // Always force update when new version is available
        await _startImmediateUpdate(context);
      } else {
        print('✅ App is up to date!');
      }
    } catch (e) {
      print('❌ Error checking for update: $e');
      // Fallback to manual update check if Play Store API fails
      // This can happen in development or sideloaded apps
    }
  }

  _ParsedVersion _parseVersion(String input) {
    final s = input.trim();
    final coreMatch = RegExp(r'(\d+)(?:\.(\d+))?(?:\.(\d+))?').firstMatch(s);
    final major = int.tryParse(coreMatch?.group(1) ?? '') ?? 0;
    final minor = int.tryParse(coreMatch?.group(2) ?? '') ?? 0;
    final patch = int.tryParse(coreMatch?.group(3) ?? '') ?? 0;

    final buildMatch = RegExp(r'\+(\d+)').firstMatch(s);
    final build = int.tryParse(buildMatch?.group(1) ?? '') ?? 0;

    return _ParsedVersion(major, minor, patch, build);
  }

  int _compareVersions(_ParsedVersion a, _ParsedVersion b) {
    if (a.major != b.major) return a.major.compareTo(b.major);
    if (a.minor != b.minor) return a.minor.compareTo(b.minor);
    if (a.patch != b.patch) return a.patch.compareTo(b.patch);
    return a.build.compareTo(b.build);
  }

  bool _parseBool(dynamic value) {
    final v = (value ?? '').toString().trim().toLowerCase();
    return v == '1' || v == 'true' || v == 'yes' || v == 'on';
  }

  Future<bool> _checkAdminControlledUpdate(BuildContext context) async {
    try {
      final packageInfo = await PackageInfo.fromPlatform();
      final current = _parseVersion('${packageInfo.version}+${packageInfo.buildNumber}');

      final res = await ApiService.getPublicSettings();
      if (res['success'] != true) return false;

      final settings = res['settings'];
      if (settings is! Map) return false;

      final latestVersionStr = (settings['app_latest_version'] ?? '').toString().trim();
      if (latestVersionStr.isEmpty) return false;

      final latest = _parseVersion(latestVersionStr);
      final isUpdateAvailable = _compareVersions(current, latest) < 0;
      if (!isUpdateAvailable) return false;

      final forceUpdate = _parseBool(settings['app_force_update']);
      final updateUrl = (settings['app_update_url'] ?? '').toString().trim();
      final updateMessage = (settings['app_update_message'] ?? '').toString().trim();

      final prefs = await SharedPreferences.getInstance();
      final dismissedFor = prefs.getString(_dismissedVersionKey);
      if (!forceUpdate && dismissedFor == latestVersionStr) {
        return false;
      }

      if (!context.mounted) return false;

      await showModalBottomSheet<void>(
        context: context,
        isDismissible: !forceUpdate,
        enableDrag: !forceUpdate,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(18)),
        ),
        builder: (ctx) {
          return WillPopScope(
            onWillPop: () async => !forceUpdate,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: Colors.green.shade50,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Icon(Icons.system_update, color: Colors.green.shade700),
                      ),
                      const SizedBox(width: 12),
                      const Expanded(
                        child: Text(
                          'Update available',
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Current: ${packageInfo.version}+${packageInfo.buildNumber}\nLatest: $latestVersionStr',
                    style: TextStyle(color: Colors.grey.shade700, height: 1.3),
                  ),
                  if (updateMessage.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    Text(
                      updateMessage,
                      style: const TextStyle(fontSize: 14, height: 1.4),
                    ),
                  ],
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      if (!forceUpdate)
                        Expanded(
                          child: OutlinedButton(
                            onPressed: () async {
                              await prefs.setString(_dismissedVersionKey, latestVersionStr);
                              if (ctx.mounted) Navigator.pop(ctx);
                            },
                            child: const Text('Later'),
                          ),
                        ),
                      if (!forceUpdate) const SizedBox(width: 12),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () async {
                            final url = updateUrl.isNotEmpty ? updateUrl : _defaultPlayStoreUrl;
                            final uri = Uri.tryParse(url);
                            if (uri != null && await canLaunchUrl(uri)) {
                              await launchUrl(uri, mode: LaunchMode.externalApplication);
                            }
                          },
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.green,
                            foregroundColor: Colors.white,
                          ),
                          child: const Text('Update'),
                        ),
                      ),
                    ],
                  ),
                  if (forceUpdate) ...[
                    const SizedBox(height: 8),
                    Text(
                      'Update is required to continue.',
                      style: TextStyle(color: Colors.grey.shade700, fontSize: 12),
                    ),
                  ],
                ],
              ),
            ),
          );
        },
      );

      return true;
    } catch (e) {
      print('❌ Admin update check failed: $e');
      return false;
    }
  }

  /// Start immediate (forced) update
  Future<void> _startImmediateUpdate(BuildContext context) async {
    try {
      print('🚀 Starting immediate update...');
      await InAppUpdate.performImmediateUpdate();
    } catch (e) {
      print('❌ Immediate update failed: $e');
      // Show manual update dialog as fallback
      _showManualUpdateDialog(context);
    }
  }

  /// Start flexible update (background download)
  Future<void> _startFlexibleUpdate(BuildContext context) async {
    try {
      print('🚀 Starting flexible update...');
      
      await InAppUpdate.startFlexibleUpdate();
      
      // Show snackbar when download completes
      InAppUpdate.completeFlexibleUpdate().then((_) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text('Update downloaded! Restart to install.'),
            action: SnackBarAction(
              label: 'RESTART',
              onPressed: () {
                InAppUpdate.completeFlexibleUpdate();
              },
            ),
            duration: const Duration(seconds: 10),
          ),
        );
      });
    } catch (e) {
      print('❌ Flexible update failed: $e');
    }
  }

  /// Show update available dialog
  Future<void> _showUpdateDialog(BuildContext context) async {
    final packageInfo = await PackageInfo.fromPlatform();
    
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.green.shade50,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(Icons.system_update, color: Colors.green.shade600, size: 28),
            ),
            const SizedBox(width: 12),
            const Expanded(
              child: Text(
                'Update Available!',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'A new version of ${packageInfo.appName} is available.',
              style: const TextStyle(fontSize: 15),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.grey.shade100,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Column(
                children: [
                  _buildInfoRow('Current Version', packageInfo.version),
                  const SizedBox(height: 4),
                  _buildInfoRow('New Version', 'Available on Play Store'),
                ],
              ),
            ),
            const SizedBox(height: 12),
            const Text(
              '✨ Update now for new features and improvements!',
              style: TextStyle(fontSize: 13, color: Colors.grey),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('LATER', style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              _startFlexibleUpdate(context);
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
            child: const Text('UPDATE NOW'),
          ),
        ],
      ),
    );
  }

  /// Show manual update dialog (fallback)
  void _showManualUpdateDialog(BuildContext context) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Row(
          children: [
            Icon(Icons.warning_amber_rounded, color: Colors.orange.shade600, size: 28),
            const SizedBox(width: 12),
            const Text('Update Required', style: TextStyle(fontSize: 18)),
          ],
        ),
        content: const Text(
          'Please update the app from Play Store to continue using all features.',
          style: TextStyle(fontSize: 15),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('LATER'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              _openPlayStore();
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
            ),
            child: const Text('OPEN PLAY STORE'),
          ),
        ],
      ),
    );
  }

  /// Open app's Play Store page
  Future<void> _openPlayStore() async {
    final uri = Uri.parse(_defaultPlayStoreUrl);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    }
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
        Text(value, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
      ],
    );
  }

  /// Get current app version
  static Future<String> getCurrentVersion() async {
    final packageInfo = await PackageInfo.fromPlatform();
    return '${packageInfo.version}+${packageInfo.buildNumber}';
  }
}

