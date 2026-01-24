import 'dart:convert';
import 'dart:io';
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

import 'app_navigator.dart';
import '../screens/login_page.dart';

class ApiService {
  // ============================================
  // CONFIGURATION: Change this based on your setup
  // ============================================
  // For Android Emulator: Use '10.0.2.2'
  // For Physical Device: Use your computer's IP (e.g., '192.168.1.4')
  // To find your IP: Windows: ipconfig | Mac/Linux: ifconfig
  static const String androidHost = '192.168.1.4'; // Change to '10.0.2.2' for emulator, or your IP for physical device
  
  // Get base URL based on platform
  // Set to true to use localhost for testing (default OTP: 111111)
  // Set to false to use production (Hostinger) - also supports OTP 111111 now
  static const bool useLocalhost = false;
  
  static String get baseUrl {
    if (useLocalhost) {
      if (kIsWeb) {
        // Web running locally
        return 'http://localhost/MockTest/api';
      } else if (Platform.isAndroid) {
        // Android emulator uses 10.0.2.2, physical device uses your PC IP
        return 'http://$androidHost/MockTest/api';
      } else {
        return 'http://localhost/MockTest/api';
      }
    }
    
    // Production URLs
    if (kIsWeb) {
      return 'https://sudartnpscapp.in/api';
    } else if (Platform.isAndroid) {
      return 'https://sudartnpscapp.in/api';
    } else if (Platform.isIOS) {
      return 'https://sudartnpscapp.in/api';
    } else {
      return 'https://sudartnpscapp.in/api';
    }
  }
  
  static const Duration timeout = Duration(seconds: 30);

  static Map<String, String> _baseHeaders() => {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      };

  // Backwards-compatible: some endpoints are public and still use this.
  static Map<String, String> get headers => _baseHeaders();

  static Future<String> getOrCreateDeviceId() async {
    final prefs = await SharedPreferences.getInstance();
    final existing = prefs.getString('deviceId');
    if (existing != null && existing.isNotEmpty) return existing;

    // Stable per-install identifier (privacy-safe, not hardware-based)
    final deviceId = const Uuid().v4().replaceAll('-', '');
    await prefs.setString('deviceId', deviceId);
    return deviceId;
  }

  static Future<String?> _getToken() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    if (token == null || token.isEmpty) return null;
    return token;
  }

  static Future<Map<String, String>> authHeaders({String? tokenOverride}) async {
    final deviceId = await getOrCreateDeviceId();
    final token = tokenOverride ?? await _getToken();

    final headers = _baseHeaders();
    headers['X-Device-Id'] = deviceId;
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  static Future<void> _handleSessionRevokedIfNeeded(http.Response response) async {
    if (response.statusCode != 401) return;
    try {
      final decoded = jsonDecode(response.body);
      if (decoded is Map && decoded['error_code'] == 'SESSION_REVOKED') {
        final prefs = await SharedPreferences.getInstance();
        await prefs.setBool('isLoggedIn', false);
        await prefs.remove('token');
        await prefs.remove('userId');
        await prefs.remove('userName');
        await prefs.remove('userMobile');
        await prefs.remove('selectedExam');
        await prefs.remove('selectedExamId');

        // Redirect to login (works even if user is currently inside the app)
        final nav = appNavigatorKey.currentState;
        if (nav != null) {
          nav.pushAndRemoveUntil(
            MaterialPageRoute(builder: (_) => const LoginPage()),
            (route) => false,
          );
        }
      }
    } catch (_) {
      // ignore parse errors
    }
  }

  /// Send OTP to mobile number via AuthKey.io SMS Gateway
  /// 
  /// Returns:
  /// - success: true/false
  /// - message: Status message
  /// - mobile: Masked mobile number (e.g., "98****3210")
  /// - expires_in: OTP validity in seconds (600 = 10 minutes)
  /// - resend_in: Cooldown before resend allowed (60 seconds)
  /// - error_code: Error type for handling (INVALID_MOBILE, RATE_LIMITED, etc.)
  static Future<Map<String, dynamic>> sendOTP(String mobile) async {
    try {
      // Clean mobile number - remove spaces, country code
      mobile = mobile.replaceAll(RegExp(r'[\s\-\+]'), '');
      if (mobile.startsWith('91') && mobile.length > 10) {
        mobile = mobile.substring(2);
      }
      
      final url = '$baseUrl/auth/send_otp.php';
      print('📱 ========== SEND OTP DEBUG ==========');
      print('📱 Base URL: $baseUrl');
      print('📱 Full URL: $url');
      print('📱 Mobile: $mobile');
      print('📱 useLocalhost: $useLocalhost');
      
      final response = await http
          .post(
            Uri.parse(url),
            headers: _baseHeaders(),
            body: jsonEncode({'mobile': mobile}),
          )
          .timeout(timeout);
      
      print('📱 OTP Response: ${response.statusCode} - ${response.body}');
      
      final data = jsonDecode(response.body);
      
      // Handle rate limiting
      if (response.statusCode == 429) {
        return {
          'success': false,
          'message': data['message'] ?? 'Too many requests. Please try again later.',
          'error_code': data['error_code'] ?? 'RATE_LIMITED',
          'retry_after': data['retry_after'] ?? 60,
        };
      }
      
      return data;
    } catch (e) {
      String errorMsg = 'Connection error';
      String errorCode = 'CONNECTION_ERROR';
      
      if (e is TimeoutException) {
        errorMsg = 'Connection timeout. Please check your internet connection.';
        errorCode = 'TIMEOUT';
      } else if (e is SocketException) {
        errorMsg = 'Cannot connect to server. Please try again.';
        errorCode = 'NETWORK_ERROR';
      } else {
        errorMsg = 'Something went wrong. Please try again.';
      }
      
      print('📱 OTP Error: $e');
      return {
        'success': false,
        'message': errorMsg,
        'error_code': errorCode,
      };
    }
  }

  /// Verify OTP entered by user
  /// 
  /// Returns:
  /// - success: true/false
  /// - message: Status message
  /// - is_new_user: true if registration needed
  /// - user: User data (if existing user)
  /// - token: Authentication token
  /// - remaining_attempts: Attempts left (on failure)
  static Future<Map<String, dynamic>> verifyOTP(String mobile, String otp) async {
    try {
      // Clean mobile number
      mobile = mobile.replaceAll(RegExp(r'[\s\-\+]'), '');
      if (mobile.startsWith('91') && mobile.length > 10) {
        mobile = mobile.substring(2);
      }
      
      final url = '$baseUrl/auth/verify_otp.php';
      print('✅ Verify OTP API Call: POST $url');
      print('✅ Mobile: $mobile, OTP: $otp');

      final deviceId = await getOrCreateDeviceId();
      
      final response = await http
          .post(
            Uri.parse(url),
            headers: _baseHeaders(),
            body: jsonEncode({'mobile': mobile, 'otp': otp, 'device_id': deviceId}),
          )
          .timeout(timeout);
      
      print('✅ Verify Response: ${response.statusCode} - ${response.body}');
      
      final data = jsonDecode(response.body);
      
      // Handle max attempts exceeded
      if (response.statusCode == 429) {
        return {
          'success': false,
          'message': data['message'] ?? 'Too many failed attempts. Please request a new OTP.',
          'error_code': 'MAX_ATTEMPTS_EXCEEDED',
        };
      }
      
      return data;
    } catch (e) {
      print('✅ Verify Error: $e');
      return {
        'success': false,
        'message': 'Verification failed. Please try again.',
        'error_code': 'VERIFICATION_ERROR',
      };
    }
  }

  /// Resend OTP with rate limiting protection
  /// 
  /// Returns same structure as sendOTP
  static Future<Map<String, dynamic>> resendOTP(String mobile) async {
    try {
      // Clean mobile number
      mobile = mobile.replaceAll(RegExp(r'[\s\-\+]'), '');
      if (mobile.startsWith('91') && mobile.length > 10) {
        mobile = mobile.substring(2);
      }
      
      final url = '$baseUrl/auth/resend_otp.php';
      print('🔄 Resend OTP API Call: POST $url');
      
      final response = await http
          .post(
            Uri.parse(url),
            headers: _baseHeaders(),
            body: jsonEncode({'mobile': mobile}),
          )
          .timeout(timeout);
      
      print('🔄 Resend Response: ${response.statusCode} - ${response.body}');
      
      final data = jsonDecode(response.body);
      
      // Handle rate limiting and cooldown
      if (response.statusCode == 429) {
        return {
          'success': false,
          'message': data['message'] ?? 'Please wait before requesting a new OTP.',
          'error_code': data['error_code'] ?? 'COOLDOWN_ACTIVE',
          'retry_after': data['retry_after'] ?? 60,
        };
      }
      
      return data;
    } catch (e) {
      print('🔄 Resend Error: $e');
      return {
        'success': false,
        'message': 'Failed to resend OTP. Please try again.',
        'error_code': 'RESEND_ERROR',
      };
    }
  }

  /// Truecaller Login - verify access token with backend
  /// 
  /// The backend will:
  /// 1. Call Truecaller /userinfo endpoint to verify the token
  /// 2. Extract the verified phone number
  /// 3. Check if user exists or is new
  /// 4. Return session token for existing users
  /// 
  /// Returns same structure as verifyOTP:
  /// - success: true/false
  /// - is_new_user: true if registration needed
  /// - user: User data (if existing user)
  /// - token: Authentication token
  /// - mobile: Phone number (for new user registration)
  static Future<Map<String, dynamic>> truecallerLogin({
    required String accessToken,
  }) async {
    try {
      final deviceId = await getOrCreateDeviceId();
      final url = '$baseUrl/auth/truecaller_login.php';
      
      print('📱 ========== TRUECALLER LOGIN DEBUG ==========');
      print('📱 URL: $url');
      print('📱 Access Token length: ${accessToken.length}');
      print('📱 Device ID: $deviceId');
      
      final response = await http
          .post(
            Uri.parse(url),
            headers: _baseHeaders(),
            body: jsonEncode({
              'access_token': accessToken,
              'device_id': deviceId,
            }),
          )
          .timeout(timeout);
      
      print('📱 Truecaller Login Response: ${response.statusCode} - ${response.body}');
      
      final data = jsonDecode(response.body);
      
      if (response.statusCode == 401) {
        return {
          'success': false,
          'message': data['message'] ?? 'Truecaller verification failed',
          'error_code': data['error_code'] ?? 'TRUECALLER_VERIFICATION_FAILED',
        };
      }
      
      return data;
    } catch (e) {
      String errorMsg = 'Connection error';
      String errorCode = 'CONNECTION_ERROR';
      
      if (e is TimeoutException) {
        errorMsg = 'Connection timeout. Please check your internet connection.';
        errorCode = 'TIMEOUT';
      } else if (e is SocketException) {
        errorMsg = 'Cannot connect to server. Please try again.';
        errorCode = 'NETWORK_ERROR';
      } else {
        errorMsg = 'Something went wrong. Please try again.';
      }
      
      print('📱 Truecaller Login Error: $e');
      return {
        'success': false,
        'message': errorMsg,
        'error_code': errorCode,
      };
    }
  }

  static Future<Map<String, dynamic>> createUser({
    required String mobile,
    required String name,
    String? email,
    int? age,
    String? district,
    String? education,
    String language = 'en',
  }) async {
    try {
      final deviceId = await getOrCreateDeviceId();
      final response = await http
          .post(
            Uri.parse('$baseUrl/users/create.php'),
            headers: _baseHeaders(),
            body: jsonEncode({
              'mobile': mobile,
              'name': name,
              'email': email,
              'age': age,
              'district': district,
              'education': education,
              'language': language,
              'device_id': deviceId,
            }),
          )
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> updateUser({
    required int userId,
    String? name,
    String? email,
    int? age,
    String? district,
    String? education,
    String? language,
    String? profilePic,
  }) async {
    try {
      final hdrs = await authHeaders();
      final response = await http
          .put(
            Uri.parse('$baseUrl/users/update.php'),
            headers: hdrs,
            body: jsonEncode({
              'user_id': userId,
              'name': name,
              'email': email,
              'age': age,
              'district': district,
              'education': education,
              'language': language,
              'profile_pic': profilePic,
            }),
          )
          .timeout(timeout);
      await _handleSessionRevokedIfNeeded(response);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getUserProfile(int userId) async {
    try {
      final hdrs = await authHeaders();
      final response = await http
          .get(
            Uri.parse('$baseUrl/users/get_profile.php?user_id=$userId'),
            headers: hdrs,
          )
          .timeout(timeout);
      await _handleSessionRevokedIfNeeded(response);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  // Guest login for Google reviews (temporary)
  static Future<Map<String, dynamic>> guestLogin() async {
    try {
      // Guest user ID: U015
      final userId = 15; // Assuming U015 maps to user ID 15
      final response = await http
          .get(
            Uri.parse('$baseUrl/users/get_profile.php?user_id=$userId'),
            headers: headers,
          )
          .timeout(timeout);
      
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true && data['user'] != null) {
          return {
            'success': true,
            'message': 'Guest login successful',
            'is_new_user': false,
            'user': data['user'],
            'token': base64Encode(utf8.encode('$userId:${DateTime.now().millisecondsSinceEpoch}'))
          };
        }
      }
      
      // Fallback: return guest user data even if API fails
      return {
        'success': true,
        'message': 'Guest login successful',
        'is_new_user': false,
        'user': {
          'id': userId,
          'name': 'Guest User',
          'mobile': '0000000000',
          'email': 'guest@example.com',
        },
        'token': base64Encode(utf8.encode('$userId:${DateTime.now().millisecondsSinceEpoch}'))
      };
    } catch (e) {
      // Fallback: return guest user data even if API fails
      return {
        'success': true,
        'message': 'Guest login successful',
        'is_new_user': false,
        'user': {
          'id': 15,
          'name': 'Guest User',
          'mobile': '0000000000',
          'email': 'guest@example.com',
        },
        'token': base64Encode(utf8.encode('15:${DateTime.now().millisecondsSinceEpoch}'))
      };
    }
  }

  static Future<Map<String, dynamic>> getTestCategories({int? examId, int? userId}) async {
    try {
      String url = '$baseUrl/tests/get_categories.php';
      List<String> params = [];
      if (examId != null) {
        params.add('exam_id=$examId');
      }
      if (userId != null) {
        params.add('user_id=$userId');
      }
      if (params.isNotEmpty) {
        url += '?${params.join('&')}';
      }
      final response = await http
          .get(Uri.parse(url), headers: headers)
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getQuestionSessions({int? categoryId}) async {
    try {
      String url = '$baseUrl/tests/get_sessions.php';
      if (categoryId != null) {
        url += '?category_id=$categoryId';
      }
      final response = await http
          .get(Uri.parse(url), headers: headers)
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getQuestions({
    required int sessionId,
    String language = 'en',
  }) async {
    try {
      final url = '$baseUrl/tests/get_questions.php?session_id=$sessionId&language=$language';
      print('🔷 API Call: GET $url');
      
      final response = await http
          .get(
            Uri.parse(url),
            headers: headers,
          )
          .timeout(timeout);
      
      print('🔷 API Response Status: ${response.statusCode}');
      print('🔷 API Response Body (first 2000 chars): ${response.body.substring(0, response.body.length > 2000 ? 2000 : response.body.length)}');
      
      final decoded = jsonDecode(response.body);
      
      // Print first question's HTML for debugging
      if (decoded['success'] == true && decoded['questions'] != null && (decoded['questions'] as List).isNotEmpty) {
        final firstQ = decoded['questions'][0];
        print('🔷 FIRST QUESTION DATA:');
        print('   Question EN: ${firstQ['question_en']?.substring(0, firstQ['question_en']?.length > 300 ? 300 : firstQ['question_en']?.length)}');
        print('   Question TA: ${firstQ['question_ta']?.substring(0, firstQ['question_ta']?.length > 300 ? 300 : firstQ['question_ta']?.length)}');
      }
      
      return decoded;
    } catch (e) {
      print('🔷 API Error: $e');
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> submitTestResult({
    required int userId,
    required int sessionId,
    required String startedAt,
    required int timeTaken,
    required List<Map<String, dynamic>> answers,
  }) async {
    try {
      final hdrs = await authHeaders();
      final response = await http
          .post(
            Uri.parse('$baseUrl/tests/submit_result.php'),
            headers: hdrs,
            body: jsonEncode({
              'user_id': userId,
              'session_id': sessionId,
              'started_at': startedAt,
              'time_taken': timeTaken,
              'answers': answers,
            }),
          )
          .timeout(timeout);
      await _handleSessionRevokedIfNeeded(response);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getTestHistory({
    required int userId,
    int limit = 50,
  }) async {
    try {
      final url = '$baseUrl/tests/get_history.php?user_id=$userId&limit=$limit';
      print('API Call: GET $url');
      final hdrs = await authHeaders();
      
      final response = await http
          .get(
            Uri.parse(url),
            headers: hdrs,
          )
          .timeout(timeout);
      await _handleSessionRevokedIfNeeded(response);
      
      print('API Response Status: ${response.statusCode}');
      print('API Response Body: ${response.body}');
      
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        return {
          'success': false,
          'message': 'Server error: ${response.statusCode} - ${response.body}'
        };
      }
    } catch (e) {
      print('API Error: $e');
      String errorMsg = 'Error: $e';
      if (e.toString().contains('Failed host lookup') || e.toString().contains('Connection refused')) {
        errorMsg = 'Cannot connect to server. Make sure XAMPP Apache is running at http://localhost';
      }
      return {'success': false, 'message': errorMsg};
    }
  }

  static Future<Map<String, dynamic>> getRankings({
    int? sessionId,
    int limit = 100,
  }) async {
    try {
      String url = '$baseUrl/tests/get_rankings.php?limit=$limit';
      if (sessionId != null) {
        url += '&session_id=$sessionId';
      }
      final response = await http
          .get(Uri.parse(url), headers: headers)
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getExamCategories() async {
    try {
      final response = await http
          .get(Uri.parse('$baseUrl/tests/get_exam_categories.php'), headers: headers)
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getPerformance({
    required int userId,
    String period = 'all', // week, month, year, all
  }) async {
    try {
      final hdrs = await authHeaders();
      final response = await http
          .get(
            Uri.parse('$baseUrl/tests/get_performance.php?user_id=$userId&period=$period'),
            headers: hdrs,
          )
          .timeout(timeout);
      await _handleSessionRevokedIfNeeded(response);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  // Submit Feedback
  static Future<Map<String, dynamic>> submitFeedback({
    required String message,
    String? subject,
    int? userId,
    String? userName,
    String? userEmail,
    String? userMobile,
    int? rating,
    String category = 'general',
  }) async {
    try {
      final url = '$baseUrl/feedback/submit.php';
      print('API Call: POST $url');
      
      final response = await http
          .post(
            Uri.parse(url),
            headers: headers,
            body: jsonEncode({
              'user_name': userName ?? 'Anonymous',
              'user_email': userEmail,
              'user_mobile': userMobile,
              'user_id': userId,
              'subject': subject ?? 'App Feedback',
              'message': message,
              'rating': rating,
              'category': category,
            }),
          )
          .timeout(timeout);
      
      print('Feedback API Response Status: ${response.statusCode}');
      print('Feedback API Response Body: ${response.body}');
      
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        return {
          'success': false,
          'message': 'Server error: ${response.statusCode}'
        };
      }
    } catch (e) {
      print('Feedback API Error: $e');
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getProgressAnalytics({
    required int userId,
  }) async {
    try {
      final url = '$baseUrl/tests/get_progress_analytics.php?user_id=$userId';
      print('API Call: GET $url');
      final hdrs = await authHeaders();
      
      final response = await http
          .get(
            Uri.parse(url),
            headers: hdrs,
          )
          .timeout(timeout);
      await _handleSessionRevokedIfNeeded(response);
      
      print('API Response Status: ${response.statusCode}');
      print('API Response Body: ${response.body}');
      
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        return {
          'success': false,
          'message': 'Server error: ${response.statusCode} - ${response.body}'
        };
      }
    } catch (e) {
      print('API Error: $e');
      String errorMsg = 'Error: $e';
      if (e.toString().contains('Failed host lookup') || e.toString().contains('Connection refused')) {
        errorMsg = 'Cannot connect to server. Make sure XAMPP Apache is running at http://localhost';
      }
      return {'success': false, 'message': errorMsg};
    }
  }

  static Future<Map<String, dynamic>> getTestDetails({
    required int resultId,
    int? userId,
  }) async {
    try {
      String url = '$baseUrl/tests/get_test_details.php?result_id=$resultId';
      if (userId != null) {
        url += '&user_id=$userId';
      }
      final hdrs = userId != null ? await authHeaders() : _baseHeaders();
      final response = await http
          .get(
            Uri.parse(url),
            headers: hdrs,
          )
          .timeout(timeout);
      await _handleSessionRevokedIfNeeded(response);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  // Update OneSignal Player ID
  static Future<Map<String, dynamic>> updateOneSignalPlayerId({
    required int userId,
    required String playerId,
  }) async {
    try {
      final url = '$baseUrl/users/update_onesignal_id.php';
      print('API Call: POST $url');
      final hdrs = await authHeaders();
      
      final response = await http
          .post(
            Uri.parse(url),
            headers: hdrs,
            body: jsonEncode({
              'user_id': userId,
              'player_id': playerId,
            }),
          )
          .timeout(timeout);
      await _handleSessionRevokedIfNeeded(response);
      
      print('API Response Status: ${response.statusCode}');
      print('API Response Body: ${response.body}');
      
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        return {
          'success': false,
          'message': 'Server error: ${response.statusCode}'
        };
      }
    } catch (e) {
      print('API Error: $e');
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getLeaderboard({
    int? sessionId,
    int? categoryId,
    String period = 'all',
    int limit = 100,
  }) async {
    try {
      String url = '$baseUrl/tests/get_leaderboard.php?period=$period&limit=$limit';
      if (sessionId != null) {
        url += '&session_id=$sessionId';
      }
      if (categoryId != null) {
        url += '&category_id=$categoryId';
      }
      final response = await http
          .get(
            Uri.parse(url),
            headers: headers,
          )
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getNotifications({
    int? userId,
    bool? isRead,
    int limit = 50,
    int offset = 0,
  }) async {
    try {
      String url = '$baseUrl/admin/notifications/crud.php?limit=$limit&offset=$offset';
      if (userId != null) {
        url += '&user_id=$userId';
      }
      if (isRead != null) {
        url += '&is_read=$isRead';
      }
      final response = await http
          .get(
            Uri.parse(url),
            headers: headers,
          )
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getPublicSettings({String? key}) async {
    try {
      String url = '$baseUrl/settings/get_public.php';
      if (key != null) {
        url += '?key=$key';
      }
      final response = await http
          .get(
            Uri.parse(url),
            headers: headers,
          )
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  /// Check if this device is still the active device for this account.
  /// Returns SESSION_REVOKED (401) if logged in from another device.
  static Future<Map<String, dynamic>> checkSession({required int userId}) async {
    try {
      final url = '$baseUrl/auth/check_session.php';
      final hdrs = await authHeaders();
      final response = await http
          .post(
            Uri.parse(url),
            headers: hdrs,
            body: jsonEncode({'user_id': userId}),
          )
          .timeout(timeout);
      await _handleSessionRevokedIfNeeded(response);
      final decoded = jsonDecode(response.body);
      if (decoded is Map<String, dynamic>) {
        decoded['http_status'] = response.statusCode;
        return decoded;
      }
      return {
        'success': false,
        'message': 'Invalid response',
        'http_status': response.statusCode,
      };
    } catch (e) {
      return {'success': false, 'message': 'Error: $e', 'local_error': true};
    }
  }

  static Future<bool> checkConnection() async {
    try {
      final result = await InternetAddress.lookup('google.com').timeout(
        const Duration(seconds: 5),
      );
      return result.isNotEmpty && result[0].rawAddress.isNotEmpty;
    } on SocketException catch (_) {
      return false;
    } on TimeoutException catch (_) {
      return false;
    }
  }

  static String getErrorMessage(dynamic error) {
    if (error is SocketException) {
      return 'No internet connection';
    } else if (error is TimeoutException) {
      return 'Connection timeout';
    } else if (error is http.ClientException) {
      return 'Network error';
    } else {
      return 'Something went wrong';
    }
  }

  /// Get the public base URL (without /api suffix) for serving static files
  static String get publicBaseUrl {
    final base = baseUrl;
    // Remove trailing /api to get the public root
    if (base.endsWith('/api')) {
      return base.substring(0, base.length - 4);
    }
    return base;
  }

  /// Resolve a test category image path to a full URL
  /// 
  /// Handles various input formats:
  /// - `uploads/test_categories/xxx.jpg` → full URL
  /// - `test_category_xxx.jpg` (filename only) → full URL with path prefix
  /// - `/uploads/test_categories/xxx.jpg` (leading slash) → normalized
  /// - `null` or empty → returns null
  static String? resolveTestCategoryImageUrl(String? imagePath) {
    if (imagePath == null || imagePath.trim().isEmpty) {
      return null;
    }

    String path = imagePath.trim();

    // Remove leading slash if present
    if (path.startsWith('/')) {
      path = path.substring(1);
    }

    // If it's just a filename (no directory), prefix with uploads/test_categories/
    if (!path.contains('/')) {
      path = 'uploads/test_categories/$path';
    }

    // Build full URL
    return '$publicBaseUrl/$path';
  }
}
