<?php
/**
 * Auth Middleware (Single-Device Session)
 *
 * Enforces:
 * - Authorization: Bearer <session_token>
 * - X-Device-Id: <device_id>
 * - session_token + device_id must match the current active session for the user_id
 *
 * On mismatch => 401 SESSION_REVOKED (means logged in from another device)
 */

function getAuthorizationBearerToken() {
    $headers = function_exists('getallheaders') ? getallheaders() : [];

    $authHeader = null;
    if (isset($headers['Authorization'])) $authHeader = $headers['Authorization'];
    if (isset($headers['authorization'])) $authHeader = $headers['authorization'];
    if (!$authHeader && isset($_SERVER['HTTP_AUTHORIZATION'])) $authHeader = $_SERVER['HTTP_AUTHORIZATION'];

    if (!$authHeader) return null;

    if (preg_match('/Bearer\s+(.*)$/i', $authHeader, $matches)) {
        return trim($matches[1]);
    }
    return null;
}

function getDeviceIdHeader() {
    $headers = function_exists('getallheaders') ? getallheaders() : [];
    if (isset($headers['X-Device-Id'])) return trim($headers['X-Device-Id']);
    if (isset($headers['x-device-id'])) return trim($headers['x-device-id']);
    if (isset($_SERVER['HTTP_X_DEVICE_ID'])) return trim($_SERVER['HTTP_X_DEVICE_ID']);
    return null;
}

/**
 * @param PDO $db
 * @param int $userId
 * @return array user row (id, device_id, session_token)
 */
function requireUserSession($db, $userId) {
    $token = getAuthorizationBearerToken();
    $deviceId = getDeviceIdHeader();

    if (!$token || !$deviceId) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Unauthorized.',
            'error_code' => 'UNAUTHORIZED'
        ]);
        exit;
    }

    $query = "SELECT id, device_id, session_token
              FROM users
              WHERE id = :id
              LIMIT 1";
    $stmt = $db->prepare($query);
    $stmt->bindParam(':id', $userId, PDO::PARAM_INT);
    $stmt->execute();
    $row = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$row) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Unauthorized.',
            'error_code' => 'UNAUTHORIZED'
        ]);
        exit;
    }

    // If session not set yet, treat as unauthorized
    if (empty($row['session_token']) || empty($row['device_id'])) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Session not initialized. Please login again.',
            'error_code' => 'SESSION_NOT_INITIALIZED'
        ]);
        exit;
    }

    if (!hash_equals($row['session_token'], $token) || !hash_equals($row['device_id'], $deviceId)) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'You have been logged out because this account was used on another device.',
            'error_code' => 'SESSION_REVOKED'
        ]);
        exit;
    }

    return $row;
}

?>

