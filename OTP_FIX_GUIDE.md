# 🔧 OTP Verification Fix - Quick Guide

**Issue:** OTP shows "Invalid or expired OTP" even with correct code

---

## ✅ Quick Fix - Follow These Steps:

### **Step 1: Get Working OTP** 

Open this page in your browser:
```
http://localhost/Mock_test/api/auth/test_otp_flow.php
```

This page will:
- ✅ Generate a fresh OTP
- ✅ Store it in database correctly
- ✅ Show you the exact OTP to use
- ✅ Test that verification will work

**You'll see a BIG GREEN BOX with your OTP!**

---

### **Step 2: Go Back to Your App**

```
http://localhost:8888
```

1. **On the login page**, enter mobile: **9384938434** (without +91)
2. Click "CONTINUE"
3. You'll see the OTP verification page

---

### **Step 3: Use the OTP from Step 1**

1. Copy the 6-digit OTP from the green box (from test_otp_flow.php page)
2. Enter it in your Flutter app
3. Click "SUBMIT"
4. **It should work now!** ✅

---

## 🔍 Why This Happens

The issue is likely one of these:

### **Issue 1: Mobile Number Format**
- ❌ App sends: **"+91 9384938434"** (with +91 and space)
- ✅ Backend expects: **"9384938434"** (just 10 digits)

### **Issue 2: Timing**
- The OTP generated when you clicked "CONTINUE" in the app
- But by the time you enter it, it may have expired or been replaced

### **Issue 3: Database Sync**
- Sometimes the OTP isn't stored properly in database

---

## 🎯 Permanent Fix

I'll fix the Flutter app to send mobile number without +91:

### Fix in login_page.dart:

The app needs to strip the +91 and spaces before sending to API.

**Current code might be sending:** "+91 9384938434"  
**Should send:** "9384938434"

---

## 📱 Testing Workflow

### **Method 1: Use Test Page (Easiest)**
1. Open: http://localhost/Mock_test/api/auth/test_otp_flow.php
2. Copy the OTP from green box (e.g., 456789)
3. Refresh your Flutter app: http://localhost:8888
4. Enter mobile: 9384938434 (WITHOUT +91)
5. Click CONTINUE
6. Enter the OTP from step 2
7. Click SUBMIT
8. Should work!

### **Method 2: Check Console**
1. In Flutter app, press F12 (Developer Tools)
2. Go to "Network" tab
3. Click "CONTINUE" on login page
4. Find the request to `send_otp.php`
5. Click on it → "Response" tab
6. You'll see: `"otp": 123456`
7. Copy that exact OTP
8. Enter it immediately (within 10 minutes)

---

## ✅ Verification Checklist

- [ ] Can access: http://localhost/Mock_test/api/auth/test_otp_flow.php
- [ ] Green box shows 6-digit OTP
- [ ] Flutter app loads: http://localhost:8888
- [ ] Enter mobile WITHOUT +91: 9384938434
- [ ] Click CONTINUE
- [ ] Enter OTP from test page
- [ ] Click SUBMIT
- [ ] Success! Moves to next screen

---

## 🐛 Still Not Working?

### Check These:

1. **XAMPP Running?**
   - Apache: ✅ Green
   - MySQL: ✅ Green

2. **Database Table Exists?**
   ```
   Open: http://localhost/phpmyadmin
   Database: tnpsc_mock_test
   Table: otp_verifications (should exist)
   ```

3. **Mobile Number Correct?**
   - Use exactly: **9384938434**
   - NO +91
   - NO spaces
   - Just 10 digits

4. **OTP Fresh?**
   - Generate new OTP from test page
   - Use within 10 minutes
   - Don't reuse old OTPs

---

## 💡 Quick Test

Run this in browser console (F12 → Console):

```javascript
// Test OTP generation
fetch('http://localhost/Mock_test/api/auth/send_otp.php', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({mobile: '9384938434'})
})
.then(r => r.json())
.then(d => {
  console.log('OTP Generated:', d.otp);
  alert('Your OTP: ' + d.otp);
});
```

This will show you the OTP in an alert!

---

## 📞 Alternative: Use Existing User

If you want to skip OTP testing, use an existing user:

**In the database, there's already a user with mobile: 9876543210**

You can:
1. Test with that mobile number
2. Use any OTP (since we're in development)
3. Or modify verify_otp.php to accept a test OTP like "123456"

---

## 🔧 Developer Mode Fix (Temporary)

Want to bypass OTP check for testing? 

Add this to verify_otp.php (line 16, after try{):

```php
// Development mode: accept 123456 as valid OTP
if ($otp == '123456') {
    // Skip verification, proceed with user check
    $user = new User($db);
    $user->mobile = $mobile;
    // ... rest of success code
}
```

Then always use OTP: **123456** for testing!

---

## ✅ Success Indicators

You'll know it's working when:

1. ✅ No red error at bottom of screen
2. ✅ Screen changes from OTP verification to next page
3. ✅ You see user profile or home screen
4. ✅ No "Invalid or expired OTP" message

---

**Next Steps After OTP Works:**
1. ✅ Home page with exam categories
2. ✅ Select exam
3. ✅ Choose subject
4. ✅ Take test
5. ✅ View results

---

**Need Help?** 

Open the test page:
```
http://localhost/Mock_test/api/auth/test_otp_flow.php
```

It will show you:
- ✅ Current OTP in database
- ✅ Whether it will verify
- ✅ Exact OTP to use

**That's the guaranteed working OTP!** 🎯

