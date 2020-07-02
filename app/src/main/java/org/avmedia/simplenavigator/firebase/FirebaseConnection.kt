package org.avmedia.simplenavigator.firebase

import android.util.Log
import com.github.kittinunf.fuel.httpPost
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.reactivex.processors.PublishProcessor
import org.avmedia.simplenavigator.ConnectionProgressEvents
import org.avmedia.simplenavigator.nearby.NearbyConnection

object FirebaseConnection {
    val TAG = "FirebaseConnection"
    var token: String? = ""
    val apiId =
        "AAAAVpOoEmA:APA91bHNh7musupADwmDjQgxxzEk2DWIFDj0UWpNnqs2--7nBra__i7sBKavgMdnQ1AlxZnyVAne0q4t_V0vIH5iXJJZq2vGoDGoFKh_ZfGiuv0qw1GpNifDVifQCLYbL6_dfoKkhuG8"
    val messageURL = "https://fcm.googleapis.com/fcm/send"
    lateinit var connectionEventProcessor: PublishProcessor<ConnectionProgressEvents>

    fun init(connectionEventProcessor: PublishProcessor<ConnectionProgressEvents>) {
        this.connectionEventProcessor = connectionEventProcessor
    }

    fun getToken() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                token = task.result?.token
            })
    }

    fun subscribe(topic: String) {
        var subscription = FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                var msg = "Subscription FAILED"
                if (!task.isSuccessful) {
                    connectionEventProcessor.onNext(ConnectionProgressEvents.SubscribedToFirebaseFailed)
                } else {
                    msg = "Subscription SUCCESSFUL"
                    connectionEventProcessor.onNext(ConnectionProgressEvents.SubscribedToFirebaseSuccess)
                }
            }
    }

    data class Msg(
        val data: ShareLocationMessage,
        val topic: String,
        var webpush: Webpush = Webpush(UrgencyHeader()),
        var apns: Apns = Apns(PriorityHeader())
    ) {
        data class UrgencyHeader(var Urgency: String = "high")
        data class Webpush(var headers: UrgencyHeader)

        data class PriorityHeader(
            @SerializedName("apns-priority")
            var priority: String = "10"
        )

        data class Apns(var headers: PriorityHeader)

        val to = "/topics/" + topic
    }

    fun send(shareLocationMsg: ShareLocationMessage) {

        if ("".equals(NearbyConnection.otherId)) {
            return
        }

        val msg = Msg(shareLocationMsg, NearbyConnection.otherId)
        var bodyJson = Gson().toJson(msg)

        val httpAsync = messageURL
            .httpPost()
            .body(bodyJson)
            .header(mapOf("content-type" to "application/json"))
            .header(
                mapOf(
                    "authorization" to
                            "key=" + apiId
                )
            )
            .responseString { request, response, result ->
            }

        httpAsync.join()
    }
}