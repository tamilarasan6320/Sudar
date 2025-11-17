# 📱 Physical Device Setup Guide

## ✅ Configuration Updated

Your API service has been configured to use your computer's IP address: **192.168.1.4**

## 🔧 Important Steps

### 1. **Ensure Same WiFi Network**
- ✅ Your phone and computer must be on the **same WiFi network**
- ✅ Check WiFi name matches on both devices

### 2. **Check XAMPP Apache is Running**
- ✅ Open XAMPP Control Panel
- ✅ Apache should show "Running" (green)
- ✅ Test in browser: `http://localhost/MockTest/api/test_connection.php`

### 3. **Windows Firewall Configuration**
If the API still doesn't work, you may need to allow Apache through Windows Firewall:

**Option A: Allow Apache through Firewall**
1. Open Windows Defender Firewall
2. Click "Allow an app or feature through Windows Defender Firewall"
3. Find "Apache HTTP Server" and check both Private and Public
4. If not listed, click "Allow another app" → Browse → Select `C:\xampp\apache\bin\httpd.exe`

**Option B: Temporarily Disable Firewall (for testing only)**
1. Open Windows Defender Firewall
2. Turn off firewall for Private networks (temporarily)
3. Test the app
4. Re-enable firewall after testing

### 4. **Test API Accessibility**
Test if your API is accessible from the network:

**On your phone's browser:**
```
http://192.168.1.4/MockTest/api/test_connection.php
```

If this works, the app should work too!

## 🔄 Switching Between Emulator and Physical Device

In `lib/services/api_service.dart`, line 14:

**For Android Emulator:**
```dart
static const String androidHost = '10.0.2.2';
```

**For Physical Device:**
```dart
static const String androidHost = '192.168.1.4';  // Your computer's IP
```

## 🧪 Testing Steps

1. **Rebuild the app** in Android Studio
2. **Run on your physical device**
3. **Check Android Studio Logcat** for debug messages:
   - Look for: `API Call: POST http://192.168.1.4/MockTest/api/...`
   - Look for: `API Response: ...` or `API Error: ...`

## ❌ Troubleshooting

### Issue: Still getting timeout error
1. ✅ Verify XAMPP Apache is running
2. ✅ Check phone and computer on same WiFi
3. ✅ Test API in phone browser: `http://192.168.1.4/MockTest/api/test_connection.php`
4. ✅ Check Windows Firewall settings
5. ✅ Verify IP address is correct: Run `ipconfig` in Command Prompt

### Issue: "Cannot reach server"
- Check if your computer's IP changed (WiFi networks can assign different IPs)
- Run `ipconfig` again and update `androidHost` in `api_service.dart`

### Issue: API works in browser but not in app
- Check Android Studio Logcat for detailed error messages
- Verify `usesCleartextTraffic="true"` is in AndroidManifest.xml (already added)

## 📝 Current Configuration

- **API Base URL**: `http://192.168.1.4/MockTest/api`
- **Your Computer IP**: `192.168.1.4`
- **Platform**: Physical Android Device

## ✅ Next Steps

1. Rebuild and run the app
2. Try logging in with mobile number
3. Check Logcat for API call logs
4. If still not working, test API in phone browser first

---

**Note**: If your computer's IP address changes (e.g., connecting to different WiFi), you'll need to update `androidHost` in `api_service.dart` again.

