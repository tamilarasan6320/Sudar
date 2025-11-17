# Database Import Guide

## Database Export File
- **File:** `database_export.sql`
- **Database Name:** `mock_test_db`
- **Export Date:** 2025-11-17

## How to Import the Database

### Method 1: Using phpMyAdmin (Easiest)

1. Open phpMyAdmin: `http://localhost/phpmyadmin`
2. Click on "Import" tab
3. Click "Choose File" and select `database_export.sql`
4. Click "Go" to import
5. Done!

### Method 2: Using MySQL Command Line

```bash
# Navigate to MySQL bin directory
cd C:\xampp\mysql\bin

# Import database
mysql.exe -u root mock_test_db < database_export.sql
```

Or if database doesn't exist:

```bash
# Create database first
mysql.exe -u root -e "CREATE DATABASE mock_test_db;"

# Import database
mysql.exe -u root mock_test_db < database_export.sql
```

### Method 3: Using MySQL Workbench

1. Open MySQL Workbench
2. Connect to your MySQL server
3. Go to Server → Data Import
4. Select "Import from Self-Contained File"
5. Choose `database_export.sql`
6. Select "Create New Schema" or "Use Existing Schema" → `mock_test_db`
7. Click "Start Import"

## Database Structure

The database includes:
- Users table
- Exam categories
- Test categories
- Question sessions
- Questions
- Languages
- Test results
- Admin users

## Notes

- Default MySQL user: `root` (no password)
- If you have a password, add `-p` flag: `mysql.exe -u root -p`
- Make sure MySQL is running in XAMPP before importing

## Verification

After importing, verify tables exist:

```sql
USE mock_test_db;
SHOW TABLES;
```

You should see tables like:
- `users`
- `exam_categories`
- `test_categories`
- `languages`
- `question_sessions`
- `questions`
- `test_results`
- `admin_users`

