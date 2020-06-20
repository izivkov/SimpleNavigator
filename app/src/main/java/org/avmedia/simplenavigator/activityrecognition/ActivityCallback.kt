package org.avmedia.simplenavigator.activityrecognition

import kotlin.properties.Delegates.observable

object ActivityCallback {

    lateinit var callback: ActivityCallbackAbstract

    var event: String by observable("UNKNOWN") { _, oldValue, newValue ->
        callback.update(oldValue, newValue)
    }
}