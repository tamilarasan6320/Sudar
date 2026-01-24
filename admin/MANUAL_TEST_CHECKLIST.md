# Admin Panel - Manual Testing Checklist

## 🔐 Login Test
- [ ] Navigate to: `http://localhost/MockTest/admin/`
- [ ] Username: `admin`
- [ ] Password: `admin123`
- [ ] **Expected**: Should login successfully and show dashboard

---

## 📊 Dashboard Tests
- [ ] Check if page loads without errors
- [ ] Verify stats show: Total Users, Total Tests, Total Questions, Active Sessions
- [ ] Check if "Recent Activity" section displays
- [ ] **Expected**: All stats should be 0 (empty database)

---

## 👥 Users Management Tests

### View Users
- [ ] Click "Users" in sidebar
- [ ] **Expected**: Empty table or "No users found" message

### Search Users (will work after adding data)
- [ ] Use search box to filter users
- [ ] **Expected**: Filtered results or empty state

---

## 📚 Exam Categories Tests

### View Categories
- [ ] Click "Exam Categories" in sidebar
- [ ] **Expected**: Empty grid with "+ Add Exam Category" button

### Add New Category
- [ ] Click "+ Add Exam Category"
- [ ] Fill form:
  - **Title**: TNPSC Group 4
  - **Name**: Combined Civil Services Examination - IV
  - **Description**: VAO and other Group IV services
  - **Icon**: 4
  - **Color**: #6C63FF
- [ ] Click "Save"
- [ ] **Expected**: Category card appears immediately
- [ ] **Database Check**: Open phpMyAdmin → `exam_categories` table should have 1 row

### Edit Category
- [ ] Click edit icon on category card
- [ ] Modify description
- [ ] Click "Save"
- [ ] **Expected**: Changes persist after page refresh

### Delete Category
- [ ] Click delete icon
- [ ] Confirm deletion
- [ ] **Expected**: Category removed from grid

---

## 📝 Test Categories Tests

### View Test Categories
- [ ] Click "Test Categories" in sidebar
- [ ] **Expected**: Empty table

### Add New Test Category
- [ ] Click "+ Add Test Category"
- [ ] Fill form:
  - **Exam Category**: TNPSC Group 4 (select from dropdown)
  - **Name**: Tamil Language
  - **Description**: Tamil language and literature
  - **Icon**: fas fa-language
  - **Color**: #9C27B0
- [ ] Click "Save"
- [ ] **Expected**: New row appears in table

---

## 📋 Question Sessions Tests

### View Sessions
- [ ] Click "Question Sessions" in sidebar
- [ ] **Expected**: Empty table

### Add New Session
- [ ] Click "+ Add Question Session"
- [ ] Fill form:
  - **Exam Category**: TNPSC Group 4
  - **Test Category**: Tamil Language
  - **Session Name**: Tamil - Practice Test 1
  - **Duration**: 60 minutes
  - **Total Questions**: 100
  - **Status**: Active
- [ ] Click "Save"
- [ ] **Expected**: New session appears in table

### View Session Questions
- [ ] Click eye icon on any session
- [ ] **Expected**: Modal opens showing "No questions yet"

---

## ❓ Questions Tests

### Upload CSV Questions
- [ ] Go to "Add Question" page
- [ ] Select a session from dropdown
- [ ] Click "Upload CSV"
- [ ] Upload sample CSV file
- [ ] **Expected**: Success message with count of added questions

### View Uploaded Questions
- [ ] Go back to "Question Sessions"
- [ ] Click eye icon on the session
- [ ] **Expected**: All uploaded questions displayed with language badges

### Edit Question
- [ ] In questions modal, click edit icon
- [ ] Modify question text
- [ ] Click "Save"
- [ ] **Expected**: Changes saved successfully

### Delete Question
- [ ] Click delete icon on a question
- [ ] Confirm deletion
- [ ] **Expected**: Question removed, count updated

### Clear All Questions
- [ ] In upload CSV modal, click "Clear Existing Questions First"
- [ ] Confirm action
- [ ] **Expected**: All questions removed from session

---

## 🎨 Settings Tests

### Update Privacy Policy
- [ ] Click "Settings" in sidebar
- [ ] Go to "Privacy Policy" tab
- [ ] Edit content
- [ ] Click "Save"
- [ ] **Expected**: Success notification

### Update Terms & Conditions
- [ ] Go to "Terms & Conditions" tab
- [ ] Edit content
- [ ] Click "Save"
- [ ] **Expected**: Success notification

---

## 🧪 API Test Page

### Run Automated Tests
- [ ] Open: `http://localhost/MockTest/admin/test_api.html`
- [ ] Click "🚀 Run All Tests"
- [ ] **Expected**: 
  - Summary shows total tests
  - Each test shows PASS/FAIL status
  - Green cards show passed tests
  - Red cards show failed tests

### Individual Test Categories
- [ ] Run "Authentication Tests"
  - Admin login should PASS
  - Invalid login should FAIL (expected)
- [ ] Run "Exam Categories API"
  - GET should return success
  - POST should create category
- [ ] Run other test categories
- [ ] **Expected**: All core API tests should pass

---

## 🔍 Browser Console Checks

### Network Tab
- [ ] Open DevTools (F12)
- [ ] Go to Network tab → Filter XHR
- [ ] Navigate through admin pages
- [ ] **Expected API Calls**:
  - `admin/users/list.php` → Status 200
  - `admin/exam_categories/crud.php` → Status 200
  - `admin/test_categories/crud.php` → Status 200
  - `admin/sessions/crud.php` → Status 200
  - `admin/questions/crud.php` → Status 200

### Console Tab
- [ ] Check for JavaScript errors (red text)
- [ ] **Expected**: No errors, only info/debug messages

---

## 💾 Database Persistence Tests

### Test Data Persistence
1. [ ] Add an exam category
2. [ ] Close browser completely
3. [ ] Open admin panel again
4. [ ] **Expected**: Category still visible (proves DB connection)

### Test Data Modification
1. [ ] Edit a category
2. [ ] Refresh page (F5)
3. [ ] **Expected**: Changes are still there

### Test Data Deletion
1. [ ] Delete a test category
2. [ ] Check phpMyAdmin
3. [ ] **Expected**: `is_active` set to 0 (soft delete) OR row removed

---

## ✅ Success Criteria

All tests should:
- ✓ Load without 404 errors
- ✓ Show proper empty states
- ✓ Save data to database
- ✓ Persist data after refresh
- ✓ Display success/error notifications
- ✓ Update UI immediately after actions
- ✓ Show correct API responses in test page

---

## 🐛 Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| 404 on API calls | Check XAMPP Apache is running |
| Empty response | Check MySQL is running |
| CORS errors | Verify `cors.php` is included in API files |
| Data not saving | Check phpMyAdmin for errors |
| Login fails | Verify admin user exists in `admin_users` table |

---

**Test Date**: _____________  
**Tested By**: _____________  
**Status**: ⬜ Pass  ⬜ Fail  ⬜ Partial  
**Notes**: ___________________________________

