package com.sudar.tnpscapp.nativeapp.navigation

import android.net.Uri

object Routes {
    const val Splash = "splash"
    const val Startup = "startup"
    const val Login = "login"

    private const val OtpPattern = "otp/{mobile}"
    fun otp(mobile: String): String = "otp/${Uri.encode(mobile)}"
    const val Otp = OtpPattern

    private const val ProfileSetupPattern = "profile_setup/{mobile}?token={token}&method={method}"
    fun profileSetup(mobile: String, token: String = "", method: String = "otp"): String {
        return "profile_setup/${Uri.encode(mobile)}?token=${Uri.encode(token)}&method=${Uri.encode(method)}"
    }
    const val ProfileSetup = ProfileSetupPattern

    const val ExamSelection = "exam_selection"
    
    // Subscription offer screen with mode parameter
    // mode = "startup" (default): shown during app startup, continue goes to Main
    // mode = "gate": shown when trying to start a test without premium, skip goes back
    private const val SubscriptionOfferPattern = "subscription_offer?mode={mode}"
    fun subscriptionOffer(mode: String = "startup"): String = "subscription_offer?mode=$mode"
    const val SubscriptionOffer = SubscriptionOfferPattern
    
    const val Subscription = "subscription"
    
    // Main screen with bottom navigation
    const val Main = "main"
    
    // Legacy Home route (now replaced by Main)
    const val Home = "home"

    const val TestCategories = "test_categories"

    private const val SessionsPattern = "sessions/{categoryId}"
    fun sessions(categoryId: Int): String = "sessions/$categoryId"
    const val Sessions = SessionsPattern

    // Test intro screen - shown before starting test
    private const val TestIntroPattern = "test_intro/{sessionId}?title={title}&category={category}&duration={duration}&questions={questions}"
    fun testIntro(sessionId: Int, title: String, category: String, durationMinutes: Int, totalQuestions: Int = 0): String {
        return "test_intro/$sessionId?title=${Uri.encode(title)}&category=${Uri.encode(category)}&duration=$durationMinutes&questions=$totalQuestions"
    }
    const val TestIntro = TestIntroPattern

    private const val TakeTestPattern = "take_test/{sessionId}?title={title}&category={category}&duration={duration}"
    fun takeTest(sessionId: Int, title: String, category: String, durationMinutes: Int): String {
        return "take_test/$sessionId?title=${Uri.encode(title)}&category=${Uri.encode(category)}&duration=$durationMinutes"
    }
    const val TakeTest = TakeTestPattern

    // Test results - uses TestResultHolder for data passing
    const val TestResults = "test_results"

    const val TestHistory = "test_history"
    
    // Profile screens
    const val Profile = "profile"
    const val EditProfile = "edit_profile"
    
    // Progress screens
    const val Progress = "progress"
    const val Performance = "performance"
    
    // Tickets
    const val Tickets = "tickets"
    const val RaiseTicket = "raise_ticket"
    private const val RaiseTicketGuestPattern = "raise_ticket_guest?mobile={mobile}"
    fun raiseTicketGuest(mobile: String = ""): String = "raise_ticket_guest?mobile=${Uri.encode(mobile)}"
    const val RaiseTicketGuest = RaiseTicketGuestPattern
    private const val TicketDetailsPattern = "ticket_details/{ticketId}"
    fun ticketDetails(ticketId: Int): String = "ticket_details/$ticketId"
    const val TicketDetails = TicketDetailsPattern

    // Info screens
    const val About = "about"
    const val PrivacyPolicy = "privacy_policy"
    const val AccountDeletion = "account_deletion"
    
    // Settings screens
    const val NotificationsSettings = "notifications_settings"
    const val LanguageSettings = "language_settings"
    
    // Saved tests
    const val SavedTests = "saved_tests"
}
