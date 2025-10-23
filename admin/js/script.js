// API Configuration
const API_BASE_URL = '../api';

// Data Storage (loaded from API)
let users = [];

let exams = [];
let questions = [];
let testCategories = [];
let questionSessions = [];
let examCategories = [];
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

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    loadDashboard();
    loadUsers();
    loadExamCategories();
    loadTestCategories();
    loadQuestionSessions();
    loadSessionCards();
    loadSettings();
    populateExamCategoryDropdown();
    populateSessionDropdowns();
    generateUserRankings();
    loadRankings();
    generateTestResults();
    loadTestResults();
    // Removed for now - will add one by one
    // loadExams();
    // loadResults();
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
        'rankings': 'User Rankings',
        'settings': 'Settings'
        // Removed sections - will add one by one
        // 'exams': 'Exams Management',
        // 'results': 'Test Results'
    };
    document.getElementById('pageTitle').textContent = titles[pageName];
}

// Toggle Sidebar
function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('active');
}

// Dashboard
function loadDashboard() {
    const activities = [
        { icon: 'fa-user-plus', color: '#6C63FF', title: 'New user registered', subtitle: 'Rajesh Kumar joined TNPSC Group 4', time: '2 minutes ago' },
        { icon: 'fa-check-circle', color: '#4ECDC4', title: 'Test completed', subtitle: 'Priya completed Tamil Language Test', time: '15 minutes ago' },
        { icon: 'fa-plus-circle', color: '#FFD93D', title: 'New questions added', subtitle: '45 questions added to General Science', time: '1 hour ago' }
    ];
    
    const activityList = document.getElementById('activityList');
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
}

// Users CRUD
async function loadUsers() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/users/list.php`);
        const data = await response.json();
        
        if (data.success) {
            users = data.users;
    const tbody = document.getElementById('usersTableBody');
    tbody.innerHTML = users.map(user => `
        <tr>
            <td>#U${user.id.toString().padStart(3, '0')}</td>
            <td>${user.name}</td>
                    <td>${user.email || 'N/A'}</td>
            <td>${user.mobile || 'N/A'}</td>
                    <td>-</td>
            <td>${user.language === 'en' ? 'English' : 'Tamil'}</td>
                    <td><span class="badge badge-${user.is_active ? 'success' : 'danger'}">${user.is_active ? 'active' : 'inactive'}</span></td>
            <td>
                <button class="btn-icon btn-view" onclick="viewUser(${user.id})"><i class="fas fa-eye"></i></button>
                <button class="btn-icon btn-edit" onclick="editUser(${user.id})"><i class="fas fa-edit"></i></button>
                <button class="btn-icon btn-delete" onclick="deleteUser(${user.id})"><i class="fas fa-trash"></i></button>
            </td>
        </tr>
    `).join('');
            
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

function saveUser() {
    const user = {
        id: users.length + 1,
        name: document.getElementById('userName').value,
        email: document.getElementById('userEmail').value,
        mobile: document.getElementById('userMobile').value,
        exam: document.getElementById('userExam').value,
        language: document.getElementById('userLanguage').value,
        status: 'active'
    };
    
    users.push(user);
    loadUsers();
    closeModal('userModal');
    showNotification('User added successfully!', 'success');
    
    // Reset form
    document.getElementById('userForm').reset();
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

function editUser(id) {
    const user = users.find(u => u.id === id);
    document.getElementById('userName').value = user.name;
    document.getElementById('userEmail').value = user.email;
    document.getElementById('userMobile').value = user.mobile || '';
    document.getElementById('userExam').value = user.exam;
    document.getElementById('userLanguage').value = user.language;
    openModal('userModal');
}

function deleteUser(id) {
    if (confirm('Are you sure you want to delete this user?')) {
        users = users.filter(u => u.id !== id);
        loadUsers();
        showNotification('User deleted successfully!', 'success');
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

function saveQuestion() {
    const question = {
        id: questions.length + 1,
        text: document.getElementById('questionText').value,
        category: document.getElementById('questionCategory').value,
        difficulty: document.getElementById('questionDifficulty').value,
        language: document.getElementById('questionLanguage').value,
        optionA: document.getElementById('optionA').value,
        optionB: document.getElementById('optionB').value,
        optionC: document.getElementById('optionC').value,
        optionD: document.getElementById('optionD').value,
        correctAnswer: document.getElementById('correctAnswer').value,
        explanation: document.getElementById('questionExplanation').value
    };
    
    questions.push(question);
    loadQuestions();
    closeModal('questionModal');
    showNotification('Question added successfully!', 'success');
    document.getElementById('questionForm').reset();
}

function viewQuestion(id) {
    const q = questions.find(qu => qu.id === id);
    alert(`Question: ${q.text}\n\nCategory: ${q.category}\nDifficulty: ${q.difficulty}\nLanguage: ${q.language}`);
}

function editQuestion(id) {
    const q = questions.find(qu => qu.id === id);
    document.getElementById('questionText').value = q.text;
    document.getElementById('questionCategory').value = q.category;
    document.getElementById('questionDifficulty').value = q.difficulty;
    document.getElementById('questionLanguage').value = q.language;
    openModal('questionModal');
}

function deleteQuestion(id) {
    if (confirm('Are you sure you want to delete this question?')) {
        questions = questions.filter(q => q.id !== id);
        loadQuestions();
        showNotification('Question deleted successfully!', 'success');
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

function saveExamCategory() {
    const category = {
        id: examCategories.length + 1,
        title: document.getElementById('examCategoryTitle').value,
        name: document.getElementById('examCategoryName').value,
        description: document.getElementById('examCategoryDescription').value,
        icon: document.getElementById('examCategoryIcon').value || '?',
        color: document.getElementById('examCategoryColor').value
    };
    
    examCategories.push(category);
    loadExamCategories();
    closeModal('examCategoryModal');
    showNotification('Exam category added successfully!', 'success');
    document.getElementById('examCategoryForm').reset();
}

function editExamCategory(id) {
    const cat = examCategories.find(c => c.id === id);
    document.getElementById('examCategoryTitle').value = cat.title;
    document.getElementById('examCategoryName').value = cat.name;
    document.getElementById('examCategoryDescription').value = cat.description;
    document.getElementById('examCategoryIcon').value = cat.icon;
    document.getElementById('examCategoryColor').value = cat.color;
    openModal('examCategoryModal');
}

function deleteExamCategory(id) {
    if (confirm('Are you sure you want to delete this exam category?')) {
        examCategories = examCategories.filter(c => c.id !== id);
        loadExamCategories();
        populateExamCategoryDropdown();
        showNotification('Exam category deleted successfully!', 'success');
    }
}

// Populate Exam Category Dropdown
function populateExamCategoryDropdown() {
    const dropdown = document.getElementById('testCategoryExam');
    if (dropdown) {
        const currentValue = dropdown.value;
        dropdown.innerHTML = '<option value="">-- Select Exam Category --</option>' + 
            examCategories.map(cat => `<option value="${cat.title}">${cat.title}</option>`).join('');
        if (currentValue) {
            dropdown.value = currentValue;
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
            tbody.innerHTML = testCategories.map(cat => `
                <tr>
                    <td>#TC${cat.id.toString().padStart(3, '0')}</td>
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
            `).join('');
            document.getElementById('totalTests').textContent = testCategories.length;
        } else {
            console.error('Failed to load test categories:', data.message);
        }
    } catch (error) {
        console.error('Error loading test categories:', error);
        showNotification('Failed to load test categories', 'error');
    }
}

function saveTestCategory() {
    const category = {
        id: testCategories.length + 1,
        name: document.getElementById('testCategoryName').value,
        examCategory: document.getElementById('testCategoryExam').value,
        description: document.getElementById('testCategoryDescription').value,
        icon: document.getElementById('testCategoryIcon').value || 'fas fa-tag',
        color: document.getElementById('testCategoryColor').value
    };
    
    if (!category.examCategory) {
        showNotification('Please select an exam category!', 'error');
        return;
    }
    
    testCategories.push(category);
    loadTestCategories();
    closeModal('testCategoryModal');
    showNotification('Test category added successfully!', 'success');
    document.getElementById('testCategoryForm').reset();
}

function editTestCategory(id) {
    const cat = testCategories.find(c => c.id === id);
    document.getElementById('testCategoryName').value = cat.name;
    document.getElementById('testCategoryExam').value = cat.examCategory;
    document.getElementById('testCategoryDescription').value = cat.description;
    document.getElementById('testCategoryIcon').value = cat.icon;
    document.getElementById('testCategoryColor').value = cat.color;
    openModal('testCategoryModal');
}

function deleteTestCategory(id) {
    if (confirm('Are you sure you want to delete this test category?')) {
        testCategories = testCategories.filter(c => c.id !== id);
        loadTestCategories();
        populateSessionDropdowns();
        showNotification('Test category deleted successfully!', 'success');
    }
}

// Populate Session Dropdowns
function populateSessionDropdowns() {
    const examDropdown = document.getElementById('sessionExamCategory');
    const testDropdown = document.getElementById('sessionTestCategory');
    
    if (examDropdown) {
        examDropdown.innerHTML = '<option value="">-- Select Exam Category --</option>' + 
            examCategories.map(cat => `<option value="${cat.title}">${cat.title}</option>`).join('');
    }
    
    if (testDropdown) {
        testDropdown.innerHTML = '<option value="">-- Select Test Category --</option>' + 
            testCategories.map(cat => `<option value="${cat.name}" data-exam="${cat.examCategory}">${cat.name}</option>`).join('');
    }
}

function filterTestCategories() {
    const selectedExam = document.getElementById('sessionExamCategory').value;
    const testDropdown = document.getElementById('sessionTestCategory');
    
    if (!selectedExam) {
        testDropdown.innerHTML = '<option value="">-- Select Test Category --</option>';
        return;
    }
    
    const filteredCategories = testCategories.filter(cat => cat.examCategory === selectedExam);
    testDropdown.innerHTML = '<option value="">-- Select Test Category --</option>' + 
        filteredCategories.map(cat => `<option value="${cat.name}">${cat.name}</option>`).join('');
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

function saveQuestionSession() {
    const session = {
        id: questionSessions.length + 1,
        name: document.getElementById('sessionName').value,
        examCategory: document.getElementById('sessionExamCategory').value,
        testCategory: document.getElementById('sessionTestCategory').value,
        time: parseInt(document.getElementById('sessionTime').value),
        totalQuestions: parseInt(document.getElementById('sessionTotalQuestions').value),
        status: document.getElementById('sessionStatus').value
    };
    
    if (!session.examCategory || !session.testCategory) {
        showNotification('Please select exam category and test category!', 'error');
        return;
    }
    
    questionSessions.push(session);
    loadQuestionSessions();
    closeModal('questionSessionModal');
    showNotification('Question session added successfully!', 'success');
    document.getElementById('questionSessionForm').reset();
}

// Removed - using viewSessionQuestions instead

function editQuestionSession(id) {
    const session = questionSessions.find(s => s.id === id);
    document.getElementById('sessionName').value = session.name;
    document.getElementById('sessionExamCategory').value = session.examCategory;
    filterTestCategories();
    setTimeout(() => {
        document.getElementById('sessionTestCategory').value = session.testCategory;
    }, 100);
    document.getElementById('sessionTime').value = session.time;
    document.getElementById('sessionTotalQuestions').value = session.totalQuestions;
    document.getElementById('sessionStatus').value = session.status;
    openModal('questionSessionModal');
}

function deleteQuestionSession(id) {
    if (confirm('Are you sure you want to delete this question session?')) {
        questionSessions = questionSessions.filter(s => s.id !== id);
        loadQuestionSessions();
        loadSessionCards();
        showNotification('Question session deleted successfully!', 'success');
    }
}

// Load Session Cards for Add Question Page
function loadSessionCards() {
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
    
    grid.innerHTML = questionSessions.map(session => {
        // Calculate actual uploaded questions for this session
        const actualQuestionCount = questions.filter(q => q.sessionId === session.id).length;
        
        return `
        <div class="session-card" onclick="openUploadForSession(${session.id})" style="background: white; border: 2px solid #e0e0e0; border-radius: 12px; padding: 20px; cursor: pointer; transition: all 0.3s; position: relative; overflow: hidden;">
            <div style="position: absolute; top: 0; right: 0; width: 80px; height: 80px; background: linear-gradient(135deg, ${getSessionColor(session.examCategory)} 0%, ${getSessionColor(session.examCategory)}dd 100%); border-radius: 0 0 0 80px; opacity: 0.1;"></div>
            
            <div style="position: relative; z-index: 1;">
                <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 15px;">
                    <div style="width: 50px; height: 50px; background: linear-gradient(135deg, ${getSessionColor(session.examCategory)} 0%, ${getSessionColor(session.examCategory)}dd 100%); border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px; font-weight: bold;">
                        <i class="fas fa-clipboard-list"></i>
                    </div>
                    <div style="flex: 1;">
                        <h3 style="margin: 0; font-size: 18px; color: #333;">${session.name}</h3>
                        <span class="badge badge-primary" style="margin-top: 5px; display: inline-block;">${session.status}</span>
                    </div>
                </div>
                
                <div style="background: #f8f9fa; padding: 12px; border-radius: 8px; margin: 15px 0;">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; font-size: 13px;">
                        <div>
                            <i class="fas fa-book" style="color: #6C63FF;"></i> 
                            <strong>Exam:</strong><br>
                            <span style="color: #666;">${session.examCategory}</span>
                        </div>
                        <div>
                            <i class="fas fa-tag" style="color: #FF6B6B;"></i> 
                            <strong>Category:</strong><br>
                            <span style="color: #666;">${session.testCategory}</span>
                        </div>
                        <div>
                            <i class="fas fa-clock" style="color: #4ECDC4;"></i> 
                            <strong>Duration:</strong><br>
                            <span style="color: #666;">${session.time} minutes</span>
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
                <td colspan="5" style="text-align: center; padding: 40px;">
                    <i class="fas fa-inbox" style="font-size: 48px; color: #ddd; margin-bottom: 15px; display: block;"></i>
                    <h4 style="color: #999; margin: 0;">No Questions Uploaded Yet</h4>
                    <p style="color: #bbb;">Upload questions using the CSV bulk upload feature.</p>
                </td>
            </tr>
        `;
    } else {
        tbody.innerHTML = sessionQuestions.map((q, index) => {
            const hasEnglish = q.questionEn && q.questionEn.trim();
            const hasTamil = q.questionTa && q.questionTa.trim();
            
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
                            <strong style="display: block; margin-top: 5px;">${q.questionEn}</strong>
                        </div>
                        <div style="font-size: 12px; color: #666;">
                            <div>A) ${q.optionAEn}</div>
                            <div>B) ${q.optionBEn}</div>
                            <div>C) ${q.optionCEn}</div>
                            <div>D) ${q.optionDEn}</div>
                        </div>
                        ${q.explanationEn ? `<div style="margin-top: 8px; padding: 6px; background: white; border-radius: 4px; font-size: 11px;"><strong>Explanation:</strong> ${q.explanationEn}</div>` : ''}
                    </div>
                `;
            }
                    
            // Tamil Version (if available)
            if (hasTamil) {
                content += `
                    <div style="background: #fff8e1; padding: 12px; border-radius: 8px; border-left: 4px solid #FFD93D;">
                        <div style="margin-bottom: 8px;">
                            <span class="badge badge-warning" style="font-size: 10px;">தமிழ்</span>
                            <strong style="display: block; margin-top: 5px;">${q.questionTa}</strong>
                        </div>
                        <div style="font-size: 12px; color: #666;">
                            <div>அ) ${q.optionATa}</div>
                            <div>ஆ) ${q.optionBTa}</div>
                            <div>இ) ${q.optionCTa}</div>
                            <div>ஈ) ${q.optionDTa}</div>
                        </div>
                        ${q.explanationTa ? `<div style="margin-top: 8px; padding: 6px; background: white; border-radius: 4px; font-size: 11px;"><strong>விளக்கம்:</strong> ${q.explanationTa}</div>` : ''}
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
                    <span class="badge badge-success" style="font-size: 14px;">Option ${q.correctAnswer}</span>
                </td>
                <td style="text-align: center;">
                        <button class="btn-icon btn-edit" onclick="editQuestion(${q.id}, ${sessionId})" title="Edit" style="background: #2196F3; margin-right: 5px;">
                            <i class="fas fa-edit"></i>
                        </button>
                    <button class="btn-icon btn-delete" onclick="deleteQuestionFromView(${q.id}, ${sessionId})" title="Delete"><i class="fas fa-trash"></i></button>
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

function deleteQuestionFromView(questionId, sessionId) {
    if (confirm('Are you sure you want to delete this question?')) {
        questions = questions.filter(q => q.id !== questionId);
        viewSessionQuestions(sessionId);
        showNotification('Question deleted successfully!', 'success');
    }
}

function clearSessionQuestions(sessionId) {
    const session = questionSessions.find(s => s.id === sessionId);
    if (!session) return;
    
    const questionsCount = questions.filter(q => q.sessionId === sessionId).length;
    
    if (confirm(`Are you sure you want to delete all ${questionsCount} questions from "${session.name}"?\n\nThis action cannot be undone.`)) {
        questions = questions.filter(q => q.sessionId !== sessionId);
        showNotification(`Successfully deleted ${questionsCount} questions!`, 'success');
        closeModal('csvUploadModal');
        loadSessionCards(); // Refresh the cards
    }
}

function editQuestion(questionId, sessionId) {
    const question = questions.find(q => q.id === questionId);
    if (!question) {
        showNotification('Question not found!', 'error');
        return;
    }
    
    console.log('📝 Editing question:', questionId, question);
    
    // Set hidden fields
    document.getElementById('editQuestionId').value = question.id;
    document.getElementById('editQuestionSessionId').value = sessionId;
    
    // Pre-fill English fields
    document.getElementById('editQuestionEn').value = question.questionEn || '';
    document.getElementById('editOptionAEn').value = question.optionAEn || '';
    document.getElementById('editOptionBEn').value = question.optionBEn || '';
    document.getElementById('editOptionCEn').value = question.optionCEn || '';
    document.getElementById('editOptionDEn').value = question.optionDEn || '';
    document.getElementById('editExplanationEn').value = question.explanationEn || '';
    
    // Pre-fill Tamil fields
    document.getElementById('editQuestionTa').value = question.questionTa || '';
    document.getElementById('editOptionATa').value = question.optionATa || '';
    document.getElementById('editOptionBTa').value = question.optionBTa || '';
    document.getElementById('editOptionCTa').value = question.optionCTa || '';
    document.getElementById('editOptionDTa').value = question.optionDTa || '';
    document.getElementById('editExplanationTa').value = question.explanationTa || '';
    
    // Set correct answer
    document.getElementById('editCorrectAnswer').value = question.correctAnswer;
    
    // Open the modal
    openModal('editQuestionModal');
}

function saveEditedQuestion() {
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
    
    // Find and update the question
    const questionIndex = questions.findIndex(q => q.id === questionId);
    if (questionIndex === -1) {
        showNotification('❌ Question not found!', 'error');
        return;
    }
    
    // Update the question object
    questions[questionIndex] = {
        ...questions[questionIndex],
        questionEn: questionEn,
        questionTa: questionTa,
        optionAEn: optionAEn,
        optionATa: optionATa,
        optionBEn: optionBEn,
        optionBTa: optionBTa,
        optionCEn: optionCEn,
        optionCTa: optionCTa,
        optionDEn: optionDEn,
        optionDTa: optionDTa,
        correctAnswer: correctAnswer,
        explanationEn: explanationEn,
        explanationTa: explanationTa,
        language: hasEnglish && hasTamil ? 'both' : hasEnglish ? 'en' : 'ta'
    };
    
    console.log('✅ Question updated:', questions[questionIndex]);
    
    // Close the edit modal
    closeModal('editQuestionModal');
    
    // Refresh the questions view
    viewSessionQuestions(sessionId);
    
    // Show success message
    showNotification('✅ Question updated successfully!', 'success');
}

function uploadCSV() {
    const sessionId = parseInt(document.getElementById('selectedSessionId').value);
    const fileInput = document.getElementById('csvFile');
    
    if (!sessionId) {
        showNotification('Session not selected!', 'error');
        return;
    }
    
    if (!fileInput.files || fileInput.files.length === 0) {
        showNotification('Please select a CSV file!', 'error');
        return;
    }
    
    const file = fileInput.files[0];
    const reader = new FileReader();
    
    reader.onload = function(e) {
        try {
            const csv = e.target.result;
            const lines = csv.split('\n');
            const headers = lines[0].split(',').map(h => h.trim().replace(/"/g, ''));
            
            console.log(`📁 Starting CSV Upload for Session ID: ${sessionId}`);
            console.log(`📄 Total CSV Lines (including header): ${lines.length}`);
            
            const beforeUploadCount = questions.filter(q => q.sessionId === sessionId).length;
            console.log(`📊 Questions in session before upload: ${beforeUploadCount}`);
            
            let addedCount = 0;
            let skippedCount = 0;
            let errors = [];
            
            for (let i = 1; i < lines.length; i++) {
                if (!lines[i].trim()) continue;
                
                const values = parseCSVLine(lines[i]);
                if (values.length < 13) {
                    skippedCount++;
                    errors.push(`Line ${i + 1}: Insufficient columns (expected 13, got ${values.length})`);
                    continue;
                }
                
                // Clean values
                const questionEn = values[0].trim();
                const questionTa = values[1].trim();
                const optionAEn = values[2].trim();
                const optionATa = values[3].trim();
                const optionBEn = values[4].trim();
                const optionBTa = values[5].trim();
                const optionCEn = values[6].trim();
                const optionCTa = values[7].trim();
                const optionDEn = values[8].trim();
                const optionDTa = values[9].trim();
                const correctAnswer = values[10].trim().toUpperCase();
                const explanationEn = values[11].trim();
                const explanationTa = values[12].trim();
                
                // Validate: Must have at least one language
                const hasEnglish = questionEn && optionAEn && optionBEn && optionCEn && optionDEn;
                const hasTamil = questionTa && optionATa && optionBTa && optionCTa && optionDTa;
                
                if (!hasEnglish && !hasTamil) {
                    skippedCount++;
                    errors.push(`Line ${i + 1}: Must have complete question in at least English OR Tamil`);
                    continue;
                }
                
                // Validate correct answer
                if (!['A', 'B', 'C', 'D'].includes(correctAnswer)) {
                    skippedCount++;
                    errors.push(`Line ${i + 1}: Invalid correct answer "${correctAnswer}" (must be A, B, C, or D)`);
                    continue;
                }
                
                // Check for duplicate questions in this session
                const isDuplicate = questions.some(q => 
                    q.sessionId === sessionId && (
                        (q.questionEn && q.questionEn === questionEn) ||
                        (q.questionTa && q.questionTa === questionTa)
                    )
                );
                
                if (isDuplicate) {
                    skippedCount++;
                    const dupQuestion = questionEn || questionTa;
                    errors.push(`Line ${i + 1}: Duplicate question detected - "${dupQuestion.substring(0, 50)}..."`);
                    console.warn(`⚠️ Line ${i + 1}: Skipped duplicate question`);
                    continue;
                }
                
                const question = {
                    id: questions.length + 1,
                    sessionId: sessionId,
                    questionEn: questionEn || '',
                    questionTa: questionTa || '',
                    optionAEn: optionAEn || '',
                    optionATa: optionATa || '',
                    optionBEn: optionBEn || '',
                    optionBTa: optionBTa || '',
                    optionCEn: optionCEn || '',
                    optionCTa: optionCTa || '',
                    optionDEn: optionDEn || '',
                    optionDTa: optionDTa || '',
                    correctAnswer: correctAnswer,
                    explanationEn: explanationEn || '',
                    explanationTa: explanationTa || '',
                    language: hasEnglish && hasTamil ? 'both' : hasEnglish ? 'en' : 'ta'
                };
                
                questions.push(question);
                addedCount++;
                console.log(`✅ Line ${i + 1}: Added question - Language: ${question.language}, ID: ${question.id}`);
            }
            
            const afterUploadCount = questions.filter(q => q.sessionId === sessionId).length;
            console.log(`📊 Questions in session after upload: ${afterUploadCount}`);
            console.log(`✅ Total added: ${addedCount}, Skipped: ${skippedCount}`);
            console.log(`📝 All questions in database: ${questions.length}`);
            
            closeModal('csvUploadModal');
            loadSessionCards(); // Refresh the session cards to update question counts
            
            if (addedCount > 0 && skippedCount === 0) {
                showNotification(`✅ Successfully uploaded ${addedCount} questions!`, 'success');
            } else if (addedCount > 0 && skippedCount > 0) {
                showNotification(`⚠️ Uploaded ${addedCount} questions, skipped ${skippedCount} invalid rows. Check console for details.`, 'success');
                console.warn('CSV Upload Errors:', errors);
            } else {
                showNotification(`❌ Failed to upload any questions. ${skippedCount} rows had errors.`, 'error');
                console.error('CSV Upload Errors:', errors);
            }
            
            fileInput.value = '';
            document.getElementById('csvUploadForm').reset();
        } catch (error) {
            console.error('CSV parsing error:', error);
            showNotification('Error parsing CSV file. Please check the format and try again.', 'error');
        }
    };
    
    reader.readAsText(file);
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
    const csvContent = `questionEn,questionTa,optionAEn,optionATa,optionBEn,optionBTa,optionCEn,optionCTa,optionDEn,optionDTa,correctAnswer,explanationEn,explanationTa
"What is the capital of Tamil Nadu?","தமிழ்நாட்டின் தலைநகரம் எது?","Chennai","சென்னை","Mumbai","மும்பை","Delhi","டெல்லி","Kolkata","கொல்கத்தா","A","Chennai is the capital of Tamil Nadu","சென்னை தமிழ்நாட்டின் தலைநகரம்"
"What is 2+2?","","4","","3","","5","","6","","A","2+2 equals 4",""
"","திருக்குறள் எழுதியவர் யார்?","","திருவள்ளுவர்","","கம்பர்","","பாரதி","","இளங்கோ","A","","திருவள்ளுவர் திருக்குறளை எழுதினார்"`;
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'questions_template_with_examples.csv';
    a.click();
    window.URL.revokeObjectURL(url);
    
    showNotification('CSV template downloaded! It includes examples for bilingual, English-only, and Tamil-only questions.', 'success');
}

// Test Results Management
function generateTestResults() {
    // Generate comprehensive test results for users
    testResults = [];
    let resultId = 1;
    
    users.forEach(user => {
        const testsCount = Math.floor(Math.random() * 10) + 5; // 5-15 tests per user
        
        for (let i = 0; i < testsCount; i++) {
            const session = questionSessions[Math.floor(Math.random() * questionSessions.length)];
            if (!session) continue;
            
            const totalQuestions = parseInt(session.totalQuestions) || 50;
            const correctAnswers = Math.floor(Math.random() * totalQuestions * 0.4) + Math.floor(totalQuestions * 0.4); // 40-80% correct
            const percentage = ((correctAnswers / totalQuestions) * 100).toFixed(1);
            const timeTaken = Math.floor(Math.random() * 60) + 30; // 30-90 minutes
            
            // Generate dates in the last 30 days
            const daysAgo = Math.floor(Math.random() * 30);
            const date = new Date();
            date.setDate(date.getDate() - daysAgo);
            
            testResults.push({
                id: resultId++,
                userId: user.id,
                userName: user.name,
                userMobile: user.mobile,
                examCategory: session.examCategory,
                testCategory: session.testCategory,
                sessionId: session.id,
                sessionName: session.name,
                totalQuestions: totalQuestions,
                correctAnswers: correctAnswers,
                wrongAnswers: totalQuestions - correctAnswers,
                percentage: parseFloat(percentage),
                timeTaken: timeTaken, // in minutes
                date: date.toISOString().split('T')[0],
                timestamp: date.getTime(),
                status: parseFloat(percentage) >= 60 ? 'passed' : 'failed'
            });
        }
    });
    
    // Sort by date (newest first)
    testResults.sort((a, b) => b.timestamp - a.timestamp);
    
    console.log(`📊 Generated ${testResults.length} test results`);
}

function loadTestResults() {
    if (testResults.length === 0) {
        generateTestResults();
    }
    
    // Calculate statistics
    const totalAttempts = testResults.length;
    const avgScore = (testResults.reduce((sum, r) => sum + r.percentage, 0) / totalAttempts).toFixed(1);
    const passed = testResults.filter(r => r.percentage >= 60).length;
    const failed = testResults.filter(r => r.percentage < 60).length;
    
    document.getElementById('totalTestAttempts').textContent = totalAttempts;
    document.getElementById('avgTestScore').textContent = avgScore + '%';
    document.getElementById('testsPassedCount').textContent = passed;
    document.getElementById('testsFailedCount').textContent = failed;
    
    // Populate user filter
    const filterUser = document.getElementById('filterUser');
    const uniqueUsers = [...new Set(testResults.map(r => r.userName))];
    filterUser.innerHTML = '<option value="all">All Users</option>';
    uniqueUsers.forEach(userName => {
        filterUser.innerHTML += `<option value="${userName}">${userName}</option>`;
    });
    
    // Display all results
    displayTestResults(testResults);
}

function displayTestResults(results) {
    const tbody = document.getElementById('testResultsTableBody');
    
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
        const statusColor = result.status === 'passed' ? '#4CAF50' : '#FF6B6B';
        const statusIcon = result.status === 'passed' ? 'check-circle' : 'times-circle';
        
        return `
            <tr style="border-bottom: 1px solid #f0f0f0;">
                <td style="padding: 15px;"><strong>#${result.id}</strong></td>
                <td style="padding: 15px;">
                    <div>
                        <strong style="color: #333;">${result.userName}</strong><br>
                        <small style="color: #999;">${result.userMobile}</small>
                    </div>
                </td>
                <td style="padding: 15px;">
                    <span class="badge badge-primary" style="padding: 6px 12px;">${result.examCategory}</span>
                </td>
                <td style="padding: 15px;">
                    <div>
                        <strong style="color: #333;">${result.sessionName}</strong><br>
                        <small style="color: #999;">${result.testCategory}</small>
                    </div>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <strong style="font-size: 16px; color: #6C63FF;">${result.correctAnswers}/${result.totalQuestions}</strong>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <div style="display: flex; flex-direction: column; align-items: center;">
                        <strong style="font-size: 18px; color: ${statusColor};">${result.percentage}%</strong>
                        <div style="width: 60px; height: 6px; background: #e0e0e0; border-radius: 10px; margin-top: 5px; overflow: hidden;">
                            <div style="width: ${result.percentage}%; height: 100%; background: ${statusColor}; border-radius: 10px;"></div>
                        </div>
                    </div>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <span style="color: #666;"><i class="fas fa-clock"></i> ${result.timeTaken} min</span>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <strong style="color: #666;">${result.date}</strong>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <span class="badge" style="background: ${statusColor}; color: white; padding: 6px 12px; border-radius: 15px;">
                        <i class="fas fa-${statusIcon}"></i> ${result.status.toUpperCase()}
                    </span>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <button class="btn-icon btn-view" onclick="viewAnswerSheet(${result.id})" title="View Answer Sheet">
                        <i class="fas fa-eye"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterTestResults() {
    const filterUser = document.getElementById('filterUser').value;
    const filterExam = document.getElementById('filterExamCategory').value;
    const filterStatus = document.getElementById('filterStatus').value;
    const filterDate = document.getElementById('filterDateRange').value;
    
    let filtered = [...testResults];
    
    // Filter by user
    if (filterUser !== 'all') {
        filtered = filtered.filter(r => r.userName === filterUser);
    }
    
    // Filter by exam category
    if (filterExam !== 'all') {
        filtered = filtered.filter(r => r.examCategory === filterExam);
    }
    
    // Filter by status
    if (filterStatus === 'passed') {
        filtered = filtered.filter(r => r.percentage >= 60);
    } else if (filterStatus === 'failed') {
        filtered = filtered.filter(r => r.percentage < 60);
    } else if (filterStatus === 'completed') {
        filtered = filtered.filter(r => r.status === 'passed' || r.status === 'failed');
    }
    
    // Filter by date range
    const today = new Date();
    if (filterDate === 'today') {
        const todayStr = today.toISOString().split('T')[0];
        filtered = filtered.filter(r => r.date === todayStr);
    } else if (filterDate === 'week') {
        const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);
        filtered = filtered.filter(r => new Date(r.date) >= weekAgo);
    } else if (filterDate === 'month') {
        const monthAgo = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000);
        filtered = filtered.filter(r => new Date(r.date) >= monthAgo);
    }
    
    // Update statistics
    if (filtered.length > 0) {
        const avgScore = (filtered.reduce((sum, r) => sum + r.percentage, 0) / filtered.length).toFixed(1);
        const passed = filtered.filter(r => r.percentage >= 60).length;
        const failed = filtered.filter(r => r.percentage < 60).length;
        
        document.getElementById('totalTestAttempts').textContent = filtered.length;
        document.getElementById('avgTestScore').textContent = avgScore + '%';
        document.getElementById('testsPassedCount').textContent = passed;
        document.getElementById('testsFailedCount').textContent = failed;
    }
    
    displayTestResults(filtered);
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
    
    // For now, show a detailed alert (will create a modal later)
    showNotification(`📋 Answer Sheet for ${result.userName}\n\nTest: ${result.sessionName}\nScore: ${result.correctAnswers}/${result.totalQuestions} (${result.percentage}%)\nTime: ${result.timeTaken} minutes\nStatus: ${result.status.toUpperCase()}\n\n(Detailed answer sheet modal coming soon!)`, 'success');
}

function exportTestResults() {
    const filterUser = document.getElementById('filterUser').value;
    const filterExam = document.getElementById('filterExamCategory').value;
    const filterStatus = document.getElementById('filterStatus').value;
    
    let filtered = [...testResults];
    
    // Apply same filters as display
    if (filterUser !== 'all') filtered = filtered.filter(r => r.userName === filterUser);
    if (filterExam !== 'all') filtered = filtered.filter(r => r.examCategory === filterExam);
    if (filterStatus === 'passed') filtered = filtered.filter(r => r.percentage >= 60);
    else if (filterStatus === 'failed') filtered = filtered.filter(r => r.percentage < 60);
    
    // Create CSV content
    let csvContent = 'ID,User Name,Mobile,Exam Category,Test Name,Correct Answers,Total Questions,Percentage,Time Taken (min),Date,Status\n';
    
    filtered.forEach(result => {
        csvContent += `${result.id},"${result.userName}","${result.userMobile}","${result.examCategory}","${result.sessionName}",${result.correctAnswers},${result.totalQuestions},${result.percentage}%,${result.timeTaken},${result.date},${result.status}\n`;
    });
    
    // Download CSV
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `test_results_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
    
    showNotification(`✅ Exported ${filtered.length} test results to CSV!`, 'success');
}

// Settings Management
function loadSettings() {
    document.getElementById('privacyPolicyText').value = appSettings.privacyPolicy;
    document.getElementById('termsConditionsText').value = appSettings.termsConditions;
}

function switchSettingsTab(tab) {
    const privacyTab = document.getElementById('privacyTab');
    const termsTab = document.getElementById('termsTab');
    const privacySection = document.getElementById('privacySection');
    const termsSection = document.getElementById('termsSection');
    
    if (tab === 'privacy') {
        privacyTab.classList.add('active');
        termsTab.classList.remove('active');
        privacyTab.style.borderBottomColor = '#6C63FF';
        termsTab.style.borderBottomColor = 'transparent';
        privacySection.style.display = 'block';
        termsSection.style.display = 'none';
    } else {
        termsTab.classList.add('active');
        privacyTab.classList.remove('active');
        termsTab.style.borderBottomColor = '#6C63FF';
        privacyTab.style.borderBottomColor = 'transparent';
        termsSection.style.display = 'block';
        privacySection.style.display = 'none';
    }
}

function savePrivacyPolicy() {
    const privacyText = document.getElementById('privacyPolicyText').value;
    
    if (!privacyText.trim()) {
        showNotification('Privacy policy cannot be empty!', 'error');
        return;
    }
    
    appSettings.privacyPolicy = privacyText;
    
    // In a real application, this would save to a database
    // For now, we're using localStorage
    try {
        localStorage.setItem('appSettings', JSON.stringify(appSettings));
        showNotification('Privacy policy saved successfully!', 'success');
    } catch (e) {
        showNotification('Privacy policy updated in session!', 'success');
    }
}

function saveTermsConditions() {
    const termsText = document.getElementById('termsConditionsText').value;
    
    if (!termsText.trim()) {
        showNotification('Terms & conditions cannot be empty!', 'error');
        return;
    }
    
    appSettings.termsConditions = termsText;
    
    // In a real application, this would save to a database
    // For now, we're using localStorage
    try {
        localStorage.setItem('appSettings', JSON.stringify(appSettings));
        showNotification('Terms & conditions saved successfully!', 'success');
    } catch (e) {
        showNotification('Terms & conditions updated in session!', 'success');
    }
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
    document.getElementById(modalId).classList.add('active');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

// Close modal on outside click
window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
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
function generateUserRankings() {
    // Generate comprehensive ranking data for each user
    userRankings = users.map(user => {
        const testsTaken = Math.floor(Math.random() * 50) + 25;
        const avgScore = (Math.random() * 30 + 60).toFixed(1);
        const highestScore = Math.min(100, parseFloat(avgScore) + Math.random() * 15).toFixed(1);
        const currentStreak = Math.floor(Math.random() * 15) + 1;
        const studyHours = Math.floor(Math.random() * 100) + 50;
        
        return {
            id: user.id,
            name: user.name,
            email: user.email,
            mobile: user.mobile,
            exam: user.exam,
            language: user.language,
            testsTaken: testsTaken,
            avgScore: parseFloat(avgScore),
            highestScore: parseFloat(highestScore),
            currentStreak: currentStreak,
            studyHours: studyHours,
            totalPoints: testsTaken * parseFloat(avgScore) // Calculate total points for ranking
        };
    });
    
    // Sort by total points (descending)
    userRankings.sort((a, b) => b.totalPoints - a.totalPoints);
}

function loadRankings() {
    if (userRankings.length === 0) {
        generateUserRankings();
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
    const avgScore = (userRankings.reduce((sum, user) => sum + user.avgScore, 0) / totalUsers).toFixed(1);
    const totalTests = userRankings.reduce((sum, user) => sum + user.testsTaken, 0);
    const highestStreak = Math.max(...userRankings.map(user => user.currentStreak));
    
    document.getElementById('totalRankedUsers').textContent = totalUsers;
    document.getElementById('avgRankScore').textContent = avgScore + '%';
    document.getElementById('totalTestsTaken').textContent = totalTests;
    document.getElementById('topPerformerStreak').textContent = highestStreak + ' days';
    
    // Load full rankings table
    displayRankings(userRankings);
}

function displayRankings(rankings) {
    const tbody = document.getElementById('rankingsTableBody');
    tbody.innerHTML = rankings.map((user, index) => {
        const rank = index + 1;
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
                    <span style="background: linear-gradient(135deg, #FF6B6B, #FFD93D); color: white; padding: 6px 12px; border-radius: 20px; font-weight: bold;">
                        🔥 ${user.currentStreak} days
                    </span>
                </td>
                <td style="text-align: center; padding: 15px;">
                    <strong style="color: #666;">${user.studyHours}h</strong>
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
        const highestStreak = Math.max(...filteredRankings.map(user => user.currentStreak));
        
        document.getElementById('totalRankedUsers').textContent = filteredRankings.length;
        document.getElementById('avgRankScore').textContent = avgScore + '%';
        document.getElementById('totalTestsTaken').textContent = totalTests;
        document.getElementById('topPerformerStreak').textContent = highestStreak + ' days';
        
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

function exportRankings() {
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
    
    // Create CSV content
    let csvContent = 'Rank,Name,Email,Mobile,Exam Category,Tests Taken,Average Score,Highest Score,Current Streak,Study Hours\n';
    
    filteredRankings.forEach((user, index) => {
        csvContent += `${index + 1},"${user.name}","${user.email}","${user.mobile || 'N/A'}","${user.exam}",${user.testsTaken},${user.avgScore}%,${user.highestScore}%,${user.currentStreak} days,${user.studyHours}h\n`;
    });
    
    // Download CSV
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `user_rankings_${filter}_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
    
    showNotification('Rankings exported successfully!', 'success');
}

console.log('TNPSC Mock Test Admin Panel - Mockup Version');
