# 📚 Test Categories & Application Flow Explanation

## 🏗️ Database Structure Hierarchy

The application follows a **4-level hierarchy** for organizing tests:

```
1. Exam Categories (Top Level)
   └── 2. Test Categories (Subject/Topic Level)
       └── 3. Question Sessions (Individual Tests)
           └── 4. Questions (Actual Questions)
```

---

## 📊 Level 1: Exam Categories (`exam_categories`)

**Purpose:** Represents the main exam types (e.g., TNPSC Group 1, Group 2, Group 4)

**Database Table:** `exam_categories`
- `id` - Unique identifier
- `name` - Exam name (e.g., "TNPSC Group 4")
- `description` - Exam description
- `icon` - Icon identifier
- `is_active` - Whether exam is active
- `display_order` - Order for display

**Example Data:**
- TNPSC Group 1
- TNPSC Group 2
- TNPSC Group 4
- TNPSC VAO
- TNUSRB

**API Endpoint:** `/api/tests/get_exam_categories.php`
- Returns all active exam categories
- Used in: Exam Selection Page, Home Page dropdown

---

## 📖 Level 2: Test Categories (`test_categories`)

**Purpose:** Represents subject/topic categories within an exam (e.g., Tamil Language, General Science)

**Database Table:** `test_categories`
- `id` - Unique identifier
- `exam_category_id` - **Links to Exam Category** (Foreign Key)
- `name` - Category name (e.g., "Tamil Language")
- `description` - Category description
- `icon` - Icon identifier
- `color` - Display color (hex code)
- `is_active` - Whether category is active
- `display_order` - Order for display

**Example Data (for TNPSC Group 4):**
- Tamil Language (exam_category_id: 3)
- General English (exam_category_id: 3)
- General Science (exam_category_id: 3)
- Current Affairs (exam_category_id: 3)
- Aptitude & Mental Ability (exam_category_id: 3)

**API Endpoint:** `/api/tests/get_categories.php`
- Optional parameter: `exam_id` - Filter by exam category
- Returns all test categories (optionally filtered by exam)
- Used in: Home Page (shows categories), Tests Page (tabs)

**Relationship:**
- One Exam Category → Many Test Categories
- Example: "TNPSC Group 4" has multiple test categories like "Tamil", "English", "Science"

---

## 📝 Level 3: Question Sessions (`question_sessions`)

**Purpose:** Represents individual test sessions/exams within a test category

**Database Table:** `question_sessions`
- `id` - Unique identifier
- `test_category_id` - **Links to Test Category** (Foreign Key)
- `name` - Session name (e.g., "Tamil Basics - Session 1")
- `description` - Session description
- `total_questions` - Expected number of questions
- `duration` - Test duration in minutes
- `difficulty` - Difficulty level (easy/medium/hard)
- `is_active` - Whether session is active

**Example Data (for Tamil Language category):**
- "Tamil Basics - Session 1" (10 questions, 15 min, easy)
- "Tamil Grammar - Session 2" (15 questions, 20 min, medium)
- "Tamil Literature - Session 3" (20 questions, 25 min, hard)

**API Endpoint:** `/api/tests/get_sessions.php`
- Optional parameter: `category_id` - Filter by test category
- Returns all question sessions (optionally filtered by category)
- Used in: Tests Page (shows individual tests)

**Relationship:**
- One Test Category → Many Question Sessions
- Example: "Tamil Language" category has multiple sessions like "Session 1", "Session 2"

---

## ❓ Level 4: Questions (`questions`)

**Purpose:** Individual questions within a question session

**Database Table:** `questions`
- `id` - Unique identifier
- `session_id` - **Links to Question Session** (Foreign Key)
- `question_en` / `question_ta` - Question text (English/Tamil)
- `option_a_en` / `option_a_ta` - Option A (English/Tamil)
- `option_b_en` / `option_b_ta` - Option B (English/Tamil)
- `option_c_en` / `option_c_ta` - Option C (English/Tamil)
- `option_d_en` / `option_d_ta` - Option D (English/Tamil)
- `correct_answer` - Correct answer (A/B/C/D)
- `explanation_en` / `explanation_ta` - Explanation (English/Tamil)
- `difficulty` - Question difficulty
- `marks` - Marks for correct answer
- `negative_marks` - Negative marks for wrong answer
- `display_order` - Order in test

**API Endpoint:** `/api/tests/get_questions.php`
- Required parameter: `session_id` - Get questions for specific session
- Optional parameter: `language` - Language preference (en/ta)
- Returns all questions for a session in requested language
- Used in: Test Page (when user starts a test)

**Relationship:**
- One Question Session → Many Questions
- Example: "Tamil Basics - Session 1" has 10 questions

---

## 🔄 Complete User Flow

### **Step 1: User Login/Registration**
1. User enters mobile number
2. OTP sent (default: 111111 for testing)
3. User verifies OTP
4. User profile created/loaded

### **Step 2: Exam Selection**
1. User sees list of **Exam Categories** from API
   - API: `GET /api/tests/get_exam_categories.php`
2. User selects an exam (e.g., "TNPSC Group 4")
3. Selection saved to SharedPreferences
4. User navigates to Home Page

### **Step 3: Home Page**
1. **Top Dropdown** shows selected exam
   - Can change exam anytime
   - Fetches from: `GET /api/tests/get_exam_categories.php`
2. **Test Categories Section** shows categories
   - API: `GET /api/tests/get_categories.php?exam_id=X`
   - Shows categories like "Tamil Language", "General English"
   - Displays progress (completed/total sessions)

### **Step 4: Tests Page**
1. User clicks "Tests" tab or "View All" button
2. **Tabs** show all **Test Categories**
   - API: `GET /api/tests/get_categories.php`
   - Creates tabs: "All", "Tamil Language", "General English", etc.
3. **Test List** shows **Question Sessions**
   - API: `GET /api/tests/get_sessions.php`
   - Filtered by selected category tab
   - Shows: Test name, description, difficulty, questions count, duration

### **Step 5: Start Test**
1. User clicks on a **Question Session** (e.g., "Tamil Basics - Session 1")
2. App navigates to Test Page
3. **Questions Loaded**
   - API: `GET /api/tests/get_questions.php?session_id=X&language=en`
   - Returns all questions for that session
4. User answers questions
5. Test submitted

### **Step 6: Test Results**
1. Results saved to `test_results` table
2. Individual answers saved to `user_answers` table
3. User can view results in:
   - Test History Page
   - Performance Page
   - Progress Page

---

## 📱 UI Components & Their Data Sources

### **Home Page**
- **Exam Dropdown:** `exam_categories` table
- **Test Categories Cards:** `test_categories` table (filtered by selected exam)
- **Recent Tests:** `test_results` table (user's test history)

### **Tests Page**
- **Category Tabs:** `test_categories` table
- **Test Cards:** `question_sessions` table (filtered by category)
- Each card shows: name, description, difficulty, question count, duration

### **Test Page**
- **Questions:** `questions` table (filtered by session_id)
- Shows questions in selected language (English/Tamil)
- User answers saved to `user_answers` table

---

## 🔗 Key Relationships

```
Exam Category (1) ──→ (Many) Test Categories
    │
    └── Example: "TNPSC Group 4" has "Tamil", "English", "Science"

Test Category (1) ──→ (Many) Question Sessions
    │
    └── Example: "Tamil Language" has "Session 1", "Session 2"

Question Session (1) ──→ (Many) Questions
    │
    └── Example: "Tamil Basics - Session 1" has 10 questions
```

---

## 🎯 Filtering Flow

### **By Exam Category:**
```
User selects "TNPSC Group 4"
    ↓
API: get_categories.php?exam_id=3
    ↓
Returns only test categories for TNPSC Group 4
    ↓
Shows: Tamil, English, Science, etc.
```

### **By Test Category:**
```
User selects "Tamil Language" tab
    ↓
API: get_sessions.php?category_id=1
    ↓
Returns only question sessions for Tamil Language
    ↓
Shows: "Tamil Basics - Session 1", "Tamil Grammar - Session 2", etc.
```

### **By Question Session:**
```
User clicks "Tamil Basics - Session 1"
    ↓
API: get_questions.php?session_id=1&language=en
    ↓
Returns all questions for that session
    ↓
Shows: 10 questions in English
```

---

## 📊 Data Flow Summary

```
1. Admin creates Exam Category
   └── "TNPSC Group 4"

2. Admin creates Test Categories under that exam
   └── "Tamil Language", "General English", etc.

3. Admin creates Question Sessions under each test category
   └── "Tamil Basics - Session 1", "Tamil Grammar - Session 2"

4. Admin adds Questions to each session
   └── 10 questions per session

5. User selects exam → sees test categories
6. User selects category → sees question sessions
7. User selects session → takes test with questions
8. Results saved → user can review performance
```

---

## 🛠️ Admin Panel Flow

### **Creating Content:**
1. **Exam Categories** → Admin Panel → Exam Categories → Add
2. **Test Categories** → Admin Panel → Test Categories → Add (select exam)
3. **Question Sessions** → Admin Panel → Question Sessions → Add (select test category)
4. **Questions** → Admin Panel → Add Question → Select session → Add questions

### **Viewing Content:**
- **Dashboard:** Overview of all data
- **Exam Categories:** List all exams
- **Test Categories:** List all test categories (can filter by exam)
- **Question Sessions:** List all sessions (can filter by category)
- **Test Results:** View all user test results

---

## 💡 Key Points

1. **Hierarchical Structure:** Exam → Test Category → Session → Questions
2. **Filtering:** Each level filters the next level
3. **Language Support:** Questions support both English and Tamil
4. **Flexibility:** One exam can have many categories, one category many sessions
5. **Scalability:** Easy to add new exams, categories, sessions, and questions

---

## 🔍 API Endpoints Summary

| Endpoint | Purpose | Parameters |
|----------|---------|------------|
| `/api/tests/get_exam_categories.php` | Get all exam categories | None |
| `/api/tests/get_categories.php` | Get test categories | `exam_id` (optional) |
| `/api/tests/get_sessions.php` | Get question sessions | `category_id` (optional) |
| `/api/tests/get_questions.php` | Get questions for session | `session_id` (required), `language` (optional) |

---

**This structure allows for flexible organization and easy management of mock tests!** 🎓

