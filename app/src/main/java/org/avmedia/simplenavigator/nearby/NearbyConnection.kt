package org.avmedia.simplenavigator.nearby

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets

/** Activity controlling the Rock Paper Scissors game  */
object NearbyConnection {
    // Our handle to Nearby Connections
    private var connectionsClient: ConnectionsClient? = null
    private var opponentEndpointId: String? = null
    private var opponentName: String? = null

    // Callbacks for receiving payloads
    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(
            endpointId: String,
            payload: Payload
        ) {
            Log.d(
                "onPayloadReceived", "payload: " + String(
                    payload.asBytes()!!,
                    StandardCharsets.UTF_8
                )
            )
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) {
            Log.d("onPayloadTransferUpdate", "update: " + update.toString())
        }
    }

    // Callbacks for finding other devices
    private val endpointDiscoveryCallback: EndpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {
                Log.i(
                    NearbyConnection.TAG,
                    "onEndpointFound: endpoint found, connecting"
                )
                connectionsClient!!.requestConnection(
                    "connectionName",
                    endpointId,
                    connectionLifecycleCallback
                )
            }

            override fun onEndpointLost(endpointId: String) {}
        }

    // Callbacks for connections to other devices
    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {
                Log.i(
                    NearbyConnection.TAG,
                    "onConnectionInitiated: accepting connection"
                )
                connectionsClient!!.acceptConnection(endpointId, payloadCallback)
                opponentName = connectionInfo.endpointName
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {
                if (result.status.isSuccess) {
                    Log.i(
                        NearbyConnection.TAG,
                        "onConnectionResult: connection successful"
                    )
                    connectionsClient!!.stopDiscovery()
                    connectionsClient!!.stopAdvertising()
                    opponentEndpointId = endpointId
                    sendMessage()
                } else {
                    Log.i(
                        NearbyConnection.TAG,
                        "onConnectionResult: connection failed"
                    )
                }
            }

            override fun onDisconnected(endpointId: String) {
                Log.i(
                    NearbyConnection.TAG,
                    "onDisconnected: disconnected from the opponent"
                )
            }
        }

    fun init(context: Context) {
        connectionsClient = Nearby.getConnectionsClient(context)

        startDiscovery()
        startAdvertising()
    }

    fun onStop() {
        connectionsClient!!.stopAllEndpoints()
    }

    /** Disconnects from the opponent and reset the UI.  */
    fun disconnect(view: View?) {
        connectionsClient!!.disconnectFromEndpoint(opponentEndpointId!!)
    }

    /** Starts looking for other players using Nearby Connections.  */
    fun startDiscovery(): Unit {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient!!.startDiscovery(
            "org.avmedia.simplenavigator", endpointDiscoveryCallback,
            DiscoveryOptions.Builder()
                .setStrategy(STRATEGY)
                .build()
        )
            .addOnSuccessListener {
                Log.d("startDiscovery", "We started discovery OK")
                // We're advertising!
            }.addOnFailureListener { e ->
                Log.d(
                    "startDiscovery",
                    "We were unable to start startDiscovery. Error: $e"
                )
            }
    }


    /** Broadcasts our presence using Nearby Connections so other players can find us.  */
    private fun startAdvertising() {
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.

        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient!!.startAdvertising(
            "codeName", "org.avmedia.simplenavigator", connectionLifecycleCallback,
            AdvertisingOptions.Builder()
                .setStrategy(STRATEGY)
                .build()
        )
            .addOnSuccessListener {
                Log.d("startAdvertising", "We're advertising")
                // We're advertising!
            }.addOnFailureListener {
                Log.d(
                    "startAdvertising",
                    "We were unable to start advertising."
                )
            }
    }

    /** Sends the user's selection of rock, paper, or scissors to the opponent.  */
    private fun sendMessage() {
        connectionsClient!!.sendPayload(
            opponentEndpointId!!,
            Payload.fromBytes(android.os.Build.MODEL.toByteArray(StandardCharsets.UTF_8))
        )
    }

    private const val TAG = "RockPaperScissors"
    private val STRATEGY =
        Strategy.P2P_STAR
}