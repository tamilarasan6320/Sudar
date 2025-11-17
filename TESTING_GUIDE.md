# Complete Testing Guide - Full App Test Flow

## ⚠️ Important: Follow These Steps EXACTLY

### Prerequisites
1. ✅ XAMPP Apache & MySQL running
2. ✅ Database created (`mocktest`)
3. ✅ Questions uploaded (use `demo_questions.csv`)

---

## Step-by-Step Testing

### STEP 1: Upload Questions (Admin Panel)

```
1. Open browser: http://localhost/MockTest/admin/#addQuestion
2. You should see "Mock test 1" session card
3. Click "Upload" button on the card
4. Select file: admin/demo_questions.csv
5. Click "Upload Questions"
6. Should show: "✅ Successfully uploaded 10 questions!"
7. Card should now show: "10 uploaded" instead of "0 (empty)"
```

**Verify in database:**
```sql
SELECT COUNT(*) FROM questions WHERE session_id = 1;
-- Should return: 10
```

---

### STEP 2: Run Flutter App

**Option A - Web (Easiest):**
```bash
cd C:\xampp\htdocs\MockTest
flutter run -d chrome
```

**Option B - Android Emulator:**
```bash
cd C:\xampp\htdocs\MockTest
flutter run
```

---

### STEP 3: Login to App

```
1. App opens to Splash Screen
2. Tap anywhere or wait 2 seconds
3. Login screen appears
4. Enter mobile: 1234567890
5. Tap "Send OTP"
6. OTP auto-fills with: 111111
7. Auto-verifies and goes to Exam Selection
8. Select "TNPSC Group 4"
9. Tap "Continue"
10. Home page loads
```

**Check console logs:**
```
✅ OTP sent successfully! (Test OTP: 111111)
🔍 Loading test history for user: 1
📥 Test history response: {success: true, count: 0, history: []}
✅ Loaded 0 test results
```

---

### STEP 4: Take a Test

```
1. On Home page, tap "Tests" tab (bottom navigation)
2. You should see "Mock test 1" (Tamil - 60 minutes - 10 questions)
3. Tap "Start Test" button
4. Test page loads with Question 1
5. Answer a few questions (select any option)
6. Tap "Submit Test" button at top-right
7. Confirm submission
8. Should see: "Submitting your test..." loading dialog
9. Then: "Test submitted successfully!" green message
10. Results page shows your score
```

**Check console logs:**
```
📤 Submitting test:
  User ID: 1
  Session ID: 1
  Answers: 8
  Time taken: 120 seconds
📥 Submit response: {success: true, message: Test submitted successfully, result: {...}}
```

**Verify in database:**
```sql
-- Check test result was saved
SELECT * FROM test_results WHERE user_id = 1 ORDER BY id DESC LIMIT 1;

-- Check answers were saved
SELECT COUNT(*) FROM user_answers WHERE result_id = 
  (SELECT id FROM test_results WHERE user_id = 1 ORDER BY id DESC LIMIT 1);
```

---

### STEP 5: Check Recent Tests

```
1. Tap "Home" tab (bottom navigation)
2. Scroll down to "Recent Tests" section
3. Should now show your test:
   - Test name: "Mock test 1"
   - Score: "X/10" (your score)
   - Status: "Passed" or "Failed"
   - Date: Today's date
```

**Check console logs:**
```
🔍 Loading test history for user: 1
📥 Test history response: {success: true, count: 1, history: [{...}]}
✅ Loaded 1 test results
📊 Stats:
  Tests taken: 1
  Average score: 75.0
  Recent tests: 1
```

**If NOT showing:**
- Pull down to refresh the home page
- OR restart the app
- Check console for error messages

---

### STEP 6: Take Another Test

```
1. Go to Tests tab again
2. Start the same test again
3. Answer different questions
4. Submit
5. Go back to Home → Recent Tests
6. Should now show 2 tests
```

---

### STEP 7: Check Test History

```
1. Tap "Profile" tab
2. Tap "Test History" card
3. Should show all your tests in a list
4. Each shows:
   - Test name
   - Score and percentage
   - Rank
   - Date/time
```

---

## Troubleshooting

### Problem: Recent Tests Empty

**Check 1: Is test result in database?**
```sql
SELECT * FROM test_results WHERE user_id = 1;
```
- If empty: Test submission failed
- If has data: API not fetching correctly

**Check 2: Is API working?**
```
Open browser: http://localhost/MockTest/api/tests/get_history.php?user_id=1
Should return: {"success":true,"count":1,"history":[{...}]}
```

**Check 3: Check Flutter console**
Look for these logs after taking a test:
```
📤 Submitting test: ...
📥 Submit response: ...
```

And when opening home page:
```
🔍 Loading test history for user: 1
📥 Test history response: ...
✅ Loaded X test results
```

### Problem: "Submitting..." Never Finishes

**Possible causes:**
1. XAMPP not running
2. Wrong API URL
3. Session ID wrong
4. No questions in session

**Debug:**
1. Check console for errors
2. Check Network tab in browser DevTools
3. Verify API endpoint: `http://localhost/MockTest/api/tests/submit_result.php`

### Problem: Error Message When Submitting

**Common errors:**
- "User ID, session ID, and answers are required" → Check API call parameters
- "No questions found" → Upload questions first
- "Connection timeout" → Check XAMPP running

---

## Expected API Calls

### When taking a test:
```
1. GET /api/tests/get_questions.php?session_id=1
   → Returns 10 questions
   
2. POST /api/tests/submit_result.php
   Body: {user_id, session_id, started_at, time_taken, answers[]}
   → Saves result, returns score/rank
```

### When opening home page:
```
3. GET /api/tests/get_history.php?user_id=1&limit=50
   → Returns all test results for user
   
4. GET /api/tests/get_rankings.php?limit=100
   → Returns all user rankings
```

---

## Success Criteria

✅ Can upload questions in admin
✅ Questions show in test list
✅ Can start and complete test
✅ "Submitting..." dialog appears
✅ "Test submitted successfully!" message shows
✅ Results page displays score
✅ Home page "Recent Tests" shows the test
✅ "Tests Taken" count increments
✅ Test History page shows all tests

---

## Quick Test Script

```bash
# 1. Make sure XAMPP is running
netstat -an | findstr :80
netstat -an | findstr :3306

# 2. Upload questions
# Open: http://localhost/MockTest/admin/#addQuestion
# Upload demo_questions.csv

# 3. Run app
cd C:\xampp\htdocs\MockTest
flutter run -d chrome

# 4. In app:
#    - Login: 1234567890
#    - OTP: 111111
#    - Select exam: TNPSC Group 4
#    - Go to Tests → Start test
#    - Answer some questions
#    - Submit
#    - Check Home → Recent Tests

# 5. Verify in MySQL:
mysql -u root
USE mocktest;
SELECT * FROM test_results;
SELECT * FROM user_answers;
```

---

## Status After Fix

✅ Test submission saves to database
✅ Recent tests loads from API
✅ Console logs show all API calls
✅ Error handling with clear messages
✅ Loading states during submission
✅ Success/error notifications

**Everything is now properly connected!**

