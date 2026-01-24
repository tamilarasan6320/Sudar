// API Configuration - Auto-detect based on current domain
function getApiBaseUrl() {
    const hostname = window.location.hostname;
    const protocol = window.location.protocol;
    
    // Local development
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return '../api';
    }
    
    // Production - use same protocol and hostname
    return `${protocol}//${hostname}/api`;
}

const API_BASE_URL = getApiBaseUrl();

// Data Storage (loaded from API)
let users = [];

let exams = [];
let questions = [];
let testCategories = [];
let questionSessions = [];
let examCategories = [];
let languages = [];
let results = [];

// Settings Data
let appSettings = {
    privacyPolicy: `Privacy Policy

Last Updated: October 2025

1. Information We Collect
We collect information that you provide directly to us, including:
- Name and contact information
- Email address and mobile number
- Exam preferences and test results
- Usage data and performance analytics

2. How We Use Your Information
We use the information we collect to:
- Provide and improve our mock test services
- Send you important updates and notifications
- Analyze and enhance user experience
- Ensure the security of our platform

3. Data Security
We implement appropriate security measures to protect your personal information against unauthorized access, alteration, disclosure, or destruction.

4. Your Rights
You have the right to:
- Access your personal data
- Request corrections to your data
- Delete your account and associated data
- Opt-out of communications

5. Contact Us
If you have questions about this Privacy Policy, please contact us at support@tnpscmocktest.com`,
    
    termsConditions: `Terms & Conditions

Last Updated: October 2025

1. Acceptance of Terms
By accessing and using the TNPSC Mock Test application, you accept and agree to be bound by the terms and conditions of this agreement.

2. User Accounts
- You must provide accurate and complete information
- You are responsible for maintaining account security
- You must be at least 16 years old to use this service

3. Use of Service
- The service is provided for personal, non-commercial use
- You may not reproduce, distribute, or create derivative works
- You may not attempt to gain unauthorized access

4. Test Content
- All test questions and materials are proprietary
- Content is provided for educational purposes only
- Mock tests are approximations and not official TNPSC exams

5. Limitation of Liability
We are not liable for any indirect, incidental, or consequential damages arising from your use of the service.

6. Changes to Terms
We reserve the right to modify these terms at any time. Continued use of the service constitutes acceptance of modified terms.

7. Governing Law
These terms are governed by the laws of India.

8. Contact Information
For questions about these terms, contact us at support@tnpscmocktest.com`
};

// User Rankings Data (will be generated from user performance)
let userRankings = [];

// Test Results Data (will be generated from users and sessions)
let testResults = [];

// Admin Authentication
let currentAdmin = null;

async function checkAdminAuth() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/auth/check.php`);
        const data = await response.json();
        
        if (data.success && data.authenticated) {
            currentAdmin = data.admin;
            updateAdminUI();
            return true;
        } else {
            // Not authenticated, redirect to login
            window.location.href = 'login.html';
            return false;
        }
    } catch (error) {
        console.error('Auth check error:', error);
        window.location.href = 'login.html';
        return false;
    }
}

function updateAdminUI() {
    if (currentAdmin) {
        const userProfile = document.querySelector('.user-profile span');
        if (userProfile) {
            userProfile.textContent = currentAdmin.username;
        }
        
        // Add logout button
        const headerRight = document.querySelector('.header-right');
        if (headerRight && !document.getElementById('logoutButton')) {
            const logoutBtn = document.createElement('button');
            logoutBtn.id = 'logoutButton';
            logoutBtn.className = 'logout-button';
            logoutBtn.innerHTML = '<i class="fas fa-sign-out-alt"></i> Logout';
            logoutBtn.onclick = handleLogout;
            headerRight.insertBefore(logoutBtn, headerRight.firstChild);
        }
    }
}

async function handleLogout() {
    if (confirm('Are you sure you want to logout?')) {
        try {
            const response = await fetch(`${API_BASE_URL}/admin/logout.php`, {
                method: 'POST'
            });
            const data = await response.json();
            
            if (data.success) {
                window.location.href = 'login.html';
            }
        } catch (error) {
            console.error('Logout error:', error);
            // Still redirect even if API call fails
            window.location.href = 'login.html';
        }
    }
}

// Initialize
document.addEventListener('DOMContentLoaded', async function() {
    // Check authentication first
    const isAuthenticated = await checkAdminAuth();
    
    if (isAuthenticated) {
        loadDashboard();
        loadUsers();
        loadExamCategories();
        loadTestCategories();
        loadQuestionSessions();
        loadSessionCards();
        loadSettings();
        populateExamCategoryDropdown();
        populateSessionDropdowns();
        // Rankings will be loaded when Rankings page is opened (not on initial load)
        loadTestResultsFromAPI();
        loadTestResultsAnalytics();
        // Removed for now - will add one by one
        // loadExams();
        // loadResults();
    }
});

// Navigation
function showPage(pageName) {
    // Hide all pages
    document.querySelectorAll('.page').forEach(page => page.classList.remove('active'));
    
    // Show selected page
    document.getElementById(pageName).classList.add('active');
    
    // Update nav items
    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    event.target.closest('.nav-item').classList.add('active');
    
    // Update title
    const titles = {
        'dashboard': 'Dashboard',
        'users': 'Users Management',
        'examCategories': 'Exam Categories',
        'testCategories': 'Test Categories',
        'questionSessions': 'Question Sessions',
        'addQuestion': 'Add Question',
        'testResults': 'Test Results',
        'languages': 'Languages',
        'rankings': 'User Rankings',
        'feedback': 'Feedback Management',
        'settings': 'Settings'
        // Removed sections - will add one by one
        // 'exams': 'Exams Management',
        // 'results': 'Test Results'
    };
    document.getElementById('pageTitle').textContent = titles[pageName] || pageName;
    
    // Load data based on page
    if (pageName === 'languages') {
        loadLanguages();
    } else if (pageName === 'testResults') {
        loadTestResultsFromAPI();
        loadTestResultsAnalytics();
    } else if (pageName === 'dashboard') {
        loadDashboard();
    } else if (pageName === 'rankings') {
        loadRankings();
    } else if (pageName === 'feedback') {
        loadFeedback();
    }
}

// Toggle Sidebar
function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('active');
}

// Dashboard
async function loadDashboard() {
    try {
        // Fetch enhanced analytics from API
        const period = document.getElementById('analyticsPeriod')?.value || 'week';
        const response = await fetch(`${API_BASE_URL}/admin/analytics/dashboard.php?period=${period}`);
        const data = await response.json();
        
        if (data.success) {
            const overview = data.overview || {};
            
            // Update stats cards
            document.getElementById('totalUsers').textContent = overview.total_users || 0;
            document.getElementById('totalExams').textContent = overview.total_exams || 0;
            document.getElementById('totalQuestions').textContent = overview.total_questions || 0;
            document.getElementById('testsTaken').textContent = overview.total_tests || 0;
            
            // Update additional stats if elements exist
            if (document.getElementById('activeUsers')) {
                document.getElementById('activeUsers').textContent = overview.active_users || 0;
            }
            if (document.getElementById('avgScore')) {
                document.getElementById('avgScore').textContent = (overview.avg_score || 0) + '%';
            }
            if (document.getElementById('passRate')) {
                document.getElementById('passRate').textContent = (overview.pass_rate || 0) + '%';
            }
            
            // Load charts data if available
            if (data.user_activity && data.user_activity.length > 0) {
                // User activity chart data available
                console.log('User activity data:', data.user_activity);
            }
            
            if (data.test_performance && data.test_performance.length > 0) {
                // Performance trend data available
                console.log('Performance trend data:', data.test_performance);
            }
            
            if (data.top_performers && data.top_performers.length > 0) {
                // Top performers data available
                console.log('Top performers:', data.top_performers);
            }
            
            // Load recent activity from API data
            const activities = [];
            
            // Add recent user registrations
            if (data.recent_users && data.recent_users.length > 0) {
                const latestUser = data.recent_users[0];
                const timeAgo = getTimeAgo(latestUser.created_at);
                activities.push({
                    icon: 'fa-user-plus',
                    color: '#6C63FF',
                    title: 'New user registered',
                    subtitle: `${latestUser.name} joined TNPSC Group 4`,
                    time: timeAgo
                });
            }
            
            // Add recent test completions
            if (data.recent_results && data.recent_results.length > 0) {
                const latestResult = data.recent_results[0];
                const timeAgo = getTimeAgo(latestResult.submitted_at);
                activities.push({
                    icon: 'fa-check-circle',
                    color: '#4ECDC4',
                    title: 'Test completed',
                    subtitle: `${latestResult.user_name} completed ${latestResult.test_name}`,
                    time: timeAgo
                });
            }
            
            // Add general info
            if (overview.total_questions > 0) {
                activities.push({
                    icon: 'fa-plus-circle',
                    color: '#FFD93D',
                    title: 'Questions available',
                    subtitle: `${overview.total_questions} questions in question bank`,
                    time: 'Updated recently'
                });
            }
            
            // Add recent activity from enhanced analytics
            if (data.recent_activity && data.recent_activity.length > 0) {
                data.recent_activity.slice(0, 3).forEach(activity => {
                    const timeAgo = getTimeAgo(activity.submitted_at);
                    activities.push({
                        icon: 'fa-check-circle',
                        color: activity.score >= 50 ? '#4ECDC4' : '#FF6B6B',
                        title: 'Test completed',
                        subtitle: `${activity.user_name} scored ${activity.score}% in ${activity.test_name}`,
                        time: timeAgo
                    });
                });
            }
            
            // Render activities
            const activityList = document.getElementById('activityList');
            if (activities.length > 0) {
                activityList.innerHTML = activities.map(activity => `
                    <div class="activity-item">
                        <div class="activity-icon" style="background: ${activity.color};">
                            <i class="fas ${activity.icon}"></i>
                        </div>
                        <div class="activity-details">
                            <strong>${activity.title}</strong>
                            <span>${activity.subtitle}</span><br>
                            <small style="color: #95a5a6;">${activity.time}</small>
                        </div>
                    </div>
                `).join('');
            } else {
                activityList.innerHTML = `
                    <div class="activity-item">
                        <div class="activity-icon" style="background: #6C63FF;">
                            <i class="fas fa-info-circle"></i>
                        </div>
                        <div class="activity-details">
                            <strong>Welcome to TNPSC Admin Panel</strong>
                            <span>Start by adding exam categories and questions</span>
                        </div>
                    </div>
                `;
            }
            
            console.log('Dashboard loaded successfully:', overview);
        } else {
            console.error('Failed to load dashboard:', data.message);
            showError('Failed to load dashboard statistics');
        }
    } catch (error) {
        console.error('Error loading dashboard:', error);
        showError('Error connecting to database. Please check your connection.');
    }
}

// Helper function to calculate time ago
function getTimeAgo(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);
    
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return Math.floor(seconds / 60) + ' minutes ago';
    if (seconds < 86400) return Math.floor(seconds / 3600) + ' hours ago';
    if (seconds < 604800) return Math.floor(seconds / 86400) + ' days ago';
    return date.toLocaleDateString();
}

// Helper function to show errors
function showError(message) {
    // You can implement a toast notification here
    console.error(message);
}

// Users CRUD
async function loadUsers() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/users/list.php`);
        const data = await response.json();
        
        if (data.success) {
            users = data.users;
    const tbody = document.getElementById('usersTableBody');
    tbody.innerHTML = users.map(user => {
        // Determine verification badge
        const verificationMethod = user.verification_method || 'otp';
        const isTruecaller = verificationMethod.toLowerCase() === 'truecaller';
        const verificationBadge = isTruecaller 
            ? '<span class="badge" style="background: linear-gradient(135deg, #0077B5 0%, #00a0dc 100%); color: white; padding: 4px 8px; border-radius: 12px; font-size: 11px;"><i class="fas fa-phone-alt"></i> Truecaller</span>'
            : '<span class="badge" style="background: linear-gradient(135deg, #4CAF50 0%, #66BB6A 100%); color: white; padding: 4px 8px; border-radius: 12px; font-size: 11px;"><i class="fas fa-sms"></i> OTP</span>';
        
        return `
        <tr>
            <td>#U${user.id.toString().padStart(3, '0')}</td>
            <td>${user.name}</td>
            <td>${user.email || 'N/A'}</td>
            <td>${user.mobile || 'N/A'}</td>
            <td>${verificationBadge}</td>
            <td>${user.language === 'en' ? 'English' : 'Tamil'}</td>
            <td><span class="badge badge-${user.is_active ? 'success' : 'danger'}">${user.is_active ? 'active' : 'inactive'}</span></td>
            <td>
                <button class="btn-icon btn-view" onclick="viewUser(${user.id})"><i class="fas fa-eye"></i></button>
                <button class="btn-icon btn-edit" onclick="editUser(${user.id})"><i class="fas fa-edit"></i></button>
                <button class="btn-icon btn-delete" onclick="deleteUser(${user.id})"><i class="fas fa-trash"></i></button>
            </td>
        </tr>
    `;
    }).join('');
            
            // Update dashboard stats
            document.getElementById('totalUsers').textContent = data.total || users.length;
        } else {
            console.error('Failed to load users:', data.message);
        }
    } catch (error) {
        console.error('Error loading users:', error);
        showNotification('Failed to load users', 'error');
    }
}

async function saveUser() {
    const form = document.getElementById('userForm');
    const isEditMode = form.dataset.editMode === 'true';
    const userId = form.dataset.userId;
    
    const userData = {
        name: document.getElementById('userName').value,
        email: document.getElementById('userEmail').value,
        mobile: document.getElementById('userMobile').value,
        district: document.getElementById('userDistrict')?.value || '',
        education: document.getElementById('userEducation')?.value || '',
        age: document.getElementById('userAge')?.value || null,
        language: document.getElementById('userLanguage').value
    };
    
    // If editing, add the user ID
    if (isEditMode) {
        userData.id = userId;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/users/crud.php`, {
            method: isEditMode ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(userData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            loadUsers();
            closeModal('userModal');
            showNotification(isEditMode ? 'User updated successfully!' : 'User added successfully!', 'success');
            form.reset();
            delete form.dataset.userId;
            delete form.dataset.editMode;
        } else {
            showNotification(data.message || `Failed to ${isEditMode ? 'update' : 'add'} user`, 'error');
        }
    } catch (error) {
        console.error('Error saving user:', error);
        showNotification('Error saving user', 'error');
    }
}

function viewUser(id) {
    const user = users.find(u => u.id === id);
    
    // Generate comprehensive mock data for this user
    const testsTaken = Math.floor(Math.random() * 50) + 25;
    const avgScore = (Math.random() * 30 + 60).toFixed(1);
    const rank = Math.floor(Math.random() * 500) + 1;
    const studyTime = Math.floor(Math.random() * 100) + 50;
    const currentStreak = Math.floor(Math.random() * 15) + 1;
    const longestStreak = Math.floor(Math.random() * 20) + currentStreak;
    
    // Generate comprehensive test history (last 15 tests)
    const testHistory = [
        { name: 'Tamil Language - Practice Test 1', category: 'Tamil', score: 85, total: 100, percentage: 85, time: '58 mins', date: 'Oct 21, 2025', status: 'passed' },
        { name: 'General Science - Module 5', category: 'Science', score: 72, total: 100, percentage: 72, time: '62 mins', date: 'Oct 20, 2025', status: 'passed' },
        { name: 'Current Affairs - October 2025', category: 'Current Affairs', score: 78, total: 100, percentage: 78, time: '45 mins', date: 'Oct 19, 2025', status: 'passed' },
        { name: 'Aptitude & Reasoning - Set 3', category: 'Aptitude', score: 91, total: 100, percentage: 91, time: '55 mins', date: 'Oct 18, 2025', status: 'passed' },
        { name: 'History - Tamil Nadu', category: 'History', score: 68, total: 100, percentage: 68, time: '70 mins', date: 'Oct 17, 2025', status: 'passed' },
        { name: 'General Knowledge - Mock Test 12', category: 'GK', score: 82, total: 100, percentage: 82, time: '60 mins', date: 'Oct 16, 2025', status: 'passed' },
        { name: 'Tamil Grammar - Advanced', category: 'Tamil', score: 76, total: 100, percentage: 76, time: '65 mins', date: 'Oct 15, 2025', status: 'passed' },
        { name: 'Geography - India & TN', category: 'Geography', score: 88, total: 100, percentage: 88, time: '52 mins', date: 'Oct 14, 2025', status: 'passed' },
        { name: 'Polity & Constitution', category: 'Polity', score: 45, total: 100, percentage: 45, time: '75 mins', date: 'Oct 13, 2025', status: 'failed' },
        { name: 'Economics - Basics', category: 'Economics', score: 79, total: 100, percentage: 79, time: '68 mins', date: 'Oct 12, 2025', status: 'passed' },
        { name: 'Tamil Literature - Sangam', category: 'Tamil', score: 92, total: 100, percentage: 92, time: '48 mins', date: 'Oct 11, 2025', status: 'passed' },
        { name: 'Physics & Chemistry', category: 'Science', score: 71, total: 100, percentage: 71, time: '66 mins', date: 'Oct 10, 2025', status: 'passed' },
        { name: 'Mental Ability - Set 5', category: 'Aptitude', score: 86, total: 100, percentage: 86, time: '42 mins', date: 'Oct 9, 2025', status: 'passed' },
        { name: 'Indian History - Freedom Movement', category: 'History', score: 74, total: 100, percentage: 74, time: '72 mins', date: 'Oct 8, 2025', status: 'passed' },
        { name: 'TNPSC Group 4 - Full Mock Test', category: 'Full Test', score: 81, total: 100, percentage: 81, time: '120 mins', date: 'Oct 7, 2025', status: 'passed' }
    ];
    
    // Subject-wise performance
    const subjectPerformance = [
        { subject: 'Tamil Language', tests: 12, avgScore: 84, accuracy: 84, bestScore: 95, weakTopic: 'Grammar' },
        { subject: 'General Science', tests: 15, avgScore: 72, accuracy: 72, bestScore: 89, weakTopic: 'Physics' },
        { subject: 'Current Affairs', tests: 18, avgScore: 76, accuracy: 76, bestScore: 88, weakTopic: 'International' },
        { subject: 'Aptitude & Reasoning', tests: 10, avgScore: 88, accuracy: 88, bestScore: 95, weakTopic: 'Data Interpretation' },
        { subject: 'History', tests: 8, avgScore: 68, accuracy: 68, bestScore: 82, weakTopic: 'Medieval India' },
        { subject: 'Geography', tests: 7, avgScore: 80, accuracy: 80, bestScore: 90, weakTopic: 'World Geography' }
    ];
    
    // Create a detailed modal view with tabs
    const modalHTML = `
        <div id="viewUserModal" class="modal active" style="overflow-y: auto;">
            <div class="modal-content modal-large" style="max-width: 1200px; max-height: 95vh; overflow-y: auto;">
                <div class="modal-header" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 25px;">
                    <div>
                        <h2 style="margin: 0; color: white;"><i class="fas fa-user-circle"></i> ${user.name}</h2>
                        <p style="margin: 5px 0 0 0; opacity: 0.9;">${user.email} | ${user.mobile || 'No mobile'}</p>
                </div>
                    <span class="close" onclick="closeViewUserModal()" style="color: white; opacity: 0.9;">&times;</span>
                </div>
                
                <div class="modal-body" style="padding: 0;">
                    <!-- Tabs -->
                    <div style="display: flex; border-bottom: 2px solid #e0e0e0; background: #f8f9fa; padding: 0 20px;">
                        <button class="user-detail-tab active" onclick="switchUserTab('overview')" id="overviewTab" style="flex: 1; padding: 15px; border: none; background: none; cursor: pointer; font-weight: 600; border-bottom: 3px solid #6C63FF; transition: all 0.3s;">
                            <i class="fas fa-th-large"></i> Overview
                        </button>
                        <button class="user-detail-tab" onclick="switchUserTab('performance')" id="performanceTab" style="flex: 1; padding: 15px; border: none; background: none; cursor: pointer; font-weight: 600; border-bottom: 3px solid transparent; transition: all 0.3s;">
                            <i class="fas fa-chart-line"></i> Performance
                        </button>
                        <button class="user-detail-tab" onclick="switchUserTab('history')" id="historyTab" style="flex: 1; padding: 15px; border: none; background: none; cursor: pointer; font-weight: 600; border-bottom: 3px solid transparent; transition: all 0.3s;">
                            <i class="fas fa-history"></i> Test History
                        </button>
                        <button class="user-detail-tab" onclick="switchUserTab('analytics')" id="analyticsTab" style="flex: 1; padding: 15px; border: none; background: none; cursor: pointer; font-weight: 600; border-bottom: 3px solid transparent; transition: all 0.3s;">
                            <i class="fas fa-brain"></i> Analytics
                        </button>
                    </div>
                    
                    <!-- Tab Content -->
                    <div style="padding: 25px;">
                        <!-- Overview Tab -->
                        <div id="overviewContent" class="user-tab-content">
                            <div class="user-details-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 15px; margin-bottom: 25px;">
                                <div class="detail-card" style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); display: flex; align-items: center; gap: 15px;">
                                    <div class="detail-icon" style="background: #6C63FF; width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px;">
                                <i class="fas fa-user"></i>
                            </div>
                            <div class="detail-info">
                                        <label style="font-size: 12px; color: #999; margin-bottom: 3px; display: block;">Full Name</label>
                                        <strong style="font-size: 16px; color: #333;">${user.name}</strong>
                            </div>
                        </div>
                        
                                <div class="detail-card" style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); display: flex; align-items: center; gap: 15px;">
                                    <div class="detail-icon" style="background: #4ECDC4; width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px;">
                                <i class="fas fa-envelope"></i>
                            </div>
                            <div class="detail-info">
                                        <label style="font-size: 12px; color: #999; margin-bottom: 3px; display: block;">Email Address</label>
                                        <strong style="font-size: 14px; color: #333;">${user.email}</strong>
                            </div>
                        </div>
                        
                                <div class="detail-card" style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); display: flex; align-items: center; gap: 15px;">
                                    <div class="detail-icon" style="background: #FF6B6B; width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px;">
                                <i class="fas fa-phone"></i>
                            </div>
                            <div class="detail-info">
                                        <label style="font-size: 12px; color: #999; margin-bottom: 3px; display: block;">Mobile Number</label>
                                        <strong style="font-size: 16px; color: #333;">${user.mobile || 'Not Provided'}</strong>
                            </div>
                        </div>
                        
                                <div class="detail-card" style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); display: flex; align-items: center; gap: 15px;">
                                    <div class="detail-icon" style="background: #E74C3C; width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px;">
                                <i class="fas fa-book"></i>
                            </div>
                            <div class="detail-info">
                                        <label style="font-size: 12px; color: #999; margin-bottom: 3px; display: block;">Selected Exam</label>
                                        <strong style="font-size: 16px; color: #333;">${user.exam}</strong>
                            </div>
                        </div>
                        
                                <div class="detail-card" style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); display: flex; align-items: center; gap: 15px;">
                                    <div class="detail-icon" style="background: #FFD93D; width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px;">
                                <i class="fas fa-language"></i>
                            </div>
                            <div class="detail-info">
                                        <label style="font-size: 12px; color: #999; margin-bottom: 3px; display: block;">Preferred Language</label>
                                        <strong style="font-size: 16px; color: #333;">${user.language === 'en' ? 'English' : 'தமிழ் (Tamil)'}</strong>
                            </div>
                        </div>
                        
                                <div class="detail-card" style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); display: flex; align-items: center; gap: 15px;">
                                    <div class="detail-icon" style="background: ${user.status === 'active' ? '#4CAF50' : '#95a5a6'}; width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px;">
                                <i class="fas fa-circle"></i>
                            </div>
                            <div class="detail-info">
                                        <label style="font-size: 12px; color: #999; margin-bottom: 3px; display: block;">Account Status</label>
                                        <strong style="font-size: 16px; color: ${user.status === 'active' ? '#4CAF50' : '#95a5a6'};">${user.status.toUpperCase()}</strong>
                                    </div>
                            </div>
                        </div>
                        
                            <div class="user-stats" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 12px; margin-bottom: 25px;">
                                <h3 style="margin: 0 0 20px 0; color: white;"><i class="fas fa-chart-line"></i> Performance Overview</h3>
                                <div class="stats-row" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 20px;">
                                    <div class="stat-item" style="text-align: center; background: rgba(255,255,255,0.15); padding: 20px; border-radius: 10px; backdrop-filter: blur(10px);">
                                        <div class="stat-value" style="color: white; font-size: 32px; font-weight: bold; margin-bottom: 5px;">${testsTaken}</div>
                                        <div class="stat-label" style="color: rgba(255,255,255,0.9); font-size: 14px;">Tests Taken</div>
                            </div>
                                    <div class="stat-item" style="text-align: center; background: rgba(255,255,255,0.15); padding: 20px; border-radius: 10px; backdrop-filter: blur(10px);">
                                        <div class="stat-value" style="color: white; font-size: 32px; font-weight: bold; margin-bottom: 5px;">${avgScore}%</div>
                                        <div class="stat-label" style="color: rgba(255,255,255,0.9); font-size: 14px;">Average Score</div>
                            </div>
                                    <div class="stat-item" style="text-align: center; background: rgba(255,255,255,0.15); padding: 20px; border-radius: 10px; backdrop-filter: blur(10px);">
                                        <div class="stat-value" style="color: white; font-size: 32px; font-weight: bold; margin-bottom: 5px;">#${rank}</div>
                                        <div class="stat-label" style="color: rgba(255,255,255,0.9); font-size: 14px;">Overall Rank</div>
                                    </div>
                                    <div class="stat-item" style="text-align: center; background: rgba(255,255,255,0.15); padding: 20px; border-radius: 10px; backdrop-filter: blur(10px);">
                                        <div class="stat-value" style="color: white; font-size: 32px; font-weight: bold; margin-bottom: 5px;">${studyTime}h</div>
                                        <div class="stat-label" style="color: rgba(255,255,255,0.9); font-size: 14px;">Study Time</div>
                                    </div>
                                    <div class="stat-item" style="text-align: center; background: rgba(255,255,255,0.15); padding: 20px; border-radius: 10px; backdrop-filter: blur(10px);">
                                        <div class="stat-value" style="color: white; font-size: 32px; font-weight: bold; margin-bottom: 5px;">${currentStreak}</div>
                                        <div class="stat-label" style="color: rgba(255,255,255,0.9); font-size: 14px;">Current Streak</div>
                                    </div>
                                    <div class="stat-item" style="text-align: center; background: rgba(255,255,255,0.15); padding: 20px; border-radius: 10px; backdrop-filter: blur(10px);">
                                        <div class="stat-value" style="color: white; font-size: 32px; font-weight: bold; margin-bottom: 5px;">${longestStreak}</div>
                                        <div class="stat-label" style="color: rgba(255,255,255,0.9); font-size: 14px;">Longest Streak</div>
                                    </div>
                        </div>
                    </div>
                    
                            <div style="background: white; padding: 25px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                                <h3 style="margin: 0 0 15px 0;"><i class="fas fa-history"></i> Recent Tests (Last 5)</h3>
                                <table class="mini-table" style="width: 100%; border-collapse: collapse;">
                                    <thead>
                                        <tr style="background: #f8f9fa;">
                                            <th style="padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0;">Test Name</th>
                                            <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Score</th>
                                            <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Time</th>
                                            <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Date</th>
                                            <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Status</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${testHistory.slice(0, 5).map(test => `
                                            <tr style="border-bottom: 1px solid #f0f0f0;">
                                                <td style="padding: 12px;">
                                                    <strong style="color: #333;">${test.name}</strong><br>
                                                    <small style="color: #999;">${test.category}</small>
                                                </td>
                                                <td style="padding: 12px; text-align: center;">
                                                    <strong style="color: #6C63FF; font-size: 16px;">${test.score}/${test.total}</strong><br>
                                                    <small style="color: #999;">${test.percentage}%</small>
                                                </td>
                                                <td style="padding: 12px; text-align: center;">
                                                    <span style="color: #666;">${test.time}</span>
                                                </td>
                                                <td style="padding: 12px; text-align: center;">
                                                    <span style="color: #666;">${test.date}</span>
                                                </td>
                                                <td style="padding: 12px; text-align: center;">
                                                    <span class="badge badge-${test.status === 'passed' ? 'success' : 'danger'}" style="padding: 5px 12px; border-radius: 20px; font-size: 11px;">${test.status.toUpperCase()}</span>
                                                </td>
                                            </tr>
                                        `).join('')}
                                    </tbody>
                                </table>
                            </div>
                            </div>
                        
                        <!-- Performance Tab -->
                        <div id="performanceContent" class="user-tab-content" style="display: none;">
                            <div style="background: white; padding: 25px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); margin-bottom: 20px;">
                                <h3 style="margin: 0 0 20px 0;"><i class="fas fa-chart-bar"></i> Subject-wise Performance</h3>
                                ${subjectPerformance.map(subject => `
                                    <div style="margin-bottom: 20px; padding: 15px; background: #f8f9fa; border-radius: 10px;">
                                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                                            <strong style="font-size: 16px; color: #333;">${subject.subject}</strong>
                                            <span style="color: #6C63FF; font-size: 20px; font-weight: bold;">${subject.avgScore}%</span>
                            </div>
                                        <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; font-size: 13px; color: #666; margin-bottom: 10px;">
                                            <div><strong>Tests:</strong> ${subject.tests}</div>
                                            <div><strong>Best:</strong> ${subject.bestScore}%</div>
                                            <div><strong>Weak Area:</strong> ${subject.weakTopic}</div>
                            </div>
                                        <div style="background: #e0e0e0; height: 8px; border-radius: 10px; overflow: hidden;">
                                            <div style="background: linear-gradient(90deg, #6C63FF, #764ba2); height: 100%; width: ${subject.avgScore}%; border-radius: 10px;"></div>
                        </div>
                                    </div>
                                `).join('')}
                    </div>
                    
                            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
                                <div style="background: white; padding: 25px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                                    <h3 style="margin: 0 0 20px 0; color: #4CAF50;"><i class="fas fa-star"></i> Strengths</h3>
                                    ${subjectPerformance.filter(s => s.avgScore >= 80).map(s => `
                                        <div style="background: #e8f5e9; padding: 10px 15px; margin-bottom: 10px; border-radius: 8px; border-left: 4px solid #4CAF50;">
                                            <strong>${s.subject}</strong><br>
                                            <small style="color: #666;">${s.avgScore}% average | ${s.tests} tests</small>
                                        </div>
                                    `).join('') || '<p style="color: #999;">Keep practicing to build strengths!</p>'}
                                </div>
                                
                                <div style="background: white; padding: 25px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                                    <h3 style="margin: 0 0 20px 0; color: #FF6B6B;"><i class="fas fa-exclamation-triangle"></i> Areas to Improve</h3>
                                    ${subjectPerformance.filter(s => s.avgScore < 75).map(s => `
                                        <div style="background: #ffebee; padding: 10px 15px; margin-bottom: 10px; border-radius: 8px; border-left: 4px solid #FF6B6B;">
                                            <strong>${s.subject}</strong><br>
                                            <small style="color: #666;">${s.avgScore}% average | Focus on: ${s.weakTopic}</small>
                                        </div>
                                    `).join('') || '<p style="color: #999; font-style: italic;">Great performance across all subjects!</p>'}
                                </div>
                            </div>
                        </div>
                        
                        <!-- Test History Tab -->
                        <div id="historyContent" class="user-tab-content" style="display: none;">
                            <div style="background: white; padding: 25px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                                <h3 style="margin: 0 0 20px 0;"><i class="fas fa-list-alt"></i> Complete Test History (${testHistory.length} tests)</h3>
                                <div style="max-height: 500px; overflow-y: auto;">
                                    <table class="mini-table" style="width: 100%; border-collapse: collapse;">
                                        <thead style="position: sticky; top: 0; background: white; z-index: 10;">
                                            <tr style="background: #f8f9fa;">
                                                <th style="padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0;">#</th>
                                                <th style="padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0;">Test Name</th>
                                                <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Category</th>
                                                <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Score</th>
                                                <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">%</th>
                                                <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Time</th>
                                                <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Date</th>
                                                <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                            ${testHistory.map((test, index) => `
                                                <tr style="border-bottom: 1px solid #f0f0f0; ${test.status === 'failed' ? 'background: #fff5f5;' : ''}">
                                                    <td style="padding: 12px; color: #999;">${index + 1}</td>
                                                    <td style="padding: 12px;">
                                                        <strong style="color: #333;">${test.name}</strong>
                                                    </td>
                                                    <td style="padding: 12px; text-align: center;">
                                                        <span class="badge badge-info" style="padding: 4px 10px; border-radius: 15px; font-size: 11px;">${test.category}</span>
                                                    </td>
                                                    <td style="padding: 12px; text-align: center;">
                                                        <strong style="color: ${test.percentage >= 75 ? '#4CAF50' : test.percentage >= 50 ? '#FFD93D' : '#FF6B6B'}; font-size: 15px;">${test.score}/${test.total}</strong>
                                                    </td>
                                                    <td style="padding: 12px; text-align: center;">
                                                        <strong style="color: ${test.percentage >= 75 ? '#4CAF50' : test.percentage >= 50 ? '#FF9800' : '#FF6B6B'};">${test.percentage}%</strong>
                                                    </td>
                                                    <td style="padding: 12px; text-align: center; color: #666;">${test.time}</td>
                                                    <td style="padding: 12px; text-align: center; color: #666; font-size: 12px;">${test.date}</td>
                                                    <td style="padding: 12px; text-align: center;">
                                                        <span class="badge badge-${test.status === 'passed' ? 'success' : 'danger'}" style="padding: 5px 12px; border-radius: 20px; font-size: 10px;">${test.status.toUpperCase()}</span>
                                                    </td>
                                </tr>
                                            `).join('')}
                            </tbody>
                        </table>
                                </div>
                            </div>
                    </div>
                    
                        <!-- Analytics Tab -->
                        <div id="analyticsContent" class="user-tab-content" style="display: none;">
                            <div style="background: white; padding: 25px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); margin-bottom: 20px;">
                                <h3 style="margin: 0 0 20px 0;"><i class="fas fa-brain"></i> Detailed Analytics & Insights</h3>
                                
                                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 25px;">
                                    <div style="background: linear-gradient(135deg, #6C63FF, #5a52d5); padding: 20px; border-radius: 10px; color: white;">
                                        <div style="font-size: 28px; font-weight: bold; margin-bottom: 5px;">${(testHistory.filter(t => t.percentage >= 75).length / testHistory.length * 100).toFixed(0)}%</div>
                                        <div style="font-size: 13px; opacity: 0.9;">Success Rate (≥75%)</div>
                                </div>
                                    <div style="background: linear-gradient(135deg, #4CAF50, #45a049); padding: 20px; border-radius: 10px; color: white;">
                                        <div style="font-size: 28px; font-weight: bold; margin-bottom: 5px;">${testHistory.filter(t => t.status === 'passed').length}</div>
                                        <div style="font-size: 13px; opacity: 0.9;">Tests Passed</div>
                            </div>
                                    <div style="background: linear-gradient(135deg, #FF6B6B, #e85d5d); padding: 20px; border-radius: 10px; color: white;">
                                        <div style="font-size: 28px; font-weight: bold; margin-bottom: 5px;">${testHistory.filter(t => t.status === 'failed').length}</div>
                                        <div style="font-size: 13px; opacity: 0.9;">Tests Failed</div>
                                </div>
                                    <div style="background: linear-gradient(135deg, #FFD93D, #f5cd2d); padding: 20px; border-radius: 10px; color: white;">
                                        <div style="font-size: 28px; font-weight: bold; margin-bottom: 5px;">${Math.max(...testHistory.map(t => t.percentage))}%</div>
                                        <div style="font-size: 13px; opacity: 0.9;">Highest Score</div>
                            </div>
                                </div>
                                
                                <h4 style="margin: 25px 0 15px 0;"><i class="fas fa-trophy"></i> Top Performing Tests</h4>
                                ${testHistory.sort((a, b) => b.percentage - a.percentage).slice(0, 3).map((test, index) => `
                                    <div style="background: #f8f9fa; padding: 15px; border-radius: 10px; margin-bottom: 10px; border-left: 4px solid ${index === 0 ? '#FFD700' : index === 1 ? '#C0C0C0' : '#CD7F32'};">
                                        <div style="display: flex; justify-content: space-between; align-items: center;">
                                            <div>
                                                <strong style="font-size: 15px;">${index + 1}. ${test.name}</strong><br>
                                                <small style="color: #666;">${test.category} | ${test.date}</small>
                            </div>
                                            <div style="text-align: right;">
                                                <div style="font-size: 24px; font-weight: bold; color: #4CAF50;">${test.percentage}%</div>
                                                <small style="color: #999;">${test.score}/${test.total}</small>
                        </div>
                    </div>
                </div>
                                `).join('')}
                                
                                <h4 style="margin: 25px 0 15px 0;"><i class="fas fa-chart-pie"></i> Test Category Distribution</h4>
                                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px;">
                                    ${Array.from(new Set(testHistory.map(t => t.category))).map(category => {
                                        const count = testHistory.filter(t => t.category === category).length;
                                        const percentage = ((count / testHistory.length) * 100).toFixed(0);
                                        return `
                                            <div style="background: #f8f9fa; padding: 15px; border-radius: 10px; text-align: center;">
                                                <div style="font-size: 20px; font-weight: bold; color: #6C63FF; margin-bottom: 5px;">${count}</div>
                                                <div style="font-size: 13px; color: #666; margin-bottom: 8px;">${category}</div>
                                                <div style="background: #e0e0e0; height: 6px; border-radius: 10px; overflow: hidden;">
                                                    <div style="background: #6C63FF; height: 100%; width: ${percentage}%; border-radius: 10px;"></div>
                                                </div>
                                                <small style="color: #999; font-size: 11px;">${percentage}% of total</small>
                                            </div>
                                        `;
                                    }).join('')}
                                </div>
                            </div>
                            
                            <div style="background: white; padding: 25px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                                <h3 style="margin: 0 0 20px 0;"><i class="fas fa-clock"></i> Study Pattern & Activity</h3>
                                <div style="background: #f8f9fa; padding: 20px; border-radius: 10px; text-align: center;">
                                    <p style="color: #666; margin: 0;">Average time spent per test: <strong style="color: #6C63FF; font-size: 20px;">${Math.round(testHistory.reduce((acc, t) => acc + parseInt(t.time), 0) / testHistory.length)} minutes</strong></p>
                                    <p style="color: #666; margin: 10px 0 0 0;">Total study time: <strong style="color: #4CAF50; font-size: 20px;">${studyTime} hours</strong></p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="modal-footer" style="background: #f8f9fa; padding: 20px; border-top: 2px solid #e0e0e0;">
                    <button class="btn btn-secondary" onclick="closeViewUserModal()" style="padding: 12px 25px;">
                        <i class="fas fa-times"></i> Close
                    </button>
                    <button class="btn btn-primary" onclick="editUser(${id}); closeViewUserModal();" style="padding: 12px 25px;">
                        <i class="fas fa-edit"></i> Edit User
                    </button>
                </div>
            </div>
        </div>
    `;
    
    // Add modal to body
    const modalDiv = document.createElement('div');
    modalDiv.innerHTML = modalHTML;
    document.body.appendChild(modalDiv.firstElementChild);
}

// Switch tabs in user detail modal
function switchUserTab(tab) {
    // Hide all tabs
    document.querySelectorAll('.user-tab-content').forEach(content => content.style.display = 'none');
    document.querySelectorAll('.user-detail-tab').forEach(tabBtn => {
        tabBtn.classList.remove('active');
        tabBtn.style.borderBottomColor = 'transparent';
    });
    
    // Show selected tab
    document.getElementById(`${tab}Content`).style.display = 'block';
    const activeTab = document.getElementById(`${tab}Tab`);
    activeTab.classList.add('active');
    activeTab.style.borderBottomColor = '#6C63FF';
}

function closeViewUserModal() {
    const modal = document.getElementById('viewUserModal');
    if (modal) {
        modal.remove();
    }
}

async function editUser(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/users/get_details.php?id=${id}`);
        const data = await response.json();
        
        if (data.success && data.user) {
            const user = data.user;
            document.getElementById('userName').value = user.name;
            document.getElementById('userEmail').value = user.email || '';
            document.getElementById('userMobile').value = user.mobile;
            
            if (document.getElementById('userDistrict')) {
                document.getElementById('userDistrict').value = user.district || '';
            }
            if (document.getElementById('userEducation')) {
                document.getElementById('userEducation').value = user.education || '';
            }
            if (document.getElementById('userAge')) {
                document.getElementById('userAge').value = user.age || '';
            }
            
            document.getElementById('userLanguage').value = user.language;
            
            // Store user ID for update
            document.getElementById('userForm').dataset.userId = id;
            document.getElementById('userForm').dataset.editMode = 'true';
            
            openModal('userModal');
        } else {
            showNotification('Failed to load user details', 'error');
        }
    } catch (error) {
        console.error('Error loading user:', error);
        showNotification('Error loading user details', 'error');
    }
}

async function deleteUser(id) {
    if (confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
        try {
            const response = await fetch(`${API_BASE_URL}/admin/users/crud.php`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: id })
            });
            
            const data = await response.json();
            
            if (data.success) {
                loadUsers();
                showNotification('User deleted successfully!', 'success');
            } else {
                showNotification(data.message || 'Failed to delete user', 'error');
            }
        } catch (error) {
            console.error('Error deleting user:', error);
            showNotification('Error deleting user', 'error');
        }
    }
}

// Exams CRUD
function loadExams() {
    const tbody = document.getElementById('examsTableBody');
    tbody.innerHTML = exams.map(exam => `
        <tr>
            <td>#E${exam.id.toString().padStart(3, '0')}</td>
            <td>${exam.name}</td>
            <td>${exam.category}</td>
            <td>${exam.questions}</td>
            <td>${exam.duration} min</td>
            <td><span class="badge badge-${exam.status === 'active' ? 'success' : 'warning'}">${exam.status}</span></td>
            <td>
                <button class="btn-icon btn-edit" onclick="editExam(${exam.id})"><i class="fas fa-edit"></i></button>
                <button class="btn-icon btn-delete" onclick="deleteExam(${exam.id})"><i class="fas fa-trash"></i></button>
            </td>
        </tr>
    `).join('');
}

function saveExam() {
    const exam = {
        id: exams.length + 1,
        name: document.getElementById('examName').value,
        category: document.getElementById('examCategory').value,
        questions: document.getElementById('examQuestions').value,
        duration: document.getElementById('examDuration').value,
        status: 'active'
    };
    
    exams.push(exam);
    loadExams();
    closeModal('examModal');
    showNotification('Exam added successfully!', 'success');
    document.getElementById('examForm').reset();
}

function editExam(id) {
    const exam = exams.find(e => e.id === id);
    document.getElementById('examName').value = exam.name;
    document.getElementById('examCategory').value = exam.category;
    document.getElementById('examQuestions').value = exam.questions;
    document.getElementById('examDuration').value = exam.duration;
    openModal('examModal');
}

function deleteExam(id) {
    if (confirm('Are you sure you want to delete this exam?')) {
        exams = exams.filter(e => e.id !== id);
        loadExams();
        showNotification('Exam deleted successfully!', 'success');
    }
}

// Questions CRUD
function loadQuestions() {
    const tbody = document.getElementById('questionsTableBody');
    tbody.innerHTML = questions.map(q => `
        <tr>
            <td>#Q${q.id.toString().padStart(3, '0')}</td>
            <td>${q.text.substring(0, 50)}${q.text.length > 50 ? '...' : ''}</td>
            <td>${q.category}</td>
            <td><span class="badge badge-${q.difficulty === 'easy' ? 'success' : q.difficulty === 'medium' ? 'warning' : 'danger'}">${q.difficulty}</span></td>
            <td>${q.language === 'en' ? 'English' : 'Tamil'}</td>
            <td>
                <button class="btn-icon btn-view" onclick="viewQuestion(${q.id})"><i class="fas fa-eye"></i></button>
                <button class="btn-icon btn-edit" onclick="editQuestion(${q.id})"><i class="fas fa-edit"></i></button>
                <button class="btn-icon btn-delete" onclick="deleteQuestion(${q.id})"><i class="fas fa-trash"></i></button>
            </td>
        </tr>
    `).join('');
}

async function saveQuestion() {
    // This function is for the old question form - if it exists, it needs session_id
    // For now, we'll show a message to use CSV upload instead
    showNotification('Please use the CSV bulk upload feature to add questions to a session.', 'info');
    closeModal('questionModal');
}

function viewQuestion(id) {
    const q = questions.find(qu => qu.id === id);
    alert(`Question: ${q.text}\n\nCategory: ${q.category}\nDifficulty: ${q.difficulty}\nLanguage: ${q.language}`);
}

// This function is deprecated - use editQuestion(questionId, sessionId) instead
function editQuestion(id) {
    showNotification('Please use the "View Questions" feature to edit questions from a session.', 'info');
}

async function deleteQuestion(id) {
    if (confirm('Are you sure you want to delete this question?')) {
        try {
            const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: id })
            });
            
            const data = await response.json();
            
            if (data.success) {
                showNotification('Question deleted successfully!', 'success');
                // Reload questions if we're on a questions page
                if (typeof loadQuestions === 'function') {
                    loadQuestions();
                }
                // Reload session cards to update question counts
                await loadSessionCards();
            } else {
                showNotification(data.message || 'Failed to delete question', 'error');
            }
        } catch (error) {
            console.error('Error deleting question:', error);
            showNotification('Error deleting question: ' + error.message, 'error');
        }
    }
}

// Categories CRUD
function loadCategories() {
    const grid = document.getElementById('categoriesGrid');
    grid.innerHTML = categories.map(cat => `
        <div class="category-card">
            <div class="category-header" style="background: ${cat.color};">
                <i class="${cat.icon}"></i>
            </div>
            <div class="category-body">
                <h3>${cat.name}</h3>
                <p>${cat.description}</p>
                <p style="margin-top: 10px;"><strong>${cat.tests} Tests Available</strong></p>
            </div>
            <div class="category-actions">
                <button class="btn-icon btn-edit" onclick="editCategory(${cat.id})"><i class="fas fa-edit"></i></button>
                <button class="btn-icon btn-delete" onclick="deleteCategory(${cat.id})"><i class="fas fa-trash"></i></button>
            </div>
        </div>
    `).join('');
}

function saveCategory() {
    const category = {
        id: categories.length + 1,
        name: document.getElementById('categoryName').value,
        description: document.getElementById('categoryDescription').value,
        icon: document.getElementById('categoryIcon').value || 'fas fa-book',
        color: document.getElementById('categoryColor').value,
        tests: 0
    };
    
    categories.push(category);
    loadCategories();
    closeModal('categoryModal');
    showNotification('Category added successfully!', 'success');
    document.getElementById('categoryForm').reset();
}

function editCategory(id) {
    const cat = categories.find(c => c.id === id);
    document.getElementById('categoryName').value = cat.name;
    document.getElementById('categoryDescription').value = cat.description;
    document.getElementById('categoryIcon').value = cat.icon;
    document.getElementById('categoryColor').value = cat.color;
    openModal('categoryModal');
}

function deleteCategory(id) {
    if (confirm('Are you sure you want to delete this category?')) {
        categories = categories.filter(c => c.id !== id);
        loadCategories();
        showNotification('Category deleted successfully!', 'success');
    }
}

// Exam Categories CRUD
async function loadExamCategories() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/exam_categories/crud.php`);
        const data = await response.json();
        
        if (data.success) {
            examCategories = data.categories;
    const grid = document.getElementById('examCategoriesGrid');
    grid.innerHTML = examCategories.map(cat => `
        <div class="category-card">
                    <div class="category-header" style="background: #6C63FF;">
                        <div style="font-size: 48px; font-weight: bold; color: white;">${cat.icon || cat.name.charAt(0)}</div>
            </div>
            <div class="category-body">
                        <h3>${cat.name}</h3>
                        <p style="font-size: 13px; color: #999;">${cat.description || ''}</p>
            </div>
                    <div class="category-actions">
                        <button class="btn-icon btn-edit" onclick="editExamCategory(${cat.id})"><i class="fas fa-edit"></i></button>
                        <button class="btn-icon btn-delete" onclick="deleteExamCategory(${cat.id})"><i class="fas fa-trash"></i></button>
                    </div>
                </div>
            `).join('');
            populateExamCategoryDropdown();
        } else {
            console.error('Failed to load exam categories:', data.message);
        }
    } catch (error) {
        console.error('Error loading exam categories:', error);
        showNotification('Failed to load exam categories', 'error');
    }
}

async function saveExamCategory() {
    const form = document.getElementById('examCategoryForm');
    const nameInput = document.getElementById('examCategoryName');
    const categoryIdInput = document.getElementById('categoryIdHidden') || document.createElement('input');
    
    const categoryData = {
        name: nameInput.value,
        description: document.getElementById('examCategoryDescription').value,
        icon: document.getElementById('examCategoryIcon').value || '?'
    };
    
    // Check if we're in edit mode by looking at stored ID
    const storedId = form.getAttribute('data-edit-id');
    const isEditMode = storedId && storedId !== '';
    
    if (isEditMode) {
        categoryData.id = parseInt(storedId);
    }
    
    try {
        const method = isEditMode ? 'PUT' : 'POST';
        const url = `${API_BASE_URL}/admin/exam_categories/crud.php`;
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(categoryData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            const action = isEditMode ? 'updated' : 'added';
            
            // Clear form and edit mode
            form.reset();
            form.setAttribute('data-edit-id', '');
            document.querySelector('#examCategoryModal h2').textContent = 'Add Exam Category';
            
            // Close modal
            closeModal('examCategoryModal');
            
            // Reload data
            loadExamCategories();
            
            // Show notification
            showNotification(`Exam category ${action} successfully!`, 'success');
        } else {
            showNotification(data.message || `Failed to ${isEditMode ? 'update' : 'add'} exam category`, 'error');
        }
    } catch (error) {
        console.error(`Error ${isEditMode ? 'updating' : 'adding'} exam category:`, error);
        showNotification(`Error saving exam category`, 'error');
    }
}

async function editExamCategory(id) {
    const cat = examCategories.find(c => c.id === id);
    if (cat) {
        const form = document.getElementById('examCategoryForm');
        
        // Populate form
        document.getElementById('examCategoryName').value = cat.name;
        document.getElementById('examCategoryDescription').value = cat.description || '';
        document.getElementById('examCategoryIcon').value = cat.icon || '';
        
        // Mark as edit mode
        form.setAttribute('data-edit-id', id);
        
        // Change modal title
        document.querySelector('#examCategoryModal h2').textContent = 'Edit Exam Category';
        
        openModal('examCategoryModal');
    }
}

async function deleteExamCategory(id) {
    if (confirm('Are you sure you want to delete this exam category? This will also delete all related test categories and questions.')) {
        try {
            const response = await fetch(`${API_BASE_URL}/admin/exam_categories/crud.php`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: id })
            });
            
            const data = await response.json();
            
            if (data.success) {
                loadExamCategories();
                populateExamCategoryDropdown();
                showNotification('Exam category deleted successfully!', 'success');
            } else {
                showNotification(data.message || 'Failed to delete exam category', 'error');
            }
        } catch (error) {
            console.error('Error deleting exam category:', error);
            showNotification('Error deleting exam category', 'error');
        }
    }
}

// Populate Exam Category Dropdown
function populateExamCategoryDropdown() {
    const dropdown = document.getElementById('testCategoryExam');
    if (dropdown) {
        const currentValue = dropdown.value;
        dropdown.innerHTML = '<option value="">-- Select Exam Category --</option>' + 
            examCategories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('');
        if (currentValue) {
            dropdown.value = currentValue;
        }
    }
    
    // Also populate language modal dropdown
    const langDropdown = document.getElementById('languageExamCategory');
    if (langDropdown) {
        const currentValue = langDropdown.value;
        langDropdown.innerHTML = '<option value="">Select Exam Category</option>' + 
            examCategories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('');
        if (currentValue) {
            langDropdown.value = currentValue;
        }
    }
}

// Languages CRUD
async function loadLanguages() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/languages/crud.php`);
        const data = await response.json();
        
        if (data.success) {
            languages = data.data;
            displayLanguages();
        }
    } catch (error) {
        console.error('Error loading languages:', error);
        showNotification('Error loading languages', 'error');
    }
}

function displayLanguages() {
    const grid = document.getElementById('languagesGrid');
    if (!grid) return;
    
    if (languages.length === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #666;">
                <i class="fas fa-language" style="font-size: 48px; margin-bottom: 15px; display: block;"></i>
                <p style="font-size: 16px;">No languages added yet</p>
                <p style="font-size: 14px; margin-top: 10px;">Click "Add Language" to create your first language</p>
            </div>
        `;
        return;
    }
    
    // Group languages by exam category
    const grouped = {};
    languages.forEach(lang => {
        const examName = lang.exam_name || 'Unknown';
        if (!grouped[examName]) {
            grouped[examName] = [];
        }
        grouped[examName].push(lang);
    });
    
    grid.innerHTML = '';
    
    Object.keys(grouped).forEach(examName => {
        const sectionDiv = document.createElement('div');
        sectionDiv.style.gridColumn = '1/-1';
        sectionDiv.style.marginBottom = '20px';
        
        sectionDiv.innerHTML = `
            <h3 style="font-size: 18px; margin-bottom: 15px; color: #333; border-bottom: 2px solid #6C63FF; padding-bottom: 10px;">
                ${examName}
            </h3>
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 15px;">
                ${grouped[examName].map(lang => `
                    <div class="card" style="padding: 20px; ${!lang.is_active ? 'opacity: 0.6; border: 2px dashed #ccc;' : ''}">
                        <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 15px;">
                            <div style="font-size: 40px;">${lang.icon || '🌐'}</div>
                            <div style="display: flex; gap: 8px;">
                                <button class="btn btn-sm btn-icon" onclick="editLanguage(${lang.id})" title="Edit">
                                    <i class="fas fa-edit"></i>
                                </button>
                                <button class="btn btn-sm btn-icon btn-danger" onclick="deleteLanguage(${lang.id})" title="Delete">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </div>
                        </div>
                        <h3 style="margin: 0 0 5px 0; font-size: 18px;">${lang.name}</h3>
                        <p style="color: #666; font-size: 13px; margin: 0 0 10px 0;">
                            Code: <strong>${lang.code}</strong>
                        </p>
                        <div style="display: flex; gap: 10px; margin-top: 10px;">
                            <span style="background: ${lang.is_active ? '#d4edda' : '#f8d7da'}; color: ${lang.is_active ? '#155724' : '#721c24'}; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600;">
                                ${lang.is_active ? '✅ Active' : '❌ Inactive'}
                            </span>
                            <span style="background: #e7f3ff; color: #0066cc; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600;">
                                Order: ${lang.display_order}
                            </span>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
        
        grid.appendChild(sectionDiv);
    });
}

async function saveLanguage() {
    const form = document.getElementById('languageForm');
    const editId = form.getAttribute('data-edit-id');
    const isEditMode = editId && editId !== '';
    
    const languageData = {
        exam_category_id: parseInt(document.getElementById('languageExamCategory').value),
        name: document.getElementById('languageName').value,
        code: document.getElementById('languageCode').value,
        icon: document.getElementById('languageIcon').value || '🌐',
        display_order: parseInt(document.getElementById('languageDisplayOrder').value) || 0,
        is_active: document.getElementById('languageIsActive').checked ? 1 : 0
    };
    
    if (!languageData.exam_category_id || !languageData.name || !languageData.code) {
        showNotification('Please fill in all required fields', 'error');
        return;
    }
    
    if (isEditMode) {
        languageData.id = parseInt(editId);
    }
    
    try {
        const method = isEditMode ? 'PUT' : 'POST';
        const response = await fetch(`${API_BASE_URL}/admin/languages/crud.php`, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(languageData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            const action = isEditMode ? 'updated' : 'added';
            
            // Clear form and edit mode
            form.reset();
            form.setAttribute('data-edit-id', '');
            document.querySelector('#languageModal h2').textContent = 'Add Language';
            
            // Close modal
            closeModal('languageModal');
            
            // Reload data
            loadLanguages();
            
            // Show notification
            showNotification(`Language ${action} successfully!`, 'success');
        } else {
            showNotification(data.message || `Failed to ${isEditMode ? 'update' : 'add'} language`, 'error');
        }
    } catch (error) {
        console.error(`Error saving language:`, error);
        showNotification(`Error saving language`, 'error');
    }
}

async function editLanguage(id) {
    const lang = languages.find(l => l.id === id);
    if (lang) {
        const form = document.getElementById('languageForm');
        
        // Populate form
        document.getElementById('languageExamCategory').value = lang.exam_category_id;
        document.getElementById('languageName').value = lang.name;
        document.getElementById('languageCode').value = lang.code;
        document.getElementById('languageIcon').value = lang.icon || '';
        document.getElementById('languageDisplayOrder').value = lang.display_order;
        document.getElementById('languageIsActive').checked = lang.is_active == 1;
        
        // Mark as edit mode
        form.setAttribute('data-edit-id', id);
        
        // Change modal title
        document.querySelector('#languageModal h2').textContent = 'Edit Language';
        
        openModal('languageModal');
    }
}

async function deleteLanguage(id) {
    if (confirm('Are you sure you want to delete this language?')) {
        try {
            const response = await fetch(`${API_BASE_URL}/admin/languages/crud.php`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: id })
            });
            
            const data = await response.json();
            
            if (data.success) {
                loadLanguages();
                showNotification('Language deleted successfully!', 'success');
            } else {
                showNotification(data.message || 'Failed to delete language', 'error');
            }
        } catch (error) {
            console.error('Error deleting language:', error);
            showNotification('Error deleting language', 'error');
        }
    }
}

// Test Categories CRUD
async function loadTestCategories() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/test_categories/crud.php`);
        const data = await response.json();
        
        if (data.success) {
            testCategories = data.categories;
            const tbody = document.getElementById('testCategoriesTableBody');
            tbody.innerHTML = testCategories.map(cat => {
                // Build image display
                let imageDisplay = '';
                if (cat.image_path || cat.image) {
                    const imagePath = cat.image_path || cat.image;
                    imageDisplay = `
                        <img src="../${imagePath}" 
                             alt="${cat.name}" 
                             style="width: 50px; height: 50px; object-fit: cover; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);"
                             onerror="this.onerror=null; this.src='data:image/svg+xml,%3Csvg xmlns=%27http://www.w3.org/2000/svg%27 width=%2750%27 height=%2750%27 viewBox=%270 0 50 50%27%3E%3Crect fill=%27%23e0e0e0%27 width=%2750%27 height=%2750%27/%3E%3Ctext x=%2750%25%27 y=%2750%25%27 dominant-baseline=%27middle%27 text-anchor=%27middle%27 fill=%27%23999%27 font-size=%2714%27%3E${cat.name.charAt(0)}%3C/text%3E%3C/svg%3E';"
                        >
                    `;
                } else {
                    // Fallback to icon or letter avatar
                    const iconColor = cat.color || '#6C63FF';
                    const icon = cat.icon || 'fas fa-book';
                    imageDisplay = `
                        <div style="
                            width: 50px; 
                            height: 50px; 
                            background: linear-gradient(135deg, ${iconColor}, ${iconColor}dd);
                            border-radius: 8px;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            color: white;
                            font-size: 20px;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                        ">
                            <i class="${icon}"></i>
                        </div>
                    `;
                }
                
                return `
                    <tr>
                        <td>#TC${cat.id.toString().padStart(3, '0')}</td>
                        <td>${imageDisplay}</td>
                        <td><strong>${cat.name}</strong></td>
                        <td><span class="badge badge-info">${cat.exam_name || 'N/A'}</span></td>
                        <td>${cat.description || 'N/A'}</td>
                        <td><i class="${cat.icon || 'fas fa-book'}" style="font-size: 20px; color: ${cat.color || '#6C63FF'};"></i></td>
                        <td><div style="width: 30px; height: 30px; background: ${cat.color || '#6C63FF'}; border-radius: 5px;"></div></td>
                        <td>
                            <button class="btn-icon btn-edit" onclick="editTestCategory(${cat.id})"><i class="fas fa-edit"></i></button>
                            <button class="btn-icon btn-delete" onclick="deleteTestCategory(${cat.id})"><i class="fas fa-trash"></i></button>
                        </td>
                    </tr>
                `;
            }).join('');
            // Update total count if element exists
            const totalTestsElement = document.getElementById('totalTests');
            if (totalTestsElement) {
                totalTestsElement.textContent = testCategories.length;
            }
        } else {
            console.error('Failed to load test categories:', data.message);
        }
    } catch (error) {
        console.error('Error loading test categories:', error);
        showNotification('Failed to load test categories', 'error');
    }
}

async function saveTestCategory() {
    const form = document.getElementById('testCategoryForm');
    const isEditMode = form.dataset.editMode === 'true';
    const categoryId = form.dataset.categoryId;
    
    const categoryData = {
        name: document.getElementById('testCategoryName').value,
        exam_category_id: document.getElementById('testCategoryExam').value,
        description: document.getElementById('testCategoryDescription').value,
        icon: document.getElementById('testCategoryIcon').value || 'fas fa-tag',
        color: document.getElementById('testCategoryColor').value,
        image: document.getElementById('testCategoryImage').value || '',
        image_path: document.getElementById('testCategoryImagePath').value || ''
    };
    
    if (!categoryData.exam_category_id) {
        showNotification('Please select an exam category!', 'error');
        return;
    }
    
    if (isEditMode) {
        categoryData.id = categoryId;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/test_categories/crud.php`, {
            method: isEditMode ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(categoryData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            loadTestCategories();
            populateSessionDropdowns();
            closeModal('testCategoryModal');
            showNotification(isEditMode ? 'Test category updated successfully!' : 'Test category added successfully!', 'success');
            form.reset();
            resetImageUpload();
            delete form.dataset.categoryId;
            delete form.dataset.editMode;
        } else {
            showNotification(data.message || 'Failed to save test category', 'error');
        }
    } catch (error) {
        console.error('Error saving test category:', error);
        showNotification('Error saving test category', 'error');
    }
}

async function editTestCategory(id) {
    const cat = testCategories.find(c => c.id === id);
    if (cat) {
        document.getElementById('testCategoryName').value = cat.name;
        document.getElementById('testCategoryExam').value = cat.exam_category_id || cat.examCategory;
        document.getElementById('testCategoryDescription').value = cat.description || '';
        document.getElementById('testCategoryIcon').value = cat.icon || '';
        document.getElementById('testCategoryColor').value = cat.color || '#6C63FF';
        
        // Set image data if exists
        if (cat.image_path || cat.image) {
            const imagePath = cat.image_path || cat.image;
            document.getElementById('testCategoryImage').value = cat.image || '';
            document.getElementById('testCategoryImagePath').value = imagePath;
            
            // Show image preview
            const imagePreview = document.getElementById('imagePreview');
            const imagePreviewContainer = document.getElementById('imagePreviewContainer');
            const imageUploadArea = document.getElementById('imageUploadArea');
            
            imagePreview.src = `../${imagePath}`;
            imagePreviewContainer.style.display = 'block';
            imageUploadArea.style.display = 'none';
            
            // Show image info
            document.getElementById('imageInfo').innerHTML = `<i class="fas fa-check-circle" style="color: #4CAF50;"></i> Image loaded`;
        } else {
            resetImageUpload();
        }
        
        // Store category ID for update
        document.getElementById('testCategoryForm').dataset.categoryId = id;
        document.getElementById('testCategoryForm').dataset.editMode = 'true';
        
        openModal('testCategoryModal');
    }
}

async function deleteTestCategory(id) {
    if (confirm('Are you sure you want to delete this test category? This will also delete all related question sessions.')) {
        try {
            const response = await fetch(`${API_BASE_URL}/admin/test_categories/crud.php`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: id })
            });
            
            const data = await response.json();
            
            if (data.success) {
                loadTestCategories();
                populateSessionDropdowns();
                showNotification('Test category deleted successfully!', 'success');
            } else {
                showNotification(data.message || 'Failed to delete test category', 'error');
            }
        } catch (error) {
            console.error('Error deleting test category:', error);
            showNotification('Error deleting test category', 'error');
        }
    }
}

// Populate Session Dropdowns
function populateSessionDropdowns() {
    const examDropdown = document.getElementById('sessionExamCategory');
    const testDropdown = document.getElementById('sessionTestCategory');
    
    if (examDropdown) {
        examDropdown.innerHTML = '<option value="">-- Select Exam Category --</option>' + 
            examCategories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('');
    }
    
    if (testDropdown) {
        testDropdown.innerHTML = '<option value="">-- Select Test Category --</option>' + 
            testCategories.map(cat => `<option value="${cat.id}" data-exam="${cat.exam_category_id}">${cat.name}</option>`).join('');
    }
}

function filterTestCategories() {
    const selectedExam = document.getElementById('sessionExamCategory').value;
    const testDropdown = document.getElementById('sessionTestCategory');
    
    if (!selectedExam) {
        testDropdown.innerHTML = '<option value="">-- Select Test Category --</option>';
        return;
    }
    
    const filteredCategories = testCategories.filter(cat => String(cat.exam_category_id) === String(selectedExam));
    testDropdown.innerHTML = '<option value="">-- Select Test Category --</option>' + 
        filteredCategories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('');
}

// Question Sessions CRUD
async function loadQuestionSessions() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/sessions/crud.php`);
        const data = await response.json();
        
        if (data.success) {
            questionSessions = data.sessions;
            const tbody = document.getElementById('questionSessionsTableBody');
            tbody.innerHTML = questionSessions.map(session => `
                <tr>
                    <td>#QS${session.id.toString().padStart(3, '0')}</td>
                    <td><strong>${session.name}</strong></td>
                    <td><span class="badge badge-primary">${session.category_name || 'N/A'}</span></td>
                    <td>${session.duration || 60} mins</td>
                    <td>${session.actual_question_count || 0}</td>
                    <td><span class="badge badge-${session.is_active ? 'success' : 'secondary'}">${session.is_active ? 'active' : 'inactive'}</span></td>
                    <td>
                        <button class="btn-icon btn-view" onclick="viewSessionQuestions(${session.id})" title="View Questions"><i class="fas fa-eye"></i></button>
                        <button class="btn-icon btn-edit" onclick="editQuestionSession(${session.id})" title="Edit Session"><i class="fas fa-edit"></i></button>
                        <button class="btn-icon btn-delete" onclick="deleteQuestionSession(${session.id})" title="Delete Session"><i class="fas fa-trash"></i></button>
                    </td>
                </tr>
            `).join('');
            populateSessionDropdowns();
            loadSessionCards();
        } else {
            console.error('Failed to load question sessions:', data.message);
        }
    } catch (error) {
        console.error('Error loading question sessions:', error);
        showNotification('Failed to load question sessions', 'error');
    }
}

async function saveQuestionSession() {
    const form = document.getElementById('questionSessionForm');
    const isEditMode = form.dataset.editMode === 'true';
    const sessionId = form.dataset.sessionId;
    
    const sessionData = {
        name: document.getElementById('sessionName').value,
        exam_category_id: document.getElementById('sessionExamCategory').value,
        test_category_id: document.getElementById('sessionTestCategory').value,
        time_limit: parseInt(document.getElementById('sessionTime').value),
        total_questions: parseInt(document.getElementById('sessionTotalQuestions').value),
        status: document.getElementById('sessionStatus').value
    };
    
    if (!sessionData.exam_category_id || !sessionData.test_category_id) {
        showNotification('Please select exam category and test category!', 'error');
        return;
    }
    
    if (isEditMode) {
        sessionData.id = sessionId;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/sessions/crud.php`, {
            method: isEditMode ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(sessionData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            loadQuestionSessions();
            loadSessionCards();
            closeModal('questionSessionModal');
            showNotification(isEditMode ? 'Question session updated successfully!' : 'Question session added successfully!', 'success');
            form.reset();
            delete form.dataset.sessionId;
            delete form.dataset.editMode;
        } else {
            showNotification(data.message || 'Failed to save question session', 'error');
        }
    } catch (error) {
        console.error('Error saving question session:', error);
        showNotification('Error saving question session', 'error');
    }
}

// Removed - using viewSessionQuestions instead

async function editQuestionSession(id) {
    const session = questionSessions.find(s => s.id === id);
    if (session) {
        document.getElementById('sessionName').value = session.name;
        document.getElementById('sessionExamCategory').value = session.exam_category_id || session.examCategory;
        filterTestCategories();
        setTimeout(() => {
            document.getElementById('sessionTestCategory').value = session.test_category_id || session.testCategory;
        }, 100);
        document.getElementById('sessionTime').value = session.time_limit || session.time;
        document.getElementById('sessionTotalQuestions').value = session.total_questions || session.totalQuestions;
        document.getElementById('sessionStatus').value = session.status;
        
        // Store session ID for update
        document.getElementById('questionSessionForm').dataset.sessionId = id;
        document.getElementById('questionSessionForm').dataset.editMode = 'true';
        
        openModal('questionSessionModal');
    }
}

async function deleteQuestionSession(id) {
    if (confirm('Are you sure you want to delete this question session? This will also delete all related questions.')) {
        try {
            const response = await fetch(`${API_BASE_URL}/admin/sessions/crud.php`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: id })
            });
            
            const data = await response.json();
            
            if (data.success) {
                loadQuestionSessions();
                loadSessionCards();
                showNotification('Question session deleted successfully!', 'success');
            } else {
                showNotification(data.message || 'Failed to delete question session', 'error');
            }
        } catch (error) {
            console.error('Error deleting question session:', error);
            showNotification('Error deleting question session', 'error');
        }
    }
}

// Load Session Cards for Add Question Page
async function loadSessionCards() {
    const grid = document.getElementById('sessionCardsGrid');
    if (!grid) return;
    
    if (questionSessions.length === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 60px 20px;">
                <i class="fas fa-inbox" style="font-size: 64px; color: #ddd; margin-bottom: 20px;"></i>
                <h3 style="color: #999;">No Question Sessions Available</h3>
                <p style="color: #bbb;">Please create a question session first to add questions.</p>
                <button class="btn btn-primary" onclick="showPage('questionSessions')" style="margin-top: 20px;">
                    <i class="fas fa-plus"></i> Create Question Session
                </button>
            </div>
        `;
        return;
    }
    
    // Fetch question counts from API for all sessions
    const sessionCounts = {};
    for (const session of questionSessions) {
        try {
            const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php?session_id=${session.id}`);
            const data = await response.json();
            sessionCounts[session.id] = data.questions ? data.questions.length : 0;
        } catch (error) {
            console.error(`Error fetching count for session ${session.id}:`, error);
            sessionCounts[session.id] = 0;
        }
    }
    
    grid.innerHTML = questionSessions.map(session => {
        // Get actual count from API
        const actualQuestionCount = sessionCounts[session.id] || 0;
        const examCategory = session.exam_category_name || 'N/A';
        const testCategory = session.category_name || 'N/A';
        const duration = session.duration || 0;
        const status = session.is_active ? 'ACTIVE' : 'INACTIVE';
        
        return `
        <div class="session-card" onclick="openUploadForSession(${session.id})" style="background: white; border: 2px solid #e0e0e0; border-radius: 12px; padding: 20px; cursor: pointer; transition: all 0.3s; position: relative; overflow: hidden;">
            <div style="position: absolute; top: 0; right: 0; width: 80px; height: 80px; background: linear-gradient(135deg, ${getSessionColor(examCategory)} 0%, ${getSessionColor(examCategory)}dd 100%); border-radius: 0 0 0 80px; opacity: 0.1;"></div>
            
            <div style="position: relative; z-index: 1;">
                <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 15px;">
                    <div style="width: 50px; height: 50px; background: linear-gradient(135deg, ${getSessionColor(examCategory)} 0%, ${getSessionColor(examCategory)}dd 100%); border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px; font-weight: bold;">
                        <i class="fas fa-clipboard-list"></i>
                    </div>
                    <div style="flex: 1;">
                        <h3 style="margin: 0; font-size: 18px; color: #333;">${session.name}</h3>
                        <span class="badge badge-${status === 'ACTIVE' ? 'success' : 'secondary'}" style="margin-top: 5px; display: inline-block;">${status}</span>
                    </div>
                </div>
                
                <div style="background: #f8f9fa; padding: 12px; border-radius: 8px; margin: 15px 0;">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; font-size: 13px;">
                        <div>
                            <i class="fas fa-book" style="color: #6C63FF;"></i> 
                            <strong>Exam:</strong><br>
                            <span style="color: #666;">${examCategory}</span>
                        </div>
                        <div>
                            <i class="fas fa-tag" style="color: #FF6B6B;"></i> 
                            <strong>Category:</strong><br>
                            <span style="color: #666;">${testCategory}</span>
                        </div>
                        <div>
                            <i class="fas fa-clock" style="color: #4ECDC4;"></i> 
                            <strong>Duration:</strong><br>
                            <span style="color: #666;">${duration} minutes</span>
                        </div>
                        <div>
                            <i class="fas fa-question-circle" style="color: #FFD93D;"></i> 
                            <strong>Questions:</strong><br>
                            <span style="color: ${actualQuestionCount > 0 ? '#4CAF50' : '#666'}; font-weight: ${actualQuestionCount > 0 ? 'bold' : 'normal'};">
                                ${actualQuestionCount} ${actualQuestionCount === 0 ? '(empty)' : 'uploaded'}
                            </span>
                        </div>
                    </div>
                </div>
                
                <div style="display: flex; gap: 10px; margin-top: 10px;">
                    <button class="btn btn-primary" onclick="event.stopPropagation(); openUploadForSession(${session.id})" style="flex: 1; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border: none;">
                        <i class="fas fa-cloud-upload-alt"></i> Upload
                    </button>
                    <button class="btn btn-secondary" onclick="event.stopPropagation(); viewSessionQuestions(${session.id})" style="flex: 1; background: #4ECDC4; color: white; border: none;">
                        <i class="fas fa-eye"></i> View (${actualQuestionCount})
                    </button>
                </div>
            </div>
        </div>
        `;
    }).join('');
    
    // Add hover effect
    const cards = document.querySelectorAll('.session-card');
    cards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-5px)';
            this.style.boxShadow = '0 10px 30px rgba(0,0,0,0.15)';
            this.style.borderColor = '#6C63FF';
        });
        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
            this.style.boxShadow = 'none';
            this.style.borderColor = '#e0e0e0';
        });
    });
}

function getSessionColor(examCategory) {
    const colors = {
        'TNPSC Group 4': '#6C63FF',
        'TNPSC Group 1': '#FF6B6B',
        'TNPSC Group 2': '#4ECDC4',
        'TNPSC Group 2A': '#FFD93D',
        'TNPSC VAO': '#9C27B0'
    };
    return colors[examCategory] || '#6C63FF';
}

function openUploadForSession(sessionId) {
    const session = questionSessions.find(s => s.id === sessionId);
    if (!session) return;
    
    // Count existing questions for this session
    const existingQuestionsCount = questions.filter(q => q.sessionId === sessionId).length;
    
    document.getElementById('selectedSessionId').value = sessionId;
    document.getElementById('selectedSessionName').innerHTML = `<i class="fas fa-clipboard-list"></i> ${session.name}`;
    document.getElementById('selectedSessionDetails').textContent = `${session.examCategory} - ${session.testCategory} | ${session.time} mins`;
    
    // Show existing questions count
    const existingQuestionsInfo = document.getElementById('existingQuestionsInfo');
    if (existingQuestionsCount > 0) {
        existingQuestionsInfo.innerHTML = `
            <div style="background: #fff3cd; padding: 12px; border-radius: 8px; margin-bottom: 15px; border-left: 4px solid #ffc107;">
                <strong style="color: #856404;"><i class="fas fa-exclamation-triangle"></i> Notice:</strong>
                <p style="margin: 5px 0 0 0; color: #856404; font-size: 13px;">
                    This session already has <strong>${existingQuestionsCount} questions</strong>. 
                    Uploading will <strong>ADD</strong> new questions to the existing ones.
                </p>
                <button type="button" class="btn btn-danger" onclick="clearSessionQuestions(${sessionId})" style="margin-top: 10px; font-size: 13px; padding: 8px 16px;">
                    <i class="fas fa-trash"></i> Clear Existing Questions First
                </button>
            </div>
        `;
    } else {
        existingQuestionsInfo.innerHTML = `
            <div style="background: #d4edda; padding: 12px; border-radius: 8px; margin-bottom: 15px; border-left: 4px solid #28a745;">
                <strong style="color: #155724;"><i class="fas fa-check-circle"></i> Ready:</strong>
                <p style="margin: 5px 0 0 0; color: #155724; font-size: 13px;">
                    This session has <strong>no questions yet</strong>. Upload your CSV file to add questions.
                </p>
            </div>
        `;
    }
    
    // Reset file input when modal opens
    const fileInput = document.getElementById('csvFile');
    if (fileInput) {
        fileInput.value = '';
        const fileNameDisplay = document.getElementById('fileName');
        if (fileNameDisplay) {
            fileNameDisplay.textContent = 'No file selected';
            fileNameDisplay.style.color = '#666';
            fileNameDisplay.style.fontWeight = 'normal';
        }
    }
    
    openModal('csvUploadModal');
}

async function viewSessionQuestions(sessionId) {
    const session = questionSessions.find(s => s.id === sessionId);
    if (!session) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php?session_id=${sessionId}`);
        const data = await response.json();
        
        let sessionQuestions = [];
        if (data.success) {
            sessionQuestions = data.questions;
        }
        
        document.getElementById('viewSessionName').innerHTML = `<i class="fas fa-clipboard-list"></i> ${session.name}`;
        document.getElementById('viewSessionDetails').textContent = `${session.category_name || 'N/A'} | ${session.duration || 60} mins`;
        document.getElementById('totalQuestionsCount').textContent = sessionQuestions.length;
    
    const tbody = document.getElementById('viewQuestionsTableBody');
    
    if (sessionQuestions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="3" style="text-align: center; padding: 40px;">
                    <i class="fas fa-inbox" style="font-size: 48px; color: #ddd; margin-bottom: 15px; display: block;"></i>
                    <h4 style="color: #999; margin: 0;">No Questions Uploaded Yet</h4>
                    <p style="color: #bbb;">Upload questions using the CSV bulk upload feature.</p>
                </td>
            </tr>
        `;
    } else {
        tbody.innerHTML = sessionQuestions.map((q, index) => {
            const hasEnglish = q.question_en && q.question_en.trim();
            const hasTamil = q.question_ta && q.question_ta.trim();
            
            let languageBadge = '';
            if (hasEnglish && hasTamil) {
                languageBadge = '<span class="badge badge-info" style="font-size: 11px; margin-left: 10px;">🌐 Bilingual</span>';
            } else if (hasEnglish) {
                languageBadge = '<span class="badge badge-primary" style="font-size: 11px; margin-left: 10px;">🇬🇧 English Only</span>';
            } else if (hasTamil) {
                languageBadge = '<span class="badge badge-warning" style="font-size: 11px; margin-left: 10px;">🇮🇳 Tamil Only</span>';
            }
            
            let content = '';
            
            // English Version (if available)
            if (hasEnglish) {
                content += `
                    <div style="background: #f0f8ff; padding: 12px; border-radius: 8px; margin-bottom: ${hasTamil ? '10px' : '0'}; border-left: 4px solid #6C63FF;">
                        <div style="margin-bottom: 8px;">
                            <span class="badge badge-primary" style="font-size: 10px;">ENGLISH</span>
                            <strong style="display: block; margin-top: 5px;">${q.question_en}</strong>
                        </div>
                        <div style="font-size: 12px; color: #666;">
                            <div>A) ${q.option_a_en || ''}</div>
                            <div>B) ${q.option_b_en || ''}</div>
                            <div>C) ${q.option_c_en || ''}</div>
                            <div>D) ${q.option_d_en || ''}</div>
                        </div>
                        ${q.explanation_en ? `<div style="margin-top: 8px; padding: 6px; background: white; border-radius: 4px; font-size: 11px;"><strong>Explanation:</strong> ${q.explanation_en}</div>` : ''}
                    </div>
                `;
            }
                    
            // Tamil Version (if available)
            if (hasTamil) {
                content += `
                    <div style="background: #fff8e1; padding: 12px; border-radius: 8px; border-left: 4px solid #FFD93D;">
                        <div style="margin-bottom: 8px;">
                            <span class="badge badge-warning" style="font-size: 10px;">தமிழ்</span>
                            <strong style="display: block; margin-top: 5px;">${q.question_ta}</strong>
                        </div>
                        <div style="font-size: 12px; color: #666;">
                            <div>அ) ${q.option_a_ta || ''}</div>
                            <div>ஆ) ${q.option_b_ta || ''}</div>
                            <div>இ) ${q.option_c_ta || ''}</div>
                            <div>ஈ) ${q.option_d_ta || ''}</div>
                        </div>
                        ${q.explanation_ta ? `<div style="margin-top: 8px; padding: 6px; background: white; border-radius: 4px; font-size: 11px;"><strong>விளக்கம்:</strong> ${q.explanation_ta}</div>` : ''}
                    </div>
                `;
            }
            
            return `
                <tr>
                    <td style="text-align: center;">
                        <strong>${index + 1}</strong>
                        ${languageBadge}
                </td>
                    <td>${content}</td>
                <td style="text-align: center;">
                    <span class="badge badge-success" style="font-size: 14px;">Option ${q.correct_answer || 'A'}</span>
                </td>
            </tr>
            `;
        }).join('');
    }
    
    openModal('viewQuestionsModal');
    } catch (error) {
        console.error('Error loading session questions:', error);
        showNotification('Failed to load session questions', 'error');
    }
}

async function deleteQuestionFromView(questionId, sessionId) {
    if (confirm('Are you sure you want to delete this question?')) {
        try {
            const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id: questionId })
            });
            
            const data = await response.json();
            
            if (data.success) {
                showNotification('Question deleted successfully!', 'success');
                // Reload questions for the session
                await viewSessionQuestions(sessionId);
                // Reload session cards to update question counts
                await loadSessionCards();
            } else {
                showNotification(data.message || 'Failed to delete question', 'error');
            }
        } catch (error) {
            console.error('Error deleting question:', error);
            showNotification('Error deleting question: ' + error.message, 'error');
        }
    }
}

async function clearSessionQuestions(sessionId) {
    const session = questionSessions.find(s => s.id === sessionId);
    if (!session) return;
    
    // First, get the count from the server
    try {
        const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php?session_id=${sessionId}`);
        const result = await response.json();
        const questionsCount = result.questions ? result.questions.length : 0;
        
        if (questionsCount === 0) {
            showNotification('No questions to delete in this session!', 'info');
            return;
        }
        
        // Show confirmation with exact count from server
        if (confirm(`Are you sure you want to delete all ${questionsCount} questions from "${session.name}"?\n\nThis action cannot be undone.`)) {
            // Send delete request to API
            const deleteResponse = await fetch(`${API_BASE_URL}/admin/questions/crud.php`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ session_id: sessionId })
            });
            
            const deleteResult = await deleteResponse.json();
            
            if (deleteResult.success) {
                showNotification(`✅ Successfully deleted ${questionsCount} questions!`, 'success');
                closeModal('csvUploadModal');
                closeModal('viewQuestionsModal'); // Close view modal if open
                await loadSessionCards(); // Refresh the cards
            } else {
                showNotification(deleteResult.message || 'Failed to delete questions', 'error');
            }
        }
    } catch (error) {
        console.error('Error deleting questions:', error);
        showNotification('Error deleting questions: ' + error.message, 'error');
    }
}

async function editQuestion(questionId, sessionId) {
    try {
        // Fetch question from API
        const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php?session_id=${sessionId}`);
        const data = await response.json();
        
        if (!data.success) {
            showNotification('Failed to load question', 'error');
            return;
        }
        
        const question = data.questions.find(q => q.id == questionId);
        if (!question) {
            showNotification('Question not found!', 'error');
            return;
        }
        
        console.log('📝 Editing question:', questionId, question);
        
        // Set hidden fields
        document.getElementById('editQuestionId').value = question.id;
        document.getElementById('editQuestionSessionId').value = sessionId;
        
        // Pre-fill English fields
        document.getElementById('editQuestionEn').value = question.question_en || '';
        document.getElementById('editOptionAEn').value = question.option_a_en || '';
        document.getElementById('editOptionBEn').value = question.option_b_en || '';
        document.getElementById('editOptionCEn').value = question.option_c_en || '';
        document.getElementById('editOptionDEn').value = question.option_d_en || '';
        document.getElementById('editExplanationEn').value = question.explanation_en || '';
        
        // Pre-fill Tamil fields
        document.getElementById('editQuestionTa').value = question.question_ta || '';
        document.getElementById('editOptionATa').value = question.option_a_ta || '';
        document.getElementById('editOptionBTa').value = question.option_b_ta || '';
        document.getElementById('editOptionCTa').value = question.option_c_ta || '';
        document.getElementById('editOptionDTa').value = question.option_d_ta || '';
        document.getElementById('editExplanationTa').value = question.explanation_ta || '';
        
        // Set correct answer
        document.getElementById('editCorrectAnswer').value = question.correct_answer;
        
        // Open the modal
        openModal('editQuestionModal');
    } catch (error) {
        console.error('Error loading question:', error);
        showNotification('Failed to load question: ' + error.message, 'error');
    }
}

async function saveEditedQuestion() {
    const questionId = parseInt(document.getElementById('editQuestionId').value);
    const sessionId = parseInt(document.getElementById('editQuestionSessionId').value);
    
    // Get all field values
    const questionEn = document.getElementById('editQuestionEn').value.trim();
    const questionTa = document.getElementById('editQuestionTa').value.trim();
    const optionAEn = document.getElementById('editOptionAEn').value.trim();
    const optionATa = document.getElementById('editOptionATa').value.trim();
    const optionBEn = document.getElementById('editOptionBEn').value.trim();
    const optionBTa = document.getElementById('editOptionBTa').value.trim();
    const optionCEn = document.getElementById('editOptionCEn').value.trim();
    const optionCTa = document.getElementById('editOptionCTa').value.trim();
    const optionDEn = document.getElementById('editOptionDEn').value.trim();
    const optionDTa = document.getElementById('editOptionDTa').value.trim();
    const correctAnswer = document.getElementById('editCorrectAnswer').value;
    const explanationEn = document.getElementById('editExplanationEn').value.trim();
    const explanationTa = document.getElementById('editExplanationTa').value.trim();
    
    // Validate: Must have at least one complete language
    const hasEnglish = questionEn && optionAEn && optionBEn && optionCEn && optionDEn;
    const hasTamil = questionTa && optionATa && optionBTa && optionCTa && optionDTa;
    
    if (!hasEnglish && !hasTamil) {
        showNotification('❌ Error: Question must have complete content in at least English OR Tamil!', 'error');
        return;
    }
    
    // Validate correct answer
    if (!['A', 'B', 'C', 'D'].includes(correctAnswer)) {
        showNotification('❌ Error: Please select a correct answer (A, B, C, or D)', 'error');
        return;
    }
    
    // Prepare data for API
    const questionData = {
        id: questionId,
        question_en: questionEn || null,
        question_ta: questionTa || null,
        option_a_en: optionAEn || null,
        option_a_ta: optionATa || null,
        option_b_en: optionBEn || null,
        option_b_ta: optionBTa || null,
        option_c_en: optionCEn || null,
        option_c_ta: optionCTa || null,
        option_d_en: optionDEn || null,
        option_d_ta: optionDTa || null,
        correct_answer: correctAnswer,
        explanation_en: explanationEn || null,
        explanation_ta: explanationTa || null,
        difficulty: 'medium',
        marks: 1,
        negative_marks: 0.25
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(questionData)
        });
        
        const result = await response.json();
        
        if (result.success) {
            showNotification('✅ Question updated successfully!', 'success');
            closeModal('editQuestionModal');
            // Reload questions for the session
            await viewSessionQuestions(sessionId);
            // Reload session cards to update question counts
            await loadSessionCards();
        } else {
            showNotification('❌ ' + (result.message || 'Failed to update question'), 'error');
        }
    } catch (error) {
        console.error('Error updating question:', error);
        showNotification('❌ Error updating question: ' + error.message, 'error');
    }
}

// Function removed - using label for="csvFile" instead which is more reliable

function handleFileSelect(event) {
    const fileInput = event.target;
    const fileNameDisplay = document.getElementById('fileName');
    
    if (fileInput.files && fileInput.files.length > 0) {
        const file = fileInput.files[0];
        const fileSize = (file.size / 1024).toFixed(2);
        fileNameDisplay.textContent = `Selected: ${file.name} (${fileSize} KB)`;
        fileNameDisplay.style.color = '#4CAF50';
        fileNameDisplay.style.fontWeight = '600';
    } else {
        fileNameDisplay.textContent = 'No file selected';
        fileNameDisplay.style.color = '#666';
        fileNameDisplay.style.fontWeight = 'normal';
    }
}

// Removed drag and drop functions - using simple file input now

async function uploadCSV() {
    const sessionId = parseInt(document.getElementById('selectedSessionId').value);
    const fileInput = document.getElementById('csvFile');
    
    if (!sessionId) {
        showNotification('Session not selected!', 'error');
        return;
    }
    
    if (!fileInput.files || fileInput.files.length === 0) {
        showNotification('Please select a CSV file!', 'error');
        fileInput.style.borderColor = '#f44336';
        setTimeout(() => {
            fileInput.style.borderColor = '#6C63FF';
        }, 2000);
        return;
    }
    
    const file = fileInput.files[0];
    
    // Validate file type
    if (!file.name.toLowerCase().endsWith('.csv')) {
        showNotification('Please select a CSV file!', 'error');
        return;
    }
    
    // Show loading state
    const uploadButton = document.querySelector('#csvUploadModal button[onclick*="uploadCSV"]');
    const originalButtonText = uploadButton ? uploadButton.innerHTML : '';
    if (uploadButton) {
        uploadButton.disabled = true;
        uploadButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Replacing questions...';
    }
    
    try {
        // Step 1: First, clear all existing questions in this session
        console.log(`🗑️ Clearing existing questions for Session ID: ${sessionId}`);
        
        const clearResponse = await fetch(`${API_BASE_URL}/admin/questions/crud.php`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ session_id: sessionId })
        });
        
        const clearResult = await clearResponse.json();
        console.log('🗑️ Clear response:', clearResult);
        
        if (!clearResult.success) {
            console.warn('Failed to clear previous questions, continuing with upload...');
        }
        
        // Step 2: Upload new questions from CSV
        const formData = new FormData();
        formData.append('csv_file', file);
        formData.append('session_id', sessionId);
        
        console.log(`📁 Uploading CSV file to server for Session ID: ${sessionId}`);
        
        const response = await fetch(`${API_BASE_URL}/admin/questions/upload_csv.php`, {
            method: 'POST',
            body: formData
        });
        
        const result = await response.json();
        
        console.log('📥 Upload response:', result);
        
        if (result.success) {
            const addedCount = result.added || 0;
            const skippedCount = result.skipped || 0;
            
            // Refresh session cards to show updated question counts
            await loadSessionCards();
            
            // Close modal
            closeModal('csvUploadModal');
            
            // Reset file input
            fileInput.value = '';
            const fileNameDisplay = document.getElementById('fileName');
            if (fileNameDisplay) {
                fileNameDisplay.textContent = 'No file selected';
                fileNameDisplay.style.color = '#666';
                fileNameDisplay.style.fontWeight = 'normal';
            }
            
            // Show success message with detailed errors
            if (addedCount > 0 && skippedCount === 0) {
                showNotification(`✅ Successfully replaced with ${addedCount} new questions!`, 'success');
            } else if (addedCount > 0 && skippedCount > 0) {
                let errorMsg = `⚠️ Uploaded ${addedCount} questions, skipped ${skippedCount} invalid rows.`;
                if (result.errors && result.errors.length > 0) {
                    errorMsg += '\n\nErrors:\n' + result.errors.slice(0, 5).join('\n');
                    if (result.errors.length > 5) {
                        errorMsg += `\n... and ${result.errors.length - 5} more errors`;
                    }
                }
                showNotification(errorMsg, 'success');
                console.warn('CSV Upload Errors:', result.errors);
            } else {
                let errorMsg = `❌ Failed to upload any questions. ${skippedCount} rows had errors.`;
                if (result.errors && result.errors.length > 0) {
                    errorMsg += '\n\nErrors:\n' + result.errors.join('\n');
                }
                showNotification(errorMsg, 'error');
                console.error('CSV Upload Errors:', result.errors);
                
                // Also show errors in an alert for better visibility
                if (result.errors && result.errors.length > 0) {
                    setTimeout(() => {
                        alert('Upload Errors:\n\n' + result.errors.join('\n'));
                    }, 500);
                }
            }
        } else {
            let errorMsg = result.message || 'Failed to upload CSV file';
            if (result.error) {
                errorMsg += ': ' + result.error;
            }
            showNotification('❌ ' + errorMsg, 'error');
            console.error('Upload error:', result);
        }
    } catch (error) {
        console.error('CSV upload error:', error);
        let errorMsg = 'Error uploading CSV file: ' + error.message;
        
        // Check if it's a network error
        if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError')) {
            errorMsg = '❌ Cannot connect to server. Make sure XAMPP Apache is running!';
        }
        
        showNotification(errorMsg, 'error');
    } finally {
        // Restore button state
        if (uploadButton) {
            uploadButton.disabled = false;
            uploadButton.innerHTML = originalButtonText;
        }
    }
}

function parseCSVLine(line) {
    const result = [];
    let current = '';
    let inQuotes = false;
    
    for (let i = 0; i < line.length; i++) {
        const char = line[i];
        
        if (char === '"') {
            inQuotes = !inQuotes;
        } else if (char === ',' && !inQuotes) {
            result.push(current.trim());
            current = '';
        } else {
            current += char;
        }
    }
    result.push(current.trim());
    
    return result;
}

function downloadCSVTemplate() {
    // Fetch the demo CSV file from the server
    fetch('demo_questions.csv')
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch template');
            }
            return response.text();
        })
        .then(csvContent => {
            const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'demo_questions_template.csv';
            a.click();
            window.URL.revokeObjectURL(url);
            
            showNotification('✅ Demo CSV template downloaded! It includes 10 sample questions (bilingual, English-only, and Tamil-only).', 'success');
        })
        .catch(error => {
            console.error('Error downloading template:', error);
            // Fallback to inline template
            const csvContent = `questionEn,questionTa,optionAEn,optionATa,optionBEn,optionBTa,optionCEn,optionCTa,optionDEn,optionDTa,correctAnswer,explanationEn,explanationTa
"What is the capital of Tamil Nadu?","தமிழ்நாட்டின் தலைநகரம் எது?","Chennai","சென்னை","Mumbai","மும்பை","Delhi","டெல்லி","Kolkata","கொல்கத்தா","A","Chennai is the capital of Tamil Nadu","சென்னை தமிழ்நாட்டின் தலைநகரம்"
"What is 2+2?","","4","","3","","5","","6","","A","2+2 equals 4",""
"Who wrote Thirukkural?","திருக்குறள் எழுதியவர் யார்?","Thiruvalluvar","திருவள்ளுவர்","Kambar","கம்பர்","Bharathi","பாரதி","Ilango","இளங்கோ","A","Thiruvalluvar wrote Thirukkural","திருவள்ளுவர் திருக்குறளை எழுதினார்"
"","தமிழ்நாட்டின் முதல்வர் யார்?","","எம்.கே.ஸ்டாலின்","","எடப்பாடி பழனிசாமி","","ஜெயலலிதா","","என்.டி.ராமராவ்","A","","எம்.கே.ஸ்டாலின் தமிழ்நாட்டின் முதல்வர்"`;
            
            const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'questions_template.csv';
            a.click();
            window.URL.revokeObjectURL(url);
            
            showNotification('✅ CSV template downloaded!', 'success');
        });
}

// Test Results Management - API Integration
async function loadTestResultsFromAPI() {
    try {
        console.log('📊 Loading test results from API...');
        
        // Build query parameters from filters
        const params = new URLSearchParams();
        const filterUser = document.getElementById('filterUser')?.value;
        const filterExam = document.getElementById('filterExamCategory')?.value;
        const filterStatus = document.getElementById('filterStatus')?.value;
        const filterDateRange = document.getElementById('filterDateRange')?.value;
        
        if (filterUser && filterUser !== 'all') {
            // Get user ID from user name
            const user = users.find(u => u.name === filterUser);
            if (user) params.append('user_id', user.id);
        }
        
        // Handle date range filter
        if (filterDateRange && filterDateRange !== 'all') {
            const today = new Date();
            if (filterDateRange === 'today') {
                const todayStr = today.toISOString().split('T')[0];
                params.append('date_from', todayStr);
                params.append('date_to', todayStr);
            } else if (filterDateRange === 'week') {
                const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);
                params.append('date_from', weekAgo.toISOString().split('T')[0]);
            } else if (filterDateRange === 'month') {
                const monthAgo = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000);
                params.append('date_from', monthAgo.toISOString().split('T')[0]);
            }
        }
        
        // Handle status filter
        if (filterStatus && filterStatus !== 'all') {
            if (filterStatus === 'passed') {
                params.append('min_score', '50');
            } else if (filterStatus === 'failed') {
                params.append('max_score', '49.99');
            }
        }
        
        params.append('limit', '100');
        params.append('offset', '0');
        
        const response = await fetch(`${API_BASE_URL}/admin/results/list.php?${params.toString()}`);
        const data = await response.json();
        
        if (data.success) {
            testResults = data.results || [];
            console.log(`✅ Loaded ${testResults.length} test results from API`);
            
            // Update stats
            const totalAttempts = data.total || testResults.length;
            const avgScore = testResults.length > 0 
                ? (testResults.reduce((sum, r) => sum + (r.percentage || 0), 0) / testResults.length).toFixed(1)
                : '0.0';
            const passed = testResults.filter(r => (r.percentage || 0) >= 50).length;
            const failed = testResults.filter(r => (r.percentage || 0) < 50).length;
            
            if (document.getElementById('totalTestAttempts')) {
                document.getElementById('totalTestAttempts').textContent = totalAttempts;
            }
            if (document.getElementById('avgTestScore')) {
                document.getElementById('avgTestScore').textContent = avgScore + '%';
            }
            if (document.getElementById('testsPassedCount')) {
                document.getElementById('testsPassedCount').textContent = passed;
            }
            if (document.getElementById('testsFailedCount')) {
                document.getElementById('testsFailedCount').textContent = failed;
            }
            
            // Populate user filter
            const filterUserSelect = document.getElementById('filterUser');
            if (filterUserSelect) {
                // Load users first if not loaded
                if (users.length === 0) {
                    await loadUsers();
                }
                filterUserSelect.innerHTML = '<option value="all">All Users</option>';
                users.forEach(user => {
                    filterUserSelect.innerHTML += `<option value="${user.name}">${user.name}</option>`;
                });
            }
            
            // Display results
            displayTestResults(testResults);
        } else {
            console.error('❌ Failed to load test results:', data.message);
            showNotification('Failed to load test results: ' + (data.message || 'Unknown error'), 'error');
        }
    } catch (error) {
        console.error('❌ Error loading test results:', error);
        showNotification('Error loading test results: ' + error.message, 'error');
    }
}

async function loadTestResultsAnalytics() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/results/analytics.php`);
        const data = await response.json();
        
        if (data.success) {
            console.log('✅ Loaded test results analytics');
            // Analytics data is available in data.overall, data.categories, etc.
            // Can be used for dashboard or detailed analytics view
        }
    } catch (error) {
        console.error('❌ Error loading analytics:', error);
    }
}

// Keep old function name for backward compatibility
function loadTestResults() {
    loadTestResultsFromAPI();
}

function displayTestResults(results) {
    const tbody = document.getElementById('testResultsTableBody');
    
    if (!tbody) return;
    
    if (results.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="10" style="text-align: center; padding: 40px;">
                    <i class="fas fa-inbox" style="font-size: 48px; color: #ddd; margin-bottom: 15px; display: block;"></i>
                    <h4 style="color: #999; margin: 0;">No Test Results Found</h4>
                    <p style="color: #bbb;">Try changing the filters or check back later.</p>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = results.map(result => {
        const percentage = result.percentage || 0;
        const status = percentage >= 50 ? 'passed' : 'failed';
        const statusColor = status === 'passed' ? '#4CAF50' : '#FF6B6B';
        const statusIcon = status === 'passed' ? 'check-circle' : 'times-circle';
        const submittedDate = result.submitted_at ? new Date(result.submitted_at).toLocaleDateString() : 'N/A';
        const timeTaken = result.time_taken ? Math.round(result.time_taken / 60) : 0; // Convert seconds to minutes
        
        return `
            <tr style="border-bottom: 1px solid #f0f0f0;">
                <td style="padding: 15px;"><strong>#${result.id}</strong></td>
                <td style="padding: 15px;">
                    <div>
                        <strong style="color: #333;">${result.user_name || 'Unknown'}</strong><br>
                        <small style="color: #999;">${result.user_mobile || ''}</small>
                    </div>
                </td>
                <td style="padding: 15px;">
                    <span class="badge badge-primary" style="padding: 6px 12px;">${result.exam_name || 'N/A'}</span>
                </td>
                <td style="padding: 15px;">
                    <div>
                        <strong style="color: #333;">${result.test_name || 'Unknown Test'}</strong><br>
                        <small style="color: #999;">${result.category_name || 'N/A'}</small>
                    </div>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <strong style="font-size: 16px; color: #6C63FF;">${result.correct_answers || 0}/${result.total_questions || 0}</strong>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <div style="display: flex; flex-direction: column; align-items: center;">
                        <strong style="font-size: 18px; color: ${statusColor};">${percentage.toFixed(1)}%</strong>
                        <div style="width: 60px; height: 6px; background: #e0e0e0; border-radius: 10px; margin-top: 5px; overflow: hidden;">
                            <div style="width: ${Math.min(percentage, 100)}%; height: 100%; background: ${statusColor}; border-radius: 10px;"></div>
                        </div>
                    </div>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <span style="color: #666;"><i class="fas fa-clock"></i> ${timeTaken} min</span>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <span style="color: #999; font-size: 12px;">${submittedDate}</span>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <span style="background: ${statusColor}20; color: ${statusColor}; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;">
                        <i class="fas fa-${statusIcon}"></i> ${status.toUpperCase()}
                    </span>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <button class="btn btn-sm btn-primary" onclick="viewAnswerSheet(${result.id})" style="padding: 6px 12px; font-size: 12px;">
                        <i class="fas fa-eye"></i> View
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterTestResults() {
    // Reload from API with current filter values
    loadTestResultsFromAPI();
}

function clearFilters() {
    document.getElementById('filterUser').value = 'all';
    document.getElementById('filterExamCategory').value = 'all';
    document.getElementById('filterStatus').value = 'all';
    document.getElementById('filterDateRange').value = 'all';
    loadTestResults();
}

function viewAnswerSheet(resultId) {
    const result = testResults.find(r => r.id === resultId);
    if (!result) {
        showNotification('Result not found!', 'error');
        return;
    }
    
    const percentage = result.percentage || 0;
    const status = percentage >= 50 ? 'PASSED' : 'FAILED';
    const timeTaken = result.time_taken ? Math.round(result.time_taken / 60) : 0;
    
    // Show detailed result info
    alert(`📋 Test Result Details\n\n` +
          `User: ${result.user_name || 'Unknown'}\n` +
          `Test: ${result.test_name || 'Unknown'}\n` +
          `Category: ${result.category_name || 'N/A'}\n` +
          `Score: ${result.correct_answers || 0}/${result.total_questions || 0}\n` +
          `Percentage: ${percentage.toFixed(1)}%\n` +
          `Time Taken: ${timeTaken} minutes\n` +
          `Status: ${status}\n` +
          `Submitted: ${result.submitted_at ? new Date(result.submitted_at).toLocaleString() : 'N/A'}\n\n` +
          `(Detailed answer sheet with question-by-question breakdown coming soon!)`);
}

async function exportTestResults() {
    try {
        // Build export URL with current filters
        const filterUser = document.getElementById('filterUser')?.value;
        const filterExam = document.getElementById('filterExamCategory')?.value;
        const filterDateRange = document.getElementById('filterDateRange')?.value;
        
        let url = `${API_BASE_URL}/admin/export/reports.php?type=test_results&format=csv`;
        
        // Add date filters
        if (filterDateRange && filterDateRange !== 'all') {
            const today = new Date();
            if (filterDateRange === 'today') {
                const todayStr = today.toISOString().split('T')[0];
                url += `&start_date=${todayStr}&end_date=${todayStr}`;
            } else if (filterDateRange === 'week') {
                const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);
                url += `&start_date=${weekAgo.toISOString().split('T')[0]}`;
            } else if (filterDateRange === 'month') {
                const monthAgo = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000);
                url += `&start_date=${monthAgo.toISOString().split('T')[0]}`;
            }
        }
        
        // Add user filter
        if (filterUser && filterUser !== 'all') {
            const user = users.find(u => u.name === filterUser);
            if (user) {
                url += `&user_id=${user.id}`;
            }
        }
        
        // Trigger download
        window.open(url, '_blank');
        showNotification('✅ Export started! CSV file will download shortly.', 'success');
    } catch (error) {
        console.error('Error exporting results:', error);
        showNotification('❌ Error exporting results: ' + error.message, 'error');
    }
}

// Settings Management
async function loadSettings() {
    try {
        // Load about information
        const aboutResponse = await fetch(`${API_BASE_URL}/admin/settings/crud.php?key=about`);
        const aboutData = await aboutResponse.json();
        if (aboutData.success && aboutData.setting) {
            try {
                const about = JSON.parse(aboutData.setting.setting_value || '{}');
                document.getElementById('aboutAppName').value = about.app_name || '';
                document.getElementById('aboutAppVersion').value = about.app_version || '';
                document.getElementById('aboutDescription').value = about.description || '';
                document.getElementById('aboutContactEmail').value = about.contact_email || '';
                document.getElementById('aboutContactPhone').value = about.contact_phone || '';
            } catch (e) {
                // If not JSON, treat as old format
            }
        }
        
        // Load privacy policy
        const privacyResponse = await fetch(`${API_BASE_URL}/admin/settings/crud.php?key=privacy_policy`);
        const privacyData = await privacyResponse.json();
        if (privacyData.success && privacyData.setting) {
            document.getElementById('privacyPolicyText').value = privacyData.setting.setting_value || '';
        }
        
        // Load terms & conditions
        const termsResponse = await fetch(`${API_BASE_URL}/admin/settings/crud.php?key=terms_conditions`);
        const termsData = await termsResponse.json();
        if (termsData.success && termsData.setting) {
            document.getElementById('termsConditionsText').value = termsData.setting.setting_value || '';
        }

        // Load app update settings
        await loadAppUpdateSettings();
    } catch (error) {
        console.error('Error loading settings:', error);
        // Fallback to default values
        document.getElementById('privacyPolicyText').value = appSettings.privacyPolicy || '';
        document.getElementById('termsConditionsText').value = appSettings.termsConditions || '';
    }
}

function switchSettingsTab(tab) {
    const aboutTab = document.getElementById('aboutTab');
    const privacyTab = document.getElementById('privacyTab');
    const termsTab = document.getElementById('termsTab');
    const updateTab = document.getElementById('updateTab');
    const premiumVideoTab = document.getElementById('premiumVideoTab');
    const aboutSection = document.getElementById('aboutSection');
    const privacySection = document.getElementById('privacySection');
    const termsSection = document.getElementById('termsSection');
    const updateSection = document.getElementById('updateSection');
    const premiumVideoSection = document.getElementById('premiumVideoSection');
    
    // Remove active class from all tabs
    if (aboutTab) aboutTab.classList.remove('active');
    if (privacyTab) privacyTab.classList.remove('active');
    if (termsTab) termsTab.classList.remove('active');
    if (updateTab) updateTab.classList.remove('active');
    if (premiumVideoTab) premiumVideoTab.classList.remove('active');
    if (aboutTab) aboutTab.style.borderBottom = '3px solid transparent';
    if (privacyTab) privacyTab.style.borderBottom = '3px solid transparent';
    if (termsTab) termsTab.style.borderBottom = '3px solid transparent';
    if (updateTab) updateTab.style.borderBottom = '3px solid transparent';
    if (premiumVideoTab) premiumVideoTab.style.borderBottom = '3px solid transparent';
    
    // Hide all sections
    if (aboutSection) aboutSection.style.display = 'none';
    if (privacySection) privacySection.style.display = 'none';
    if (termsSection) termsSection.style.display = 'none';
    if (updateSection) updateSection.style.display = 'none';
    if (premiumVideoSection) premiumVideoSection.style.display = 'none';
    
    // Show selected tab and section
    if (tab === 'about' && aboutTab && aboutSection) {
        aboutTab.classList.add('active');
        aboutTab.style.borderBottom = '3px solid #6C63FF';
        aboutSection.style.display = 'block';
    } else if (tab === 'privacy' && privacyTab && privacySection) {
        privacyTab.classList.add('active');
        privacyTab.style.borderBottom = '3px solid #6C63FF';
        privacySection.style.display = 'block';
    } else if (tab === 'terms' && termsTab && termsSection) {
        termsTab.classList.add('active');
        termsTab.style.borderBottom = '3px solid #6C63FF';
        termsSection.style.display = 'block';
    } else if (tab === 'update' && updateTab && updateSection) {
        updateTab.classList.add('active');
        updateTab.style.borderBottom = '3px solid #6C63FF';
        updateSection.style.display = 'block';
    } else if (tab === 'premium_video' && premiumVideoTab && premiumVideoSection) {
        premiumVideoTab.classList.add('active');
        premiumVideoTab.style.borderBottom = '3px solid #6C63FF';
        premiumVideoSection.style.display = 'block';
        loadPremiumVideo();
    }
}

// App Update Settings
async function loadAppUpdateSettings() {
    try {
        const latestRes = await fetch(`${API_BASE_URL}/admin/settings/crud.php?key=app_latest_version`);
        const latestData = await latestRes.json();
        if (latestData.success && latestData.setting) {
            const el = document.getElementById('updateLatestVersion');
            if (el) el.value = latestData.setting.setting_value || '';
        }

        const urlRes = await fetch(`${API_BASE_URL}/admin/settings/crud.php?key=app_update_url`);
        const urlData = await urlRes.json();
        if (urlData.success && urlData.setting) {
            const el = document.getElementById('updateUrl');
            if (el) el.value = urlData.setting.setting_value || '';
        }

        const msgRes = await fetch(`${API_BASE_URL}/admin/settings/crud.php?key=app_update_message`);
        const msgData = await msgRes.json();
        if (msgData.success && msgData.setting) {
            const el = document.getElementById('updateMessage');
            if (el) el.value = msgData.setting.setting_value || '';
        }

        const forceRes = await fetch(`${API_BASE_URL}/admin/settings/crud.php?key=app_force_update`);
        const forceData = await forceRes.json();
        if (forceData.success && forceData.setting) {
            const v = (forceData.setting.setting_value || '').toString().toLowerCase();
            const isTrue = v === '1' || v === 'true' || v === 'yes' || v === 'on';
            const el = document.getElementById('updateForce');
            if (el) el.checked = isTrue;
        }
    } catch (e) {
        console.error('Error loading app update settings:', e);
    }
}

async function saveAppUpdateSettings() {
    const latestVersion = (document.getElementById('updateLatestVersion')?.value || '').trim();
    const updateUrl = (document.getElementById('updateUrl')?.value || '').trim();
    const updateMessage = (document.getElementById('updateMessage')?.value || '').trim();
    const forceUpdate = document.getElementById('updateForce')?.checked ? '1' : '0';

    if (!latestVersion) {
        showNotification('Latest version cannot be empty!', 'error');
        return;
    }

    try {
        const save = async (key, value, description) => {
            const response = await fetch(`${API_BASE_URL}/admin/settings/crud.php`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    setting_key: key,
                    setting_value: value,
                    setting_type: 'text',
                    description
                })
            });
            const data = await response.json();
            if (!data.success) throw new Error(data.message || `Failed to save ${key}`);
        };

        await save('app_latest_version', latestVersion, 'Latest app version for update prompt');
        await save('app_update_url', updateUrl, 'App update link (Play Store / Website)');
        await save('app_update_message', updateMessage, 'Update message shown in the app');
        await save('app_force_update', forceUpdate, 'Force update (1/0)');

        showNotification('✅ App update settings saved successfully!', 'success');
    } catch (error) {
        console.error('Error saving app update settings:', error);
        showNotification('❌ Error saving app update settings: ' + error.message, 'error');
    }
}

async function savePrivacyPolicy() {
    const privacyText = document.getElementById('privacyPolicyText').value;
    
    if (!privacyText.trim()) {
        showNotification('Privacy policy cannot be empty!', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/settings/crud.php`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                setting_key: 'privacy_policy',
                setting_value: privacyText,
                setting_type: 'text',
                description: 'Privacy Policy for the mobile app'
            })
        });
        
        const data = await response.json();
        if (data.success) {
            showNotification('✅ Privacy policy saved successfully!', 'success');
            appSettings.privacyPolicy = privacyText;
        } else {
            showNotification('❌ Failed to save: ' + (data.message || 'Unknown error'), 'error');
        }
    } catch (error) {
        console.error('Error saving privacy policy:', error);
        showNotification('❌ Error saving privacy policy: ' + error.message, 'error');
    }
}

async function saveAbout() {
    const appName = document.getElementById('aboutAppName').value;
    const appVersion = document.getElementById('aboutAppVersion').value;
    const description = document.getElementById('aboutDescription').value;
    const contactEmail = document.getElementById('aboutContactEmail').value;
    const contactPhone = document.getElementById('aboutContactPhone').value;
    
    if (!appName.trim()) {
        showNotification('App name cannot be empty!', 'error');
        return;
    }
    
    try {
        const aboutData = {
            app_name: appName,
            app_version: appVersion,
            description: description,
            contact_email: contactEmail,
            contact_phone: contactPhone
        };
        
        const response = await fetch(`${API_BASE_URL}/admin/settings/crud.php`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                setting_key: 'about',
                setting_value: JSON.stringify(aboutData),
                setting_type: 'json',
                description: 'About information for the mobile app'
            })
        });
        
        const data = await response.json();
        if (data.success) {
            showNotification('✅ About information saved successfully!', 'success');
        } else {
            showNotification('❌ Failed to save: ' + (data.message || 'Unknown error'), 'error');
        }
    } catch (error) {
        console.error('Error saving about:', error);
        showNotification('❌ Error saving about: ' + error.message, 'error');
    }
}

async function saveTermsConditions() {
    const termsText = document.getElementById('termsConditionsText').value;
    
    if (!termsText.trim()) {
        showNotification('Terms & conditions cannot be empty!', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/settings/crud.php`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                setting_key: 'terms_conditions',
                setting_value: termsText,
                setting_type: 'text',
                description: 'Terms & Conditions for the mobile app'
            })
        });
        
        const data = await response.json();
        if (data.success) {
            showNotification('✅ Terms & conditions saved successfully!', 'success');
            appSettings.termsConditions = termsText;
        } else {
            showNotification('❌ Failed to save: ' + (data.message || 'Unknown error'), 'error');
        }
    } catch (error) {
        console.error('Error saving terms & conditions:', error);
        showNotification('❌ Error saving terms & conditions: ' + error.message, 'error');
    }
}

// Premium Video Management
let selectedPremiumVideoFile = null;

async function loadPremiumVideo() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/settings/upload_premium_video.php`, {
            method: 'GET'
        });
        const data = await response.json();
        
        if (data.success && data.data) {
            const videoData = data.data;
            const currentSection = document.getElementById('premiumVideoCurrentSection');
            const preview = document.getElementById('premiumVideoPreview');
            
            // Build video URL
            const videoUrl = `${API_BASE_URL}/../${videoData.path}`;
            preview.src = videoUrl;
            
            // Update metadata
            document.getElementById('premiumVideoCurrentName').textContent = videoData.filename || '-';
            document.getElementById('premiumVideoCurrentSize').textContent = formatFileSize(videoData.size || 0);
            document.getElementById('premiumVideoCurrentDate').textContent = videoData.uploaded_at || '-';
            document.getElementById('premiumVideoCurrentVersion').textContent = videoData.version || '-';
            
            currentSection.style.display = 'block';
        } else {
            document.getElementById('premiumVideoCurrentSection').style.display = 'none';
        }
    } catch (error) {
        console.error('Error loading premium video:', error);
        document.getElementById('premiumVideoCurrentSection').style.display = 'none';
    }
}

function handlePremiumVideoSelect(input) {
    if (input.files && input.files[0]) {
        const file = input.files[0];
        
        // Validate file size (100MB max)
        if (file.size > 100 * 1024 * 1024) {
            showNotification('❌ File is too large. Maximum size is 100MB.', 'error');
            input.value = '';
            return;
        }
        
        // Validate file type
        const allowedTypes = ['video/mp4', 'video/webm', 'video/quicktime'];
        if (!allowedTypes.includes(file.type)) {
            showNotification('❌ Invalid file type. Please upload MP4, WebM, or MOV.', 'error');
            input.value = '';
            return;
        }
        
        selectedPremiumVideoFile = file;
        
        // Show selected file info
        document.getElementById('premiumVideoFileName').textContent = file.name;
        document.getElementById('premiumVideoFileSize').textContent = formatFileSize(file.size);
        document.getElementById('premiumVideoSelected').style.display = 'block';
        document.getElementById('premiumVideoUploadBtn').style.display = 'block';
    }
}

function clearPremiumVideoSelection() {
    selectedPremiumVideoFile = null;
    document.getElementById('premiumVideoFile').value = '';
    document.getElementById('premiumVideoSelected').style.display = 'none';
    document.getElementById('premiumVideoUploadBtn').style.display = 'none';
}

async function uploadPremiumVideo() {
    if (!selectedPremiumVideoFile) {
        showNotification('❌ Please select a video file first.', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('video', selectedPremiumVideoFile);
    
    // Show progress
    document.getElementById('premiumVideoUploadBtn').style.display = 'none';
    document.getElementById('premiumVideoProgress').style.display = 'block';
    
    try {
        const xhr = new XMLHttpRequest();
        
        xhr.upload.addEventListener('progress', (e) => {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100);
                document.getElementById('premiumVideoProgressBar').style.width = percent + '%';
                document.getElementById('premiumVideoProgressText').textContent = percent + '%';
            }
        });
        
        xhr.onload = function() {
            document.getElementById('premiumVideoProgress').style.display = 'none';
            
            try {
                const data = JSON.parse(xhr.responseText);
                if (data.success) {
                    showNotification('✅ Premium video uploaded successfully!', 'success');
                    clearPremiumVideoSelection();
                    loadPremiumVideo();
                } else {
                    showNotification('❌ Upload failed: ' + (data.message || 'Unknown error'), 'error');
                    document.getElementById('premiumVideoUploadBtn').style.display = 'block';
                }
            } catch (e) {
                showNotification('❌ Upload failed: Invalid server response', 'error');
                document.getElementById('premiumVideoUploadBtn').style.display = 'block';
            }
        };
        
        xhr.onerror = function() {
            document.getElementById('premiumVideoProgress').style.display = 'none';
            document.getElementById('premiumVideoUploadBtn').style.display = 'block';
            showNotification('❌ Network error during upload', 'error');
        };
        
        xhr.open('POST', `${API_BASE_URL}/admin/settings/upload_premium_video.php`, true);
        xhr.send(formData);
        
    } catch (error) {
        document.getElementById('premiumVideoProgress').style.display = 'none';
        document.getElementById('premiumVideoUploadBtn').style.display = 'block';
        console.error('Error uploading premium video:', error);
        showNotification('❌ Error uploading video: ' + error.message, 'error');
    }
}

async function deletePremiumVideo() {
    if (!confirm('Are you sure you want to delete the premium video? The app will use its default video until you upload a new one.')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/settings/upload_premium_video.php`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        if (data.success) {
            showNotification('✅ Premium video deleted successfully!', 'success');
            document.getElementById('premiumVideoCurrentSection').style.display = 'none';
            document.getElementById('premiumVideoPreview').src = '';
        } else {
            showNotification('❌ Failed to delete: ' + (data.message || 'Unknown error'), 'error');
        }
    } catch (error) {
        console.error('Error deleting premium video:', error);
        showNotification('❌ Error deleting video: ' + error.message, 'error');
    }
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Results
function loadResults() {
    const tbody = document.getElementById('resultsTableBody');
    tbody.innerHTML = results.map(r => `
        <tr>
            <td>#R${r.id.toString().padStart(3, '0')}</td>
            <td>${r.userName}</td>
            <td>${r.testName}</td>
            <td>${r.score}/100</td>
            <td>${r.percentage}%</td>
            <td><span class="badge badge-${r.status === 'passed' ? 'success' : 'danger'}">${r.status}</span></td>
            <td>
                <button class="btn-icon btn-view" onclick="viewResult(${r.id})"><i class="fas fa-eye"></i></button>
                <button class="btn-icon btn-delete" onclick="deleteResult(${r.id})"><i class="fas fa-trash"></i></button>
            </td>
        </tr>
    `).join('');
}

function viewResult(id) {
    const r = results.find(res => res.id === id);
    alert(`Test Result:\n\nUser: ${r.userName}\nTest: ${r.testName}\nScore: ${r.score}/100\nPercentage: ${r.percentage}%\nStatus: ${r.status}`);
}

function deleteResult(id) {
    if (confirm('Are you sure you want to delete this result?')) {
        results = results.filter(r => r.id !== id);
        loadResults();
        showNotification('Result deleted successfully!', 'success');
    }
}

// Modal Functions
function openModal(modalId) {
    // Special handling for exam category modal - reset to add mode only if not editing
    if (modalId === 'examCategoryModal') {
        const form = document.getElementById('examCategoryForm');
        const editId = form.getAttribute('data-edit-id');
        
        // Only reset if not in edit mode
        if (!editId || editId === '') {
            form.reset();
            document.querySelector('#examCategoryModal h2').textContent = 'Add Exam Category';
        }
    }
    
    // Special handling for language modal
    if (modalId === 'languageModal') {
        const form = document.getElementById('languageForm');
        const editId = form.getAttribute('data-edit-id');
        
        // Only reset if not in edit mode
        if (!editId || editId === '') {
            form.reset();
            document.querySelector('#languageModal h2').textContent = 'Add Language';
            populateExamCategoryDropdown(); // Populate dropdown when opening
        }
    }
    
    document.getElementById(modalId).classList.add('active');
}

function closeModal(modalId) {
    // Reset exam category form when closing the modal
    if (modalId === 'examCategoryModal') {
        const form = document.getElementById('examCategoryForm');
        form.reset();
        form.setAttribute('data-edit-id', '');
        document.querySelector('#examCategoryModal h2').textContent = 'Add Exam Category';
    }
    
    // Reset language form when closing the modal
    if (modalId === 'languageModal') {
        const form = document.getElementById('languageForm');
        form.reset();
        form.setAttribute('data-edit-id', '');
        document.querySelector('#languageModal h2').textContent = 'Add Language';
    }
    
    document.getElementById(modalId).classList.remove('active');
}

// Close modal on outside click
window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
        const modalId = event.target.id;
        
        // Reset exam category form if closing that modal
        if (modalId === 'examCategoryModal') {
            const form = document.getElementById('examCategoryForm');
            form.reset();
            form.setAttribute('data-edit-id', '');
            document.querySelector('#examCategoryModal h2').textContent = 'Add Exam Category';
        }
        
        // Reset language form if closing that modal
        if (modalId === 'languageModal') {
            const form = document.getElementById('languageForm');
            form.reset();
            form.setAttribute('data-edit-id', '');
            document.querySelector('#languageModal h2').textContent = 'Add Language';
        }
        
        event.target.classList.remove('active');
    }
}

// Notification
function showNotification(message, type) {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
        <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-times-circle'}"></i>
        <span>${message}</span>
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideIn 0.3s reverse';
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    }, 3000);
}

// User Rankings Functions
async function loadRankingsFromAPI() {
    try {
        console.log('📊 Loading rankings from API...');
        
        const response = await fetch(`${API_BASE_URL}/tests/get_rankings.php?limit=100`);
        const data = await response.json();
        
        if (data.success && data.rankings) {
            // Fetch users to get additional details (email, mobile, exam, language)
            const usersResponse = await fetch(`${API_BASE_URL}/admin/users/list.php`);
            const usersData = await usersResponse.json();
            const usersMap = {};
            
            if (usersData.success && usersData.users) {
                usersData.users.forEach(user => {
                    usersMap[user.id] = user;
                });
            }
            
            // Map API rankings to expected format
            userRankings = data.rankings.map(ranking => {
                const user = usersMap[ranking.user_id] || {};
                
                return {
                    id: ranking.user_id,
                    name: ranking.user_name || user.name || 'Unknown',
                    email: user.email || 'N/A',
                    mobile: user.mobile || 'N/A',
                    exam: user.exam_category_name || 'N/A',
                    language: user.language || 'en',
                    testsTaken: ranking.total_tests || 0,
                    avgScore: parseFloat(ranking.avg_score) || 0,
                    highestScore: parseFloat(ranking.best_score) || 0,
                    currentStreak: 0, // Not available in API, set to 0
                    studyHours: 0, // Not available in API, set to 0
                    totalPoints: (ranking.total_tests || 0) * (parseFloat(ranking.avg_score) || 0),
                    rank: ranking.rank
                };
            });
            
            // Already sorted by API, but ensure sorting by avg_score
            userRankings.sort((a, b) => b.avgScore - a.avgScore || b.totalPoints - a.totalPoints);
            
            console.log(`✅ Loaded ${userRankings.length} rankings from API`);
            return true;
        } else {
            console.error('Failed to load rankings:', data.message);
            showNotification('Failed to load rankings: ' + (data.message || 'Unknown error'), 'error');
            return false;
        }
    } catch (error) {
        console.error('Error loading rankings:', error);
        showNotification('Error loading rankings: ' + error.message, 'error');
        return false;
    }
}

async function loadRankings() {
    // Load from API instead of generating mock data
    const success = await loadRankingsFromAPI();
    
    if (!success || userRankings.length === 0) {
        // Show empty state
        document.getElementById('rank1Name').textContent = '-';
        document.getElementById('rank1Score').textContent = '-';
        document.getElementById('rank1Tests').textContent = '- tests';
        document.getElementById('rank2Name').textContent = '-';
        document.getElementById('rank2Score').textContent = '-';
        document.getElementById('rank2Tests').textContent = '- tests';
        document.getElementById('rank3Name').textContent = '-';
        document.getElementById('rank3Score').textContent = '-';
        document.getElementById('rank3Tests').textContent = '- tests';
        
        document.getElementById('totalRankedUsers').textContent = '0';
        document.getElementById('avgRankScore').textContent = '0%';
        document.getElementById('totalTestsTaken').textContent = '0';
        document.getElementById('topPerformerStreak').textContent = '0 days';
        
        const tbody = document.getElementById('rankingsTableBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 40px; color: #999;">No rankings available yet.</td></tr>';
        }
        return;
    }
    
    // Load top 3
    if (userRankings.length >= 1) {
        document.getElementById('rank1Name').textContent = userRankings[0].name;
        document.getElementById('rank1Score').textContent = userRankings[0].avgScore + '%';
        document.getElementById('rank1Tests').textContent = userRankings[0].testsTaken + ' tests';
    }
    
    if (userRankings.length >= 2) {
        document.getElementById('rank2Name').textContent = userRankings[1].name;
        document.getElementById('rank2Score').textContent = userRankings[1].avgScore + '%';
        document.getElementById('rank2Tests').textContent = userRankings[1].testsTaken + ' tests';
    }
    
    if (userRankings.length >= 3) {
        document.getElementById('rank3Name').textContent = userRankings[2].name;
        document.getElementById('rank3Score').textContent = userRankings[2].avgScore + '%';
        document.getElementById('rank3Tests').textContent = userRankings[2].testsTaken + ' tests';
    }
    
    // Calculate statistics
    const totalUsers = userRankings.length;
    const avgScore = totalUsers > 0 ? (userRankings.reduce((sum, user) => sum + user.avgScore, 0) / totalUsers).toFixed(1) : '0';
    const totalTests = userRankings.reduce((sum, user) => sum + user.testsTaken, 0);
    const streaks = userRankings.map(user => user.currentStreak || 0).filter(s => s > 0);
    const highestStreak = streaks.length > 0 ? Math.max(...streaks) : 0;
    
    document.getElementById('totalRankedUsers').textContent = totalUsers;
    document.getElementById('avgRankScore').textContent = avgScore + '%';
    document.getElementById('totalTestsTaken').textContent = totalTests;
    document.getElementById('topPerformerStreak').textContent = highestStreak > 0 ? highestStreak + ' days' : '-';
    
    // Load full rankings table
    displayRankings(userRankings);
}

function displayRankings(rankings) {
    const tbody = document.getElementById('rankingsTableBody');
    tbody.innerHTML = rankings.map((user, index) => {
        const rank = user.rank || (index + 1);
        let rankBadge = '';
        
        if (rank === 1) {
            rankBadge = '<span style="font-size: 24px;">🥇</span>';
        } else if (rank === 2) {
            rankBadge = '<span style="font-size: 24px;">🥈</span>';
        } else if (rank === 3) {
            rankBadge = '<span style="font-size: 24px;">🥉</span>';
        } else {
            rankBadge = `<strong style="font-size: 18px;">#${rank}</strong>`;
        }
        
        const rowStyle = rank <= 3 ? 'background: linear-gradient(90deg, rgba(255,215,0,0.1) 0%, rgba(255,255,255,1) 20%);' : '';
        
        return `
            <tr style="${rowStyle} border-bottom: 1px solid #f0f0f0;">
                <td style="text-align: center; padding: 15px;">
                    ${rankBadge}
                </td>
                <td style="padding: 15px;">
                    <div>
                        <strong style="font-size: 16px; color: #333;">${user.name}</strong><br>
                        <small style="color: #999;">${user.email}</small>
                    </div>
                </td>
                <td style="padding: 15px;">
                    <span class="badge badge-primary" style="padding: 6px 12px; border-radius: 15px;">${user.exam}</span>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <strong style="font-size: 16px; color: #6C63FF;">${user.testsTaken}</strong>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <div style="display: flex; flex-direction: column; align-items: center;">
                        <strong style="font-size: 18px; color: ${user.avgScore >= 80 ? '#4CAF50' : user.avgScore >= 60 ? '#FFD93D' : '#FF6B6B'};">${user.avgScore}%</strong>
                        <div style="width: 60px; height: 6px; background: #e0e0e0; border-radius: 10px; margin-top: 5px; overflow: hidden;">
                            <div style="width: ${user.avgScore}%; height: 100%; background: linear-gradient(90deg, #6C63FF, #764ba2); border-radius: 10px;"></div>
                        </div>
                    </div>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <strong style="font-size: 16px; color: #4CAF50;">${user.highestScore}%</strong>
                </td>
                <td style="text-align: center; padding: 15px;">
                    ${user.currentStreak > 0 ? `<span style="background: linear-gradient(135deg, #FF6B6B, #FFD93D); color: white; padding: 6px 12px; border-radius: 20px; font-weight: bold;">🔥 ${user.currentStreak} days</span>` : '<span style="color: #999;">-</span>'}
                </td>
                <td style="text-align: center; padding: 15px;">
                    ${user.studyHours > 0 ? `<strong style="color: #666;">${user.studyHours}h</strong>` : '<span style="color: #999;">-</span>'}
                </td>
                <td style="text-align: center; padding: 15px;">
                    <button class="btn-icon btn-view" onclick="viewUser(${user.id})" title="View Details"><i class="fas fa-eye"></i></button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterRankings() {
    const filter = document.getElementById('rankingFilter').value;
    
    let filteredRankings = userRankings;
    
    if (filter !== 'all') {
        const examMap = {
            'group4': 'TNPSC Group 4',
            'group1': 'TNPSC Group 1',
            'group2': 'TNPSC Group 2',
            'vao': 'TNPSC VAO'
        };
        
        filteredRankings = userRankings.filter(user => user.exam === examMap[filter]);
    }
    
    displayRankings(filteredRankings);
    
    // Update statistics
    if (filteredRankings.length > 0) {
        const avgScore = (filteredRankings.reduce((sum, user) => sum + user.avgScore, 0) / filteredRankings.length).toFixed(1);
        const totalTests = filteredRankings.reduce((sum, user) => sum + user.testsTaken, 0);
        const streaks = filteredRankings.map(user => user.currentStreak || 0).filter(s => s > 0);
        const highestStreak = streaks.length > 0 ? Math.max(...streaks) : 0;
        
        document.getElementById('totalRankedUsers').textContent = filteredRankings.length;
        document.getElementById('avgRankScore').textContent = avgScore + '%';
        document.getElementById('totalTestsTaken').textContent = totalTests;
        document.getElementById('topPerformerStreak').textContent = highestStreak > 0 ? highestStreak + ' days' : '-';
        
        // Update top 3
        if (filteredRankings.length >= 1) {
            document.getElementById('rank1Name').textContent = filteredRankings[0].name;
            document.getElementById('rank1Score').textContent = filteredRankings[0].avgScore + '%';
            document.getElementById('rank1Tests').textContent = filteredRankings[0].testsTaken + ' tests';
        } else {
            document.getElementById('rank1Name').textContent = '-';
            document.getElementById('rank1Score').textContent = '-';
            document.getElementById('rank1Tests').textContent = '- tests';
        }
        
        if (filteredRankings.length >= 2) {
            document.getElementById('rank2Name').textContent = filteredRankings[1].name;
            document.getElementById('rank2Score').textContent = filteredRankings[1].avgScore + '%';
            document.getElementById('rank2Tests').textContent = filteredRankings[1].testsTaken + ' tests';
        } else {
            document.getElementById('rank2Name').textContent = '-';
            document.getElementById('rank2Score').textContent = '-';
            document.getElementById('rank2Tests').textContent = '- tests';
        }
        
        if (filteredRankings.length >= 3) {
            document.getElementById('rank3Name').textContent = filteredRankings[2].name;
            document.getElementById('rank3Score').textContent = filteredRankings[2].avgScore + '%';
            document.getElementById('rank3Tests').textContent = filteredRankings[2].testsTaken + ' tests';
        } else {
            document.getElementById('rank3Name').textContent = '-';
            document.getElementById('rank3Score').textContent = '-';
            document.getElementById('rank3Tests').textContent = '- tests';
        }
    }
}

async function exportRankings() {
    try {
        // Ensure rankings are loaded from API
        if (userRankings.length === 0) {
            const success = await loadRankingsFromAPI();
            if (!success) {
                showNotification('Failed to load rankings for export', 'error');
                return;
            }
        }
        
        const filter = document.getElementById('rankingFilter')?.value || 'all';
        let filteredRankings = userRankings;
        
        if (filter !== 'all') {
            const examMap = {
                'group4': 'TNPSC Group 4',
                'group1': 'TNPSC Group 1',
                'group2': 'TNPSC Group 2',
                'vao': 'TNPSC VAO'
            };
            filteredRankings = userRankings.filter(user => user.exam === examMap[filter]);
        }
        
        if (filteredRankings.length === 0) {
            showNotification('No rankings to export', 'info');
            return;
        }
        
        // Create CSV content with real data
        let csvContent = 'Rank,Name,Email,Mobile,Exam Category,Tests Taken,Average Score,Highest Score\n';
        
        filteredRankings.forEach((user, index) => {
            const rank = user.rank || (index + 1);
            csvContent += `${rank},"${user.name}","${user.email}","${user.mobile || 'N/A'}","${user.exam}",${user.testsTaken},${user.avgScore.toFixed(2)}%,${user.highestScore.toFixed(2)}%\n`;
        });
        
        // Download CSV
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `user_rankings_${filter}_${new Date().toISOString().split('T')[0]}.csv`;
        a.click();
        window.URL.revokeObjectURL(url);
        
        showNotification(`✅ Successfully exported ${filteredRankings.length} rankings!`, 'success');
    } catch (error) {
        console.error('Error exporting rankings:', error);
        showNotification('Error exporting rankings: ' + error.message, 'error');
    }
}

// ==================== FEEDBACK MANAGEMENT ====================

let feedbackList = [];
let feedbackCurrentPage = 0;
let feedbackPageSize = 50;
let currentFeedbackDetail = null;
let feedbackSearchTimeout = null;

async function loadFeedback(refresh = false) {
    try {
        // Show loading state
        const tbody = document.getElementById('feedbackTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 40px;">
                        <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #6C63FF;"></i>
                        <p style="margin-top: 10px; color: #666;">Loading feedback...</p>
                    </td>
                </tr>
            `;
        }

        if (refresh) {
            feedbackCurrentPage = 0;
        }

        const status = document.getElementById('feedbackStatusFilter')?.value || '';
        const category = document.getElementById('feedbackCategoryFilter')?.value || '';
        const isArchived = document.getElementById('feedbackArchivedFilter')?.value === 'true';
        const search = document.getElementById('feedbackSearch')?.value || '';

        let url = `${API_BASE_URL}/admin/feedback/crud.php?limit=${feedbackPageSize}&offset=${feedbackCurrentPage * feedbackPageSize}&is_archived=${isArchived}`;
        
        if (status) url += `&status=${status}`;
        if (category) url += `&category=${category}`;
        if (search) url += `&search=${encodeURIComponent(search)}`;

        console.log('Loading feedback from:', url);
        const response = await fetch(url);
        const data = await response.json();

        console.log('Feedback API response:', data);

        if (data.success) {
            feedbackList = data.feedback || [];
            updateFeedbackTable();
            updateFeedbackStats(data.stats || {});
            updateFeedbackPagination(data.total_count || 0);
        } else {
            if (tbody) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="8" style="text-align: center; padding: 40px; color: #f44336;">
                            <i class="fas fa-exclamation-triangle" style="font-size: 24px; margin-bottom: 10px;"></i>
                            <p>Failed to load feedback: ${data.message || 'Unknown error'}</p>
                        </td>
                    </tr>
                `;
            }
            showNotification('Failed to load feedback: ' + (data.message || 'Unknown error'), 'error');
        }
    } catch (error) {
        console.error('Error loading feedback:', error);
        const tbody = document.getElementById('feedbackTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 40px; color: #f44336;">
                        <i class="fas fa-exclamation-triangle" style="font-size: 24px; margin-bottom: 10px;"></i>
                        <p>Error loading feedback: ${error.message}</p>
                    </td>
                </tr>
            `;
        }
        showNotification('Error loading feedback: ' + error.message, 'error');
    }
}

function updateFeedbackTable() {
    const tbody = document.getElementById('feedbackTableBody');
    if (!tbody) return;

    if (feedbackList.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 40px;">
                    <i class="fas fa-inbox" style="font-size: 48px; color: #ccc; margin-bottom: 10px;"></i>
                    <p style="color: #666; margin-top: 10px;">No feedback found</p>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = feedbackList.map(feedback => {
        const date = new Date(feedback.created_at);
        const formattedDate = date.toLocaleDateString('en-US', { 
            year: 'numeric', 
            month: 'short', 
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });

        const statusColors = {
            'pending': '#FF6B6B',
            'reviewed': '#4ECDC4',
            'resolved': '#95E1D3',
            'closed': '#95A5A6'
        };

        const categoryColors = {
            'general': '#6C63FF',
            'bug': '#FF6B6B',
            'feature': '#4ECDC4',
            'complaint': '#FFA500',
            'suggestion': '#95E1D3'
        };

        const ratingStars = feedback.rating ? '★'.repeat(feedback.rating) + '☆'.repeat(5 - feedback.rating) : '-';

        return `
            <tr>
                <td>#${feedback.id}</td>
                <td>
                    <div style="font-weight: 600;">${escapeHtml(feedback.user_name)}</div>
                    ${feedback.user_email ? `<div style="font-size: 12px; color: #666;">${escapeHtml(feedback.user_email)}</div>` : ''}
                    ${feedback.user_mobile ? `<div style="font-size: 12px; color: #666;">${escapeHtml(feedback.user_mobile)}</div>` : ''}
                </td>
                <td>
                    <div style="font-weight: 600; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${escapeHtml(feedback.subject)}">
                        ${escapeHtml(feedback.subject)}
                    </div>
                    <div style="font-size: 12px; color: #666; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${escapeHtml(feedback.message)}">
                        ${escapeHtml(feedback.message.substring(0, 50))}${feedback.message.length > 50 ? '...' : ''}
                    </div>
                </td>
                <td>
                    <span style="background: ${categoryColors[feedback.category] || '#6C63FF'}; color: white; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; text-transform: capitalize;">
                        ${feedback.category}
                    </span>
                </td>
                <td style="color: #FFD700; font-size: 14px;">${ratingStars}</td>
                <td>
                    <span style="background: ${statusColors[feedback.status] || '#95A5A6'}; color: white; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; text-transform: capitalize;">
                        ${feedback.status}
                    </span>
                </td>
                <td style="font-size: 12px; color: #666;">${formattedDate}</td>
                <td>
                    <div style="display: flex; gap: 5px;">
                        <button class="btn-icon" onclick="viewFeedbackDetail(${feedback.id})" title="View Details">
                            <i class="fas fa-eye"></i>
                        </button>
                        ${feedback.status !== 'closed' ? `
                            <button class="btn-icon" onclick="quickUpdateFeedbackStatus(${feedback.id}, 'closed')" title="Close">
                                <i class="fas fa-times-circle"></i>
                            </button>
                        ` : ''}
                        ${!feedback.is_archived ? `
                            <button class="btn-icon" onclick="archiveFeedback(${feedback.id})" title="Archive">
                                <i class="fas fa-archive"></i>
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function updateFeedbackStats(stats) {
    if (document.getElementById('feedbackTotal')) {
        document.getElementById('feedbackTotal').textContent = stats.total || 0;
    }
    if (document.getElementById('feedbackPending')) {
        document.getElementById('feedbackPending').textContent = stats.pending || 0;
    }
    if (document.getElementById('feedbackReviewed')) {
        document.getElementById('feedbackReviewed').textContent = stats.reviewed || 0;
    }
    if (document.getElementById('feedbackResolved')) {
        document.getElementById('feedbackResolved').textContent = stats.resolved || 0;
    }
}

function updateFeedbackPagination(totalCount) {
    const showing = Math.min((feedbackCurrentPage + 1) * feedbackPageSize, totalCount);
    const start = feedbackCurrentPage * feedbackPageSize + 1;
    
    if (document.getElementById('feedbackShowing')) {
        document.getElementById('feedbackShowing').textContent = totalCount > 0 ? `${start}-${showing}` : '0';
    }
    if (document.getElementById('feedbackTotalCount')) {
        document.getElementById('feedbackTotalCount').textContent = totalCount;
    }

    const prevBtn = document.getElementById('feedbackPrevBtn');
    const nextBtn = document.getElementById('feedbackNextBtn');
    
    if (prevBtn) prevBtn.disabled = feedbackCurrentPage === 0;
    if (nextBtn) nextBtn.disabled = showing >= totalCount;
}

function loadFeedbackPage(direction) {
    if (direction === 'prev' && feedbackCurrentPage > 0) {
        feedbackCurrentPage--;
        loadFeedback();
    } else if (direction === 'next') {
        feedbackCurrentPage++;
        loadFeedback();
    }
}

function debounceSearch() {
    clearTimeout(feedbackSearchTimeout);
    feedbackSearchTimeout = setTimeout(() => {
        feedbackCurrentPage = 0;
        loadFeedback();
    }, 500);
}

async function viewFeedbackDetail(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/feedback/crud.php?id=${id}`);
        const data = await response.json();

        if (data.success && data.feedback) {
            currentFeedbackDetail = data.feedback;
            displayFeedbackDetail(data.feedback);
            openModal('feedbackDetailModal');
        } else {
            showNotification('Failed to load feedback details', 'error');
        }
    } catch (error) {
        console.error('Error loading feedback detail:', error);
        showNotification('Error loading feedback details: ' + error.message, 'error');
    }
}

function displayFeedbackDetail(feedback) {
    const content = document.getElementById('feedbackDetailContent');
    const responseBtn = document.getElementById('feedbackResponseBtn');
    
    if (!content) return;

    const date = new Date(feedback.created_at);
    const formattedDate = date.toLocaleDateString('en-US', { 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });

    const statusColors = {
        'pending': '#FF6B6B',
        'reviewed': '#4ECDC4',
        'resolved': '#95E1D3',
        'closed': '#95A5A6'
    };

    const categoryColors = {
        'general': '#6C63FF',
        'bug': '#FF6B6B',
        'feature': '#4ECDC4',
        'complaint': '#FFA500',
        'suggestion': '#95E1D3'
    };

    const ratingStars = feedback.rating ? '★'.repeat(feedback.rating) + '☆'.repeat(5 - feedback.rating) : 'Not rated';

    content.innerHTML = `
        <div style="background: white; padding: 25px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.05);">
            <!-- Header -->
            <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 25px; padding-bottom: 20px; border-bottom: 2px solid #e0e0e0;">
                <div>
                    <h3 style="margin: 0 0 10px 0; color: #333;">${escapeHtml(feedback.subject)}</h3>
                    <div style="display: flex; gap: 10px; flex-wrap: wrap;">
                        <span style="background: ${statusColors[feedback.status] || '#95A5A6'}; color: white; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; text-transform: capitalize;">
                            ${feedback.status}
                        </span>
                        <span style="background: ${categoryColors[feedback.category] || '#6C63FF'}; color: white; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; text-transform: capitalize;">
                            ${feedback.category}
                        </span>
                        ${feedback.rating ? `
                            <span style="color: #FFD700; font-size: 14px; padding: 6px 12px; background: #fff8e1; border-radius: 20px;">
                                ${ratingStars}
                            </span>
                        ` : ''}
                    </div>
                </div>
                <div style="text-align: right; color: #666; font-size: 12px;">
                    <div><i class="fas fa-calendar"></i> ${formattedDate}</div>
                    <div style="margin-top: 5px;"><i class="fas fa-hashtag"></i> #${feedback.id}</div>
                </div>
            </div>

            <!-- User Info -->
            <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
                <h4 style="margin: 0 0 15px 0; color: #333;"><i class="fas fa-user"></i> User Information</h4>
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;">
                    <div>
                        <div style="font-size: 12px; color: #666; margin-bottom: 5px;">Name</div>
                        <div style="font-weight: 600; color: #333;">${escapeHtml(feedback.user_name)}</div>
                    </div>
                    ${feedback.user_email ? `
                        <div>
                            <div style="font-size: 12px; color: #666; margin-bottom: 5px;">Email</div>
                            <div style="font-weight: 600; color: #333;">${escapeHtml(feedback.user_email)}</div>
                        </div>
                    ` : ''}
                    ${feedback.user_mobile ? `
                        <div>
                            <div style="font-size: 12px; color: #666; margin-bottom: 5px;">Mobile</div>
                            <div style="font-weight: 600; color: #333;">${escapeHtml(feedback.user_mobile)}</div>
                        </div>
                    ` : ''}
                    ${feedback.user_id ? `
                        <div>
                            <div style="font-size: 12px; color: #666; margin-bottom: 5px;">User ID</div>
                            <div style="font-weight: 600; color: #333;">#${feedback.user_id}</div>
                        </div>
                    ` : ''}
                </div>
            </div>

            <!-- Message -->
            <div style="margin-bottom: 20px;">
                <h4 style="margin: 0 0 15px 0; color: #333;"><i class="fas fa-comment"></i> Message</h4>
                <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #6C63FF; white-space: pre-wrap; line-height: 1.6;">
                    ${escapeHtml(feedback.message)}
                </div>
            </div>

            <!-- Admin Response -->
            ${feedback.admin_response ? `
                <div style="background: #e8f5e9; padding: 20px; border-radius: 8px; border-left: 4px solid #4CAF50; margin-bottom: 20px;">
                    <h4 style="margin: 0 0 15px 0; color: #2E7D32;"><i class="fas fa-reply"></i> Admin Response</h4>
                    <div style="white-space: pre-wrap; line-height: 1.6; color: #333;">
                        ${escapeHtml(feedback.admin_response)}
                    </div>
                    ${feedback.admin_response_at ? `
                        <div style="margin-top: 10px; font-size: 12px; color: #666;">
                            <i class="fas fa-clock"></i> ${new Date(feedback.admin_response_at).toLocaleString()}
                        </div>
                    ` : ''}
                </div>
            ` : `
                <div style="background: #fff3cd; padding: 15px; border-radius: 8px; border-left: 4px solid #FFC107; margin-bottom: 20px;">
                    <i class="fas fa-info-circle"></i> No admin response yet
                </div>
            `}
        </div>
    `;

    // Show/hide response button
    if (responseBtn) {
        responseBtn.style.display = feedback.status !== 'closed' ? 'inline-block' : 'none';
    }
}

function showFeedbackResponseForm() {
    if (!currentFeedbackDetail) return;
    
    document.getElementById('feedbackResponseId').value = currentFeedbackDetail.id;
    document.getElementById('feedbackResponseMessage').value = '';
    document.getElementById('feedbackResponseStatus').value = currentFeedbackDetail.status;
    
    closeModal('feedbackDetailModal');
    openModal('feedbackResponseModal');
}

async function submitFeedbackResponse() {
    try {
        const id = document.getElementById('feedbackResponseId').value;
        const message = document.getElementById('feedbackResponseMessage').value.trim();
        const status = document.getElementById('feedbackResponseStatus').value;

        if (!message) {
            showNotification('Please enter a response message', 'error');
            return;
        }

        const response = await fetch(`${API_BASE_URL}/admin/feedback/crud.php?id=${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                admin_response: message,
                status: status
            })
        });

        const data = await response.json();

        if (data.success) {
            showNotification('Response sent successfully', 'success');
            closeModal('feedbackResponseModal');
            loadFeedback(true);
        } else {
            showNotification('Failed to send response: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error submitting response:', error);
        showNotification('Error submitting response: ' + error.message, 'error');
    }
}

async function quickUpdateFeedbackStatus(id, status) {
    if (!confirm(`Are you sure you want to mark this feedback as ${status}?`)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/admin/feedback/crud.php?id=${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                status: status
            })
        });

        const data = await response.json();

        if (data.success) {
            showNotification(`Feedback marked as ${status}`, 'success');
            loadFeedback();
        } else {
            showNotification('Failed to update status: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error updating status:', error);
        showNotification('Error updating status: ' + error.message, 'error');
    }
}

async function archiveFeedback(id) {
    if (!confirm('Are you sure you want to archive this feedback?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/admin/feedback/crud.php?id=${id}`, {
            method: 'DELETE'
        });

        const data = await response.json();

        if (data.success) {
            showNotification('Feedback archived successfully', 'success');
            loadFeedback();
        } else {
            showNotification('Failed to archive feedback: ' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error archiving feedback:', error);
        showNotification('Error archiving feedback: ' + error.message, 'error');
    }
}

// Initialize feedback page when shown
const originalShowPage = showPage;
showPage = function(page) {
    originalShowPage(page);
    if (page === 'feedback') {
        loadFeedback(true);
    }
};

console.log('TNPSC Mock Test Admin Panel - Mockup Version');

// ============================================
// IMAGE UPLOAD FUNCTIONALITY FOR TEST CATEGORIES
// ============================================

// Global variable to store uploaded image data
let uploadedImageData = null;

/**
 * Handle image file selection
 */
function handleImageSelect(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    // Validate file type
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml'];
    if (!allowedTypes.includes(file.type)) {
        showNotification('Invalid file type. Please upload an image file (JPG, PNG, GIF, WEBP, SVG)', 'error');
        return;
    }
    
    // Validate file size (5MB)
    const maxSize = 5 * 1024 * 1024;
    if (file.size > maxSize) {
        showNotification('File size exceeds 5MB limit', 'error');
        return;
    }
    
    // Show preview immediately
    const reader = new FileReader();
    reader.onload = function(e) {
        showImagePreview(e.target.result, file);
    };
    reader.readAsDataURL(file);
    
    // Upload the image
    uploadImage(file);
}

/**
 * Show image preview
 */
function showImagePreview(src, file) {
    const imagePreview = document.getElementById('imagePreview');
    const imagePreviewContainer = document.getElementById('imagePreviewContainer');
    const imageUploadArea = document.getElementById('imageUploadArea');
    const imageInfo = document.getElementById('imageInfo');
    
    imagePreview.src = src;
    imagePreviewContainer.style.display = 'block';
    imageUploadArea.style.display = 'none';
    
    // Show file info
    const fileSize = (file.size / 1024).toFixed(2);
    imageInfo.innerHTML = `
        <i class="fas fa-file-image" style="color: #6C63FF;"></i> 
        ${file.name} (${fileSize} KB)
    `;
}

/**
 * Upload image to server
 */
async function uploadImage(file) {
    const uploadProgress = document.getElementById('uploadProgress');
    const uploadProgressBar = document.getElementById('uploadProgressBar');
    const uploadProgressText = document.getElementById('uploadProgressText');
    
    try {
        // Show progress
        uploadProgress.style.display = 'block';
        uploadProgressBar.style.width = '0%';
        uploadProgressText.textContent = 'Uploading...';
        
        const formData = new FormData();
        formData.append('image', file);
        
        // Simulate progress for better UX
        let progress = 0;
        const progressInterval = setInterval(() => {
            progress += 10;
            if (progress <= 90) {
                uploadProgressBar.style.width = progress + '%';
            }
        }, 100);
        
        const response = await fetch(`${API_BASE_URL}/admin/test_categories/upload_image.php`, {
            method: 'POST',
            body: formData
        });
        
        clearInterval(progressInterval);
        uploadProgressBar.style.width = '100%';
        
        const data = await response.json();
        
        if (data.success) {
            // Store uploaded image data
            uploadedImageData = data.data;
            
            // Set hidden fields
            document.getElementById('testCategoryImage').value = data.data.filename;
            document.getElementById('testCategoryImagePath').value = data.data.path;
            
            // Update progress
            uploadProgressText.innerHTML = '<i class="fas fa-check-circle" style="color: #4CAF50;"></i> Upload complete!';
            
            // Hide progress after 2 seconds
            setTimeout(() => {
                uploadProgress.style.display = 'none';
            }, 2000);
            
            showNotification('Image uploaded successfully!', 'success');
        } else {
            throw new Error(data.message || 'Upload failed');
        }
    } catch (error) {
        console.error('Error uploading image:', error);
        uploadProgress.style.display = 'none';
        showNotification('Failed to upload image: ' + error.message, 'error');
        resetImageUpload();
    }
}

/**
 * Remove uploaded image
 */
function removeImage() {
    if (confirm('Are you sure you want to remove this image?')) {
        // Clear hidden fields
        document.getElementById('testCategoryImage').value = '';
        document.getElementById('testCategoryImagePath').value = '';
        
        // Clear file input
        document.getElementById('testCategoryImageInput').value = '';
        
        // Reset preview
        resetImageUpload();
        
        // Clear uploaded data
        uploadedImageData = null;
        
        showNotification('Image removed', 'info');
    }
}

/**
 * Reset image upload UI
 */
function resetImageUpload() {
    const imagePreviewContainer = document.getElementById('imagePreviewContainer');
    const imageUploadArea = document.getElementById('imageUploadArea');
    const uploadProgress = document.getElementById('uploadProgress');
    
    if (imagePreviewContainer) imagePreviewContainer.style.display = 'none';
    if (imageUploadArea) imageUploadArea.style.display = 'block';
    if (uploadProgress) uploadProgress.style.display = 'none';
    
    // Clear hidden fields
    const imageField = document.getElementById('testCategoryImage');
    const imagePathField = document.getElementById('testCategoryImagePath');
    if (imageField) imageField.value = '';
    if (imagePathField) imagePathField.value = '';
    
    uploadedImageData = null;
}

/**
 * Setup drag and drop for image upload
 */
document.addEventListener('DOMContentLoaded', function() {
    const imageUploadArea = document.getElementById('imageUploadArea');
    
    if (imageUploadArea) {
        // Prevent default drag behaviors
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            imageUploadArea.addEventListener(eventName, preventDefaults, false);
            document.body.addEventListener(eventName, preventDefaults, false);
        });
        
        // Highlight drop area when item is dragged over
        ['dragenter', 'dragover'].forEach(eventName => {
            imageUploadArea.addEventListener(eventName, function() {
                this.style.borderColor = '#6C63FF';
                this.style.background = '#f0efff';
            }, false);
        });
        
        ['dragleave', 'drop'].forEach(eventName => {
            imageUploadArea.addEventListener(eventName, function() {
                this.style.borderColor = '#e0e0e0';
                this.style.background = '#f9f9f9';
            }, false);
        });
        
        // Handle dropped files
        imageUploadArea.addEventListener('drop', function(e) {
            const dt = e.dataTransfer;
            const files = dt.files;
            
            if (files.length > 0) {
                const file = files[0];
                document.getElementById('testCategoryImageInput').files = files;
                handleImageSelect({ target: { files: [file] } });
            }
        }, false);
    }
});

function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
}

// Reset image upload when modal is closed
const originalCloseModal = window.closeModal;
window.closeModal = function(modalId) {
    if (modalId === 'testCategoryModal') {
        resetImageUpload();
    }
    if (originalCloseModal) {
        originalCloseModal(modalId);
    }
};

