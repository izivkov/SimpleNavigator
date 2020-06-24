package org.avmedia.simplenavigator.activityrecognition

import kotlin.properties.Delegates.observable

object ActivityCallback {

    lateinit var callback: ActivityCallbackAbstract

    var activityTransition: String by observable("UNKNOWN") { _, _, newValue ->
        callback.update(newValue)
    }
}