# ✅ Test History & Recent Tests - API Connected

## All Features Now Connected to Database

### 1. **Test Submission** - FIXED ✅
**Location:** `lib/screens/test_page.dart` (Line 166-303)

**What happens when user submits test:**
1. Shows loading dialog
2. Collects all answers
3. Calculates time taken
4. Sends to API: `POST /api/tests/submit_result.php`
5. API calculates:
   - Score (marks - negative marks)
   - Percentage
   - Rank among all users
6. Saves to database:
   - `test_results` table
   - `user_answers` table (individual answers)
7. Shows success/error message
8. Navigates to results page

**API Request:**
```json
{
  "user_id": 1,
  "session_id": 1,
  "time_taken": 1200,
  "answers": [
    {
      "question_id": 1,
      "answer": "A",
      "time_spent": 30,
      "marked_for_review": false
    }
  ]
}
```

**API Response:**
```json
{
  "success": true,
  "message": "Test submitted successfully",
  "result": {
    "result_id": 1,
    "total_questions": 10,
    "attempted": 8,
    "correct": 6,
    "wrong": 2,
    "unanswered": 2,
    "score": 5.5,
    "percentage": 75.00,
    "rank": 5,
    "time_taken": 1200
  }
}
```

---

### 2. **Recent Tests** - Already Connected ✅
**Location:** `lib/screens/home_page.dart` (Line 180-195)

**API Call:** `GET /api/tests/get_history.php?user_id={userId}&limit=50`

**What it shows:**
- Last 3 tests taken
- Test name, score, percentage
- Time taken
- Date/time of test

**Data Flow:**
1. Home page loads
2. Calls `ApiService.getTestHistory(userId: userId)`
3. API fetches from `test_results` table
4. Returns recent tests
5. Displays in "Recent Tests" section

---

### 3. **Test History Page** - Already Connected ✅
**Location:** `lib/screens/test_history_page.dart`

**API Call:** Same as Recent Tests

**What it shows:**
- Full list of all tests taken
- Detailed stats for each test
- Filter by category
- Sort by date/score

---

### 4. **User Stats** - Already Connected ✅
**Location:** `lib/screens/home_page.dart`

**Calculated from test_history API:**
- Total Tests Taken: `history.length`
- Average Score: Sum of percentages / count
- User Rank: From rankings API

---

### 5. **Rankings** - Already Connected ✅
**API Call:** `GET /api/tests/get_rankings.php?limit=100`

**What it does:**
- Calculates user ranks based on performance
- Shows top performers
- Updates after each test submission

---

## Database Tables Used

### `test_results`
```sql
CREATE TABLE test_results (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  session_id INT NOT NULL,
  total_questions INT NOT NULL,
  attempted_questions INT NOT NULL,
  correct_answers INT NOT NULL,
  wrong_answers INT NOT NULL,
  unanswered INT NOT NULL,
  score DECIMAL(10,2) NOT NULL,
  percentage DECIMAL(5,2) NOT NULL,
  rank INT DEFAULT 0,
  time_taken INT NOT NULL COMMENT 'in seconds',
  started_at TIMESTAMP NULL,
  submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (session_id) REFERENCES question_sessions(id)
);
```

### `user_answers`
```sql
CREATE TABLE user_answers (
  id INT PRIMARY KEY AUTO_INCREMENT,
  result_id INT NOT NULL,
  question_id INT NOT NULL,
  user_answer VARCHAR(1) NOT NULL COMMENT 'A, B, C, or D',
  is_correct TINYINT(1) DEFAULT 0,
  time_spent INT DEFAULT 0 COMMENT 'in seconds',
  marked_for_review TINYINT(1) DEFAULT 0,
  FOREIGN KEY (result_id) REFERENCES test_results(id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES questions(id)
);
```

---

## How to Test

### 1. Upload Questions (if not done)
```
1. Open admin panel: http://localhost/MockTest/admin/
2. Go to "Add Question"
3. Upload demo_questions.csv (10 questions)
```

### 2. Take a Test
```
1. Open app (browser or mobile)
2. Login with mobile: 1234567890, OTP: 111111
3. Select an exam
4. Go to "Tests" tab
5. Start a test that has questions
6. Answer some questions
7. Submit test
8. Should show:
   - Loading dialog "Submitting your test..."
   - Success message
   - Results page with score
```

### 3. Check Recent Tests
```
1. Go back to Home page
2. Scroll down to "Recent Tests"
3. Should see the test you just took with:
   - Test name
   - Score and percentage
   - Date/time
```

### 4. Check Test History
```
1. Go to Profile tab
2. Tap "Test History"
3. Should see all tests you've taken
4. Can filter by category
```

### 5. Verify in Database
```sql
-- Check test results
SELECT * FROM test_results WHERE user_id = 1 ORDER BY submitted_at DESC;

-- Check answers
SELECT * FROM user_answers WHERE result_id = 1;

-- Check rankings
SELECT 
  u.name,
  tr.score,
  tr.percentage,
  tr.rank,
  tr.submitted_at
FROM test_results tr
JOIN users u ON tr.user_id = u.id
ORDER BY tr.score DESC;
```

---

## Features Summary

| Feature | Status | API Endpoint | Location |
|---------|--------|--------------|----------|
| Submit Test | ✅ Connected | POST /api/tests/submit_result.php | test_page.dart |
| Recent Tests | ✅ Connected | GET /api/tests/get_history.php | home_page.dart |
| Test History | ✅ Connected | GET /api/tests/get_history.php | test_history_page.dart |
| User Stats | ✅ Connected | GET /api/tests/get_history.php | home_page.dart |
| Rankings | ✅ Connected | GET /api/tests/get_rankings.php | home_page.dart |

---

## What Changed

### Before:
- Test results only shown locally
- No data saved to database
- Recent tests empty
- Test history empty

### After:
- ✅ Test results saved to database
- ✅ Recent tests show real data
- ✅ Test history shows all tests
- ✅ Rankings calculated and updated
- ✅ User stats accurate

---

## Status: 🎉 FULLY FUNCTIONAL

All test-related features are now connected to the API and database!

