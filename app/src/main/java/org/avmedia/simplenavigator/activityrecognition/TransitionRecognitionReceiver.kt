package org.avmedia.simplenavigator.activityrecognition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
                    ActivityCallback.event = "STILL"
                DetectedActivity.WALKING ->
                    ActivityCallback.event = "WALKING"
                DetectedActivity.ON_FOOT ->
                    ActivityCallback.event = "ON_FOOT"
                DetectedActivity.RUNNING ->
                    ActivityCallback.event = "RUNNING"
                DetectedActivity.ON_BICYCLE ->
                    ActivityCallback.event = "ON_BICYCLE"
                DetectedActivity.IN_VEHICLE ->
                    ActivityCallback.event = "IN_VEHICLE"

                // saveTransition(activity)
                else -> {
                    ActivityCallback.event = "UNKNOWN"
                }
            }
        } else {
            Log.d("<<<<<<<<<<<<< onDetectedTransitionEvent", "Exiting activity")
            ActivityCallback.event = "UNKNOWN"
        }
    }
}