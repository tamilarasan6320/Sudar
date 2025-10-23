# 🚀 MOCK TEST APP - COMPLETE SETUP GUIDE

**Last Updated:** October 23, 2025  
**Version:** 1.0  
**Requirements:** XAMPP/MAMP, Flutter, MySQL

---

## 📋 PREREQUISITES

Before starting, ensure you have:

- ✅ **XAMPP/MAMP** installed (for PHP & MySQL)
- ✅ **Flutter SDK** installed (latest stable)
- ✅ **Git** (optional, for cloning)
- ✅ **Web Browser** (Chrome recommended)

**Download Links:**
- XAMPP: https://www.apachefriends.org/
- Flutter: https://flutter.dev/docs/get-started/install
- Git: https://git-scm.com/downloads

---

## 🎯 QUICK SETUP (5 MINUTES)

### **Step 1: Copy Project** (30 seconds)

```bash
# Copy the entire Mock_test folder to:

# For Mac XAMPP:
/Applications/XAMPP/xamppfiles/htdocs/Mock_test

# For Windows XAMPP:
C:\xampp\htdocs\Mock_test

# For Mac MAMP:
/Applications/MAMP/htdocs/Mock_test

# For Windows MAMP:
C:\MAMP\htdocs\Mock_test
```

---

### **Step 2: Start Servers** (30 seconds)

#### **Mac XAMPP:**
```bash
sudo /Applications/XAMPP/xamppfiles/xampp start
# Or use XAMPP Control Panel
```

#### **Windows XAMPP:**
```
1. Open XAMPP Control Panel
2. Click "Start" for Apache
3. Click "Start" for MySQL
```

#### **MAMP:**
```
1. Open MAMP
2. Click "Start Servers"
```

**Verify servers are running:**
- Apache: `http://localhost` should load
- MySQL: `http://localhost/phpmyadmin` should load

---

### **Step 3: Import Database** (1 minute)

#### **Option A: Using phpMyAdmin (Easiest)**

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
   - Click "Import" tab at the top
   - Click "Choose File" button
   - Select: `Mock_test/database_complete.sql`
   - Scroll down and click "Go"
   - Wait for "Import has been successfully finished" message

**✅ Done! Database is ready!**

#### **Option B: Using Command Line (Faster)**

```bash
# Mac XAMPP:
cd /Applications/XAMPP/xamppfiles/htdocs/Mock_test
/Applications/XAMPP/xamppfiles/bin/mysql -u root < database_complete.sql

# Windows XAMPP:
cd C:\xampp\htdocs\Mock_test
C:\xampp\mysql\bin\mysql.exe -u root < database_complete.sql

# Mac MAMP:
cd /Applications/MAMP/htdocs/Mock_test
/Applications/MAMP/Library/bin/mysql -u root -proot < database_complete.sql
```

---

### **Step 4: Test API** (30 seconds)

**Open in browser:**
```
http://localhost/Mock_test/api/test_connection.php
```

**You should see:**
```json
{
  "status": "success",
  "message": "Database connection successful!",
  "database": "mock_test_db"
}
```

✅ **If you see this, API is working!**

❌ **If error, check:**
- Apache is running
- MySQL is running
- Database was imported correctly
- File path is correct

---

### **Step 5: Test Admin Panel** (30 seconds)

**Open in browser:**
```
http://localhost/Mock_test/admin/index.html
```

**Default admin login:**
```
Username: admin
Password: admin123
```

✅ **If you can login, admin panel is working!**

---

### **Step 6: Run Flutter App** (2 minutes)

```bash
# Navigate to project
cd /Applications/XAMPP/xamppfiles/htdocs/Mock_test

# Get Flutter dependencies
flutter pub get

# Run on web server
flutter run -d web-server --web-port 8888 --web-hostname 0.0.0.0
```

**Wait for:**
```
lib/main.dart is being served at http://0.0.0.0:8888
```

**Then open in browser:**
```
http://localhost:8888
```

✅ **Flutter app is running!**

---

### **Step 7: Test Login** (30 seconds)

**In the Flutter app:**

1. Enter mobile number: `8738474634`
2. Click "Send OTP"
3. Enter OTP: `123456`
4. Click "Verify"

✅ **If you can login, everything is working perfectly!**

---

## 🎉 DONE! YOUR APP IS READY!

**You now have:**
- ✅ Working API
- ✅ Working Admin Panel
- ✅ Working Flutter App
- ✅ Sample data loaded
- ✅ Test user configured

---

## 📱 USING THE APP

### **User App (Flutter):**
```
URL: http://localhost:8888
Test Login: 8738474634 / OTP: 123456
```

**Features:**
- Login with OTP
- Select exam category
- Take tests
- View progress
- Check rankings
- Profile management

### **Admin Panel:**
```
URL: http://localhost/Mock_test/admin/
Username: admin
Password: admin123
```

**Features:**
- Manage users
- Create/edit exams
- Add test categories
- Create test sessions
- Add questions
- View test results

---

## 🔧 CONFIGURATION (Optional)

### **1. Change Database Password:**

If your MySQL has a password:

**Edit:** `Mock_test/api/config/database.php`
```php
<?php
define('DB_HOST', 'localhost');
define('DB_NAME', 'mock_test_db');
define('DB_USER', 'root');
define('DB_PASS', 'your_password_here');  // Change this
?>
```

### **2. Change Admin Password:**

**Via phpMyAdmin:**
1. Open: `http://localhost/phpmyadmin`
2. Select `mock_test_db`
3. Click `admin_users` table
4. Click "Edit" for admin user
5. Change password (use bcrypt hash or plain text)
6. Click "Go"

### **3. Change Flutter Port:**

If port 8888 is busy:
```bash
flutter run -d web-server --web-port 9999 --web-hostname 0.0.0.0
```

---

## 🐛 TROUBLESHOOTING

### **Problem: "Database connection failed"**

**Solution:**
```bash
# Check MySQL is running
http://localhost/phpmyadmin

# Check database exists
# In phpMyAdmin, look for mock_test_db in left sidebar

# Re-import if needed
mysql -u root < database_complete.sql
```

### **Problem: "404 Not Found" on API**

**Solution:**
```bash
# Check Apache is running
http://localhost

# Check file path is correct
# Should be: htdocs/Mock_test/api/...

# Check .htaccess files exist
ls -la Mock_test/api/.htaccess
```

### **Problem: "CORS error" in Flutter**

**Solution:**
```bash
# Already configured in PHP APIs
# Check api/config/cors.php exists

# Restart Apache
# Mac XAMPP:
sudo /Applications/XAMPP/xamppfiles/xampp restart

# Windows XAMPP:
# Use XAMPP Control Panel - Stop & Start Apache
```

### **Problem: "Port 8888 already in use"**

**Solution:**
```bash
# Option 1: Use different port
flutter run -d web-server --web-port 9999

# Option 2: Kill existing process
# Mac:
lsof -ti:8888 | xargs kill -9

# Windows:
netstat -ano | findstr :8888
taskkill /PID <PID> /F
```

### **Problem: Flutter build errors**

**Solution:**
```bash
# Clean and rebuild
flutter clean
flutter pub get
flutter run -d web-server --web-port 8888
```

---

## 📊 SAMPLE DATA INCLUDED

After importing `database_complete.sql`:

### **Users:**
- 1 test user: `8738474634`

### **Exams:**
- TNPSC Group 1
- TNPSC Group 2
- TNPSC Group 4
- TNUSRB Police
- VAO Exam

### **Test Categories:**
- Tamil Language
- English Language
- Mathematics
- General Knowledge
- General Science
- History

### **Test Sessions:**
- 6 sessions with varying difficulties

### **Questions:**
- 10 sample questions (Tamil Basics session)

### **Test History:**
- 1 completed test (80% score)

---

## 🔐 SECURITY NOTES

### **For Development:**
✅ Test OTP bypass enabled (123456)
✅ Sample admin user included
✅ CORS enabled for localhost

### **For Production:**
❌ Disable OTP bypass in `api/auth/verify_otp.php`
❌ Change admin password
❌ Configure CORS for specific domains
❌ Use environment variables for DB credentials
❌ Enable HTTPS

---

## 📁 PROJECT STRUCTURE

```
Mock_test/
├── api/                          # PHP Backend
│   ├── auth/                     # Authentication APIs
│   ├── tests/                    # Test management APIs
│   ├── users/                    # User management APIs
│   ├── admin/                    # Admin APIs
│   └── config/                   # Configuration files
├── admin/                        # Admin Panel (HTML/JS)
│   ├── index.html                # Login page
│   ├── dashboard.html            # Main dashboard
│   └── js/                       # JavaScript files
├── lib/                          # Flutter App
│   ├── screens/                  # App screens
│   ├── services/                 # API services
│   └── utils/                    # Utilities
├── database_complete.sql         # Complete DB with data
├── database_structure.sql        # DB structure only
├── SETUP_GUIDE.md               # This file
└── pubspec.yaml                 # Flutter config
```

---

## 🚀 DEPLOYMENT

### **Local Network Access:**

To access from other devices on same WiFi:

```bash
# Find your IP
# Mac:
ifconfig | grep "inet " | grep -v 127.0.0.1

# Windows:
ipconfig

# Run Flutter with 0.0.0.0
flutter run -d web-server --web-port 8888 --web-hostname 0.0.0.0

# Access from other devices:
http://YOUR_IP:8888
```

### **Production Deployment:**

1. **Backend (API):**
   - Deploy to web hosting with PHP & MySQL
   - Update `DB_HOST`, `DB_USER`, `DB_PASS`
   - Configure CORS for your domain
   - Enable HTTPS

2. **Flutter App:**
   ```bash
   flutter build web
   # Deploy build/web folder to hosting
   ```

3. **Update API URL:**
   - Edit `lib/services/api_service.dart`
   - Change `baseUrl` to production URL

---

## ✅ VERIFICATION CHECKLIST

After setup, verify everything works:

- [ ] Apache server running
- [ ] MySQL server running
- [ ] Database `mock_test_db` exists
- [ ] 9 tables imported successfully
- [ ] API test connection works
- [ ] Admin panel accessible
- [ ] Admin login works
- [ ] Flutter app runs on port 8888
- [ ] User can login with test credentials
- [ ] Tests are visible
- [ ] Questions load correctly
- [ ] Progress page shows data
- [ ] Profile page shows data

---

## 📞 SUPPORT

### **Common Issues:**

1. **Database import fails:**
   - Check MySQL is running
   - Check file path
   - Check file permissions

2. **API returns 404:**
   - Check Apache is running
   - Check mod_rewrite is enabled
   - Check .htaccess files exist

3. **Flutter build fails:**
   - Run `flutter doctor`
   - Run `flutter clean`
   - Delete `build/` folder

4. **Login doesn't work:**
   - Check API connection
   - Check database has user data
   - Check OTP verification endpoint

---

## 🎓 NEXT STEPS

After successful setup:

1. **Add More Questions:**
   - Login to admin panel
   - Navigate to Questions section
   - Add questions to test sessions

2. **Customize Exams:**
   - Add/edit exam categories
   - Create new test sessions
   - Configure difficulty levels

3. **Test the App:**
   - Take multiple tests
   - Check progress tracking
   - Verify rankings work

4. **Deploy to Production:**
   - Follow deployment guide above
   - Update configurations
   - Test thoroughly

---

## 🎉 SUCCESS!

**Your Mock Test App is now fully configured and running!**

- 🎯 API: `http://localhost/Mock_test/api/`
- 👨‍💼 Admin: `http://localhost/Mock_test/admin/`
- 📱 App: `http://localhost:8888`

**Happy Testing!** 🚀

---

**Version:** 1.0  
**Last Updated:** October 23, 2025  
**Support:** Check README.md for more details

