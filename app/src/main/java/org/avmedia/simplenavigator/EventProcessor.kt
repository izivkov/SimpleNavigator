package org.avmedia.simplenavigator

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor

object EventProcessor {
    private val eventProcessor: PublishProcessor<ProgressEvents> =
        PublishProcessor.create()

    val connectionEventFlowable = EventProcessor.eventProcessor as Flowable<ProgressEvents>

    fun onNext(e: ProgressEvents) {
        if (eventProcessor.hasSubscribers()) {
            return eventProcessor.onNext(e)
        } else {
            Log.d("EventProcessor:onNext", "----------- No subscribers")
        }
    }

    open class ProgressEvents(var payload: String = "") {

        object Init : ProgressEvents()
        object CloseAllConnection : ProgressEvents()

        object NearbyConnecting : ProgressEvents()
        object NearbyConnectionFailed : ProgressEvents()
        object NearbyConnectionSuccess : ProgressEvents()
        object NearbyConnectionDisconnected : ProgressEvents()
        object NearbyConnectionPayload : ProgressEvents()

        object SubscribingToFirebaseTopic : ProgressEvents()
        object SubscribedToFirebaseSuccess : ProgressEvents()
        object SubscribedToFirebaseFailed : ProgressEvents()
        object FirebaseMessageReceived : ProgressEvents()
        object UnSubscribedToFirebaseSuccess : ProgressEvents()
        object UnSubscribedToFirebaseFailed : ProgressEvents()
        object PairedDeviceLocation : ProgressEvents()
        object ActivityChangeEvent : ProgressEvents()
        object StepsChangeEvent : ProgressEvents()
    }
}