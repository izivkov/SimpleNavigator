package org.avmedia.simplenavigator.firebase

data class ShareLocationMessage(
    val longitude: Double,
    val latitude: Double,
    val activity: String,
    val userName: String
)