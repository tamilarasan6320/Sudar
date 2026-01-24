# Parity spec (Flutter → Android Native)

## 1) Flutter screens (source of truth)

From `lib/screens/`:
- `app_startup_router.dart` (startup session check + premium check)
- `login_page.dart` / `otp_verification_page.dart` / `guest_login_page.dart`
- `exam_selection_page.dart`
- `home_page.dart`
- `tests_page.dart` / `test_page.dart` / `test_results_page.dart` / `test_history_page.dart` / `saved_tests_page.dart`
- `performance_page.dart` / `progress_page.dart`
- `profile_page.dart` / `edit_profile_page.dart` / `profile_setup_page.dart`
- `subscription_offer_page.dart` / `subscription_page.dart`
- `notifications_settings_page.dart`
- `language_selection_page.dart` / `language_settings_page.dart`
- `theme_settings_page.dart`
- `feedback_page.dart`
- `privacy_policy_page.dart` / `account_deletion_page.dart` / `about_page.dart` / `help_faq_page.dart`

## 2) Flutter services we must match

From `lib/services/`:
- `api_service.dart` (**base URL selection, device-id, auth headers, all endpoints**)
- `onesignal_service.dart` (push registration + player ID update)
- `firebase_service.dart` (firebase init, messaging)
- `google_analytics_service.dart` + `meta_app_events_service.dart` (analytics)
- `sms_retriever_service.dart` (OTP auto-fill)
- `theme_service.dart` + `language_service.dart`
- `truecaller_service.dart`

## 3) Backend contract (from Flutter `ApiService`)

### Headers
- `Content-Type: application/json`
- `Accept: application/json`
- `X-Device-Id: <stable-install-id>`
- `Authorization: Bearer <session_token>` (when logged-in)

### Auth endpoints
- `POST auth/send_otp.php`
- `POST auth/resend_otp.php`
- `POST auth/verify_otp.php` (includes `device_id`)
- `POST auth/truecaller_login.php`
- `POST auth/check_session.php` (requires bearer + device id; returns 401 `SESSION_REVOKED` etc)

### User endpoints
- `POST users/create.php`
- `PUT users/update.php` (auth)
- `GET users/get_profile.php?user_id=...` (auth)
- `POST users/update_onesignal_id.php` (auth)

### Test endpoints
- `GET tests/get_exam_categories.php`
- `GET tests/get_categories.php?exam_id=...&user_id=...`
- `GET tests/get_sessions.php?category_id=...`
- `GET tests/get_questions.php?session_id=...&language=en|ta`
- `POST tests/submit_result.php` (auth)
- `GET tests/get_history.php?user_id=...`
- `GET tests/get_rankings.php?...`
- `GET tests/get_leaderboard.php?...`
- `GET tests/get_performance.php?user_id=...&period=...` (auth)
- `GET tests/get_progress_analytics.php?user_id=...` (auth)
- `GET tests/get_test_details.php?result_id=...&user_id=...` (auth if user_id provided)

### Subscription endpoints
- `GET subscriptions/status.php?user_id=...`

### Settings/notifications
- `GET settings/get_public.php?key=...`
- `GET admin/notifications/crud.php?...`

## 4) Client-side stored keys (Flutter SharedPreferences)

We will mirror these in Android DataStore:
- `isLoggedIn`
- `token`
- `userId`
- `userName`
- `userMobile`
- `selectedExam`
- `selectedExamId`
- `is_premium`
- `deviceId`

