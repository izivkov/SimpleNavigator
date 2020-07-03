package org.avmedia.simplenavigator.nearby

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import org.avmedia.simplenavigator.EventProcessor
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import kotlin.math.absoluteValue

/** Activity controlling the Rock Paper Scissors game  */
object NearbyConnection {
    private lateinit var context: Context
    private val connectionName = "simpleNavigatorConnection"

    // Our handle to Nearby Connections
    private var connectionsClient: ConnectionsClient? = null
    private var pairedDeviceEndpointId: String? = null
    private var pairedDeviceName: String? = null
    private var myUniqueID: Int = SecureRandom.getInstance("SHA1PRNG").nextInt().absoluteValue

    fun init() {
    }

    // Callbacks for receiving payloads
    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(
            endpointId: String,
            payload: Payload
        ) {
            val payloadStr = String(
                payload.asBytes()!!,
                StandardCharsets.UTF_8
            )
            val topic = "" + Integer(payloadStr).toInt()

            val event: EventProcessor.ProgressEvents =
                EventProcessor.ProgressEvents.NearbyConnectionPayload

            event.payload = topic
            EventProcessor.onNext(event)
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) {
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
                        Log.d(TAG, "OnFailureListener requestConnection: " + it.toString())

                        if (it.message!!.contains("8012: STATUS_ENDPOINT_IO_ERROR")) {
                            // retry. this usually helps...
                            Log.d("NearbyConnection", "Got 8012. Reconnecting...")

                            connectionsClient!!.stopDiscovery()
                            connectionsClient!!.stopAdvertising()

                            NearbyConnection.connect(context)
                        }

                        if (it.message!!.contains("8002: STATUS_ALREADY_DISCOVERING")) {
                            Log.d("NearbyConnection", "Got 8002: STATUS_ALREADY_DISCOVERING")
                            connectionsClient!!.stopDiscovery()
                        }

                        if (it.message!!.contains("8003: STATUS_ALREADY_CONNECTED_TO_ENDPOINT")) {
                            Log.d("NearbyConnection", "Got 8003. Do nothing...")
                        }

                        if (it.message!!.contains("8007: STATUS_BLUETOOTH_ERROR")) {
                            Log.d("NearbyConnection", "Got 8007. Reconnecting...")
                            connectionsClient!!.stopDiscovery()
                            connectionsClient!!.stopAdvertising()

                            NearbyConnection.connect(context)
                        }
                    }
            }

            override fun onEndpointLost(endpointId: String) {
                EventProcessor.onNext(EventProcessor.ProgressEvents.NearbyConnectionFailed)
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
                    Log.i(
                        NearbyConnection.TAG,
                        "onConnectionResult: connection successful"
                    )
                    connectionsClient!!.stopDiscovery()
                    connectionsClient!!.stopAdvertising()
                    pairedDeviceEndpointId = endpointId
                    sendMessage("" + myUniqueID)

                    EventProcessor.onNext(EventProcessor.ProgressEvents.NearbyConnectionSuccess)
                } else {
                    abortConnection()

                    Log.i(
                        NearbyConnection.TAG,
                        "onConnectionResult: connection failed"
                    )
                    EventProcessor.onNext(EventProcessor.ProgressEvents.NearbyConnectionFailed)
                }
            }

            override fun onDisconnected(endpointId: String) {
                Log.i(
                    NearbyConnection.TAG,
                    "onDisconnected: disconnected from the pairing device"
                )
                EventProcessor.onNext(EventProcessor.ProgressEvents.NearbyConnectionDisconnected)
            }
        }

    fun connect(context: Context) {
        this.context = context
        connectionsClient = Nearby.getConnectionsClient(context)

        startAdvertising()
        startDiscovery()
    }

    /** Disconnects from the opponent and reset the UI.  */
    fun disconnect() {
        connectionsClient!!.stopDiscovery()
        connectionsClient!!.stopAdvertising()

        connectionsClient!!.disconnectFromEndpoint(pairedDeviceEndpointId!!)
        connectionsClient!!.stopAllEndpoints()
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
        Strategy.P2P_CLUSTER
}
