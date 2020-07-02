package org.avmedia.simplenavigator

import android.content.Context
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import org.avmedia.simplenavigator.firebase.FirebaseConnection
import org.avmedia.simplenavigator.nearby.NearbyConnection

object PairConnection {

    val connectionEventProcessor: PublishProcessor<ConnectionProgressEvents> =
        PublishProcessor.create()
    val connectionEventFlowable = connectionEventProcessor as Flowable<ConnectionProgressEvents>

    fun onNext(event: ConnectionProgressEvents) {
        return connectionEventProcessor.onNext(event)
    }

    fun init() {
        NearbyConnection.init(connectionEventProcessor)
        FirebaseConnection.init(connectionEventProcessor)
        FirebaseConnection.getToken()
    }

    fun nearbyConnect(context: Context) {
        NearbyConnection.connect(context)
    }
}