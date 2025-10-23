# 🔍 DATA SOURCE ANALYSIS - Mock Test App

## ❌ CRITICAL FINDING: ALL DATA IS HARDCODED!

**Date:** October 23, 2025  
**Status:** ⚠️ **NO DATABASE INTEGRATION IN UI**

---

## 📊 SUMMARY

| Page | Data Source | Status |
|------|-------------|--------|
| **Home Page** | Hardcoded | ❌ NOT FROM DB |
| **Tests Page** | Hardcoded | ❌ NOT FROM DB |
| **Test Page** | Hardcoded | ❌ NOT FROM DB |
| **Progress Page** | Hardcoded | ❌ NOT FROM DB |
| **Profile Page** | SharedPreferences | ❌ NOT FROM DB |

---

## 🏠 HOME PAGE

### What You See:
- **Tests Taken:** 24
- **Avg Score:** 78%
- **Rank:** #142
- **Test Categories:** 
  - Previous Year Papers (50 tests, 32/50 completed)
  - Tamil Language Section (45 tests, 18/45 completed)
  - General Science (60 tests, 12/60 completed)
- **Recent Tests:** 3 sample tests with hardcoded scores

### Reality:
```dart
// File: lib/screens/home_page.dart, lines 260-290
Widget _buildStatsSection() {
  return Row(
    children: [
      _buildStatCard(
        label: 'Tests Taken',
        value: '24',  // ❌ HARDCODED
      ),
      _buildStatCard(
        label: 'Avg Score',
        value: '78%',  // ❌ HARDCODED
      ),
      _buildStatCard(
        label: 'Rank',
        value: '#142',  // ❌ HARDCODED
      ),
    ],
  );
}
```

### What Should Happen:
```dart
// ✅ SHOULD CALL API:
- ApiService.getTestHistory(userId) → Get actual tests taken
- ApiService.getUserStats(userId) → Get real average score
- ApiService.getRankings() → Get actual user rank
```

---

## 📝 TESTS PAGE

### What You See:
- TNPSC Group 1 - Full Mock Test (200 questions)
- Tamil Grammar - Practice Test (50 questions)
- General Science - Mock Test (75 questions)
- Indian History - Quick Test (30 questions)
- 8 hardcoded test cards

### Reality:
```dart
// File: lib/screens/tests_page.dart, lines 85-161
Widget _buildTestList(String category) {
  return ListView(
    children: [
      _buildTestCard(
        title: 'TNPSC Group 1 - Full Mock Test',  // ❌ HARDCODED
        questions: 200,  // ❌ HARDCODED
        duration: '180 min',  // ❌ HARDCODED
      ),
      // ... more hardcoded tests
    ],
  );
}
```

### What Should Happen:
```dart
// ✅ SHOULD CALL API:
- ApiService.getQuestionSessions(examCategory) → Get real test list
- Display actual tests from database
```

---

## 📖 TEST PAGE (Questions)

### What You See:
- 5 sample questions:
  1. Capital of Tamil Nadu?
  2. Who wrote Silappatikaram?
  3. Chemical formula of water?
  4. India independence year?
  5. 15% of 200?

### Reality:
```dart
// File: lib/screens/test_page.dart, lines 27-58
final List<Question> _questions = [
  Question(
    id: 1,
    text: 'Which of the following is the capital of Tamil Nadu?',  // ❌ HARDCODED
    options: ['Chennai', 'Coimbatore', 'Madurai', 'Salem'],
    correctAnswer: 0,
  ),
  // ... only 5 hardcoded questions!
];
```

### What Should Happen:
```dart
// ✅ SHOULD CALL API:
- ApiService.getQuestions(sessionId) → Get real questions from database
- Load 50-200 questions based on test type
```

---

## 📈 PROGRESS PAGE

### What You See:
- Tests Taken: 24
- Avg Score: 78%
- Total Tests: 120
- Subject-wise performance with progress bars
- Recent activity with hardcoded data

### Reality:
```dart
// File: lib/screens/progress_page.dart, lines 50-68
child: _buildStatItem(context,
  label: 'Tests Taken',
  value: '24',  // ❌ HARDCODED
),
child: _buildStatItem(context,
  label: 'Avg Score',
  value: '78%',  // ❌ HARDCODED
),
```

### What Should Happen:
```dart
// ✅ SHOULD CALL API:
- ApiService.getTestHistory(userId) → Get actual test history
- ApiService.getUserStats(userId) → Get real statistics
- Calculate actual subject-wise performance
```

---

## 👤 PROFILE PAGE

### What You See:
- User Name: "adsdsds" or "prasad"
- Mobile Number: 8738474634

### Reality:
```dart
// File: lib/screens/profile_page.dart, lines 30-35
Future<void> _loadUserData() async {
  final prefs = await SharedPreferences.getInstance();
  setState(() {
    _userName = prefs.getString('userName') ?? 'User';  // ❌ FROM LOCAL STORAGE
    _userMobile = prefs.getString('userMobile') ?? '';  // ❌ FROM LOCAL STORAGE
  });
}
```

### What Should Happen:
```dart
// ✅ SHOULD CALL API:
- ApiService.getUserProfile(mobile) → Get fresh data from database
- Show email, age, district, education from DB
- Update profile via API
```

---

## ✅ WHAT'S ACTUALLY WORKING (Backend)

### APIs That Exist and Work:
1. ✅ **send_otp.php** - Working
2. ✅ **verify_otp.php** - Working with test OTP (123456)
3. ✅ **create.php** - User creation working
4. ✅ **update.php** - User update working
5. ✅ **profile.php** - Get user profile working
6. ✅ **question_sessions.php** - Get test sessions working
7. ✅ **questions.php** - Get questions working
8. ✅ **submit_result.php** - Submit test results working
9. ✅ **test_history.php** - Get test history working
10. ✅ **rankings.php** - Get rankings working

### API Service Methods Available:
```dart
// File: lib/services/api_service.dart
class ApiService {
  ✅ static Future<Map<String, dynamic>> sendOTP(String mobile)
  ✅ static Future<Map<String, dynamic>> verifyOTP(String mobile, String otp)
  ✅ static Future<Map<String, dynamic>> createUser({...})
  ✅ static Future<Map<String, dynamic>> updateUser({...})
  ✅ static Future<Map<String, dynamic>> getUserProfile(String mobile)
  ✅ static Future<Map<String, dynamic>> getTestCategories()
  ✅ static Future<Map<String, dynamic>> getQuestionSessions(String examCategory)
  ✅ static Future<Map<String, dynamic>> getQuestions(int sessionId)
  ✅ static Future<Map<String, dynamic>> submitTestResult({...})
  ✅ static Future<Map<String, dynamic>> getTestHistory(int userId)
  ✅ static Future<Map<String, dynamic>> getRankings(String examCategory)
}
```

**ALL THESE APIs EXIST BUT ARE NOT BEING USED IN THE UI!**

---

## 🚨 WHAT NEEDS TO BE FIXED

### Priority 1: HOME PAGE
- [ ] Replace hardcoded stats with `getTestHistory()` and `getRankings()`
- [ ] Load test categories from `getTestCategories()`
- [ ] Load recent tests from `getTestHistory()`

### Priority 2: TESTS PAGE
- [ ] Replace hardcoded test list with `getQuestionSessions()`
- [ ] Load real test data from database

### Priority 3: TEST PAGE (Questions)
- [ ] Replace 5 hardcoded questions with `getQuestions(sessionId)`
- [ ] Load 50-200 real questions based on selected test
- [ ] Submit results to database using `submitTestResult()`

### Priority 4: PROGRESS PAGE
- [ ] Load stats from `getTestHistory()` and `getUserStats()`
- [ ] Calculate real subject-wise performance
- [ ] Show actual activity history

### Priority 5: PROFILE PAGE
- [ ] Load profile from `getUserProfile()` instead of SharedPreferences
- [ ] Update profile using `updateUser()`
- [ ] Show all user data (email, age, district, education)

---

## 🎯 CONCLUSION

**Your backend is 100% ready!**  
**Your frontend is showing 100% dummy data!**

The disconnect is clear:
- ✅ **Backend:** All APIs work perfectly
- ❌ **Frontend:** Not calling any APIs except login/OTP

**Next Step:** Integrate all API calls into the Flutter UI to show real database data.

---

## 📋 RECOMMENDED ACTION PLAN

1. **Start with Tests Page** - Most critical for functionality
2. **Fix Test Page Questions** - Enable real test taking
3. **Update Home Page Stats** - Show real progress
4. **Fix Progress Page** - Show accurate history
5. **Enhance Profile Page** - Load from DB, not local storage

**Estimated Effort:** 4-6 hours to integrate all APIs into UI

---

**Report Generated:** October 23, 2025  
**Flutter App:** Running on localhost:8888  
**Backend APIs:** Running on localhost/Mock_test/api  
**Database:** Connected and working

