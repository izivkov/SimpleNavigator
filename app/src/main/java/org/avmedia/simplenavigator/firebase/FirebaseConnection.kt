package org.avmedia.simplenavigator.firebase

import android.util.Log
import androidx.annotation.Keep
import com.github.kittinunf.fuel.httpPost
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.avmedia.simplenavigator.EventProcessor

object FirebaseConnection {
    val TAG = "FirebaseConnection"
    var token: String? = ""
    val apiId =
    // dev
    // "AAAAVpOoEmA:APA91bHNh7musupADwmDjQgxxzEk2DWIFDj0UWpNnqs2--7nBra__i7sBKavgMdnQ1AlxZnyVAne0q4t_V0vIH5iXJJZq2vGoDGoFKh_ZfGiuv0qw1GpNifDVifQCLYbL6_dfoKkhuG8"

        // release
        "AAAAVpOoEmA:APA91bEU38i90opHOMjkyyMOVlKhT3mor0e4KCmmWOuHGC_4D1Qkes_ivBgjvngPgqTtuVuPzBmgnCvsdYw9tkm-BGNpcBLQnPiUojl9q7SM71QDIGPqHi3E42jK9UxNjYCOYNMgVXm2"
    val messageURL = "https://fcm.googleapis.com/fcm/send"

    fun init() {
    }

    fun getToken() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                token = task.result?.token
            })
    }

    fun subscribe(topic: String) {
        EventProcessor.onNext(EventProcessor.ProgressEvents.SubscribingToFirebaseTopic)

        var subscription = FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                var msg = "Subscription FAILED"
                if (task.isSuccessful) {
                    msg = "Subscription SUCCESSFUL"
                    EventProcessor.onNext(EventProcessor.ProgressEvents.SubscribedToFirebaseSuccess)
                    Log.d(
                        "FirebaseConnection",
                        "############ sending SubscribedToFirebaseSuccess event"
                    )
                } else {
                    EventProcessor.onNext(EventProcessor.ProgressEvents.SubscribedToFirebaseFailed)
                }
            }
    }

    fun unsubscribe(topic: String) {
        var subscription = FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                var msg = "Un-Subscription FAILED"
                if (!task.isSuccessful) {
                    Log.d("unsubscribe", "Failed")
                    EventProcessor.onNext(EventProcessor.ProgressEvents.UnSubscribedToFirebaseFailed)
                } else {
                    Log.d("unsubscribe", "Sucess")
                    msg = "Un-Subscription SUCCESSFUL"
                    EventProcessor.onNext(EventProcessor.ProgressEvents.UnSubscribedToFirebaseSuccess)
                }
            }
    }

    @Keep
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

    fun send(topic: String, shareLocationMsg: ShareLocationMessage) {

        val msg = Msg(shareLocationMsg, topic)
        var bodyJson = Gson().toJson(msg)

        Log.d("send", "topic: ${topic}")

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
//                Log.d("send", "***request: $request")
//                Log.d("send", "***response: $response")
                Log.d("send", "***result: $result")
                // in release version getting: Failure: HTTP Exception 400 Bad Request
            }


        httpAsync.join()
    }
}