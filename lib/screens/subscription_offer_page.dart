import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:http/http.dart' as http;
import 'package:razorpay_flutter/razorpay_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:video_player/video_player.dart';
import '../services/api_service.dart';
import '../services/meta_app_events_service.dart';
import '../services/google_analytics_service.dart';

/// A visually rich paywall / offer screen that mimics the provided design.
/// Opens Razorpay directly when user taps "Start Trial".
/// 
/// [useVideoHero] - If true, shows an auto-playing video in the hero box (open-app version).
///                  If false (default), shows the logo image (in-app version).
class SubscriptionOfferPage extends StatefulWidget {
  final bool useVideoHero;

  const SubscriptionOfferPage({super.key, this.useVideoHero = false});

  @override
  State<SubscriptionOfferPage> createState() => _SubscriptionOfferPageState();
}

class _SubscriptionOfferPageState extends State<SubscriptionOfferPage> {
  late Razorpay _razorpay;
  bool _isProcessing = false;
  String? _currentSubscriptionId;

  // Video player for hero (only initialized if useVideoHero is true)
  VideoPlayerController? _videoController;
  bool _isVideoInitialized = false;

  @override
  void initState() {
    super.initState();
    _razorpay = Razorpay();
    _razorpay.on(Razorpay.EVENT_PAYMENT_SUCCESS, _handlePaymentSuccess);
    _razorpay.on(Razorpay.EVENT_PAYMENT_ERROR, _handlePaymentError);
    _razorpay.on(Razorpay.EVENT_EXTERNAL_WALLET, _handleExternalWallet);

    // Initialize video player if useVideoHero is true
    if (widget.useVideoHero) {
      _initVideoPlayer();
    }
  }

  Future<void> _initVideoPlayer() async {
    _videoController = VideoPlayerController.asset('assets/videos/premium_square_2.mp4');
    try {
      await _videoController!.initialize();
      _videoController!.setLooping(true);
      _videoController!.setVolume(1.0); // Enable audio
      _videoController!.play();
      if (mounted) {
        setState(() {
          _isVideoInitialized = true;
        });
      }
    } catch (e) {
      debugPrint('Error initializing video player: $e');
    }
  }

  Widget _buildMiniFeature(IconData icon, String text) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, color: const Color(0xFF5E60CE), size: 14),
        const SizedBox(width: 4),
        Text(
          text,
          style: GoogleFonts.poppins(
            fontSize: 11,
            color: Colors.white.withOpacity(0.8),
            fontWeight: FontWeight.w500,
          ),
        ),
      ],
    );
  }

  /// Builds the hero media widget - logo image (for in-app version)
  Widget _buildHeroMedia() {
    return Container(
      width: 80,
      height: 80,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5E60CE).withOpacity(0.4),
            blurRadius: 20,
            spreadRadius: 2,
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.all(10),
        child: Image.asset(
          'assets/images/logo.png',
          fit: BoxFit.contain,
        ),
      ),
    );
  }

  /// Builds the full video banner widget (for open-app version)
  Widget _buildFullVideoBanner() {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        gradient: const LinearGradient(
          colors: [Color(0xFF5E60CE), Color(0xFFFF6B6B)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      padding: const EdgeInsets.all(3),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(15),
        child: Container(
          height: 200,
          width: double.infinity,
          color: Colors.black,
          child: _isVideoInitialized && _videoController != null
              ? FittedBox(
                  fit: BoxFit.cover,
                  child: SizedBox(
                    width: _videoController!.value.size.width,
                    height: _videoController!.value.size.height,
                    child: VideoPlayer(_videoController!),
                  ),
                )
              : const Center(
                  child: CircularProgressIndicator(color: Colors.white),
                ),
        ),
      ),
    );
  }

  /// Builds the original logo-based banner (for in-app version)
  Widget _buildLogoBanner() {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        gradient: const LinearGradient(
          colors: [Color(0xFF5E60CE), Color(0xFFFF6B6B)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      padding: const EdgeInsets.all(3),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(15),
        child: Container(
          height: 180,
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              colors: [Color(0xFF1A1A2E), Color(0xFF16213E)],
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
            ),
          ),
          child: Stack(
            children: [
              // Background pattern circles
              Positioned(
                right: -30,
                top: -30,
                child: Container(
                  width: 120,
                  height: 120,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: const Color(0xFF5E60CE).withOpacity(0.1),
                  ),
                ),
              ),
              Positioned(
                left: -20,
                bottom: -20,
                child: Container(
                  width: 80,
                  height: 80,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: const Color(0xFFFF6B6B).withOpacity(0.1),
                  ),
                ),
              ),
              // Main content
              Padding(
                padding: const EdgeInsets.all(20),
                child: Row(
                  children: [
                    // Left side - Logo
                    _buildHeroMedia(),
                    const SizedBox(width: 16),
                    // Right side - Text content
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          ShaderMask(
                            shaderCallback: (bounds) => const LinearGradient(
                              colors: [Color(0xFF5E60CE), Color(0xFFFF6B6B)],
                            ).createShader(bounds),
                            child: Text(
                              'SUDAR TNPSC',
                              style: GoogleFonts.poppins(
                                fontSize: 18,
                                fontWeight: FontWeight.w800,
                                color: Colors.white,
                                letterSpacing: 1,
                              ),
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Premium Features',
                            style: GoogleFonts.poppins(
                              fontSize: 14,
                              fontWeight: FontWeight.w500,
                              color: Colors.white.withOpacity(0.9),
                            ),
                          ),
                          const SizedBox(height: 8),
                          // Feature highlights
                          Row(
                            children: [
                              _buildMiniFeature(Icons.check_circle, 'All Tests'),
                              const SizedBox(width: 12),
                              _buildMiniFeature(Icons.star, 'Ad-Free'),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              // Premium badge
              Positioned(
                right: 12,
                top: 12,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFFFFD700), Color(0xFFFFA500)],
                    ),
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFFFFD700).withOpacity(0.4),
                        blurRadius: 8,
                        spreadRadius: 1,
                      ),
                    ],
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.workspace_premium, color: Colors.white, size: 14),
                      const SizedBox(width: 4),
                      Text(
                        'PRO',
                        style: GoogleFonts.poppins(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontSize: 11,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _razorpay.clear();
    _videoController?.dispose();
    super.dispose();
  }

  // ---------------------------------------------------------------------------
  // Razorpay Flow (same endpoints as SubscriptionPage)
  // ---------------------------------------------------------------------------

  Future<void> _startSubscription() async {
    setState(() => _isProcessing = true);

    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('user_id') ?? prefs.getInt('userId');

      if (userId == null) {
        _showError('Please login to continue');
        return;
      }

      final url = '${ApiService.baseUrl}/subscriptions/create.php';
      final response = await http
          .post(
            Uri.parse(url),
            headers: {'Content-Type': 'application/json'},
            body: json.encode({'user_id': userId}),
          )
          .timeout(const Duration(seconds: 30));

      final data = json.decode(response.body);

      if (data['success'] == true) {
        final subData = data['data'];
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

        _razorpay.open(options);
      } else {
        _showError(data['message'] ?? 'Failed to create subscription');
      }
    } catch (e) {
      _showError('Connection error. Please try again.');
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  void _handlePaymentSuccess(PaymentSuccessResponse response) async {
    setState(() => _isProcessing = true);

    try {
      final prefs = await SharedPreferences.getInstance();
      final userId = prefs.getInt('user_id') ?? prefs.getInt('userId');
      final subscriptionId = _currentSubscriptionId;

      if (subscriptionId == null) {
        _showError('Subscription ID not found. Please try again.');
        return;
      }

      final url = '${ApiService.baseUrl}/subscriptions/verify.php';
      final verifyResponse = await http
          .post(
            Uri.parse(url),
            headers: {'Content-Type': 'application/json'},
            body: json.encode({
              'user_id': userId,
              'razorpay_payment_id': response.paymentId,
              'razorpay_subscription_id': subscriptionId,
              'razorpay_signature': response.signature,
            }),
          )
          .timeout(const Duration(seconds: 30));

      final data = json.decode(verifyResponse.body);

      if (data['success'] == true) {
        final apiIsPremium =
            data['data'] != null && data['data']['is_premium'] == true;

        if (apiIsPremium) {
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
          _showSuccessAndClose();
        } else {
          await prefs.setBool('is_premium', false);
          _showError(
              data['message'] ?? 'Payment not confirmed yet. Please complete payment.');
        }
      } else {
        _showError(data['message'] ?? 'Payment verification failed');
      }
    } catch (e) {
      _showError('Error verifying payment. Please contact support.');
    } finally {
      if (mounted) setState(() => _isProcessing = false);
      _currentSubscriptionId = null;
    }
  }

  void _handlePaymentError(PaymentFailureResponse response) {
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

  void _showSuccessAndClose() {
    if (!mounted) return;
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.check_circle, color: Colors.green, size: 60),
            const SizedBox(height: 16),
            Text(
              '🎉 Welcome!\nYour 7-day free trial is active.',
              textAlign: TextAlign.center,
              style: GoogleFonts.poppins(fontSize: 16),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(ctx); // close dialog
              Navigator.pop(context, true); // return with result=true
            },
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // UI
  // ---------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Column(
          children: [
            // Scrollable content
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                child: Column(
                  children: [
                    const SizedBox(height: 16),

                    // Professional Premium Banner - Video or Logo based on useVideoHero
                    widget.useVideoHero ? _buildFullVideoBanner() : _buildLogoBanner(),

                    const SizedBox(height: 20),

                    // "UNLOCK PREMIUM" title
                    Text(
                      'UNLOCK PREMIUM',
                      style: GoogleFonts.poppins(
                        color: const Color(0xFFFF6B6B),
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 1.2,
                      ),
                    ),

                    const SizedBox(height: 4),

                    // Divider line
                    Container(
                      width: 180,
                      height: 2,
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [Color(0xFF5E60CE), Color(0xFFFF6B6B)],
                        ),
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),

                    const SizedBox(height: 28),

                    // Headline
                    Text(
                      'Try 7 days for',
                      style: GoogleFonts.poppins(
                        color: const Color(0xFF1F2937),
                        fontSize: 26,
                        fontWeight: FontWeight.w600,
                      ),
                    ),

                    // Big price
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '₹',
                          style: GoogleFonts.poppins(
                            color: const Color(0xFF6B7280),
                            fontSize: 38,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          '5',
                          style: GoogleFonts.poppins(
                            color: const Color(0xFF1F2937),
                            fontSize: 80,
                            fontWeight: FontWeight.w800,
                            height: 1,
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 16),

                    // Then ₹299/month
                    Text(
                      'Then ₹299/month',
                      style: GoogleFonts.poppins(
                        color: const Color(0xFF9CA3AF),
                        fontSize: 11,
                        fontWeight: FontWeight.w400,
                      ),
                    ),

                    const SizedBox(height: 4),

                    Text(
                      'Cancel Anytime',
                      style: GoogleFonts.poppins(
                        color: const Color(0xFFFF6B6B),
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),

                    const SizedBox(height: 24),

                    // Feature list
                    _buildFeatureRow('10,000+ questions'),
                    _buildFeatureRow('New questions daily'),
                    _buildFeatureRow('No Ads'),

                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ),

            // Bottom fixed section
            Container(
              width: double.infinity,
              padding: const EdgeInsets.fromLTRB(20, 12, 20, 20),
              decoration: BoxDecoration(
                color: Colors.white,
                border: Border(
                  top: BorderSide(color: Colors.grey.shade200, width: 1),
                ),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'Try for 7 days and cancel anytime',
                    style: GoogleFonts.poppins(
                      color: Colors.grey,
                      fontSize: 12,
                    ),
                  ),
                  const SizedBox(height: 12),

                  // CTA button
                  GestureDetector(
                    onTap: _isProcessing ? null : _startSubscription,
                    child: Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [Color(0xFFFF6B6B), Color(0xFFFFAB40)],
                        ),
                        borderRadius: BorderRadius.circular(30),
                      ),
                      child: _isProcessing
                          ? const Center(
                              child: SizedBox(
                                height: 22,
                                width: 22,
                                child: CircularProgressIndicator(
                                  color: Colors.white,
                                  strokeWidth: 2.5,
                                ),
                              ),
                            )
                          : Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(
                                  '₹5',
                                  style: GoogleFonts.poppins(
                                    color: Colors.white,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 16,
                                  ),
                                ),
                                const SizedBox(width: 14),
                                Text(
                                  'START TRIAL',
                                  style: GoogleFonts.poppins(
                                    color: Colors.white,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 16,
                                    letterSpacing: 0.5,
                                  ),
                                ),
                                const SizedBox(width: 8),
                                const Icon(Icons.arrow_forward,
                                    color: Colors.white, size: 20),
                              ],
                            ),
                    ),
                  ),

                  const SizedBox(height: 14),

                  // Skip link
                  GestureDetector(
                    onTap: () => Navigator.pop(context, false),
                    child: Text(
                      'Skip and explore Home',
                      style: GoogleFonts.poppins(
                        color: const Color(0xFF6B7280),
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFeatureRow(String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.check_circle, color: Color(0xFF00C853), size: 20),
          const SizedBox(width: 10),
          Text(
            text,
            style: GoogleFonts.poppins(
              color: const Color(0xFF374151),
              fontSize: 15,
            ),
          ),
        ],
      ),
    );
  }
}
