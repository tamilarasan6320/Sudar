# Admin Panel - Database Connection Test Guide

## ✅ Pages Connected to Database

These pages now fetch data from MySQL database:

### 1. **Dashboard** (`#dashboard`)
- Shows empty stats when database is empty
- **Test**: Check if "Total Users: 0" and "Total Tests: 0" appear

### 2. **Users Management** (`#users`)
- Fetches from: `api/admin/users/list.php`
- **Test**: Should show empty table or "No users" message
- **Connected**: ✅ YES

### 3. **Exam Categories** (`#examCategories`)
- Fetches from: `api/admin/exam_categories/crud.php`
- **Test**: Should show empty grid
- **Connected**: ✅ YES

### 4. **Test Categories** (`#testCategories`)
- Fetches from: `api/admin/test_categories/crud.php`
- **Test**: Should show empty table
- **Connected**: ✅ YES

### 5. **Question Sessions** (`#questionSessions`)
- Fetches from: `api/admin/sessions/crud.php`
- **Test**: Should show empty table
- **Connected**: ✅ YES

### 6. **Add Question - View Questions** (Modal)
- Fetches from: `api/admin/questions/crud.php?session_id=X`
- **Test**: Click "View Questions" on any session
- **Connected**: ✅ YES

---

## 🧪 How to Test Database Connection

### Step 1: Open Browser Console
- Press `F12` or right-click → "Inspect"
- Go to "Console" tab

### Step 2: Check for API Calls
- Go to "Network" tab in DevTools
- Filter by "XHR" or "Fetch"
- Navigate to different pages in admin panel
- **You should see API requests like:**
  - `admin/users/list.php`
  - `admin/exam_categories/crud.php`
  - `admin/test_categories/crud.php`
  - `admin/sessions/crud.php`

### Step 3: Verify Response
- Click on any API request in Network tab
- Check "Response" tab
- **Should see JSON like:**
```json
{
  "success": true,
  "users": [],
  "total": 0
}
```

### Step 4: Add Test Data
1. Go to **Exam Categories** page
2. Click "+ Add Exam Category"
3. Fill form and save
4. **Check**: Category should appear immediately
5. **Database**: Open phpMyAdmin → Check `exam_categories` table

---

## 🔍 Quick Verification Commands

### Check Database Tables
```sql
USE mock_test_db;
SHOW TABLES;
```

### Check Admin User
```sql
SELECT * FROM admin_users;
```
**Result**: Should show `admin` user

### Check Empty Tables
```sql
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM exam_categories;
SELECT COUNT(*) FROM test_categories;
```
**Result**: All should be `0` (empty)

---

## 🐛 Troubleshooting

### If API calls fail:
1. Check XAMPP Apache is running
2. Check MySQL is running
3. Open: `http://localhost/MockTest/api/test_connection.php`
4. Verify all tests pass

### If console shows errors:
- `Failed to fetch` → XAMPP Apache not running
- `404 Not Found` → PHP file path incorrect
- `500 Internal Server Error` → Check PHP error logs
- CORS errors → Check `api/config/cors.php` is loaded

---

## 📊 Expected Behavior (Empty Database)

| Page | Expected Result |
|------|----------------|
| Dashboard | Stats show 0 |
| Users | Empty table |
| Exam Categories | Empty grid with "+ Add" button |
| Test Categories | Empty table |
| Question Sessions | Empty table |
| Questions | "No questions" message |

---

## ✅ Success Indicators

✔️ No console errors  
✔️ Network tab shows API requests  
✔️ API responses return JSON with `"success": true`  
✔️ Empty tables/grids display correctly  
✔️ Add/Edit forms work and save to database  
✔️ Data persists after page refresh  

---

**Created:** $(date)  
**Status:** Database integration complete - Ready for testing

