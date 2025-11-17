# 🔥 Quick Firewall Fix for Physical Device

## ⚠️ Problem
The API is not accessible from your phone because Windows Firewall is blocking Apache.

## ✅ Quick Fix (Choose One)

### Method 1: Allow Apache Through Firewall (Recommended)

1. **Open Windows Defender Firewall:**
   - Press `Win + R`
   - Type: `firewall.cpl`
   - Press Enter

2. **Allow Apache:**
   - Click "Allow an app or feature through Windows Defender Firewall"
   - Click "Change settings" (if needed)
   - Click "Allow another app..."
   - Click "Browse"
   - Navigate to: `C:\xampp\apache\bin\`
   - Select `httpd.exe`
   - Click "Add"
   - Check both "Private" and "Public" boxes
   - Click "OK"

3. **Restart Apache:**
   - Open XAMPP Control Panel
   - Stop Apache
   - Start Apache

### Method 2: Create Firewall Rule via Command (Fastest)

Run this in **Administrator Command Prompt**:

```cmd
netsh advfirewall firewall add rule name="Apache HTTP Server" dir=in action=allow protocol=TCP localport=80
```

### Method 3: Temporarily Disable Firewall (Testing Only)

1. Open Windows Defender Firewall
2. Click "Turn Windows Defender Firewall on or off"
3. Turn off for "Private network settings" (temporarily)
4. Test your app
5. **Re-enable firewall after testing!**

## 🧪 Test After Fix

**On your phone's browser, open:**
```
http://192.168.1.4/MockTest/api/test_connection.php
```

If you see JSON response, the firewall is fixed! ✅

## 📱 Then Test App

1. Rebuild app in Android Studio
2. Run on physical device
3. Try login - should work now!

---

**Note**: Method 1 is recommended for permanent solution. Method 2 is fastest for quick testing.

