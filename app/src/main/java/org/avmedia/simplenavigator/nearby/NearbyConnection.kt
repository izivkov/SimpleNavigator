package org.avmedia.simplenavigator.nearby

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets


/** Activity controlling the Rock Paper Scissors game  */
object NearbyConnection {
    private lateinit var context: Context
    private val connectionName = "simpleNavigatorConnection"

    // Our handle to Nearby Connections
    private var connectionsClient: ConnectionsClient? = null
    private var pairedDeviceEndpointId: String? = null
    private var pairedDeviceName: String? = null
    var connecting: Boolean = false

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
                    connectionName,
                    endpointId,
                    connectionLifecycleCallback
                ).addOnSuccessListener {
                    Log.i(TAG, "OnSuccessListener requestConnection")
                }
                    .addOnFailureListener {
                        Log.i(TAG, "OnFailureListener requestConnection: " + it.toString())

                        connectionsClient!!.stopDiscovery()
                        connectionsClient!!.stopAdvertising()

                        // retry. this usually helps...
                        NearbyConnection.connect(context)
                    }
            }

            override fun onEndpointLost(endpointId: String) {
                Log.i(
                    NearbyConnection.TAG,
                    "onEndpointLost: endpoint lost."
                )
                ConnectionCallback.connectionStatus = "FAIL"
            }
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
                pairedDeviceName = connectionInfo.endpointName
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {
                if (result.status.isSuccess) {
                    connecting = false

                    Log.i(
                        NearbyConnection.TAG,
                        "onConnectionResult: connection successful"
                    )
                    connectionsClient!!.stopDiscovery()
                    connectionsClient!!.stopAdvertising()
                    pairedDeviceEndpointId = endpointId
                    sendMessage(getUserName())
                    ConnectionCallback.connectionStatus = "SUCCESS"

                    shutdownConnection()
                } else {
                    abortConnection()

                    Log.i(
                        NearbyConnection.TAG,
                        "onConnectionResult: connection failed"
                    )
                }
            }

            override fun onDisconnected(endpointId: String) {
                Log.i(
                    NearbyConnection.TAG,
                    "onDisconnected: disconnected from the pairing device"
                )
            }
        }

    fun getUserName(): String {

        return ""
    }

    fun shutdownConnection() {
        Log.d(
            "shutdownConnection",
            "Shutting down, connectionsClient: " + connectionsClient.toString()
        )

        if (connectionsClient != null) {
            connectionsClient!!.disconnectFromEndpoint(pairedDeviceEndpointId!!)
            connectionsClient!!.stopAllEndpoints()
        }
    }

    fun stopTryingToConnect() {
        connectionsClient!!.stopDiscovery()
        connectionsClient!!.stopAdvertising()
        abortConnection()
    }

    fun connect(context: Context) {
        this.context = context
        connectionsClient = Nearby.getConnectionsClient(context)
        connecting = true

        getUserName()

        startDiscovery()
        startAdvertising()
    }

    fun onStop() {
        connectionsClient!!.stopAllEndpoints()
    }

    /** Disconnects from the opponent and reset the UI.  */
    fun disconnect(view: View?) {
        connectionsClient!!.disconnectFromEndpoint(pairedDeviceEndpointId!!)
    }

    /** Starts looking for other players using Nearby Connections.  */
    fun startDiscovery(): Unit {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient!!.startDiscovery(
            context.packageName, endpointDiscoveryCallback,
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
                abortConnection()
            }
    }


    /** Broadcasts our presence using Nearby Connections so other players can find us.  */
    private fun startAdvertising() {
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.

        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient!!.startAdvertising(
            connectionName, context.packageName, connectionLifecycleCallback,
            AdvertisingOptions.Builder()
                .setStrategy(STRATEGY)
                .build()
        )
            .addOnSuccessListener {
                Log.d("startAdvertising", "We're advertising")
                // We're advertising!
            }.addOnFailureListener {
                abortConnection()
                Log.d(
                    "startAdvertising",
                    "We were unable to start advertising."
                )
            }
    }

    private fun abortConnection() {
        connecting = false
        ConnectionCallback.connectionStatus = "FAIL"
    }

    /** Sends the user's selection of rock, paper, or scissors to the opponent.  */
    private fun sendMessage(message: String) {
        connectionsClient!!.sendPayload(
            pairedDeviceEndpointId!!,
            Payload.fromBytes(message.toByteArray(StandardCharsets.UTF_8))
        )
    }

    private const val TAG = "NearbyConnection"
    private val STRATEGY =
        Strategy.P2P_STAR
}