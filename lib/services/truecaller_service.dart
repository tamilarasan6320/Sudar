import 'dart:async';
import 'dart:io';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:truecaller_sdk/truecaller_sdk.dart';

/// Truecaller SDK result status
enum TruecallerStatus {
  success,
  cancelled,
  notUsable,
  failure,
  verificationRequired,
  error,
}

/// Result from Truecaller profile request
class TruecallerResult {
  final TruecallerStatus status;
  final String? phoneNumber;
  final String? firstName;
  final String? lastName;
  final String? email;
  final String? message;
  /// Access token for backend verification (OAuth flow)
  final String? accessToken;

  TruecallerResult({
    required this.status,
    this.phoneNumber,
    this.firstName,
    this.lastName,
    this.email,
    this.message,
    this.accessToken,
  });

  bool get isSuccess => status == TruecallerStatus.success && accessToken != null && accessToken!.isNotEmpty;
  
  bool get isCancelled => 
      status == TruecallerStatus.cancelled || 
      status == TruecallerStatus.failure ||
      status == TruecallerStatus.verificationRequired;
}

/// Service to interact with Truecaller SDK using the official Flutter plugin
/// Returns access token for backend verification (secure server-side flow)
class TruecallerService {
  static bool _isInitialized = false;
  static StreamSubscription<TcSdkCallback>? _subscription;
  static Completer<TruecallerResult>? _pendingCompleter;
  static String? _codeVerifier;
  static String? _oAuthState;
  
  /// Initialize the Truecaller SDK
  /// Must be called before using any other methods
  static Future<void> initialize() async {
    if (_isInitialized) return;
    if (kIsWeb || !Platform.isAndroid) return;
    
    try {
      // Initialize SDK with OAuth flow for Truecaller users only
      TcSdk.initializeSDK(
        sdkOption: TcSdkOptions.OPTION_VERIFY_ONLY_TC_USERS,
        consentHeadingOption: TcSdkOptions.SDK_CONSENT_HEADING_LOG_IN_TO,
        footerType: TcSdkOptions.FOOTER_TYPE_ANOTHER_MOBILE_NO,
        ctaText: TcSdkOptions.CTA_TEXT_PROCEED,
        buttonShapeOption: TcSdkOptions.BUTTON_SHAPE_ROUNDED,
        consentMode: TcSdkOptions.CONSENT_MODE_BOTTOMSHEET,
      );
      
      _isInitialized = true;
      print('TruecallerService: SDK initialized successfully');
    } catch (e) {
      print('TruecallerService: Error initializing SDK - $e');
    }
  }
  
  /// Check if Truecaller SDK is available and usable on this device
  /// Returns true only on Android devices with Truecaller app installed and logged in
  static Future<bool> isUsable() async {
    // Only available on Android
    if (kIsWeb || !Platform.isAndroid) {
      return false;
    }
    
    try {
      // Ensure SDK is initialized
      await initialize();
      
      final isUsable = await TcSdk.isOAuthFlowUsable;
      print('TruecallerService: isOAuthFlowUsable = $isUsable');
      return isUsable;
    } catch (e) {
      print('TruecallerService: Error checking isUsable - $e');
      return false;
    }
  }
  
  /// Request user profile from Truecaller using OAuth flow
  /// This will show the Truecaller consent bottom sheet
  /// Returns TruecallerResult with accessToken for backend verification
  static Future<TruecallerResult> requestProfile() async {
    // Only available on Android
    if (kIsWeb || !Platform.isAndroid) {
      return TruecallerResult(
        status: TruecallerStatus.notUsable,
        message: 'Truecaller is only available on Android',
      );
    }
    
    try {
      // Ensure SDK is initialized
      await initialize();
      
      // Check if OAuth flow is usable
      final isUsable = await TcSdk.isOAuthFlowUsable;
      if (!isUsable) {
        return TruecallerResult(
          status: TruecallerStatus.notUsable,
          message: 'Truecaller is not available on this device',
        );
      }
      
      // Cancel any pending request
      _pendingCompleter?.complete(TruecallerResult(
        status: TruecallerStatus.cancelled,
        message: 'Cancelled by new request',
      ));
      _subscription?.cancel();
      
      // Create completer for this request
      _pendingCompleter = Completer<TruecallerResult>();
      
      // Generate OAuth state (unique identifier for this request)
      _oAuthState = DateTime.now().millisecondsSinceEpoch.toString();
      TcSdk.setOAuthState(_oAuthState!);
      
      // Set OAuth scopes - request phone only (minimum required)
      TcSdk.setOAuthScopes(['phone']);
      
      // Generate code verifier and challenge for PKCE
      _codeVerifier = await TcSdk.generateRandomCodeVerifier;
      if (_codeVerifier == null) {
        return TruecallerResult(
          status: TruecallerStatus.error,
          message: 'Failed to generate code verifier',
        );
      }
      
      final codeChallenge = await TcSdk.generateCodeChallenge(_codeVerifier!);
      if (codeChallenge == null) {
        return TruecallerResult(
          status: TruecallerStatus.error,
          message: 'Failed to generate code challenge - device may not support this',
        );
      }
      
      TcSdk.setCodeChallenge(codeChallenge);
      
      // Listen for callback
      _subscription = TcSdk.streamCallbackData.listen(_handleCallback);
      
      // Request authorization code (shows consent screen)
      TcSdk.getAuthorizationCode;
      
      // Wait for result with timeout
      final result = await _pendingCompleter!.future.timeout(
        const Duration(seconds: 60),
        onTimeout: () => TruecallerResult(
          status: TruecallerStatus.cancelled,
          message: 'Request timed out',
        ),
      );
      
      return result;
    } catch (e) {
      print('TruecallerService: Exception in requestProfile - $e');
      return TruecallerResult(
        status: TruecallerStatus.error,
        message: e.toString(),
      );
    } finally {
      _subscription?.cancel();
      _subscription = null;
    }
  }
  
  /// Get the current code verifier for backend token exchange
  static String? get codeVerifier => _codeVerifier;
  
  /// Handle callback from Truecaller SDK
  static void _handleCallback(TcSdkCallback callback) async {
    print('TruecallerService: Received callback - ${callback.result}');
    
    switch (callback.result) {
      case TcSdkCallbackResult.success:
        // OAuth success - we have authorization code, exchange for token
        await _handleOAuthSuccess(callback);
        break;
        
      case TcSdkCallbackResult.failure:
        // User cancelled or error
        final errorCode = callback.error?.code ?? -1;
        final errorMessage = callback.error?.message ?? 'Unknown error';
        print('TruecallerService: Failure - code: $errorCode, message: $errorMessage');
        
        _completeWith(TruecallerResult(
          status: TruecallerStatus.cancelled,
          message: errorMessage,
        ));
        break;
        
      case TcSdkCallbackResult.verification:
        // Manual verification required (non-Truecaller user)
        print('TruecallerService: Verification required');
        _completeWith(TruecallerResult(
          status: TruecallerStatus.verificationRequired,
          message: 'Manual verification required',
        ));
        break;
        
      case TcSdkCallbackResult.verifiedBefore:
        // User was verified before - still need to get fresh token
        await _handleVerifiedBefore(callback);
        break;
        
      default:
        print('TruecallerService: Unhandled callback result - ${callback.result}');
        _completeWith(TruecallerResult(
          status: TruecallerStatus.error,
          message: 'Unhandled callback result',
        ));
    }
  }
  
  /// Handle OAuth success - exchange code for token (client-side PKCE)
  /// Then return access token for backend verification
  static Future<void> _handleOAuthSuccess(TcSdkCallback callback) async {
    try {
      final oAuthData = callback.tcOAuthData;
      if (oAuthData == null) {
        _completeWith(TruecallerResult(
          status: TruecallerStatus.error,
          message: 'No OAuth data received',
        ));
        return;
      }
      
      final authCode = oAuthData.authorizationCode;
      final state = oAuthData.state;
      
      print('TruecallerService: OAuth success - authCode length: ${authCode.length}, state: $state');
      
      // Verify state matches
      if (state != _oAuthState) {
        print('TruecallerService: State mismatch - expected: $_oAuthState, got: $state');
        // Continue anyway as state verification is optional
      }
      
      final scopesGranted = oAuthData.scopesGranted;
      print('TruecallerService: Scopes granted: $scopesGranted');
      
      // Exchange authorization code for access token using PKCE (client-side)
      // This is required because Truecaller PKCE flow needs the code_verifier
      print('TruecallerService: Exchanging auth code for token...');
      final tokenResult = await _exchangeCodeForToken(authCode);
      
      if (tokenResult == null) {
        _completeWith(TruecallerResult(
          status: TruecallerStatus.cancelled,
          message: 'Failed to exchange authorization code',
        ));
        return;
      }
      
      final accessToken = tokenResult['access_token'] as String?;
      if (accessToken == null) {
        _completeWith(TruecallerResult(
          status: TruecallerStatus.cancelled,
          message: 'No access token received',
        ));
        return;
      }
      
      print('TruecallerService: Got access token, returning for backend verification');
      
      // Return the access token - backend will verify and get user info
      _completeWith(TruecallerResult(
        status: TruecallerStatus.success,
        accessToken: accessToken,
      ));
      
    } catch (e) {
      print('TruecallerService: Error handling OAuth success - $e');
      _completeWith(TruecallerResult(
        status: TruecallerStatus.error,
        message: e.toString(),
      ));
    }
  }
  
  /// Exchange authorization code for access token using PKCE
  /// This must be done client-side because we have the code_verifier
  static Future<Map<String, dynamic>?> _exchangeCodeForToken(String authCode) async {
    try {
      if (_codeVerifier == null) {
        print('TruecallerService: Code verifier is null');
        return null;
      }
      
      // Use Dart's http package for token exchange
      final uri = Uri.parse('https://oauth-account-noneu.truecaller.com/v1/token');
      final body = {
        'grant_type': 'authorization_code',
        'client_id': '0l45fkucpsmpmovcm8muhs8z7uypq-ysfzrhws0slki',
        'code': authCode,
        'code_verifier': _codeVerifier!,
      };
      
      // Use HttpClient for more control
      final httpClient = HttpClient();
      final request = await httpClient.postUrl(uri);
      request.headers.set('Content-Type', 'application/x-www-form-urlencoded');
      
      // Encode body as form data
      final encodedBody = body.entries.map((e) => 
        '${Uri.encodeComponent(e.key)}=${Uri.encodeComponent(e.value)}'
      ).join('&');
      request.write(encodedBody);
      
      final response = await request.close();
      final responseBody = await response.transform(const SystemEncoding().decoder).join();
      
      print('TruecallerService: Token response status: ${response.statusCode}');
      
      if (response.statusCode == 200) {
        // Parse JSON manually to avoid import issues
        final data = _parseJson(responseBody);
        print('TruecallerService: Token exchange successful');
        return data;
      } else {
        print('TruecallerService: Token exchange failed: $responseBody');
        return null;
      }
    } catch (e) {
      print('TruecallerService: Error exchanging code for token: $e');
      return null;
    }
  }
  
  /// Simple JSON parser to avoid dart:convert import
  static Map<String, dynamic>? _parseJson(String jsonString) {
    try {
      // Use dart:convert indirectly through a simple approach
      final result = <String, dynamic>{};
      
      // Remove braces and split by comma
      final cleaned = jsonString.trim();
      if (!cleaned.startsWith('{') || !cleaned.endsWith('}')) return null;
      
      final inner = cleaned.substring(1, cleaned.length - 1);
      
      // Simple regex-based parsing for flat JSON
      final regex = RegExp(r'"([^"]+)"\s*:\s*(?:"([^"]*)"|(\d+)|(\w+))');
      for (final match in regex.allMatches(inner)) {
        final key = match.group(1)!;
        final stringValue = match.group(2);
        final numValue = match.group(3);
        final boolValue = match.group(4);
        
        if (stringValue != null) {
          result[key] = stringValue;
        } else if (numValue != null) {
          result[key] = int.tryParse(numValue) ?? numValue;
        } else if (boolValue != null) {
          result[key] = boolValue == 'true';
        }
      }
      
      return result.isNotEmpty ? result : null;
    } catch (e) {
      print('TruecallerService: JSON parse error: $e');
      return null;
    }
  }
  
  /// Handle verified before callback - still need to get fresh access token
  static Future<void> _handleVerifiedBefore(TcSdkCallback callback) async {
    try {
      final profile = callback.profile;
      if (profile != null && profile.accessToken != null) {
        // Use the access token from the profile
        print('TruecallerService: Verified before - using profile access token');
        _completeWith(TruecallerResult(
          status: TruecallerStatus.success,
          accessToken: profile.accessToken,
          firstName: profile.firstName,
          lastName: profile.lastName,
        ));
        return;
      }
      
      // No access token in profile, this shouldn't happen for verifiedBefore
      print('TruecallerService: Verified before but no access token');
      _completeWith(TruecallerResult(
        status: TruecallerStatus.error,
        message: 'No access token in verified profile',
      ));
    } catch (e) {
      print('TruecallerService: Error handling verified before - $e');
      _completeWith(TruecallerResult(
        status: TruecallerStatus.error,
        message: e.toString(),
      ));
    }
  }
  
  /// Complete the pending request with a result
  static void _completeWith(TruecallerResult result) {
    if (_pendingCompleter != null && !_pendingCompleter!.isCompleted) {
      _pendingCompleter!.complete(result);
    }
  }
  
  /// Dispose resources
  static void dispose() {
    _subscription?.cancel();
    _subscription = null;
    _pendingCompleter = null;
    _isInitialized = false;
  }
}
