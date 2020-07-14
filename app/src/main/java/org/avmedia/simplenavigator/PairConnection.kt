package org.avmedia.simplenavigator

import android.content.Context
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.avmedia.simplenavigator.firebase.FirebaseConnection
import org.avmedia.simplenavigator.firebase.ShareLocationMessage
import org.avmedia.simplenavigator.nearby.NearbyConnection
import java.util.*

object PairConnection {

    var currentTopic: String = ""
    var myTopic: String =
        UUID.randomUUID().toString()
    // ""+SecureRandom.getInstance("SHA1PRNG").nextInt().absoluteValue

    enum class ConnectionStatus(val state: Int) {
        DISCONNECTED(0), NEARBY_CONNECTING(1), NEARBY_CONNECTED(2), SUBSCRIBING_TO_TOPIC(3), SUBSCRIBED_TO_TOPIC(
            4
        ),
        GOT_FIRST_PAYLOAD(5)
    }

    var whereToRun = AndroidSchedulers.mainThread()
    // Schedulers.io()
    // Schedulers.single()
    // Schedulers.computation()
    // Schedulers.newThread()
    // AndroidSchedulers.mainThread()

    var connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED
    lateinit var eventLoop: Disposable

    fun onNext(event: EventProcessor.ProgressEvents) {
        return EventProcessor.onNext(event)
    }

    fun init() {
        NearbyConnection.init()
        FirebaseConnection.init()
        FirebaseConnection.getToken()
    }

    fun send(shareLocationMsg: ShareLocationMessage) {
        FirebaseConnection.send(myTopic, shareLocationMsg)
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
}