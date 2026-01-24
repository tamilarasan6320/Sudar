import 'package:firebase_analytics/firebase_analytics.dart';
import 'package:flutter/foundation.dart';

/// Service wrapper for Google Analytics (Firebase Analytics).
/// 
/// Events tracked (same as Facebook):
/// - App Open: on every app launch
/// - CompleteRegistration: on signup success
/// - Purchase: on 7-day trial activation
/// 
/// View events in Firebase Console > Analytics > Events
class GoogleAnalyticsService {
  static final FirebaseAnalytics _analytics = FirebaseAnalytics.instance;
  static bool _initialized = false;

  /// Initialize Google Analytics. Call this once at app startup.
  static Future<void> initialize() async {
    if (_initialized) return;
    
    try {
      // Enable analytics collection
      await _analytics.setAnalyticsCollectionEnabled(true);
      
      // Log app open event
      await _analytics.logAppOpen();
      
      _initialized = true;
      debugPrint('📊 [GA] Google Analytics initialized successfully');
    } catch (e) {
      debugPrint('❌ [GA] Failed to initialize Google Analytics: $e');
    }
  }

  /// Log app open event. Call this on each app open.
  static Future<void> logAppOpen() async {
    try {
      await _analytics.logAppOpen();
      debugPrint('📊 [GA] App open logged');
    } catch (e) {
      debugPrint('❌ [GA] Failed to log app open: $e');
    }
  }

  /// Log sign up / complete registration event.
  /// Call this after a new user completes profile setup.
  /// 
  /// [signUpMethod] - e.g. "phone", "email", "truecaller", "google"
  static Future<void> logSignUp({
    required String signUpMethod,
  }) async {
    try {
      await _analytics.logSignUp(signUpMethod: signUpMethod);
      debugPrint('📊 [GA] SignUp logged (method: $signUpMethod)');
    } catch (e) {
      debugPrint('❌ [GA] Failed to log SignUp: $e');
    }
  }

  /// Log Purchase event for trial activation.
  /// Call this when 7-day trial is activated.
  /// 
  /// [amount] - Total amount (e.g. 299.0)
  /// [currency] - ISO 4217 currency code (e.g. "INR", "USD")
  /// [transactionId] - Optional order/transaction ID
  /// [itemName] - Optional item name (e.g. "Premium Monthly")
  static Future<void> logPurchase({
    required double amount,
    required String currency,
    String? transactionId,
    String? itemName,
  }) async {
    try {
      await _analytics.logPurchase(
        value: amount,
        currency: currency,
        transactionId: transactionId,
        items: itemName != null
            ? [
                AnalyticsEventItem(
                  itemName: itemName,
                  price: amount,
                  quantity: 1,
                ),
              ]
            : null,
      );
      
      debugPrint('📊 [GA] Purchase logged (amount: $amount $currency, txnId: $transactionId)');
    } catch (e) {
      debugPrint('❌ [GA] Failed to log Purchase: $e');
    }
  }

  /// Log begin checkout event.
  /// Call this when user starts the subscription flow.
  static Future<void> logBeginCheckout({
    required double value,
    required String currency,
    String? itemName,
  }) async {
    try {
      await _analytics.logBeginCheckout(
        value: value,
        currency: currency,
        items: itemName != null
            ? [
                AnalyticsEventItem(
                  itemName: itemName,
                  price: value,
                  quantity: 1,
                ),
              ]
            : null,
      );
      debugPrint('📊 [GA] BeginCheckout logged');
    } catch (e) {
      debugPrint('❌ [GA] Failed to log BeginCheckout: $e');
    }
  }

  /// Log a custom event.
  /// 
  /// [eventName] - Custom event name (e.g. "test_completed", "lesson_viewed")
  /// [parameters] - Optional key-value parameters
  static Future<void> logCustomEvent({
    required String eventName,
    Map<String, Object>? parameters,
  }) async {
    try {
      await _analytics.logEvent(
        name: eventName,
        parameters: parameters,
      );
      debugPrint('📊 [GA] Custom event logged: $eventName');
    } catch (e) {
      debugPrint('❌ [GA] Failed to log custom event $eventName: $e');
    }
  }

  /// Set the user ID for better attribution.
  /// Call after login to tie events to a specific user.
  static Future<void> setUserId(String userId) async {
    try {
      await _analytics.setUserId(id: userId);
      debugPrint('📊 [GA] User ID set: $userId');
    } catch (e) {
      debugPrint('❌ [GA] Failed to set user ID: $e');
    }
  }

  /// Clear the user ID (call on logout).
  static Future<void> clearUserId() async {
    try {
      await _analytics.setUserId(id: null);
      debugPrint('📊 [GA] User ID cleared');
    } catch (e) {
      debugPrint('❌ [GA] Failed to clear user ID: $e');
    }
  }

  /// Set user property for segmentation.
  static Future<void> setUserProperty({
    required String name,
    required String? value,
  }) async {
    try {
      await _analytics.setUserProperty(name: name, value: value);
      debugPrint('📊 [GA] User property set: $name = $value');
    } catch (e) {
      debugPrint('❌ [GA] Failed to set user property: $e');
    }
  }

  /// Log login event.
  static Future<void> logLogin({required String loginMethod}) async {
    try {
      await _analytics.logLogin(loginMethod: loginMethod);
      debugPrint('📊 [GA] Login logged (method: $loginMethod)');
    } catch (e) {
      debugPrint('❌ [GA] Failed to log login: $e');
    }
  }
}
