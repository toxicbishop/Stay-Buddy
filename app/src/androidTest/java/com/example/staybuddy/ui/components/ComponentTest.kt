package com.example.staybuddy.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import com.example.staybuddy.ui.theme.StayBuddyTheme
import org.junit.Rule
import org.junit.Test

class ComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testListingCardSkeletonDisplays() {
        // Set the UI content for the test
        composeTestRule.setContent {
            StayBuddyTheme {
                ListingCardSkeleton()
            }
        }

        // Verify that the skeleton root is displayed in the composition
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
