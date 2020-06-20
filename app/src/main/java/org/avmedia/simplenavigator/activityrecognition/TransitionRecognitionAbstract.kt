package org.avmedia.simplenavigator.activityrecognition

import android.content.Context

abstract class TransitionRecognitionAbstract {
    abstract fun startTracking(context: Context)
    abstract fun stopTracking()
}