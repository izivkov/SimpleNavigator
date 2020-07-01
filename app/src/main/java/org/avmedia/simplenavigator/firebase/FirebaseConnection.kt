package org.avmedia.simplenavigator.firebase

import android.util.Log
import com.github.kittinunf.fuel.httpPost
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import org.avmedia.simplenavigator.nearby.NearbyConnection

object FirebaseConnection {
    val TAG = "FirebaseConnection"
    var token: String? = ""
    val apiId =
        "AAAAVpOoEmA:APA91bHNh7musupADwmDjQgxxzEk2DWIFDj0UWpNnqs2--7nBra__i7sBKavgMdnQ1AlxZnyVAne0q4t_V0vIH5iXJJZq2vGoDGoFKh_ZfGiuv0qw1GpNifDVifQCLYbL6_dfoKkhuG8"
    val messageURL = "https://fcm.googleapis.com/fcm/send"

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
        Log.d(TAG, "*************** subscribing to topic: $topic")
        var subscription = FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                var msg = "Subscription SUCCESSFUL"
                if (!task.isSuccessful) {
                    msg = "Subscription FAILED"
                }
                Log.d(TAG, msg)
            }
    }

    data class Msg(
        val data: ShareLocationMessage,
        val topic: String
    ) {
        val to = "/topics/" + topic
    }

    fun send(shareLocationMsg: ShareLocationMessage) {

        if ("".equals(NearbyConnection.otherId)) {
            return
        }

        /*
         {
          "data": {
            "longitude": 34.5,
            "latitude": -73.3,
            "activity": "STILL"
          },
          "to": "/topics/123456"
        }
        */

        val msg = Msg(shareLocationMsg, NearbyConnection.otherId)
        var bodyJson = Gson().toJson(msg)
        Log.d(TAG, ">>>>>>>>>>>>>>> sending to topic ${msg.topic}")

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