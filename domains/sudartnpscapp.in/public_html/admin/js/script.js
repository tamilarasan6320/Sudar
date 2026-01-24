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

// Utility function to escape HTML characters (prevent XSS)
// Define globally to ensure it's available everywhere
window.escapeHtml = function(text) {
    if (text == null || text === undefined || text === '') {
        return '';
    }
    // Convert to string if not already
    const str = String(text);
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
};

// Also define as regular function for compatibility
function escapeHtml(text) {
    return window.escapeHtml(text);
}

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
    // Prevent browser restoring old scroll positions (fixes "cut" view)
    try {
        if ('scrollRestoration' in history) {
            history.scrollRestoration = 'manual';
        }
    } catch (e) {}

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

        // If URL has #pageName, open that page and reset scroll.
        // Also remove the hash so the browser doesn't auto-scroll to hidden elements on refresh.
        const hash = (window.location.hash || '').replace('#', '').trim();
        if (hash && document.getElementById(hash)) {
            showPage(hash);
            try {
                history.replaceState(null, document.title, window.location.pathname + window.location.search);
            } catch (e) {}
        } else {
            try { window.scrollTo(0, 0); } catch (e) {}
        }
    }
    
    // ✅ Add event listener for exam category dropdown
    const examCategoryDropdown = document.getElementById('sessionExamCategory');
    if (examCategoryDropdown) {
        examCategoryDropdown.addEventListener('change', function() {
            console.log('🔄 Exam category changed, filtering test categories...');
            filterTestCategories(); // No pre-select value when manually changed
        });
    }
});

// Navigation
// ═══════════════════════════════════════════════════════════════
// Extract Questions Functions
// ═══════════════════════════════════════════════════════════════

function generateExtractionScript() {
    const examName = document.getElementById('examName').value.trim();
    const totalPages = document.getElementById('totalPages').value.trim();
    const baseUrl = document.getElementById('baseUrl').value.trim();
    
    if (!examName || !totalPages || !baseUrl) {
        alert('⚠️ Please fill in all three fields!');
        return;
    }
    
    const script = `// Extraction Script for: ${examName}
(async function() {
    const EXAM_NAME = '${examName}';
    const TOTAL_PAGES = ${totalPages};
    const BASE_URL = '${baseUrl}';
    const QUESTIONS_PER_PAGE = 10;
    
    console.log('🚀 Starting: ' + EXAM_NAME);
    console.log('📊 Pages: ' + TOTAL_PAGES);
    
    const allQuestions = [];
    
    async function loadPage(pageNum) {
        return new Promise((resolve) => {
            const url = BASE_URL + pageNum + '.php';
            console.log('📄 Page ' + pageNum + '...');
            
            const iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = url;
            
            iframe.onload = function() {
                try {
                    const doc = iframe.contentDocument || iframe.contentWindow.document;
                    const divs = doc.querySelectorAll('.row.quest_mar');
                    
                    console.log('✅ Found ' + divs.length + ' questions');
                    
                    const questions = [];
                    divs.forEach((div, idx) => {
                        try {
                            const badge = div.querySelector('.badge')?.textContent?.trim() || '';
                            const match = badge.match(/Q(?:uestion)?\\\\s*(\\\\d+)/i);
                            const qNum = match ? parseInt(match[1]) : ((pageNum - 1) * QUESTIONS_PER_PAGE) + idx + 1;
                            
                            const html = div.querySelector('[ng-bind-html]')?.innerHTML || '';
                            
                            const opts = {};
                            div.querySelectorAll('.ans_opt').forEach((opt) => {
                                const label = opt.querySelector('label');
                                if (label) {
                                    const text = label.textContent.trim();
                                    const m = text.match(/^([A-D])[.\\\\)]\\\\s*(.+)$/);
                                    if (m) opts[m[1]] = m[2].trim();
                                }
                            });
                            
                            const ans = div.querySelector('input[type="hidden"][name="crtans"]')?.value?.trim() || '';
                            const hasTable = html.includes('<table');
                            const hasImg = html.includes('<img');
                            const imgMatch = html.match(/<img[^>]+src=["']([^"']+)["']/);
                            
                            questions.push({
                                questionNumber: qNum,
                                question_en: html,
                                question_ta: '',
                                options: opts,
                                options_ta: {},
                                correctAnswer: ans,
                                table: null,
                                hasTable: hasTable,
                                imageUrl: imgMatch ? imgMatch[1] : '',
                                hasImage: hasImg
                            });
                        } catch (e) {
                            console.error('❌ Error:', e);
                        }
                    });
                    
                    document.body.removeChild(iframe);
                    resolve(questions);
                } catch (e) {
                    console.error('❌ Error:', e);
                    document.body.removeChild(iframe);
                    resolve([]);
                }
            };
            
            iframe.onerror = function() {
                console.error('❌ Failed page ' + pageNum);
                document.body.removeChild(iframe);
                resolve([]);
            };
            
            document.body.appendChild(iframe);
        });
    }
    
    for (let page = 1; page <= TOTAL_PAGES; page++) {
        const questions = await loadPage(page);
        allQuestions.push(...questions);
        await new Promise(r => setTimeout(r, 500));
    }
    
    console.log('✅ DONE! Total: ' + allQuestions.length);
    
    const json = JSON.stringify(allQuestions, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'extracted_' + EXAM_NAME.replace(/\\\\s+/g, '_') + '_' + Date.now() + '.json';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    console.log('💾 Downloaded! Check Downloads folder');
})();`;
    
    document.getElementById('extractionScript').value = script;
    alert('✅ Script generated! Click "Copy Script" button to copy it.');
}

function copyScriptToClipboard() {
    const scriptText = document.getElementById('extractionScript').value;
    
    if (scriptText.includes('Click "Generate Custom Script"')) {
        alert('⚠️ Please generate a custom script first!');
        return;
    }
    
    navigator.clipboard.writeText(scriptText).then(() => {
        alert('✅ Script copied to clipboard!\n\nNow:\n1. Go to question page\n2. Press F12\n3. Click Console tab\n4. Paste and press Enter');
    }).catch(err => {
        alert('❌ Failed to copy. Please select the text manually.');
    });
}

function showPage(pageName) {
    // Hide all pages
    document.querySelectorAll('.page').forEach(page => page.classList.remove('active'));
    
    // Show selected page
    const pageElement = document.getElementById(pageName);
    if (pageElement) {
        pageElement.classList.add('active');
    } else {
        console.error('Page not found:', pageName);
        return;
    }

    // ✅ Fix "left cut / blank top" issue:
    // Reset scroll (both vertical + horizontal) whenever switching pages.
    // Some pages can leave main-content scrolled, causing the next page to look cut off.
    requestAnimationFrame(() => {
        try {
            window.scrollTo(0, 0);
        } catch (e) {}

        document.body.scrollTop = 0;
        document.documentElement.scrollTop = 0;
        document.body.scrollLeft = 0;
        document.documentElement.scrollLeft = 0;

        const main = document.querySelector('.main-content');
        if (main) {
            main.scrollTop = 0;
            main.scrollLeft = 0;
        }

        if (pageElement) {
            pageElement.scrollTop = 0;
            pageElement.scrollLeft = 0;
        }
    });
    
    // Update nav items
    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    
    // Pages that belong to Questions Management section
    const questionsManagementSubPages = ['testCategories', 'questionSessions', 'addQuestion', 'extractQuestions'];
    
    // Find and activate the correct nav item
    let navItem = document.querySelector(`.nav-item[href="#${pageName}"]`);
    
    // If we're on a sub-page of Questions Management, highlight the parent menu
    if (questionsManagementSubPages.includes(pageName)) {
        navItem = document.querySelector('.nav-item[href="#questionsManagement"]');
    }
    
    if (navItem) {
        navItem.classList.add('active');
    }
    
    // Update title
    const titles = {
        'dashboard': 'Dashboard',
        'users': 'Users Management',
        'registrations': 'Registrations',
        'trialStarts': '₹5 Trial Users',
        'examCategories': 'Exam Categories',
        'questionsManagement': 'Questions Management',
        'testCategories': 'Test Categories',
        'questionSessions': 'Question Sessions',
        'addQuestion': 'Add Question',
        'extractQuestions': 'Extract Questions',
        'testResults': 'Test Results',
        'languages': 'Languages',
        'rankings': 'User Rankings',
        'subscriptions': 'Subscriptions & Premium',
        'feedback': 'Feedback Management',
        'settings': 'Settings'
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
    } else if (pageName === 'registrations') {
        refreshRegistrationsPage();
    } else if (pageName === 'trialStarts') {
        refreshTrialStartsPage();
    } else if (pageName === 'rankings') {
        loadRankings();
    } else if (pageName === 'subscriptions') {
        refreshSubscriptionsPage();
    } else if (pageName === 'feedback') {
        loadFeedback();
    } else if (pageName === 'questionsManagement') {
        loadQuestionsManagementStats();
    } else if (pageName === 'settings') {
        // Ensure one settings section is visible (prevents blank page if sections are hidden by CSS/state)
        try {
            if (typeof switchSettingsTab === 'function') {
                switchSettingsTab('about');
            } else {
                console.warn('switchSettingsTab not available');
            }
        } catch (e) {
            console.error('Failed to switch settings tab:', e);
        }
    } else if (pageName === 'marketing') {
        initMarketingPage();
    } else if (pageName === 'reports') {
        initReportsPage();
    }
}

// ==========================================
// REGISTRATIONS (DATE-WISE) - IST TIMEZONE
// ==========================================

// Get IST date as YYYY-MM-DD (for date pickers and API calls)
function getIstDateYYYYMMDD(date = new Date()) {
    const formatter = new Intl.DateTimeFormat('en-CA', {
        timeZone: 'Asia/Kolkata',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });
    return formatter.format(date);
}

function getIstTodayYYYYMMDD() {
    return getIstDateYYYYMMDD(new Date());
}

function getIstYesterdayYYYYMMDD() {
    const d = new Date();
    d.setDate(d.getDate() - 1);
    return getIstDateYYYYMMDD(d);
}

function setRegistrationsDate(which) {
    const input = document.getElementById('registrationsDate');
    if (!input) return;

    if (which === 'today') {
        input.value = getIstTodayYYYYMMDD();
        return;
    }
    if (which === 'yesterday') {
        input.value = getIstYesterdayYYYYMMDD();
        return;
    }
    if (typeof which === 'string' && which.trim() !== '') {
        input.value = which.trim();
    }
}

function setRegistrationsDateAndLoad(which) {
    setRegistrationsDate(which);
    loadRegistrationsBySelectedDate();
}

function openRegistrationsPage(which) {
    setRegistrationsDate(which || 'today');
    showPage('registrations');
}

function refreshRegistrationsPage() {
    const input = document.getElementById('registrationsDate');
    if (input && !input.value) {
        input.value = getIstTodayYYYYMMDD();
    }
    loadRegistrationsBySelectedDate();
}

async function loadRegistrationsBySelectedDate() {
    const date = document.getElementById('registrationsDate')?.value || getIstTodayYYYYMMDD();
    await loadRegistrations(date);
}

async function loadRegistrations(date, limit = 200, offset = 0) {
    const tbody = document.getElementById('registrationsTableBody');
    const summary = document.getElementById('registrationsSummary');

    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 40px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #6C63FF;"></i>
                    <p style="margin-top: 10px; color: #666;">Loading registrations...</p>
                </td>
            </tr>
        `;
    }
    if (summary) summary.textContent = '';

    try {
        const url = `${API_BASE_URL}/admin/users/registrations.php?date=${encodeURIComponent(date)}&limit=${encodeURIComponent(limit)}&offset=${encodeURIComponent(offset)}`;
        const response = await fetch(url);
        const data = await response.json();

        if (!data.success) {
            throw new Error(data.message || 'Failed to load registrations');
        }

        const users = Array.isArray(data.users) ? data.users : [];
        renderRegistrationsTable(users);

        if (summary) {
            const total = typeof data.total === 'number' ? data.total : users.length;
            summary.textContent = `Date: ${date} | Showing ${users.length} / ${total} registrations`;
        }
    } catch (error) {
        console.error('Error loading registrations:', error);
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align: center; padding: 40px; color: #f44336;">
                        <i class="fas fa-exclamation-triangle" style="font-size: 24px; margin-bottom: 10px;"></i>
                        <p style="margin: 0;">Failed to load registrations</p>
                        <small style="color:#777;">${escapeHtml(error.message || '')}</small>
                    </td>
                </tr>
            `;
        }
        if (summary) summary.textContent = '';
        if (typeof showNotification === 'function') {
            showNotification('Failed to load registrations', 'error');
        }
    }
}

function renderRegistrationsTable(users) {
    const tbody = document.getElementById('registrationsTableBody');
    if (!tbody) return;

    if (!users || users.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:30px; color:#666;">No registrations found</td></tr>`;
        return;
    }

    tbody.innerHTML = users.map(u => {
        const isActive = String(u.is_active) === '1' || u.is_active === 1 || u.is_active === true;
        const statusBadge = `<span class="badge badge-${isActive ? 'success' : 'danger'}">${isActive ? 'active' : 'inactive'}</span>`;

        return `
            <tr>
                <td><strong>#${escapeHtml(u.id)}</strong></td>
                <td>${escapeHtml(u.name || 'Unknown')}</td>
                <td>${escapeHtml(u.mobile || '')}</td>
                <td>${escapeHtml(u.email || 'N/A')}</td>
                <td>${escapeHtml(formatDateTime(u.created_at))}</td>
                <td>${statusBadge}</td>
            </tr>
        `;
    }).join('');
}

// ==========================================
// ₹5 TRIAL STARTS (DATE-WISE) - IST TIMEZONE
// ==========================================

function setTrialStartsDate(which) {
    const input = document.getElementById('trialStartsDate');
    if (!input) return;

    if (which === 'today') {
        input.value = getIstTodayYYYYMMDD();
        return;
    }
    if (which === 'yesterday') {
        input.value = getIstYesterdayYYYYMMDD();
        return;
    }
    if (typeof which === 'string' && which.trim() !== '') {
        input.value = which.trim();
    }
}

function setTrialStartsDateAndLoad(which) {
    setTrialStartsDate(which);
    loadTrialStartsBySelectedDate();
}

function openTrialStartsPage(which) {
    setTrialStartsDate(which || 'today');
    showPage('trialStarts');
}

function refreshTrialStartsPage() {
    const input = document.getElementById('trialStartsDate');
    if (input && !input.value) {
        input.value = getIstTodayYYYYMMDD();
    }
    loadTrialStartsBySelectedDate();
}

async function loadTrialStartsBySelectedDate() {
    const date = document.getElementById('trialStartsDate')?.value || getIstTodayYYYYMMDD();
    await loadTrialStarts(date);
}

async function loadTrialStarts(date, limit = 200, offset = 0) {
    const tbody = document.getElementById('trialStartsTableBody');
    const summary = document.getElementById('trialStartsSummary');

    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 40px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #6C63FF;"></i>
                    <p style="margin-top: 10px; color: #666;">Loading trial users...</p>
                </td>
            </tr>
        `;
    }
    if (summary) summary.textContent = '';

    try {
        const url = `${API_BASE_URL}/admin/subscriptions/trial_starts.php?date=${encodeURIComponent(date)}&limit=${encodeURIComponent(limit)}&offset=${encodeURIComponent(offset)}`;
        const response = await fetch(url);
        const data = await response.json();

        if (!data.success) {
            throw new Error(data.message || 'Failed to load trial users');
        }

        const trials = Array.isArray(data.trials) ? data.trials : [];
        renderTrialStartsTable(trials);

        if (summary) {
            const total = typeof data.total === 'number' ? data.total : trials.length;
            summary.textContent = `Date: ${date} | Showing ${trials.length} / ${total} trial users`;
        }
    } catch (error) {
        console.error('Error loading trial users:', error);
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align: center; padding: 40px; color: #f44336;">
                        <i class="fas fa-exclamation-triangle" style="font-size: 24px; margin-bottom: 10px;"></i>
                        <p style="margin: 0;">Failed to load trial users</p>
                        <small style="color:#777;">${escapeHtml(error.message || '')}</small>
                    </td>
                </tr>
            `;
        }
        if (summary) summary.textContent = '';
        if (typeof showNotification === 'function') {
            showNotification('Failed to load trial users', 'error');
        }
    }
}

function renderTrialStartsTable(trials) {
    const tbody = document.getElementById('trialStartsTableBody');
    if (!tbody) return;

    if (!trials || trials.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:30px; color:#666;">No trial users found</td></tr>`;
        return;
    }

    tbody.innerHTML = trials.map(t => {
        const status = (t.status || '-').toString();
        const statusColor =
            status === 'active' ? '#4CAF50' :
            status === 'authenticated' ? '#2196F3' :
            status === 'cancelled' ? '#f44336' :
            status === 'expired' ? '#607d8b' :
            '#FF9800';
        const statusBadge = `<span class="badge" style="background:${statusColor}; color:white; padding:4px 10px; border-radius: 12px; font-size: 11px;">${escapeHtml(status)}</span>`;

        const userId = parseInt(t.user_id || 0, 10) || 0;

        return `
            <tr>
                <td><strong>#${escapeHtml(t.subscription_id)}</strong></td>
                <td>${escapeHtml(t.user_name || 'Unknown')}</td>
                <td>${escapeHtml(t.user_mobile || '')}</td>
                <td>${statusBadge}</td>
                <td>${escapeHtml(formatDateTime(t.trial_start))}</td>
                <td>${escapeHtml(formatDateTime(t.trial_end))}</td>
                <td style="text-align:center;">
                    ${userId ? `<button class="btn-icon btn-view" onclick="viewUser(${userId})" title="View User"><i class="fas fa-eye"></i></button>` : '-'}
                </td>
            </tr>
        `;
    }).join('');
}

// ==========================================
// SUBSCRIPTIONS MANAGEMENT
// ==========================================

let allSubscriptions = [];
let allPremiumUsers = [];

function refreshSubscriptionsPage() {
    loadSubscriptions();
    loadPremiumUsers();
}

async function loadSubscriptions() {
    const tbody = document.getElementById('subscriptionsTableBody');
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 40px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #6C63FF;"></i>
                    <p style="margin-top: 10px; color: #666;">Syncing Razorpay statuses...</p>
                </td>
            </tr>
        `;
    }

    try {
        // Auto-sync all Razorpay statuses first
        try {
            await fetch(`${API_BASE_URL}/admin/subscriptions/sync_all_razorpay_status.php`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });
        } catch (syncErr) {
            console.warn('Auto-sync failed:', syncErr);
        }
        
        // Now load subscriptions with updated statuses
        const response = await fetch(`${API_BASE_URL}/admin/subscriptions/list.php?limit=200&offset=0`);
        const data = await response.json();

        if (!data.success) {
            throw new Error(data.message || 'Failed to load subscriptions');
        }

        allSubscriptions = Array.isArray(data.subscriptions) ? data.subscriptions : [];

        // Update stats (defensive: elements may not exist if HTML changed)
        const stats = data.stats || {};
        const activeCount = (parseInt(stats.active_count) || 0) + (parseInt(stats.authenticated_count) || 0);
        const trialCount = parseInt(stats.trial_count) || 0;
        const monthlyRevenue = stats.monthly_revenue != null ? stats.monthly_revenue : 0;
        const totalPayments = parseInt(stats.total_payments_count) || 0;

        const elActive = document.getElementById('subsActiveCount');
        const elTrial = document.getElementById('subsTrialCount');
        const elRevenue = document.getElementById('subsMonthlyRevenue');
        const elPayments = document.getElementById('subsTotalPayments');
        if (elActive) elActive.textContent = activeCount;
        if (elTrial) elTrial.textContent = trialCount;
        if (elRevenue) elRevenue.textContent = `₹${monthlyRevenue}`;
        if (elPayments) elPayments.textContent = totalPayments;

        renderSubscriptionsTable(allSubscriptions);
        filterSubscriptionsTable(); // apply any active filters
    } catch (error) {
        console.error('Error loading subscriptions:', error);
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align: center; padding: 40px; color: #f44336;">
                        <i class="fas fa-exclamation-triangle" style="font-size: 24px; margin-bottom: 10px;"></i>
                        <p style="margin: 0;">Failed to load subscriptions</p>
                        <small style="color:#777;">${escapeHtml(error.message || '')}</small>
                    </td>
                </tr>
            `;
        }
        if (typeof showNotification === 'function') {
            showNotification('Failed to load subscriptions', 'error');
        }
    }
}

function getAccessUntil(sub) {
    // Use whichever exists: current_period_end, trial_end, premium_expires_at
    return sub?.current_period_end || sub?.trial_end || sub?.premium_expires_at || sub?.expires_at || null;
}

// Helper: Parse DB datetime string as UTC (MySQL returns 'YYYY-MM-DD HH:MM:SS' without timezone)
function parseUtcDateTime(dateStr) {
    if (!dateStr) return null;
    // If already ISO format with Z, use as-is
    if (dateStr.includes('T') && dateStr.includes('Z')) {
        return new Date(dateStr);
    }
    // Convert MySQL format 'YYYY-MM-DD HH:MM:SS' to ISO UTC: 'YYYY-MM-DDTHH:MM:SSZ'
    const isoStr = dateStr.replace(' ', 'T') + 'Z';
    return new Date(isoStr);
}

function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const d = parseUtcDateTime(dateStr);
    if (!d || isNaN(d.getTime())) return dateStr;
    // Display in IST (Asia/Kolkata)
    return d.toLocaleString('en-IN', { 
        timeZone: 'Asia/Kolkata',
        day: '2-digit', 
        month: 'short', 
        year: 'numeric', 
        hour: '2-digit', 
        minute: '2-digit' 
    });
}

function renderSubscriptionsTable(subscriptions) {
    const tbody = document.getElementById('subscriptionsTableBody');
    if (!tbody) return;

    if (!subscriptions || subscriptions.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:30px; color:#666;">No subscriptions found</td></tr>`;
        return;
    }

    tbody.innerHTML = subscriptions.map(sub => {
        const accessUntil = getAccessUntil(sub);
        const status = (sub.status || '-').toString();
        const razorpayStatus = (sub.razorpay_status || '-').toString();
        const statusColor =
            status === 'active' ? '#4CAF50' :
            status === 'authenticated' ? '#2196F3' :
            status === 'cancelled' ? '#f44336' :
            status === 'expired' ? '#607d8b' :
            '#FF9800';
        const rzpStatusColor =
            razorpayStatus === 'active' ? '#4CAF50' :
            razorpayStatus === 'authenticated' ? '#2196F3' :
            razorpayStatus === 'cancelled' ? '#f44336' :
            razorpayStatus === 'halted' ? '#f44336' :
            razorpayStatus === 'expired' ? '#607d8b' :
            razorpayStatus === 'pending' ? '#FF9800' :
            razorpayStatus === 'created' ? '#9E9E9E' :
            '#666';

        return `
            <tr data-sub-id="${escapeHtml(sub.id)}"
                data-sub-user="${escapeHtml((sub.user_name || '').toLowerCase())}"
                data-sub-mobile="${escapeHtml((sub.user_mobile || '').toLowerCase())}"
                data-sub-status="${escapeHtml(status.toLowerCase())}">
                <td><strong>#${escapeHtml(sub.id)}</strong></td>
                <td>
                    <div style="font-weight:600;">${escapeHtml(sub.user_name || 'Unknown')}</div>
                    <small style="color:#666;">${escapeHtml(sub.user_mobile || '')}</small>
                </td>
                <td>
                    <div style="font-weight:600;">${escapeHtml(sub.plan_name || 'Premium')}</div>
                    <small style="color:#666;">₹${escapeHtml(sub.amount || '')}/month</small>
                </td>
                <td><span style="color:${statusColor}; font-weight:700;">${escapeHtml(status)}</span></td>
                <td>
                    <span style="color:${rzpStatusColor}; font-weight:600;">${escapeHtml(razorpayStatus)}</span>
                    ${sub.last_payment_status ? `<br><small>Pay: <strong style="color:${
                        sub.last_payment_status === 'captured' || sub.last_payment_status === 'paid' ? '#4CAF50' : 
                        sub.last_payment_status === 'failed' ? '#f44336' : 
                        sub.last_payment_status === 'refunded' || sub.last_payment_status === 'full' || sub.last_payment_status === 'partial' ? '#9C27B0' : 
                        '#FF9800'
                    }">${escapeHtml(sub.last_payment_status)}</strong></small>` : ''}
                </td>
                <td>${escapeHtml(formatDateTime(accessUntil))}</td>
                <td>
                    <button class="btn btn-secondary" style="padding:6px 10px;" onclick="openSubscriptionDetails(${sub.id})" title="View Details">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-secondary" style="padding:6px 10px; background:#ffebee; color:#c62828;" onclick="cancelSubscriptionAdmin(${sub.user_id || 0})" title="Cancel Subscription">
                        <i class="fas fa-ban"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterSubscriptionsTable() {
    const search = (document.getElementById('subsSearch')?.value || '').trim().toLowerCase();
    const status = (document.getElementById('subsStatusFilter')?.value || '').trim().toLowerCase();
    const tbody = document.getElementById('subscriptionsTableBody');
    if (!tbody) return;

    const rows = Array.from(tbody.querySelectorAll('tr'));
    let visible = 0;
    rows.forEach(row => {
        // Skip empty/loading rows
        if (!row.hasAttribute('data-sub-status')) return;

        const name = row.getAttribute('data-sub-user') || '';
        const mobile = row.getAttribute('data-sub-mobile') || '';
        const rowStatus = row.getAttribute('data-sub-status') || '';

        const matchesSearch = !search || name.includes(search) || mobile.includes(search);
        const matchesStatus = !status || rowStatus === status;

        const show = matchesSearch && matchesStatus;
        row.style.display = show ? '' : 'none';
        if (show) visible += 1;
    });

    // If all rows hidden, show a single message row (but only if we actually have data rows)
    const hasDataRows = rows.some(r => r.hasAttribute('data-sub-status'));
    if (hasDataRows) {
        const existingEmpty = tbody.querySelector('tr[data-empty="1"]');
        if (existingEmpty) existingEmpty.remove();
        if (visible === 0) {
            const tr = document.createElement('tr');
            tr.setAttribute('data-empty', '1');
            tr.innerHTML = `<td colspan="7" style="text-align:center; padding:30px; color:#666;">No matching subscriptions</td>`;
            tbody.appendChild(tr);
        }
    }
}

function openSubscriptionDetails(subscriptionId) {
    const sub = allSubscriptions.find(s => String(s.id) === String(subscriptionId));
    if (!sub) return;

    const body = document.getElementById('subscriptionDetailsBody');
    if (!body) return;

    const accessUntil = getAccessUntil(sub);
    const internalStatus = sub.status || '-';
    const razorpayStatus = sub.razorpay_status || '-';
    const internalStatusColor =
        internalStatus === 'active' ? '#4CAF50' :
        internalStatus === 'authenticated' ? '#2196F3' :
        internalStatus === 'cancelled' ? '#f44336' :
        '#FF9800';
    const rzpStatusColor =
        razorpayStatus === 'active' ? '#4CAF50' :
        razorpayStatus === 'authenticated' ? '#2196F3' :
        razorpayStatus === 'cancelled' ? '#f44336' :
        razorpayStatus === 'halted' ? '#f44336' :
        razorpayStatus === 'pending' ? '#FF9800' :
        '#666';

    body.innerHTML = `
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div style="background:#f8f9fa; padding:12px; border-radius:10px;">
                <div style="font-size:12px; color:#666;">User</div>
                <div style="font-weight:700;">${escapeHtml(sub.user_name || 'Unknown')}</div>
                <div style="color:#666;">${escapeHtml(sub.user_mobile || '')}</div>
            </div>
            <div style="background:#f8f9fa; padding:12px; border-radius:10px;">
                <div style="font-size:12px; color:#666;">Subscription</div>
                <div style="font-weight:700;">#${escapeHtml(sub.id)}</div>
                <div style="color:#666;">${escapeHtml(sub.razorpay_subscription_id || '-')}</div>
            </div>
            <div style="background:#f8f9fa; padding:12px; border-radius:10px;">
                <div style="font-size:12px; color:#666;">Plan</div>
                <div style="font-weight:700;">${escapeHtml(sub.plan_name || 'Premium')}</div>
                <div style="color:#666;">₹${escapeHtml(sub.amount || '')}/month</div>
            </div>
            <div style="background:#f8f9fa; padding:12px; border-radius:10px;">
                <div style="font-size:12px; color:#666;">Internal Status</div>
                <div style="font-weight:700; color:${internalStatusColor};">${escapeHtml(internalStatus)}</div>
                <div style="color:#666;">Trial: ${sub.is_trial == 1 ? 'Yes' : 'No'}</div>
            </div>
            <div style="background:#e3f2fd; padding:12px; border-radius:10px; border: 1px solid #90caf9;">
                <div style="font-size:12px; color:#1565c0;">Razorpay Status (Live)</div>
                <div style="font-weight:700; color:${rzpStatusColor};">${escapeHtml(razorpayStatus)}</div>
                ${sub.last_payment_status ? `<div style="margin-top:6px; font-size:12px;">Payment: <strong style="color:${
                    sub.last_payment_status === 'captured' || sub.last_payment_status === 'paid' ? '#4CAF50' : 
                    sub.last_payment_status === 'failed' ? '#f44336' : 
                    sub.last_payment_status === 'refunded' || sub.last_payment_status === 'full' || sub.last_payment_status === 'partial' ? '#9C27B0' : '#FF9800'
                }">${escapeHtml(sub.last_payment_status)}</strong></div>` : ''}
            </div>
            <div style="background:#f8f9fa; padding:12px; border-radius:10px;">
                <div style="font-size:12px; color:#666;">Current Period</div>
                <div style="font-weight:700;">${escapeHtml(formatDateTime(sub.current_period_start))}</div>
                <div style="color:#666;">to ${escapeHtml(formatDateTime(sub.current_period_end))}</div>
            </div>
            <div style="background:#f8f9fa; padding:12px; border-radius:10px;">
                <div style="font-size:12px; color:#666;">Access Until</div>
                <div style="font-weight:700;">${escapeHtml(formatDateTime(accessUntil))}</div>
                <div style="color:#666;">Payments: ${escapeHtml(sub.total_payments || 0)}</div>
            </div>
        </div>
    `;

    openModal('subscriptionDetailsModal');
}

async function cancelSubscriptionAdmin(userId) {
    if (!userId) {
        showNotification('User ID missing for cancellation', 'error');
        return;
    }
    if (!confirm('Cancel this user subscription at period end?')) return;

    try {
        const response = await fetch(`${API_BASE_URL}/subscriptions/cancel.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ user_id: parseInt(userId) })
        });
        const data = await response.json();
        if (data.success) {
            showNotification(data.message || 'Subscription cancelled', 'success');
            refreshSubscriptionsPage();
        } else {
            showNotification(data.message || 'Failed to cancel subscription', 'error');
        }
    } catch (error) {
        console.error('Cancel subscription error:', error);
        showNotification('Error cancelling subscription', 'error');
    }
}

async function syncRazorpayStatus(subscriptionId) {
    if (!subscriptionId) {
        showNotification('Subscription ID missing', 'error');
        return;
    }

    // Find the sync button and show loading state
    const row = document.querySelector(`tr[data-sub-id="${subscriptionId}"]`);
    const syncBtn = row?.querySelector('button[onclick*="syncRazorpayStatus"]');
    if (syncBtn) {
        syncBtn.disabled = true;
        syncBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    }

    try {
        const response = await fetch(`${API_BASE_URL}/admin/subscriptions/sync_razorpay_status.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ subscription_id: parseInt(subscriptionId) })
        });
        const data = await response.json();
        
        if (data.success) {
            // Update the subscription in allSubscriptions array
            const subIndex = allSubscriptions.findIndex(s => String(s.id) === String(subscriptionId));
            if (subIndex !== -1) {
                allSubscriptions[subIndex].razorpay_status = data.data.new_razorpay_status;
                allSubscriptions[subIndex].last_payment_status = data.data.latest_payment_status;
            }
            
            // Re-render the table
            renderSubscriptionsTable(allSubscriptions);
            filterSubscriptionsTable();
            
            const subStatus = data.data.new_razorpay_status || '-';
            const payStatus = data.data.latest_payment_status || 'none';
            showNotification(`Synced! Sub: ${subStatus} | Payment: ${payStatus}`, 'success');
            
            // Log payment details to console for debugging
            if (data.data.latest_payments && data.data.latest_payments.length > 0) {
                console.log('Latest Payments:', data.data.latest_payments);
            }
        } else {
            showNotification(data.message || 'Failed to sync Razorpay status', 'error');
        }
    } catch (error) {
        console.error('Sync Razorpay status error:', error);
        showNotification('Error syncing Razorpay status', 'error');
    } finally {
        // Restore button state if still present
        if (syncBtn) {
            syncBtn.disabled = false;
            syncBtn.innerHTML = '<i class="fas fa-sync-alt"></i>';
        }
    }
}

async function loadPremiumUsers() {
    const tbody = document.getElementById('premiumUsersTableBody');
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" style="text-align: center; padding: 40px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #6C63FF;"></i>
                    <p style="margin-top: 10px; color: #666;">Loading premium users...</p>
                </td>
            </tr>
        `;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/admin/users/list.php?limit=500`);
        const data = await response.json();
        if (!data.success) throw new Error(data.message || 'Failed to load users');

        const users = Array.isArray(data.users) ? data.users : [];
        allPremiumUsers = users.filter(u => String(u.is_premium) === '1' || u.is_premium === 1 || u.is_premium === true);
        renderPremiumUsersTable(allPremiumUsers);
    } catch (error) {
        console.error('Error loading premium users:', error);
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" style="text-align: center; padding: 40px; color: #f44336;">
                        <i class="fas fa-exclamation-triangle" style="font-size: 24px; margin-bottom: 10px;"></i>
                        <p style="margin: 0;">Failed to load premium users</p>
                        <small style="color:#777;">${escapeHtml(error.message || '')}</small>
                    </td>
                </tr>
            `;
        }
    }
}

function renderPremiumUsersTable(users) {
    const tbody = document.getElementById('premiumUsersTableBody');
    if (!tbody) return;

    if (!users || users.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:30px; color:#666;">No premium users</td></tr>`;
        return;
    }

    tbody.innerHTML = users.map(u => `
        <tr>
            <td><strong>#${escapeHtml(u.id)}</strong></td>
            <td>${escapeHtml(u.name || 'Unknown')}</td>
            <td>${escapeHtml(u.mobile || '')}</td>
            <td>${escapeHtml(formatDateTime(u.premium_expires_at))}</td>
            <td>
                <button class="btn btn-secondary" style="padding:8px 12px;" onclick="openGrantPremiumModal(${escapeHtml(u.id)})">
                    <i class="fas fa-edit"></i> Edit
                </button>
                <button class="btn btn-secondary" style="padding:8px 12px; background:#ffebee; color:#c62828;" onclick="revokePremium(${escapeHtml(u.id)}, '${escapeHtml(u.name || 'User').replace(/'/g, "\\'")}')">
                    <i class="fas fa-times"></i> Revoke
                </button>
            </td>
        </tr>
    `).join('');
}

function openGrantPremiumModal(userId = '') {
    const form = document.getElementById('grantPremiumForm');
    if (form) form.reset();

    const result = document.getElementById('searchUserResult');
    if (result) result.textContent = '';

    const custom = document.getElementById('grantCustomDays');
    if (custom) custom.style.display = 'none';

    const idInput = document.getElementById('grantUserId');
    if (idInput && userId) idInput.value = userId;

    const daysSelect = document.getElementById('grantDays');
    if (daysSelect) {
        daysSelect.onchange = function() {
            const customInput = document.getElementById('grantCustomDays');
            if (!customInput) return;
            customInput.style.display = this.value === 'custom' ? 'block' : 'none';
        };
    }

    // Hide duration group when action = revoke
    document.querySelectorAll('input[name="premiumAction"]').forEach(r => {
        r.addEventListener('change', () => {
            const group = document.getElementById('grantDaysGroup');
            const action = document.querySelector('input[name="premiumAction"]:checked')?.value;
            if (group) group.style.display = action === 'revoke' ? 'none' : 'block';
        });
    });

    // Set default visibility
    const group = document.getElementById('grantDaysGroup');
    if (group) group.style.display = 'block';

    openModal('grantPremiumModal');
}

async function searchUserByMobile() {
    const mobile = (document.getElementById('grantUserMobile')?.value || '').trim();
    const resultEl = document.getElementById('searchUserResult');
    if (!resultEl) return;

    if (!mobile) {
        resultEl.innerHTML = '<span style="color:#f44336;">Enter mobile number</span>';
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/admin/users/list.php?search=${encodeURIComponent(mobile)}&limit=20`);
        const data = await response.json();
        if (data.success && data.users && data.users.length > 0) {
            const user = data.users[0];
            const idInput = document.getElementById('grantUserId');
            if (idInput) idInput.value = user.id;
            resultEl.innerHTML = `<span style="color:#4CAF50;">✓ Found: ${escapeHtml(user.name || 'User')} (ID: ${escapeHtml(user.id)})</span>`;
        } else {
            resultEl.innerHTML = '<span style="color:#f44336;">No user found</span>';
        }
    } catch (error) {
        console.error('Search user error:', error);
        resultEl.innerHTML = '<span style="color:#f44336;">Error searching user</span>';
    }
}

async function submitGrantPremium(event) {
    event.preventDefault();

    const userId = parseInt(document.getElementById('grantUserId')?.value || '0');
    if (!userId) {
        showNotification('User ID required', 'error');
        return;
    }

    const action = document.querySelector('input[name="premiumAction"]:checked')?.value || 'grant';
    const daysSelect = document.getElementById('grantDays')?.value || '30';
    const customDays = parseInt(document.getElementById('grantCustomDays')?.value || '0');
    const days = daysSelect === 'custom' ? customDays : parseInt(daysSelect);

    try {
        const response = await fetch(`${API_BASE_URL}/admin/subscriptions/grant_premium.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                user_id: userId,
                is_premium: action === 'grant',
                days: days || 30
            })
        });
        const data = await response.json();
        if (data.success) {
            showNotification(data.message || 'Updated', 'success');
            closeModal('grantPremiumModal');
            refreshSubscriptionsPage();
        } else {
            showNotification(data.message || 'Failed', 'error');
        }
    } catch (error) {
        console.error('Grant premium error:', error);
        showNotification('Error updating premium', 'error');
    }
}

async function revokePremium(userId, userName) {
    if (!confirm(`Revoke premium for ${userName}?`)) return;

    try {
        const response = await fetch(`${API_BASE_URL}/admin/subscriptions/grant_premium.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ user_id: parseInt(userId), is_premium: false, days: 0 })
        });
        const data = await response.json();
        if (data.success) {
            showNotification(data.message || 'Premium revoked', 'success');
            refreshSubscriptionsPage();
        } else {
            showNotification(data.message || 'Failed to revoke', 'error');
        }
    } catch (error) {
        console.error('Revoke premium error:', error);
        showNotification('Error revoking premium', 'error');
    }
}

// Toggle Sidebar
function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('active');
}

// Questions Management Stats
async function loadQuestionsManagementStats() {
    try {
        // Load test categories count
        const testCatResponse = await fetch(`${API_BASE_URL}/admin/test_categories/crud.php`);
        const testCatData = await testCatResponse.json();
        if (testCatData.success && testCatData.data) {
            const countEl = document.getElementById('totalTestCategoriesCount');
            if (countEl) countEl.textContent = testCatData.data.length;
        }
        
        // Load question sessions count
        const sessionsResponse = await fetch(`${API_BASE_URL}/admin/sessions/crud.php`);
        const sessionsData = await sessionsResponse.json();
        if (sessionsData.success && sessionsData.data) {
            const countEl = document.getElementById('totalQuestionSessionsCount');
            if (countEl) countEl.textContent = sessionsData.data.length;
        }
        
        // Load total questions count from analytics
        const analyticsResponse = await fetch(`${API_BASE_URL}/admin/analytics/dashboard.php`);
        const analyticsData = await analyticsResponse.json();
        if (analyticsData.success && analyticsData.overview) {
            const countEl = document.getElementById('totalQuestionsCount');
            if (countEl) countEl.textContent = analyticsData.overview.total_questions || 0;
        }
    } catch (error) {
        console.error('Error loading questions management stats:', error);
    }
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
            
            // Update stats cards (with null checks for removed elements)
            if (document.getElementById('totalUsers')) {
                document.getElementById('totalUsers').textContent = overview.total_users || 0;
            }
            if (document.getElementById('totalExams')) {
                document.getElementById('totalExams').textContent = overview.total_exams || 0;
            }
            if (document.getElementById('totalQuestions')) {
                document.getElementById('totalQuestions').textContent = overview.total_questions || 0;
            }
            if (document.getElementById('testsTaken')) {
                document.getElementById('testsTaken').textContent = overview.total_tests || 0;
            }
            
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
            
            // Update subscription stats cards
            if (document.getElementById('todayRegistrations')) {
                document.getElementById('todayRegistrations').textContent = overview.today_registrations || 0;
            }
            if (document.getElementById('yesterdayRegistrations')) {
                document.getElementById('yesterdayRegistrations').textContent = overview.yesterday_registrations || 0;
            }
            if (document.getElementById('trialStartedToday')) {
                document.getElementById('trialStartedToday').textContent = overview.trial_started_today || 0;
            }
            if (document.getElementById('activePaid299')) {
                document.getElementById('activePaid299').textContent = overview.paid_active_299_count || 0;
            }
            
            // Revenue & Subscription stats
            if (document.getElementById('monthlyRevenue')) {
                document.getElementById('monthlyRevenue').textContent = '₹' + formatNumber(overview.monthly_revenue || 0);
            }
            if (document.getElementById('totalPayments')) {
                document.getElementById('totalPayments').textContent = formatNumber(overview.total_payments || 0);
            }
            if (document.getElementById('totalRevenue')) {
                document.getElementById('totalRevenue').textContent = '₹' + formatNumber(overview.total_revenue || 0) + ' total';
            }
            if (document.getElementById('activeTrialCount')) {
                document.getElementById('activeTrialCount').textContent = formatNumber(overview.active_trial_count || 0);
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
            if (typeof showNotification === 'function') {
                showNotification(data.message || 'Failed to load dashboard statistics', 'error');
            }
            showError(data.message || 'Failed to load dashboard statistics');
        }
    } catch (error) {
        console.error('Error loading dashboard:', error);
        if (typeof showNotification === 'function') {
            showNotification('Dashboard error: ' + (error?.message || 'Unknown error'), 'error');
        }
        showError('Error connecting to database. Please check your connection.');
    }
}

// Helper function to calculate time ago (parses DB datetime as UTC)
function getTimeAgo(dateString) {
    const date = parseUtcDateTime(dateString);
    if (!date || isNaN(date.getTime())) return dateString;
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);
    
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return Math.floor(seconds / 60) + ' minutes ago';
    if (seconds < 86400) return Math.floor(seconds / 3600) + ' hours ago';
    if (seconds < 604800) return Math.floor(seconds / 86400) + ' days ago';
    // Display date in IST
    return date.toLocaleDateString('en-IN', { timeZone: 'Asia/Kolkata' });
}

// Helper function to show errors
function showError(message) {
    // You can implement a toast notification here
    console.error(message);
}

// Users CRUD - Enhanced with Search, Filter, and Pagination
let usersCurrentPage = 1;
let usersPerPage = 50;
let usersTotalCount = 0;
let allUsersCache = [];
let filteredUsers = [];

async function loadUsers(searchQuery = '', resetPage = true) {
    if (resetPage) {
        usersCurrentPage = 1;
    }
    
    try {
        // Build query params
        const params = new URLSearchParams();
        params.append('limit', 1000); // Load more for client-side filtering
        params.append('offset', 0);
        if (searchQuery) {
            params.append('search', searchQuery);
        }
        
        const response = await fetch(`${API_BASE_URL}/admin/users/list.php?${params.toString()}`);
        const data = await response.json();
        
        if (data.success) {
            allUsersCache = data.users;
            usersTotalCount = data.total || data.users.length;
            
            // Apply filters
            filterUsers(false);
            
            // Update dashboard stats
            document.getElementById('totalUsers').textContent = usersTotalCount;
        } else {
            console.error('Failed to load users:', data.message);
        }
    } catch (error) {
        console.error('Error loading users:', error);
        showNotification('Failed to load users', 'error');
    }
}

function filterUsers(updateInfo = true) {
    const premiumFilter = document.getElementById('userPremiumFilter')?.value || '';
    const verificationFilter = document.getElementById('userVerificationFilter')?.value || '';
    const languageFilter = document.getElementById('userLanguageFilter')?.value || '';
    const searchInput = document.getElementById('userSearchInput')?.value?.toLowerCase() || '';
    
    // Filter users based on criteria
    filteredUsers = allUsersCache.filter(user => {
        // Premium filter
        if (premiumFilter) {
            const status = user.subscription_status || 'free';
            const isPremium = user.is_premium || false;
            if (premiumFilter === 'premium' && (!isPremium || status === 'trial' || status === 'free')) return false;
            if (premiumFilter === 'trial' && status !== 'trial') return false;
            if (premiumFilter === 'free' && (isPremium && status !== 'free')) return false;
        }
        
        // Verification filter
        if (verificationFilter) {
            const method = (user.verification_method || 'otp').toLowerCase();
            if (method !== verificationFilter) return false;
        }
        
        // Language filter
        if (languageFilter && user.language !== languageFilter) return false;
        
        // Search filter (if searching locally)
        if (searchInput) {
            const name = (user.name || '').toLowerCase();
            const email = (user.email || '').toLowerCase();
            const mobile = (user.mobile || '').toLowerCase();
            if (!name.includes(searchInput) && !email.includes(searchInput) && !mobile.includes(searchInput)) {
                return false;
            }
        }
        
        return true;
    });
    
    // Update search info
    if (updateInfo) {
        updateUserSearchInfo();
    }
    
    // Render current page
    renderUsersTable();
    updateUsersPagination();
}

function renderUsersTable() {
    const startIdx = (usersCurrentPage - 1) * usersPerPage;
    const endIdx = startIdx + usersPerPage;
    const pageUsers = filteredUsers.slice(startIdx, endIdx);
    
    users = pageUsers; // Update global for compatibility
    
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;
    
    if (pageUsers.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; padding: 40px; color: #6b7280;">
                    <i class="fas fa-search" style="font-size: 48px; margin-bottom: 15px; opacity: 0.3;"></i>
                    <p style="font-size: 16px; margin: 0;">No users found matching your criteria</p>
                    <p style="font-size: 13px; margin-top: 5px;">Try adjusting your search or filters</p>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = pageUsers.map(user => {
        // Verification method badge (Truecaller or OTP)
        const verificationMethod = user.verification_method || 'otp';
        const isTruecaller = verificationMethod.toLowerCase() === 'truecaller';
        const verificationBadge = isTruecaller 
            ? '<span class="badge" style="background: linear-gradient(135deg, #0077B5 0%, #00a0dc 100%); color: white; padding: 4px 8px; border-radius: 12px; font-size: 11px;"><i class="fas fa-phone-alt"></i> Truecaller</span>'
            : '<span class="badge" style="background: linear-gradient(135deg, #4CAF50 0%, #66BB6A 100%); color: white; padding: 4px 8px; border-radius: 12px; font-size: 11px;"><i class="fas fa-sms"></i> OTP</span>';
        
        // Premium status badge
        const isPremium = user.is_premium || false;
        const subscriptionStatus = user.subscription_status || 'free';
        let premiumBadge;
        if (isPremium) {
            const statusText = subscriptionStatus.charAt(0).toUpperCase() + subscriptionStatus.slice(1);
            premiumBadge = `<span class="badge" style="background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%); color: #333; padding: 4px 8px; border-radius: 12px; font-size: 11px;"><i class="fas fa-crown"></i> ${statusText}</span>`;
        } else {
            premiumBadge = '<span class="badge" style="background: #e0e0e0; color: #666; padding: 4px 8px; border-radius: 12px; font-size: 11px;">Free</span>';
        }
        
        return `
        <tr>
            <td>#U${user.id.toString().padStart(3, '0')}</td>
            <td><strong>${escapeHtml(user.name)}</strong></td>
            <td>${escapeHtml(user.email) || '<span style="color:#9ca3af">N/A</span>'}</td>
            <td><a href="tel:${user.mobile}" style="color: #6C63FF; text-decoration: none;">${user.mobile || 'N/A'}</a></td>
            <td>${verificationBadge}</td>
            <td>${premiumBadge}</td>
            <td>${user.language === 'en' ? 'English' : 'Tamil'}</td>
            <td><span class="badge badge-${user.is_active ? 'success' : 'danger'}">${user.is_active ? 'active' : 'inactive'}</span></td>
            <td>
                <button class="btn-icon btn-view" onclick="viewUser(${user.id})" title="View Details"><i class="fas fa-eye"></i></button>
                <button class="btn-icon btn-edit" onclick="editUser(${user.id})" title="Edit User"><i class="fas fa-edit"></i></button>
                <button class="btn-icon btn-delete" onclick="showDeleteConfirm('user', ${user.id}, '${escapeHtml(user.name)}')" title="Delete User"><i class="fas fa-trash"></i></button>
            </td>
        </tr>
        `;
    }).join('');
}

function updateUsersPagination() {
    const totalPages = Math.ceil(filteredUsers.length / usersPerPage);
    const startIdx = (usersCurrentPage - 1) * usersPerPage + 1;
    const endIdx = Math.min(usersCurrentPage * usersPerPage, filteredUsers.length);
    
    // Update counts
    const showingFrom = document.getElementById('usersShowingFrom');
    const showingTo = document.getElementById('usersShowingTo');
    const totalCount = document.getElementById('usersTotalCount');
    const pageInfo = document.getElementById('usersPageInfo');
    const prevBtn = document.getElementById('usersPrevBtn');
    const nextBtn = document.getElementById('usersNextBtn');
    
    if (showingFrom) showingFrom.textContent = filteredUsers.length > 0 ? startIdx : 0;
    if (showingTo) showingTo.textContent = endIdx;
    if (totalCount) totalCount.textContent = filteredUsers.length;
    if (pageInfo) pageInfo.textContent = `Page ${usersCurrentPage} of ${totalPages || 1}`;
    
    if (prevBtn) prevBtn.disabled = usersCurrentPage <= 1;
    if (nextBtn) nextBtn.disabled = usersCurrentPage >= totalPages;
}

function loadUsersPrevPage() {
    if (usersCurrentPage > 1) {
        usersCurrentPage--;
        renderUsersTable();
        updateUsersPagination();
    }
}

function loadUsersNextPage() {
    const totalPages = Math.ceil(filteredUsers.length / usersPerPage);
    if (usersCurrentPage < totalPages) {
        usersCurrentPage++;
        renderUsersTable();
        updateUsersPagination();
    }
}

function updateUserSearchInfo() {
    const infoDiv = document.getElementById('userSearchInfo');
    const infoText = document.getElementById('userSearchResultText');
    
    if (!infoDiv || !infoText) return;
    
    const searchInput = document.getElementById('userSearchInput')?.value || '';
    const premiumFilter = document.getElementById('userPremiumFilter')?.value || '';
    const verificationFilter = document.getElementById('userVerificationFilter')?.value || '';
    const languageFilter = document.getElementById('userLanguageFilter')?.value || '';
    
    const hasFilters = searchInput || premiumFilter || verificationFilter || languageFilter;
    
    if (hasFilters) {
        let filterParts = [];
        if (searchInput) filterParts.push(`"${searchInput}"`);
        if (premiumFilter) filterParts.push(premiumFilter + ' users');
        if (verificationFilter) filterParts.push(verificationFilter + ' verified');
        if (languageFilter) filterParts.push(languageFilter === 'en' ? 'English' : 'Tamil');
        
        infoText.innerHTML = `Found <strong>${filteredUsers.length}</strong> users${filterParts.length ? ' matching: ' + filterParts.join(', ') : ''}`;
        infoDiv.style.display = 'block';
    } else {
        infoDiv.style.display = 'none';
    }
}

function searchUsers() {
    const searchInput = document.getElementById('userSearchInput')?.value?.trim() || '';
    usersCurrentPage = 1;
    
    if (searchInput.length >= 2 || searchInput === '') {
        // For server-side search
        loadUsers(searchInput, true);
    } else if (searchInput.length > 0) {
        showNotification('Please enter at least 2 characters to search', 'warning');
    }
}

function handleUserSearchKeyup(event) {
    // Search on Enter key
    if (event.key === 'Enter') {
        searchUsers();
    }
    // Also do local filtering as user types
    if (allUsersCache.length > 0) {
        filterUsers();
    }
}

function clearUserFilters() {
    const searchInput = document.getElementById('userSearchInput');
    const premiumFilter = document.getElementById('userPremiumFilter');
    const verificationFilter = document.getElementById('userVerificationFilter');
    const languageFilter = document.getElementById('userLanguageFilter');
    
    if (searchInput) searchInput.value = '';
    if (premiumFilter) premiumFilter.value = '';
    if (verificationFilter) verificationFilter.value = '';
    if (languageFilter) languageFilter.value = '';
    
    usersCurrentPage = 1;
    loadUsers('', true);
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

async function viewUser(id) {
    // Show loading
    showNotification('Loading user details...', 'info');
    
    try {
        // Fetch full user details from API
        const response = await fetch(`${API_BASE_URL}/admin/users/get_details.php?user_id=${id}`);
        const data = await response.json();
        
        let user, performance, testHistory;
        
        if (data.success) {
            user = data.user || {};
            performance = data.performance || {};
            testHistory = data.test_history || [];
        } else {
            // Fall back to cached user data
            user = users.find(u => u.id === id) || {};
            performance = {};
            testHistory = [];
        }
        
        // Get stats from performance data or use defaults
        const testsTaken = performance.total_tests || user.total_tests || 0;
        const avgScore = performance.avg_score ? parseFloat(performance.avg_score).toFixed(1) : '0.0';
        const rank = performance.rank || user.rank || '-';
        const studyTime = Math.round((performance.total_time_spent || 0) / 60) || 0;
        const currentStreak = performance.current_streak || 0;
        const longestStreak = performance.longest_streak || currentStreak;
        
        // Premium status
        const isPremium = user.is_premium || false;
        const subscriptionStatus = user.subscription_status || 'free';
        const premiumExpires = user.premium_expires_at;
        
        // Premium badge HTML
        let premiumBadge = '';
        if (isPremium) {
            let badgeColor = '#4CAF50';
            let badgeText = 'Premium';
            if (subscriptionStatus === 'trial') {
                badgeColor = '#FF9800';
                badgeText = 'Trial';
            }
            premiumBadge = `<span style="background:${badgeColor}; color:white; padding:4px 12px; border-radius:20px; font-size:12px; margin-left:10px;"><i class="fas fa-crown"></i> ${badgeText}</span>`;
        }
        
        // Account status
        const isActive = user.is_active === 1 || user.is_active === true || user.is_active === '1';
        const statusText = isActive ? 'ACTIVE' : 'INACTIVE';
        const statusColor = isActive ? '#4CAF50' : '#95a5a6';
    
    // Create a detailed modal view with tabs
    const modalHTML = `
        <div id="viewUserModal" class="modal active" style="overflow-y: auto;">
            <div class="modal-content modal-large" style="max-width: 1200px; max-height: 95vh; overflow-y: auto;">
                <div class="modal-header" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 25px;">
                    <div>
                        <h2 style="margin: 0; color: white;"><i class="fas fa-user-circle"></i> ${user.name || 'Unknown'}${premiumBadge}</h2>
                        <p style="margin: 5px 0 0 0; opacity: 0.9;">${user.email || 'No email'} | ${user.mobile || 'No mobile'}</p>
                </div>
                    <span class="close" onclick="closeViewUserModal()" style="color: white; opacity: 0.9;">&times;</span>
                </div>
                
                <div class="modal-body" style="padding: 0;">
                    <!-- Tabs -->
                    <div style="display: flex; border-bottom: 2px solid #e0e0e0; background: #f8f9fa; padding: 0 20px;">
                        <button class="user-detail-tab active" onclick="switchUserTab('overview')" id="overviewTab" style="flex: 1; padding: 15px; border: none; background: none; cursor: pointer; font-weight: 600; border-bottom: 3px solid #6C63FF; transition: all 0.3s;">
                            <i class="fas fa-user"></i> Overview
                        </button>
                        <button class="user-detail-tab" onclick="switchUserTab('performance')" id="performanceTab" style="flex: 1; padding: 15px; border: none; background: none; cursor: pointer; font-weight: 600; border-bottom: 3px solid transparent; transition: all 0.3s;">
                            <i class="fas fa-chart-bar"></i> Stats
                        </button>
                        <button class="user-detail-tab" onclick="switchUserTab('history')" id="historyTab" style="flex: 1; padding: 15px; border: none; background: none; cursor: pointer; font-weight: 600; border-bottom: 3px solid transparent; transition: all 0.3s;">
                            <i class="fas fa-history"></i> History
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
                                        <strong style="font-size: 16px; color: #333;">${user.name || 'N/A'}</strong>
                            </div>
                        </div>
                        
                                <div class="detail-card" style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); display: flex; align-items: center; gap: 15px;">
                                    <div class="detail-icon" style="background: #4ECDC4; width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px;">
                                <i class="fas fa-envelope"></i>
                            </div>
                            <div class="detail-info">
                                        <label style="font-size: 12px; color: #999; margin-bottom: 3px; display: block;">Email Address</label>
                                        <strong style="font-size: 14px; color: #333;">${user.email || 'N/A'}</strong>
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
                                <i class="fas fa-crown"></i>
                            </div>
                            <div class="detail-info">
                                        <label style="font-size: 12px; color: #999; margin-bottom: 3px; display: block;">Subscription</label>
                                        <strong style="font-size: 16px; color: ${isPremium ? '#4CAF50' : '#666'};">${isPremium ? subscriptionStatus.charAt(0).toUpperCase() + subscriptionStatus.slice(1) : 'Free'}</strong>
                                        ${premiumExpires ? `<small style="display:block; color:#999; font-size:11px;">Expires: ${parseUtcDateTime(premiumExpires) ? parseUtcDateTime(premiumExpires).toLocaleDateString('en-IN', { timeZone: 'Asia/Kolkata' }) : premiumExpires}</small>` : ''}
                            </div>
                        </div>
                        
                                <div class="detail-card" style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); display: flex; align-items: center; gap: 15px;">
                                    <div class="detail-icon" style="background: #FFD93D; width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px;">
                                <i class="fas fa-language"></i>
                            </div>
                            <div class="detail-info">
                                        <label style="font-size: 12px; color: #999; margin-bottom: 3px; display: block;">Preferred Language</label>
                                        <strong style="font-size: 16px; color: #333;">${user.language === 'ta' ? 'தமிழ் (Tamil)' : 'English'}</strong>
                            </div>
                        </div>
                        
                                <div class="detail-card" style="background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); display: flex; align-items: center; gap: 15px;">
                                    <div class="detail-icon" style="background: ${statusColor}; width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: white; font-size: 20px;">
                                <i class="fas fa-circle"></i>
                            </div>
                            <div class="detail-info">
                                        <label style="font-size: 12px; color: #999; margin-bottom: 3px; display: block;">Account Status</label>
                                        <strong style="font-size: 16px; color: ${statusColor};">${statusText}</strong>
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
                                ${testHistory.length > 0 ? `
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
                                        ${testHistory.slice(0, 5).map(test => {
                                            const testName = test.session_name || test.name || 'Unknown Test';
                                            const testCategory = test.category_name || test.category || '-';
                                            const score = test.correct_answers || test.score || 0;
                                            const total = test.total_questions || test.total || 0;
                                            const percentage = test.percentage || (total > 0 ? Math.round((score/total)*100) : 0);
                                            const timeTaken = test.time_taken || test.time || '-';
                                            const testDate = test.submitted_at ? (parseUtcDateTime(test.submitted_at)?.toLocaleDateString('en-IN', { timeZone: 'Asia/Kolkata' }) || '-') : (test.date || '-');
                                            const status = percentage >= 40 ? 'passed' : 'failed';
                                            return `
                                            <tr style="border-bottom: 1px solid #f0f0f0;">
                                                <td style="padding: 12px;">
                                                    <strong style="color: #333;">${testName}</strong><br>
                                                    <small style="color: #999;">${testCategory}</small>
                                                </td>
                                                <td style="padding: 12px; text-align: center;">
                                                    <strong style="color: #6C63FF; font-size: 16px;">${score}/${total}</strong><br>
                                                    <small style="color: #999;">${percentage}%</small>
                                                </td>
                                                <td style="padding: 12px; text-align: center;">
                                                    <span style="color: #666;">${timeTaken} min</span>
                                                </td>
                                                <td style="padding: 12px; text-align: center;">
                                                    <span style="color: #666;">${testDate}</span>
                                                </td>
                                                <td style="padding: 12px; text-align: center;">
                                                    <span class="badge badge-${status === 'passed' ? 'success' : 'danger'}" style="padding: 5px 12px; border-radius: 20px; font-size: 11px;">${status.toUpperCase()}</span>
                                                </td>
                                            </tr>
                                        `}).join('')}
                                    </tbody>
                                </table>
                                ` : `<p style="text-align: center; color: #999; padding: 30px;"><i class="fas fa-inbox" style="font-size: 40px; display: block; margin-bottom: 15px;"></i>No tests taken yet</p>`}
                            </div>
                            </div>
                        
                        <!-- Performance Tab -->
                        <div id="performanceContent" class="user-tab-content" style="display: none;">
                            <div style="background: white; padding: 25px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); margin-bottom: 20px;">
                                <h3 style="margin: 0 0 20px 0;"><i class="fas fa-chart-bar"></i> Performance Summary</h3>
                                ${testHistory.length > 0 ? `
                                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;">
                                    <div style="background: linear-gradient(135deg, #6C63FF, #5a52d5); padding: 20px; border-radius: 10px; color: white; text-align: center;">
                                        <div style="font-size: 32px; font-weight: bold;">${testsTaken}</div>
                                        <div style="font-size: 13px; opacity: 0.9;">Total Tests</div>
                                    </div>
                                    <div style="background: linear-gradient(135deg, #4CAF50, #45a049); padding: 20px; border-radius: 10px; color: white; text-align: center;">
                                        <div style="font-size: 32px; font-weight: bold;">${avgScore}%</div>
                                        <div style="font-size: 13px; opacity: 0.9;">Average Score</div>
                                    </div>
                                    <div style="background: linear-gradient(135deg, #FF9800, #f57c00); padding: 20px; border-radius: 10px; color: white; text-align: center;">
                                        <div style="font-size: 32px; font-weight: bold;">${testHistory.filter(t => (t.percentage || 0) >= 40).length}</div>
                                        <div style="font-size: 13px; opacity: 0.9;">Tests Passed</div>
                                    </div>
                                    <div style="background: linear-gradient(135deg, #FF6B6B, #e85d5d); padding: 20px; border-radius: 10px; color: white; text-align: center;">
                                        <div style="font-size: 32px; font-weight: bold;">${testHistory.filter(t => (t.percentage || 0) < 40).length}</div>
                                        <div style="font-size: 13px; opacity: 0.9;">Tests Failed</div>
                                    </div>
                                </div>
                                ` : `<p style="text-align: center; color: #999; padding: 30px;"><i class="fas fa-chart-line" style="font-size: 40px; display: block; margin-bottom: 15px;"></i>No performance data yet</p>`}
                            </div>
                        </div>
                        
                        <!-- Test History Tab -->
                        <div id="historyContent" class="user-tab-content" style="display: none;">
                            <div style="background: white; padding: 25px; border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.08);">
                                <h3 style="margin: 0 0 20px 0;"><i class="fas fa-list-alt"></i> Complete Test History (${testHistory.length} tests)</h3>
                                ${testHistory.length > 0 ? `
                                <div style="max-height: 500px; overflow-y: auto;">
                                    <table class="mini-table" style="width: 100%; border-collapse: collapse;">
                                        <thead style="position: sticky; top: 0; background: white; z-index: 10;">
                                            <tr style="background: #f8f9fa;">
                                                <th style="padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0;">#</th>
                                                <th style="padding: 12px; text-align: left; border-bottom: 2px solid #e0e0e0;">Test Name</th>
                                                <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Score</th>
                                                <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">%</th>
                                                <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Date</th>
                                                <th style="padding: 12px; text-align: center; border-bottom: 2px solid #e0e0e0;">Status</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${testHistory.map((test, index) => {
                                                const testName = test.session_name || test.name || 'Unknown Test';
                                                const score = test.correct_answers || test.score || 0;
                                                const total = test.total_questions || test.total || 0;
                                                const percentage = test.percentage || (total > 0 ? Math.round((score/total)*100) : 0);
                                                const testDate = test.submitted_at ? (parseUtcDateTime(test.submitted_at)?.toLocaleDateString('en-IN', { timeZone: 'Asia/Kolkata' }) || '-') : (test.date || '-');
                                                const status = percentage >= 40 ? 'passed' : 'failed';
                                                return `
                                                <tr style="border-bottom: 1px solid #f0f0f0; ${status === 'failed' ? 'background: #fff5f5;' : ''}">
                                                    <td style="padding: 12px; color: #999;">${index + 1}</td>
                                                    <td style="padding: 12px;">
                                                        <strong style="color: #333;">${testName}</strong>
                                                    </td>
                                                    <td style="padding: 12px; text-align: center;">
                                                        <strong style="color: ${percentage >= 75 ? '#4CAF50' : percentage >= 50 ? '#FFD93D' : '#FF6B6B'}; font-size: 15px;">${score}/${total}</strong>
                                                    </td>
                                                    <td style="padding: 12px; text-align: center;">
                                                        <strong style="color: ${percentage >= 75 ? '#4CAF50' : percentage >= 50 ? '#FF9800' : '#FF6B6B'};">${percentage}%</strong>
                                                    </td>
                                                    <td style="padding: 12px; text-align: center; color: #666; font-size: 12px;">${testDate}</td>
                                                    <td style="padding: 12px; text-align: center;">
                                                        <span class="badge badge-${status === 'passed' ? 'success' : 'danger'}" style="padding: 5px 12px; border-radius: 20px; font-size: 10px;">${status.toUpperCase()}</span>
                                                    </td>
                                                </tr>
                                            `}).join('')}
                                        </tbody>
                                    </table>
                                </div>
                                ` : `<p style="text-align: center; color: #999; padding: 30px;"><i class="fas fa-inbox" style="font-size: 40px; display: block; margin-bottom: 15px;"></i>No test history available</p>`}
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
    
    } catch (error) {
        console.error('Error loading user details:', error);
        showNotification('Error loading user details', 'error');
    }
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
        // First try to get user from local cache
        let user = users.find(u => u.id === id);
        
        // If not in cache, fetch from API
        if (!user) {
            const response = await fetch(`${API_BASE_URL}/admin/users/get_details.php?user_id=${id}`);
            const data = await response.json();
            
            if (data.success && data.user) {
                user = data.user;
            } else {
                showNotification('Failed to load user details', 'error');
                return;
            }
        }
        
        // Populate form
        document.getElementById('userName').value = user.name || '';
        document.getElementById('userEmail').value = user.email || '';
        document.getElementById('userMobile').value = user.mobile || '';
        
        if (document.getElementById('userDistrict')) {
            document.getElementById('userDistrict').value = user.district || '';
        }
        if (document.getElementById('userEducation')) {
            document.getElementById('userEducation').value = user.education || '';
        }
        if (document.getElementById('userAge')) {
            document.getElementById('userAge').value = user.age || '';
        }
        
        document.getElementById('userLanguage').value = user.language || 'en';
        
        // Store user ID for update
        document.getElementById('userForm').dataset.userId = id;
        document.getElementById('userForm').dataset.editMode = 'true';
        
        // Update modal title
        const modalHeader = document.querySelector('#userModal .modal-header h2');
        if (modalHeader) {
            modalHeader.textContent = 'Edit User';
        }
        
        openModal('userModal');
    } catch (error) {
        console.error('Error loading user:', error);
        showNotification('Error loading user details', 'error');
    }
}

// Delete confirmation modal state
let pendingDeleteType = null;
let pendingDeleteId = null;

function showDeleteConfirm(type, id, name) {
    pendingDeleteType = type;
    pendingDeleteId = id;
    
    const modal = document.getElementById('deleteConfirmModal');
    const title = document.getElementById('deleteConfirmTitle');
    const message = document.getElementById('deleteConfirmMessage');
    
    // Customize message based on type
    if (type === 'user') {
        title.textContent = 'Delete User?';
        message.innerHTML = `Are you sure you want to delete <strong>"${name}"</strong>?<br><small style="color:#888;">User ID: #U${id.toString().padStart(3, '0')}</small>`;
    } else if (type === 'question') {
        title.textContent = 'Delete Question?';
        message.innerHTML = `Are you sure you want to delete this question?`;
    } else if (type === 'session') {
        title.textContent = 'Delete Session?';
        message.innerHTML = `Are you sure you want to delete <strong>"${name}"</strong>?<br><small style="color:#888;">All questions in this session will be deleted.</small>`;
    } else if (type === 'category') {
        title.textContent = 'Delete Category?';
        message.innerHTML = `Are you sure you want to delete <strong>"${name}"</strong>?`;
    } else {
        title.textContent = 'Confirm Delete?';
        message.innerHTML = `Are you sure you want to delete this item?`;
    }
    
    modal.classList.add('active');
}

function closeDeleteConfirmModal() {
    const modal = document.getElementById('deleteConfirmModal');
    modal.classList.remove('active');
    pendingDeleteType = null;
    pendingDeleteId = null;
}

async function confirmDeleteAction() {
    if (!pendingDeleteType || !pendingDeleteId) return;
    
    const type = pendingDeleteType;
    const id = pendingDeleteId;
    
    closeDeleteConfirmModal();
    
    if (type === 'user') {
        await executeDeleteUser(id);
    } else if (type === 'question') {
        await executeDeleteQuestion(id);
    } else if (type === 'session') {
        await executeDeleteSession(id);
    } else if (type === 'category') {
        await executeDeleteCategory(id);
    }
}

async function executeDeleteUser(id) {
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

// Keep old function for backward compatibility
async function deleteUser(id) {
    const user = users.find(u => u.id === id);
    showDeleteConfirm('user', id, user ? user.name : 'Unknown');
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
        // Fetch both exam categories and test categories
        const [examResponse, testResponse] = await Promise.all([
            fetch(`${API_BASE_URL}/admin/exam_categories/crud.php`),
            fetch(`${API_BASE_URL}/admin/test_categories/crud.php`)
        ]);
        
        const examData = await examResponse.json();
        const testData = await testResponse.json();
        
        if (testData.success) {
            testCategories = testData.categories;
            const examCategories = examData.success ? examData.categories : [];
            
            // Group test categories by exam_category_id
            const groupedByExam = {};
            
            // Initialize groups for all exam categories
            examCategories.forEach(exam => {
                groupedByExam[exam.id] = {
                    examId: exam.id,
                    examName: exam.name,
                    examIcon: exam.icon || 'fas fa-book',
                    examColor: exam.color || '#6C63FF',
                    testCategories: []
                };
            });
            
            // Add "Uncategorized" group for test categories without exam
            groupedByExam['uncategorized'] = {
                examId: null,
                examName: 'Uncategorized',
                examIcon: 'fas fa-folder',
                examColor: '#7F8C8D',
                testCategories: []
            };
            
            // Group test categories
            testCategories.forEach(cat => {
                const examId = cat.exam_category_id || 'uncategorized';
                if (groupedByExam[examId]) {
                    groupedByExam[examId].testCategories.push(cat);
                } else {
                    groupedByExam['uncategorized'].testCategories.push(cat);
                }
            });
            
            // Render the grouped cards
            const container = document.getElementById('testCategoriesByExamGrid');
            if (container) {
                let html = '';
                
                Object.values(groupedByExam).forEach(group => {
                    // Skip empty groups (except show message if all empty)
                    if (group.testCategories.length === 0 && group.examId !== null) return;
                    
                    html += `
                    <div class="exam-category-card" style="background: white; border-radius: 16px; margin-bottom: 24px; box-shadow: 0 4px 15px rgba(0,0,0,0.08); overflow: hidden;">
                        <div class="exam-card-header" style="background: linear-gradient(135deg, ${group.examColor}, ${adjustColor(group.examColor, -20)}); padding: 20px 24px; color: white; display: flex; align-items: center; justify-content: space-between;">
                            <div style="display: flex; align-items: center; gap: 15px;">
                                <div style="width: 50px; height: 50px; background: rgba(255,255,255,0.2); border-radius: 12px; display: flex; align-items: center; justify-content: center;">
                                    <i class="${group.examIcon}" style="font-size: 24px;"></i>
                                </div>
                                <div>
                                    <h3 style="margin: 0; font-size: 20px; font-weight: 600;">${group.examName}</h3>
                                    <p style="margin: 4px 0 0 0; opacity: 0.9; font-size: 14px;">${group.testCategories.length} Test Categories</p>
                                </div>
                            </div>
                            <button class="btn" onclick="openTestCategoryModalForExam(${group.examId})" style="background: rgba(255,255,255,0.2); color: white; border: none; padding: 10px 16px;">
                                <i class="fas fa-plus"></i> Add
                            </button>
                        </div>
                        <div class="exam-card-body" style="padding: 20px;">
                            ${group.testCategories.length === 0 ? `
                                <div style="text-align: center; padding: 30px; color: #999;">
                                    <i class="fas fa-folder-open" style="font-size: 48px; margin-bottom: 15px; opacity: 0.5;"></i>
                                    <p style="margin: 0;">No test categories yet</p>
                                    <button class="btn btn-primary" onclick="openTestCategoryModalForExam(${group.examId})" style="margin-top: 15px;">
                                        <i class="fas fa-plus"></i> Add First Category
                                    </button>
                                </div>
                            ` : `
                                <div class="test-categories-list" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px;">
                                    ${group.testCategories.map(cat => {
                                        const isActive = cat.is_active == 1 || cat.is_active === true;
                                        const imagePath = cat.image_path || cat.image;
                                        const imageTag = imagePath ? `<img src="../${imagePath}" alt="${cat.name}" style="position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; z-index: 1;" onerror="this.style.display='none';">` : '';
                                        const avatarHtml = `
                                            <div style="width: 40px; height: 40px; background: ${cat.color || group.examColor}; border-radius: 10px; overflow: hidden; position: relative; display: flex; align-items: center; justify-content: center;">
                                                <i class="${cat.icon || 'fas fa-book'}" style="color: white; font-size: 18px; position: relative; z-index: 0;"></i>
                                                ${imageTag}
                                            </div>
                                        `;
                                        return `
                                        <div class="test-category-item" style="background: ${isActive ? '#f8f9fa' : '#e9ecef'}; border-radius: 12px; padding: 16px; border-left: 4px solid ${cat.color || group.examColor}; ${!isActive ? 'opacity: 0.6;' : ''} transition: all 0.3s;">
                                            <div style="display: flex; align-items: flex-start; justify-content: space-between;">
                                                <div style="display: flex; align-items: center; gap: 12px;">
                                                    ${avatarHtml}
                                                    <div>
                                                        <h4 style="margin: 0; font-size: 15px; font-weight: 600; color: #2C3E50;">${cat.name}</h4>
                                                        <p style="margin: 4px 0 0 0; font-size: 12px; color: #7F8C8D;">${cat.description || 'No description'}</p>
                                                    </div>
                                                </div>
                                                <span class="badge ${isActive ? 'badge-success' : 'badge-danger'}" style="font-size: 10px;">${isActive ? 'Active' : 'Inactive'}</span>
                                            </div>
                                            <div style="display: flex; gap: 8px; margin-top: 12px; justify-content: flex-end;">
                                                <button class="btn-icon btn-view" onclick="event.stopPropagation(); toggleTestCategoryStatus(${cat.id}, ${isActive ? 0 : 1})" title="${isActive ? 'Deactivate' : 'Activate'}">
                                                    <i class="fas fa-${isActive ? 'eye-slash' : 'eye'}"></i>
                                                </button>
                                                <button class="btn-icon btn-edit" onclick="event.stopPropagation(); editTestCategory(${cat.id})" title="Edit">
                                                    <i class="fas fa-edit"></i>
                                                </button>
                                                <button class="btn-icon btn-delete" onclick="event.stopPropagation(); deleteTestCategory(${cat.id})" title="Delete">
                                                    <i class="fas fa-trash"></i>
                                                </button>
                                            </div>
                                        </div>
                                        `;
                                    }).join('')}
                                </div>
                            `}
                        </div>
                    </div>
                    `;
                });
                
                // If no exam categories at all, show empty state
                if (html === '') {
                    html = `
                        <div style="text-align: center; padding: 60px; background: white; border-radius: 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.08);">
                            <i class="fas fa-tags" style="font-size: 64px; color: #ddd; margin-bottom: 20px;"></i>
                            <h3 style="color: #666; margin-bottom: 10px;">No Test Categories Yet</h3>
                            <p style="color: #999; margin-bottom: 20px;">Create exam categories first, then add test categories under them.</p>
                            <button class="btn btn-primary" onclick="showPage('examCategories')">
                                <i class="fas fa-plus"></i> Create Exam Category
                            </button>
                        </div>
                    `;
                }
                
                container.innerHTML = html;
            }
            
            // Update total count if element exists
            const totalTestsElement = document.getElementById('totalTests');
            if (totalTestsElement) {
                totalTestsElement.textContent = testCategories.length;
            }
        } else {
            console.error('Failed to load test categories:', testData.message);
        }
    } catch (error) {
        console.error('Error loading test categories:', error);
        showNotification('Failed to load test categories', 'error');
    }
}

// Helper function to darken/lighten color
function adjustColor(color, amount) {
    const clamp = (num) => Math.min(255, Math.max(0, num));
    
    // Remove # if present
    color = color.replace('#', '');
    
    // Parse hex color
    let r = parseInt(color.substring(0, 2), 16);
    let g = parseInt(color.substring(2, 4), 16);
    let b = parseInt(color.substring(4, 6), 16);
    
    // Adjust
    r = clamp(r + amount);
    g = clamp(g + amount);
    b = clamp(b + amount);
    
    // Convert back to hex
    return '#' + [r, g, b].map(x => x.toString(16).padStart(2, '0')).join('');
}

// Open test category modal pre-filled with exam category
function openTestCategoryModalForExam(examId) {
    openModal('testCategoryModal');
    // Pre-select the exam category in the dropdown
    setTimeout(() => {
        const examSelect = document.getElementById('testCategoryExam');
        if (examSelect && examId) {
            examSelect.value = examId;
        }
    }, 100);
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
            resetTestCategoryImageUpload();
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
        
        // Load existing image (if any) into modal
        const imagePath = cat.image_path || cat.image;
        if (imagePath) {
            document.getElementById('testCategoryImage').value = cat.image || '';
            document.getElementById('testCategoryImagePath').value = imagePath;
            
            const preview = document.getElementById('testCategoryImagePreview');
            const previewContainer = document.getElementById('testCategoryImagePreviewContainer');
            const uploadArea = document.getElementById('testCategoryImageUploadArea');
            const info = document.getElementById('testCategoryImageInfo');
            
            if (preview && previewContainer && uploadArea) {
                preview.src = `../${imagePath}`;
                previewContainer.style.display = 'block';
                uploadArea.style.display = 'none';
                if (info) {
                    info.innerHTML = '<i class="fas fa-check-circle" style="color: #4CAF50;"></i> Image loaded';
                }
            }
        } else {
            resetTestCategoryImageUpload();
        }
        
        // Store category ID for update
        document.getElementById('testCategoryForm').dataset.categoryId = id;
        document.getElementById('testCategoryForm').dataset.editMode = 'true';
        
        const title = document.querySelector('#testCategoryModal h2');
        if (title) title.textContent = 'Edit Test Category';
        
        openModal('testCategoryModal');
    }
}

async function deleteTestCategory(id) {
    console.log('🗑️ deleteTestCategory called with id:', id);
    
    const cat = testCategories.find(c => c.id === id);
    const catName = cat ? cat.name : 'this category';
    
    if (!confirm(`Delete "${catName}"?\n\nThis will delete all related sessions and questions.`)) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/test_categories/crud.php`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ id: id })
        });
        
        const data = await response.json();
        console.log('Delete response:', data);
        
        if (data.success) {
            loadTestCategories();
            populateSessionDropdowns();
            showNotification('Test category deleted successfully!', 'success');
        } else {
            showNotification(data.message || 'Failed to delete test category', 'error');
        }
    } catch (error) {
        console.error('Error deleting test category:', error);
        showNotification('Error: ' + error.message, 'error');
    }
}

// Toggle Test Category ON/OFF status
async function toggleTestCategoryStatus(id, newStatus) {
    const cat = testCategories.find(c => c.id === id);
    if (!cat) {
        showNotification('Category not found', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/test_categories/crud.php`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                id: id,
                name: cat.name,
                exam_category_id: cat.exam_category_id,
                description: cat.description || '',
                icon: cat.icon || '',
                color: cat.color || '',
                image: cat.image || '',
                image_path: cat.image_path || '',
                is_active: newStatus,
                display_order: cat.display_order || 0
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            loadTestCategories();
            showNotification(`Category "${cat.name}" is now ${newStatus ? 'ON (visible)' : 'OFF (hidden)'}`, 'success');
        } else {
            showNotification(data.message || 'Failed to update status', 'error');
        }
    } catch (error) {
        console.error('Error toggling test category status:', error);
        showNotification('Error updating status', 'error');
    }
}

// ============================================
// TEST CATEGORY IMAGE UPLOAD (ADD/EDIT)
// ============================================

let testCategoryUploadedImageData = null;

function handleTestCategoryImageSelect(event) {
    const file = event?.target?.files?.[0];
    if (!file) return;
    
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml'];
    if (!allowedTypes.includes(file.type)) {
        showNotification('Invalid file type. Please upload JPG, PNG, GIF, WEBP, or SVG', 'error');
        return;
    }
    
    const maxSize = 5 * 1024 * 1024; // 5MB
    if (file.size > maxSize) {
        showNotification('File size exceeds 5MB limit', 'error');
        return;
    }
    
    // Show preview immediately
    const reader = new FileReader();
    reader.onload = function(e) {
        showTestCategoryImagePreview(e.target.result, file);
    };
    reader.readAsDataURL(file);
    
    // Upload to server
    uploadTestCategoryImage(file);
}

function showTestCategoryImagePreview(src, file) {
    const preview = document.getElementById('testCategoryImagePreview');
    const previewContainer = document.getElementById('testCategoryImagePreviewContainer');
    const uploadArea = document.getElementById('testCategoryImageUploadArea');
    const info = document.getElementById('testCategoryImageInfo');
    
    if (!preview || !previewContainer || !uploadArea) return;
    
    preview.src = src;
    previewContainer.style.display = 'block';
    uploadArea.style.display = 'none';
    
    if (info) {
        const sizeKb = (file.size / 1024).toFixed(2);
        info.innerHTML = `<i class="fas fa-file-image" style="color: #6C63FF;"></i> ${file.name} (${sizeKb} KB)`;
    }
}

async function uploadTestCategoryImage(file) {
    const progressWrap = document.getElementById('testCategoryImageUploadProgress');
    const progressBar = document.getElementById('testCategoryImageUploadProgressBar');
    const progressText = document.getElementById('testCategoryImageUploadProgressText');
    
    try {
        if (progressWrap) progressWrap.style.display = 'block';
        if (progressBar) progressBar.style.width = '0%';
        if (progressText) progressText.textContent = 'Uploading...';
        
        const formData = new FormData();
        formData.append('image', file);
        
        // Simulated progress for UX
        let p = 0;
        const iv = setInterval(() => {
            p += 10;
            if (p <= 90 && progressBar) progressBar.style.width = `${p}%`;
        }, 120);
        
        const response = await fetch(`${API_BASE_URL}/admin/test_categories/upload_image.php`, {
            method: 'POST',
            body: formData
        });
        
        clearInterval(iv);
        if (progressBar) progressBar.style.width = '100%';
        
        const data = await response.json();
        if (!data.success) {
            throw new Error(data.message || 'Upload failed');
        }
        
        testCategoryUploadedImageData = data.data || null;
        
        // Store in hidden fields
        const fileField = document.getElementById('testCategoryImage');
        const pathField = document.getElementById('testCategoryImagePath');
        if (fileField) fileField.value = data.data?.filename || '';
        if (pathField) pathField.value = data.data?.path || '';
        
        if (progressText) {
            progressText.innerHTML = '<i class="fas fa-check-circle" style="color: #4CAF50;"></i> Upload complete!';
        }
        
        setTimeout(() => {
            if (progressWrap) progressWrap.style.display = 'none';
        }, 1500);
        
        showNotification('Image uploaded successfully!', 'success');
    } catch (error) {
        console.error('Error uploading test category image:', error);
        if (progressWrap) progressWrap.style.display = 'none';
        showNotification('Failed to upload image: ' + (error?.message || 'Unknown error'), 'error');
        resetTestCategoryImageUpload();
    }
}

function removeTestCategoryImage() {
    // Only clears the form fields (does not delete server file)
    const fileField = document.getElementById('testCategoryImage');
    const pathField = document.getElementById('testCategoryImagePath');
    const fileInput = document.getElementById('testCategoryImageInput');
    
    if (fileField) fileField.value = '';
    if (pathField) pathField.value = '';
    if (fileInput) fileInput.value = '';
    
    resetTestCategoryImageUpload();
    testCategoryUploadedImageData = null;
}

function resetTestCategoryImageUpload() {
    const previewContainer = document.getElementById('testCategoryImagePreviewContainer');
    const uploadArea = document.getElementById('testCategoryImageUploadArea');
    const progressWrap = document.getElementById('testCategoryImageUploadProgress');
    const progressBar = document.getElementById('testCategoryImageUploadProgressBar');
    
    if (previewContainer) previewContainer.style.display = 'none';
    if (uploadArea) uploadArea.style.display = 'block';
    if (progressWrap) progressWrap.style.display = 'none';
    if (progressBar) progressBar.style.width = '0%';
    
    const fileField = document.getElementById('testCategoryImage');
    const pathField = document.getElementById('testCategoryImagePath');
    if (fileField) fileField.value = '';
    if (pathField) pathField.value = '';
    
    testCategoryUploadedImageData = null;
}

function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
}

document.addEventListener('DOMContentLoaded', function() {
    const uploadArea = document.getElementById('testCategoryImageUploadArea');
    if (!uploadArea) return;
    
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        uploadArea.addEventListener(eventName, preventDefaults, false);
        document.body.addEventListener(eventName, preventDefaults, false);
    });
    
    ['dragenter', 'dragover'].forEach(eventName => {
        uploadArea.addEventListener(eventName, function() {
            this.style.borderColor = '#6C63FF';
            this.style.background = '#f0efff';
        }, false);
    });
    
    ['dragleave', 'drop'].forEach(eventName => {
        uploadArea.addEventListener(eventName, function() {
            this.style.borderColor = '#e0e0e0';
            this.style.background = '#f9f9f9';
        }, false);
    });
    
    uploadArea.addEventListener('drop', function(e) {
        const dt = e.dataTransfer;
        const files = dt?.files;
        if (files && files.length > 0) {
            handleTestCategoryImageSelect({ target: { files: [files[0]] } });
        }
    }, false);
});

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

function filterTestCategories(preSelectValue = null) {
    const selectedExam = document.getElementById('sessionExamCategory').value;
    const testDropdown = document.getElementById('sessionTestCategory');
    
    if (!selectedExam) {
        testDropdown.innerHTML = '<option value="">-- Select Test Category --</option>';
        return;
    }
    
    const filteredCategories = testCategories.filter(cat => String(cat.exam_category_id) === String(selectedExam));
    testDropdown.innerHTML = '<option value="">-- Select Test Category --</option>' + 
        filteredCategories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('');
    
    // ✅ If preSelectValue provided, set it after rebuilding options
    if (preSelectValue) {
        // Use double requestAnimationFrame to ensure DOM is fully updated
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                testDropdown.value = String(preSelectValue);
                console.log('✅ Pre-selected test category:', testDropdown.value, '(Text:', testDropdown.options[testDropdown.selectedIndex]?.text + ')');
            });
        });
    }
}

// Question Sessions CRUD
async function loadQuestionSessions() {
    try {
        // Fetch all required data
        const [examResponse, testResponse, sessionResponse] = await Promise.all([
            fetch(`${API_BASE_URL}/admin/exam_categories/crud.php`),
            fetch(`${API_BASE_URL}/admin/test_categories/crud.php`),
            fetch(`${API_BASE_URL}/admin/sessions/crud.php`)
        ]);
        
        const examData = await examResponse.json();
        const testData = await testResponse.json();
        const sessionData = await sessionResponse.json();
        
        if (sessionData.success) {
            questionSessions = sessionData.sessions;
            const examCategories = examData.success ? examData.categories : [];
            const testCategories = testData.success ? testData.categories : [];
            
            // Build hierarchy: Exam > Test Category > Sessions
            const hierarchy = {};
            
            // Initialize exam categories
            examCategories.forEach(exam => {
                hierarchy[exam.id] = {
                    examId: exam.id,
                    examName: exam.name,
                    examIcon: exam.icon || 'fas fa-book',
                    examColor: exam.color || '#6C63FF',
                    testCategories: {}
                };
            });
            
            // Add uncategorized exam
            hierarchy['uncategorized'] = {
                examId: null,
                examName: 'Uncategorized',
                examIcon: 'fas fa-folder',
                examColor: '#7F8C8D',
                testCategories: {}
            };
            
            // Map test categories to exams
            testCategories.forEach(testCat => {
                const examId = testCat.exam_category_id || 'uncategorized';
                if (hierarchy[examId]) {
                    hierarchy[examId].testCategories[testCat.id] = {
                        testId: testCat.id,
                        testName: testCat.name,
                        testIcon: testCat.icon || 'fas fa-tags',
                        testColor: testCat.color || '#4ECDC4',
                        sessions: []
                    };
                }
            });
            
            // Add uncategorized test category to each exam
            Object.keys(hierarchy).forEach(examId => {
                hierarchy[examId].testCategories['uncategorized'] = {
                    testId: null,
                    testName: 'Uncategorized',
                    testIcon: 'fas fa-folder-open',
                    testColor: '#95a5a6',
                    sessions: []
                };
            });
            
            // Map sessions to test categories
            questionSessions.forEach(session => {
                const examId = session.exam_category_id || 'uncategorized';
                const testId = session.test_category_id || 'uncategorized';
                
                if (hierarchy[examId] && hierarchy[examId].testCategories[testId]) {
                    hierarchy[examId].testCategories[testId].sessions.push(session);
                } else if (hierarchy[examId]) {
                    hierarchy[examId].testCategories['uncategorized'].sessions.push(session);
                } else {
                    hierarchy['uncategorized'].testCategories['uncategorized'].sessions.push(session);
                }
            });
            
            // Render the hierarchical view
            const container = document.getElementById('questionSessionsByExamGrid');
            if (container) {
                let html = '';
                
                Object.values(hierarchy).forEach(exam => {
                    // Check if this exam has any sessions
                    let totalSessions = 0;
                    Object.values(exam.testCategories).forEach(tc => {
                        totalSessions += tc.sessions.length;
                    });
                    
                    // Skip exams with no sessions (except show empty state)
                    if (totalSessions === 0 && exam.examId !== null) return;
                    
                    html += `
                    <div class="exam-session-card" style="background: white; border-radius: 16px; margin-bottom: 24px; box-shadow: 0 4px 15px rgba(0,0,0,0.08); overflow: hidden;">
                        <div class="exam-card-header" style="background: linear-gradient(135deg, ${exam.examColor}, ${adjustColor(exam.examColor, -20)}); padding: 20px 24px; color: white; display: flex; align-items: center; justify-content: space-between;">
                            <div style="display: flex; align-items: center; gap: 15px;">
                                <div style="width: 50px; height: 50px; background: rgba(255,255,255,0.2); border-radius: 12px; display: flex; align-items: center; justify-content: center;">
                                    <i class="${exam.examIcon}" style="font-size: 24px;"></i>
                                </div>
                                <div>
                                    <h3 style="margin: 0; font-size: 20px; font-weight: 600;">${exam.examName}</h3>
                                    <p style="margin: 4px 0 0 0; opacity: 0.9; font-size: 14px;">${totalSessions} Question Sessions</p>
                                </div>
                            </div>
                        </div>
                        <div class="exam-card-body" style="padding: 20px;">
                    `;
                    
                    // Loop through test categories
                    Object.values(exam.testCategories).forEach(testCat => {
                        if (testCat.sessions.length === 0) return;
                        
                        html += `
                        <div class="test-category-section" style="margin-bottom: 20px; background: #f8f9fa; border-radius: 12px; overflow: hidden;">
                            <div style="background: linear-gradient(135deg, ${testCat.testColor}, ${adjustColor(testCat.testColor, -15)}); padding: 14px 20px; display: flex; align-items: center; justify-content: space-between;">
                                <div style="display: flex; align-items: center; gap: 12px; color: white;">
                                    <i class="${testCat.testIcon}" style="font-size: 18px;"></i>
                                    <span style="font-weight: 600;">${testCat.testName}</span>
                                    <span style="background: rgba(255,255,255,0.3); padding: 2px 10px; border-radius: 10px; font-size: 12px;">${testCat.sessions.length} sessions</span>
                                </div>
                                <button class="btn" onclick="openSessionModalForTestCategory(${exam.examId}, ${testCat.testId})" style="background: rgba(255,255,255,0.2); color: white; border: none; padding: 6px 12px; font-size: 12px;">
                                    <i class="fas fa-plus"></i> Add
                                </button>
                            </div>
                            <div style="padding: 15px;">
                                <div class="sessions-list" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 12px;">
                                    ${testCat.sessions.map(session => {
                                        const progress = session.total_questions > 0 
                                            ? Math.round((session.actual_question_count || 0) / session.total_questions * 100) 
                                            : 0;
                                        const progressColor = progress >= 100 ? '#4CAF50' : progress >= 50 ? '#FF9800' : '#FF6B6B';
                                        
                                        return `
                                        <div class="session-item" style="background: white; border-radius: 10px; padding: 15px; border: 1px solid #e0e0e0; transition: all 0.3s; ${!session.is_active ? 'opacity: 0.6;' : ''}">
                                            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px;">
                                                <div>
                                                    <h4 style="margin: 0; font-size: 14px; font-weight: 600; color: #2C3E50;">${session.name}</h4>
                                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #7F8C8D;">
                                                        <i class="fas fa-clock"></i> ${session.duration || 180} mins
                                                    </p>
                                                </div>
                                                <span class="badge ${session.is_active ? 'badge-success' : 'badge-danger'}" style="font-size: 10px;">${session.is_active ? 'Active' : 'Off'}</span>
                                            </div>
                                            <div style="margin-bottom: 10px;">
                                                <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px;">
                                                    <span style="color: #666;">Questions</span>
                                                    <span style="font-weight: 600; color: ${progressColor};">${session.actual_question_count || 0}/${session.total_questions || 0}</span>
                                                </div>
                                                <div style="background: #e9ecef; border-radius: 10px; height: 6px; overflow: hidden;">
                                                    <div style="background: ${progressColor}; height: 100%; width: ${Math.min(progress, 100)}%; transition: width 0.3s;"></div>
                                                </div>
                                            </div>
                                            <div style="display: flex; gap: 6px; justify-content: flex-end;">
                                                <button class="btn-icon btn-primary" onclick="openUploadForSession(${session.id})" title="Upload Questions" style="background: rgba(108,99,255,0.1); color: #6C63FF;">
                                                    <i class="fas fa-upload"></i>
                                                </button>
                                                <button class="btn-icon btn-view" onclick="viewSessionQuestions(${session.id})" title="View Questions">
                                                    <i class="fas fa-eye"></i>
                                                </button>
                                                <button class="btn-icon btn-edit" onclick="editQuestionSession(${session.id})" title="Edit">
                                                    <i class="fas fa-edit"></i>
                                                </button>
                                                <button class="btn-icon btn-delete" onclick="deleteQuestionSession(${session.id})" title="Delete">
                                                    <i class="fas fa-trash"></i>
                                                </button>
                                            </div>
                                        </div>
                                        `;
                                    }).join('')}
                                </div>
                            </div>
                        </div>
                        `;
                    });
                    
                    // If exam has no sessions at all
                    if (totalSessions === 0) {
                        html += `
                            <div style="text-align: center; padding: 40px; color: #999;">
                                <i class="fas fa-clipboard-list" style="font-size: 48px; margin-bottom: 15px; opacity: 0.5;"></i>
                                <p style="margin: 0 0 15px 0;">No question sessions yet</p>
                                <button class="btn btn-primary" onclick="openModal('questionSessionModal')">
                                    <i class="fas fa-plus"></i> Add First Session
                                </button>
                            </div>
                        `;
                    }
                    
                    html += `
                        </div>
                    </div>
                    `;
                });
                
                // If no sessions at all
                if (html === '') {
                    html = `
                        <div style="text-align: center; padding: 60px; background: white; border-radius: 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.08);">
                            <i class="fas fa-clipboard-list" style="font-size: 64px; color: #ddd; margin-bottom: 20px;"></i>
                            <h3 style="color: #666; margin-bottom: 10px;">No Question Sessions Yet</h3>
                            <p style="color: #999; margin-bottom: 20px;">Create test categories first, then add question sessions.</p>
                            <button class="btn btn-primary" onclick="showPage('testCategories')">
                                <i class="fas fa-plus"></i> Create Test Category
                            </button>
                        </div>
                    `;
                }
                
                container.innerHTML = html;
            }
            
            populateSessionDropdowns();
            loadSessionCards();
        } else {
            console.error('Failed to load question sessions:', sessionData.message);
        }
    } catch (error) {
        console.error('Error loading question sessions:', error);
        showNotification('Failed to load question sessions', 'error');
    }
}

// Open session modal pre-filled with exam and test category
function openSessionModalForTestCategory(examId, testCategoryId) {
    openModal('questionSessionModal');
    setTimeout(() => {
        const testCatSelect = document.getElementById('sessionTestCategory');
        if (testCatSelect && testCategoryId) {
            testCatSelect.value = testCategoryId;
        }
    }, 100);
}

async function saveQuestionSession() {
    const form = document.getElementById('questionSessionForm');
    const isEditMode = form.dataset.editMode === 'true';
    const sessionId = form.dataset.sessionId;
    
    // Get form values
    const name = document.getElementById('sessionName').value.trim();
    const testCategoryId = parseInt(document.getElementById('sessionTestCategory').value) || 0;
    const duration = parseInt(document.getElementById('sessionTime').value) || 180;
    const totalQuestions = parseInt(document.getElementById('sessionTotalQuestions').value) || 0;
    
    console.log('📝 Form values:', { name, testCategoryId, duration, totalQuestions, isEditMode, sessionId });
    
    // Basic Validation only
    if (!name) {
        showNotification('⚠️ Session name is required!', 'error');
        return;
    }
    
    if (!testCategoryId || testCategoryId === 0) {
        showNotification('⚠️ Please select a test category!', 'error');
        return;
    }
    
    // Build session data
    const sessionData = {
        name: name,
        test_category_id: testCategoryId,
        duration: duration > 0 ? duration : 180,
        total_questions: totalQuestions >= 0 ? totalQuestions : 0,
        description: '',
        difficulty: 'medium'
    };
    
    if (isEditMode && sessionId) {
        sessionData.id = parseInt(sessionId);
    }
    
    console.log('💾 Sending to API:', sessionData);
    
    try {
        console.log('📤 Sending data:', sessionData);
        
        const response = await fetch(`${API_BASE_URL}/admin/sessions/crud.php`, {
            method: isEditMode ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(sessionData)
        });
        
        const data = await response.json();
        console.log('📥 API Response:', data);
        
        if (!response.ok) {
            console.error('❌ HTTP Error:', response.status, response.statusText);
            console.error('❌ Error message:', data.message);
            showNotification('❌ ' + (data.message || `Server error (${response.status})`), 'error');
            return;
        }
        
        if (data.success) {
            await loadQuestionSessions();
            await loadSessionCards();
            closeModal('questionSessionModal');
            showNotification(
                isEditMode 
                    ? '✅ Session updated successfully!' 
                    : '✅ Session created successfully!', 
                'success'
            );
            form.reset();
            delete form.dataset.sessionId;
            delete form.dataset.editMode;
        } else {
            showNotification('❌ ' + (data.message || 'Failed to save session'), 'error');
        }
    } catch (error) {
        console.error('❌ Error:', error);
        showNotification('❌ Error: ' + error.message, 'error');
    }
}

// Removed - using viewSessionQuestions instead

async function editQuestionSession(id) {
    console.log('📝 EDIT SESSION CALLED - ID:', id);
    
    try {
        // Find session
        const session = questionSessions.find(s => s.id == id);
        if (!session) {
            alert('Session not found! ID: ' + id);
            console.error('Session not found. Available:', questionSessions.map(s => s.id));
            return;
        }
        
        console.log('📋 Session data:', session);
        console.log('📋 exam_category_id:', session.exam_category_id);
        console.log('📋 test_category_id:', session.test_category_id);
        
        // ALWAYS reload categories fresh
        console.log('⏳ Loading categories...');
        await loadExamCategories();
        await loadTestCategories();
        console.log('✅ Loaded - Exams:', examCategories.length, 'Tests:', testCategories.length);
        
        // Get form elements
        const form = document.getElementById('questionSessionForm');
        const examDropdown = document.getElementById('sessionExamCategory');
        const testDropdown = document.getElementById('sessionTestCategory');
        
        if (!form || !examDropdown || !testDropdown) {
            alert('Form elements not found!');
            return;
        }
        
        // Set text fields FIRST
        document.getElementById('sessionName').value = session.name || '';
        document.getElementById('sessionTime').value = session.duration || 180;
        document.getElementById('sessionTotalQuestions').value = session.total_questions || 0;
        
        // Get IDs as numbers
        const examId = parseInt(session.exam_category_id) || 0;
        const testId = parseInt(session.test_category_id) || 0;
        
        console.log('🎯 Target IDs - Exam:', examId, 'Test:', testId);
        
        // ========== EXAM DROPDOWN ==========
        examDropdown.innerHTML = '<option value="">-- Select Exam Category --</option>';
        let examFound = false;
        
        examCategories.forEach(cat => {
            const opt = document.createElement('option');
            opt.value = cat.id;
            opt.textContent = cat.name;
            
            if (parseInt(cat.id) === examId) {
                opt.selected = true;
                examFound = true;
                console.log('✅ EXAM MATCH:', cat.id, '=', examId, cat.name);
            }
            examDropdown.appendChild(opt);
        });
        
        if (!examFound) {
            console.warn('⚠️ Exam category NOT found! Looking for:', examId);
            console.log('Available exams:', examCategories.map(c => ({id: c.id, name: c.name})));
        }
        
        // ========== TEST DROPDOWN ==========
        // Filter tests by exam category
        const filteredTests = testCategories.filter(cat => parseInt(cat.exam_category_id) === examId);
        console.log('📋 Filtered tests for exam', examId, ':', filteredTests.length);
        
        testDropdown.innerHTML = '<option value="">-- Select Test Category --</option>';
        let testFound = false;
        
        filteredTests.forEach(cat => {
            const opt = document.createElement('option');
            opt.value = cat.id;
            opt.textContent = cat.name;
            
            if (parseInt(cat.id) === testId) {
                opt.selected = true;
                testFound = true;
                console.log('✅ TEST MATCH:', cat.id, '=', testId, cat.name);
            }
            testDropdown.appendChild(opt);
        });
        
        if (!testFound) {
            console.warn('⚠️ Test category NOT found! Looking for:', testId);
            console.log('Available tests:', filteredTests.map(c => ({id: c.id, name: c.name})));
        }
        
        // Set form metadata
        form.dataset.sessionId = id;
        form.dataset.editMode = 'true';
        
        // Final check
        console.log('📊 FINAL VALUES:');
        console.log('   Exam dropdown:', examDropdown.value, '(selected:', examDropdown.selectedIndex, ')');
        console.log('   Test dropdown:', testDropdown.value, '(selected:', testDropdown.selectedIndex, ')');
        
        // Open modal
        openModal('questionSessionModal');
        
    } catch (error) {
        console.error('❌ ERROR:', error);
        alert('Error: ' + error.message);
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

// Load Session Cards for Add Question Page - Hierarchical View
async function loadSessionCards() {
    const container = document.getElementById('addQuestionsByExamGrid');
    if (!container) return;
    
    if (questionSessions.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; padding: 60px; background: white; border-radius: 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.08);">
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
    
    // Fetch exam and test categories
    let examCategories = [];
    let testCategoriesData = [];
    
    try {
        const [examResponse, testResponse] = await Promise.all([
            fetch(`${API_BASE_URL}/admin/exam_categories/crud.php`),
            fetch(`${API_BASE_URL}/admin/test_categories/crud.php`)
        ]);
        const examData = await examResponse.json();
        const testData = await testResponse.json();
        examCategories = examData.success ? examData.categories : [];
        testCategoriesData = testData.success ? testData.categories : [];
    } catch (error) {
        console.error('Error fetching categories:', error);
    }
    
    // Build hierarchy
    const hierarchy = {};
    
    examCategories.forEach(exam => {
        hierarchy[exam.id] = {
            examId: exam.id,
            examName: exam.name,
            examIcon: exam.icon || 'fas fa-book',
            examColor: exam.color || '#6C63FF',
            testCategories: {}
        };
    });
    
    hierarchy['uncategorized'] = {
        examId: null,
        examName: 'Uncategorized',
        examIcon: 'fas fa-folder',
        examColor: '#7F8C8D',
        testCategories: {}
    };
    
    testCategoriesData.forEach(testCat => {
        const examId = testCat.exam_category_id || 'uncategorized';
        if (hierarchy[examId]) {
            hierarchy[examId].testCategories[testCat.id] = {
                testId: testCat.id,
                testName: testCat.name,
                testIcon: testCat.icon || 'fas fa-tags',
                testColor: testCat.color || '#4ECDC4',
                sessions: []
            };
        }
    });
    
    Object.keys(hierarchy).forEach(examId => {
        hierarchy[examId].testCategories['uncategorized'] = {
            testId: null,
            testName: 'Uncategorized',
            testIcon: 'fas fa-folder-open',
            testColor: '#95a5a6',
            sessions: []
        };
    });
    
    questionSessions.forEach(session => {
        const examId = session.exam_category_id || 'uncategorized';
        const testId = session.test_category_id || 'uncategorized';
        
        if (hierarchy[examId] && hierarchy[examId].testCategories[testId]) {
            hierarchy[examId].testCategories[testId].sessions.push(session);
        } else if (hierarchy[examId]) {
            hierarchy[examId].testCategories['uncategorized'].sessions.push(session);
        } else {
            hierarchy['uncategorized'].testCategories['uncategorized'].sessions.push(session);
        }
    });
    
    // Render hierarchical view
    let html = '';
    
    Object.values(hierarchy).forEach(exam => {
        let totalSessions = 0;
        Object.values(exam.testCategories).forEach(tc => {
            totalSessions += tc.sessions.length;
        });
        
        if (totalSessions === 0) return;
        
        html += `
        <div class="add-question-exam-card" style="background: white; border-radius: 16px; margin-bottom: 24px; box-shadow: 0 4px 15px rgba(0,0,0,0.08); overflow: hidden;">
            <div style="background: linear-gradient(135deg, ${exam.examColor}, ${adjustColor(exam.examColor, -20)}); padding: 20px 24px; color: white; display: flex; align-items: center; gap: 15px;">
                <div style="width: 50px; height: 50px; background: rgba(255,255,255,0.2); border-radius: 12px; display: flex; align-items: center; justify-content: center;">
                    <i class="${exam.examIcon}" style="font-size: 24px;"></i>
                </div>
                <div>
                    <h3 style="margin: 0; font-size: 20px; font-weight: 600;">${exam.examName}</h3>
                    <p style="margin: 4px 0 0 0; opacity: 0.9; font-size: 14px;">${totalSessions} Sessions available for upload</p>
                </div>
            </div>
            <div style="padding: 20px;">
        `;
        
        Object.values(exam.testCategories).forEach(testCat => {
            if (testCat.sessions.length === 0) return;
            
            html += `
            <div class="add-question-test-section" style="margin-bottom: 16px; background: #f8f9fa; border-radius: 12px; overflow: hidden;">
                <div style="background: linear-gradient(135deg, ${testCat.testColor}, ${adjustColor(testCat.testColor, -15)}); padding: 12px 18px; display: flex; align-items: center; gap: 10px; color: white;">
                    <i class="${testCat.testIcon}" style="font-size: 16px;"></i>
                    <span style="font-weight: 600;">${testCat.testName}</span>
                    <span style="background: rgba(255,255,255,0.3); padding: 2px 10px; border-radius: 10px; font-size: 11px; margin-left: auto;">${testCat.sessions.length} sessions</span>
                </div>
                <div style="padding: 15px; display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px;">
            `;
            
            testCat.sessions.forEach(session => {
                const progress = session.total_questions > 0 
                    ? Math.round((session.actual_question_count || 0) / session.total_questions * 100) 
                    : 0;
                const progressColor = progress >= 100 ? '#4CAF50' : progress >= 50 ? '#FF9800' : '#FF6B6B';
                const isEmpty = (session.actual_question_count || 0) === 0;
                
                html += `
                <div class="upload-session-card" onclick="openUploadForSession(${session.id})" style="background: white; border-radius: 10px; padding: 15px; border: 2px solid ${isEmpty ? '#FF6B6B' : '#e0e0e0'}; cursor: pointer; transition: all 0.3s; ${!session.is_active ? 'opacity: 0.6;' : ''}">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
                        <div>
                            <h4 style="margin: 0; font-size: 14px; font-weight: 600; color: #2C3E50;">${session.name}</h4>
                            <p style="margin: 4px 0 0 0; font-size: 12px; color: #7F8C8D;">
                                <i class="fas fa-clock"></i> ${session.duration || 180} mins
                            </p>
                        </div>
                        ${isEmpty ? '<span style="background: #FF6B6B; color: white; padding: 3px 8px; border-radius: 4px; font-size: 10px; font-weight: 600;">EMPTY</span>' : 
                          '<span class="badge badge-success" style="font-size: 10px;">Ready</span>'}
                    </div>
                    <div style="margin-bottom: 12px;">
                        <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px;">
                            <span style="color: #666;">Questions Added</span>
                            <span style="font-weight: 600; color: ${progressColor};">${session.actual_question_count || 0}/${session.total_questions || 0}</span>
                        </div>
                        <div style="background: #e9ecef; border-radius: 10px; height: 8px; overflow: hidden;">
                            <div style="background: ${progressColor}; height: 100%; width: ${Math.min(progress, 100)}%; transition: width 0.3s;"></div>
                        </div>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <button class="btn" onclick="event.stopPropagation(); openUploadForSession(${session.id})" style="flex: 1; background: linear-gradient(135deg, #667eea, #764ba2); color: white; border: none; padding: 8px; font-size: 12px;">
                            <i class="fas fa-cloud-upload-alt"></i> Upload
                        </button>
                        <button class="btn" onclick="event.stopPropagation(); viewSessionQuestions(${session.id})" style="background: #4ECDC4; color: white; border: none; padding: 8px 12px; font-size: 12px;">
                            <i class="fas fa-eye"></i>
                        </button>
                    </div>
                </div>
                `;
            });
            
            html += `
                </div>
            </div>
            `;
        });
        
        html += `
            </div>
        </div>
        `;
    });
    
    if (html === '') {
        html = `
            <div style="text-align: center; padding: 60px; background: white; border-radius: 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.08);">
                <i class="fas fa-inbox" style="font-size: 64px; color: #ddd; margin-bottom: 20px;"></i>
                <h3 style="color: #999;">No Question Sessions Available</h3>
                <p style="color: #bbb;">Please create a question session first to add questions.</p>
                <button class="btn btn-primary" onclick="showPage('questionSessions')" style="margin-top: 20px;">
                    <i class="fas fa-plus"></i> Create Question Session
                </button>
            </div>
        `;
    }
    
    container.innerHTML = html;
    
    // Add hover effects
    document.querySelectorAll('.upload-session-card').forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-4px)';
            this.style.boxShadow = '0 8px 20px rgba(0,0,0,0.15)';
            this.style.borderColor = '#6C63FF';
        });
        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
            this.style.boxShadow = 'none';
            this.style.borderColor = this.querySelector('[style*="EMPTY"]') ? '#FF6B6B' : '#e0e0e0';
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
                    This session has <strong>no questions yet</strong>. Upload your JSON file to add questions.
                </p>
            </div>
        `;
    }
    
    // Reset file input when modal opens
    const fileInput = document.getElementById('jsonFile');
    if (fileInput) {
        fileInput.value = '';
        const fileNameDisplay = document.getElementById('jsonFileName');
        if (fileNameDisplay) {
            fileNameDisplay.textContent = 'No file selected';
            fileNameDisplay.style.color = '#666';
            fileNameDisplay.style.fontWeight = 'normal';
        }
    }
    
    openModal('jsonUploadModal');
}

// Global variables for questions view
let allSessionQuestions = [];
let filteredQuestions = [];
let currentViewSessionId = null;
let currentPage = 1;
let questionsPerPage = 10;
let currentQuestionIndex = 0; // For single question view
const TOTAL_QUESTIONS = 200; // Total question slots

async function viewSessionQuestions(sessionId) {
    const session = questionSessions.find(s => s.id === sessionId);
    if (!session) return;
    
    currentViewSessionId = sessionId;
    currentQuestionIndex = 0;
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php?session_id=${sessionId}`);
        const data = await response.json();
        
        allSessionQuestions = [];
        if (data.success) {
            allSessionQuestions = data.questions;
        }
        filteredQuestions = [...allSessionQuestions];
        
        // Update header info
        document.getElementById('viewSessionName').textContent = session.name;
        document.getElementById('totalQuestionsCount').textContent = allSessionQuestions.length;
        
        // Count empty slots
        const emptyCount = TOTAL_QUESTIONS - allSessionQuestions.length;
        const emptyEl = document.getElementById('emptyQuestionsCount');
        if (emptyEl) emptyEl.textContent = emptyCount;
        
        // Count bilingual questions
        const bilingualCount = allSessionQuestions.filter(q => 
            (q.question_en && q.question_en.trim()) && (q.question_ta && q.question_ta.trim())
        ).length;
        const bilingualEl = document.getElementById('bilingualCount');
        if (bilingualEl) bilingualEl.textContent = bilingualCount;
        
        renderQuestionPills();
        renderSingleQuestion();
        openModal('viewQuestionsModal');
    } catch (error) {
        console.error('Error loading session questions:', error);
        showNotification('Failed to load session questions', 'error');
    }
}

// Refresh questions
async function refreshViewQuestions() {
    if (currentViewSessionId) {
        await viewSessionQuestions(currentViewSessionId);
        showNotification('Questions refreshed!', 'success');
    }
}

function renderQuestionsTable() {
    renderSingleQuestion();
}

// Render single question - App-like READ-ONLY view first
function renderSingleQuestion() {
    const container = document.getElementById('questionsCardsContainer');
    const displayOrder = currentQuestionIndex + 1;
    
    // Update navigation info
    document.getElementById('currentPageInfo').textContent = `Question ${displayOrder} of ${TOTAL_QUESTIONS}`;
    
    // Update navigation buttons
    const prevBtn = document.getElementById('prevQBtn');
    const nextBtn = document.getElementById('nextQBtn');
    if (prevBtn) prevBtn.disabled = currentQuestionIndex === 0;
    if (nextBtn) nextBtn.disabled = currentQuestionIndex >= TOTAL_QUESTIONS - 1;
    
    // Find question by display_order
    const q = allSessionQuestions.find(q => q.display_order === displayOrder) || null;
    
    // Update pills to highlight current
    updateQuestionPillsHighlight();
    
    if (!q) {
        // Empty question slot - show create form
        container.innerHTML = `
            <div style="max-width: 800px; margin: 0 auto;">
                <div style="background: white; border-radius: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); overflow: hidden;">
                    <!-- Header -->
                    <div style="background: linear-gradient(135deg, #ff9800 0%, #f57c00 100%); padding: 20px; display: flex; align-items: center; justify-content: space-between;">
                        <div style="display: flex; align-items: center; gap: 15px;">
                            <div style="background: white; color: #ff9800; width: 50px; height: 50px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 700;">${displayOrder}</div>
                            <div>
                                <h3 style="margin: 0; color: white;">Question ${displayOrder}</h3>
                                <span style="background: rgba(255,255,255,0.2); padding: 3px 10px; border-radius: 10px; font-size: 11px; color: white;">EMPTY SLOT</span>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Empty State -->
                    <div style="padding: 60px 20px; text-align: center;">
                        <i class="fas fa-plus-circle" style="font-size: 64px; color: #ddd; margin-bottom: 20px;"></i>
                        <h3 style="color: #999; margin: 0 0 10px 0;">No Question Here</h3>
                        <p style="color: #bbb; margin: 0 0 20px 0;">This slot is empty. Create a new question.</p>
                        <button onclick="openCreateQuestionForm(${displayOrder})" style="background: linear-gradient(135deg, #4CAF50 0%, #43a047 100%); color: white; border: none; padding: 12px 30px; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer;">
                            <i class="fas fa-plus"></i> Create Question ${displayOrder}
                        </button>
                    </div>
                </div>
            </div>
        `;
        return;
    }
    
    // Show existing question - READ-ONLY VIEW (like app)
    // ═══════════════════════════════════════════════════════════════
    // NEW APPROACH: NO SPLIT - Display question as-is with both languages
    // question_en now contains combined English + Tamil content
    // ═══════════════════════════════════════════════════════════════
    
    // Get question text - combine both fields if needed
    let questionText = q.question_en || q.question_ta || '';
    
    // If both fields have content, show question_en (which has combined content)
    // and fallback to question_ta if question_en is empty
    if (!questionText && q.question_ta) {
        questionText = q.question_ta;
    }
    
    const hasContent = questionText && questionText.trim();
    const isPlaceholder = questionText?.includes('[PLACEHOLDER');
    
    // Helper to escape HTML but preserve <br> tags for line breaks
    const escapeHtml = (text) => {
        if (!text) return '';
        // First, convert <br> tags to a placeholder
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    };
    
    // Helper to render text with HTML (preserves <br>, <p>, tables, etc.)
    const renderHtml = (text) => {
        if (!text) return '-';
        // The API already preserves <br>, <table>, <img> tags via cleanText()
        // Just escape dangerous tags but keep formatting tags
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, (match) => {
                // Allow safe tags: br, p, table, tr, td, th, tbody, thead, img, div
                const tagMatch = text.match(/<(br|p|table|tr|td|th|tbody|thead|img|div)[\s>]/i);
                if (tagMatch) return '<';
                return '&lt;';
            })
            .replace(/(?<!<)(br|p|table|tr|td|th|tbody|thead|img|div)(?=[\s>])/gi, (match) => match)
            .replace(/(?<!<)\/(br|p|table|tr|td|th|tbody|thead|img|div)>/gi, (match) => match);
    };
    
    // Format question text - preserve HTML tags like <br>, <p>, <table>, <img>
    // The API preserves safe HTML tags, so we just sanitize dangerous content
    const formatQuestionText = (text) => {
        if (!text) return '';
        const str = String(text);
        
        // Remove dangerous script/iframe tags for security
        let safe = str
            .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
            .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
            .replace(/javascript:/gi, '')
            .replace(/on\w+\s*=\s*["'][^"']*["']/gi, ''); // Remove event handlers like onclick="..."
        
        // The API/admin endpoint preserves: <br>, <p>, <table>, <tr>, <td>, <th>, <tbody>, <thead>, <img>, <div>
        // These are safe to render directly with innerHTML
        return safe;
    };
    
    // Helper to render option with HTML support
    const renderOpt = (label, tamilLabel, textEn, textTa, isCorrect) => {
        const text = textEn || textTa || '-';
        const formattedText = formatQuestionText(text);
        return `
        <div style="display: flex; gap: 10px; margin-bottom: 8px;">
            <div style="flex: 1; padding: 12px; background: ${isCorrect ? 'linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%)' : '#f5f5f5'}; border-radius: 10px; border: 2px solid ${isCorrect ? '#4CAF50' : 'transparent'};">
                <div style="display: flex; align-items: flex-start; gap: 10px;">
                    <span style="background: ${isCorrect ? '#4CAF50' : '#e0e0e0'}; color: ${isCorrect ? 'white' : '#666'}; min-width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px;">${label}</span>
                    <span style="color: #333; font-size: 14px; line-height: 1.5;">${formattedText}</span>
                </div>
            </div>
        </div>
        `;
    };
    
    const renderOptTa = (label, textTa, isCorrect) => {
        const formattedText = formatQuestionText(textTa || '-');
        return `
        <div style="display: flex; gap: 10px; margin-bottom: 8px;">
            <div style="flex: 1; padding: 12px; background: ${isCorrect ? 'linear-gradient(135deg, #fff8e1 0%, #ffecb3 100%)' : '#fafafa'}; border-radius: 10px; border: 2px solid ${isCorrect ? '#FF9800' : 'transparent'};">
                <div style="display: flex; align-items: flex-start; gap: 10px;">
                    <span style="background: ${isCorrect ? '#FF9800' : '#e0e0e0'}; color: ${isCorrect ? 'white' : '#666'}; min-width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px;">${label}</span>
                    <span style="color: #333; font-size: 14px; line-height: 1.5;">${formattedText}</span>
                </div>
            </div>
        </div>
        `;
    };
    
    // Check if options have Tamil (for display)
    const hasTamilOptions = q.option_a_ta || q.option_b_ta || q.option_c_ta || q.option_d_ta;
    
    container.innerHTML = `
        <div style="max-width: 800px; margin: 0 auto;">
            <div style="background: white; border-radius: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); overflow: hidden;">
                <!-- Question Header -->
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 15px 20px; display: flex; justify-content: space-between; align-items: center;">
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <div style="background: white; color: #667eea; width: 45px; height: 45px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700;">${displayOrder}</div>
                        <div>
                            <span style="color: white; font-weight: 600;">Question ${displayOrder}</span>
                            <div style="display: flex; gap: 5px; margin-top: 3px;">
                                <span style="background: rgba(255,255,255,0.2); padding: 2px 8px; border-radius: 10px; font-size: 10px; color: white;">EN + த</span>
                            </div>
                        </div>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <button onclick="openJsonEditModal(${q.id})" title="JSON Edit" style="background: rgba(255,255,255,0.2); color: white; border: none; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; transition: background 0.2s;" onmouseover="this.style.background='rgba(255,255,255,0.4)'" onmouseout="this.style.background='rgba(255,255,255,0.2)'">
                            <i class="fas fa-code"></i>
                        </button>
                        <button onclick="openEditQuestionForm(${q.id})" title="Edit Form" style="background: rgba(255,255,255,0.2); color: white; border: none; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; transition: background 0.2s;" onmouseover="this.style.background='rgba(255,255,255,0.4)'" onmouseout="this.style.background='rgba(255,255,255,0.2)'">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button onclick="deleteSingleQuestion(${q.id})" title="Delete" style="background: rgba(255,87,87,0.8); color: white; border: none; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; transition: background 0.2s;" onmouseover="this.style.background='rgba(255,87,87,1)'" onmouseout="this.style.background='rgba(255,87,87,0.8)'">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
                
                <!-- Question Content - Single combined view (English + Tamil together) -->
                <div style="padding: 20px;">
                    ${hasContent ? `
                        <div style="margin-bottom: 20px;">
                            <div style="font-size: 11px; color: #667eea; font-weight: 600; margin-bottom: 8px; text-transform: uppercase; background: #ede7f6; display: inline-block; padding: 3px 10px; border-radius: 4px;">Question</div>
                            <div style="margin: 0 0 15px 0; font-size: 15px; color: #333; line-height: 1.8;">${formatQuestionText(questionText)}</div>
                        </div>
                    ` : '<p style="color: #999; text-align: center;">No question content</p>'}
                    
                    <!-- Options Section -->
                    <div style="margin-top: 15px;">
                        <div style="font-size: 11px; color: #666; font-weight: 600; margin-bottom: 10px; text-transform: uppercase;">Options</div>
                        ${renderOpt('A', 'அ', q.option_a_en || q.option_a_ta, q.option_a_ta, q.correct_answer === 'A')}
                        ${renderOpt('B', 'ஆ', q.option_b_en || q.option_b_ta, q.option_b_ta, q.correct_answer === 'B')}
                        ${renderOpt('C', 'இ', q.option_c_en || q.option_c_ta, q.option_c_ta, q.correct_answer === 'C')}
                        ${renderOpt('D', 'ஈ', q.option_d_en || q.option_d_ta, q.option_d_ta, q.correct_answer === 'D')}
                    </div>
                    
                    ${hasTamilOptions ? `
                        <hr style="border: none; border-top: 2px dashed #e0e0e0; margin: 20px 0;">
                        <div>
                            <div style="font-size: 11px; color: #e65100; font-weight: 600; margin-bottom: 10px; text-transform: uppercase; background: #fff3e0; display: inline-block; padding: 3px 10px; border-radius: 4px;">தமிழ் Options</div>
                            ${renderOptTa('அ', q.option_a_ta, q.correct_answer === 'A')}
                            ${renderOptTa('ஆ', q.option_b_ta, q.correct_answer === 'B')}
                            ${renderOptTa('இ', q.option_c_ta, q.correct_answer === 'C')}
                            ${renderOptTa('ஈ', q.option_d_ta, q.correct_answer === 'D')}
                        </div>
                    ` : ''}
                </div>
                
                <!-- Answer Footer -->
                <div style="background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%); padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #c8e6c9;">
                    <span style="color: #2e7d32; font-weight: 600;"><i class="fas fa-check-circle"></i> Correct Answer</span>
                    <span style="background: #4CAF50; color: white; padding: 8px 20px; border-radius: 20px; font-weight: bold; font-size: 14px;">Option ${q.correct_answer || 'A'}</span>
                </div>
            </div>
                    </div>
                `;
            }
                    
// Open EDIT form for question
function openEditQuestionForm(questionId) {
    const q = allSessionQuestions.find(q => q.id === questionId);
    if (!q) return;
    
    const displayOrder = q.display_order;
    const container = document.getElementById('questionsCardsContainer');
    
    container.innerHTML = `
        <div style="max-width: 800px; margin: 0 auto;">
            <div style="background: white; border-radius: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); overflow: hidden;">
                <!-- Edit Header -->
                <div style="background: linear-gradient(135deg, #4CAF50 0%, #43a047 100%); padding: 15px 20px; display: flex; justify-content: space-between; align-items: center;">
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <div style="background: white; color: #4CAF50; width: 45px; height: 45px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700;">${displayOrder}</div>
                        <div>
                            <span style="color: white; font-weight: 600;"><i class="fas fa-edit"></i> Editing Question ${displayOrder}</span>
                        </div>
                        </div>
                    <button onclick="renderSingleQuestion()" style="background: rgba(255,255,255,0.2); color: white; border: none; padding: 8px 15px; border-radius: 8px; cursor: pointer; font-size: 13px;">
                        <i class="fas fa-times"></i> Cancel
                    </button>
                </div>
                
                <!-- Edit Form -->
                <div style="padding: 20px;">
                    <!-- English Section -->
                    <div style="margin-bottom: 20px;">
                        <label style="display: inline-block; background: #e3f2fd; color: #1565c0; padding: 4px 12px; border-radius: 4px; font-size: 11px; font-weight: 600; margin-bottom: 8px;">English Question</label>
                        <textarea id="editQEn" style="width: 100%; min-height: 80px; padding: 12px; border: 2px solid #e0e0e0; border-radius: 10px; font-size: 14px; resize: vertical; font-family: inherit;">${q.question_en || ''}</textarea>
                    </div>
                    
                    <!-- Tamil Section -->
                    <div style="margin-bottom: 20px;">
                        <label style="display: inline-block; background: #fff3e0; color: #e65100; padding: 4px 12px; border-radius: 4px; font-size: 11px; font-weight: 600; margin-bottom: 8px;">தமிழ் கேள்வி</label>
                        <textarea id="editQTa" style="width: 100%; min-height: 80px; padding: 12px; border: 2px solid #e0e0e0; border-radius: 10px; font-size: 14px; resize: vertical; font-family: inherit;">${q.question_ta || ''}</textarea>
                    </div>
                </div>
                
                <!-- Options Section -->
                <div style="padding: 0 20px 20px;">
                    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 15px;">
                        <h4 style="margin: 0; font-size: 14px; color: #333;"><i class="fas fa-list"></i> Options</h4>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <span style="font-size: 13px; color: #666;">Correct:</span>
                            <select id="editCorrectAnswer" style="padding: 6px 12px; border: 2px solid #4CAF50; border-radius: 6px; font-weight: 600; color: #2e7d32; background: #e8f5e9;">
                                <option value="A" ${q.correct_answer === 'A' ? 'selected' : ''}>A</option>
                                <option value="B" ${q.correct_answer === 'B' ? 'selected' : ''}>B</option>
                                <option value="C" ${q.correct_answer === 'C' ? 'selected' : ''}>C</option>
                                <option value="D" ${q.correct_answer === 'D' ? 'selected' : ''}>D</option>
                            </select>
                        </div>
                    </div>
                    
                    <!-- Option A -->
                    <div style="background: #f8f9fa; border-radius: 10px; padding: 12px; margin-bottom: 10px;">
                        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
                            <span style="background: #e3f2fd; color: #1565c0; width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 12px;">A</span>
                            <span style="font-size: 12px; color: #666;">Option A</span>
                        </div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                            <input type="text" id="editOptAEn" value="${q.option_a_en || ''}" placeholder="English" style="padding: 10px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 13px;">
                            <input type="text" id="editOptATa" value="${q.option_a_ta || ''}" placeholder="தமிழ்" style="padding: 10px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 13px;">
                        </div>
                    </div>
                    
                    <!-- Option B -->
                    <div style="background: #f8f9fa; border-radius: 10px; padding: 12px; margin-bottom: 10px;">
                        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
                            <span style="background: #fce4ec; color: #c2185b; width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 12px;">B</span>
                            <span style="font-size: 12px; color: #666;">Option B</span>
                        </div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                            <input type="text" id="editOptBEn" value="${q.option_b_en || ''}" placeholder="English" style="padding: 10px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 13px;">
                            <input type="text" id="editOptBTa" value="${q.option_b_ta || ''}" placeholder="தமிழ்" style="padding: 10px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 13px;">
                        </div>
                    </div>
                    
                    <!-- Option C -->
                    <div style="background: #f8f9fa; border-radius: 10px; padding: 12px; margin-bottom: 10px;">
                        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
                            <span style="background: #fff3e0; color: #e65100; width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 12px;">C</span>
                            <span style="font-size: 12px; color: #666;">Option C</span>
                        </div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                            <input type="text" id="editOptCEn" value="${q.option_c_en || ''}" placeholder="English" style="padding: 10px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 13px;">
                            <input type="text" id="editOptCTa" value="${q.option_c_ta || ''}" placeholder="தமிழ்" style="padding: 10px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 13px;">
                        </div>
                    </div>
                    
                    <!-- Option D -->
                    <div style="background: #f8f9fa; border-radius: 10px; padding: 12px; margin-bottom: 10px;">
                        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
                            <span style="background: #e8f5e9; color: #2e7d32; width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 12px;">D</span>
                            <span style="font-size: 12px; color: #666;">Option D</span>
                        </div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                            <input type="text" id="editOptDEn" value="${q.option_d_en || ''}" placeholder="English" style="padding: 10px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 13px;">
                            <input type="text" id="editOptDTa" value="${q.option_d_ta || ''}" placeholder="தமிழ்" style="padding: 10px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 13px;">
                        </div>
                    </div>
                </div>
                
                <!-- Table Section -->
                <div style="padding: 0 20px 20px;">
                    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px;">
                        <h4 style="margin: 0; font-size: 14px; color: #333;"><i class="fas fa-table"></i> Table Data</h4>
                        <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
                            <span style="font-size: 13px;">Has Table</span>
                            <input type="checkbox" id="editHasTable" ${q.table_data ? 'checked' : ''} onchange="toggleEditTableEditor()" style="width: 18px; height: 18px;">
                        </label>
                    </div>
                    <div id="editTableEditor" style="display: ${q.table_data ? 'block' : 'none'}; background: #f8f9fa; border-radius: 10px; padding: 15px;">
                        <div id="editTableRows"></div>
                        <button onclick="addEditTableRow()" style="width: 100%; margin-top: 10px; padding: 10px; background: #e8f5e9; color: #4CAF50; border: none; border-radius: 8px; cursor: pointer;">
                            <i class="fas fa-plus"></i> Add Row
                        </button>
                    </div>
                </div>
                
                <!-- Save Button -->
                <div style="padding: 20px; background: #f8f9fa; border-top: 1px solid #e0e0e0; display: flex; gap: 10px;">
                    <button onclick="renderSingleQuestion()" style="flex: 1; padding: 15px; background: #f0f0f0; color: #666; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer;">
                        <i class="fas fa-times"></i> Cancel
                    </button>
                    <button onclick="saveEditedQuestion(${q.id})" style="flex: 2; padding: 15px; background: linear-gradient(135deg, #4CAF50 0%, #43a047 100%); color: white; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer;">
                        <i class="fas fa-save"></i> Save Changes
                    </button>
                </div>
            </div>
                    </div>
                `;
    
    // Load table data if exists
    if (q.table_data) {
        loadEditTableData(q.table_data);
    }
}

// Toggle table editor
function toggleEditTableEditor() {
    const editor = document.getElementById('editTableEditor');
    const checkbox = document.getElementById('editHasTable');
    if (editor && checkbox) {
        editor.style.display = checkbox.checked ? 'block' : 'none';
        if (checkbox.checked && document.getElementById('editTableRows').children.length === 0) {
            addEditTableRow();
        }
    }
}

// Add table row in editor
function addEditTableRow() {
    const container = document.getElementById('editTableRows');
    if (!container) return;
    
    const row = document.createElement('div');
    row.style.cssText = 'display: grid; grid-template-columns: 1fr 1fr auto; gap: 10px; margin-bottom: 8px;';
    row.innerHTML = `
        <input type="text" placeholder="Column A value" style="padding: 8px; border: 1px solid #e0e0e0; border-radius: 6px;">
        <input type="text" placeholder="Column B value" style="padding: 8px; border: 1px solid #e0e0e0; border-radius: 6px;">
        <button onclick="this.parentElement.remove()" style="width: 32px; height: 32px; background: #ffebee; color: #f44336; border: none; border-radius: 6px; cursor: pointer;"><i class="fas fa-times"></i></button>
    `;
    container.appendChild(row);
}

// Load table data into editor
function loadEditTableData(tableDataStr) {
    const container = document.getElementById('editTableRows');
    if (!container) return;
    container.innerHTML = '';
    
    try {
        const data = typeof tableDataStr === 'string' ? JSON.parse(tableDataStr) : tableDataStr;
        const maxLen = Math.max(data.column_A?.length || 0, data.column_B?.length || 0);
        
        for (let i = 0; i < maxLen; i++) {
            const row = document.createElement('div');
            row.style.cssText = 'display: grid; grid-template-columns: 1fr 1fr auto; gap: 10px; margin-bottom: 8px;';
            row.innerHTML = `
                <input type="text" value="${data.column_A?.[i]?.value || ''}" placeholder="Column A value" style="padding: 8px; border: 1px solid #e0e0e0; border-radius: 6px;">
                <input type="text" value="${data.column_B?.[i]?.value || ''}" placeholder="Column B value" style="padding: 8px; border: 1px solid #e0e0e0; border-radius: 6px;">
                <button onclick="this.parentElement.remove()" style="width: 32px; height: 32px; background: #ffebee; color: #f44336; border: none; border-radius: 6px; cursor: pointer;"><i class="fas fa-times"></i></button>
            `;
            container.appendChild(row);
        }
    } catch (e) {
        console.error('Error loading table data:', e);
    }
}

// Get table data from editor
function getEditTableData() {
    const rows = document.querySelectorAll('#editTableRows > div');
    const data = { column_A: [], column_B: [] };
    rows.forEach((row, idx) => {
        const inputs = row.querySelectorAll('input');
        if (inputs[0]?.value || inputs[1]?.value) {
            data.column_A.push({ key: String.fromCharCode(97 + idx), value: inputs[0]?.value || '' });
            data.column_B.push({ key: String(idx + 1), value: inputs[1]?.value || '' });
        }
    });
    return JSON.stringify(data);
}

// Save edited question
async function saveEditedQuestion(questionId) {
    const questionData = {
        id: questionId,
        session_id: currentViewSessionId,
        question_en: document.getElementById('editQEn')?.value || null,
        question_ta: document.getElementById('editQTa')?.value || null,
        option_a_en: document.getElementById('editOptAEn')?.value || null,
        option_a_ta: document.getElementById('editOptATa')?.value || null,
        option_b_en: document.getElementById('editOptBEn')?.value || null,
        option_b_ta: document.getElementById('editOptBTa')?.value || null,
        option_c_en: document.getElementById('editOptCEn')?.value || null,
        option_c_ta: document.getElementById('editOptCTa')?.value || null,
        option_d_en: document.getElementById('editOptDEn')?.value || null,
        option_d_ta: document.getElementById('editOptDTa')?.value || null,
        correct_answer: document.getElementById('editCorrectAnswer')?.value || 'A'
    };
    
    // Add table data if enabled
    if (document.getElementById('editHasTable')?.checked) {
        questionData.table_data = getEditTableData();
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(questionData)
        });
        
        const result = await response.json();
        if (result.success) {
            showNotification('Question saved!', 'success');
            await refreshViewQuestions();
        } else {
            showNotification(result.message || 'Error saving', 'error');
        }
    } catch (error) {
        console.error('Save error:', error);
        showNotification('Error saving question', 'error');
    }
}

// Open create question form for empty slot
async function openCreateQuestionForm(displayOrder) {
    const questionData = {
        session_id: currentViewSessionId,
        display_order: displayOrder,
        question_en: '',
        question_ta: '',
        option_a_en: '-',
        option_a_ta: '-',
        option_b_en: '-',
        option_b_ta: '-',
        option_c_en: '-',
        option_c_ta: '-',
        option_d_en: '-',
        option_d_ta: '-',
        correct_answer: 'A',
        difficulty: 'medium',
        marks: 1,
        negative_marks: 0.25
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(questionData)
        });
        
        const result = await response.json();
        if (result.success) {
            showNotification('Question slot created! Now edit it.', 'success');
            await refreshViewQuestions();
        } else {
            showNotification(result.message || 'Error creating', 'error');
        }
    } catch (error) {
        console.error('Create error:', error);
        showNotification('Error creating question', 'error');
    }
}

// Navigation functions
function navigateToNextQuestion() {
    if (currentQuestionIndex < TOTAL_QUESTIONS - 1) {
        currentQuestionIndex++;
        renderSingleQuestion();
    }
}

function navigateToPrevQuestion() {
    if (currentQuestionIndex > 0) {
        currentQuestionIndex--;
        renderSingleQuestion();
    }
}

// Update pills highlight
function updateQuestionPillsHighlight() {
    const pills = document.querySelectorAll('#questionNumberPills button');
    pills.forEach((pill, idx) => {
        const isCurrent = idx === currentQuestionIndex;
        if (isCurrent) {
            pill.style.background = '#1e3a5f';
            pill.style.color = 'white';
            pill.style.border = '2px solid #1e3a5f';
        }
    });
}

// Original function kept for compatibility
function renderQuestionsTableOld() {
    const container = document.getElementById('questionsCardsContainer');
    const pillsContainer = document.getElementById('questionNumberPills');
    const totalPages = Math.ceil(filteredQuestions.length / questionsPerPage);
    const startIndex = (currentPage - 1) * questionsPerPage;
    const endIndex = startIndex + questionsPerPage;
    const pageQuestions = filteredQuestions.slice(startIndex, endIndex);
    
    // Update page info
    document.getElementById('currentPageInfo').textContent = `${currentPage} / ${totalPages || 1}`;
    
    // Render question number pills (show all question numbers)
    renderQuestionPills();
    
    if (filteredQuestions.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; padding: 60px;">
                <i class="fas fa-inbox" style="font-size: 64px; color: #ddd; margin-bottom: 20px; display: block;"></i>
                <h3 style="color: #999; margin: 0;">No Questions Found</h3>
                <p style="color: #bbb;">Upload questions to this session.</p>
            </div>
        `;
        return;
    }
    
    // Render Flutter-style question cards
    container.innerHTML = pageQuestions.map((q, index) => {
        const qNum = q.question_number || (startIndex + index + 1);
            const hasEnglish = q.question_en && q.question_en.trim();
            const hasTamil = q.question_ta && q.question_ta.trim();
            
            return `
            <div class="question-card" style="
                background: white;
                border-radius: 16px;
                margin-bottom: 15px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.08);
                overflow: hidden;
                transition: transform 0.2s, box-shadow 0.2s;
            " onmouseover="this.style.transform='translateY(-2px)';this.style.boxShadow='0 4px 20px rgba(0,0,0,0.12)';" onmouseout="this.style.transform='none';this.style.boxShadow='0 2px 10px rgba(0,0,0,0.08)';">
                
                <!-- Question Header -->
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 12px 20px; display: flex; justify-content: space-between; align-items: center;">
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <div style="background: white; color: #667eea; width: 45px; height: 45px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 18px;">
                            ${qNum}
                        </div>
                        <div>
                            <span style="color: white; font-weight: 600;">Question ${qNum}</span>
                            <div style="display: flex; gap: 5px; margin-top: 3px;">
                                ${hasEnglish ? '<span style="background: rgba(255,255,255,0.3); color: white; padding: 2px 8px; border-radius: 10px; font-size: 10px;">EN</span>' : ''}
                                ${hasTamil ? '<span style="background: rgba(255,255,255,0.3); color: white; padding: 2px 8px; border-radius: 10px; font-size: 10px;">த</span>' : ''}
                        </div>
                    </div>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <button onclick="openJsonEditModal(${q.id})" title="Edit with JSON" style="background: rgba(255,255,255,0.2); color: white; border: none; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; transition: background 0.2s;" onmouseover="this.style.background='rgba(255,255,255,0.4)'" onmouseout="this.style.background='rgba(255,255,255,0.2)'">
                            <i class="fas fa-code"></i>
                        </button>
                        <button onclick="editSingleQuestion(${q.id})" title="Edit Form" style="background: rgba(255,255,255,0.2); color: white; border: none; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; transition: background 0.2s;" onmouseover="this.style.background='rgba(255,255,255,0.4)'" onmouseout="this.style.background='rgba(255,255,255,0.2)'">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button onclick="deleteSingleQuestion(${q.id})" title="Delete" style="background: rgba(255,87,87,0.8); color: white; border: none; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; transition: background 0.2s;" onmouseover="this.style.background='rgba(255,87,87,1)'" onmouseout="this.style.background='rgba(255,87,87,0.8)'">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
                
                <!-- Question Content -->
                <div style="padding: 20px;">
                    ${hasEnglish ? `
                        <div style="margin-bottom: ${hasTamil ? '15px' : '0'};">
                            <div style="font-size: 11px; color: #6C63FF; font-weight: 600; margin-bottom: 5px; text-transform: uppercase;">English</div>
                            <p style="margin: 0 0 12px 0; font-size: 15px; color: #333; line-height: 1.5;">${q.question_en}</p>
                            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                                ${renderOption('A', q.option_a_en, q.correct_answer === 'A')}
                                ${renderOption('B', q.option_b_en, q.correct_answer === 'B')}
                                ${renderOption('C', q.option_c_en, q.correct_answer === 'C')}
                                ${renderOption('D', q.option_d_en, q.correct_answer === 'D')}
                        </div>
                            ${q.explanation_en ? `<div style="margin-top: 10px; padding: 10px; background: #e8f5e9; border-radius: 8px; font-size: 13px; color: #2e7d32;"><i class="fas fa-lightbulb"></i> ${q.explanation_en}</div>` : ''}
                        </div>
                    ` : ''}
                    
                    ${hasTamil ? `
                        ${hasEnglish ? '<hr style="border: none; border-top: 1px dashed #e0e0e0; margin: 15px 0;">' : ''}
                        <div>
                            <div style="font-size: 11px; color: #F57C00; font-weight: 600; margin-bottom: 5px; text-transform: uppercase;">தமிழ்</div>
                            <p style="margin: 0 0 12px 0; font-size: 15px; color: #333; line-height: 1.5;">${q.question_ta}</p>
                            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                                ${renderOption('அ', q.option_a_ta, q.correct_answer === 'A')}
                                ${renderOption('ஆ', q.option_b_ta, q.correct_answer === 'B')}
                                ${renderOption('இ', q.option_c_ta, q.correct_answer === 'C')}
                                ${renderOption('ஈ', q.option_d_ta, q.correct_answer === 'D')}
                            </div>
                            ${q.explanation_ta ? `<div style="margin-top: 10px; padding: 10px; background: #fff3e0; border-radius: 8px; font-size: 13px; color: #e65100;"><i class="fas fa-lightbulb"></i> ${q.explanation_ta}</div>` : ''}
                        </div>
                    ` : ''}
                </div>
                
                <!-- Answer Footer -->
                <div style="background: #f8f9fa; padding: 12px 20px; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #eee;">
                    <span style="color: #666; font-size: 13px;"><i class="fas fa-check-circle" style="color: #4CAF50;"></i> Correct Answer</span>
                    <span style="background: #4CAF50; color: white; padding: 6px 20px; border-radius: 20px; font-weight: bold; font-size: 14px;">Option ${q.correct_answer || 'A'}</span>
                </div>
                    </div>
            `;
        }).join('');
    }
    
// Helper function to render option
function renderOption(label, text, isCorrect) {
            return `
        <div style="
            padding: 10px 12px;
            background: ${isCorrect ? 'linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%)' : '#f5f5f5'};
            border-radius: 8px;
            border: 2px solid ${isCorrect ? '#4CAF50' : 'transparent'};
            font-size: 13px;
            display: flex;
            align-items: flex-start;
            gap: 8px;
        ">
            <span style="
                background: ${isCorrect ? '#4CAF50' : '#e0e0e0'};
                color: ${isCorrect ? 'white' : '#666'};
                width: 22px;
                height: 22px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-weight: bold;
                font-size: 11px;
                flex-shrink: 0;
            ">${label}</span>
            <span style="color: #333;">${text || '-'}</span>
        </div>
    `;
}

// Render question number pills for quick navigation (1-200 grid)
function renderQuestionPills() {
    const container = document.getElementById('questionNumberPills');
    if (!container) return;
    
    // Create a map of existing questions by display_order
    const existingQuestions = {};
    allSessionQuestions.forEach(q => {
        existingQuestions[q.display_order] = q;
    });
    
    let html = '';
    for (let i = 1; i <= TOTAL_QUESTIONS; i++) {
        const q = existingQuestions[i];
        const isCurrent = (i - 1) === currentQuestionIndex;
        const hasQuestion = !!q;
        const isPlaceholder = q?.question_en?.includes('[PLACEHOLDER') || q?.question_ta?.includes('[PLACEHOLDER');
        
        let bgColor = '#ffebee'; // Empty - light red
        let borderColor = '#f44336';
        let textColor = '#c62828';
        
        if (hasQuestion && !isPlaceholder) {
            bgColor = '#e8f5e9'; // Filled - green
            borderColor = '#4CAF50';
            textColor = '#2e7d32';
        } else if (isPlaceholder) {
            bgColor = '#fff3e0'; // Placeholder - orange
            borderColor = '#ff9800';
            textColor = '#e65100';
        }
        
        if (isCurrent) {
            bgColor = '#1e3a5f';
            borderColor = '#1e3a5f';
            textColor = 'white';
        }
        
        html += `
            <button onclick="goToQuestionIndex(${i - 1})" style="
                aspect-ratio: 1;
                border: 2px solid ${borderColor};
                background: ${bgColor};
                border-radius: 6px;
                cursor: pointer;
                font-size: 11px;
                font-weight: 600;
                color: ${textColor};
                transition: all 0.15s;
                display: flex;
                align-items: center;
                justify-content: center;
            " onmouseover="if(!${isCurrent}){this.style.background='#1e3a5f';this.style.color='white';this.style.borderColor='#1e3a5f'}" 
               onmouseout="if(!${isCurrent}){this.style.background='${bgColor}';this.style.color='${textColor}';this.style.borderColor='${borderColor}'}">${i}</button>
        `;
    }
    
    container.innerHTML = html;
}

// Go to specific question index
function goToQuestionIndex(index) {
    if (index >= 0 && index < TOTAL_QUESTIONS) {
        currentQuestionIndex = index;
        renderSingleQuestion();
        renderQuestionPills(); // Re-render pills to update highlight
    }
}

// Jump to specific question (updated for single-question view)
function jumpToQuestion() {
    const input = document.getElementById('jumpToQuestion');
    const qNum = parseInt(input.value);
    if (!qNum) return;
    
    jumpToQuestionNumber(qNum);
}

function jumpToQuestionNumber(qNum) {
    if (qNum < 1 || qNum > TOTAL_QUESTIONS) {
        showNotification(`Question ${qNum} not found (1-${TOTAL_QUESTIONS})`, 'warning');
        return;
    }
    
    goToQuestionIndex(qNum - 1);
    showNotification(`Showing Question ${qNum}`, 'info');
}

// Open JSON Edit Modal for single question
function openJsonEditModal(questionId) {
    const question = allSessionQuestions.find(q => q.id === questionId);
    if (!question) return;
    
    document.getElementById('editJsonQuestionId').value = question.id;
    document.getElementById('editJsonSessionId').value = currentViewSessionId;
    document.getElementById('editQuestionNumber').textContent = `Q.${question.question_number || '?'}`;
    
    // Show current question preview
    const hasEnglish = question.question_en && question.question_en.trim();
    const hasTamil = question.question_ta && question.question_ta.trim();
    
    document.getElementById('currentQuestionContent').innerHTML = `
        ${hasEnglish ? `<p style="margin: 0; font-size: 13px;"><strong>EN:</strong> ${question.question_en.substring(0, 100)}...</p>` : ''}
        ${hasTamil ? `<p style="margin: 5px 0 0 0; font-size: 13px;"><strong>த:</strong> ${question.question_ta.substring(0, 100)}...</p>` : ''}
        <p style="margin: 5px 0 0 0; font-size: 12px; color: #4CAF50;"><strong>Answer:</strong> ${question.correct_answer}</p>
    `;
    
    // Pre-fill JSON editor with current question data
    const jsonData = {
        questionNumber: question.question_number || '',
        question: question.question_en || '',
        question_ta: question.question_ta || '',
        options: {
            A: question.option_a_en || '',
            B: question.option_b_en || '',
            C: question.option_c_en || '',
            D: question.option_d_en || ''
        },
        options_ta: {
            A: question.option_a_ta || '',
            B: question.option_b_ta || '',
            C: question.option_c_ta || '',
            D: question.option_d_ta || ''
        },
        correctAnswer: question.correct_answer || 'A',
        answerExplanation: question.explanation_en || '',
        answerExplanation_ta: question.explanation_ta || ''
    };
    
    document.getElementById('questionJsonEditor').value = JSON.stringify(jsonData, null, 2);
    openModal('editQuestionJsonModal');
}

// Format JSON in editor
function formatJsonEditor() {
    const editor = document.getElementById('questionJsonEditor');
    try {
        const json = JSON.parse(editor.value);
        editor.value = JSON.stringify(json, null, 2);
    } catch (e) {
        showNotification('Invalid JSON format', 'error');
    }
}

// Load JSON from file for single question
function loadJsonFromFile() {
    document.getElementById('singleQuestionJsonFile').click();
}

function handleSingleQuestionFile(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    const reader = new FileReader();
    reader.onload = function(e) {
        document.getElementById('questionJsonEditor').value = e.target.result;
        formatJsonEditor();
    };
    reader.readAsText(file);
}

// Save question from JSON editor
async function saveQuestionFromJson() {
    const questionId = document.getElementById('editJsonQuestionId').value;
    const sessionId = document.getElementById('editJsonSessionId').value;
    const jsonText = document.getElementById('questionJsonEditor').value;
    
    let data;
    try {
        data = JSON.parse(jsonText);
    } catch (e) {
        showNotification('Invalid JSON format. Please check syntax.', 'error');
        return;
    }
    
    // Prepare update data
    const updateData = {
        id: parseInt(questionId),
        question_en: data.question || null,
        question_ta: data.question_ta || null,
        option_a_en: data.options?.A || null,
        option_b_en: data.options?.B || null,
        option_c_en: data.options?.C || null,
        option_d_en: data.options?.D || null,
        option_a_ta: data.options_ta?.A || data.options?.A || null,
        option_b_ta: data.options_ta?.B || data.options?.B || null,
        option_c_ta: data.options_ta?.C || data.options?.C || null,
        option_d_ta: data.options_ta?.D || data.options?.D || null,
        correct_answer: data.correctAnswer || 'A',
        explanation_en: data.answerExplanation || null,
        explanation_ta: data.answerExplanation_ta || null
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updateData)
        });
        
        const result = await response.json();
        
        if (result.success) {
            showNotification('✅ Question updated successfully!', 'success');
            closeModal('editQuestionJsonModal');
            viewSessionQuestions(parseInt(sessionId));
        } else {
            showNotification(result.message || 'Failed to update question', 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('Error updating question', 'error');
    }
}

// Open Bulk Update Modal
function openBulkUpdateModal() {
    document.getElementById('bulkUpdateSessionId').value = currentViewSessionId;
    document.getElementById('bulkUpdateFile').value = '';
    document.getElementById('bulkDropContent').style.display = 'block';
    document.getElementById('bulkFileSelected').style.display = 'none';
    document.getElementById('bulkPreview').style.display = 'none';
    document.getElementById('bulkUpdateBtn').disabled = true;
    window.bulkUpdateData = null;
    openModal('bulkUpdateModal');
}

// Handle bulk update file
function handleBulkUpdateFile(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    const reader = new FileReader();
    reader.onload = function(e) {
        try {
            const data = JSON.parse(e.target.result);
            const questions = data.questions || [];
            
            if (questions.length === 0) {
                showNotification('No questions found in file', 'error');
                return;
            }
            
            window.bulkUpdateData = data;
            
            document.getElementById('bulkDropContent').style.display = 'none';
            document.getElementById('bulkFileSelected').style.display = 'block';
            document.getElementById('bulkFileName').textContent = file.name;
            document.getElementById('bulkFileInfo').textContent = `${questions.length} questions to update`;
            
            // Preview
            document.getElementById('bulkPreview').innerHTML = questions.slice(0, 5).map(q => 
                `<div style="padding: 5px 10px; background: white; margin-bottom: 3px; border-radius: 4px; font-size: 12px;"><strong>Q${q.questionNumber}:</strong> ${(q.question || '').substring(0, 40)}...</div>`
            ).join('') + (questions.length > 5 ? `<div style="text-align: center; color: #666; padding: 5px;">+${questions.length - 5} more</div>` : '');
            document.getElementById('bulkPreview').style.display = 'block';
            document.getElementById('bulkUpdateBtn').disabled = false;
            
        } catch (error) {
            showNotification('Invalid JSON file', 'error');
        }
    };
    reader.readAsText(file);
}

// Execute bulk update
async function executeBulkUpdate() {
    if (!window.bulkUpdateData) return;
    
    const sessionId = document.getElementById('bulkUpdateSessionId').value;
    const btn = document.getElementById('bulkUpdateBtn');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Updating...';
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/questions/partial_update.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                session_id: sessionId,
                questions: window.bulkUpdateData.questions
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification(`✅ Updated ${data.updated_count || 0}, Added ${data.added_count || 0} questions`, 'success');
            closeModal('bulkUpdateModal');
            viewSessionQuestions(parseInt(sessionId));
        } else {
            showNotification(data.message || 'Update failed', 'error');
        }
    } catch (error) {
        showNotification('Error updating questions', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-sync-alt"></i> Update Questions';
    }
}


function goToPage(page) {
    const totalPages = Math.ceil(filteredQuestions.length / questionsPerPage);
    if (page < 1) page = 1;
    if (page > totalPages) page = totalPages;
    if (totalPages === 0) return;
    
    currentPage = page;
    renderQuestionsTable();
    
    // Scroll to top of cards container
    const container = document.getElementById('questionsCardsContainer');
    if (container) {
        container.scrollTop = 0;
    }
}

function changeQuestionsPerPage() {
    questionsPerPage = parseInt(document.getElementById('questionsPerPage').value);
    currentPage = 1;
    renderQuestionsTable();
}

function filterQuestions() {
    // In the new design, we don't have search/filter UI
    // Just show all questions
    filteredQuestions = [...allSessionQuestions];
    currentPage = 1;
    renderQuestionsTable();
}

// Edit single question - directly populate form from loaded data
function editSingleQuestion(questionId) {
    const question = allSessionQuestions.find(q => q.id === questionId);
    if (!question) {
        showNotification('Question not found!', 'error');
        return;
    }
    
    console.log('📝 Editing question:', questionId, question);
    
    // Set hidden fields
    document.getElementById('editQuestionId').value = question.id;
    document.getElementById('editQuestionSessionId').value = currentViewSessionId;
    
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
    document.getElementById('editCorrectAnswer').value = question.correct_answer || 'A';
    
    // Open the edit modal
    openModal('editQuestionModal');
}

// Delete single question
async function deleteSingleQuestion(questionId) {
    if (!confirm('Are you sure you want to delete this question?')) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/questions/crud.php`, {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: questionId })
        });
        
        const data = await response.json();
        if (data.success) {
            showNotification('Question deleted successfully', 'success');
            // Refresh the view
            viewSessionQuestions(currentViewSessionId);
        } else {
            showNotification(data.message || 'Failed to delete question', 'error');
        }
    } catch (error) {
        console.error('Error deleting question:', error);
        showNotification('Error deleting question', 'error');
    }
}

// Export session questions as JSON
function exportSessionQuestions() {
    if (allSessionQuestions.length === 0) {
        showNotification('No questions to export', 'warning');
        return;
    }
    
    const exportData = {
        totalQuestions: allSessionQuestions.length,
        questions: allSessionQuestions.map(q => ({
            questionNumber: q.question_number || '',
            question: q.question_en || q.question_ta || '',
            question_ta: q.question_ta || '',
            options: {
                A: q.option_a_en || q.option_a_ta || '',
                B: q.option_b_en || q.option_b_ta || '',
                C: q.option_c_en || q.option_c_ta || '',
                D: q.option_d_en || q.option_d_ta || ''
            },
            options_ta: {
                A: q.option_a_ta || '',
                B: q.option_b_ta || '',
                C: q.option_c_ta || '',
                D: q.option_d_ta || ''
            },
            correctAnswer: q.correct_answer || 'A',
            answerExplanation: q.explanation_en || q.explanation_ta || '',
            answerExplanation_ta: q.explanation_ta || ''
        }))
    };
    
    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `questions_session_${currentViewSessionId}_${Date.now()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    showNotification('Questions exported successfully', 'success');
}

// Open Update Questions Modal
function openUpdateQuestionsModal() {
    document.getElementById('updateSessionId').value = currentViewSessionId;
    document.getElementById('updateJsonFile').value = '';
    document.getElementById('updateDropZoneContent').style.display = 'block';
    document.getElementById('updateFileSelectedContent').style.display = 'none';
    document.getElementById('updatePreview').style.display = 'none';
    document.getElementById('updateQuestionsBtn').disabled = true;
    window.updateQuestionsData = null;
    openModal('updateQuestionsModal');
}

// Handle update file selection
function handleUpdateFileSelect(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    if (!file.name.endsWith('.json')) {
        showNotification('Please select a valid JSON file', 'error');
        return;
    }
    
    const reader = new FileReader();
    reader.onload = function(e) {
        try {
            const data = JSON.parse(e.target.result);
            const questions = data.questions || [];
            
            if (questions.length === 0) {
                showNotification('No questions found in the file', 'error');
                return;
            }
            
            window.updateQuestionsData = data;
            
            // Update UI
            document.getElementById('updateDropZoneContent').style.display = 'none';
            document.getElementById('updateFileSelectedContent').style.display = 'block';
            document.getElementById('updateSelectedFileName').textContent = file.name;
            document.getElementById('updateSelectedFileInfo').textContent = `${questions.length} questions to update`;
            
            // Show preview
            const previewList = document.getElementById('updatePreviewList');
            previewList.innerHTML = questions.slice(0, 10).map(q => `
                <div style="padding: 8px; background: white; margin-bottom: 5px; border-radius: 6px; border-left: 3px solid #4CAF50;">
                    <strong>Q${q.questionNumber}:</strong> ${(q.question || q.question_ta || '').substring(0, 50)}...
                </div>
            `).join('') + (questions.length > 10 ? `<div style="text-align: center; color: #666; padding: 10px;">...and ${questions.length - 10} more</div>` : '');
            
            document.getElementById('updatePreview').style.display = 'block';
            document.getElementById('updateQuestionsBtn').disabled = false;
            
        } catch (error) {
            console.error('Error parsing JSON:', error);
            showNotification('Invalid JSON file format', 'error');
        }
    };
    reader.readAsText(file);
}

function clearUpdateFile() {
    document.getElementById('updateJsonFile').value = '';
    document.getElementById('updateDropZoneContent').style.display = 'block';
    document.getElementById('updateFileSelectedContent').style.display = 'none';
    document.getElementById('updatePreview').style.display = 'none';
    document.getElementById('updateQuestionsBtn').disabled = true;
    window.updateQuestionsData = null;
}

// Execute partial update
async function executePartialUpdate() {
    if (!window.updateQuestionsData) {
        showNotification('No questions data to update', 'error');
        return;
    }
    
    const sessionId = document.getElementById('updateSessionId').value;
    const btn = document.getElementById('updateQuestionsBtn');
    const originalText = btn.innerHTML;
    
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Updating...';
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/questions/partial_update.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                session_id: sessionId,
                questions: window.updateQuestionsData.questions
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification(`✅ Updated ${data.updated_count || 0} questions, Added ${data.added_count || 0} new questions`, 'success');
            closeModal('updateQuestionsModal');
            // Refresh the view
            viewSessionQuestions(parseInt(sessionId));
        } else {
            showNotification(data.message || 'Failed to update questions', 'error');
        }
    } catch (error) {
        console.error('Error updating questions:', error);
        showNotification('Error updating questions', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
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
        const submittedDate = result.submitted_at ? (parseUtcDateTime(result.submitted_at)?.toLocaleDateString('en-IN', { timeZone: 'Asia/Kolkata' }) || 'N/A') : 'N/A';
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
          `Submitted: ${result.submitted_at ? (parseUtcDateTime(result.submitted_at)?.toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' }) || 'N/A') : 'N/A'}\n\n` +
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
        
        // Load guest login toggle
        await loadGuestLoginSetting();
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

// Guest Login Toggle (feature flag)
async function loadGuestLoginSetting() {
    try {
        const res = await fetch(`${API_BASE_URL}/admin/settings/crud.php?key=guest_login_enabled`);
        const data = await res.json();
        const el = document.getElementById('guestLoginEnabled');
        if (!el) return;

        if (data.success && data.setting) {
            const v = (data.setting.setting_value || '').toString().trim().toLowerCase();
            el.checked = (v === '1' || v === 'true' || v === 'yes' || v === 'on');
        } else {
            // Default ON if not set yet
            el.checked = true;
        }
    } catch (e) {
        console.error('Error loading guest login setting:', e);
    }
}

async function saveGuestLoginSetting() {
    const el = document.getElementById('guestLoginEnabled');
    const enabled = el && el.checked ? '1' : '0';
    try {
        const response = await fetch(`${API_BASE_URL}/admin/settings/crud.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                setting_key: 'guest_login_enabled',
                setting_value: enabled,
                setting_type: 'text',
                description: 'Show/hide Guest Login in the mobile app (1/0)'
            })
        });

        const data = await response.json();
        if (data.success) {
            showNotification('✅ Guest Login setting saved!', 'success');
        } else {
            showNotification('❌ Failed to save: ' + (data.message || 'Unknown error'), 'error');
        }
    } catch (error) {
        console.error('Error saving guest login setting:', error);
        showNotification('❌ Error saving: ' + error.message, 'error');
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

    // Special handling for test category modal
    if (modalId === 'testCategoryModal') {
        const form = document.getElementById('testCategoryForm');
        const isEditMode = form?.dataset?.editMode === 'true';
        
        // Only reset if not in edit mode
        if (!isEditMode) {
            form?.reset();
            if (form?.dataset) {
                delete form.dataset.categoryId;
                delete form.dataset.editMode;
            }
            const title = document.querySelector('#testCategoryModal h2');
            if (title) title.textContent = 'Add Test Category';
            resetTestCategoryImageUpload();
        }
    }
    
    const modal = document.getElementById(modalId);
    if (!modal) {
        console.error('❌ Modal not found:', modalId);
        return;
    }
    modal.classList.add('active');
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

    // Reset test category form when closing the modal
    if (modalId === 'testCategoryModal') {
        const form = document.getElementById('testCategoryForm');
        if (form) {
            form.reset();
            delete form.dataset.categoryId;
            delete form.dataset.editMode;
        }
        const title = document.querySelector('#testCategoryModal h2');
        if (title) title.textContent = 'Add Test Category';
        resetTestCategoryImageUpload();
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

        // Reset test category form if closing that modal
        if (modalId === 'testCategoryModal') {
            const form = document.getElementById('testCategoryForm');
            if (form) {
                form.reset();
                delete form.dataset.categoryId;
                delete form.dataset.editMode;
            }
            const title = document.querySelector('#testCategoryModal h2');
            if (title) title.textContent = 'Add Test Category';
            resetTestCategoryImageUpload();
        }
        
        event.target.classList.remove('active');
    }
}

// Notification
function showNotification(message, type = 'info') {
    // Remove any existing notifications first
    const existingNotifications = document.querySelectorAll('.notification');
    existingNotifications.forEach(n => n.remove());
    
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    
    // Set icon and colors based on type
    let icon = 'fa-info-circle';
    let bgColor = '#2196F3'; // info blue
    
    if (type === 'success') {
        icon = 'fa-check-circle';
        bgColor = '#4CAF50';
    } else if (type === 'error') {
        icon = 'fa-times-circle';
        bgColor = '#f44336';
    } else if (type === 'warning') {
        icon = 'fa-exclamation-triangle';
        bgColor = '#FF9800';
    }
    
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 16px 24px;
        border-radius: 12px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
        z-index: 99999;
        display: flex;
        align-items: center;
        gap: 12px;
        background: ${bgColor};
        color: white;
        font-size: 14px;
        font-weight: 500;
        animation: slideInNotification 0.3s ease;
        max-width: 400px;
    `;
    
    notification.innerHTML = `
        <i class="fas ${icon}" style="font-size: 20px;"></i>
        <span>${message}</span>
    `;
    
    // Add animation keyframes if not exists
    if (!document.getElementById('notification-styles')) {
        const style = document.createElement('style');
        style.id = 'notification-styles';
        style.textContent = `
            @keyframes slideInNotification {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }
            @keyframes slideOutNotification {
                from { transform: translateX(0); opacity: 1; }
                to { transform: translateX(100%); opacity: 0; }
            }
        `;
        document.head.appendChild(style);
    }
    
    document.body.appendChild(notification);
    
    // Auto remove after 3 seconds
    setTimeout(() => {
        notification.style.animation = 'slideOutNotification 0.3s ease';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
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
        const date = parseUtcDateTime(feedback.created_at);
        const formattedDate = date ? date.toLocaleString('en-IN', { 
            timeZone: 'Asia/Kolkata',
            year: 'numeric', 
            month: 'short', 
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }) : '-';

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

    const date = parseUtcDateTime(feedback.created_at);
    const formattedDate = date ? date.toLocaleString('en-IN', { 
        timeZone: 'Asia/Kolkata',
        year: 'numeric', 
        month: 'long', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    }) : '-';

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
                            <i class="fas fa-clock"></i> ${parseUtcDateTime(feedback.admin_response_at) ? parseUtcDateTime(feedback.admin_response_at).toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' }) : feedback.admin_response_at}
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
    } // else if (page === 'questionScraper') { // REMOVED
        // Initialize scraper if needed
    // }
};

// ============================================
// QUESTION SCRAPER TOOL FUNCTIONS
// ============================================

let extractedQuestions = [];

// Function is now defined directly on window (see below)

// Make functions globally accessible - MUST be after function definitions
// Define immediately, don't wait for window
window.extractQuestions = function(event) {
    console.log('=== EXTRACT QUESTIONS CALLED ===');
    console.log('Event:', event);
    
    // Prevent default if event exists
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    
    const htmlContent = document.getElementById('htmlContent');
    if (!htmlContent) {
        console.error('htmlContent textarea not found!');
        alert('Error: HTML content textarea not found');
        return;
    }
    
    const content = htmlContent.value.trim();
    
    if (!content) {
        alert('Please paste HTML content first!');
        if (typeof showNotification === 'function') {
            showNotification('Please paste HTML content', 'error');
        }
        return;
    }
    
    console.log('HTML content length:', content.length);
    
    // Get button reference
    const extractBtn = document.getElementById('extractBtn') || (event && event.target);
    
    try {
        // Show loading state
        if (extractBtn) {
            extractBtn.disabled = true;
            extractBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Extracting...';
        }
        
        // Create a temporary DOM parser
        const parser = new DOMParser();
        const doc = parser.parseFromString(content, 'text/html');
        
        // Check for parsing errors
        const parserError = doc.querySelector('parsererror');
        if (parserError) {
            console.error('HTML parsing error:', parserError.textContent);
            showNotification('Error parsing HTML. Please check the format.', 'error');
            if (extractBtn) {
                extractBtn.disabled = false;
                extractBtn.innerHTML = '<i class="fas fa-magic"></i> Extract Questions';
            }
            return;
        }
        
        // Find all question containers - try multiple selectors
        let questionDivs = doc.querySelectorAll('.qus');
        console.log('Found questions with .qus:', questionDivs.length);
        
        if (questionDivs.length === 0) {
            questionDivs = doc.querySelectorAll('.mt-4.mt-50.mg-bottom-30.qus');
            console.log('Found questions with .mt-4.mt-50.mg-bottom-30.qus:', questionDivs.length);
        }
        
        if (questionDivs.length === 0) {
            questionDivs = doc.querySelectorAll('[class*="qus"]');
            console.log('Found questions with [class*="qus"]:', questionDivs.length);
        }
        
        extractedQuestions = [];
        
        questionDivs.forEach((qDiv, index) => {
            try {
                // Extract question number
                const qusNum = qDiv.querySelector('.qusnum')?.textContent.trim() || `${index + 1}.`;
                const qusNumClean = qusNum.replace(/\.$/, '').trim();
                
                // Extract question description
                const qusDes = qDiv.querySelector('.qusdes');
                let questionText = '';
                if (qusDes) {
                    // Get all text content, preserving HTML structure
                    questionText = qusDes.innerHTML.trim();
                }
                
                // Extract options
                const options = [];
                const optionDivs = qDiv.querySelectorAll('.list-unstyled .mt-2.col-12.p-0');
                optionDivs.forEach((optDiv, optIndex) => {
                    const optLabel = optDiv.querySelector('.qusnum')?.textContent.trim() || String.fromCharCode(65 + optIndex);
                    const optText = optDiv.querySelector('.qusdes')?.innerHTML.trim() || '';
                    if (optText) {
                        options.push({
                            label: optLabel.replace(/\.$/, '').trim(),
                            text: optText
                        });
                    }
                });
                
                // Extract answer
                const answerDiv = qDiv.querySelector('.answer');
                let answerText = '';
                if (answerDiv) {
                    const answerContent = answerDiv.textContent || answerDiv.innerText || '';
                    // Extract answer after "ANSWER" keyword
                    const answerMatch = answerContent.match(/ANSWER\s*[:\-]?\s*(.+)/i);
                    if (answerMatch) {
                        answerText = answerMatch[1].trim();
                    } else {
                        answerText = answerContent.replace(/ANSWER/gi, '').trim();
                    }
                }
                
                // Extract correct answer option (from onclick or answer text)
                let correctAnswer = '';
                if (answerText) {
                    // Try to extract option letter (A, B, C, D)
                    const answerMatch = answerText.match(/([A-D])\./);
                    if (answerMatch) {
                        correctAnswer = answerMatch[1];
                    } else {
                        // Try to find in answer text
                        const optionMatch = answerText.match(/^([A-D])/);
                        if (optionMatch) {
                            correctAnswer = optionMatch[1];
                        }
                    }
                }
                
                if (questionText || options.length > 0) {
                    extractedQuestions.push({
                        questionNumber: qusNumClean,
                        question: questionText,
                        options: options,
                        correctAnswer: correctAnswer,
                        answerText: answerText,
                        rawHtml: qDiv.outerHTML
                    });
                }
            } catch (e) {
                console.error('Error extracting question:', e);
            }
        });
        
        console.log('Total extracted questions:', extractedQuestions.length);
        
        if (extractedQuestions.length === 0) {
            showNotification('No questions found in the HTML. Please check the format.', 'error');
            // Reset button
            if (extractBtn) {
                extractBtn.disabled = false;
                extractBtn.innerHTML = '<i class="fas fa-magic"></i> Extract Questions';
            }
            return;
        }
        
        // Display results
        displayExtractedQuestions();
        showNotification(`Successfully extracted ${extractedQuestions.length} questions!`, 'success');
        
        // Reset button
        if (extractBtn) {
            extractBtn.disabled = false;
            extractBtn.innerHTML = '<i class="fas fa-magic"></i> Extract Questions';
        }
        
    } catch (error) {
        console.error('Extraction error:', error);
        console.error('Error stack:', error.stack);
        showNotification('Error extracting questions: ' + error.message, 'error');
        
        // Reset button
        if (extractBtn) {
            extractBtn.disabled = false;
            extractBtn.innerHTML = '<i class="fas fa-magic"></i> Extract Questions';
        }
    }
};

window.clearScraper = clearScraper;
window.exportQuestions = exportQuestions;
window.copyQuestions = copyQuestions;

// Question Editor functions
window.refreshViewQuestions = refreshViewQuestions;
window.navigateToNextQuestion = navigateToNextQuestion;
window.navigateToPrevQuestion = navigateToPrevQuestion;
window.goToQuestionIndex = goToQuestionIndex;
window.toggleEditTableEditor = toggleEditTableEditor;
window.addEditTableRow = addEditTableRow;
window.saveEditedQuestion = saveEditedQuestion;
window.openCreateQuestionForm = openCreateQuestionForm;
window.openEditQuestionForm = openEditQuestionForm;
// Removed: clearOldSources, getLocalStorageUsage (localStorage not used anymore)

// Also keep the original function name for compatibility
if (typeof extractQuestions === 'undefined') {
    window.extractQuestions = window.extractQuestions;
}

console.log('Question Scraper functions loaded:', {
    extractQuestions: typeof window.extractQuestions,
    clearScraper: typeof window.clearScraper,
    exportQuestions: typeof window.exportQuestions,
    copyQuestions: typeof window.copyQuestions
});

function displayExtractedQuestions() {
    const resultsDiv = document.getElementById('scraperResults');
    const listDiv = document.getElementById('extractedQuestionsList');
    const statsDiv = document.getElementById('statsContent');
    
    if (!resultsDiv || !listDiv) return;
    
    resultsDiv.style.display = 'block';
    
    // Display questions
    listDiv.innerHTML = extractedQuestions.map((q, index) => `
        <div class="question-item" style="margin-bottom: 30px; padding: 20px; background: #f8f9fa; border-radius: 8px; border-left: 4px solid #6C63FF;">
            <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 15px;">
                <h4 style="margin: 0; color: #6C63FF;">Question ${q.questionNumber}</h4>
                <span class="badge badge-info">#${index + 1}</span>
            </div>
            <div style="margin-bottom: 15px; padding: 15px; background: white; border-radius: 6px;">
                <div class="question-text">${q.question || 'No question text found'}</div>
            </div>
            <div style="margin-bottom: 15px;">
                <strong>Options:</strong>
                <div style="margin-top: 10px;">
                    ${q.options.map((opt, optIdx) => `
                        <div style="padding: 8px; margin: 5px 0; background: ${opt.label === q.correctAnswer ? '#d4edda' : 'white'}; border-radius: 4px; border-left: 3px solid ${opt.label === q.correctAnswer ? '#28a745' : '#ddd'};">
                            <strong>${opt.label}.</strong> ${opt.text}
                            ${opt.label === q.correctAnswer ? '<span class="badge badge-success ml-2">Correct</span>' : ''}
                        </div>
                    `).join('')}
                </div>
            </div>
            ${q.answerText ? `
                <div style="padding: 10px; background: #fff3cd; border-radius: 4px; margin-top: 10px;">
                    <strong>Answer:</strong> ${escapeHtml(q.answerText)}
                </div>
            ` : ''}
        </div>
    `).join('');
    
    // Display statistics
    const totalQuestions = extractedQuestions.length;
    const withAnswers = extractedQuestions.filter(q => q.correctAnswer || q.answerText).length;
    const withOptions = extractedQuestions.filter(q => q.options.length > 0).length;
    
    statsDiv.innerHTML = `
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;">
            <div>
                <strong>Total Questions:</strong> ${totalQuestions}
            </div>
            <div>
                <strong>With Answers:</strong> ${withAnswers}
            </div>
            <div>
                <strong>With Options:</strong> ${withOptions}
            </div>
            <div>
                <strong>Success Rate:</strong> ${Math.round((withAnswers / totalQuestions) * 100)}%
            </div>
        </div>
    `;
    
    // Scroll to results
    resultsDiv.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function exportQuestions() {
    if (extractedQuestions.length === 0) {
        showNotification('No questions to export', 'error');
        return;
    }
    
    // Convert to CSV format
    let csvContent = 'Question Number,Question,Option A,Option B,Option C,Option D,Correct Answer,Answer Text\n';
    
    extractedQuestions.forEach(q => {
        const question = (q.question || '').replace(/"/g, '""').replace(/\n/g, ' ').trim();
        const optA = (q.options[0]?.text || '').replace(/"/g, '""').replace(/\n/g, ' ').trim();
        const optB = (q.options[1]?.text || '').replace(/"/g, '""').replace(/\n/g, ' ').trim();
        const optC = (q.options[2]?.text || '').replace(/"/g, '""').replace(/\n/g, ' ').trim();
        const optD = (q.options[3]?.text || '').replace(/"/g, '""').replace(/\n/g, ' ').trim();
        const answer = (q.answerText || '').replace(/"/g, '""').replace(/\n/g, ' ').trim();
        
        csvContent += `"${q.questionNumber}","${question}","${optA}","${optB}","${optC}","${optD}","${q.correctAnswer}","${answer}"\n`;
    });
    
    // Create download link
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `extracted_questions_${new Date().getTime()}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    showNotification('Questions exported successfully!', 'success');
}

function copyQuestions() {
    if (extractedQuestions.length === 0) {
        showNotification('No questions to copy', 'error');
        return;
    }
    
    const jsonData = JSON.stringify(extractedQuestions, null, 2);
    
    navigator.clipboard.writeText(jsonData).then(() => {
        showNotification('Questions copied to clipboard as JSON!', 'success');
    }).catch(err => {
        // Fallback for older browsers
        const textArea = document.createElement('textarea');
        textArea.value = jsonData;
        textArea.style.position = 'fixed';
        textArea.style.opacity = '0';
        document.body.appendChild(textArea);
        textArea.select();
        try {
            document.execCommand('copy');
            showNotification('Questions copied to clipboard as JSON!', 'success');
        } catch (err) {
            showNotification('Failed to copy. Please select and copy manually.', 'error');
        }
        document.body.removeChild(textArea);
    });
}

function clearScraper() {
    document.getElementById('htmlContent').value = '';
    document.getElementById('scraperResults').style.display = 'none';
    extractedQuestions = [];
    showNotification('Scraper cleared', 'info');
}

// ==========================================
// SOURCE STORAGE FUNCTIONS
// ==========================================

let allSources = [];
let currentSourceId = null;
let currentSourcePages = [];
let currentPageIndex = null;

// Load sources when page is shown (SERVER ONLY - no localStorage)
async function loadSources() {
    console.log('Loading sources from server...');
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/sources/list.php`);
        const data = await response.json();
        
        if (data.success && data.sources) {
            allSources = data.sources;
            console.log('✅ Loaded sources from server:', allSources.length);
        } else {
            allSources = [];
            console.log('No sources found on server');
        }
    } catch (error) {
        console.error('Error loading from server:', error);
        allSources = [];
        showNotification('Failed to load sources from server', 'error');
    }
    
    renderSourcesGrid();
}

function renderSourcesGrid() {
    const grid = document.getElementById('sourcesGrid');
    const emptyState = document.getElementById('sourcesEmptyState');
    
    if (!grid) return;
    
    if (allSources.length === 0) {
        grid.style.display = 'none';
        emptyState.style.display = 'block';
        return;
    }
    
    grid.style.display = 'grid';
    emptyState.style.display = 'none';
    
    grid.innerHTML = allSources.map(source => {
        // Use page_count from server API, or fallback to pages.length
        const pageCount = source.page_count || (source.pages ? source.pages.length : 0);
        const createdDate = source.created_at ? (parseUtcDateTime(source.created_at)?.toLocaleDateString('en-IN', { timeZone: 'Asia/Kolkata' }) || 'N/A') : 'N/A';
        
        return `
            <div class="source-card" style="
                background: white;
                border-radius: 16px;
                box-shadow: 0 2px 15px rgba(0,0,0,0.08);
                overflow: hidden;
                transition: transform 0.2s, box-shadow 0.2s;
                cursor: pointer;
            " onclick="openSource(${source.id})" onmouseover="this.style.transform='translateY(-3px)';this.style.boxShadow='0 8px 25px rgba(0,0,0,0.15)'" onmouseout="this.style.transform='none';this.style.boxShadow='0 2px 15px rgba(0,0,0,0.08)'">
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; color: white;">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                        <div style="flex: 1;">
                            <h3 style="margin: 0; font-size: 18px; color: white;">${source.name}</h3>
                            <p style="margin: 8px 0 0 0; opacity: 0.8; font-size: 13px;">${createdDate}</p>
                        </div>
                        <div style="background: rgba(255,255,255,0.2); padding: 8px 15px; border-radius: 20px;">
                            <span style="font-weight: bold; font-size: 18px;">${pageCount}</span>
                            <span style="font-size: 12px; opacity: 0.9;"> pages</span>
                        </div>
                    </div>
                </div>
                <div style="padding: 15px; display: flex; justify-content: space-between; align-items: center; gap: 10px;">
                    <span style="color: #666; font-size: 13px;"><i class="fas fa-folder-open"></i> Click to open</span>
                    <div style="display: flex; gap: 8px;">
                        <button onclick="event.stopPropagation(); scrapeAllPages(${source.id}, '${source.name.replace(/'/g, "\\'")}')" style="
                            background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
                            color: white;
                            border: none;
                            padding: 8px 12px;
                            border-radius: 20px;
                            cursor: pointer;
                            font-size: 11px;
                            font-weight: 600;
                        " title="Extract questions from all pages"><i class="fas fa-magic"></i> Scrape All</button>
                        <button onclick="event.stopPropagation(); deleteSource(${source.id})" style="
                            background: #ff5252;
                            color: white;
                            border: none;
                            padding: 8px 12px;
                            border-radius: 20px;
                            cursor: pointer;
                            font-size: 11px;
                        "><i class="fas fa-trash"></i></button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function openAddSourceModal() {
    document.getElementById('newSourceName').value = '';
    openModal('addSourceModal');
}

async function createSource() {
    const name = document.getElementById('newSourceName').value.trim();
    
    if (!name) {
        showNotification('Please enter a source name', 'warning');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/sources/create.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: name })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Source created successfully!', 'success');
            closeModal('addSourceModal');
            loadSources();
        } else {
            throw new Error(data.message || 'Failed to create source');
        }
    } catch (error) {
        console.error('Error creating source:', error);
        showNotification('Failed to create source: ' + error.message, 'error');
    }
}

async function openSource(sourceId) {
    currentSourceId = sourceId;
    const source = allSources.find(s => s.id === sourceId);
    
    if (!source) {
        showNotification('Source not found', 'error');
        return;
    }
    
    console.log('Opening source:', sourceId, source.name);
    
    // Load pages from SERVER ONLY
    try {
        const response = await fetch(`${API_BASE_URL}/admin/sources/get_pages.php?source_id=${sourceId}`);
        const data = await response.json();
        
        if (data.success && data.pages && data.pages.length > 0) {
            currentSourcePages = data.pages;
            console.log('✅ Loaded pages from server:', currentSourcePages.length);
        } else {
            currentSourcePages = [];
            console.log('No pages found on server');
        }
    } catch (error) {
        console.error('Error loading pages from server:', error);
        currentSourcePages = [];
        showNotification('Failed to load pages from server', 'error');
    }
    
    document.getElementById('viewSourceTitle').textContent = source.name;
    document.getElementById('viewSourcePageCount').textContent = `${currentSourcePages.length} Pages`;
    
    renderSourcePagesList();
    
    // Reset editor
    currentPageIndex = null;
    document.getElementById('sourceEditorPlaceholder').style.display = 'flex';
    document.getElementById('sourceEditorContainer').style.display = 'none';
    
    openModal('viewSourceModal');
}

function renderSourcePagesList() {
    const container = document.getElementById('sourcePagesList');
    
    if (currentSourcePages.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; padding: 30px; color: #999;">
                <i class="fas fa-file-alt" style="font-size: 32px; margin-bottom: 10px; color: #ddd;"></i>
                <p style="margin: 0; font-size: 13px;">No pages yet</p>
                <p style="margin: 5px 0 0 0; font-size: 12px;">Click "Add Page" to start</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = currentSourcePages.map((page, index) => `
        <div onclick="selectSourcePage(${index})" style="
            padding: 12px 15px;
            margin-bottom: 8px;
            background: ${currentPageIndex === index ? '#667eea' : 'white'};
            color: ${currentPageIndex === index ? 'white' : '#333'};
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.2s;
            border: 1px solid ${currentPageIndex === index ? '#667eea' : '#e0e0e0'};
        " onmouseover="if(${currentPageIndex !== index}) this.style.background='#f0f0f0'" onmouseout="if(${currentPageIndex !== index}) this.style.background='white'">
            <div style="font-weight: 600; font-size: 14px;">
                <i class="fas fa-file-code"></i> Page ${index + 1}
            </div>
            <div style="font-size: 12px; opacity: 0.8; margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                ${page.title || 'Untitled'}
            </div>
        </div>
    `).join('');
    
    document.getElementById('viewSourcePageCount').textContent = `${currentSourcePages.length} Pages`;
}

function addSourcePage() {
    const newPage = {
        id: Date.now(),
        title: `Page ${currentSourcePages.length + 1}`,
        content: '',
        created_at: new Date().toISOString()
    };
    
    currentSourcePages.push(newPage);
    renderSourcePagesList();
    selectSourcePage(currentSourcePages.length - 1);
    showNotification('New page added', 'success');
}

function selectSourcePage(index) {
    currentPageIndex = index;
    const page = currentSourcePages[index];
    
    if (!page) return;
    
    document.getElementById('sourceEditorPlaceholder').style.display = 'none';
    document.getElementById('sourceEditorContainer').style.display = 'flex';
    
    document.getElementById('currentPageNumber').textContent = index + 1;
    document.getElementById('currentPageTitle').value = page.title || '';
    document.getElementById('sourceHtmlEditor').value = page.content || '';
    
    renderSourcePagesList();
}

// NOTE: All storage is now SERVER-ONLY (no localStorage)

// Import JSON file with pages (from fetched data) - SERVER ONLY, NO LOCALSTORAGE
function importSourceFromJSON() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    input.onchange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;
        
        try {
            showNotification('📖 Reading JSON file...', 'info');
            const text = await file.text();
            const data = JSON.parse(text);
            
            // Validate structure
            if (!data.pages || !Array.isArray(data.pages)) {
                showNotification('Invalid JSON format. Must have "pages" array.', 'error');
                return;
            }
            
            // Get source name
            const sourceName = data.source || data.name || file.name.replace('.json', '');
            const totalPages = data.pages.length;
            
            console.log(`Importing source "${sourceName}" with ${totalPages} pages (SERVER ONLY)`);
            showNotification(`📤 Uploading ${totalPages} pages to server...`, 'info');
            
            // Step 1: Create source on server
            console.log('📤 Step 1: Creating source on server...');
            const createResponse = await fetch(`${API_BASE_URL}/admin/sources/create.php`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: sourceName })
            });
            
            console.log('Create response status:', createResponse.status);
            const createText = await createResponse.text();
            console.log('Create response raw:', createText);
            
            let createData;
            try {
                createData = JSON.parse(createText);
            } catch (e) {
                console.error('Failed to parse create response:', e);
                showNotification('Server error: Invalid response from create API', 'error');
                return;
            }
            
            console.log('Create response parsed:', createData);
            
            if (!createData.success || (!createData.source_id && !createData.id)) {
                showNotification('Failed to create source: ' + (createData.message || 'Unknown error'), 'error');
                return;
            }
            
            // Accept either source_id or id
            createData.source_id = createData.source_id || createData.id;
            
            const sourceId = createData.source_id;
            console.log('Created source on server with ID:', sourceId);
            
            // Step 2: Prepare pages for server
            const pages = data.pages.map((p, idx) => ({
                title: p.title || `Page ${p.page_order || p.pageNumber || idx + 1}`,
                content: p.content || '',
                page_order: p.page_order || p.pageNumber || idx + 1
            }));
            
            // Step 3: Save pages to server (one at a time to avoid size limits)
            let savedCount = 0;
            let failedPages = [];
            
            for (let i = 0; i < pages.length; i++) {
                const page = pages[i];
                showNotification(`📤 Saving page ${i + 1}/${pages.length}...`, 'info');
                
                try {
                    // Encode content as base64 to bypass WAF/firewall
                    const contentB64 = btoa(unescape(encodeURIComponent(page.content || '')));
                    
                    const pageResponse = await fetch(`${API_BASE_URL}/admin/sources/save_page.php`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            source_id: sourceId,
                            title: page.title,
                            content_b64: contentB64,  // Base64 encoded
                            page_order: page.page_order
                        })
                    });
                    const pageData = await pageResponse.json();
                    
                    if (pageData.success) {
                        savedCount++;
                        console.log(`✅ Page ${i + 1} saved`);
                    } else {
                        failedPages.push(i + 1);
                        console.error(`❌ Page ${i + 1} failed:`, pageData.message);
                    }
                } catch (err) {
                    failedPages.push(i + 1);
                    console.error(`❌ Page ${i + 1} error:`, err);
                }
            }
            
            if (savedCount === pages.length) {
                showNotification(`✅ Imported "${sourceName}" with ${savedCount} pages to SERVER!`, 'success');
                console.log('✅ All pages saved to server database');
            } else {
                showNotification(`⚠️ Imported ${savedCount}/${pages.length} pages. Failed: ${failedPages.join(', ')}`, 'warning');
            }
            loadSources(); // Reload sources from server
            
        } catch (error) {
            console.error('Import error:', error);
            showNotification('Error: ' + error.message, 'error');
        }
    };
    input.click();
}

window.importSourceFromJSON = importSourceFromJSON;

// Open Fetch from URL modal
function openFetchUrlModal() {
    document.getElementById('fetchSourceName').value = '';
    document.getElementById('fetchBaseUrl').value = '';
    document.getElementById('fetchTotalPages').value = '20';
    document.getElementById('fetchEmail').value = '';
    document.getElementById('fetchPassword').value = '';
    document.getElementById('fetchProgress').style.display = 'none';
    document.getElementById('generatedScriptSection').style.display = 'none';
    document.getElementById('fetchStartBtn').disabled = false;
    openModal('fetchUrlModal');
}

/**
 * Generate F12 Console Script for fetching pages
 * This generates a script that the user can run in their browser console
 */
function generateF12Script() {
    const sourceName = document.getElementById('fetchSourceName').value.trim() || 'Source';
    const baseUrl = document.getElementById('fetchBaseUrl').value.trim();
    const totalPages = parseInt(document.getElementById('fetchTotalPages').value) || 20;
    
    if (!baseUrl) {
        showNotification('Please enter a Page URL first', 'warning');
        return;
    }
    
    // Detect URL pattern and generate appropriate script
    let script = '';
    
    // Check if URL matches pattern like "...-1.php" or "...-2.php" etc.
    const phpFileMatch = baseUrl.match(/^(.+)-(\d+)\.php/);
    
    if (phpFileMatch) {
        // Pattern: Group-4-2022-july-1.php, Group-4-2022-july-2.php, etc.
        const baseUrlPart = phpFileMatch[1];
        const safeSourceName = sourceName.replace(/[^a-zA-Z0-9-_]/g, '-');
        
        script = `// ═══════════════════════════════════════════════════════════════
// 📥 Fetch ${totalPages} Pages: ${sourceName}
// Generated by TNPSC Mock Test Admin
// ═══════════════════════════════════════════════════════════════

(async function() {
    const pages = [];
    const baseUrl = '${baseUrlPart}-';
    const totalPages = ${totalPages};
    
    console.log('🚀 Starting fetch of ' + totalPages + ' pages...');
    
    for (let i = 1; i <= totalPages; i++) {
        const url = baseUrl + i + '.php';
        console.log('📄 Fetching page ' + i + '/' + totalPages + '...');
        
        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error('HTTP ' + response.status);
            const html = await response.text();
            pages.push({ page_order: i, content: html });
            console.log('✅ Page ' + i + ' done');
        } catch (err) {
            console.error('❌ Page ' + i + ' failed:', err.message);
        }
        
        // Small delay between requests
        await new Promise(r => setTimeout(r, 500));
    }
    
    console.log('📦 Creating download file...');
    
    const data = {
        source: '${sourceName}',
        pages: pages
    };
    
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = '${safeSourceName}-pages.json';
    a.click();
    
    console.log('');
    console.log('════════════════════════════════════════════');
    console.log('✅ SUCCESS! Downloaded ' + pages.length + ' pages');
    console.log('📁 File: ${safeSourceName}-pages.json');
    console.log('👉 Now go to Admin Panel → Source Storage');
    console.log('👉 Click "Import JSON" and select the file');
    console.log('════════════════════════════════════════════');
})();`;
    } else {
        // Pattern: ?page=1, ?page=2, etc.
        const safeSourceName = sourceName.replace(/[^a-zA-Z0-9-_]/g, '-');
        const separator = baseUrl.includes('?') ? '&' : '?';
        
        script = `// ═══════════════════════════════════════════════════════════════
// 📥 Fetch ${totalPages} Pages: ${sourceName}
// Generated by TNPSC Mock Test Admin
// ═══════════════════════════════════════════════════════════════

(async function() {
    const pages = [];
    const baseUrl = '${baseUrl}';
    const totalPages = ${totalPages};
    
    console.log('🚀 Starting fetch of ' + totalPages + ' pages...');
    
    for (let i = 1; i <= totalPages; i++) {
        const url = baseUrl + '${separator}page=' + i;
        console.log('📄 Fetching page ' + i + '/' + totalPages + '...');
        
        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error('HTTP ' + response.status);
            const html = await response.text();
            pages.push({ page_order: i, content: html });
            console.log('✅ Page ' + i + ' done');
        } catch (err) {
            console.error('❌ Page ' + i + ' failed:', err.message);
        }
        
        // Small delay between requests
        await new Promise(r => setTimeout(r, 500));
    }
    
    console.log('📦 Creating download file...');
    
    const data = {
        source: '${sourceName}',
        pages: pages
    };
    
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = '${safeSourceName}-pages.json';
    a.click();
    
    console.log('');
    console.log('════════════════════════════════════════════');
    console.log('✅ SUCCESS! Downloaded ' + pages.length + ' pages');
    console.log('📁 File: ${safeSourceName}-pages.json');
    console.log('👉 Now go to Admin Panel → Source Storage');
    console.log('👉 Click "Import JSON" and select the file');
    console.log('════════════════════════════════════════════');
})();`;
    }
    
    // Show the generated script
    document.getElementById('generatedScript').value = script;
    document.getElementById('generatedScriptSection').style.display = 'block';
    
    showNotification('Script generated! Copy and run in F12 Console', 'success');
}

/**
 * Copy generated script to clipboard
 */
function copyGeneratedScript() {
    const textarea = document.getElementById('generatedScript');
    textarea.select();
    document.execCommand('copy');
    
    showNotification('Script copied to clipboard!', 'success');
}

// Make functions globally available
window.generateF12Script = generateF12Script;
window.copyGeneratedScript = copyGeneratedScript;

// Start fetching from URL
async function startFetchFromUrl() {
    const sourceName = document.getElementById('fetchSourceName').value.trim();
    const baseUrl = document.getElementById('fetchBaseUrl').value.trim();
    const totalPages = parseInt(document.getElementById('fetchTotalPages').value) || 20;
    const email = document.getElementById('fetchEmail').value.trim();
    const password = document.getElementById('fetchPassword').value;
    
    if (!sourceName || !baseUrl) {
        showNotification('Please enter source name and URL', 'warning');
        return;
    }
    
    // Show progress
    document.getElementById('fetchProgress').style.display = 'block';
    document.getElementById('fetchStartBtn').disabled = true;
    document.getElementById('fetchProgressBar').style.width = '10%';
    document.getElementById('fetchProgressBar').textContent = '10%';
    document.getElementById('fetchStatus').textContent = 'Connecting to server...';
    
    try {
        // Call server-side fetch
        const response = await fetch(`${API_BASE_URL}/admin/sources/fetch_pages.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                source_name: sourceName,
                base_url: baseUrl,
                total_pages: totalPages,
                email: email,
                password: password
            })
        });
        
        document.getElementById('fetchProgressBar').style.width = '50%';
        document.getElementById('fetchProgressBar').textContent = '50%';
        document.getElementById('fetchStatus').textContent = 'Fetching pages...';
        
        const data = await response.json();
        
        if (data.success) {
            document.getElementById('fetchProgressBar').style.width = '100%';
            document.getElementById('fetchProgressBar').textContent = '100%';
            document.getElementById('fetchStatus').textContent = `✅ ${data.message}`;
            
            showNotification(`✅ Fetched ${data.pages_fetched} pages successfully!`, 'success');
            
            // Reload sources
            setTimeout(() => {
                closeModal('fetchUrlModal');
                loadSources();
            }, 1500);
        } else {
            throw new Error(data.message || 'Fetch failed');
        }
        
    } catch (error) {
        console.error('Fetch error:', error);
        document.getElementById('fetchProgressBar').style.width = '0%';
        document.getElementById('fetchStatus').textContent = '❌ ' + error.message;
        showNotification('Error: ' + error.message, 'error');
        document.getElementById('fetchStartBtn').disabled = false;
    }
}

window.openFetchUrlModal = openFetchUrlModal;
window.startFetchFromUrl = startFetchFromUrl;

async function saveSourcePage() {
    if (currentPageIndex === null) {
        showNotification('No page selected', 'warning');
        return;
    }
    
    const title = document.getElementById('currentPageTitle').value.trim();
    const content = document.getElementById('sourceHtmlEditor').value;
    
    console.log('Saving page:', currentPageIndex + 1, 'Source ID:', currentSourceId);
    console.log('Content length:', content.length);
    
    // Update the page data in memory
    currentSourcePages[currentPageIndex].title = title || `Page ${currentPageIndex + 1}`;
    currentSourcePages[currentPageIndex].content = content;
    currentSourcePages[currentPageIndex].updated_at = new Date().toISOString();
    
    // Save to SERVER (no localStorage)
    try {
        const response = await fetch(`${API_BASE_URL}/admin/sources/save_pages.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                source_id: currentSourceId,
                pages: currentSourcePages
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            console.log('✅ Saved to server successfully');
            showNotification('Page saved to server!', 'success');
            renderSourcePagesList();
        } else {
            throw new Error(data.message || 'Save failed');
        }
    } catch (error) {
        console.error('Server save error:', error);
        showNotification('Failed to save: ' + error.message, 'error');
    }
}

function copySourceContent() {
    const content = document.getElementById('sourceHtmlEditor').value;
    
    if (!content) {
        showNotification('No content to copy', 'warning');
        return;
    }
    
    navigator.clipboard.writeText(content).then(() => {
        showNotification('HTML copied to clipboard!', 'success');
    }).catch(() => {
        // Fallback
        const textArea = document.createElement('textarea');
        textArea.value = content;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showNotification('HTML copied to clipboard!', 'success');
    });
}

async function deleteSourcePage() {
    if (currentPageIndex === null) {
        showNotification('No page selected', 'warning');
        return;
    }
    
    if (!confirm(`Delete Page ${currentPageIndex + 1}?`)) return;
    
    currentSourcePages.splice(currentPageIndex, 1);
    
    // Save to SERVER (no localStorage)
    try {
        const response = await fetch(`${API_BASE_URL}/admin/sources/save_pages.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                source_id: currentSourceId,
                pages: currentSourcePages
            })
        });
        
        const data = await response.json();
        if (!data.success) {
            throw new Error(data.message || 'Delete failed');
        }
        
        console.log('✅ Page deleted on server');
    } catch (error) {
        console.error('Server delete error:', error);
        showNotification('Failed to delete on server: ' + error.message, 'error');
    }
    
    // Reset editor
    currentPageIndex = null;
    document.getElementById('sourceEditorPlaceholder').style.display = 'flex';
    document.getElementById('sourceEditorContainer').style.display = 'none';
    
    renderSourcePagesList();
    showNotification('Page deleted', 'success');
}

async function deleteSource(sourceId) {
    if (!confirm('Delete this source and all its pages?')) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/sources/delete.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: sourceId })
        });
        
        const data = await response.json();
        if (data.success) {
            console.log('✅ Source deleted from server');
            allSources = allSources.filter(s => s.id !== sourceId);
            renderSourcesGrid();
            showNotification('Source deleted', 'success');
        } else {
            throw new Error(data.message || 'Delete failed');
        }
    } catch (error) {
        console.error('Error deleting from server:', error);
        showNotification('Failed to delete: ' + error.message, 'error');
    }
}

/**
 * Delete ALL sources from server
 */
async function deleteAllSources() {
    if (!confirm('⚠️ DELETE ALL SOURCES?\n\nThis will permanently delete ALL sources and their pages from the server.\n\nAre you sure?')) return;
    if (!confirm('⚠️ FINAL WARNING!\n\nThis action CANNOT be undone. Click OK to proceed with deletion.')) return;
    
    try {
        showNotification('Deleting all sources...', 'info');
        
        const response = await fetch(`${API_BASE_URL}/admin/sources/delete_all.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        
        const data = await response.json();
        if (data.success) {
            console.log('✅ All sources deleted:', data.deleted_count);
            allSources = [];
            renderSourcesGrid();
            showNotification(`Deleted ${data.deleted_count} sources successfully!`, 'success');
        } else {
            throw new Error(data.message || 'Delete failed');
        }
    } catch (error) {
        console.error('Error deleting all sources:', error);
        showNotification('Failed to delete: ' + error.message, 'error');
    }
}

// ==========================================
// SCRAPER TOOLS - Verify, Merge, Export JSON
// ==========================================

/**
 * Parse HTML and extract questions with their details
 * Supports multiple HTML structures:
 * 1. Rendered HTML with .qus containers (pages 1-5 style)
 * 2. AngularJS pages with JSON in script tags (pages 6+ premium style)
 */
function parseQuestionsFromHTML(html, debugPage = false) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    let questions = [];
    
    // ============================================
    // HELPER FUNCTIONS
    // ============================================
    const hasTamil = (text) => text && /[\u0B80-\u0BFF]/.test(text);
    const hasEnglish = (text) => text && /[a-zA-Z]/.test(text);
    const cleanText = (text) => (text || '').replace(/<[^>]+>/g, '').trim();
    
    // Process option - Split by <br> OR copy numeric to both
    const processOption = (optText) => {
        if (!optText || optText.trim() === '') return { en: '', ta: '' };
        
        optText = optText.trim();
        const optClean = cleanText(optText);
        
        // NUMERIC: Copy same value to BOTH _en and _ta
        if (/^[\d\s,\-\.]+$/.test(optClean)) {
            return { en: optClean, ta: optClean };
        }
        
        // Has <br> separator - SPLIT into English and Tamil
        if (optText.includes('<br')) {
            const parts = optText.split(/<br\s*\/?>/i);
            if (parts.length >= 2) {
                const p1 = cleanText(parts[0]);
                const p2 = cleanText(parts.slice(1).join(' '));
                
                const p1HasTamil = hasTamil(p1);
                const p2HasTamil = hasTamil(p2);
                
                if (!p1HasTamil && p2HasTamil) {
                    return { en: p1, ta: p2 };
                } else if (p1HasTamil && !p2HasTamil) {
                    return { en: p2, ta: p1 };
                }
            }
        }
        
        // Pure English
        if (hasEnglish(optClean) && !hasTamil(optClean)) {
            return { en: optClean, ta: '' };
        }
        
        // Pure Tamil
        if (hasTamil(optClean) && !hasEnglish(optClean)) {
            return { en: '', ta: optClean };
        }
        
        // Mixed without <br> - put in both
        return { en: optClean, ta: optClean };
    };
    
    // ============================================
    // METHOD 1: Try to extract from AngularJS JSON
    // Look for: this.qus_list = [...] in script tags
    // Uses bracket-counting for robust JSON extraction
    // ============================================
    const qusListIndex = html.indexOf('this.qus_list');
    
    if (qusListIndex !== -1) {
        if (debugPage) console.log('🔎 Found "this.qus_list" at index:', qusListIndex);
        
        // Find the opening bracket of the array
        const arrayStart = html.indexOf('[', qusListIndex);
        
        if (arrayStart !== -1) {
            // Use bracket counting to find the matching closing bracket
            let depth = 0;
            let arrayEnd = -1;
            let inString = false;
            let escapeNext = false;
            
            for (let i = arrayStart; i < html.length && i < arrayStart + 500000; i++) {
                const char = html[i];
                
                // Handle string escaping
                if (escapeNext) {
                    escapeNext = false;
                    continue;
                }
                
                if (char === '\\' && inString) {
                    escapeNext = true;
                    continue;
                }
                
                // Toggle string mode on quotes (but not escaped ones)
                if (char === '"' && !escapeNext) {
                    inString = !inString;
                    continue;
                }
                
                // Only count brackets when not inside a string
                if (!inString) {
                    if (char === '[') depth++;
                    if (char === ']') depth--;
                    
                    if (depth === 0) {
                        arrayEnd = i;
                        break;
                    }
                }
            }
            
            if (arrayEnd !== -1) {
                const jsonString = html.substring(arrayStart, arrayEnd + 1);
                if (debugPage) console.log('🔎 Extracted JSON array, length:', jsonString.length);
                
                try {
                    const jsonData = JSON.parse(jsonString);
                    if (debugPage) console.log('✅ Parsed JSON successfully, found', jsonData.length, 'questions');
                    
                    jsonData.forEach(q => {
                        // Map answer number (1-4) to letter (A-D)
                        const answerMap = { '1': 'A', '2': 'B', '3': 'C', '4': 'D' };
                        const correctAnswer = answerMap[q.qbq_ans_num] || '';
                        
                        // ═══════════════════════════════════════════════════
                        // QUESTION: NO SPLIT - Keep together in question_en
                        // ═══════════════════════════════════════════════════
                        const questionText = q.qbq_description || '';
                        
                        // OPTIONS: Split by <br> OR copy numeric to both
                        const optA = processOption(q.qbq_option_1 || '');
                        const optB = processOption(q.qbq_option_2 || '');
                        const optC = processOption(q.qbq_option_3 || '');
                        const optD = processOption(q.qbq_option_4 || '');
                        
                        if (debugPage) {
                            console.log(`Q${q.qbq_order}: Question="${questionText.substring(0,50)}..."`);
                        }
                        
                        questions.push({
                            questionNumber: parseInt(q.qbq_order) || 0,
                            question_en: questionText,  // Keep everything together
                            question_ta: '',            // Empty - no split
                            option_a_en: optA.en,
                            option_b_en: optB.en,
                            option_c_en: optC.en,
                            option_d_en: optD.en,
                            option_a_ta: optA.ta,
                            option_b_ta: optB.ta,
                            option_c_ta: optC.ta,
                            option_d_ta: optD.ta,
                            correct_answer: correctAnswer
                        });
                    });
                    
                    if (questions.length > 0) {
                        if (debugPage) console.log('✅ Extracted', questions.length, 'questions from JSON');
                        return questions;
                    }
                } catch (e) {
                    if (debugPage) console.log('⚠️ Failed to parse JSON:', e.message);
                    // Log a snippet around the error for debugging
                    if (debugPage && e.message.includes('position')) {
                        const posMatch = e.message.match(/position (\d+)/);
                        if (posMatch) {
                            const pos = parseInt(posMatch[1]);
                            console.log('⚠️ JSON error near:', jsonString.substring(Math.max(0, pos-50), pos+50));
                        }
                    }
                }
            } else {
                if (debugPage) console.log('⚠️ Could not find closing bracket for JSON array');
            }
        }
    }
    
    // ============================================
    // METHOD 2: Try DOM-based parsing (.qus containers)
    // For rendered HTML pages (pages 1-5 style)
    // ============================================
    if (debugPage) console.log('🔎 Trying DOM-based parsing...');
    
    let questionContainers = doc.querySelectorAll('.qus');
    
    if (debugPage) {
        console.log('🔎 Debug: .qus containers found:', questionContainers.length);
    }
    
    // If no .qus found, try alternative structures
    if (questionContainers.length === 0) {
        questionContainers = doc.querySelectorAll('.mt-4.mt-50.mg-bottom-30');
        if (debugPage) {
            console.log('🔎 Debug: .mt-4.mt-50 containers found:', questionContainers.length);
        }
    }
    
    // Convert NodeList to Array
    const containers = Array.from(questionContainers);
    
    containers.forEach(container => {
        try {
            // Get question number
            let qNumEl = container.querySelector('.qusnum');
            if (!qNumEl) qNumEl = container.querySelector('.text-muted.row > .qusnum');
            
            if (!qNumEl) return;
            
            const qNumText = qNumEl.textContent.trim();
            const qNum = parseInt(qNumText.replace('.', ''));
            if (isNaN(qNum) || qNum <= 0) return;
            
            // Get question text
            let qTextEl = container.querySelector('.qusdes');
            if (!qTextEl) qTextEl = container.querySelector('.text-muted.row > .qusdes');
            const questionText = qTextEl ? qTextEl.innerHTML.trim() : '';
            
            // SPLIT QUESTION TEXT INTO ENGLISH AND TAMIL
            const qText = splitEnglishTamil(questionText);
            
            // Get options
            const options = { A: '', B: '', C: '', D: '' };
            let optionContainers = container.querySelectorAll('div[onclick*="changeemoji"]');
            
            if (optionContainers.length === 0) {
                optionContainers = container.querySelectorAll('.list-unstyled div.text-muted');
            }
            
            optionContainers.forEach(optContainer => {
                const optLabel = optContainer.querySelector('.qusnum');
                const optText = optContainer.querySelector('.qusdes');
                
                if (optLabel && optText) {
                    const label = optLabel.textContent.trim().charAt(0);
                    if (['A', 'B', 'C', 'D'].includes(label)) {
                        options[label] = optText.innerHTML.trim();
                    }
                }
            });
            
            // SPLIT OPTIONS INTO ENGLISH AND TAMIL
            const optA = processOption(options.A);
            const optB = processOption(options.B);
            const optC = processOption(options.C);
            const optD = processOption(options.D);
            
            // Get correct answer
            let answerEl = container.querySelector('.answer');
            let correctAnswer = '';
            if (answerEl) {
                const answerText = answerEl.textContent || '';
                const match = answerText.match(/([A-D])\./);
                if (match) correctAnswer = match[1];
            }
            
            // Validate and add
            if (questionText && correctAnswer && (options.A || options.B || options.C || options.D)) {
                questions.push({
                    questionNumber: qNum,
                    question_en: qText.en,
                    question_ta: qText.ta,
                    option_a_en: optA.en,
                    option_b_en: optB.en,
                    option_c_en: optC.en,
                    option_d_en: optD.en,
                    option_a_ta: optA.ta,
                    option_b_ta: optB.ta,
                    option_c_ta: optC.ta,
                    option_d_ta: optD.ta,
                    correct_answer: correctAnswer
                });
            }
        } catch (error) {
            console.error('Error parsing question:', error);
        }
    });
    
    if (debugPage) {
        console.log('🔎 DOM parsing found:', questions.length, 'questions');
    }
    
    return questions;
}

/**
 * Debug function to analyze page HTML structure
 */
function debugPageStructure(pageIndex) {
    if (!currentSourcePages[pageIndex]) {
        console.log('Page not found');
        return;
    }
    
    const html = currentSourcePages[pageIndex].content || '';
    console.log(`\n========== DEBUG PAGE ${pageIndex + 1} ==========`);
    console.log('Content length:', html.length);
    
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    
    // Check for .qus
    const qus = doc.querySelectorAll('.qus');
    console.log('.qus containers:', qus.length);
    
    // Check for .qusnum
    const qusnum = doc.querySelectorAll('.qusnum');
    console.log('.qusnum elements:', qusnum.length);
    if (qusnum.length > 0) {
        console.log('First 3 qusnum values:', Array.from(qusnum).slice(0, 3).map(el => el.textContent.trim()));
    }
    
    // Check for .qusdes
    const qusdes = doc.querySelectorAll('.qusdes');
    console.log('.qusdes elements:', qusdes.length);
    
    // Check for .answer
    const answer = doc.querySelectorAll('.answer');
    console.log('.answer elements:', answer.length);
    
    // Show unique class names
    const classes = new Set();
    doc.querySelectorAll('*').forEach(el => el.classList.forEach(c => classes.add(c)));
    console.log('Unique classes (first 30):', Array.from(classes).slice(0, 30));
    
    // Try parsing with debug
    console.log('\nParsing with debug:');
    parseQuestionsFromHTML(html, true);
}

/**
 * Verify Questions 1-200 across all pages
 */
function verifyQuestions() {
    if (currentSourcePages.length === 0) {
        showNotification('No pages to verify. Add pages first.', 'warning');
        return;
    }
    
    console.log('🔍 Verifying questions across', currentSourcePages.length, 'pages...');
    
    // Parse all pages
    const allQuestions = [];
    const questionMap = new Map(); // questionNumber -> count (for duplicates)
    const pageStats = []; // Track stats per page
    
    currentSourcePages.forEach((page, pageIndex) => {
        const contentLength = (page.content || '').length;
        console.log(`📄 Page ${pageIndex + 1}: Content length = ${contentLength} chars`);
        
        if (contentLength < 100) {
            console.warn(`⚠️ Page ${pageIndex + 1} appears to be empty or very short!`);
            pageStats.push({ page: pageIndex + 1, questions: 0, contentLength, status: 'empty' });
            return;
        }
        
        const pageQuestions = parseQuestionsFromHTML(page.content || '');
        console.log(`✅ Page ${pageIndex + 1}: Found ${pageQuestions.length} questions (Q${pageQuestions.map(q => q.questionNumber).join(', Q')})`);
        
        pageStats.push({ 
            page: pageIndex + 1, 
            questions: pageQuestions.length, 
            contentLength,
            qNumbers: pageQuestions.map(q => q.questionNumber)
        });
        
        pageQuestions.forEach(q => {
            allQuestions.push({ ...q, page: pageIndex + 1 });
            const count = questionMap.get(q.questionNumber) || 0;
            questionMap.set(q.questionNumber, count + 1);
        });
    });
    
    // Log summary
    console.log('📊 Page Stats:', pageStats);
    console.log('📊 Pages with content:', pageStats.filter(p => p.questions > 0).length);
    console.log('📊 Pages without questions:', pageStats.filter(p => p.questions === 0).map(p => p.page));
    
    // Analyze results
    const found = new Set(allQuestions.map(q => q.questionNumber));
    const missing = [];
    const duplicates = [];
    
    for (let i = 1; i <= 200; i++) {
        if (!found.has(i)) {
            missing.push(i);
        }
    }
    
    questionMap.forEach((count, qNum) => {
        if (count > 1) {
            duplicates.push({ number: qNum, count });
        }
    });
    
    // Render results
    document.getElementById('verifyTotal').textContent = found.size;
    document.getElementById('verifyMissing').textContent = missing.length;
    document.getElementById('verifyDuplicates').textContent = duplicates.length;
    
    // Render grid
    const grid = document.getElementById('verificationGrid');
    let gridHtml = '';
    
    for (let i = 1; i <= 200; i++) {
        let bgColor = '#4CAF50'; // Found - green
        let textColor = 'white';
        
        if (!found.has(i)) {
            bgColor = '#f44336'; // Missing - red
        } else if (questionMap.get(i) > 1) {
            bgColor = '#ff9800'; // Duplicate - orange
        }
        
        gridHtml += `<div style="
            background: ${bgColor};
            color: ${textColor};
            padding: 5px;
            text-align: center;
            border-radius: 4px;
            font-size: 11px;
            font-weight: 600;
            cursor: pointer;
        " title="Q${i}">${i}</div>`;
    }
    grid.innerHTML = gridHtml;
    
    // Show missing questions section if any
    const missingSection = document.getElementById('missingQuestionsSection');
    const missingList = document.getElementById('missingQuestionsList');
    
    if (missing.length > 0) {
        missingSection.style.display = 'block';
        missingList.innerHTML = `<strong>Missing:</strong> Q${missing.join(', Q')}`;
    } else {
        missingSection.style.display = 'none';
    }
    
    openModal('verificationModal');
    
    console.log('✅ Verification complete:', {
        total: found.size,
        missing: missing.length,
        duplicates: duplicates.length
    });
}

/**
 * Merge all pages into single HTML
 */
function mergeAllPages() {
    if (currentSourcePages.length === 0) {
        showNotification('No pages to merge. Add pages first.', 'warning');
        return;
    }
    
    console.log('📋 Merging', currentSourcePages.length, 'pages...');
    
    // Combine all page content
    let mergedHtml = `<!-- Merged HTML from ${currentSourcePages.length} pages -->\n`;
    mergedHtml += `<!-- Source: ${document.getElementById('viewSourceTitle').textContent} -->\n`;
    mergedHtml += `<!-- Generated: ${new Date().toLocaleString()} -->\n\n`;
    
    currentSourcePages.forEach((page, index) => {
        mergedHtml += `\n<!-- ========== PAGE ${index + 1} ========== -->\n`;
        mergedHtml += page.content || '';
        mergedHtml += `\n<!-- ========== END PAGE ${index + 1} ========== -->\n`;
    });
    
    // Display in modal
    document.getElementById('mergedPagesCount').textContent = currentSourcePages.length;
    document.getElementById('mergedHtmlContent').value = mergedHtml;
    
    openModal('mergedHtmlModal');
    
    console.log('✅ Merged HTML ready:', mergedHtml.length, 'characters');
}

/**
 * Copy merged HTML to clipboard
 */
function copyMergedHtml() {
    const content = document.getElementById('mergedHtmlContent').value;
    navigator.clipboard.writeText(content).then(() => {
        showNotification('Merged HTML copied to clipboard!', 'success');
    }).catch(() => {
        // Fallback
        const textArea = document.createElement('textarea');
        textArea.value = content;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showNotification('Merged HTML copied to clipboard!', 'success');
    });
}

/**
 * Download merged HTML as file
 */
function downloadMergedHtml() {
    const content = document.getElementById('mergedHtmlContent').value;
    const sourceName = document.getElementById('viewSourceTitle').textContent.replace(/[^a-zA-Z0-9]/g, '_');
    const filename = `merged_${sourceName}_${Date.now()}.html`;
    
    const blob = new Blob([content], { type: 'text/html' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    
    showNotification('HTML file downloaded!', 'success');
}

/**
 * Parse all pages and export as JSON compatible with Question Session
 */
function parseAndExportJSON() {
    if (currentSourcePages.length === 0) {
        showNotification('No pages to parse. Add pages first.', 'warning');
        return;
    }
    
    console.log('📤 Parsing and exporting JSON...');
    
    // Parse all pages
    const allQuestions = [];
    const seen = new Set();
    
    currentSourcePages.forEach((page, pageIndex) => {
        const pageQuestions = parseQuestionsFromHTML(page.content || '');
        
        pageQuestions.forEach(q => {
            // Skip duplicates (keep first occurrence)
            if (seen.has(q.questionNumber)) return;
            seen.add(q.questionNumber);
            
            allQuestions.push({
                question_en: null,
                question_ta: q.question_ta,
                option_a_en: null,
                option_a_ta: q.option_a_ta,
                option_b_en: null,
                option_b_ta: q.option_b_ta,
                option_c_en: null,
                option_c_ta: q.option_c_ta,
                option_d_en: null,
                option_d_ta: q.option_d_ta,
                correct_answer: q.correct_answer,
                explanation_en: null,
                explanation_ta: null,
                display_order: q.questionNumber - 1
            });
        });
    });
    
    // Sort by question number
    allQuestions.sort((a, b) => a.display_order - b.display_order);
    
    // Create export object
    const exportData = {
        source: document.getElementById('viewSourceTitle').textContent,
        exported_at: new Date().toISOString(),
        total_questions: allQuestions.length,
        questions: allQuestions
    };
    
    // Format JSON with indentation
    const jsonString = JSON.stringify(exportData, null, 2);
    
    // Display in modal
    document.getElementById('exportedQuestionsCount').textContent = allQuestions.length;
    document.getElementById('exportedJsonContent').value = jsonString;
    
    openModal('exportJsonModal');
    
    console.log('✅ JSON export ready:', allQuestions.length, 'questions');
}

/**
 * Copy exported JSON to clipboard
 */
function copyExportedJson() {
    const content = document.getElementById('exportedJsonContent').value;
    navigator.clipboard.writeText(content).then(() => {
        showNotification('JSON copied to clipboard!', 'success');
    }).catch(() => {
        // Fallback
        const textArea = document.createElement('textarea');
        textArea.value = content;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showNotification('JSON copied to clipboard!', 'success');
    });
}

/**
 * Download exported JSON as file
 */
function downloadExportedJson() {
    const content = document.getElementById('exportedJsonContent').value;
    const sourceName = document.getElementById('viewSourceTitle').textContent.replace(/[^a-zA-Z0-9]/g, '_');
    const filename = `questions_${sourceName}_${Date.now()}.json`;
    
    const blob = new Blob([content], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    
    showNotification('JSON file downloaded!', 'success');
}

/**
 * Scrape all pages from a source and extract questions in bulk
 */
async function scrapeAllPages(sourceId, sourceName) {
    console.log(`🚀 Scrape All Pages: Source ID=${sourceId}, Name="${sourceName}"`);
    
    // Show progress modal
    const progressHtml = `
        <div id="scrapeProgressModal" style="
            position: fixed; top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0,0,0,0.7); z-index: 10000;
            display: flex; align-items: center; justify-content: center;
        ">
            <div style="
                background: white; border-radius: 16px; padding: 30px;
                max-width: 500px; width: 90%; text-align: center;
            ">
                <h3 style="margin: 0 0 20px 0; color: #333;">
                    <i class="fas fa-magic" style="color: #00d4ff;"></i> Scraping All Pages
                </h3>
                <p style="color: #666; margin-bottom: 15px;">${sourceName}</p>
                <div style="background: #e0e0e0; border-radius: 10px; height: 20px; overflow: hidden; margin-bottom: 10px;">
                    <div id="scrapeProgressBar" style="
                        width: 0%; height: 100%;
                        background: linear-gradient(90deg, #00d4ff 0%, #0099cc 100%);
                        transition: width 0.3s;
                    "></div>
                </div>
                <p id="scrapeProgressText" style="margin: 0; color: #888; font-size: 13px;">Loading pages...</p>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', progressHtml);
    
    const updateProgress = (percent, text) => {
        document.getElementById('scrapeProgressBar').style.width = `${percent}%`;
        document.getElementById('scrapeProgressText').textContent = text;
    };
    
    try {
        let pages = [];
        
        // Load pages from SERVER ONLY (no localStorage)
        updateProgress(5, 'Loading pages from server...');
        const response = await fetch(`${API_BASE_URL}/admin/sources/get_pages.php?source_id=${sourceId}`);
        const data = await response.json();
        
        if (data.success && data.pages && data.pages.length > 0) {
            pages = data.pages;
            console.log(`📄 Loaded ${pages.length} pages from SERVER`);
        }
        
        if (pages.length === 0) {
            throw new Error('No pages found on server. Use "Fetch from URL" to import pages first.');
        }
        
        updateProgress(10, `Found ${pages.length} pages. Starting extraction...`);
        
        // 2. Extract questions from each page
        const allQuestions = [];
        let totalExtracted = 0;
        
        for (let i = 0; i < pages.length; i++) {
            const page = pages[i];
            const pageNumber = page.page_order || (i + 1);
            const percent = 10 + ((i + 1) / pages.length) * 80;
            updateProgress(percent, `Processing page ${i + 1}/${pages.length}...`);
            
            const questions = extractQuestionsFromHtml(page.content, pageNumber);
            console.log(`   Page ${pageNumber}: ${questions.length} questions`);
            
            allQuestions.push(...questions);
            totalExtracted += questions.length;
            
            // Small delay to prevent UI freeze
            await new Promise(r => setTimeout(r, 50));
        }
        
        console.log(`✅ Total extracted: ${allQuestions.length} questions`);
        updateProgress(95, `Extracted ${allQuestions.length} questions. Preparing JSON...`);
        
        // 3. Format the output JSON (matching Question Scraper / Upload format exactly)
        const outputJson = {
            source: sourceName,
            totalQuestions: allQuestions.length,
            extractedAt: new Date().toISOString(),
            questions: allQuestions.map((q, idx) => ({
                questionNumber: q.questionNumber || (idx + 1),
                question_en: q.question_en || '',
                question_ta: q.question_ta || '',
                options: {
                    A: q.options?.A || '',
                    B: q.options?.B || '',
                    C: q.options?.C || '',
                    D: q.options?.D || ''
                },
                options_ta: {
                    A: q.options_ta?.A || '',
                    B: q.options_ta?.B || '',
                    C: q.options_ta?.C || '',
                    D: q.options_ta?.D || ''
                },
                correctAnswer: q.correctAnswer || '',
                // Table in column_A/column_B format (matches upload format)
                table: q.table || null,
                hasTable: q.hasTable || false,
                // Image URL (will be downloaded via proxy during upload)
                imageUrl: q.imageUrl || '',
                hasImage: q.hasImage || false
            }))
        };
        
        updateProgress(100, 'Done!');
        
        // Close progress modal
        document.getElementById('scrapeProgressModal').remove();
        
        // Show result in a modal
        showScrapedQuestionsResult(outputJson, sourceName);
        
    } catch (error) {
        console.error('Scrape error:', error);
        document.getElementById('scrapeProgressModal')?.remove();
        showNotification(`Error: ${error.message}`, 'error');
    }
}

/**
 * Extract questions from HTML content (reusable parser)
 * Matches the Question Scraper Tool format exactly
 */
function extractQuestionsFromHtml(htmlContent, pageNumber = 1) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(htmlContent, 'text/html');
    const questions = [];
    
    // Helper functions
    const hasTamilChars = (text) => /[\u0B80-\u0BFF]/.test(text);
    const hasEnglishChars = (text) => /[a-zA-Z]/.test(text);
    const cleanText = (text) => (text || '').replace(/<[^>]+>/g, '').trim();
    
    // Find question containers
    let questionDivs = doc.querySelectorAll('.qus');
    if (questionDivs.length === 0) {
        questionDivs = doc.querySelectorAll('[class*="qus"]');
    }
    
    questionDivs.forEach((qDiv, index) => {
        try {
            // Extract question number
            const qusNum = qDiv.querySelector('.qusnum')?.textContent.trim() || `${index + 1}.`;
            const qusNumClean = qusNum.replace(/\.$/, '').replace(/[^\d]/g, '').trim();
            
            // Extract question text (preserving HTML for tables/images)
            const qusDes = qDiv.querySelector('.qusdes');
            let questionHtml = qusDes ? qusDes.innerHTML.trim() : '';
            
            // ═══════════════════════════════════════════════════════════════
            // QUESTION: NO SPLIT - Keep English + Tamil together with spacing
            // Store everything in question_en, leave question_ta empty
            // ═══════════════════════════════════════════════════════════════
            let question_en = questionHtml;  // Keep as-is with all content
            let question_ta = '';            // Empty - no split
            
            // ═══════════════════════════════════════════════════════════════
            // OPTIONS: Split by <br> OR copy numeric to both
            // ═══════════════════════════════════════════════════════════════
            const options = { A: '', B: '', C: '', D: '' };
            const options_ta = { A: '', B: '', C: '', D: '' };
            const optionDivs = qDiv.querySelectorAll('.list-unstyled .mt-2.col-12.p-0');
            
            optionDivs.forEach((optDiv, optIndex) => {
                if (optIndex > 3) return; // Only A, B, C, D
                const optLabel = String.fromCharCode(65 + optIndex);
                let optText = optDiv.querySelector('.qusdes')?.innerHTML.trim() || '';
                
                // Remove <p> tags if present
                optText = optText.replace(/<\/?p>/gi, '').trim();
                
                if (optText) {
                    const optTextClean = cleanText(optText);
                    const optHasTamil = hasTamilChars(optTextClean);
                    const optHasEnglish = hasEnglishChars(optTextClean);
                    
                    // Check if numeric (like "1, 2, 3, 4" or "2, 4, 3, 5, 1")
                    const isNumeric = /^[\d\s\.,\-]+$/.test(optTextClean);
                    
                    if (isNumeric) {
                        // NUMERIC: Copy same value to BOTH _en and _ta
                        options[optLabel] = optTextClean;
                        options_ta[optLabel] = optTextClean;
                    } else if (optText.includes('<br')) {
                        // Has <br> separator - SPLIT into English and Tamil
                        const parts = optText.split(/<br\s*\/?>/i);
                        if (parts.length >= 2) {
                            const p1 = cleanText(parts[0]);
                            const p2 = cleanText(parts.slice(1).join(' '));
                            
                            const p1HasTamil = hasTamilChars(p1);
                            const p2HasTamil = hasTamilChars(p2);
                            
                            if (!p1HasTamil && p2HasTamil) {
                                // English first, Tamil second
                                options[optLabel] = p1;
                                options_ta[optLabel] = p2;
                            } else if (p1HasTamil && !p2HasTamil) {
                                // Tamil first, English second
                                options_ta[optLabel] = p1;
                                options[optLabel] = p2;
                            } else {
                                // Can't determine - put cleaned text in both
                                options[optLabel] = optTextClean;
                                options_ta[optLabel] = optTextClean;
                            }
                        } else {
                            options[optLabel] = optTextClean;
                            options_ta[optLabel] = optTextClean;
                        }
                    } else if (optHasTamil && !optHasEnglish) {
                        // Pure Tamil option
                        options[optLabel] = '';
                        options_ta[optLabel] = optTextClean;
                    } else if (!optHasTamil && optHasEnglish) {
                        // Pure English option
                        options[optLabel] = optTextClean;
                        options_ta[optLabel] = '';
                    } else {
                        // Mixed without <br> - put in both
                        options[optLabel] = optTextClean;
                        options_ta[optLabel] = optTextClean;
                    }
                }
            });
            
            // Extract answer
            const ansDiv = qDiv.querySelector('.answer, .ans-div, [class*="ans"]');
            let correctAnswer = '';
            if (ansDiv) {
                const ansText = ansDiv.textContent.trim();
                const match = ansText.match(/([A-D])/);
                if (match) correctAnswer = match[1];
            }
            
            // ========== TABLE EXTRACTION ==========
            // Convert HTML table to column_A/column_B format for upload
            const tables = qDiv.querySelectorAll('table');
            let hasTable = tables.length > 0;
            let table = null;
            let tableHtml = null;
            
            if (hasTable && tables.length > 0) {
                tableHtml = tables[0].outerHTML;
                
                // Parse table into column_A / column_B format
                const tableEl = tables[0];
                const rows = tableEl.querySelectorAll('tr');
                const column_A = [];
                const column_B = [];
                
                rows.forEach((row, rowIdx) => {
                    const cells = row.querySelectorAll('td, th');
                    if (cells.length >= 2) {
                        const cellA = cells[0].innerHTML.trim();
                        const cellB = cells[1].innerHTML.trim();
                        
                        // Skip header rows with "பட்டியல்" or "List"
                        if (cellA.includes('பட்டியல்') || cellA.includes('List') || 
                            cellB.includes('பட்டியல்') || cellB.includes('List')) {
                            return;
                        }
                        
                        column_A.push({ index: column_A.length, value: cellA });
                        column_B.push({ index: column_B.length, value: cellB });
                    }
                });
                
                if (column_A.length > 0 || column_B.length > 0) {
                    table = { column_A, column_B };
                }
            }
            
            // ========== IMAGE EXTRACTION ==========
            // Get image URL (will be processed by proxy during upload)
            const images = qDiv.querySelectorAll('img');
            let hasImage = images.length > 0;
            let imageUrl = '';
            
            if (hasImage && images.length > 0) {
                imageUrl = images[0].getAttribute('src') || images[0].src || '';
                // Convert relative URLs to absolute
                if (imageUrl && !imageUrl.startsWith('http')) {
                    // Try to make absolute from civilserviceaspirants.in
                    if (imageUrl.startsWith('/')) {
                        imageUrl = 'https://civilserviceaspirants.in' + imageUrl;
                    } else {
                        imageUrl = 'https://civilserviceaspirants.in/' + imageUrl;
                    }
                }
            }
            
            if (questionHtml || Object.values(options).some(v => v) || Object.values(options_ta).some(v => v)) {
                // Calculate GLOBAL question number: (page-1)*10 + localNumber
                // e.g., Page 2, Q3 = (2-1)*10 + 3 = 13
                const localQNum = parseInt(qusNumClean) || (index + 1);
                const globalQNum = ((pageNumber - 1) * 10) + localQNum;
                
                questions.push({
                    questionNumber: globalQNum,
                    question_en,
                    question_ta,
                    options,       // { A: '', B: '', C: '', D: '' }
                    options_ta,    // { A: '', B: '', C: '', D: '' }
                    correctAnswer,
                    hasTable,
                    table,         // { column_A: [], column_B: [] } format for upload
                    tableHtml,     // Raw HTML for display
                    hasImage,
                    imageUrl       // Full URL for proxy download
                });
            }
        } catch (e) {
            console.warn(`Error parsing question at index ${index}:`, e);
        }
    });
    
    return questions;
}

/**
 * Show scraped questions result modal with download option
 */
function showScrapedQuestionsResult(jsonData, sourceName) {
    const modalHtml = `
        <div id="scrapedResultModal" class="modal" style="display: flex;">
            <div class="modal-content" style="max-width: 900px; max-height: 90vh;">
                <div class="modal-header" style="background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%); color: white; border-radius: 12px 12px 0 0;">
                    <h2 style="color: white; margin: 0;">
                        <i class="fas fa-check-circle"></i> Extraction Complete!
                    </h2>
                    <span class="close" onclick="document.getElementById('scrapedResultModal').remove()" style="color: white; cursor: pointer;">&times;</span>
                </div>
                <div class="modal-body" style="padding: 20px;">
                    <div style="display: flex; gap: 20px; margin-bottom: 20px;">
                        <div style="flex: 1; background: #e3f2fd; padding: 15px; border-radius: 10px; text-align: center;">
                            <h3 style="margin: 0; color: #1565c0; font-size: 32px;">${jsonData.totalQuestions}</h3>
                            <p style="margin: 5px 0 0 0; color: #666;">Questions Extracted</p>
                        </div>
                        <div style="flex: 1; background: #e8f5e9; padding: 15px; border-radius: 10px; text-align: center;">
                            <h3 style="margin: 0; color: #2e7d32; font-size: 32px;">${jsonData.questions.filter(q => q.hasTable).length}</h3>
                            <p style="margin: 5px 0 0 0; color: #666;">With Tables</p>
                        </div>
                        <div style="flex: 1; background: #fff3e0; padding: 15px; border-radius: 10px; text-align: center;">
                            <h3 style="margin: 0; color: #e65100; font-size: 32px;">${jsonData.questions.filter(q => q.hasImage).length}</h3>
                            <p style="margin: 5px 0 0 0; color: #666;">With Images</p>
                        </div>
                    </div>
                    
                    <div style="background: #f5f5f5; border-radius: 8px; padding: 10px; margin-bottom: 15px;">
                        <strong>Source:</strong> ${sourceName}<br>
                        <strong>Extracted:</strong> ${new Date().toLocaleString()}
                    </div>
                    
                    <div style="margin-bottom: 15px;">
                        <label style="font-weight: 600; margin-bottom: 8px; display: block;">JSON Preview:</label>
                        <textarea id="scrapedJsonContent" readonly style="
                            width: 100%; height: 300px; font-family: monospace; font-size: 12px;
                            border: 2px solid #e0e0e0; border-radius: 8px; padding: 10px;
                            background: #fafafa;
                        ">${JSON.stringify(jsonData, null, 2)}</textarea>
                    </div>
                </div>
                <div class="modal-footer" style="padding: 15px 20px; border-top: 1px solid #eee; display: flex; gap: 10px; justify-content: flex-end;">
                    <button onclick="copyScrapedJson()" class="btn" style="background: #607d8b; color: white; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer;">
                        <i class="fas fa-copy"></i> Copy JSON
                    </button>
                    <button onclick="downloadScrapedJson('${sourceName}')" class="btn" style="background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%); color: white; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer;">
                        <i class="fas fa-download"></i> Download JSON
                    </button>
                    <button onclick="loadJsonToScraper()" class="btn" style="background: linear-gradient(135deg, #FF9800 0%, #F57C00 100%); color: white; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer;">
                        <i class="fas fa-arrow-right"></i> Load to Scraper
                    </button>
                </div>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // Store for later use
    window._scrapedJsonData = jsonData;
}

function copyScrapedJson() {
    const content = document.getElementById('scrapedJsonContent').value;
    navigator.clipboard.writeText(content).then(() => {
        showNotification('JSON copied to clipboard!', 'success');
    });
}

function downloadScrapedJson(sourceName) {
    const content = document.getElementById('scrapedJsonContent').value;
    const filename = `extracted_questions_${sourceName.replace(/[^a-zA-Z0-9]/g, '_')}_${Date.now()}.json`;
    
    const blob = new Blob([content], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    
    showNotification(`Downloaded: ${filename}`, 'success');
}

function loadJsonToScraper() {
    if (!window._scrapedJsonData) {
        showNotification('No scraped data found', 'error');
        return;
    }
    
    // Close the result modal
    document.getElementById('scrapedResultModal')?.remove();
    
    // Navigate to Question Scraper and set the data - REMOVED
    // showSection('questionScraper');
    showNotification('Question Scraper removed. Use "Add Question" page instead.', 'info');
    
    // Store in global extractedQuestions for the scraper
    extractedQuestions = window._scrapedJsonData.questions.map(q => ({
        questionNumber: q.questionNumber,
        question: q.question_en || q.question_ta,
        options: [
            { label: 'A', text: q.options?.A || '' },
            { label: 'B', text: q.options?.B || '' },
            { label: 'C', text: q.options?.C || '' },
            { label: 'D', text: q.options?.D || '' }
        ],
        correctAnswer: q.correctAnswer,
        hasTable: q.hasTable,
        tableData: q.tableData,
        hasImage: q.hasImage,
        imageUrl: q.imageUrl
    }));
    
    // Update results display
    displayExtractedQuestions();
    
    showNotification(`Loaded ${extractedQuestions.length} questions to Question Scraper`, 'success');
}

console.log('TNPSC Mock Test Admin Panel - Mockup Version');

// ============================================
// MARKETING PAGE - META ADS INSIGHTS
// ============================================

// Initialize Marketing page
function initMarketingPage() {
    // Set default date range (last 30 days)
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 30);
    
    document.getElementById('metaAdsStartDate').value = startDate.toISOString().split('T')[0];
    document.getElementById('metaAdsEndDate').value = endDate.toISOString().split('T')[0];
    
    // Load initial data
    loadMetaAdsInsights();
}

// Load Meta Ads insights from cached DB
async function loadMetaAdsInsights() {
    const startDate = document.getElementById('metaAdsStartDate').value;
    const endDate = document.getElementById('metaAdsEndDate').value;
    const level = document.getElementById('metaAdsLevel').value;
    
    if (!startDate || !endDate) {
        showNotification('Please select date range', 'warning');
        return;
    }
    
    const tbody = document.getElementById('metaAdsTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="9" style="text-align: center; padding: 40px;">
                <div class="loading-spinner"></div>
                <p style="margin-top: 10px; color: #666;">Loading Meta Ads data...</p>
            </td>
        </tr>
    `;
    
    try {
        const url = `${API_BASE_URL}/admin/marketing/meta_ads_insights.php?start_date=${startDate}&end_date=${endDate}&level=${level}&view=all`;
        const response = await fetch(url, { credentials: 'include' });
        const data = await response.json();
        
        if (!data.success) {
            throw new Error(data.message || 'Failed to load insights');
        }
        
        // Check if configured
        const configStatus = document.getElementById('metaAdsConfigStatus');
        if (!data.configured) {
            configStatus.style.display = 'block';
        } else {
            configStatus.style.display = 'none';
        }
        
        // Update last sync
        const lastSync = data.last_sync || 'Never';
        document.getElementById('metaAdsLastSync').textContent = `Last sync: ${lastSync}`;
        
        // Update stats
        const totals = data.totals || {};
        document.getElementById('metaAdsTotalSpend').textContent = formatCurrency(totals.total_spend || 0);
        
        // Handle "Multiple conversions" mode (like Meta Ads Manager)
        if (totals.result_mode === 'multiple') {
            document.getElementById('metaAdsTotalResults').textContent = 'Multiple';
            document.getElementById('metaAdsCostPerResult').textContent = '-';
        } else {
            document.getElementById('metaAdsTotalResults').textContent = formatNumber(totals.total_results || 0);
            document.getElementById('metaAdsCostPerResult').textContent = totals.avg_cost_per_result ? formatCurrency(totals.avg_cost_per_result) : '-';
        }
        document.getElementById('metaAdsTotalClicks').textContent = formatNumber(totals.total_clicks || 0);
        
        // Render table
        renderMetaAdsTable(data.insights || [], level);
        
    } catch (error) {
        console.error('Error loading Meta Ads insights:', error);
        tbody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; padding: 40px; color: #f44336;">
                    <i class="fas fa-exclamation-triangle" style="font-size: 24px; margin-bottom: 10px;"></i>
                    <p>${escapeHtml(error.message)}</p>
                </td>
            </tr>
        `;
    }
}

// Get result type display info
function getResultTypeInfo(actionType) {
    const types = {
        'omni_app_install': { label: 'Install', icon: 'fa-download', color: '#2196F3', bg: '#E3F2FD' },
        'mobile_app_install': { label: 'Install', icon: 'fa-download', color: '#2196F3', bg: '#E3F2FD' },
        'app_install': { label: 'Install', icon: 'fa-download', color: '#2196F3', bg: '#E3F2FD' },
        'omni_purchase': { label: 'Purchase', icon: 'fa-shopping-cart', color: '#4CAF50', bg: '#E8F5E9' },
        'purchase': { label: 'Purchase', icon: 'fa-shopping-cart', color: '#4CAF50', bg: '#E8F5E9' },
        'app_custom_event.fb_mobile_purchase': { label: 'In-App Purchase', icon: 'fa-credit-card', color: '#4CAF50', bg: '#E8F5E9' },
        'link_click': { label: 'Click', icon: 'fa-mouse-pointer', color: '#FF9800', bg: '#FFF3E0' },
        'omni_complete_registration': { label: 'Registration', icon: 'fa-user-plus', color: '#9C27B0', bg: '#F3E5F5' },
        'complete_registration': { label: 'Registration', icon: 'fa-user-plus', color: '#9C27B0', bg: '#F3E5F5' },
        'lead': { label: 'Lead', icon: 'fa-user-tag', color: '#00BCD4', bg: '#E0F7FA' },
        'video_view': { label: 'Video View', icon: 'fa-play', color: '#F44336', bg: '#FFEBEE' }
    };
    return types[actionType] || { label: 'Result', icon: 'fa-check', color: '#666', bg: '#F5F5F5' };
}

// Render Meta Ads table
function renderMetaAdsTable(insights, level) {
    const tbody = document.getElementById('metaAdsTableBody');
    
    if (!insights || insights.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; padding: 40px; color: #666;">
                    <i class="fas fa-inbox" style="font-size: 32px; margin-bottom: 10px; opacity: 0.5;"></i>
                    <p>No data found for selected date range</p>
                    <p style="font-size: 13px; margin-top: 5px;">Click "Sync Now" to fetch data from Meta Ads</p>
                </td>
            </tr>
        `;
        return;
    }
    
    // Build table rows
    let html = '';
    insights.forEach(row => {
        const entityName = level === 'campaign' ? row.campaign_name :
                          level === 'adset' ? row.adset_name :
                          row.ad_name;
        
        // Get result type info for badge
        const resultType = getResultTypeInfo(row.result_action_type);
        const results = row.results || 0;
        const costPerResult = row.cost_per_result;
        
        // Determine cost color based on result type
        let costColor = '#4CAF50'; // Default green
        if (costPerResult) {
            if (row.result_action_type && row.result_action_type.includes('purchase')) {
                // For purchases, higher threshold
                costColor = costPerResult > 100 ? '#f44336' : costPerResult > 50 ? '#FF9800' : '#4CAF50';
            } else {
                // For installs, lower threshold
                costColor = costPerResult > 20 ? '#f44336' : costPerResult > 10 ? '#FF9800' : '#4CAF50';
            }
        }
        
        html += `
            <tr>
                <td>${escapeHtml(row.day)}</td>
                <td>
                    <div style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${escapeHtml(entityName)}">
                        ${escapeHtml(entityName || 'N/A')}
                    </div>
                </td>
                <td style="font-weight: 600;">${formatCurrency(row.spend)}</td>
                <td>
                    <div style="display: flex; align-items: center; gap: 6px;">
                        <span style="background: ${resultType.bg}; color: ${resultType.color}; padding: 4px 10px; border-radius: 4px; font-weight: 600;">
                            ${formatNumber(results)}
                        </span>
                        <span style="font-size: 11px; color: #888;">
                            <i class="fas ${resultType.icon}" style="margin-right: 2px;"></i>${resultType.label}
                        </span>
                    </div>
                </td>
                <td>
                    <div style="display: flex; flex-direction: column; align-items: flex-start;">
                        <span style="color: ${costColor}; font-weight: 600;">
                            ${costPerResult ? formatCurrency(costPerResult) : '-'}
                        </span>
                        ${costPerResult ? `<span style="font-size: 10px; color: #999;">Per ${resultType.label}</span>` : ''}
                    </div>
                </td>
                <td>${formatNumber(row.clicks)}</td>
                <td>${row.cpc ? formatCurrency(row.cpc) : '-'}</td>
                <td>${formatNumber(row.impressions)}</td>
                <td>${row.ctr ? parseFloat(row.ctr).toFixed(2) + '%' : '-'}</td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

// Sync Meta Ads data from API
async function syncMetaAds() {
    const startDate = document.getElementById('metaAdsStartDate').value;
    const endDate = document.getElementById('metaAdsEndDate').value;
    const level = document.getElementById('metaAdsLevel').value;
    
    if (!startDate || !endDate) {
        showNotification('Please select date range', 'warning');
        return;
    }
    
    showNotification('Syncing Meta Ads data...', 'info');
    
    try {
        const response = await fetch(`${API_BASE_URL}/admin/marketing/meta_ads_sync.php`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                start_date: startDate,
                end_date: endDate,
                level: level
            })
        });
        
        const data = await response.json();
        
        if (!data.success) {
            throw new Error(data.message || 'Sync failed');
        }
        
        showNotification(`Synced ${data.data.rows_synced} rows. Total spend: ${formatCurrency(data.data.total_spend)}`, 'success');
        
        // Reload insights
        loadMetaAdsInsights();
        
    } catch (error) {
        console.error('Error syncing Meta Ads:', error);
        showNotification(error.message, 'error');
    }
}

// Export Meta Ads data as CSV
function exportMetaAdsCSV() {
    const startDate = document.getElementById('metaAdsStartDate').value;
    const endDate = document.getElementById('metaAdsEndDate').value;
    const level = document.getElementById('metaAdsLevel').value;
    
    if (!startDate || !endDate) {
        showNotification('Please select date range', 'warning');
        return;
    }
    
    // Build export URL
    const url = `${API_BASE_URL}/admin/export/reports.php?type=meta_ads_insights&format=csv&start_date=${startDate}&end_date=${endDate}&level=${level}`;
    
    // Trigger download
    window.location.href = url;
    showNotification('Export started. CSV file will download shortly.', 'success');
}

// Helper: Format currency (INR)
function formatCurrency(value) {
    if (value === null || value === undefined) return '₹0';
    const num = parseFloat(value);
    if (isNaN(num)) return '₹0';
    
    if (num >= 100000) {
        return '₹' + (num / 100000).toFixed(2) + 'L';
    } else if (num >= 1000) {
        return '₹' + (num / 1000).toFixed(2) + 'K';
    }
    return '₹' + num.toFixed(2);
}

// Helper: Format number with commas
function formatNumber(value) {
    if (value === null || value === undefined) return '0';
    const num = parseInt(value);
    if (isNaN(num)) return '0';
    return num.toLocaleString('en-IN');
}

// ============================================
// REPORTS PAGE - DOWNLOADS
// ============================================

// Initialize Reports page
function initReportsPage() {
    // Set default date range (last 30 days)
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 30);
    
    document.getElementById('reportStartDate').value = startDate.toISOString().split('T')[0];
    document.getElementById('reportEndDate').value = endDate.toISOString().split('T')[0];
}

// Current report type for download
let currentReportType = null;
let currentReportData = null;

// Download report as CSV
function downloadReport(type) {
    const startDate = document.getElementById('reportStartDate').value;
    const endDate = document.getElementById('reportEndDate').value;
    
    if (!startDate || !endDate) {
        showNotification('Please select date range', 'warning');
        return;
    }
    
    let url = `${API_BASE_URL}/admin/export/reports.php?type=${type}&format=csv&start_date=${startDate}&end_date=${endDate}`;
    
    // Add level for meta ads
    if (type === 'meta_ads_insights') {
        url += '&level=campaign';
    }
    
    showNotification(`Downloading ${type.replace(/_/g, ' ')}...`, 'info');
    
    // Trigger download
    window.location.href = url;
}

// View report in table
async function viewReport(type) {
    const startDate = document.getElementById('reportStartDate').value;
    const endDate = document.getElementById('reportEndDate').value;
    
    if (!startDate || !endDate) {
        showNotification('Please select date range', 'warning');
        return;
    }
    
    currentReportType = type;
    
    let url = `${API_BASE_URL}/admin/export/reports.php?type=${type}&format=json&start_date=${startDate}&end_date=${endDate}`;
    
    if (type === 'meta_ads_insights') {
        url += '&level=campaign';
    }
    
    showNotification(`Loading ${type.replace(/_/g, ' ')}...`, 'info');
    
    try {
        const response = await fetch(url, { credentials: 'include' });
        const result = await response.json();
        
        if (!result.success) {
            throw new Error(result.message || 'Failed to load report');
        }
        
        currentReportData = result.data;
        // Pass totals and result_mode for combined_daily_report
        renderReportPreview(type, result.data, startDate, endDate, result.totals, result.result_mode);
        
    } catch (error) {
        console.error('Error loading report:', error);
        showNotification(error.message, 'error');
    }
}

// Render report preview table
function renderReportPreview(type, data, startDate, endDate, apiTotals = null, resultMode = null) {
    const section = document.getElementById('reportPreviewSection');
    const title = document.getElementById('reportPreviewTitle');
    const thead = document.getElementById('reportPreviewHead');
    const tbody = document.getElementById('reportPreviewBody');
    const stats = document.getElementById('reportPreviewStats');
    
    // Set title
    const titles = {
        'daily_user_metrics': 'Daily User Metrics',
        'meta_ads_insights': 'Meta Ads Insights',
        'test_results': 'Test Results',
        'users': 'All Users',
        'combined_daily_report': 'Combined Daily Report (Users + Ads)'
    };
    title.innerHTML = `<i class="fas fa-table"></i> ${titles[type] || type} (${startDate} to ${endDate})`;
    
    // Define columns for each type
    const columns = {
        'daily_user_metrics': ['Date', 'Registrations', 'Trial Users', 'Active Users', 'Total Users'],
        'meta_ads_insights': ['Date', 'Campaign', 'Spend', 'Results', 'Cost/Result', 'Clicks', 'Impressions'],
        'test_results': ['User', 'Mobile', 'Category', 'Score', 'Questions', 'Time', 'Date'],
        'users': ['ID', 'Name', 'Mobile', 'Tests', 'Avg Score', 'Registered', 'Last Login'],
        'combined_daily_report': ['Date', 'Registrations', 'Trial', 'Spend', 'Installs', 'Cost/Install', 'Purchases', 'Cost/Purchase']
    };
    
    // Render header
    const cols = columns[type] || Object.keys(data[0] || {});
    thead.innerHTML = `<tr>${cols.map(c => `<th>${c}</th>`).join('')}</tr>`;
    
    // Render body
    if (!data || data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="' + cols.length + '" style="text-align: center; padding: 40px;">No data found</td></tr>';
    } else {
        tbody.innerHTML = data.slice(0, 100).map(row => {
            let cells = [];
            
            if (type === 'daily_user_metrics') {
                cells = [
                    row.date,
                    row.registrations,
                    row.trial_users,
                    row.active_users,
                    row.total_users
                ];
            } else if (type === 'meta_ads_insights') {
                cells = [
                    row.day,
                    row.entity_name || row.campaign_name || '-',
                    '₹' + parseFloat(row.spend || 0).toFixed(2),
                    row.results || 0,
                    row.cost_per_result ? '₹' + parseFloat(row.cost_per_result).toFixed(2) : '-',
                    row.clicks || 0,
                    row.impressions || 0
                ];
            } else if (type === 'test_results') {
                cells = [
                    row.user_name || '-',
                    row.user_mobile || '-',
                    row.category_name || '-',
                    (row.percentage || 0) + '%',
                    row.total_questions || 0,
                    (row.time_taken || 0) + 's',
                    row.submitted_at ? row.submitted_at.split(' ')[0] : '-'
                ];
            } else if (type === 'users') {
                cells = [
                    row.id,
                    row.name || '-',
                    row.mobile || '-',
                    row.total_tests || 0,
                    row.avg_score ? parseFloat(row.avg_score).toFixed(1) + '%' : '-',
                    row.created_at ? row.created_at.split(' ')[0] : '-',
                    row.last_login ? row.last_login.split(' ')[0] : 'Never'
                ];
            } else if (type === 'combined_daily_report') {
                // Show Installs and Purchases separately
                cells = [
                    row.date,
                    row.registrations || 0,
                    row.trial_users || 0,
                    '₹' + parseFloat(row.ad_spend || 0).toFixed(2),
                    row.installs || 0,
                    row.cost_per_install ? '₹' + parseFloat(row.cost_per_install).toFixed(2) : '-',
                    row.purchases || 0,
                    row.cost_per_purchase ? '₹' + parseFloat(row.cost_per_purchase).toFixed(2) : '-'
                ];
            }
            
            return `<tr>${cells.map(c => `<td>${escapeHtml(String(c))}</td>`).join('')}</tr>`;
        }).join('');
    }
    
    // Show stats
    let statsHtml = `<strong>Total Records:</strong> ${data.length}`;
    
    if (type === 'daily_user_metrics' && data.length > 0) {
        const totalReg = data.reduce((s, r) => s + (r.registrations || 0), 0);
        const totalTrial = data.reduce((s, r) => s + (r.trial_users || 0), 0);
        const totalActive = data.reduce((s, r) => s + (r.active_users || 0), 0);
        statsHtml += ` | <strong>Total Registrations:</strong> ${totalReg} | <strong>Total Trial:</strong> ${totalTrial} | <strong>Total Active:</strong> ${totalActive}`;
    } else if (type === 'meta_ads_insights' && data.length > 0) {
        const totalSpend = data.reduce((s, r) => s + parseFloat(r.spend || 0), 0);
        const totalResults = data.reduce((s, r) => s + (parseInt(r.results) || 0), 0);
        statsHtml += ` | <strong>Total Spend:</strong> ₹${totalSpend.toFixed(2)} | <strong>Total Results:</strong> ${totalResults}`;
        if (totalResults > 0) {
            statsHtml += ` | <strong>Avg Cost/Result:</strong> ₹${(totalSpend / totalResults).toFixed(2)}`;
        }
    } else if (type === 'combined_daily_report' && data.length > 0) {
        // Use API totals if available
        if (apiTotals) {
            statsHtml += ` | <strong>Registrations:</strong> ${apiTotals.registrations || 0}`;
            statsHtml += ` | <strong>Spend:</strong> ₹${parseFloat(apiTotals.ad_spend || 0).toFixed(2)}`;
            statsHtml += ` | <strong>Installs:</strong> ${apiTotals.installs || 0}`;
            if (apiTotals.cost_per_install) {
                statsHtml += ` (₹${parseFloat(apiTotals.cost_per_install).toFixed(2)}/install)`;
            }
            statsHtml += ` | <strong>Purchases:</strong> ${apiTotals.purchases || 0}`;
            if (apiTotals.cost_per_purchase) {
                statsHtml += ` (₹${parseFloat(apiTotals.cost_per_purchase).toFixed(2)}/purchase)`;
            }
        } else {
            // Fallback
            const totalReg = data.reduce((s, r) => s + (r.registrations || 0), 0);
            const totalInstalls = data.reduce((s, r) => s + (r.installs || 0), 0);
            const totalPurchases = data.reduce((s, r) => s + (r.purchases || 0), 0);
            statsHtml += ` | <strong>Registrations:</strong> ${totalReg}`;
            statsHtml += ` | <strong>Installs:</strong> ${totalInstalls}`;
            statsHtml += ` | <strong>Purchases:</strong> ${totalPurchases}`;
        }
    }
    
    if (data.length > 100) {
        statsHtml += ` <span style="color: #f57c00;">(Showing first 100 rows - download CSV for full data)</span>`;
    }
    
    stats.innerHTML = statsHtml;
    
    // Show section
    section.style.display = 'block';
    section.scrollIntoView({ behavior: 'smooth' });
}

// Download current viewed report
function downloadCurrentReport() {
    if (currentReportType) {
        downloadReport(currentReportType);
    }
}

// Close report preview
function closeReportPreview() {
    document.getElementById('reportPreviewSection').style.display = 'none';
    currentReportType = null;
    currentReportData = null;
}

// Open report in Google Sheets
function openInGoogleSheets(type) {
    const startDate = document.getElementById('reportStartDate').value;
    const endDate = document.getElementById('reportEndDate').value;
    
    if (!startDate || !endDate) {
        showNotification('Please select date range', 'warning');
        return;
    }
    
    // First download the CSV
    downloadReport(type);
    
    // Then open Google Sheets
    setTimeout(() => {
        showNotification('CSV downloaded! Now upload it to Google Sheets: File → Import → Upload', 'success');
        window.open('https://sheets.google.com/create', '_blank');
    }, 1000);
}
