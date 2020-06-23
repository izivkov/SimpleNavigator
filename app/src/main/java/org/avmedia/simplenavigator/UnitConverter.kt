package org.avmedia.simplenavigator

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import java.io.IOException
import java.lang.String.format
import java.util.*

class UnitConverter {
    private var context: Context? = null
    private var location: Location? = null
    private var unitType: UNIT_TYPE? = null
    var isInitialized: Boolean = false

    enum class UNIT_TYPE {
        IMPERIAL,
        METRIC
    }

    val METERS_TO_FEET = 3.28084
    val KM_TO_MILES = 0.62137121212121f

    fun init (context: Context, location: Location) {
        this.context = context
        this.location = location
        this.unitType = getUnits ()
        isInitialized = true
    }

    private fun getCountry(): String {
        if (context == null || location == null) {
            return ""
        }
        val gcd = Geocoder(context, Locale.getDefault())

        try {
            val addresses: List<Address> = gcd.getFromLocation(location!!.latitude, location!!.longitude, 1)
            var countryCode = ""

            if (addresses.size > 0) {
                countryCode = addresses.get(0).countryCode
            }
            return countryCode
        } catch(e: IOException) {
            return ""
        }
    }

    private fun getUnits(): UNIT_TYPE {
        val country = getCountry()

        when(country) {
            "US", "UK", "LR", "MM" -> return UNIT_TYPE.IMPERIAL
            else -> return UNIT_TYPE.METRIC
        }
    }

    fun formatMeters (meters: Double, label: String): String {
        var value: Double = meters
        var unit = "m"
        if (unitType == UNIT_TYPE.IMPERIAL) {
            value *= METERS_TO_FEET
            unit = "ft"
        }
        return format("%s %.${0}f %s", label, value, unit)
    }

    fun formatSpeed (kmPerHour: Float, label: String): String {
        var value: Float = kmPerHour
        var unit = "km/h"
        if (unitType == UNIT_TYPE.IMPERIAL) {
            value *= KM_TO_MILES
            unit = "mph"
        }
        return format("%s %.${0}f %s", label, value, unit)
    }

    fun formatKm (kms: Float, label: String): String {
        var value: Float = kms
        var unit = "km"
        if (unitType == UNIT_TYPE.IMPERIAL) {
            value *= KM_TO_MILES
            unit = "miles"
        }
        return format("%s %.${1}f %s", label, value, unit)
    }
}