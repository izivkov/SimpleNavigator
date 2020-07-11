package org.avmedia.simplenavigator

import android.util.Log
import io.reactivex.disposables.Disposable
import java.util.*

object TimeoutManager {

    var timer: TimerTask? = null

    val nearbyConnectingMonitor = object : TimerTask() {
        override fun run() {
            val event: EventProcessor.ProgressEvents =
                EventProcessor.ProgressEvents.CloseAllConnection

            PairConnection.onNext(event)
        }
    }

    val nearbyConnectedSuccessMonitor = object : TimerTask() {
        override fun run() {
            val event: EventProcessor.ProgressEvents =
                EventProcessor.ProgressEvents.CloseAllConnection

            PairConnection.onNext(event)
        }
    }

    val subscribingToFirebaseTopicMonitor = object : TimerTask() {
        override fun run() {
            val event: EventProcessor.ProgressEvents =
                EventProcessor.ProgressEvents.CloseAllConnection

            PairConnection.onNext(event)
        }
    }

    val subscribedToFirebaseTopicMonitor = object : TimerTask() {
        override fun run() {
            val event: EventProcessor.ProgressEvents =
                EventProcessor.ProgressEvents.CloseAllConnection

            PairConnection.onNext(event)
        }
    }

    fun createTimerSubscription(): Disposable =
        EventProcessor.connectionEventFlowable
            .observeOn(PairConnection.whereToRun)
            .doOnNext {
                when (it) {
                    // Nearby
                    EventProcessor.ProgressEvents.NearbyConnecting -> {
                        startTimeout(1 * 60 * 1000, nearbyConnectingMonitor)
                    }

                    EventProcessor.ProgressEvents.NearbyConnectionSuccess -> {
                        nearbyConnectingMonitor.cancel()
                        startTimeout(30 * 1000, nearbyConnectedSuccessMonitor)
                    }

                    EventProcessor.ProgressEvents.NearbyConnectionPayload -> {
                        nearbyConnectedSuccessMonitor.cancel()
                    }

                    // Firebase
                    EventProcessor.ProgressEvents.SubscribingToFirebaseTopic -> {
                        startTimeout(30 * 1000, subscribingToFirebaseTopicMonitor)
                    }

                    EventProcessor.ProgressEvents.SubscribedToFirebaseSuccess -> {

                        subscribingToFirebaseTopicMonitor.cancel()
                        startTimeout(120 * 1000, subscribedToFirebaseTopicMonitor)
                    }

                    EventProcessor.ProgressEvents.FirebaseMessageReceived -> {
                        subscribedToFirebaseTopicMonitor.cancel()
                    }
                }
            }
            .subscribe({ }, { throwable ->
                Log.d("createTimerSubscription", "Got error on subscribe: $throwable")
            })

    private fun startTimeout(delay: Long, monitor: TimerTask) {
        val timer = Timer()
        timer.schedule(monitor, delay)
    }
}

