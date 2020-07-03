package org.avmedia.simplenavigator.activityrecognition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_ENTER
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import org.avmedia.simplenavigator.EventProcessor

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

        val event: EventProcessor.ProgressEvents =
            EventProcessor.ProgressEvents.ActivityChangeEvent

        if (activity.transitionType == ACTIVITY_TRANSITION_ENTER) {
            when (activity.activityType) {
                DetectedActivity.STILL -> {
                    event.payload = "STILL"
                    EventProcessor.onNext(event)
                }
                DetectedActivity.WALKING -> {
                    event.payload = "WALKING"
                    EventProcessor.onNext(event)
                }
                DetectedActivity.ON_FOOT -> {
                    event.payload = "ON_FOOT"
                    EventProcessor.onNext(event)
                }
                DetectedActivity.RUNNING -> {
                    event.payload = "RUNNING"
                    EventProcessor.onNext(event)
                }
                DetectedActivity.ON_BICYCLE -> {
                    event.payload = "ON_BICYCLE"
                    EventProcessor.onNext(event)
                }
                DetectedActivity.IN_VEHICLE -> {
                    event.payload = "IN_VEHICLE"
                    EventProcessor.onNext(event)
                }
                // saveTransition(activity)
                else -> {
                    event.payload = "UNKNOWN"
                    EventProcessor.onNext(event)
                }
            }
        } else {
            event.payload = "UNKNOWN"
            EventProcessor.onNext(event)
        }
    }
}