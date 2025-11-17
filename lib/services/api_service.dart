import 'dart:convert';
import 'dart:io';
import 'dart:async';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;

class ApiService {
  // ============================================
  // CONFIGURATION: Change this based on your setup
  // ============================================
  // For Android Emulator: Use '10.0.2.2'
  // For Physical Device: Use your computer's IP (e.g., '192.168.1.4')
  // To find your IP: Windows: ipconfig | Mac/Linux: ifconfig
  static const String androidHost = '192.168.1.4'; // Change to '10.0.2.2' for emulator, or your IP for physical device
  
  // Get base URL based on platform
  static String get baseUrl {
    if (kIsWeb) {
      // Web platform - use localhost
      return 'http://localhost/MockTest/api';
    } else if (Platform.isAndroid) {
      // Android - use configured host
      return 'http://$androidHost/MockTest/api';
    } else if (Platform.isIOS) {
      // iOS - use localhost for simulator, or your computer's IP for physical device
      return 'http://localhost/MockTest/api';
    } else {
      // Default fallback
      return 'http://localhost/MockTest/api';
    }
  }
  
  static const Duration timeout = Duration(seconds: 30);

  static Map<String, String> get headers => {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      };

  static Future<Map<String, dynamic>> sendOTP(String mobile) async {
    try {
      final url = '$baseUrl/auth/send_otp.php';
      print('API Call: POST $url'); // Debug log
      
      final response = await http
          .post(
            Uri.parse(url),
            headers: headers,
            body: jsonEncode({'mobile': mobile}),
          )
          .timeout(timeout);
      
      print('API Response: ${response.statusCode} - ${response.body}'); // Debug log
      return jsonDecode(response.body);
    } catch (e) {
      String errorMsg = 'Connection error';
      if (e is TimeoutException) {
        errorMsg = 'Connection timeout. Check:\n1. XAMPP Apache is running\n2. Using correct IP (10.0.2.2 for emulator, your IP for physical device)\n3. Phone and computer on same WiFi';
      } else if (e is SocketException) {
        errorMsg = 'Cannot reach server at $baseUrl\nCheck network connection and server IP';
      } else {
        errorMsg = 'Error: $e';
      }
      print('API Error: $errorMsg'); // Debug log
      return {'success': false, 'message': errorMsg};
    }
  }

  static Future<Map<String, dynamic>> verifyOTP(String mobile, String otp) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl/auth/verify_otp.php'),
            headers: headers,
            body: jsonEncode({'mobile': mobile, 'otp': otp}),
          )
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
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
      final response = await http
          .post(
            Uri.parse('$baseUrl/users/create.php'),
            headers: headers,
            body: jsonEncode({
              'mobile': mobile,
              'name': name,
              'email': email,
              'age': age,
              'district': district,
              'education': education,
              'language': language,
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
      final response = await http
          .put(
            Uri.parse('$baseUrl/users/update.php'),
            headers: headers,
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
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getUserProfile(int userId) async {
    try {
      final response = await http
          .get(
            Uri.parse('$baseUrl/users/get_profile.php?user_id=$userId'),
            headers: headers,
          )
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
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
      final response = await http
          .get(
            Uri.parse(
                '$baseUrl/tests/get_questions.php?session_id=$sessionId&language=$language'),
            headers: headers,
          )
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
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
      final response = await http
          .post(
            Uri.parse('$baseUrl/tests/submit_result.php'),
            headers: headers,
            body: jsonEncode({
              'user_id': userId,
              'session_id': sessionId,
              'started_at': startedAt,
              'time_taken': timeTaken,
              'answers': answers,
            }),
          )
          .timeout(timeout);
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
      
      final response = await http
          .get(
            Uri.parse(url),
            headers: headers,
          )
          .timeout(timeout);
      
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
      final response = await http
          .get(
            Uri.parse('$baseUrl/tests/get_performance.php?user_id=$userId&period=$period'),
            headers: headers,
          )
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
    }
  }

  static Future<Map<String, dynamic>> getProgressAnalytics({
    required int userId,
  }) async {
    try {
      final url = '$baseUrl/tests/get_progress_analytics.php?user_id=$userId';
      print('API Call: GET $url');
      
      final response = await http
          .get(
            Uri.parse(url),
            headers: headers,
          )
          .timeout(timeout);
      
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
}
