/**
 * OneSignal Admin Integration
 * Handles sending push notifications from admin panel
 */

// OneSignal Configuration
const ONESIGNAL_APP_ID = '4ead7a7e-e376-41bf-8219-dd1a04e36da4';
const API_BASE_URL = 'https://sudartnpscapp.in/api';

/**
 * Send push notification to users
 * @param {Object} notificationData - Notification data
 * @param {string} notificationData.title - Notification title
 * @param {string} notificationData.message - Notification message
 * @param {string} notificationData.targetType - 'all' | 'specific_user' | 'user_segment'
 * @param {number} [notificationData.targetUserId] - User ID (if specific_user)
 * @param {Object} [notificationData.additionalData] - Custom data
 * @returns {Promise<Object>} API response
 */
async function sendPushNotification(notificationData) {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/notifications/send_push.php`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include', // Include session cookies
            body: JSON.stringify({
                title: notificationData.title,
                message: notificationData.message,
                target_type: notificationData.targetType || 'all',
                target_user_id: notificationData.targetUserId || null,
                additional_data: notificationData.additionalData || {}
            })
        });

        const result = await response.json();
        
        if (result.success) {
            showNotification('✅ Notification sent successfully!', 'success');
            return result;
        } else {
            showNotification('❌ Failed to send notification: ' + (result.message || 'Unknown error'), 'error');
            return result;
        }
    } catch (error) {
        console.error('Error sending push notification:', error);
        showNotification('❌ Error: ' + error.message, 'error');
        return { success: false, message: error.message };
    }
}

/**
 * Show notification message
 * @param {string} message - Message to display
 * @param {string} type - 'success' | 'error' | 'info'
 */
function showNotification(message, type = 'info') {
    // Use existing showNotification function from script.js if available
    if (typeof window.showNotification === 'function') {
        window.showNotification(message, type);
    } else {
        // Fallback to alert
        if (type === 'success') {
            alert('✅ ' + message);
        } else if (type === 'error') {
            alert('❌ ' + message);
        } else {
            alert('ℹ️ ' + message);
        }
    }
}

/**
 * Example usage in admin panel:
 * 
 * // Send to all users
 * sendPushNotification({
 *     title: 'New Test Available',
 *     message: 'Check out the new mock test!',
 *     targetType: 'all'
 * });
 * 
 * // Send to specific user
 * sendPushNotification({
 *     title: 'Test Result Ready',
 *     message: 'Your test results are available!',
 *     targetType: 'specific_user',
 *     targetUserId: 15,
 *     additionalData: {
 *         type: 'test_result',
 *         test_id: 123
 *     }
 * });
 */

