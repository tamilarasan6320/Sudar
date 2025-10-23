# 📱 TNPSC Mock Test - Manual Testing Guide

## 🎯 Complete Step-by-Step Testing Instructions

**Date:** October 23, 2025  
**App URL:** http://localhost:8888  
**API Test URL:** http://localhost/Mock_test/api_test.html

---

## ⚠️ Important Fix Applied

**API Base URL has been corrected in the code:**
- File: `lib/services/api_service.dart`
- Line 8: Changed from `http://localhost/MockTest/api` to `http://localhost/Mock_test/api`

**To apply this fix to the running app:**
1. Stop the current Flutter app (press `q` in the terminal where Flutter is running)
2. Run: `flutter clean`
3. Restart with: `flutter run -d web-server --web-port 8888 --web-hostname 0.0.0.0`

---

## 🧪 Testing Method 1: Using API Test Page (Recommended)

This is the **easiest** way to verify all APIs are working:

### Step 1: Open API Test Page
```
http://localhost/Mock_test/api_test.html
```

### Step 2: Test Individual APIs or All at Once
- Click "🧪 Test All APIs" button to test everything
- OR click individual API buttons to test one by one

### Step 3: Verify Results
- ✅ Green boxes = API working
- ✗ Red boxes = API error

All 11 APIs should show ✅ SUCCESS

---

## 📱 Testing Method 2: Manual Flutter App Testing

### Prerequisites:
1. ✅ XAMPP running (Apache + MySQL)
2. ✅ Flutter app running on port 8888
3. ✅ API base URL fixed (see above)
4. ✅ Browser opened to `http://localhost:8888`

---

## 🔑 STEP 1: Login Page Testing

### What You Should See:
- App logo with "O S T C P U R I" letters
- Large pink icon with "A" letter
- "Largest Learning Destination" text
- "Log in/Sign up" heading
- "Mobile Number" label
- Input field: "Enter Your 10 digit Mobile no."
- "CONTINUE" button

### Actions to Test:

#### Test 1.1: Enter Mobile Number
1. Click on the mobile number input field
2. Type: `9876543210`
3. Verify: Number appears in the field

#### Test 1.2: Validation Testing
1. Try entering less than 10 digits
2. Expected: Error message or button disabled
3. Try entering 11 digits
4. Expected: Only 10 digits accepted

#### Test 1.3: Send OTP
1. Enter valid mobile: `9876543210`
2. Click "CONTINUE" button
3. **Expected API Call:** `POST /api/auth/send_otp.php`
4. **Expected Response:**
   ```json
   {
     "success": true,
     "message": "OTP sent successfully",
     "otp": 123456,
     "expires_in": 600
   }
   ```

### How to Check API Call:
1. Open browser Developer Tools (F12)
2. Go to "Network" tab
3. Click "CONTINUE"
4. Look for request to: `localhost/Mock_test/api/auth/send_otp.php`
5. Check if status is `200 OK`
6. Click on the request and view "Response" tab to see the OTP

---

## 🔢 STEP 2: OTP Verification Page Testing

### What You Should See:
- "Verify OTP" heading
- 6 input boxes for OTP digits
- "Verify" button
- "Resend OTP" link/button
- Timer showing remaining time

### Actions to Test:

#### Test 2.1: Enter OTP
1. Copy the OTP from the previous API response
2. Enter the 6-digit OTP
3. Verify: Each digit appears in separate box

#### Test 2.2: Verify OTP
1. Enter correct OTP (from API response)
2. Click "Verify" button
3. **Expected API Call:** `POST /api/auth/verify_otp.php`
4. **Expected Response:**
   ```json
   {
     "success": true,
     "message": "OTP verified successfully",
     "user": {
       "id": 1,
       "mobile": "9876543210",
       "is_new_user": false
     }
   }
   ```
5. **Expected Behavior:**
   - If `is_new_user: true` → Redirect to Profile Setup Page
   - If `is_new_user: false` → Redirect to Home/Exam Selection Page

#### Test 2.3: Resend OTP
1. Click "Resend OTP" button
2. **Expected API Call:** `POST /api/auth/send_otp.php`
3. New OTP should be generated
4. Timer should reset

#### Test 2.4: Wrong OTP
1. Enter incorrect OTP: `000000`
2. Click "Verify"
3. **Expected:** Error message: "Invalid OTP"

---

## 👤 STEP 3: Profile Setup Page (New Users Only)

### What You Should See:
- "Complete Your Profile" heading
- Input fields:
  - Name (required)
  - Email (optional)
  - Age (optional)
  - District (optional)
  - Education (optional)
- Language selection: English/Tamil
- "Save" or "Continue" button

### Actions to Test:

#### Test 3.1: Fill Profile
1. Enter Name: `Test User`
2. Enter Email: `test@example.com`
3. Enter Age: `25`
4. Select District: `Chennai`
5. Select Education: `Graduate`
6. Select Language: `English`

#### Test 3.2: Create Profile
1. Click "Save" or "Continue"
2. **Expected API Call:** `POST /api/users/create.php`
3. **Expected Response:**
   ```json
   {
     "success": true,
     "message": "User created successfully",
     "user_id": 8
   }
   ```
4. **Expected Behavior:** Redirect to Exam Selection Page

---

## 🏠 STEP 4: Home / Exam Selection Page

### What You Should See:
- App header with user name
- "Select Exam Category" heading
- List of exam categories (cards):
  1. **TNPSC Group 1**
  2. **TNPSC Group 2**
  3. **TNPSC Group 4**
  4. **TNUSRB**
  5. More categories...
- Each card shows:
  - Icon
  - Exam name
  - Brief description

### Actions to Test:

#### Test 4.1: Load Exam Categories
1. Page loads automatically
2. **Expected API Call:** `GET /api/tests/get_exam_categories.php`
3. **Expected Response:**
   ```json
   {
     "success": true,
     "count": 5,
     "categories": [...]
   }
   ```
4. **Expected:** 5 exam category cards displayed

#### Test 4.2: Select Exam Category
1. Click on "TNPSC Group 1" card
2. **Expected Behavior:** Redirect to Test Categories Page

---

## 📚 STEP 5: Test Categories / Subjects Page

### What You Should See:
- "Select Subject" heading
- Exam name displayed (e.g., "TNPSC Group 1")
- List of subject categories:
  1. **Tamil Language**
  2. **General English**
  3. **General Science**
  4. **Indian History**
  5. **Geography**
  6. More subjects...
- Each card shows:
  - Subject icon/color
  - Subject name
  - Number of available tests

### Actions to Test:

#### Test 5.1: Load Test Categories
1. Page loads after selecting exam
2. **Expected API Call:** `GET /api/tests/get_categories.php?exam_id=1`
3. **Expected Response:**
   ```json
   {
     "success": true,
     "count": 6,
     "categories": [...]
   }
   ```
4. **Expected:** 6 subject cards displayed

#### Test 5.2: Select Subject
1. Click on "Tamil Language" card
2. **Expected Behavior:** Redirect to Question Sessions Page

---

## 📝 STEP 6: Question Sessions / Test Selection Page

### What You Should See:
- "Select Test" heading
- Subject name displayed (e.g., "Tamil Language")
- List of available test sessions:
  - **Tamil Basics - Session 1**
    - Duration: 15 minutes
    - Questions: 10
    - Difficulty: Easy
  - More sessions...
- Each session card shows:
  - Session name
  - Duration
  - Number of questions
  - Difficulty level
  - "Start Test" button

### Actions to Test:

#### Test 6.1: Load Question Sessions
1. Page loads after selecting subject
2. **Expected API Call:** `GET /api/tests/get_sessions.php?category_id=1`
3. **Expected Response:**
   ```json
   {
     "success": true,
     "count": 2,
     "sessions": [...]
   }
   ```
4. **Expected:** 2 or more session cards displayed

#### Test 6.2: Start Test
1. Click "Start Test" button on any session
2. **Expected Behavior:** Redirect to Test Taking Page

---

## ✍️ STEP 7: Test Taking Page

### What You Should See:
- Timer at the top (counting down)
- Question number (e.g., "Question 1/10")
- Question text
- Four option buttons (A, B, C, D)
- "Previous" and "Next" buttons
- "Submit Test" button (on last question)
- Progress indicator

### Actions to Test:

#### Test 7.1: Load Questions
1. Page loads after clicking "Start Test"
2. **Expected API Call:** `GET /api/tests/get_questions.php?session_id=1&language=en`
3. **Expected Response:**
   ```json
   {
     "success": true,
     "count": 10,
     "questions": [...]
   }
   ```
4. **Expected:** First question displayed

#### Test 7.2: Answer Questions
1. Click on option "A"
2. **Expected:** Option highlighted
3. Click "Next" button
4. **Expected:** Move to question 2
5. Repeat for all 10 questions

#### Test 7.3: Submit Test
1. Answer all questions
2. Click "Submit Test" button
3. Confirmation dialog: "Are you sure?"
4. Click "Yes, Submit"
5. **Expected API Call:** `POST /api/tests/submit_result.php`
6. **Expected Request Body:**
   ```json
   {
     "user_id": 1,
     "session_id": 1,
     "started_at": "2025-10-23 10:30:00",
     "time_taken": 12,
     "answers": [
       {"question_id": 1, "selected_option": "a", "is_correct": true},
       ...
     ]
   }
   ```
7. **Expected Response:**
   ```json
   {
     "success": true,
     "result_id": 5,
     "score": 8,
     "total": 10,
     "percentage": 80
   }
   ```
8. **Expected Behavior:** Redirect to Results Page

---

## 🏆 STEP 8: Test Results Page

### What You Should See:
- "Test Results" heading
- Score display (e.g., "8/10")
- Percentage: "80%"
- Time taken: "12 minutes"
- Breakdown:
  - Correct: 8
  - Wrong: 2
  - Unanswered: 0
- Detailed question-by-question analysis
- "View Rankings" button
- "Back to Home" button

### Actions to Test:

#### Test 8.1: View Results
1. Results page loads automatically after submission
2. Verify all statistics are correct
3. Scroll through question analysis

#### Test 8.2: View Rankings
1. Click "View Rankings" button
2. **Expected API Call:** `GET /api/tests/get_rankings.php?session_id=1`
3. **Expected Response:**
   ```json
   {
     "success": true,
     "count": 5,
     "rankings": [
       {
         "rank": 1,
         "user_name": "Arun Kumar",
         "avg_score": 90,
         ...
       },
       ...
     ]
   }
   ```
4. **Expected:** Leaderboard displayed with top users

---

## 📊 STEP 9: Test History Page

### What You Should See:
- "Test History" heading
- List of all tests taken by user
- Each entry shows:
  - Test name
  - Date taken
  - Score achieved
  - Time taken
  - "View Details" button

### Actions to Test:

#### Test 9.1: Load History
1. Navigate to History page (from menu/profile)
2. **Expected API Call:** `GET /api/tests/get_history.php?user_id=1`
3. **Expected Response:**
   ```json
   {
     "success": true,
     "count": 3,
     "history": [...]
   }
   ```
4. **Expected:** All previous tests displayed

---

## 👤 STEP 10: User Profile Page

### What You Should See:
- User avatar/icon
- User name
- Mobile number
- Email
- Statistics:
  - Total tests taken
  - Average score
  - Overall rank
- "Edit Profile" button
- "Logout" button

### Actions to Test:

#### Test 10.1: Load Profile
1. Navigate to Profile page
2. **Expected API Call:** `GET /api/users/get_profile.php?user_id=1`
3. **Expected Response:**
   ```json
   {
     "success": true,
     "user": {
       "id": 1,
       "name": "Test User",
       "mobile": "9876543210",
       "total_tests": 3,
       "avg_score": 85.5,
       ...
     }
   }
   ```
4. **Expected:** All user data displayed correctly

#### Test 10.2: Edit Profile
1. Click "Edit Profile"
2. Update name to "Updated Name"
3. Click "Save"
4. **Expected API Call:** `PUT /api/users/update.php`
5. **Expected Response:**
   ```json
   {
     "success": true,
     "message": "Profile updated successfully"
   }
   ```

---

## 🔍 Debugging Tips

### Check API Calls in Browser:
1. Open Developer Tools (F12)
2. Go to "Network" tab
3. Perform action in app
4. Look for requests to `localhost/Mock_test/api/...`
5. Check:
   - Request URL (should be `/Mock_test/api/...` not `/MockTest/api/...`)
   - Status Code (should be `200`)
   - Response data (should have `success: true`)

### Check Console Errors:
1. Open Developer Tools (F12)
2. Go to "Console" tab
3. Look for red error messages
4. Common errors:
   - `Failed to fetch` - API URL incorrect
   - `CORS error` - CORS headers not set
   - `404` - API endpoint not found
   - `500` - Server error

### Verify Backend:
1. Open: `http://localhost/Mock_test/api_test.html`
2. Click "Test All APIs"
3. All should show ✅ Success
4. If any show ✗ Error, check:
   - XAMPP is running
   - Apache service is active
   - MySQL service is active
   - Database exists and has data

---

## ✅ Testing Checklist

Use this checklist to track your testing progress:

- [ ] **Login Page**
  - [ ] Mobile input accepts 10 digits
  - [ ] Continue button works
  - [ ] OTP API call succeeds
  
- [ ] **OTP Page**
  - [ ] OTP input works
  - [ ] Verification succeeds
  - [ ] Resend OTP works
  
- [ ] **Profile Setup** (new users)
  - [ ] All fields accept input
  - [ ] Create user API works
  - [ ] Redirects to home
  
- [ ] **Exam Selection**
  - [ ] Shows 5 exam categories
  - [ ] Cards are clickable
  - [ ] Navigation works
  
- [ ] **Subject Selection**
  - [ ] Shows 6 subjects
  - [ ] Cards display correctly
  - [ ] Navigation works
  
- [ ] **Test Selection**
  - [ ] Shows available sessions
  - [ ] Session details display
  - [ ] Start test button works
  
- [ ] **Test Taking**
  - [ ] Questions load
  - [ ] Timer works
  - [ ] Options selectable
  - [ ] Navigation works
  - [ ] Submit works
  
- [ ] **Results Page**
  - [ ] Score displays correctly
  - [ ] Statistics accurate
  - [ ] Rankings work
  
- [ ] **Test History**
  - [ ] Shows all tests
  - [ ] Data is accurate
  
- [ ] **User Profile**
  - [ ] Profile loads
  - [ ] Statistics correct
  - [ ] Edit works
  - [ ] Logout works

---

## 🎯 Expected Outcomes

### All Tests Passing:
- ✅ All API calls return `200 OK`
- ✅ All responses have `success: true`
- ✅ All screens display correctly
- ✅ All navigation works smoothly
- ✅ No console errors
- ✅ Data persists across sessions

### If Tests Fail:
1. Check API base URL in code
2. Verify XAMPP is running
3. Check database connection
4. Review console errors
5. Use API test page to isolate issue
6. Check CORS configuration

---

## 📞 Quick Reference

**Flutter App:** http://localhost:8888  
**API Test Page:** http://localhost/Mock_test/api_test.html  
**Admin Panel:** http://localhost/Mock_test/admin/  
**Test Mobile:** 9876543210  
**Sample OTP:** Check API response

---

## 🚀 Quick Start for Testing

1. **Start XAMPP** (Apache + MySQL)
2. **Start Flutter App**: `flutter run -d web-server --web-port 8888`
3. **Open Browser**: http://localhost:8888
4. **Test Login**: Enter `9876543210`, click Continue
5. **Check Console**: Look for API call
6. **Check Response**: Should show OTP in Network tab
7. **Continue Testing**: Follow steps 1-10 above

---

**Happy Testing!** 🎉

If all tests pass, your app is **production-ready**! 🚀

