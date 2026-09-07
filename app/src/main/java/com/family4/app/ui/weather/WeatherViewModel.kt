package com.family4.app.ui.weather

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.prefs.SettingsKeys
import com.family4.app.data.prefs.settingsDataStore
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import javax.inject.Inject

data class WeatherData(
    val cityName: String,
    val tempC: Double,
    val feelsLikeC: Double,
    val description: String,
    val humidity: Int,
    val windKph: Double,
    val emoji: String
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val app: Application
) : AndroidViewModel(app) {

    private val _weather    = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather

    private val _isLoading  = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error      = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Display unit chosen in Settings ("C" or "F"). The API is always queried
     * in metric; conversion happens at render time so flipping the toggle does
     * not require a network round trip.
     */
    val temperatureUnit: StateFlow<String> = app.settingsDataStore.data
        .map { it[SettingsKeys.TEMP_UNIT] ?: DEFAULT_UNIT }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_UNIT)

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Check location permission
                val hasPerm = ContextCompat.checkSelfPermission(
                    app, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                val lat: Double
                val lon: Double
                if (hasPerm) {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(app)
                    val cts = CancellationTokenSource()
                    val loc = fusedClient
                        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .await()
                    lat = loc?.latitude  ?: 37.3861
                    lon = loc?.longitude ?: -122.0839
                } else {
                    // Default: Mountain View, CA (Google HQ — placeholder)
                    lat = 37.3861
                    lon = -122.0839
                }

                // Reverse geocode on IO thread
                // getFromLocation(lat, lon, maxResults) is deprecated on API 33+ in favour of the
                // listener-based overload, but the blocking form is simpler for coroutine IO.
                val cityName = withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        Geocoder(app, Locale.getDefault())
                            .getFromLocation(lat, lon, 1)
                            ?.firstOrNull()
                            ?.locality ?: "Your Location"
                    } catch (_: Exception) { "Your Location" }
                }

                // Fetch weather on IO thread
                val weatherResult = withContext(Dispatchers.IO) {
                    val apiKey = try {
                        com.family4.app.BuildConfig.WEATHER_API_KEY
                    } catch (_: Exception) { "" }

                    if (apiKey.isBlank()) {
                        // No API key — return placeholder data
                        WeatherData(
                            cityName    = cityName,
                            tempC       = 22.0,
                            feelsLikeC  = 21.0,
                            description = "Add OpenWeatherMap key to local.properties",
                            humidity    = 55,
                            windKph     = 12.0,
                            emoji       = "🌤️"
                        )
                    } else {
                        val url = "https://api.openweathermap.org/data/2.5/weather" +
                                  "?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
                        val json = JSONObject(URL(url).readText())
                        val main    = json.getJSONObject("main")
                        val wind    = json.getJSONObject("wind")
                        val weather = json.getJSONArray("weather").getJSONObject(0)
                        WeatherData(
                            cityName    = json.optString("name", cityName),
                            tempC       = main.getDouble("temp"),
                            feelsLikeC  = main.getDouble("feels_like"),
                            description = weather.getString("description"),
                            humidity    = main.getInt("humidity"),
                            windKph     = wind.getDouble("speed") * 3.6,
                            emoji       = iconToEmoji(weather.getString("icon"))
                        )
                    }
                }
                _weather.value = weatherResult
            } catch (e: SecurityException) {
                _error.value = "Location permission required"
            } catch (e: Exception) {
                _error.value = "Weather unavailable: ${e.message?.take(80)}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Formats a Celsius reading in the user's unit, e.g. `72°F` / `22°C`.
     *
     * @param includeUnit false renders just the degree symbol (used for the
     *        "feels like" line, where the unit is already established).
     */
    fun formatTemperature(celsius: Double, unit: String = temperatureUnit.value, includeUnit: Boolean = true): String {
        val value = if (unit == "C") celsius else celsius * 9.0 / 5.0 + 32.0
        val rounded = Math.round(value).toInt()
        return if (includeUnit) "$rounded°$unit" else "$rounded°"
    }

    /** Wind is fetched in m/s and converted to km/h; imperial users see mph. */
    fun formatWind(windKph: Double, unit: String = temperatureUnit.value): String =
        if (unit == "C") {
            "${Math.round(windKph)} km/h"
        } else {
            "${Math.round(windKph * 0.621371)} mph"
        }

    private fun iconToEmoji(icon: String): String = when {
        icon.startsWith("01") -> "☀️"
        icon.startsWith("02") -> "⛅"
        icon.startsWith("03") || icon.startsWith("04") -> "☁️"
        icon.startsWith("09") || icon.startsWith("10") -> "🌧️"
        icon.startsWith("11") -> "⛈️"
        icon.startsWith("13") -> "🌨️"
        icon.startsWith("50") -> "🌫️"
        else -> "🌤️"
    }

    private companion object {
        /** Matches SettingsViewModel's default. */
        const val DEFAULT_UNIT = "F"
    }
}
