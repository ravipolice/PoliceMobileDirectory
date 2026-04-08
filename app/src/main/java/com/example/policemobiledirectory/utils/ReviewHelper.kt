package com.example.policemobiledirectory.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.policemobiledirectory.data.local.SessionManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewHelper @Inject constructor(
    private val sessionManager: SessionManager
) {
    /**
     * Checks if the user is eligible for a rating request and triggers it if so.
     * Criteria:
     * - App opened at least 5 times (launchCount >= 5)
     * - At least 3 successful actions performed (eventCount >= 3)
     * - At least 7 days since last request
     * - Haven't already rated or opted out permanently
     */
    suspend fun tryTriggerReview(activity: Activity) {
        try {
            val launchCount = sessionManager.launchCount.first()
            val eventCount = sessionManager.successfulEventsCount.first()
            val lastRequestTime = sessionManager.lastRatingRequestTime.first()
            val alreadyRated = sessionManager.hasRatedOrNeverShow.first()

            if (alreadyRated) {
                Log.d("ReviewHelper", "Skipping review: User already rated or opted out.")
                return
            }

            val curTime = System.currentTimeMillis()
            val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
            val isCooldownOver = (curTime - lastRequestTime) > sevenDaysInMillis

            Log.d("ReviewHelper", "Checking eligibility: launches=$launchCount, events=$eventCount, cooldownOver=$isCooldownOver")

            if (launchCount >= 5 && eventCount >= 3 && isCooldownOver) {
                showReviewFlow(activity)
            }
        } catch (e: Exception) {
            Log.e("ReviewHelper", "Error checking review eligibility", e)
        }
    }

    private fun showReviewFlow(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown.
                    // We mark it as "attempted" and update the cooldown.
                    Log.d("ReviewHelper", "Review flow completed.")
                    updateMetadataAfterRequest()
                }
            } else {
                Log.e("ReviewHelper", "Failed to request review flow: ${task.exception?.message}")
            }
        }
    }

    private fun updateMetadataAfterRequest() {
        // We can't know if they rated or not, but we update the timestamp to ensure cooldown
        // If they rated, we might want to set hasRatedOrNeverShow to true, 
        // but since we don't know, we just rely on the 7-day cooldown or 
        // a maximum number of requests (e.g. 3 times total).
        // For simplicity, we just update the last request time.
        // If the user wants to be more "intelligent", we could count total requests.
        
        CoroutineScope(Dispatchers.IO).launch {
             sessionManager.setLastRatingRequestTime(System.currentTimeMillis())
        }
    }
}
