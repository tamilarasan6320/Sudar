# Dark Mode Implementation Guide

## ✅ Completed
- **Main App** - Full light/dark theme setup
- **Theme Service** - Theme management with persistence
- **App Colors** - Complete dark theme color palette
- **Theme Helper** - Utility class for easy theme-aware colors
- **Profile Page** - Fully dark mode compatible

## 🔧 To Apply Dark Mode to All Screens

### Pattern to Follow:

```dart
// 1. Import theme helper
import '../utils/theme_helper.dart';

// 2. In build method, use ThemeHelper methods:
return Scaffold(
  backgroundColor: ThemeHelper.backgroundColor(context),
  appBar: AppBar(
    backgroundColor: ThemeHelper.cardColor(context),
    // ... other properties
  ),
  body: Container(
    color: ThemeHelper.cardColor(context),  // for cards
    decoration: BoxDecoration(
      boxShadow: [ThemeHelper.cardShadow(context)],  // for shadows
    ),
    child: Text(
      'Sample',
      style: TextStyle(
        color: ThemeHelper.textPrimary(context),  // for text
      ),
    ),
  ),
);
```

### Replace These Hardcoded Colors:

| Hardcoded | Replace With |
|-----------|-------------|
| `Colors.white` (background) | `ThemeHelper.cardColor(context)` |
| `AppColors.background` | `ThemeHelper.backgroundColor(context)` |
| `AppColors.textPrimary` | `ThemeHelper.textPrimary(context)` |
| `AppColors.textSecondary` | `ThemeHelper.textSecondary(context)` |
| `AppColors.textLight` | `ThemeHelper.textLight(context)` |
| `AppColors.border` | `ThemeHelper.borderColor(context)` |
| `Colors.black.withOpacity(0.05)` | `ThemeHelper.cardShadow(context)` |

### Screens to Update:

- [ ] Performance Page (`lib/screens/performance_page.dart`)
- [ ] Saved Tests Page (`lib/screens/saved_tests_page.dart`)
- [ ] Test History Page (`lib/screens/test_history_page.dart`)
- [ ] Home Page (`lib/screens/home_page.dart`)
- [ ] Tests Page (`lib/screens/tests_page.dart`)
- [ ] Progress Page (`lib/screens/progress_page.dart`)
- [ ] Exam Selection Page (`lib/screens/exam_selection_page.dart`)
- [ ] Login Page (`lib/screens/login_page.dart`)
- [ ] OTP Page (`lib/screens/otp_verification_page.dart`)
- [ ] Profile Setup Page (`lib/screens/profile_setup_page.dart`)
- [ ] Test Page (`lib/screens/test_page.dart`)
- [ ] Test Results Page (`lib/screens/test_results_page.dart`)
- [ ] Language Selection Page (`lib/screens/language_selection_page.dart`)

## Quick Implementation

Run this command to see all hardcoded color usages:
```bash
grep -r "Colors.white" lib/screens/
grep -r "AppColors.textPrimary" lib/screens/
grep -r "AppColors.background" lib/screens/
```

Then replace them with ThemeHelper methods as shown in the pattern above.

