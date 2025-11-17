# 🔥 Flutter Hot Reload Guide

## Understanding Hot Reload vs Refresh

### **Hot Reload (Recommended)**
- **Press `r` in terminal** while app is running
- Updates code changes instantly
- Preserves app state
- Works for most UI changes

### **Hot Restart**
- **Press `R` (capital) in terminal**
- Restarts the app but keeps it running
- Loses app state
- Needed for some changes (routes, initState changes)

### **Full Restart**
- **Press `q` to quit, then run again**
- Complete restart
- Needed for major changes

### **Browser Refresh (F5)**
- **Does NOT hot reload**
- Reloads the entire app
- May show cached version
- Use terminal commands instead!

---

## ✅ **How to Use Hot Reload:**

1. **Start app:** `flutter run -d chrome`
2. **Make code changes**
3. **Press `r` in terminal** (not browser!)
4. **Changes appear instantly**

---

## ⚠️ **When Hot Reload Doesn't Work:**

- Changes to `main()` function
- Changes to `initState()` logic
- Adding/removing imports
- Changes to static variables
- Changes to enum values

**Solution:** Press `R` for hot restart, or `q` then restart

---

## 🚀 **Best Practice:**

1. Keep terminal open with `flutter run`
2. Make changes in editor
3. Press `r` in terminal (not browser F5!)
4. See changes instantly

**Remember:** Browser refresh (F5) is NOT hot reload!

