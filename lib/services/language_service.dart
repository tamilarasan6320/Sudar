import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LanguageService extends ChangeNotifier {
  static const String _languageKey = 'selected_language';
  
  Locale _currentLocale = const Locale('en', 'US');
  
  Locale get currentLocale => _currentLocale;
  
  String get currentLanguageCode => _currentLocale.languageCode;
  
  bool get isTamil => _currentLocale.languageCode == 'ta';
  bool get isEnglish => _currentLocale.languageCode == 'en';
  
  Future<void> initializeLanguage() async {
    final prefs = await SharedPreferences.getInstance();
    final savedLanguage = prefs.getString(_languageKey);
    
    if (savedLanguage != null) {
      _currentLocale = Locale(savedLanguage);
      notifyListeners();
    }
  }
  
  Future<void> setLanguage(String languageCode) async {
    _currentLocale = Locale(languageCode);
    notifyListeners();
    
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_languageKey, languageCode);
  }
  
  Future<void> setTamil() async {
    await setLanguage('ta');
  }
  
  Future<void> setEnglish() async {
    await setLanguage('en');
  }
}

// Translation service for Tamil and English
class AppTranslations {
  static const Map<String, Map<String, String>> _translations = {
    'en': {
      // Common
      'continue': 'CONTINUE',
      'select': 'Select',
      'please_select': 'Please select',
      'error': 'Error',
      'success': 'Success',
      
      // Language Selection
      'choose_language': 'Choose Your Language',
      'select_language_description': 'Select your preferred language for the app interface',
      'select_language_to_continue': 'Please select a language to continue',
      
      // Exam Selection
      'choose_exam': 'Choose Your TNPSC Exam',
      'select_exam_description': 'Select the exam you are preparing for to get personalized mock tests',
      'select_exam_to_continue': 'Please select an exam to continue',
      'select_your_exam': 'Select Your Exam',
      
      // Home Page
      'mock_test': 'Mock Test',
      'tests_taken': 'Tests Taken',
      'avg_score': 'Avg Score',
      'rank': 'Rank',
      'quick_actions': 'Quick Actions',
      'start_test': 'Start Test',
      'performance': 'Performance',
      'test_categories': 'Test Categories',
      'view_all': 'View All',
      'recent_tests': 'Recent Tests',
      'home': 'Home',
      'tests': 'Tests',
      'progress': 'Progress',
      'profile': 'Profile',
      
      // Test Categories
      'previous_year_papers': 'Previous Year Papers',
      'tamil_language_section': 'Tamil Language Section',
      'general_science': 'General Science',
      'social_science': 'Social Science',
      'aptitude_mental_ability': 'Aptitude & Mental Ability',
      'current_affairs': 'Current Affairs',
      'sslc_level_tests': '10th / SSLC Level Tests',
      'hsc_level_tests': '12th / HSC Level Tests',
      'tests_available': 'Tests Available',
      
      // Recent Tests
      'completed_on': 'Completed on',
      'score': 'Score',
      'passed': 'Passed',
      'failed': 'Failed',
      
      // Exam Names
      'tnpsc_group_1': 'TNPSC Group 1',
      'tnpsc_group_2': 'TNPSC Group 2',
      'tnpsc_group_2a': 'TNPSC Group 2A',
      'tnpsc_group_4': 'TNPSC Group 4',
      'tnpsc_vao': 'TNPSC VAO',
      'tnpsc_police': 'TNPSC Police',
    },
    'ta': {
      // Common
      'continue': 'தொடரவும்',
      'select': 'தேர்ந்தெடுக்கவும்',
      'please_select': 'தயவுசெய்து தேர்ந்தெடுக்கவும்',
      'error': 'பிழை',
      'success': 'வெற்றி',
      
      // Language Selection
      'choose_language': 'உங்கள் மொழியைத் தேர்ந்தெடுக்கவும்',
      'select_language_description': 'பயன்பாட்டு இடைமுகத்திற்கான உங்கள் விருப்பமான மொழியைத் தேர்ந்தெடுக்கவும்',
      'select_language_to_continue': 'தொடர தயவுசெய்து ஒரு மொழியைத் தேர்ந்தெடுக்கவும்',
      
      // Exam Selection
      'choose_exam': 'உங்கள் TNPSC தேர்வைத் தேர்ந்தெடுக்கவும்',
      'select_exam_description': 'தனிப்பயனாக்கப்பட்ட மோக் டெஸ்ட்களைப் பெற நீங்கள் தயாராகும் தேர்வைத் தேர்ந்தெடுக்கவும்',
      'select_exam_to_continue': 'தொடர தயவுசெய்து ஒரு தேர்வைத் தேர்ந்தெடுக்கவும்',
      'select_your_exam': 'உங்கள் தேர்வைத் தேர்ந்தெடுக்கவும்',
      
      // Home Page
      'mock_test': 'மோக் டெஸ்ட்',
      'tests_taken': 'தேர்வுகள் எடுத்தது',
      'avg_score': 'சராசரி மதிப்பெண்',
      'rank': 'தரவரிசை',
      'quick_actions': 'விரைவு செயல்கள்',
      'start_test': 'தேர்வைத் தொடங்கவும்',
      'performance': 'செயல்திறன்',
      'test_categories': 'தேர்வு பிரிவுகள்',
      'view_all': 'அனைத்தையும் பார்க்கவும்',
      'recent_tests': 'சமீபத்திய தேர்வுகள்',
      'home': 'முகப்பு',
      'tests': 'தேர்வுகள்',
      'progress': 'முன்னேற்றம்',
      'profile': 'சுயவிவரம்',
      
      // Test Categories
      'previous_year_papers': 'முந்தைய ஆண்டு தாள்கள்',
      'tamil_language_section': 'தமிழ் மொழி பிரிவு',
      'general_science': 'பொது அறிவியல்',
      'social_science': 'சமூக அறிவியல்',
      'aptitude_mental_ability': 'பண்பு மற்றும் மன திறன்',
      'current_affairs': 'நடப்பு நிகழ்வுகள்',
      'sslc_level_tests': '10வது / SSLC நிலை தேர்வுகள்',
      'hsc_level_tests': '12வது / HSC நிலை தேர்வுகள்',
      'tests_available': 'தேர்வுகள் கிடைக்கின்றன',
      
      // Recent Tests
      'completed_on': 'முடிக்கப்பட்ட தேதி',
      'score': 'மதிப்பெண்',
      'passed': 'தேர்ச்சி',
      'failed': 'தோல்வி',
      
      // Exam Names
      'tnpsc_group_1': 'TNPSC குழு 1',
      'tnpsc_group_2': 'TNPSC குழு 2',
      'tnpsc_group_2a': 'TNPSC குழு 2A',
      'tnpsc_group_4': 'TNPSC குழு 4',
      'tnpsc_vao': 'TNPSC VAO',
      'tnpsc_police': 'TNPSC காவல்துறை',
    },
  };
  
  static String translate(String key, String languageCode) {
    return _translations[languageCode]?[key] ?? _translations['en']?[key] ?? key;
  }
  
  static String getTranslation(BuildContext context, String key) {
    final locale = Localizations.localeOf(context);
    return translate(key, locale.languageCode);
  }
}
