package org.avmedia.simplenavigator

open class ConnectionProgressEvents(var payload: String? = "") {
    object Init : ConnectionProgressEvents()
    object SubscribedToFirebaseSuccess : ConnectionProgressEvents()
    object SubscribedToFirebaseFailed : ConnectionProgressEvents()
    object NearbyConnectionFailed : ConnectionProgressEvents()
    object NearbyConnectionSuccess : ConnectionProgressEvents()
    object NearbyConnectionDisconnected : ConnectionProgressEvents()
    object ActivityChangeEvent : ConnectionProgressEvents()
}