# TNPSC Mock Test - Admin Panel (Mockup)

A complete admin panel mockup designed to match your Flutter app, featuring all necessary CRUD operations.

## 🎯 Features

### ✅ Dashboard
- Statistics cards (Users, Exams, Questions, Tests)
- Recent activity feed
- Modern, clean interface

### ✅ Users Management (Full CRUD)
- **Create** - Add new users with all details
- **Read** - View all users in a table
- **Update** - Edit user information
- **Delete** - Remove users with confirmation

### ✅ Exams Management (Full CRUD)
- **Create** - Add new exams
- **Read** - View all exams
- **Update** - Edit exam details
- **Delete** - Remove exams

### ✅ Questions Management (Full CRUD)
- **Create** - Add questions with multiple-choice options
- **Read** - View all questions
- **Update** - Edit question details
- **Delete** - Remove questions
- Supports Tamil & English languages

### ✅ Categories Management (Full CRUD)
- **Create** - Add test categories
- **Read** - View categories in grid layout
- **Update** - Edit category details
- **Delete** - Remove categories
- Custom icons and colors

### ✅ Results Management
- View test results
- Filter and search
- Delete results

## 📁 File Structure

```
admin/
├── index.html          # Main admin panel page
├── css/
│   └── style.css      # All styles (~600 lines)
├── js/
│   └── script.js      # All functionality (~500 lines)
└── README.md          # This file
```

## 🚀 How to Use

### Open the Admin Panel

1. **Direct File:**
   - Double-click `admin/index.html`
   - OR
   - Open in browser: `file:///path/to/admin/index.html`

2. **Using XAMPP:**
   - `http://localhost/MockTest/admin/`

### Navigation

- **Dashboard** - Overview and statistics
- **Users** - Manage user accounts
- **Exams** - Manage exam definitions
- **Questions** - Manage question bank
- **Categories** - Manage test categories
- **Results** - View test results

### CRUD Operations

#### Add New Item
1. Click page (e.g., Users)
2. Click "+ Add" button
3. Fill in form
4. Click "Save"

#### Edit Item
1. Find item in table/grid
2. Click edit icon (✏️)
3. Update form
4. Click "Save"

#### Delete Item
1. Find item in table/grid
2. Click delete icon (🗑️)
3. Confirm deletion

#### View Item
1. Find item in table
2. Click view icon (👁️)
3. See details

## 🎨 Design Features

### Colors
- **Primary:** #6C63FF (Purple) - Matches Flutter app
- **Secondary:** #4ECDC4 (Teal)
- **Success:** #4CAF50 (Green)
- **Danger:** #FF6B6B (Red)
- **Warning:** #FFD93D (Yellow)

### Components
- ✅ Gradient sidebar navigation
- ✅ Responsive layout
- ✅ Modal forms
- ✅ Data tables
- ✅ Grid layouts
- ✅ Toast notifications
- ✅ Status badges
- ✅ Action buttons

### Responsive Design
- Desktop (> 768px) - Full sidebar
- Mobile (< 768px) - Collapsible sidebar

## 📊 Sample Data Included

The mockup includes sample data for:
- **3 Users** - With different exams and languages
- **3 Exams** - Different categories and durations
- **3 Questions** - English and Tamil examples
- **3 Categories** - With icons and colors
- **3 Results** - Pass and fail examples

## 🔧 Mockup Notes

### What Works:
✅ All UI interactions
✅ Form validations
✅ CRUD operations (in-memory)
✅ Modal dialogs
✅ Notifications
✅ Responsive design
✅ Navigation

### What's Simulated:
⚠️ Data storage (no backend)
⚠️ Changes reset on page reload
⚠️ No database connection
⚠️ No authentication

## 🔌 Converting to Production

To connect this to your Flutter app backend:

### 1. Add Backend API
```javascript
// Replace mock functions with API calls
async function saveUser() {
    const userData = {
        name: document.getElementById('userName').value,
        email: document.getElementById('userEmail').value,
        // ... other fields
    };
    
    const response = await fetch('/api/users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userData)
    });
    
    const result = await response.json();
    // Handle response
}
```

### 2. Load Data from API
```javascript
async function loadUsers() {
    const response = await fetch('/api/users');
    const users = await response.json();
    // Display users
}
```

### 3. Add Authentication
```javascript
// Add login check
// Store JWT token
// Include token in API requests
```

## 📱 Features Matching Flutter App

### Based on Your Flutter App:
✅ Language support (Tamil & English)
✅ Exam types (Group 1, 2, 4, VAO, Police)
✅ Test categories
✅ Question management
✅ User profiles
✅ Test results
✅ Color scheme matches

### Admin Panel Additions:
✅ CRUD operations for all entities
✅ Dashboard with statistics
✅ Activity tracking
✅ Bulk management
✅ Search and filter (ready)

## 🎯 Key Functions

### Users
- `loadUsers()` - Display all users
- `saveUser()` - Add/update user
- `deleteUser(id)` - Remove user
- `viewUser(id)` - Show user details

### Exams
- `loadExams()` - Display all exams
- `saveExam()` - Add/update exam
- `deleteExam(id)` - Remove exam

### Questions
- `loadQuestions()` - Display all questions
- `saveQuestion()` - Add/update question
- `deleteQuestion(id)` - Remove question
- `viewQuestion(id)` - Show question details

### Categories
- `loadCategories()` - Display categories
- `saveCategory()` - Add/update category
- `deleteCategory(id)` - Remove category

### Modals
- `openModal(id)` - Show modal
- `closeModal(id)` - Hide modal

### Notifications
- `showNotification(message, type)` - Display toast

## 🛠️ Customization

### Change Colors
Edit `css/style.css`:
```css
:root {
    --primary: #6C63FF;    /* Your primary color */
    --secondary: #4ECDC4;  /* Your secondary color */
}
```

### Add New Fields
1. Add input in HTML modal
2. Update save function in JS
3. Update display function in JS

### Add New Page
1. Add nav item in sidebar
2. Create page div
3. Add show/hide logic

## 📞 Browser Support

- ✅ Chrome (Recommended)
- ✅ Firefox
- ✅ Safari
- ✅ Edge
- ✅ Mobile browsers

## 🔒 Security Notes

For production:
- Add authentication
- Validate all inputs
- Sanitize data
- Use HTTPS
- Implement CSRF protection
- Add rate limiting

## ✨ Tips

1. **Testing CRUD:**
   - Add items to see them in tables
   - Edit to update values
   - Delete removes from list
   - All changes are temporary (refresh resets)

2. **Responsive Testing:**
   - Resize browser window
   - Test on mobile devices
   - Sidebar collapses on small screens

3. **Data Persistence:**
   - Changes lost on refresh
   - Connect to backend for real persistence

## 📝 License

This admin panel is part of the TNPSC Mock Test project.

---

**Note:** This is a mockup design for demonstration. All data is stored in memory and resets on page reload. For production use, connect to a backend API and database.

## 🎉 Enjoy Your Admin Panel!

Your complete admin panel is ready with:
- ✅ Full CRUD operations
- ✅ Modern, responsive design
- ✅ Matches Flutter app design
- ✅ Easy to use and customize

**Location:** `admin/index.html`

**Just open and start managing!** 🚀

