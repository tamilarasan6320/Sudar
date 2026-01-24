package com.sudar.tnpscapp.nativeapp.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyPolicyUiState(
    val isLoading: Boolean = true,
    val privacyText: String = DEFAULT_PRIVACY_POLICY
)

private const val DEFAULT_PRIVACY_POLICY = """
PRIVACY POLICY

Last updated: January 2025

1. INTRODUCTION

Welcome to Sudar TNPSC App ("we," "our," or "us"). We are committed to protecting your personal information and your right to privacy. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our mobile application.

2. INFORMATION WE COLLECT

We collect information that you provide directly to us:
• Mobile phone number (for account creation and verification)
• Name (optional, for personalization)
• Test performance data and progress
• App usage statistics

3. HOW WE USE YOUR INFORMATION

We use the information we collect to:
• Create and manage your account
• Provide personalized learning experience
• Track your test progress and performance
• Send important notifications about the app
• Improve our services and user experience

4. DATA SECURITY

We implement appropriate technical and organizational security measures to protect your personal information against unauthorized access, alteration, disclosure, or destruction.

5. DATA RETENTION

We retain your personal information for as long as your account is active or as needed to provide you services. You can request deletion of your account at any time.

6. YOUR RIGHTS

You have the right to:
• Access your personal data
• Correct inaccurate data
• Request deletion of your data
• Withdraw consent at any time

7. CHILDREN'S PRIVACY

Our app is intended for users who are at least 13 years old. We do not knowingly collect personal information from children under 13.

8. CHANGES TO THIS POLICY

We may update this Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page.

9. CONTACT US

If you have any questions about this Privacy Policy, please contact us at:
Email: sudar.tnpscapp@gmail.com
Phone: +91 83003 21814

By using the Sudar TNPSC App, you agree to the collection and use of information in accordance with this policy.
"""

@HiltViewModel
class PrivacyPolicyViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyPolicyUiState())
    val uiState: StateFlow<PrivacyPolicyUiState> = _uiState.asStateFlow()

    fun loadPrivacyPolicy() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val result = authRepository.getPublicSettings("privacy_policy")
                if (result.isSuccess) {
                    val value = result.getOrNull()
                    val text = when (value) {
                        is String -> value
                        is Map<*, *> -> value["text"]?.toString() ?: value["content"]?.toString()
                        else -> value?.toString()
                    }
                    if (!text.isNullOrBlank() && text != "null") {
                        _uiState.value = _uiState.value.copy(privacyText = text)
                    }
                    // If text is empty/null, keep the default
                }
            } catch (e: Exception) {
                // Use default - already set
                e.printStackTrace()
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
