import 'dart:io';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:sms_autofill/sms_autofill.dart';

/// Service to help with SMS Retriever setup
/// Provides app signature hash needed for SMS template
class SmsRetrieverService {
  static String? _cachedSignature;
  
  /// Get the app signature hash (11 characters)
  /// This hash MUST be included at the end of your SMS template
  /// for SMS Retriever to work
  static Future<String?> getAppSignature() async {
    if (kIsWeb || !Platform.isAndroid) {
      print('SmsRetrieverService: Not available on this platform');
      return null;
    }
    
    if (_cachedSignature != null) {
      return _cachedSignature;
    }
    
    try {
      _cachedSignature = await SmsAutoFill().getAppSignature;
      print('═══════════════════════════════════════════════════════════════');
      print('  SMS RETRIEVER - APP SIGNATURE HASH');
      print('═══════════════════════════════════════════════════════════════');
      print('  Hash: $_cachedSignature');
      print('');
      print('  Add this hash to your AuthKey.io SMS template!');
      print('  Example SMS format:');
      print('  "<#> Your OTP is 123456');
      print('  $_cachedSignature"');
      print('');
      print('  Note: The hash MUST be on its own line at the end.');
      print('  Note: Debug and Release builds have DIFFERENT hashes!');
      print('═══════════════════════════════════════════════════════════════');
      return _cachedSignature;
    } catch (e) {
      print('SmsRetrieverService: Error getting app signature - $e');
      return null;
    }
  }
  
  /// Print the app signature to console (call this during development)
  static Future<void> printAppSignature() async {
    await getAppSignature();
  }
  
  /// Check if SMS Retriever is available on this device
  static bool get isAvailable => !kIsWeb && Platform.isAndroid;
}
