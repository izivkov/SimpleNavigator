package org.avmedia.simplenavigator.activityrecognition

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.util.*

class TransitionRecognition : TransitionRecognitionAbstract() {
    private val TAG = TransitionRecognition::class.java.simpleName
    lateinit var mContext: Context
    lateinit var mPendingIntent: PendingIntent

    override fun startTracking(context: Context) {
        mContext = context
        launchTransitionsTracker()
    }

    override fun stopTracking() {
        if (mContext != null && mPendingIntent != null) {
            ActivityRecognition.getClient(mContext).removeActivityTransitionUpdates(mPendingIntent)
                .addOnSuccessListener(OnSuccessListener<Void> {
                    mPendingIntent.cancel()
                })
                .addOnFailureListener(OnFailureListener { e ->
                })
        }
    }

    /***********************************************************************************************
     * LAUNCH TRANSITIONS TRACKER
     **********************************************************************************************/
    private fun launchTransitionsTracker() {
        val transitions = ArrayList<ActivityTransition>()

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        val request = ActivityTransitionRequest(transitions)
        val activityRecognitionClient = ActivityRecognition.getClient(mContext)

        val intent = Intent(mContext, TransitionRecognitionReceiver::class.java)
        mPendingIntent =
            PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (activityRecognitionPermissionApproved()) {

            val task =
                activityRecognitionClient.requestActivityTransitionUpdates(request, mPendingIntent)

            task.addOnSuccessListener(
                object : OnSuccessListener<Void> {
                    override fun onSuccess(p0: Void?) {
                    }
                })

            task.addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(p0: Exception) {
                    }
                })
        }
    }

    private fun activityRecognitionPermissionApproved(): Boolean {

        val runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

        return !runningQOrLater ||
                return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
    }
}