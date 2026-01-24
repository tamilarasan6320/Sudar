import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:razorpay_flutter/razorpay_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:google_fonts/google_fonts.dart';
import '../services/api_service.dart';
import '../services/meta_app_events_service.dart';
import '../services/google_analytics_service.dart';

class SubscriptionPage extends StatefulWidget {
  const SubscriptionPage({super.key});

  @override
  State<SubscriptionPage> createState() => _SubscriptionPageState();
}

class _SubscriptionPageState extends State<SubscriptionPage> {
  late Razorpay _razorpay;
  bool _isLoading = true;
  bool _isProcessing = false;
  bool _isPremium = false;
  Map<String, dynamic>? _subscription;
  String? _error;
  String? _currentSubscriptionId; // Store subscription ID for verification

  @override
  void initState() {
    super.initState();
    _razorpay = Razorpay();
    _razorpay.on(Razorpay.EVENT_PAYMENT_SUCCESS, _handlePaymentSuccess);
    _razorpay.on(Razorpay.EVENT_PAYMENT_ERROR, _handlePaymentError);
    _razorpay.on(Razorpay.EVENT_EXTERNAL_WALLET, _handleExternalWallet);
    _loadSubscriptionStatus();
  }

  @override
  void dispose() {
    _razorpay.clear();
    super.dispose();
  }

  Future<void> _loadSubscriptionStatus() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('user_id') ?? prefs.getInt('userId');

      if (userId == null) {
        setState(() {
          _error = 'Please login to continue';
          _isLoading = false;
        });
        return;
      }

      final response = await http.get(
        Uri.parse('${ApiService.baseUrl}/subscriptions/status.php?user_id=$userId'),
      ).timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        setState(() {
          _isPremium = data['is_premium'] ?? false;
          _subscription = data['subscription'];
          _isLoading = false;
        });
      } else {
        setState(() {
          _error = 'Failed to load subscription status';
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() {
        _error = 'Connection error. Please try again.';
        _isLoading = false;
      });
    }
  }

  Future<void> _startSubscription() async {
    setState(() => _isProcessing = true);

    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('user_id') ?? prefs.getInt('userId');

      debugPrint('🔵 [SUBSCRIPTION] Starting subscription for user_id: $userId');

      if (userId == null) {
        debugPrint('❌ [SUBSCRIPTION] User ID is null');
        _showError('Please login to continue');
        return;
      }

      final url = '${ApiService.baseUrl}/subscriptions/create.php';
      debugPrint('🔵 [SUBSCRIPTION] API URL: $url');
      debugPrint('🔵 [SUBSCRIPTION] Request body: {"user_id": $userId}');

      final response = await http.post(
        Uri.parse(url),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({'user_id': userId}),
      ).timeout(const Duration(seconds: 30));

      debugPrint('🔵 [SUBSCRIPTION] Response status: ${response.statusCode}');
      debugPrint('🔵 [SUBSCRIPTION] Response body: ${response.body}');

      final data = json.decode(response.body);

      if (data['success'] == true) {
        final subData = data['data'];
        
        debugPrint('✅ [SUBSCRIPTION] Success! Subscription ID: ${subData['subscription_id']}');
        debugPrint('🔵 [SUBSCRIPTION] Key: ${subData['key']}');
        
        // Store subscription ID for verification after payment
        _currentSubscriptionId = subData['subscription_id'];

        var options = {
          'key': subData['key'],
          'subscription_id': subData['subscription_id'],
          'name': subData['name'],
          'description': subData['description'],
          'image': subData['image'],
          'prefill': subData['prefill'],
          'theme': subData['theme'],
          'notes': subData['notes'],
        };

        debugPrint('🔵 [SUBSCRIPTION] Opening Razorpay with options: $options');
        _razorpay.open(options);
      } else {
        debugPrint('❌ [SUBSCRIPTION] API Error: ${data['message']}');
        debugPrint('❌ [SUBSCRIPTION] Full response: $data');
        _showError(data['message'] ?? 'Failed to create subscription');
      }
    } catch (e, stackTrace) {
      debugPrint('❌ [SUBSCRIPTION] Exception: $e');
      debugPrint('❌ [SUBSCRIPTION] Stack trace: $stackTrace');
      _showError('Connection error: $e');
    } finally {
      setState(() => _isProcessing = false);
    }
  }

  void _handlePaymentSuccess(PaymentSuccessResponse response) async {
    debugPrint('✅ [PAYMENT] Payment Success!');
    debugPrint('🔵 [PAYMENT] Payment ID: ${response.paymentId}');
    debugPrint('🔵 [PAYMENT] Order ID: ${response.orderId}');
    debugPrint('🔵 [PAYMENT] Signature: ${response.signature}');
    
    setState(() => _isProcessing = true);

    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('user_id') ?? prefs.getInt('userId');

      // Use stored subscription ID since PaymentSuccessResponse doesn't have it
      final subscriptionId = _currentSubscriptionId;
      
      debugPrint('🔵 [PAYMENT] User ID: $userId');
      debugPrint('🔵 [PAYMENT] Stored Subscription ID: $subscriptionId');
      
      if (subscriptionId == null) {
        debugPrint('❌ [PAYMENT] Subscription ID is null!');
        _showError('Subscription ID not found. Please try again.');
        return;
      }

      final url = '${ApiService.baseUrl}/subscriptions/verify.php';
      debugPrint('🔵 [PAYMENT] Verify URL: $url');

      final verifyResponse = await http.post(
        Uri.parse(url),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'user_id': userId,
          'razorpay_payment_id': response.paymentId,
          'razorpay_subscription_id': subscriptionId,
          'razorpay_signature': response.signature,
        }),
      ).timeout(const Duration(seconds: 30));

      debugPrint('🔵 [PAYMENT] Verify response: ${verifyResponse.body}');

      final data = json.decode(verifyResponse.body);

      if (data['success'] == true) {
        final apiIsPremium = data['data'] != null && data['data']['is_premium'] == true;

        if (apiIsPremium) {
          debugPrint('✅ [PAYMENT] Verification successful! Premium granted.');
          await prefs.setBool('is_premium', true);
          
          // ✅ Razorpay subscription authorization (AutoPay mandate) succeeded.
          // Log Purchase event when 7-day trial is activated for ad attribution.
          if (userId != null) {
            await MetaAppEventsService.setUserId(userId.toString());
            await GoogleAnalyticsService.setUserId(userId.toString());
          }
          // Log Purchase event for trial activation (₹299 value for attribution)
          // Facebook
          await MetaAppEventsService.logPurchase(
            amount: 299.0,
            currency: 'INR',
            contentId: 'premium_monthly_trial',
            contentType: 'subscription',
            orderId: 'trial_${subscriptionId}_${response.paymentId}',
          );
          // Google Analytics
          await GoogleAnalyticsService.logPurchase(
            amount: 299.0,
            currency: 'INR',
            transactionId: 'trial_${subscriptionId}_${response.paymentId}',
            itemName: 'Premium Monthly Trial',
          );
          
          _showSuccess('🎉 Welcome!\nYour 7-day free trial is active.');
        } else {
          // This happens when Razorpay payment is not yet confirmed (UPI pending / user closed)
          debugPrint('ℹ️ [PAYMENT] Payment not confirmed yet. Not granting premium.');
          await prefs.setBool('is_premium', false);
          _showError(data['message'] ?? 'Payment not confirmed yet. Please complete payment.');
        }

        _loadSubscriptionStatus();
      } else {
        debugPrint('❌ [PAYMENT] Verification failed: ${data['message']}');
        _showError(data['message'] ?? 'Payment verification failed');
      }
    } catch (e, stackTrace) {
      debugPrint('❌ [PAYMENT] Exception: $e');
      debugPrint('❌ [PAYMENT] Stack: $stackTrace');
      _showError('Error verifying payment. Please contact support.');
    } finally {
      setState(() => _isProcessing = false);
      _currentSubscriptionId = null; // Clear after use
    }
  }

  void _handlePaymentError(PaymentFailureResponse response) {
    debugPrint('❌ [PAYMENT] Payment Error!');
    debugPrint('❌ [PAYMENT] Code: ${response.code}');
    debugPrint('❌ [PAYMENT] Message: ${response.message}');
    
    setState(() => _isProcessing = false);
    String message = response.message ?? 'Payment failed';
    if (response.code == 2) {
      message = 'Payment cancelled by user';
    }
    _showError(message);
  }

  void _handleExternalWallet(ExternalWalletResponse response) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('External wallet: ${response.walletName}')),
    );
  }

  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red.shade600,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  void _showSuccess(String message) {
    if (!mounted) return;
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.check_circle, color: Colors.green, size: 60),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: GoogleFonts.poppins(fontSize: 16),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF667eea), Color(0xFF764ba2)],
          ),
        ),
        child: SafeArea(
          child: _isLoading
              ? const Center(child: CircularProgressIndicator(color: Colors.white))
              : _error != null
                  ? _buildErrorView()
                  : _isPremium
                      ? _buildPremiumView()
                      : _buildSubscribeView(),
        ),
      ),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 60, color: Colors.white70),
            const SizedBox(height: 16),
            Text(
              _error!,
              style: GoogleFonts.poppins(color: Colors.white70, fontSize: 16),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _loadSubscriptionStatus,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.white,
                foregroundColor: const Color(0xFF667eea),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPremiumView() {
    final trialDaysLeft = _subscription?['trial_days_left'];
    final daysLeft = _subscription?['days_left'];
    // Backend might temporarily send is_trial=false due to webhook timing,
    // so also treat as trial if trial_days_left is present and > 0.
    final bool isTrial = (_subscription?['is_trial'] == true) ||
        ((trialDaysLeft is num && trialDaysLeft > 0) ||
            (trialDaysLeft is String && int.tryParse(trialDaysLeft) != null && int.parse(trialDaysLeft) > 0));

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          // Header
          Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back, color: Colors.white),
                onPressed: () => Navigator.pop(context),
              ),
              const Spacer(),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.green,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  'PREMIUM',
                  style: GoogleFonts.poppins(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
              ),
            ],
          ),

          const SizedBox(height: 30),

          // Premium Badge
          Container(
            padding: const EdgeInsets.all(30),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.15),
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: Colors.amber.withOpacity(0.5), width: 2),
            ),
            child: Column(
              children: [
                const Icon(Icons.workspace_premium, size: 80, color: Colors.amber),
                const SizedBox(height: 16),
                Text(
                  '👑 Premium Member',
                  style: GoogleFonts.poppins(
                    fontSize: 26,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 12),
                if (trialDaysLeft != null && isTrial)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                    decoration: BoxDecoration(
                      color: Colors.orange,
                      borderRadius: BorderRadius.circular(25),
                    ),
                    child: Text(
                      '🆓 Free Trial: $trialDaysLeft days left',
                      style: GoogleFonts.poppins(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                      ),
                    ),
                  )
                else if (daysLeft != null)
                  Text(
                    '$daysLeft days remaining',
                    style: GoogleFonts.poppins(
                      color: Colors.white.withOpacity(0.9),
                      fontSize: 16,
                    ),
                  ),
              ],
            ),
          ),

          const SizedBox(height: 30),

          // Features Unlocked
          _buildFeatureCard('📚', 'All Premium Questions', 'Access 10,000+ questions'),
          _buildFeatureCard('📊', 'Detailed Analytics', 'Track your progress'),
          _buildFeatureCard('🎯', 'Topic-wise Tests', 'Practice by topic'),
          _buildFeatureCard('📱', 'Ad-free Experience', 'Focus on learning'),

          const SizedBox(height: 30),

          // Subscription Details
          if (_subscription != null) ...[
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.1),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Subscription Details',
                    style: GoogleFonts.poppins(
                      color: Colors.white,
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 16),
                  _buildDetailRow('Plan', '₹${_subscription!['amount']?.toInt() ?? 299}/month'),
                  _buildDetailRow('Status', (_subscription!['status'] ?? 'active').toString().toUpperCase()),
                  if (_subscription!['next_billing_date'] != null)
                    _buildDetailRow('Next Billing', _formatDate(_subscription!['next_billing_date'])),
                  _buildDetailRow('Auto-Renew', _subscription!['auto_renew'] == true ? 'Yes' : 'No'),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildSubscribeView() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          // Header
          Align(
            alignment: Alignment.topLeft,
            child: IconButton(
              icon: const Icon(Icons.arrow_back, color: Colors.white),
              onPressed: () => Navigator.pop(context),
            ),
          ),

          const SizedBox(height: 10),

          // Title
          const Icon(Icons.workspace_premium, size: 70, color: Colors.amber),
          const SizedBox(height: 16),
          Text(
            'Go Premium',
            style: GoogleFonts.poppins(
              fontSize: 32,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Unlock all features and ace your TNPSC exam!',
            style: GoogleFonts.poppins(
              fontSize: 15,
              color: Colors.white.withOpacity(0.9),
            ),
            textAlign: TextAlign.center,
          ),

          const SizedBox(height: 30),

          // Price Card
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.2),
                  blurRadius: 20,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Column(
              children: [
                // Trial Badge
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFF11998e), Color(0xFF38ef7d)],
                    ),
                    borderRadius: BorderRadius.circular(25),
                  ),
                  child: Text(
                    '🆓 7-DAY FREE TRIAL',
                    style: GoogleFonts.poppins(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                    ),
                  ),
                ),

                const SizedBox(height: 24),

                // Price
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('₹', style: GoogleFonts.poppins(fontSize: 24, fontWeight: FontWeight.bold, color: const Color(0xFF667eea))),
                    Text('299', style: GoogleFonts.poppins(fontSize: 56, fontWeight: FontWeight.bold, color: const Color(0xFF667eea))),
                    Text('/month', style: GoogleFonts.poppins(fontSize: 16, color: Colors.grey)),
                  ],
                ),

                const SizedBox(height: 8),

                Text(
                  '7-day free trial · ₹299/month after trial',
                  style: GoogleFonts.poppins(
                    color: Colors.grey.shade700,
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                  ),
                  textAlign: TextAlign.center,
                ),

                const SizedBox(height: 24),

                // Features List
                _buildFeatureRow('✅', '10,000+ Premium Questions'),
                _buildFeatureRow('✅', 'Detailed Performance Analytics'),
                _buildFeatureRow('✅', 'Topic-wise Practice Tests'),
                _buildFeatureRow('✅', 'Previous Year Questions'),
                _buildFeatureRow('✅', 'English & Tamil Support'),
                _buildFeatureRow('✅', 'Ad-free Experience'),

                const SizedBox(height: 24),

                // Subscribe Button
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: _isProcessing ? null : _startSubscription,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF6C63FF),
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                      elevation: 3,
                    ),
                    child: _isProcessing
                        ? const SizedBox(
                            height: 24,
                            width: 24,
                            child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                          )
                        : Text(
                            'Start Free Trial',
                            style: GoogleFonts.poppins(
                              fontSize: 17,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                          ),
                  ),
                ),

                const SizedBox(height: 12),

                Text(
                  'Cancel anytime • No hidden fees',
                  style: GoogleFonts.poppins(color: Colors.grey, fontSize: 12),
                ),
              ],
            ),
          ),

          const SizedBox(height: 30),

          // Why Premium
          Text(
            'Why Go Premium?',
            style: GoogleFonts.poppins(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 16),

          _buildFeatureCard('📚', 'Complete Question Bank', 'Access all 10,000+ questions from every topic'),
          _buildFeatureCard('📊', 'Smart Analytics', 'Track weak areas and improve systematically'),
          _buildFeatureCard('🎯', 'Targeted Practice', 'Focus on topics that matter most'),
          _buildFeatureCard('🏆', 'Guaranteed Results', 'Join thousands of successful candidates'),

          const SizedBox(height: 30),
        ],
      ),
    );
  }

  Widget _buildFeatureCard(String emoji, String title, String subtitle) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.1),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Text(emoji, style: const TextStyle(fontSize: 32)),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: GoogleFonts.poppins(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                  ),
                ),
                Text(
                  subtitle,
                  style: GoogleFonts.poppins(
                    color: Colors.white.withOpacity(0.8),
                    fontSize: 13,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFeatureRow(String icon, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Text(icon, style: const TextStyle(fontSize: 16)),
          const SizedBox(width: 12),
          Text(text, style: GoogleFonts.poppins(fontSize: 14)),
        ],
      ),
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: GoogleFonts.poppins(color: Colors.white.withOpacity(0.7))),
          Text(value, style: GoogleFonts.poppins(color: Colors.white, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }

  String _formatDate(String? dateStr) {
    if (dateStr == null) return 'N/A';
    try {
      final date = DateTime.parse(dateStr);
      return '${date.day}/${date.month}/${date.year}';
    } catch (e) {
      return dateStr;
    }
  }
}

