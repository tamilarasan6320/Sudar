<?php
/**
 * Question Scraper for civilserviceaspirants.in
 * Enhanced version with robust HTML parsing
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Get POST data
$input = json_decode(file_get_contents('php://input'), true);
$url = isset($input['url']) ? trim($input['url']) : '';

if (empty($url)) {
    echo json_encode([
        'success' => false,
        'message' => 'URL is required'
    ]);
    exit;
}

// Validate URL
if (strpos($url, 'civilserviceaspirants.in') === false) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid URL. Only civilserviceaspirants.in URLs are supported'
    ]);
    exit;
}

try {
    // Determine base URL for pagination
    $baseUrl = preg_replace('/-\d+\.php$/', '', $url);
    if (!preg_match('/-\d+\.php$/', $url)) {
        $baseUrl = preg_replace('/\.php$/', '', $url);
    }
    
    $allQuestions = [];
    $topic = '';
    $category = 'பொதுத் தமிழ் - அலகு I : இலக்கணம்';
    $pagesScraped = 0;
    
    // Try to scrape up to 10 pages
    for ($pageNum = 1; $pageNum <= 10; $pageNum++) {
        $pageUrl = $baseUrl . '-' . $pageNum . '.php';
        
        // Fetch HTML content
        $html = fetchURL($pageUrl);
        
        if (!$html) {
            if ($pageNum == 1) {
                throw new Exception("Failed to fetch content from URL. Please check your internet connection and try again.");
            }
            break; // No more pages
        }
        
        // Extract topic on first page
        if ($pageNum == 1) {
            if (preg_match('/<h1[^>]*>([^<]+)<\/h1>/u', $html, $match)) {
                $fullTitle = trim(strip_tags($match[1]));
                // Extract just the Tamil topic name
                if (preg_match('/^([^\s]+)/u', $fullTitle, $topicMatch)) {
                    $topic = $topicMatch[1];
                } else {
                    $topic = $fullTitle;
                }
            }
        }
        
        // Extract questions from this page
        $pageQuestions = extractQuestionsImproved($html);
        
        if (empty($pageQuestions)) {
            if ($pageNum == 1) {
                throw new Exception("No questions found on the page. The website structure may have changed.");
            }
            break; // No more questions
        }
        
        $allQuestions = array_merge($allQuestions, $pageQuestions);
        $pagesScraped = $pageNum;
        
        // Respectful delay between requests
        if ($pageNum < 10) {
            usleep(500000); // 0.5 seconds
        }
    }
    
    if (empty($allQuestions)) {
        throw new Exception("No questions could be extracted. Please verify the URL and try again.");
    }
    
    // Return success response
    echo json_encode([
        'success' => true,
        'topic' => $topic ?: 'சேர்த்து எழுதுதல்',
        'category' => $category,
        'totalPages' => $pagesScraped,
        'questions' => $allQuestions,
        'message' => 'Successfully scraped ' . count($allQuestions) . ' questions from ' . $pagesScraped . ' page(s)'
    ], JSON_UNESCAPED_UNICODE);
    
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
}

function fetchURL($url) {
    $errorDetails = [];
    
    // Method 1: Try cURL with multiple configurations
    if (function_exists('curl_init')) {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_MAXREDIRS, 5);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
        curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36');
        curl_setopt($ch, CURLOPT_TIMEOUT, 45);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 30);
        curl_setopt($ch, CURLOPT_ENCODING, '');
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language: en-US,en;q=0.9,ta;q=0.8',
            'Accept-Encoding: gzip, deflate',
            'Connection: keep-alive',
            'Upgrade-Insecure-Requests: 1'
        ]);
        
        $html = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        $curlErrno = curl_errno($ch);
        curl_close($ch);
        
        if ($html && $httpCode == 200) {
            return $html;
        }
        
        $errorDetails[] = "cURL: HTTP $httpCode, Error: $curlError (Code: $curlErrno)";
    } else {
        $errorDetails[] = "cURL extension not available";
    }
    
    // Method 2: Try file_get_contents
    if (ini_get('allow_url_fopen')) {
        $context = stream_context_create([
            'http' => [
                'method' => 'GET',
                'header' => "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36\r\n" .
                           "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" .
                           "Accept-Language: en-US,en;q=0.9\r\n" .
                           "Connection: keep-alive\r\n",
                'timeout' => 45,
                'follow_location' => 1,
                'max_redirects' => 5
            ],
            'ssl' => [
                'verify_peer' => false,
                'verify_peer_name' => false,
                'allow_self_signed' => true
            ]
        ]);
        
        $html = @file_get_contents($url, false, $context);
        if ($html) {
            return $html;
        }
        
        $error = error_get_last();
        $errorDetails[] = "file_get_contents: " . ($error ? $error['message'] : 'Unknown error');
    } else {
        $errorDetails[] = "allow_url_fopen is disabled in php.ini";
    }
    
    // Log detailed error for debugging
    error_log("Failed to fetch URL: $url. Details: " . implode('; ', $errorDetails));
    
    return false;
}

function extractQuestionsImproved($html) {
    $questions = [];
    
    // Remove extra whitespace and normalize
    $html = preg_replace('/\s+/u', ' ', $html);
    $html = str_replace('&nbsp;', ' ', $html);
    
    // Get plain text content
    $text = strip_tags($html);
    
    // Method 1: Split by question numbers
    $pattern = '/(\d+)\.\s+/u';
    $parts = preg_split($pattern, $text, -1, PREG_SPLIT_DELIM_CAPTURE | PREG_SPLIT_NO_EMPTY);
    
    for ($i = 0; $i < count($parts) - 1; $i += 2) {
        $questionNum = $parts[$i];
        $content = isset($parts[$i + 1]) ? $parts[$i + 1] : '';
        
        if (empty($content)) continue;
        
        // Extract question text (everything before "A.")
        if (!preg_match('/^(.*?)\s+A\.\s+/u', $content, $qMatch)) {
            continue;
        }
        $questionText = trim($qMatch[1]);
        
        // Skip if question is empty or too short
        if (strlen($questionText) < 3) continue;
        
        // Extract all options
        $options = ['A' => '', 'B' => '', 'C' => '', 'D' => ''];
        
        // Extract Option A
        if (preg_match('/A\.\s+(.+?)\s+B\.\s+/u', $content, $match)) {
            $options['A'] = trim($match[1]);
        }
        
        // Extract Option B
        if (preg_match('/B\.\s+(.+?)\s+C\.\s+/u', $content, $match)) {
            $options['B'] = trim($match[1]);
        }
        
        // Extract Option C
        if (preg_match('/C\.\s+(.+?)\s+D\.\s+/u', $content, $match)) {
            $options['C'] = trim($match[1]);
        }
        
        // Extract Option D (before View Answer or emoji or ANSWER)
        if (preg_match('/D\.\s+(.+?)(?:\s+View Answer|\s+😑|\s+ANSWER|\s+Rough Work)/u', $content, $match)) {
            $options['D'] = trim($match[1]);
        }
        
        // Extract correct answer
        $correctAnswer = '';
        if (preg_match('/ANSWER\s*:\s*([A-D])\./u', $content, $match)) {
            $correctAnswer = $match[1];
        }
        
        // Validate we have all required data
        if ($questionText && 
            $correctAnswer && 
            !empty($options['A']) && 
            !empty($options['B']) && 
            !empty($options['C']) && 
            !empty($options['D'])) {
            
            $questions[] = [
                'question' => $questionText,
                'options' => $options,
                'correctAnswer' => $correctAnswer
            ];
        }
    }
    
    return $questions;
}
?>
