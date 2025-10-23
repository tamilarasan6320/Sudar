# ⚡ QUICK START - 5 MINUTES SETUP

**For:** New machine setup  
**Time:** 5 minutes  
**Result:** Fully working app

---

## ✅ COPY THIS AND FOLLOW STEP BY STEP

### **1️⃣ COPY PROJECT (30 sec)**

```bash
# Copy Mock_test folder to:
/Applications/XAMPP/xamppfiles/htdocs/Mock_test     # Mac XAMPP
C:\xampp\htdocs\Mock_test                           # Windows XAMPP
```

---

### **2️⃣ START SERVERS (30 sec)**

**Mac XAMPP:**
```bash
sudo /Applications/XAMPP/xamppfiles/xampp start
```

**Windows XAMPP:**
- Open XAMPP Control Panel
- Start Apache
- Start MySQL

✅ **Check:** `http://localhost/phpmyadmin` should load

---

### **3️⃣ IMPORT DATABASE (1 min)**

**Open:** `http://localhost/phpmyadmin`

1. Click "New" → Database name: `mock_test_db` → Create
2. Click "Import" tab
3. Choose file: `Mock_test/database_complete.sql`
4. Click "Go"

✅ **Check:** Should see "Import has been successfully finished"

---

### **4️⃣ TEST API (15 sec)**

**Open:** `http://localhost/Mock_test/api/test_connection.php`

✅ **Should see:** `{"status":"success",...}`

---

### **5️⃣ TEST ADMIN (15 sec)**

**Open:** `http://localhost/Mock_test/admin/`

**Login:**
- Username: `admin`
- Password: `admin123`

✅ **Should see:** Admin dashboard

---

### **6️⃣ RUN FLUTTER (2 min)**

```bash
cd /Applications/XAMPP/xamppfiles/htdocs/Mock_test
flutter pub get
flutter run -d web-server --web-port 8888 --web-hostname 0.0.0.0
```

✅ **Wait for:** "lib/main.dart is being served at..."

---

### **7️⃣ TEST APP (30 sec)**

**Open:** `http://localhost:8888`

**Login:**
- Mobile: `8738474634`
- OTP: `123456`

✅ **Should see:** Exam selection screen

---

## 🎉 DONE! 

**Your app is running:**

- 📱 **Flutter App:** `http://localhost:8888`
- 👨‍💼 **Admin Panel:** `http://localhost/Mock_test/admin/`
- 🔌 **API:** `http://localhost/Mock_test/api/`

**Test credentials:**
- User: `8738474634` / OTP: `123456`
- Admin: `admin` / `admin123`

---

## ❌ IF SOMETHING FAILS:

**Database error?**
```bash
# Re-import:
mysql -u root < database_complete.sql
```

**Port 8888 busy?**
```bash
# Use different port:
flutter run -d web-server --web-port 9999
```

**API 404 error?**
- Check Apache is running
- Check path: `htdocs/Mock_test/api/`

**Flutter build error?**
```bash
flutter clean
flutter pub get
flutter run -d web-server --web-port 8888
```

---

## 📖 FULL GUIDE

For detailed instructions: **See SETUP_GUIDE.md**

---

**That's it! App should be fully working now!** 🚀

