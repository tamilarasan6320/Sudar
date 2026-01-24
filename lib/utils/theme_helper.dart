import 'package:flutter/material.dart';
import 'app_colors.dart';

class ThemeHelper {
  static bool isDark(BuildContext context) {
    return Theme.of(context).brightness == Brightness.dark;
  }

  static Color backgroundColor(BuildContext context) {
    return isDark(context) ? AppColors.darkBackground : AppColors.background;
  }

  static Color cardColor(BuildContext context) {
    return isDark(context) ? AppColors.darkCardBackground : Colors.white;
  }

  static Color surfaceColor(BuildContext context) {
    return isDark(context) ? AppColors.darkSurface : AppColors.surfaceLight;
  }

  static Color textPrimary(BuildContext context) {
    return isDark(context) ? AppColors.darkTextPrimary : AppColors.textPrimary;
  }

  static Color textSecondary(BuildContext context) {
    return isDark(context) ? AppColors.darkTextSecondary : AppColors.textSecondary;
  }

  static Color textLight(BuildContext context) {
    return isDark(context) ? AppColors.darkTextLight : AppColors.textLight;
  }

  static Color borderColor(BuildContext context) {
    return isDark(context) ? AppColors.darkBorder : AppColors.border;
  }

  static double shadowOpacity(BuildContext context) {
    return isDark(context) ? 0.3 : 0.05;
  }

  static List<BoxShadow> cardShadow(BuildContext context) {
    return [
      BoxShadow(
        color: Colors.black.withOpacity(shadowOpacity(context)),
        blurRadius: 10,
        offset: const Offset(0, 4),
      ),
    ];
  }
}

