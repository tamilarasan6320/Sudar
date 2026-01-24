import 'package:onesignal_flutter/onesignal_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:async';
import 'api_service.dart';

/// OneSignal Push Notification Service
/// Handles all push notification functionality
class OneSignalService {
  static const String _onesignalAppId = '4ead7a7e-e376-41bf-8219-dd1a04e36da4';
  static const String _lastSentPlayerIdKey = 'onesignal_last_sent_player_id';
  
  static bool _isInitialized = false;
  static String? _playerId;
  static String? _userId;

  /// Initialize OneSignal
  /// Call this in main.dart before runApp()
  static Future<void> initialize() async {
    if (_isInitialized) {
      print('OneSignal: Already initialized');
      return;
    }

    try {
      // Set App ID
      OneSignal.initialize(_onesignalAppId);

      // Set up notification handlers
      _setupNotificationHandlers();

      // Get player ID (directly accessible, not async)
      _playerId = OneSignal.User.pushSubscription.id;
      print('OneSignal: Player ID: $_playerId');

      // Set up user ID if available
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      if (userId != null) {
        await setUserId(userId.toString());
        
        // Send player ID to backend for admin targeting
        if (_playerId != null && _playerId!.isNotEmpty) {
          await _sendPlayerIdToBackend(_playerId!);
        }
      }

      _isInitialized = true;
      print('OneSignal: Initialized successfully');
    } catch (e) {
      print('OneSignal: Initialization error: $e');
    }
  }

  /// Set up notification event handlers
  static void _setupNotificationHandlers() {
    // Handle notification received while app is in foreground
    OneSignal.Notifications.addForegroundWillDisplayListener((event) {
      print('OneSignal: Notification received in foreground');
      print('OneSignal: Title: ${event.notification.title}');
      print('OneSignal: Body: ${event.notification.body}');
      
      // Display notification (void function - no return needed)
      // Notification will be displayed automatically
    });

    // Handle notification clicked/tapped
    OneSignal.Notifications.addClickListener((event) {
      print('OneSignal: Notification clicked');
      print('OneSignal: Title: ${event.notification.title}');
      print('OneSignal: Body: ${event.notification.body}');
      
      // Handle notification click action
      _handleNotificationClick(event.notification);
    });

    // Handle permission changes
    OneSignal.Notifications.addPermissionObserver((hasPrompted) {
      print('OneSignal: Permission changed: $hasPrompted');
    });

    // Handle subscription changes
    OneSignal.User.pushSubscription.addObserver((state) {
      print('OneSignal: Subscription changed');
      
      // Get current subscription ID (directly accessible)
      final previous = _playerId;
      final id = OneSignal.User.pushSubscription.id;
      if (id != null) {
        _playerId = id;
        print('OneSignal: Player ID: $id');

        // Best-effort: send to backend if it changed (enables admin targeting)
        if (id.isNotEmpty && id != previous) {
          _sendPlayerIdToBackend(id);
        }
      }
    });
  }

  /// Handle notification click/tap
  static void _handleNotificationClick(OSNotification notification) {
    final additionalData = notification.additionalData;
    
    if (additionalData != null) {
      // Handle custom data from notification
      final type = additionalData['type'];
      final targetId = additionalData['target_id'];
      
      print('OneSignal: Notification type: $type');
      print('OneSignal: Target ID: $targetId');
      
      // You can navigate to specific screens based on notification data
      // Example:
      // if (type == 'test_result') {
      //   Navigator.push(...);
      // } else if (type == 'announcement') {
      //   Navigator.push(...);
      // }
    }
  }

  /// Set user ID for targeted notifications
  /// Call this after user logs in
  static Future<void> setUserId(String userId) async {
    try {
      _userId = userId;
      
      // Set external user ID
      await OneSignal.login(userId);
      
      // Save to preferences
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('onesignal_user_id', userId);

      // Best-effort: ensure backend has the latest playerId for this user (admin targeting)
      final id = OneSignal.User.pushSubscription.id;
      if (id != null && id.isNotEmpty) {
        await _sendPlayerIdToBackend(id);
      }
      
      print('OneSignal: User ID set: $userId');
    } catch (e) {
      print('OneSignal: Error setting user ID: $e');
    }
  }

  /// Remove user ID (logout)
  /// Call this when user logs out
  static Future<void> removeUserId() async {
    try {
      await OneSignal.logout();
      
      // Remove from preferences
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('onesignal_user_id');
      
      _userId = null;
      print('OneSignal: User ID removed');
    } catch (e) {
      print('OneSignal: Error removing user ID: $e');
    }
  }

  /// Get current player ID
  static String? getPlayerId() {
    return _playerId;
  }

  /// Get current user ID
  static String? getUserId() {
    return _userId;
  }

  /// Check if notifications are enabled
  static Future<bool> areNotificationsEnabled() async {
    try {
      final state = await OneSignal.Notifications.permission;
      return state == OSNotificationPermission.authorized;
    } catch (e) {
      print('OneSignal: Error checking permission: $e');
      return false;
    }
  }

  /// Whether we should show an in-app permission dialog (i.e., user hasn't decided yet).
  static Future<bool> shouldShowPermissionDialog() async {
    try {
      final state = await OneSignal.Notifications.permission;
      return state == OSNotificationPermission.notDetermined;
    } catch (e) {
      print('OneSignal: Error checking permission state: $e');
      return false;
    }
  }

  /// Request notification permission
  static Future<bool> requestPermission() async {
    try {
      final result = await OneSignal.Notifications.requestPermission(true);

      // Refresh playerId and send to backend (if logged in) after permission flow.
      final id = OneSignal.User.pushSubscription.id;
      if (id != null) {
        _playerId = id;
        if (id.isNotEmpty) {
          await _sendPlayerIdToBackend(id);
        }
      }
      return result;
    } catch (e) {
      print('OneSignal: Error requesting permission: $e');
      return false;
    }
  }

  /// Send tags to OneSignal (for segmentation)
  static Future<void> sendTags(Map<String, String> tags) async {
    try {
      await OneSignal.User.addTags(tags);
      print('OneSignal: Tags sent: $tags');
    } catch (e) {
      print('OneSignal: Error sending tags: $e');
    }
  }

  /// Remove tags from OneSignal
  static Future<void> removeTags(List<String> tagKeys) async {
    try {
      await OneSignal.User.removeTags(tagKeys);
      print('OneSignal: Tags removed: $tagKeys');
    } catch (e) {
      print('OneSignal: Error removing tags: $e');
    }
  }

  /// Send player ID to backend (for admin targeting)
  static Future<void> _sendPlayerIdToBackend(String playerId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('userId');
      final lastSent = prefs.getString(_lastSentPlayerIdKey);
      
      if (userId != null && playerId.isNotEmpty) {
        if (lastSent == playerId) return;
        final res = await ApiService.updateOneSignalPlayerId(
          userId: userId,
          playerId: playerId,
        );
        if (res['success'] == true) {
          await prefs.setString(_lastSentPlayerIdKey, playerId);
          print('OneSignal: Player ID sent to backend');
        } else {
          print('OneSignal: Failed to send player ID: ${res['message'] ?? 'Unknown error'}');
        }
      }
    } catch (e) {
      print('OneSignal: Error sending player ID to backend: $e');
    }
  }
}

