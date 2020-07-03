package org.avmedia.simplenavigator

import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor

object EventProcessor {
    private val eventProcessor: PublishProcessor<ProgressEvents> =
        PublishProcessor.create()

    val connectionEventFlowable = EventProcessor.eventProcessor as Flowable<ProgressEvents>

    fun onNext(e: ProgressEvents) {
        return eventProcessor.onNext(e)
    }

    open class ProgressEvents(var payload: String = "") {

        object Init : ProgressEvents()
        object CloseAllConnection : ProgressEvents()

        object NearbyConnecting : ProgressEvents()
        object NearbyConnectionFailed : ProgressEvents()
        object NearbyConnectionSuccess : ProgressEvents()
        object NearbyConnectionDisconnected : ProgressEvents()
        object NearbyConnectionPayload : ProgressEvents()

        object SubscribedToFirebaseSuccess : ProgressEvents()
        object SubscribedToFirebaseFailed : ProgressEvents()
        object UnSubscribedToFirebaseSuccess : ProgressEvents()
        object UnSubscribedToFirebaseFailed : ProgressEvents()

        object ActivityChangeEvent : ProgressEvents()
    }
}