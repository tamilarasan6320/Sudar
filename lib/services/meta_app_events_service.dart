import 'package:facebook_app_events/facebook_app_events.dart';
import 'package:flutter/foundation.dart';

/// Service wrapper for Meta (Facebook) App Events.
/// 
/// Events tracked:
/// - Install / App Launch: auto-logged by SDK (requires initialize())
/// - CompleteRegistration: manual (on signup success)
/// - ExistingUserLogin: custom (on existing user login success)
/// - Purchase: manual (on external payment success)
/// 
/// Verify events in browser: Meta App Ads Helper > Test App Events
/// https://developers.facebook.com/docs/app-events/getting-started-app-events-android
class MetaAppEventsService {
  static final FacebookAppEvents _fb = FacebookAppEvents();
  static bool _initialized = false;

  /// Initialize the Facebook SDK. Call this once at app startup.
  /// This enables automatic app install and app open tracking.
  static Future<void> initialize() async {
    if (_initialized) return;
    
    try {
      debugPrint('════════════════════════════════════════════════════════════');
      debugPrint('[META SDK] INITIALIZING...');
      debugPrint('════════════════════════════════════════════════════════════');
      
      // Enable auto-logging of app events (installs, opens, etc.)
      await _fb.setAutoLogAppEventsEnabled(true);
      debugPrint('[META SDK] ✅ setAutoLogAppEventsEnabled(true)');
      
      // Enable advertiser ID collection for better attribution
      await _fb.setAdvertiserTracking(enabled: true);
      debugPrint('[META SDK] ✅ setAdvertiserTracking(true)');
      
      // Get and log the anonymous ID for debugging
      try {
        final anonId = await _fb.getAnonymousId();
        debugPrint('[META SDK] Anonymous ID: $anonId');
      } catch (e) {
        debugPrint('[META SDK] ⚠️ Could not get anonymous ID: $e');
      }
      
      // Log app activation event manually
      debugPrint('[fb_mobile_activate_app] TRIGGERING (init)...');
      await _fb.logEvent(name: 'fb_mobile_activate_app');
      debugPrint('[fb_mobile_activate_app] ✅ SUCCESS');
      
      _initialized = true;
      debugPrint('════════════════════════════════════════════════════════════');
      debugPrint('[META SDK] ✅ INITIALIZED');
      debugPrint('════════════════════════════════════════════════════════════');
    } catch (e, stack) {
      debugPrint('════════════════════════════════════════════════════════════');
      debugPrint('[META SDK] ❌ INIT FAILED: $e');
      debugPrint('[META SDK] Stack: $stack');
      debugPrint('════════════════════════════════════════════════════════════');
    }
  }

  /// Log app activation. Call this on each app open for better tracking.
  /// This helps ensure install and app open events are captured.
  static Future<void> logActivateApp() async {
    try {
      debugPrint('[fb_mobile_activate_app] TRIGGERING...');
      await _fb.logEvent(name: 'fb_mobile_activate_app');
      debugPrint('[fb_mobile_activate_app] ✅ SUCCESS');
    } catch (e) {
      debugPrint('[fb_mobile_activate_app] ❌ FAILED: $e');
    }
  }

  /// Manually trigger install event for testing (use this to verify SDK is working)
  static Future<void> testInstallEvent() async {
    try {
      debugPrint('════════════════════════════════════════════════════════════');
      debugPrint('[fb_mobile_install] TRIGGERING...');
      debugPrint('════════════════════════════════════════════════════════════');
      
      // 1. fb_mobile_install - App Install event
      await _fb.logEvent(
        name: 'fb_mobile_install',
        parameters: {
          'fb_mobile_launch_source': 'Organic',
        },
      );
      debugPrint('[fb_mobile_install] ✅ SUCCESS - Event sent to Facebook');
      
      debugPrint('════════════════════════════════════════════════════════════');
    } catch (e) {
      debugPrint('[fb_mobile_install] ❌ FAILED: $e');
    }
  }
  
  /// Log fb_mobile_install event explicitly
  static Future<void> logInstall() async {
    try {
      debugPrint('[fb_mobile_install] TRIGGERING...');
      await _fb.logEvent(
        name: 'fb_mobile_install',
        parameters: {
          'fb_mobile_launch_source': 'Organic',
        },
      );
      debugPrint('[fb_mobile_install] ✅ SUCCESS');
    } catch (e) {
      debugPrint('[fb_mobile_install] ❌ FAILED: $e');
    }
  }

  /// Log CompleteRegistration standard event.
  /// Call this after a new user completes profile setup.
  /// 
  /// [registrationMethod] - e.g. "phone", "email", "truecaller", "google"
  static Future<void> logCompleteRegistration({
    required String registrationMethod,
  }) async {
    try {
      debugPrint('[fb_mobile_complete_registration] TRIGGERING... (method: $registrationMethod)');
      await _fb.logCompletedRegistration(registrationMethod: registrationMethod);
      debugPrint('[fb_mobile_complete_registration] ✅ SUCCESS');
    } catch (e) {
      debugPrint('[fb_mobile_complete_registration] ❌ FAILED: $e');
    }
  }

  /// Log Purchase standard event for external payments (Razorpay, Stripe, etc.).
  /// Call this after payment verification succeeds.
  /// 
  /// [amount] - Total amount paid (e.g. 299.0)
  /// [currency] - ISO 4217 currency code (e.g. "INR", "USD")
  /// [contentId] - Optional product/plan ID
  /// [contentType] - Optional type (e.g. "subscription", "product")
  /// [orderId] - Optional order/transaction ID for deduplication
  static Future<void> logPurchase({
    required double amount,
    required String currency,
    String? contentId,
    String? contentType,
    String? orderId,
  }) async {
    try {
      if (amount <= 0 || amount.isNaN || amount.isInfinite) {
        debugPrint('[fb_mobile_purchase] ❌ Skipped: amount must be a finite number > 0 (got $amount)');
        return;
      }

      // Normalize types for Meta diagnostics:
      // - amount must be numeric > 0
      // - currency should be ISO 4217 (e.g. INR)
      final safeCurrency = currency.trim().toUpperCase();
      final safeAmount = double.tryParse(amount.toStringAsFixed(2)) ?? amount;

      debugPrint('════════════════════════════════════════════════════════════');
      debugPrint('[fb_mobile_purchase] TRIGGERING...');
      debugPrint('[fb_mobile_purchase] Amount: $safeAmount $safeCurrency');
      debugPrint('[fb_mobile_purchase] ContentId: $contentId');
      debugPrint('[fb_mobile_purchase] OrderId: $orderId');
      debugPrint('════════════════════════════════════════════════════════════');
      
      final Map<String, dynamic> parameters = {
        // Explicit value/currency params for Meta Diagnostics (some views look for `value`)
        'fb_currency': safeCurrency,
        '_valueToSum': safeAmount,
        'value': safeAmount,
        'currency': safeCurrency,
        'amount': safeAmount,
      };
      
      if (contentId != null) {
        parameters['fb_content_id'] = contentId;
      }
      if (contentType != null) {
        parameters['fb_content_type'] = contentType;
      }
      if (orderId != null) {
        parameters['fb_order_id'] = orderId;
      }

      await _fb.logPurchase(
        amount: safeAmount,
        currency: safeCurrency,
        parameters: parameters,
      );
      
      debugPrint('[fb_mobile_purchase] ✅ SUCCESS');
      debugPrint('════════════════════════════════════════════════════════════');
    } catch (e) {
      debugPrint('[fb_mobile_purchase] ❌ FAILED: $e');
    }
  }

  /// Log a custom event (for any non-standard tracking needs).
  /// 
  /// [eventName] - Custom event name (e.g. "TestCompleted", "LessonViewed")
  /// [parameters] - Optional key-value parameters
  /// [valueToSum] - Optional numeric value to aggregate
  static Future<void> logCustomEvent({
    required String eventName,
    Map<String, dynamic>? parameters,
    double? valueToSum,
  }) async {
    try {
      debugPrint('[$eventName] TRIGGERING...');
      await _fb.logEvent(
        name: eventName,
        parameters: parameters,
        valueToSum: valueToSum,
      );
      debugPrint('[$eventName] ✅ SUCCESS');
    } catch (e) {
      debugPrint('[$eventName] ❌ FAILED: $e');
    }
  }

  /// Set the user ID for better attribution (optional).
  /// Call after login if you want to tie events to a specific user.
  static Future<void> setUserId(String userId) async {
    try {
      debugPrint('[META SDK] setUserId($userId)...');
      await _fb.setUserID(userId);
      debugPrint('[META SDK] ✅ User ID set: $userId');
    } catch (e) {
      debugPrint('[META SDK] ❌ setUserId FAILED: $e');
    }
  }

  /// Clear the user ID (call on logout).
  static Future<void> clearUserId() async {
    try {
      await _fb.clearUserID();
      debugPrint('[META SDK] ✅ User ID cleared');
    } catch (e) {
      debugPrint('[META SDK] ❌ clearUserID FAILED: $e');
    }
  }

  /// Enable debug logging for development (shows request/response in logcat).
  /// Only call this in debug builds.
  static Future<void> enableDebugLogging() async {
    if (kDebugMode) {
      debugPrint('[META SDK] DEBUG MODE ENABLED');
      debugPrint('[META SDK] Filter logcat: FB|facebook|Facebook|fb_mobile');
    }
  }
}
