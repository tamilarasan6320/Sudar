import 'dart:convert';
import 'dart:io';
import 'dart:async';

import 'package:http/http.dart' as http;

class ApiService {
  static const String baseUrl = 'http://localhost/MockTest/api';
  static const Duration timeout = Duration(seconds: 30);

  static Map<String, String> get headers => {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      };

  static Future<Map<String, dynamic>> sendOTP(String mobile) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl/auth/send_otp.php'),
            headers: headers,
            body: jsonEncode({'mobile': mobile}),
          )
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
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

  static Future<Map<String, dynamic>> getTestCategories({int? examId}) async {
    try {
      String url = '$baseUrl/tests/get_categories.php';
      if (examId != null) {
        url += '?exam_id=$examId';
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
      final response = await http
          .get(
            Uri.parse('$baseUrl/tests/get_history.php?user_id=$userId&limit=$limit'),
            headers: headers,
          )
          .timeout(timeout);
      return jsonDecode(response.body);
    } catch (e) {
      return {'success': false, 'message': 'Error: $e'};
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
