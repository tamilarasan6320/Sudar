# 📋 PENDING API INTEGRATIONS LIST

## 🔴 **APP (Flutter) - PENDING INTEGRATIONS**

### ✅ **COMPLETED APIs:**
1. ✅ `POST /auth/send_otp.php` - Send OTP
2. ✅ `POST /auth/verify_otp.php` - Verify OTP
3. ✅ `POST /users/create.php` - Create User
4. ✅ `GET /users/get_profile.php` - Get User Profile
5. ✅ `PUT /users/update.php` - Update User
6. ✅ `GET /tests/get_exam_categories.php` - Get Exam Categories
7. ✅ `GET /tests/get_categories.php` - Get Test Categories (with counts)
8. ✅ `GET /tests/get_sessions.php` - Get Question Sessions
9. ✅ `GET /tests/get_questions.php` - Get Questions
10. ✅ `POST /tests/submit_result.php` - Submit Test Result
11. ✅ `GET /tests/get_history.php` - Get Test History
12. ✅ `GET /tests/get_rankings.php` - Get Rankings

---

### 🔴 **PENDING APIs - APP:**

#### 1. **Performance Page** (`lib/screens/performance_page.dart`)
**Status:** ❌ **NOT CONNECTED - Using Hardcoded Data**

**Current State:**
- Shows hardcoded subject performance data
- Hardcoded strengths/weaknesses
- Hardcoded recent activity
- No API calls

**Required APIs:**
- ❌ `GET /tests/get_performance.php?user_id={id}` 
  - Should return:
    - Overall performance stats
    - Subject-wise performance breakdown
    - Strengths and weaknesses analysis
    - Performance trends over time
    - Recent activity timeline

**Priority:** 🔴 **HIGH** - Important feature for users

---

#### 2. **Saved Tests Page** (`lib/screens/saved_tests_page.dart`)
**Status:** ❌ **NOT IMPLEMENTED - No API**

**Current State:**
- Page exists but functionality not implemented
- No API service method
- No backend endpoint

**Required APIs:**
- ❌ `GET /tests/get_saved.php?user_id={id}` - Get saved tests
- ❌ `POST /tests/save.php` - Save a test for later
- ❌ `DELETE /tests/unsave.php` - Remove saved test

**Priority:** 🟡 **MEDIUM** - Nice to have feature

---

#### 3. **Test History Page** (`lib/screens/test_history_page.dart`)
**Status:** ⚠️ **PARTIALLY CONNECTED**

**Current State:**
- Uses `ApiService.getTestHistory()` ✅
- But may need additional details per test

**Required APIs:**
- ⚠️ `GET /tests/get_history.php` - Already exists ✅
- ❌ `GET /tests/get_result_details.php?id={result_id}` - Get detailed result
  - Should return:
    - Full question-by-question breakdown
    - Time spent per question
    - Correct/incorrect answers with explanations
    - Performance metrics

**Priority:** 🟡 **MEDIUM** - Enhancement

---

#### 4. **Profile Page - Update Profile**
**Status:** ⚠️ **PARTIALLY CONNECTED**

**Current State:**
- `ApiService.updateUser()` exists ✅
- But profile picture upload not implemented

**Required APIs:**
- ❌ `POST /users/upload_profile_pic.php` - Upload profile picture
  - Should accept multipart/form-data
  - Return image URL

**Priority:** 🟢 **LOW** - Optional feature

---

#### 5. **Progress Page - Detailed Analytics**
**Status:** ⚠️ **BASIC CONNECTION ONLY**

**Current State:**
- Uses `ApiService.getTestHistory()` ✅
- Shows basic stats only
- No charts/trends

**Required APIs:**
- ❌ `GET /tests/get_progress.php?user_id={id}&period={week|month|year}`
  - Should return:
    - Progress trends over time
    - Category-wise progress
    - Improvement metrics
    - Chart data points

**Priority:** 🟡 **MEDIUM** - Enhancement

---

#### 6. **Notifications/Updates**
**Status:** ❌ **NOT IMPLEMENTED**

**Required APIs:**
- ❌ `GET /notifications/list.php?user_id={id}` - Get notifications
- ❌ `POST /notifications/mark_read.php` - Mark as read
- ❌ `GET /updates/check.php` - Check for app updates

**Priority:** 🟢 **LOW** - Future feature

---

#### 7. **Offline Mode / Sync**
**Status:** ❌ **NOT IMPLEMENTED**

**Required APIs:**
- ❌ `POST /sync/upload.php` - Upload offline test results
- ❌ `GET /sync/check.php` - Check for pending syncs

**Priority:** 🟢 **LOW** - Future feature

---

## 🔴 **ADMIN PANEL - PENDING INTEGRATIONS**

### ✅ **COMPLETED APIs:**
1. ✅ `GET /admin/get_dashboard_stats.php` - Dashboard stats
2. ✅ `GET /admin/users/list.php` - List users
3. ✅ `POST /admin/users/crud.php` - Create user
4. ✅ `PUT /admin/users/crud.php` - Update user
5. ✅ `DELETE /admin/users/crud.php` - Delete user
6. ✅ `GET /admin/users/get_details.php` - Get user details
7. ✅ `GET /admin/exam_categories/crud.php` - List exam categories
8. ✅ `POST /admin/exam_categories/crud.php` - Create exam category
9. ✅ `PUT /admin/exam_categories/crud.php` - Update exam category
10. ✅ `DELETE /admin/exam_categories/crud.php` - Delete exam category
11. ✅ `GET /admin/languages/crud.php` - List languages
12. ✅ `POST /admin/languages/crud.php` - Create language
13. ✅ `PUT /admin/languages/crud.php` - Update language
14. ✅ `DELETE /admin/languages/crud.php` - Delete language
15. ✅ `GET /admin/test_categories/crud.php` - List test categories
16. ✅ `POST /admin/test_categories/crud.php` - Create test category
17. ✅ `PUT /admin/test_categories/crud.php` - Update test category
18. ✅ `DELETE /admin/test_categories/crud.php` - Delete test category
19. ✅ `GET /admin/sessions/crud.php` - List question sessions
20. ✅ `POST /admin/sessions/crud.php` - Create session
21. ✅ `PUT /admin/sessions/crud.php` - Update session
22. ✅ `DELETE /admin/sessions/crud.php` - Delete session
23. ✅ `GET /admin/questions/crud.php?session_id={id}` - Get questions
24. ✅ `POST /admin/questions/crud.php` - Create question
25. ✅ `PUT /admin/questions/crud.php` - Update question
26. ✅ `DELETE /admin/questions/crud.php` - Delete question
27. ✅ `POST /admin/questions/upload_csv.php` - Bulk upload questions

---

### 🔴 **PENDING APIs - ADMIN:**

#### 1. **Dashboard - Real-time Stats**
**Status:** ⚠️ **PARTIALLY CONNECTED**

**Current State:**
- `get_dashboard_stats.php` exists ✅
- But may need more detailed analytics

**Required APIs:**
- ❌ `GET /admin/get_analytics.php?period={day|week|month}`
  - Should return:
    - User growth trends
    - Test completion rates
    - Popular categories
    - Performance metrics
    - Revenue stats (if applicable)

**Priority:** 🟡 **MEDIUM** - Enhancement

---

#### 2. **Test Results Management**
**Status:** ❌ **NOT IMPLEMENTED**

**Current State:**
- No page/section to view all test results
- No way to analyze user performance from admin

**Required APIs:**
- ❌ `GET /admin/results/list.php?filters={...}` - List all test results
  - Filters: user_id, session_id, date_range, score_range
- ❌ `GET /admin/results/get_details.php?id={id}` - Get result details
- ❌ `GET /admin/results/analytics.php` - Get analytics
  - Average scores
  - Pass/fail rates
  - Category performance
  - User rankings

**Priority:** 🔴 **HIGH** - Important for admin

---

#### 3. **User Performance Analysis**
**Status:** ❌ **NOT IMPLEMENTED**

**Required APIs:**
- ❌ `GET /admin/users/performance.php?user_id={id}` - Get user performance
  - Test history
  - Improvement trends
  - Weak areas
  - Recommendations

**Priority:** 🟡 **MEDIUM** - Useful feature

---

#### 4. **Bulk Operations**
**Status:** ❌ **NOT IMPLEMENTED**

**Required APIs:**
- ❌ `POST /admin/questions/bulk_delete.php` - Delete multiple questions
- ❌ `POST /admin/sessions/bulk_delete.php` - Delete multiple sessions
- ❌ `POST /admin/users/bulk_delete.php` - Delete multiple users
- ❌ `POST /admin/questions/bulk_update.php` - Update multiple questions

**Priority:** 🟢 **LOW** - Convenience feature

---

#### 5. **Export/Import**
**Status:** ❌ **NOT IMPLEMENTED**

**Required APIs:**
- ❌ `GET /admin/export/questions.php?session_id={id}` - Export questions to CSV
- ❌ `GET /admin/export/results.php?format={csv|excel}` - Export results
- ❌ `POST /admin/import/questions.php` - Import questions (already have CSV upload)

**Priority:** 🟡 **MEDIUM** - Useful for backup

---

#### 6. **Settings Management**
**Status:** ❌ **NOT IMPLEMENTED**

**Current State:**
- Settings stored in JavaScript object
- No backend persistence

**Required APIs:**
- ❌ `GET /admin/settings/get.php` - Get app settings
- ❌ `POST /admin/settings/update.php` - Update settings
  - Privacy policy
  - Terms & conditions
  - App configuration
  - Feature flags

**Priority:** 🟡 **MEDIUM** - Important for configuration

---

#### 7. **Reports Generation**
**Status:** ❌ **NOT IMPLEMENTED**

**Required APIs:**
- ❌ `GET /admin/reports/users.php?format={pdf|excel}` - User report
- ❌ `GET /admin/reports/tests.php?format={pdf|excel}` - Test report
- ❌ `GET /admin/reports/performance.php?format={pdf|excel}` - Performance report

**Priority:** 🟢 **LOW** - Future feature

---

#### 8. **Admin Authentication**
**Status:** ❌ **NOT IMPLEMENTED**

**Current State:**
- No login required
- Anyone can access admin panel

**Required APIs:**
- ❌ `POST /admin/login.php` - Admin login
- ❌ `POST /admin/logout.php` - Admin logout
- ❌ `GET /admin/auth/check.php` - Check if logged in
- ❌ `POST /admin/auth/refresh.php` - Refresh session

**Priority:** 🔴 **HIGH** - Security critical!

---

#### 9. **Activity Logs**
**Status:** ❌ **NOT IMPLEMENTED**

**Required APIs:**
- ❌ `GET /admin/logs/list.php?type={action|error|access}` - Get logs
- ❌ `GET /admin/logs/export.php` - Export logs

**Priority:** 🟢 **LOW** - Debugging feature

---

#### 10. **Question Validation & Preview**
**Status:** ⚠️ **PARTIALLY IMPLEMENTED**

**Current State:**
- Can view questions ✅
- But no validation before saving

**Required APIs:**
- ❌ `POST /admin/questions/validate.php` - Validate question data
- ❌ `GET /admin/questions/preview.php?id={id}` - Preview question

**Priority:** 🟢 **LOW** - Quality assurance

---

## 📊 **SUMMARY**

### **APP (Flutter):**
- ✅ **12 APIs Connected**
- 🔴 **7 APIs Pending** (1 High, 3 Medium, 3 Low priority)

### **ADMIN PANEL:**
- ✅ **27 APIs Connected**
- 🔴 **10 APIs Pending** (2 High, 4 Medium, 4 Low priority)

---

## 🎯 **RECOMMENDED PRIORITY ORDER**

### **Phase 1 - Critical (Do First):**
1. 🔴 Admin Authentication (Security)
2. 🔴 Performance Page API (User feature)
3. 🔴 Test Results Management in Admin

### **Phase 2 - Important (Do Next):**
4. 🟡 Progress Page Analytics
5. 🟡 Test History Details
6. 🟡 Settings Management
7. 🟡 User Performance Analysis

### **Phase 3 - Nice to Have:**
8. 🟢 Saved Tests
9. 🟢 Export/Import
10. 🟢 Bulk Operations
11. 🟢 Other features

---

## 📝 **NOTES**

- All existing APIs are working ✅
- Most core functionality is connected ✅
- Pending items are mostly enhancements and admin features
- Security (admin auth) should be prioritized
- Performance analytics is important for user engagement

