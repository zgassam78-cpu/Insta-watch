package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthScreenMode
import com.example.auth.SignUpOnboardingDraft
import com.example.ui.screens.AuthScreenContent
import com.example.ui.screens.CreateFormScreen
import com.example.ui.theme.InstaWatchTheme
import com.example.viewmodel.CreateFormMode
import com.example.viewmodel.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context matches InstaWatch`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("InstaWatch", appName)
    }

    @Test
    fun `authentication screen supports both Log In and Sign Up flows`() {
        var mode by mutableStateOf(AuthScreenMode.LOG_IN)
        var draft by mutableStateOf(SignUpOnboardingDraft())
        var authTriggeredCount = 0

        composeTestRule.setContent {
            InstaWatchTheme {
                AuthScreenContent(
                    authMode = mode,
                    signUpDraft = draft,
                    isRestoringSession = false,
                    isAuthenticating = false,
                    errorMessage = null,
                    onSelectMode = { mode = it },
                    onChannelNameChanged = { draft = draft.copy(channelName = it) },
                    onHandleChanged = { draft = draft.copy(handle = it) },
                    onCategoryChanged = { draft = draft.copy(category = it) },
                    onAuthenticateWithGoogle = { authTriggeredCount += 1 },
                    onDismissError = {},
                )
            }
        }

        // Verify Log In mode default CTA
        composeTestRule.onNodeWithTag("google_sign_in_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(1, authTriggeredCount)

        // Switch to Sign Up mode
        composeTestRule.onNodeWithTag("auth_tab_signup")
            .performScrollTo()
            .performClick()
        assertEquals(AuthScreenMode.SIGN_UP, mode)

        // Fill Sign Up creator channel details and trigger Sign Up
        composeTestRule.onNodeWithTag("signup_channel_name_input")
            .performScrollTo()
            .performTextInput("Aarav Studio")
        composeTestRule.onNodeWithTag("signup_handle_input")
            .performScrollTo()
            .performTextInput("@aarav_watch")
        composeTestRule.onNodeWithTag("google_sign_up_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertTrue(draft.hasCustomInput)
        assertEquals("Aarav Studio", draft.channelName)
        assertEquals("@aarav_watch", draft.handle)
        assertEquals(2, authTriggeredCount)
    }

    @Test
    fun `create form switches across Photo Reel LongVideo and Creator Form modes`() {
        var activeMode by mutableStateOf(CreateFormMode.PHOTO)

        composeTestRule.setContent {
            InstaWatchTheme {
                CreateFormScreen(
                    mode = activeMode,
                    isSubmitting = false,
                    myProfileState = UiState.Success(null),
                    myAccountState = UiState.Success(null),
                    onSelectMode = { activeMode = it },
                    onPublishPost = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
                    onSaveCreatorForm = { _, _, _, _, _, _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithTag("form_title_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mode_tab_long_video").performClick()
        assertEquals(CreateFormMode.LONG_VIDEO, activeMode)

        composeTestRule.onNodeWithTag("mode_tab_creator_form").performClick()
        assertEquals(CreateFormMode.CREATOR_FORM, activeMode)
        composeTestRule.onNodeWithTag("creator_name_input").assertIsDisplayed()
    }
}
