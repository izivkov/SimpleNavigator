package org.avmedia.simplenavigator

import com.google.android.gms.maps.GoogleMap
import org.avmedia.simplenavigator.utils.SingletonHolder


// data class Trip(var lastLocation: Location = Location("dummyprovider")) {
data class CalorieCounter(var googleMap: GoogleMap) {

    companion object : SingletonHolder<CalorieCounter, GoogleMap>(::CalorieCounter);

    init {
//        val readRequest: DataReadRequest = Builder()
//            .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
//            .bucketByActivityType(1, TimeUnit.SECONDS)
//            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
//            .build()
    }
}


