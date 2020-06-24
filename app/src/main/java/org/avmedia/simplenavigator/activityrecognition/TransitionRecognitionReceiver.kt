package org.avmedia.simplenavigator.activityrecognition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_ENTER
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class TransitionRecognitionReceiver : BroadcastReceiver() {

    lateinit var mContext: Context

    init {
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        mContext = context!!

        if (ActivityTransitionResult.hasResult(intent)) {
            var result = ActivityTransitionResult.extractResult(intent)

            if (result != null) {
                processTransitionResult(result)
            }
        }
    }

    fun processTransitionResult(result: ActivityTransitionResult) {
        for (event in result.transitionEvents) {
            onDetectedTransitionEvent(event)
        }
    }

    private fun onDetectedTransitionEvent(activity: ActivityTransitionEvent) {

        if (activity.transitionType == ACTIVITY_TRANSITION_ENTER) {
            when (activity.activityType) {
                DetectedActivity.STILL ->
                    ActivityCallback.activityTransition = "STILL"
                DetectedActivity.WALKING ->
                    ActivityCallback.activityTransition = "WALKING"
                DetectedActivity.ON_FOOT ->
                    ActivityCallback.activityTransition = "ON_FOOT"
                DetectedActivity.RUNNING ->
                    ActivityCallback.activityTransition = "RUNNING"
                DetectedActivity.ON_BICYCLE ->
                    ActivityCallback.activityTransition = "ON_BICYCLE"
                DetectedActivity.IN_VEHICLE ->
                    ActivityCallback.activityTransition = "IN_VEHICLE"

                // saveTransition(activity)
                else -> {
                    ActivityCallback.activityTransition = "UNKNOWN"
                }
            }
        } else {
            ActivityCallback.activityTransition = "UNKNOWN"
        }
    }
}