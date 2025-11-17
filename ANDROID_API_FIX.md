# 🔧 Android API Localhost Fix

## ✅ Problem Fixed

The Android app was using `localhost` for API calls, which doesn't work on Android devices/emulators because `localhost` refers to the device itself, not your development machine.

## 🛠️ Changes Made

### 1. **API Service Updated** (`lib/services/api_service.dart`)
- Added platform detection to use the correct base URL:
  - **Web**: `http://localhost/MockTest/api` ✅
  - **Android Emulator**: `http://10.0.2.2/MockTest/api` ✅
  - **Android Physical Device**: Needs your computer's IP address (see below)
  - **iOS Simulator**: `http://localhost/MockTest/api` ✅

### 2. **Android Manifest Updated** (`android/app/src/main/AndroidManifest.xml`)
- Added `INTERNET` permission
- Added `ACCESS_NETWORK_STATE` permission
- Enabled `usesCleartextTraffic="true"` to allow HTTP (not HTTPS) connections

## 📱 How to Use

### For Android Emulator:
✅ **Ready to use!** The app will automatically use `10.0.2.2` which maps to your host machine's localhost.

### For Physical Android Device:

1. **Find your computer's IP address:**
   - **Windows**: Open Command Prompt and run:
     ```cmd
     ipconfig
     ```
     Look for "IPv4 Address" under your active network adapter (usually starts with `192.168.x.x`)
   
   - **Mac/Linux**: Open Terminal and run:
     ```bash
     ifconfig | grep "inet "
     ```
     Look for IP address starting with `192.168.x.x` or `10.x.x.x`

2. **Update the API service:**
   - Open `lib/services/api_service.dart`
   - Find line 18 (Android section)
   - Replace `10.0.2.2` with your computer's IP address:
     ```dart
     return 'http://192.168.1.100/MockTest/api';  // Replace with your IP
     ```

3. **Ensure your phone and computer are on the same WiFi network**

4. **Make sure XAMPP Apache is running and accessible:**
   - Test in browser on your computer: `http://localhost/MockTest/api/test_connection.php`
   - Test with your IP: `http://YOUR_IP/MockTest/api/test_connection.php`

## 🧪 Testing

1. **Run the app from Android Studio**
2. **Try to login** - The API should now connect successfully
3. **Check the logs** in Android Studio if there are any connection errors

## 🔍 Troubleshooting

### Issue: Still can't connect on emulator
- ✅ Make sure XAMPP Apache is running
- ✅ Verify `10.0.2.2` works: Open browser in emulator and go to `http://10.0.2.2/MockTest/api/test_connection.php`

### Issue: Can't connect on physical device
- ✅ Check phone and computer are on same WiFi
- ✅ Verify firewall isn't blocking port 80
- ✅ Test API in phone's browser: `http://YOUR_IP/MockTest/api/test_connection.php`
- ✅ Make sure you updated the IP address in `api_service.dart`

### Issue: "Cleartext HTTP traffic not permitted"
- ✅ Already fixed! `usesCleartextTraffic="true"` is in AndroidManifest
- ✅ Rebuild the app: `flutter clean && flutter pub get`

## 📝 Notes

- **10.0.2.2** is a special IP address that Android emulator uses to access the host machine's localhost
- For production, you should use HTTPS and a proper domain name
- This fix allows HTTP for development purposes only

## ✅ Status

- ✅ Android Emulator: **Working**
- ✅ Android Physical Device: **Requires IP configuration** (see above)
- ✅ Web: **Working**
- ✅ iOS Simulator: **Working**

---

**Last Updated:** Fixed for Android Studio development
**Version:** 1.0

