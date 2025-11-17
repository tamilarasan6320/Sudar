# API Connections - Test Results & History

## ✅ Already Connected & Working

### 1. **Recent Tests in Home Page**
**Location:** `lib/screens/home_page.dart` (Line 178-195)

**API Call:**
```dart
final historyResponse = await ApiService.getTestHistory(userId: userId);
```

**API Endpoint:** `GET /api/tests/get_history.php?user_id={userId}&limit={limit}`

**What it does:**
- Fetches user's test history from database
- Shows last 3 tests in "Recent Tests" section
- Calculates total tests taken
- Calculates average score

**Response Format:**
```json
{
  "success": true,
  "count": 3,
  "history": [
    {
      "id": 1,
      "test_name": "Mock test 1",
      "category_name": "Tamil",
      "total_questions": 10,
      "attempted": 8,
      "correct": 6,
      "wrong": 2,
      "unanswered": 2,
      "score": 5.5,
      "percentage": 75.00,
      "rank": 5,
      "time_taken": 1200,
      "submitted_at": "2025-01-16 10:30:00"
    }
  ]
}
```

### 2. **Submit Test Results**
**Location:** `lib/screens/test_page.dart` (Line 127-180)

**API Call:**
```dart
final response = await ApiService.submitTestResult(
  userId: userId,
  sessionId: widget.sessionId,
  answers: answers,
  timeTaken: (3600 - _timeRemaining),
);
```

**API Endpoint:** `POST /api/tests/submit_result.php`

**What it does:**
- Saves test results to database
- Calculates score, percentage, rank
- Stores individual answers
- Updates user rankings

**Request Format:**
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

### 3. **Test History Page**
**Location:** `lib/screens/test_history_page.dart`

**API Call:** Same as Recent Tests (get_history.php)

**What it shows:**
- Full list of all tests taken
- Test scores and rankings
- Time taken for each test
- Filter by test category

### 4. **Rankings**
**Location:** `lib/screens/home_page.dart` (Line 197-205)

**API Call:**
```dart
final rankingsResponse = await ApiService.getRankings();
```

**API Endpoint:** `GET /api/tests/get_rankings.php?limit={limit}`

**What it does:**
- Fetches all user rankings
- Shows user's current rank on home page

---

## How It Works

### When User Takes a Test:

1. **Test Page Loads:**
   - `test_page.dart` fetches questions from API
   - Timer starts

2. **User Answers Questions:**
   - Answers stored in `_selectedAnswers` map
   - Marked questions stored in `_markedForReview` set

3. **User Submits Test:**
   - `_submitTest()` method called
   - Sends all answers to `submit_result.php`
   - API calculates:
     - Total correct/wrong/unanswered
     - Score (marks - negative marks)
     - Percentage
     - Rank among all users

4. **Results Saved to Database:**
   - `test_results` table: Overall result
   - `user_answers` table: Individual answers
   - Ranks updated for all users

5. **User Redirected:**
   - Goes to `test_results_page.dart`
   - Shows score, percentage, rank
   - Option to review answers

### When User Opens Home Page:

1. **Home Page Loads:**
   - Calls `_loadUserStats()`
   - Fetches test history from `get_history.php`

2. **Data Displayed:**
   - Total tests taken
   - Average score
   - Last 3 tests in "Recent Tests" section
   - User's current rank

---

## Database Tables

### 1. `test_results`
```sql
- id
- user_id
- session_id
- total_questions
- attempted_questions
- correct_answers
- wrong_answers
- unanswered
- score
- percentage
- rank
- time_taken (seconds)
- started_at
- submitted_at
```

### 2. `user_answers`
```sql
- id
- result_id
- question_id
- user_answer (A, B, C, D)
- is_correct (0 or 1)
- time_spent (seconds)
- marked_for_review (0 or 1)
```

---

## Testing

### To verify it's working:

1. **Login to app** (use OTP 111111)
2. **Select an exam** (e.g., TNPSC Group 4)
3. **Start a test** from Tests page
4. **Answer some questions** and submit
5. **Check Results Page** - Should show score/rank
6. **Go to Home Page** - Should show:
   - Tests Taken count updated
   - Recent test in Recent Tests section
   - Average score calculated
7. **Check Test History** - Should list all completed tests

---

## Status: ✅ FULLY CONNECTED

All features are already connected to the API:
- ✅ Submit test results
- ✅ View recent tests
- ✅ View test history
- ✅ Calculate rankings
- ✅ Show user stats

No additional work needed - everything is functional!

