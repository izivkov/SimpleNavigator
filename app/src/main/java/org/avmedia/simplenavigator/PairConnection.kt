package org.avmedia.simplenavigator

import android.content.Context
import io.reactivex.disposables.Disposable
import org.avmedia.simplenavigator.firebase.FirebaseConnection
import org.avmedia.simplenavigator.firebase.ShareLocationMessage
import org.avmedia.simplenavigator.nearby.NearbyConnection

object PairConnection {

    var currentTopic: String = ""

    enum class ConnectionStatus(val state: Int) {
        DISCONNECTED(0), NEARBY_CONNECTING(1), NEARBY_CONNECTED(2), SUBSCRIBING_TO_TOPIC(3), SUBSCRIBED_TO_TOPIC(
            4
        ),
        GOT_FIRST_PAYLOAD(5)
    }

    var connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED

    fun onNext(event: EventProcessor.ProgressEvents) {
        return EventProcessor.onNext(event)
    }

    fun init() {
        NearbyConnection.init()
        FirebaseConnection.init()
        FirebaseConnection.getToken()

        createAppEventsSubscription()
    }

    fun send(shareLocationMsg: ShareLocationMessage) {
        FirebaseConnection.send(shareLocationMsg)
    }

    fun nearbyConnect(context: Context) {
        connectionStatus = ConnectionStatus.NEARBY_CONNECTING
        NearbyConnection.connect(context)
    }

    fun disconnect() {

        val event: EventProcessor.ProgressEvents =
            EventProcessor.ProgressEvents.CloseAllConnection

        onNext(event)
    }

    fun suspendMessages() {
        if (!currentTopic.isBlank()) {
            FirebaseConnection.unsubscribe(currentTopic)
        }
    }

    fun resumeMessages() {
        if (!currentTopic.isBlank()) {
            FirebaseConnection.unsubscribe(currentTopic)
        }
    }

    private fun createAppEventsSubscription(): Disposable =
        EventProcessor.connectionEventFlowable
            .doOnNext {
                when (it) {
                    // Nearby
                    EventProcessor.ProgressEvents.NearbyConnecting -> {
                        connectionStatus = ConnectionStatus.NEARBY_CONNECTING
                    }
                    EventProcessor.ProgressEvents.NearbyConnectionPayload -> {
                        currentTopic = "" + it.payload
                        connectionStatus = ConnectionStatus.NEARBY_CONNECTED
                        NearbyConnection.disconnect()
                        FirebaseConnection.subscribe(currentTopic)
                    }

                    // Firebase
                    EventProcessor.ProgressEvents.SubscribedToFirebaseSuccess -> {
                        connectionStatus = ConnectionStatus.SUBSCRIBED_TO_TOPIC
                    }

                    // General
                    EventProcessor.ProgressEvents.CloseAllConnection -> {
                        NearbyConnection.disconnect()
                        FirebaseConnection.unsubscribe(currentTopic)
                        connectionStatus = ConnectionStatus.DISCONNECTED
                    }
                }
            }
            .subscribe()

}