import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ThemeService extends ChangeNotifier {
  static const String _themeKey = 'theme_mode';
  
  // TODO: Dark mode feature - implement later
  // For now, force light mode only
  ThemeMode _themeMode = ThemeMode.light;

  ThemeMode get themeMode => _themeMode;
  bool get isDarkMode => _themeMode == ThemeMode.dark;

  ThemeService() {
    // _loadTheme(); // Disabled - using system theme for now
  }

  // TODO: Implement theme switching later
  // Future<void> _loadTheme() async {
  //   final prefs = await SharedPreferences.getInstance();
  //   final themeModeString = prefs.getString(_themeKey) ?? 'light';
  //   _themeMode = _getThemeModeFromString(themeModeString);
  //   notifyListeners();
  // }

  // Future<void> setThemeMode(ThemeMode mode) async {
  //   _themeMode = mode;
  //   notifyListeners();
  //   
  //   final prefs = await SharedPreferences.getInstance();
  //   await prefs.setString(_themeKey, _getStringFromThemeMode(mode));
  // }

  // Future<void> toggleTheme() async {
  //   if (_themeMode == ThemeMode.light) {
  //     await setThemeMode(ThemeMode.dark);
  //   } else {
  //     await setThemeMode(ThemeMode.light);
  //   }
  // }

  // ThemeMode _getThemeModeFromString(String mode) {
  //   switch (mode) {
  //     case 'dark':
  //       return ThemeMode.dark;
  //     case 'system':
  //       return ThemeMode.system;
  //     default:
  //       return ThemeMode.light;
  //   }
  // }

  // String _getStringFromThemeMode(ThemeMode mode) {
  //   switch (mode) {
  //     case ThemeMode.dark:
  //       return 'dark';
  //     case ThemeMode.system:
  //       return 'system';
  //     default:
  //       return 'light';
  //   }
  // }
}
