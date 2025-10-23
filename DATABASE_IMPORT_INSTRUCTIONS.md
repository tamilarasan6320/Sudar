# 📦 DATABASE IMPORT INSTRUCTIONS

**Date:** October 23, 2025  
**Database Name:** `mock_test_db`

---

## 📁 EXPORTED FILES

We've created **2 SQL files** for easy database setup:

### 1. **`database_structure.sql`** (Structure Only)
- ✅ Contains all table definitions (CREATE TABLE)
- ✅ No data included
- ✅ Perfect for fresh installation
- ✅ ~2-5 KB file size

### 2. **`database_complete.sql`** (Structure + Sample Data)
- ✅ Contains all table definitions
- ✅ Includes all current data (users, tests, questions, etc.)
- ✅ Perfect for cloning your exact setup
- ✅ Larger file size (depends on data)

---

## 🚀 IMPORT METHODS

### **Method 1: Using phpMyAdmin** (Easiest)

1. **Open phpMyAdmin:**
   ```
   http://localhost/phpmyadmin
   ```

2. **Create Database:**
   - Click "New" in left sidebar
   - Database name: `mock_test_db`
   - Collation: `utf8mb4_general_ci`
   - Click "Create"

3. **Import SQL File:**
   - Select `mock_test_db` from left sidebar
   - Click "Import" tab
   - Click "Choose File"
   - Select `database_complete.sql` (or `database_structure.sql`)
   - Click "Go"
   - Wait for success message

---

### **Method 2: Using MySQL Command Line**

#### Option A: Structure Only
```bash
# Open Terminal/Command Prompt
cd /path/to/Mock_test

# Import structure
mysql -u root -p < database_structure.sql

# You'll be prompted for password (default: empty for XAMPP)
```

#### Option B: Complete with Data
```bash
# Open Terminal/Command Prompt
cd /path/to/Mock_test

# Import complete database
mysql -u root -p < database_complete.sql
```

---

### **Method 3: Using XAMPP/MAMP Command Line**

#### For XAMPP (Mac/Windows):
```bash
# Mac
/Applications/XAMPP/xamppfiles/bin/mysql -u root mock_test_db < database_complete.sql

# Windows
C:\xampp\mysql\bin\mysql.exe -u root mock_test_db < database_complete.sql
```

#### For MAMP:
```bash
/Applications/MAMP/Library/bin/mysql -u root -p mock_test_db < database_complete.sql
```

---

## 📊 DATABASE STRUCTURE

### **Tables Included:**

1. **`users`** - User accounts
   - id, name, mobile, email, age, district, education, language, profile_pic
   - created_at, updated_at, last_login

2. **`otp_verifications`** - OTP authentication
   - id, mobile, otp, expires_at, is_verified, created_at

3. **`exam_categories`** - Exam types (TNPSC, TNUSRB, etc.)
   - id, name, description, icon, is_active, created_at

4. **`test_categories`** - Test subject categories
   - id, exam_category_id, name, description, icon, is_active, created_at

5. **`question_sessions`** - Test sessions
   - id, category_id, name, description, duration, passing_percentage
   - total_questions, difficulty_level, is_active, created_at

6. **`questions`** - Test questions
   - id, session_id, question_text, option_a, option_b, option_c, option_d
   - correct_answer, explanation, marks, difficulty, is_active, created_at

7. **`test_history`** - User test results
   - id, user_id, session_id, score, total_questions, percentage
   - time_taken, started_at, completed_at

8. **`rankings`** - Leaderboard
   - id, user_id, session_id, rank, score, percentage, updated_at

---

## ✅ VERIFICATION

After importing, verify the database:

### **1. Check Tables:**
```sql
USE mock_test_db;
SHOW TABLES;
```

Expected output:
```
+-------------------------+
| Tables_in_mock_test_db  |
+-------------------------+
| exam_categories         |
| otp_verifications       |
| question_sessions       |
| questions               |
| rankings                |
| test_categories         |
| test_history            |
| users                   |
+-------------------------+
```

### **2. Check Data:**
```sql
SELECT COUNT(*) as exam_count FROM exam_categories;
SELECT COUNT(*) as user_count FROM users;
SELECT COUNT(*) as session_count FROM question_sessions;
SELECT COUNT(*) as question_count FROM questions;
```

---

## 🔧 TROUBLESHOOTING

### **Error: Database already exists**
```sql
DROP DATABASE IF EXISTS mock_test_db;
-- Then import again
```

### **Error: Access denied**
```bash
# Check MySQL is running
# Check username/password
# Default XAMPP: user=root, password=(empty)
```

### **Error: File too large (phpMyAdmin)**
Solution 1: Use command line method instead

Solution 2: Edit `php.ini`:
```ini
upload_max_filesize = 128M
post_max_size = 128M
max_execution_time = 600
```

### **Error: Unknown collation**
Edit the SQL file and change:
```sql
-- Change from:
CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci

-- To:
CHARSET=utf8 COLLATE=utf8_general_ci
```

---

## 📝 CONFIGURATION

After importing, update your API configuration:

### **File:** `api/config/database.php`
```php
<?php
define('DB_HOST', 'localhost');
define('DB_NAME', 'mock_test_db');
define('DB_USER', 'root');
define('DB_PASS', '');  // Change if you set a password
```

---

## 🎯 SAMPLE DATA INCLUDED

If you imported `database_complete.sql`, you'll have:

- ✅ 5 Exam Categories (TNPSC Group 1, 2, 4, TNUSRB, VAO)
- ✅ 6 Test Categories (Tamil, English, Maths, GK, Science, History)
- ✅ 6 Test Sessions (Tamil Basics, English Grammar, etc.)
- ✅ 10 Sample Questions (for Tamil Basics session)
- ✅ 1 Test User (mobile: 8738474634)
- ✅ 1 Test History Entry (80% score)

### **Test User Credentials:**
```
Mobile: 8738474634
OTP: 123456 (test bypass enabled)
```

---

## 🚀 QUICK START

### **1. Import Database:**
```bash
mysql -u root < database_complete.sql
```

### **2. Configure API:**
```bash
# Edit api/config/database.php if needed
```

### **3. Test Connection:**
```bash
# Open in browser:
http://localhost/Mock_test/api/test_connection.php
```

### **4. Run Flutter App:**
```bash
cd Mock_test
flutter run -d web-server --web-port 8888
```

### **5. Test Login:**
```
Mobile: 8738474634
OTP: 123456
```

---

## 📦 FOR DISTRIBUTION

### **What to Share:**

**Minimum (Structure Only):**
```
Mock_test/
├── database_structure.sql         ← Import this
├── DATABASE_IMPORT_INSTRUCTIONS.md
├── api/
├── lib/
├── pubspec.yaml
└── ... other project files
```

**Complete (With Sample Data):**
```
Mock_test/
├── database_complete.sql          ← Import this
├── DATABASE_IMPORT_INSTRUCTIONS.md
├── api/
├── lib/
├── pubspec.yaml
└── ... other project files
```

---

## ⚠️ SECURITY NOTE

**Before sharing `database_complete.sql`:**
- ❌ Remove real user data
- ❌ Remove sensitive information
- ❌ Don't include production passwords
- ✅ Only include sample/test data

**For production:**
- Use `database_structure.sql` only
- Let each environment have its own data
- Use environment variables for DB credentials

---

## 🎉 DONE!

After importing, your database is ready to use with:
- ✅ All tables created
- ✅ Proper relationships
- ✅ Sample data (if imported complete version)
- ✅ Ready for the Flutter app

**Test the app at:** `http://localhost:8888`

**Questions?** Check the main project README!

