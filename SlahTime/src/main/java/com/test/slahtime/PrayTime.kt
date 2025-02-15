import java.util.*

class PrayTime {

    // ---------------------- Global Variables --------------------
    private var asrJuristic: Int = 0 // Juristic method for Asr
    private var dhuhrMinutes: Int = 0 // minutes after mid-day for Dhuhr
    private var adjustHighLats: Int = 0 // adjusting method for higher latitudes
    private var timeFormat: Int = 0 // time format
    private var lat: Double = 0.0 // latitude
    private var lng: Double = 0.0 // longitude
    private var timeZone: Double = 0.0 // time-zone
    private var JDate: Double = 0.0 // Julian date

    // Adjusting Methods for Higher Latitudes
    private val None = 0 // No adjustment
    private val MidNight = 1 // middle of night
    private val OneSeventh = 2 // 1/7th of night
    private val AngleBased = 3 // angle/60th of night

    // Time Formats
    private val Time24 = 0 // 24-hour format
    private val Time12 = 1 // 12-hour format
    private val Time12NS = 2 // 12-hour format with no suffix
    private val Floating = 3 // floating point number

    // Time Names
    val timeNames = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Sunset", "Maghrib", "Isha")
    private val InvalidTime = "-----" // The string used for invalid times

    // --------------------- Technical Settings --------------------
    private var numIterations: Int = 1 // number of iterations needed to compute times

    // ------------------- Calc Method Parameters --------------------
    private lateinit var methodParams: PolygonSearchModel

    // Tuning offsets {fajr, sunrise, dhuhr, asr, sunset, maghrib, isha}
    private val offsets = IntArray(7)

    init {
        // Initialize offsets
        offsets.fill(0)
    }

    // ---------------------- Trigonometric Functions -----------------------
    // Range reduce angle in degrees.
    private fun fixangle(a: Double): Double {
        var angle = a - 360 * Math.floor(a / 360.0)
        if (angle < 0) angle += 360
        return angle
    }

    // Range reduce hours to 0..23
    private fun fixhour(a: Double): Double {
        var hour = a - 24.0 * Math.floor(a / 24.0)
        if (hour < 0) hour += 24
        return hour
    }

    // Radian to degree
    private fun radiansToDegrees(alpha: Double): Double {
        return alpha * 180.0 / Math.PI
    }

    // Degree to radian
    private fun degreesToRadians(alpha: Double): Double {
        return alpha * Math.PI / 180.0
    }

    // Degree sin
    private fun dsin(d: Double): Double {
        return Math.sin(degreesToRadians(d))
    }

    // Degree cos
    private fun dcos(d: Double): Double {
        return Math.cos(degreesToRadians(d))
    }

    // Degree tan
    private fun dtan(d: Double): Double {
        return Math.tan(degreesToRadians(d))
    }

    // Degree arcsin
    private fun darcsin(x: Double): Double {
        return radiansToDegrees(Math.asin(x))
    }

    // Degree arccos
    private fun darccos(x: Double): Double {
        return radiansToDegrees(Math.acos(x))
    }

    // Degree arctan
    private fun darctan(x: Double): Double {
        return radiansToDegrees(Math.atan(x))
    }

    // Degree arctan2
    private fun darctan2(y: Double, x: Double): Double {
        return radiansToDegrees(Math.atan2(y, x))
    }

    // Degree arccot
    private fun darccot(x: Double): Double {
        return radiansToDegrees(Math.atan2(1.0, x))
    }

    // ---------------------- Time-Zone Functions -----------------------
    // Compute local time-zone for a specific date
    private fun getTimeZone1(): Double {
        val timez = TimeZone.getDefault()
        return (timez.rawOffset / 1000.0) / 3600
    }

    // Compute base time-zone of the system
    private fun getBaseTimeZone(): Double {
        val timez = TimeZone.getDefault()
        return (timez.rawOffset / 1000.0) / 3600
    }

    // Detect daylight saving in a given date
    private fun detectDaylightSaving(): Double {
        val timez = TimeZone.getDefault()
        return timez.dstSavings.toDouble()
    }

    // ---------------------- Julian Date Functions -----------------------
    // Calculate Julian date from a calendar date
    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val A = Math.floor(y / 100.0)
        val B = 2 - A + Math.floor(A / 4.0)
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + B - 1524.5
    }

    // Convert a calendar date to Julian date (second method)
    private fun calcJD(year: Int, month: Int, day: Int): Double {
        val J1970 = 2440588.0
        val date = Date(year, month - 1, day)
        val ms = date.time // # of milliseconds since midnight Jan 1, 1970
        val days = Math.floor(ms / (1000.0 * 60 * 60 * 24))
        return J1970 + days - 0.5
    }

    // ---------------------- Calculation Functions -----------------------
    // Compute declination angle of sun and equation of time
    private fun sunPosition(jd: Double): DoubleArray {
        val D = jd - 2451545
        val g = fixangle(357.529 + 0.98560028 * D)
        val q = fixangle(280.459 + 0.98564736 * D)
        val L = fixangle(q + (1.915 * dsin(g)) + (0.020 * dsin(2 * g)))
        val e = 23.439 - (0.00000036 * D)
        val d = darcsin(dsin(e) * dsin(L))
        var RA = darctan2((dcos(e) * dsin(L)), (dcos(L))) / 15.0
        RA = fixhour(RA)
        val EqT = q / 15.0 - RA
        return doubleArrayOf(d, EqT)
    }

    // Compute equation of time
    private fun equationOfTime(jd: Double): Double {
        return sunPosition(jd)[1]
    }

    // Compute declination angle of sun
    private fun sunDeclination(jd: Double): Double {
        return sunPosition(jd)[0]
    }

    // Compute mid-day (Dhuhr, Zawal) time
    private fun computeMidDay(t: Double): Double {
        val T = equationOfTime(JDate + t)
        return fixhour(12 - T)
    }

    // Compute time for a given angle G
    private fun computeTime(G: Double, t: Double): Double {
        val D = sunDeclination(JDate + t)
        val Z = computeMidDay(t)
        val Beg = -dsin(G) - dsin(D) * dsin(lat)
        val Mid = dcos(D) * dcos(lat)
        val V = darccos(Beg / Mid) / 15.0
        return Z + if (G > 90) -V else V
    }

    // Compute the time of Asr
    private fun computeAsr(step: Double, t: Double): Double {
        val D = sunDeclination(JDate + t)
        val G = -darccot(step + dtan(Math.abs(lat - D)))
        return computeTime(G, t)
    }

    // ---------------------- Misc Functions -----------------------
    // Compute the difference between two times
    private fun timeDiff(time1: Double, time2: Double): Double {
        return fixhour(time2 - time1)
    }

    // -------------------- Interface Functions --------------------
    // Return prayer times for a given date
    fun getDatePrayerTimes(year: Int, month: Int, day: Int, latitude: Double, longitude: Double, tZone: Double): ArrayList<String> {
        lat = latitude
        lng = longitude
        timeZone = tZone
        JDate = julianDate(year, month, day)
        val lonDiff = longitude / (15.0 * 24.0)
        JDate -= lonDiff
        return computeDayTimes()
    }

    // Return prayer times for a given date
    fun getPrayerTimes(date: Calendar, latitude: Double, longitude: Double, tZone: Double): ArrayList<String> {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH)
        val day = date.get(Calendar.DATE)
        return getDatePrayerTimes(year, month + 1, day, latitude, longitude, tZone)
    }

    // Convert double hours to 24h format
    private fun floatToTime24(time: Double): String {
        if (time.isNaN()) return InvalidTime
        var time = fixhour(time + 0.5 / 60.0) // add 0.5 minutes to round
        val hours = Math.floor(time).toInt()
        val minutes = Math.floor((time - hours) * 60.0)
        return String.format("%02d:%02d", hours, minutes.toInt())
    }

    // Convert double hours to 12h format
    private fun floatToTime12(time: Double, noSuffix: Boolean): String {
        if (time.isNaN()) return InvalidTime
        var time = fixhour(time + 0.5 / 60) // add 0.5 minutes to round
        var hours = Math.floor(time).toInt()
        val minutes = Math.floor((time - hours) * 60)
        val suffix = if (hours >= 12) "PM" else "AM"
        hours = ((hours + 12 - 1) % 12) + 1
        return if (noSuffix) {
            String.format("%02d:%02d", hours, minutes.toInt())
        } else {
            String.format("%02d:%02d %s", hours, minutes.toInt(), suffix)
        }
    }

    // Convert double hours to 12h format with no suffix
    private fun floatToTime12NS(time: Double): String {
        return floatToTime12(time, true)
    }

    // ---------------------- Compute Prayer Times -----------------------
    // Compute prayer times at given Julian date
    private fun computeTimes(times: DoubleArray): DoubleArray {
        val t = dayPortion(times)
        val Fajr = computeTime(180 - methodParams.fajrAngle, t[0])
        val Sunrise = computeTime(180 - 0.833, t[1])
        val Dhuhr = computeMidDay(t[2])
        val Asr = computeAsr(1 + asrJuristic.toDouble(), t[3])
        val Sunset = computeTime(0.833, t[4])
        val Maghrib = computeTime(methodParams.maghribMinutes, t[5])
        val Isha = computeTime(methodParams.ishaAngle, t[6])
        return doubleArrayOf(Fajr, Sunrise, Dhuhr, Asr, Sunset, Maghrib, Isha)
    }

    // Compute prayer times at given Julian date
    private fun computeDayTimes(): ArrayList<String> {
        var times = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0, 18.0, 0.0) // default times
        for (i in 1..numIterations) {
            times = computeTimes(times)
        }
        times = adjustTimes(times)
        times = tuneTimes(times)
        return adjustTimesFormat(times)
    }

    // Adjust times in a prayer time array
    private fun adjustTimes(times: DoubleArray): DoubleArray {
        for (i in times.indices) {
            times[i] += timeZone - lng / 15
        }
        times[2] += dhuhrMinutes / 60.0 // Dhuhr
        if (methodParams.maghribSelector == 1) { // Maghrib
            times[5] = times[4] + methodParams.maghribMinutes / 60
        }
        if (methodParams.ishaSeletor == 1) { // Isha
            times[6] = times[5] + methodParams.ishaAngle / 60
        }
        if (adjustHighLats != None) {
            return adjustHighLatTimes(times)
        }
        return times
    }

    // Convert times array to given time format
    private fun adjustTimesFormat(times: DoubleArray): ArrayList<String> {
        val result = ArrayList<String>()
        for (time in times) {
            when (timeFormat) {
                Time12 -> result.add(floatToTime12(time, false))
                Time12NS -> result.add(floatToTime12(time, true))
                Floating -> result.add(time.toString())
                else -> result.add(floatToTime24(time))
            }
        }
        return result
    }

    // Adjust Fajr, Isha and Maghrib for locations in higher latitudes
    private fun adjustHighLatTimes(times: DoubleArray): DoubleArray {
        val nightTime = timeDiff(times[4], times[1]) // sunset to sunrise
        // Adjust Fajr
        val FajrDiff = nightPortion(methodParams.fajrAngle) * nightTime
        if (times[0].isNaN() || timeDiff(times[0], times[1]) > FajrDiff) {
            times[0] = times[1] - FajrDiff
        }
        // Adjust Isha
        val IshaAngle = if (methodParams.ishaSeletor == 0) methodParams.ishaAngle else 18.0
        val IshaDiff = nightPortion(IshaAngle) * nightTime
        if (times[6].isNaN() || timeDiff(times[4], times[6]) > IshaDiff) {
            times[6] = times[4] + IshaDiff
        }
        // Adjust Maghrib
        val MaghribAngle = if (methodParams.maghribSelector == 0) methodParams.maghribMinutes else 4.0
        val MaghribDiff = nightPortion(MaghribAngle) * nightTime
        if (times[5].isNaN() || timeDiff(times[4], times[5]) > MaghribDiff) {
            times[5] = times[4] + MaghribDiff
        }
        return times
    }

    // The night portion used for adjusting times in higher latitudes
    private fun nightPortion(angle: Double): Double {
        return when (adjustHighLats) {
            AngleBased -> angle / 60.0
            MidNight -> 0.5
            OneSeventh -> 0.14286
            else -> 0.0
        }
    }

    // Convert hours to day portions
    private fun dayPortion(times: DoubleArray): DoubleArray {

        for (i in times.indices) {
            times[i] =times[i]/ 24
        }
        return times
    }

    // Tune timings for adjustments
    fun tune(offsetTimes: IntArray) {
        System.arraycopy(offsetTimes, 0, offsets, 0, offsetTimes.size)
    }

    private fun tuneTimes(times: DoubleArray): DoubleArray {
        for (i in times.indices) {
            times[i] += offsets[i] / 60.0
        }
        return times
    }

    // Set calculation method
    fun setCalcMethod(calcMethod: PolygonSearchModel) {
        this.methodParams = calcMethod
    }

    // Get current prayer times for a given location
    fun getCurrentPrayerTimes(
        latitude: Double,
        longitude: Double,
        method: CalculationMethod
    ): ArrayList<String> {
        // Set calculation method based on the enum
        val methodParams = getMethodParams(method)
        setCalcMethod(methodParams)

        // Get the current date and time
        val calendar = Calendar.getInstance()

        // Determine the time zone dynamically based on the location
        val timeZone = TimeZone.getDefault().getOffset(calendar.timeInMillis) / (1000.0 * 60 * 60)

        // Calculate and return prayer times
        return getPrayerTimes(calendar, latitude, longitude, timeZone)
    }
}

// PolygonSearchModel class
data class PolygonSearchModel(
    val fajrAngle: Double,
    val maghribSelector: Int,
    val maghribMinutes: Double,
    val ishaSeletor: Int,
    val ishaAngle: Double
)

enum class CalculationMethod {
    UNIVERSITY_OF_ISLAMIC_SCIENCES_KARACHI,
    UMM_AL_QURAH_MAKKAH,
    MUSLIM_WORLD_LEAGUE,
    EGYPTIAN_GENERAL_AUTHORITY,
    NORTH_AMERICA,
    DUBAI,
    KUWAIT,
    QATAR,
    SINGAPORE,
    OTHER
}

private fun getMethodParams(method: CalculationMethod): PolygonSearchModel {
    return when (method) {
        CalculationMethod.UNIVERSITY_OF_ISLAMIC_SCIENCES_KARACHI -> PolygonSearchModel(
            fajrAngle = 18.0,
            maghribSelector = 0,
            maghribMinutes = 0.0,
            ishaSeletor = 0,
            ishaAngle = 18.0
        )
        CalculationMethod.UMM_AL_QURAH_MAKKAH -> PolygonSearchModel(
            fajrAngle = 18.5,
            maghribSelector = 1,
            maghribMinutes = 0.0,
            ishaSeletor = 1,
            ishaAngle = 90.0
        )
        CalculationMethod.MUSLIM_WORLD_LEAGUE -> PolygonSearchModel(
            fajrAngle = 18.0,
            maghribSelector = 0,
            maghribMinutes = 0.0,
            ishaSeletor = 0,
            ishaAngle = 17.0
        )
        CalculationMethod.EGYPTIAN_GENERAL_AUTHORITY -> PolygonSearchModel(
            fajrAngle = 19.5,
            maghribSelector = 0,
            maghribMinutes = 0.0,
            ishaSeletor = 0,
            ishaAngle = 17.5
        )
        CalculationMethod.NORTH_AMERICA -> PolygonSearchModel(
            fajrAngle = 15.0,
            maghribSelector = 0,
            maghribMinutes = 0.0,
            ishaSeletor = 0,
            ishaAngle = 15.0
        )
        CalculationMethod.DUBAI -> PolygonSearchModel(
            fajrAngle = 18.2,
            maghribSelector = 0,
            maghribMinutes = 0.0,
            ishaSeletor = 0,
            ishaAngle = 18.2
        )
        CalculationMethod.KUWAIT -> PolygonSearchModel(
            fajrAngle = 18.0,
            maghribSelector = 0,
            maghribMinutes = 0.0,
            ishaSeletor = 0,
            ishaAngle = 17.5
        )
        CalculationMethod.QATAR -> PolygonSearchModel(
            fajrAngle = 18.0,
            maghribSelector = 0,
            maghribMinutes = 0.0,
            ishaSeletor = 0,
            ishaAngle = 18.0
        )
        CalculationMethod.SINGAPORE -> PolygonSearchModel(
            fajrAngle = 20.0,
            maghribSelector = 0,
            maghribMinutes = 0.0,
            ishaSeletor = 0,
            ishaAngle = 18.0
        )
        CalculationMethod.OTHER -> PolygonSearchModel(
            fajrAngle = 0.0,
            maghribSelector = 0,
            maghribMinutes = 0.0,
            ishaSeletor = 0,
            ishaAngle = 0.0
        )
    }
}