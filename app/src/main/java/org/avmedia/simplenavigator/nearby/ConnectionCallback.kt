package org.avmedia.simplenavigator.nearby

import kotlin.properties.Delegates.observable

object ConnectionCallback {

    lateinit var callback: ConnectionsCallbackAbstract

    var connectionStatus: String by observable("UNKNOWN") { _, _, newValue ->
        callback.update(newValue)
    }
}