# 🎓 TNPSC Mock Test App

A comprehensive Flutter-based mock test application for TNPSC exam preparation with PHP backend and MySQL database.

---

## 📱 FEATURES

### **User App (Flutter)**
- ✅ OTP-based authentication
- ✅ Multiple exam categories (TNPSC Group 1, 2, 4, TNUSRB, VAO)
- ✅ Dynamic test sessions with real-time questions
- ✅ Progress tracking and analytics
- ✅ Test history with detailed results
- ✅ Leaderboard/Rankings
- ✅ Profile management
- ✅ Responsive web design

### **Admin Panel (HTML/JS)**
- ✅ User management
- ✅ Exam category management
- ✅ Test creation and editing
- ✅ Question bank management
- ✅ Results and analytics dashboard
- ✅ Role-based access control

### **Backend API (PHP)**
- ✅ RESTful API architecture
- ✅ Secure OTP authentication
- ✅ MySQL database integration
- ✅ CORS configured for web access
- ✅ Error handling and validation
- ✅ Real-time data synchronization

---

## 🚀 QUICK START

**⚡ 5-Minute Setup:**

1. **Copy project** to `htdocs/Mock_test`
2. **Start XAMPP/MAMP** (Apache + MySQL)
3. **Import database:** `database_complete.sql` via phpMyAdmin
4. **Test API:** `http://localhost/Mock_test/api/test_connection.php`
5. **Run Flutter:** `flutter run -d web-server --web-port 8888`
6. **Open app:** `http://localhost:8888`

**📖 Detailed Instructions:** See `QUICK_START.md` or `SETUP_GUIDE.md`

---

## 📋 REQUIREMENTS

- **XAMPP/MAMP** (Apache + MySQL + PHP 7.4+)
- **Flutter SDK** (latest stable)
- **MySQL** (5.7+ or MariaDB 10+)
- **Web Browser** (Chrome recommended)

---

## 📦 PROJECT STRUCTURE

```
Mock_test/
├── api/                          # PHP Backend
│   ├── auth/                     # Authentication endpoints
│   ├── tests/                    # Test management
│   ├── users/                    # User management
│   ├── admin/                    # Admin endpoints
│   └── config/                   # Configuration
├── admin/                        # Admin Panel
│   ├── index.html                # Admin login
│   ├── dashboard.html            # Dashboard
│   └── js/                       # JavaScript
├── lib/                          # Flutter App
│   ├── screens/                  # UI Screens
│   ├── services/                 # API Services
│   ├── utils/                    # Utilities
│   └── main.dart                 # Entry point
├── assets/                       # Images & Resources
├── database_complete.sql         # DB with sample data
├── database_structure.sql        # DB structure only
├── SETUP_GUIDE.md               # Complete setup guide
├── QUICK_START.md               # 5-minute setup
└── pubspec.yaml                 # Flutter dependencies
```

---

## 🔧 CONFIGURATION

### **Database Setup:**

1. **Create database:**
   ```sql
   CREATE DATABASE mock_test_db;
   ```

2. **Import tables:**
   ```bash
   mysql -u root < database_complete.sql
   ```

3. **Configure credentials:**
   ```php
   // api/config/database.php
   define('DB_HOST', 'localhost');
   define('DB_NAME', 'mock_test_db');
   define('DB_USER', 'root');
   define('DB_PASS', '');
   ```

### **API Configuration:**

```php
// api/config/config.php
define('API_BASE_URL', 'http://localhost/Mock_test/api');
define('ENABLE_DEBUG', true);
```

### **Flutter Configuration:**

```dart
// lib/services/api_service.dart
static const String baseUrl = 'http://localhost/Mock_test/api';
```

---

## 🎯 USAGE

### **User App:**

1. **Access:** `http://localhost:8888`
2. **Login:** Mobile: `8738474634` | OTP: `123456`
3. **Select exam** category
4. **Take tests** and view results
5. **Track progress** in Progress tab
6. **Check profile** and rankings

### **Admin Panel:**

1. **Access:** `http://localhost/Mock_test/admin/`
2. **Login:** Username: `admin` | Password: `admin123`
3. **Manage** exams, tests, questions
4. **View** user statistics and results
5. **Create** new test sessions

---

## 📊 DATABASE STRUCTURE

### **Tables:**

1. **users** - User accounts and profiles
2. **otp_verifications** - OTP authentication records
3. **exam_categories** - Exam types (TNPSC, TNUSRB, etc.)
4. **test_categories** - Subject categories
5. **question_sessions** - Test sessions
6. **questions** - Question bank with options
7. **test_history** - User test results
8. **rankings** - Leaderboard data
9. **admin_users** - Admin accounts

---

## 🔐 AUTHENTICATION

### **User Authentication:**
- Mobile number + OTP verification
- Test OTP: `123456` (for development)
- Session management with SharedPreferences

### **Admin Authentication:**
- Username + password login
- Session-based authentication
- Role-based access control

---

## 🌐 API ENDPOINTS

### **Authentication:**
- `POST /auth/send_otp.php` - Send OTP
- `POST /auth/verify_otp.php` - Verify OTP

### **Tests:**
- `GET /tests/get_categories.php` - Get test categories
- `GET /tests/get_sessions.php` - Get test sessions
- `GET /tests/get_questions.php` - Get questions
- `POST /tests/submit_test.php` - Submit test

### **Users:**
- `GET /users/get_profile.php` - Get user profile
- `POST /users/create.php` - Create user
- `PUT /users/update.php` - Update profile

### **History & Rankings:**
- `GET /tests/get_history.php` - Get test history
- `GET /tests/get_rankings.php` - Get rankings

---

## 🛠️ DEVELOPMENT

### **Setup Development Environment:**

```bash
# Clone/Copy project
cd /path/to/htdocs
cp -r Mock_test /Applications/XAMPP/xamppfiles/htdocs/

# Install Flutter dependencies
cd Mock_test
flutter pub get

# Run development server
flutter run -d web-server --web-port 8888
```

### **Hot Reload:**
```bash
# Press 'r' in terminal for hot reload
# Press 'R' for hot restart
```

### **Build for Production:**
```bash
flutter build web
# Output: build/web/
```

---

## 🐛 TROUBLESHOOTING

### **Database Connection Failed:**
```bash
# Check MySQL is running
http://localhost/phpmyadmin

# Verify database exists
SHOW DATABASES LIKE 'mock_test_db';

# Re-import if needed
mysql -u root < database_complete.sql
```

### **API 404 Errors:**
```bash
# Check Apache is running
http://localhost

# Verify .htaccess exists
ls -la api/.htaccess

# Check mod_rewrite is enabled (Apache config)
```

### **Flutter Build Errors:**
```bash
flutter clean
flutter pub get
flutter doctor
flutter run -d web-server --web-port 8888
```

### **CORS Issues:**
```bash
# Already configured in api/config/cors.php
# Restart Apache if changed
sudo /Applications/XAMPP/xamppfiles/xampp restart
```

---

## 📚 DOCUMENTATION

- **SETUP_GUIDE.md** - Complete setup instructions
- **QUICK_START.md** - 5-minute quick setup
- **DATABASE_IMPORT_INSTRUCTIONS.md** - Database setup
- **DATABASE_EXPORT_SUMMARY.md** - Database structure info

---

## 🚀 DEPLOYMENT

### **Production Deployment:**

1. **Backend:**
   - Upload to web hosting (PHP + MySQL)
   - Import database
   - Update `api/config/database.php`
   - Configure CORS for domain
   - Enable HTTPS

2. **Frontend:**
   ```bash
   flutter build web
   # Upload build/web/ to hosting
   ```

3. **Update API URL:**
   ```dart
   // lib/services/api_service.dart
   static const String baseUrl = 'https://yourdomain.com/api';
   ```

---

## 📝 CHANGELOG

### **Version 1.0 (October 2025)**
- ✅ Complete Flutter app with all features
- ✅ PHP REST API backend
- ✅ Admin panel with full CRUD
- ✅ OTP authentication
- ✅ Test history and rankings
- ✅ Progress tracking
- ✅ Database integration
- ✅ Sample data included
- ✅ Complete documentation

---

## 🤝 CONTRIBUTING

1. Fork the project
2. Create feature branch
3. Commit changes
4. Push to branch
5. Open pull request

---

## 📄 LICENSE

This project is for educational purposes. 

---

## 👥 SUPPORT

- **Issues:** Check troubleshooting section
- **Questions:** See SETUP_GUIDE.md
- **Updates:** Check for new versions

---

## 🎯 ROADMAP

### **Coming Soon:**
- [ ] Android/iOS mobile app build
- [ ] Offline mode support
- [ ] Advanced analytics dashboard
- [ ] Video explanations for questions
- [ ] Study materials section
- [ ] Discussion forum
- [ ] Push notifications
- [ ] Social sharing features

---

## ⚡ QUICK LINKS

- **App:** `http://localhost:8888`
- **Admin:** `http://localhost/Mock_test/admin/`
- **API Test:** `http://localhost/Mock_test/api/test_connection.php`
- **phpMyAdmin:** `http://localhost/phpmyadmin`

---

## 🎉 ACKNOWLEDGMENTS

Built with:
- Flutter for cross-platform UI
- PHP for backend API
- MySQL for database
- HTML/CSS/JS for admin panel

---

**Version:** 1.0  
**Last Updated:** October 23, 2025  
**Status:** Production Ready ✅

---

**Ready to start? See QUICK_START.md for 5-minute setup!** 🚀
