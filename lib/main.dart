import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'screens/app_startup_router.dart';
import 'utils/app_colors.dart';
import 'services/theme_service.dart';
import 'services/firebase_service.dart';
import 'services/onesignal_service.dart';
import 'services/app_navigator.dart';
import 'services/meta_app_events_service.dart';
import 'services/google_analytics_service.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Run the app immediately
  runApp(
    ChangeNotifierProvider(
      create: (_) => ThemeService(),
      child: const TNPSCMockTestApp(),
    ),
  );
  
  // Initialize Firebase and OneSignal after first frame to avoid blocking UI
  WidgetsBinding.instance.addPostFrameCallback((_) {
    _initializeServices();
  });
}

/// Initialize services after the first frame (non-blocking)
Future<void> _initializeServices() async {
  debugPrint('╔════════════════════════════════════════════════════════════╗');
  debugPrint('║           INITIALIZING ALL SERVICES                        ║');
  debugPrint('╚════════════════════════════════════════════════════════════╝');
  
  // Initialize Firebase first (required for OneSignal and Analytics)
  await FirebaseService.initialize();
  
  // Initialize OneSignal for push notifications
  await OneSignalService.initialize();
  
  // Initialize Meta (Facebook) SDK for app events tracking
  // This enables automatic app install and app open tracking
  await MetaAppEventsService.initialize();
  
  // Test install event (for internal testing)
  await MetaAppEventsService.testInstallEvent();
  
  // Initialize Google Analytics for event tracking
  await GoogleAnalyticsService.initialize();
  
  debugPrint('╔════════════════════════════════════════════════════════════╗');
  debugPrint('║           ALL SERVICES INITIALIZED                         ║');
  debugPrint('╚════════════════════════════════════════════════════════════╝');
}

class TNPSCMockTestApp extends StatelessWidget {
  const TNPSCMockTestApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final themeService = Provider.of<ThemeService>(context);
    
    return MaterialApp(
      title: 'SUDAR - TNPSC Mock Test',
      debugShowCheckedModeBanner: false,
      navigatorKey: appNavigatorKey,
      themeMode: themeService.themeMode,
      // Prevent black screen during page transitions
      builder: (context, child) {
        return Container(
          color: Theme.of(context).scaffoldBackgroundColor,
          child: child,
        );
      },
      
      // Light Theme
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.light,
        colorScheme: ColorScheme.fromSeed(
          seedColor: AppColors.primary,
          primary: AppColors.primary,
          secondary: AppColors.secondary,
          brightness: Brightness.light,
        ),
        textTheme: GoogleFonts.poppinsTextTheme(),
        scaffoldBackgroundColor: AppColors.background,
        cardColor: AppColors.cardBackground,
        appBarTheme: const AppBarTheme(
          elevation: 0,
          centerTitle: true,
          backgroundColor: Colors.white,
          foregroundColor: AppColors.textPrimary,
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            elevation: 0,
            padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: Colors.white,
          contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppColors.border),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppColors.border),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppColors.primary, width: 2),
          ),
        ),
      ),
      
      // Dark Theme
      darkTheme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        colorScheme: ColorScheme.fromSeed(
          seedColor: AppColors.primary,
          primary: AppColors.primary,
          secondary: AppColors.secondary,
          brightness: Brightness.dark,
          surface: AppColors.darkCardBackground,
        ),
        textTheme: GoogleFonts.poppinsTextTheme(
          ThemeData.dark().textTheme,
        ),
        scaffoldBackgroundColor: AppColors.darkBackground,
        cardColor: AppColors.darkCardBackground,
        appBarTheme: const AppBarTheme(
          elevation: 0,
          centerTitle: true,
          backgroundColor: AppColors.darkCardBackground,
          foregroundColor: AppColors.darkTextPrimary,
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            elevation: 0,
            padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: AppColors.darkSurface,
          contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppColors.darkBorder),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppColors.darkBorder),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppColors.primary, width: 2),
          ),
        ),
      ),
      
      home: const AppStartupRouter(),
    );
  }
}
