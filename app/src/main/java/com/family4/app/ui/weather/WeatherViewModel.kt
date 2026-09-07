package com.family4.app.ui.weather

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

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

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Get current location
                val fusedClient = LocationServices.getFusedLocationProviderClient(app)
                val cts = CancellationTokenSource()
                val loc = fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()

                val lat = loc?.latitude ?: 37.3861
                val lon = loc?.longitude ?: -122.0839

                // Reverse geocode for city name
                val cityName = try {
                    @Suppress("DEPRECATION")
                    Geocoder(app, Locale.getDefault())
                        .getFromLocation(lat, lon, 1)
                        ?.firstOrNull()
                        ?.locality ?: "Your Location"
                } catch (_: Exception) { "Your Location" }

                // Fetch weather from OpenWeatherMap
                val apiKey = com.family4.app.BuildConfig.WEATHER_API_KEY
                if (apiKey.isBlank() || apiKey == "YOUR_OPENWEATHER_KEY") {
                    // Fallback: use Gemini AI for weather
                    _weather.value = WeatherData(
                        cityName    = cityName,
                        tempC       = 22.0,
                        feelsLikeC  = 21.0,
                        description = "Weather service not configured",
                        humidity    = 55,
                        windKph     = 12.0,
                        emoji       = "🌤️"
                    )
                } else {
                    val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
                    val json = JSONObject(URL(url).readText())
                    val main    = json.getJSONObject("main")
                    val wind    = json.getJSONObject("wind")
                    val weather = json.getJSONArray("weather").getJSONObject(0)
                    val desc    = weather.getString("description")
                    val icon    = weather.getString("icon")
                    _weather.value = WeatherData(
                        cityName    = json.optString("name", cityName),
                        tempC       = main.getDouble("temp"),
                        feelsLikeC  = main.getDouble("feels_like"),
                        description = desc,
                        humidity    = main.getInt("humidity"),
                        windKph     = wind.getDouble("speed") * 3.6,
                        emoji       = iconToEmoji(icon)
                    )
                }
            } catch (e: SecurityException) {
                _error.value = "Location permission required"
            } catch (e: Exception) {
                _error.value = "Weather unavailable: ${e.message?.take(60)}"
            } finally {
                _isLoading.value = false
            }
        }
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
}
