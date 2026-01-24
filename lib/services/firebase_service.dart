import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';

/// Firebase Service
/// Handles Firebase initialization and messaging
class FirebaseService {
  static bool _isInitialized = false;
  static FirebaseMessaging? _messaging;
  static String? _fcmToken;

  /// Initialize Firebase
  /// Call this in main.dart before runApp()
  static Future<void> initialize({bool requestPermission = false}) async {
    if (_isInitialized) {
      print('Firebase: Already initialized');
      return;
    }

    try {
      await Firebase.initializeApp();
      _messaging = FirebaseMessaging.instance;
      
      // Permission should be requested from UI (e.g., Home dialog) to improve UX.
      if (requestPermission) {
        await _requestPermission();
      }
      
      // Get FCM token
      await _getFCMToken();
      
      // Set up message handlers
      _setupMessageHandlers();
      
      // Set background message handler
      FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
      
      _isInitialized = true;
      print('Firebase: Initialized successfully');
    } catch (e) {
      print('Firebase: Initialization error: $e');
      if (kDebugMode) {
        print('Firebase: Make sure google-services.json is in android/app/');
      }
    }
  }

  /// Request notification permission
  static Future<void> _requestPermission() async {
    try {
      final settings = await _messaging!.requestPermission(
        alert: true,
        announcement: false,
        badge: true,
        carPlay: false,
        criticalAlert: false,
        provisional: false,
        sound: true,
      );
      
      print('Firebase: Permission status: ${settings.authorizationStatus}');
    } catch (e) {
      print('Firebase: Error requesting permission: $e');
    }
  }

  /// Request notification permission from UI (recommended).
  static Future<void> requestPermission() async {
    if (_messaging == null) {
      print('Firebase: Messaging not initialized');
      return;
    }
    await _requestPermission();
  }

  /// Get FCM token
  static Future<void> _getFCMToken() async {
    try {
      _fcmToken = await _messaging!.getToken();
      print('Firebase: FCM Token: $_fcmToken');
      
      // Listen for token refresh
      _messaging!.onTokenRefresh.listen((newToken) {
        _fcmToken = newToken;
        print('Firebase: FCM Token refreshed: $newToken');
        // You can send this token to your backend here
      });
    } catch (e) {
      print('Firebase: Error getting FCM token: $e');
    }
  }

  /// Set up message handlers
  static void _setupMessageHandlers() {
    // Handle foreground messages
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      print('Firebase: Foreground message received');
      print('Firebase: Title: ${message.notification?.title}');
      print('Firebase: Body: ${message.notification?.body}');
      print('Firebase: Data: ${message.data}');
      
      // Handle foreground notification display
      // You can show a local notification here if needed
    });

    // Handle background message taps
    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      print('Firebase: Notification opened from background');
      print('Firebase: Title: ${message.notification?.title}');
      print('Firebase: Body: ${message.notification?.body}');
      print('Firebase: Data: ${message.data}');
      
      // Handle navigation based on notification data
      _handleNotificationClick(message);
    });

    // Check if app was opened from terminated state
    _messaging!.getInitialMessage().then((RemoteMessage? message) {
      if (message != null) {
        print('Firebase: App opened from terminated state');
        print('Firebase: Title: ${message.notification?.title}');
        print('Firebase: Body: ${message.notification?.body}');
        print('Firebase: Data: ${message.data}');
        
        _handleNotificationClick(message);
      }
    });
  }

  /// Handle notification click/tap
  static void _handleNotificationClick(RemoteMessage message) {
    final data = message.data;
    
    if (data.isNotEmpty) {
      final type = data['type'];
      final targetId = data['target_id'];
      
      print('Firebase: Notification type: $type');
      print('Firebase: Target ID: $targetId');
      
      // You can navigate to specific screens based on notification data
      // Example:
      // if (type == 'test_result') {
      //   Navigator.push(...);
      // } else if (type == 'announcement') {
      //   Navigator.push(...);
      // }
    }
  }

  /// Get FCM token
  static String? getFCMToken() {
    return _fcmToken;
  }

  /// Subscribe to topic
  static Future<void> subscribeToTopic(String topic) async {
    try {
      await _messaging!.subscribeToTopic(topic);
      print('Firebase: Subscribed to topic: $topic');
    } catch (e) {
      print('Firebase: Error subscribing to topic: $e');
    }
  }

  /// Unsubscribe from topic
  static Future<void> unsubscribeFromTopic(String topic) async {
    try {
      await _messaging!.unsubscribeFromTopic(topic);
      print('Firebase: Unsubscribed from topic: $topic');
    } catch (e) {
      print('Firebase: Error unsubscribing from topic: $e');
    }
  }
}

/// Background message handler
/// Must be a top-level function
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  print('Firebase: Background message received');
  print('Firebase: Title: ${message.notification?.title}');
  print('Firebase: Body: ${message.notification?.body}');
  print('Firebase: Data: ${message.data}');
}

