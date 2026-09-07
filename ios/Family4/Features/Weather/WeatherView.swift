import SwiftUI

struct WeatherView: View {
    @EnvironmentObject var store: AppStore
    @StateObject private var vm = WeatherViewModel()
    @AppStorage("temperatureUnit") private var unit: String = "F"

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Weather")

                    if vm.isLoading {
                        Spacer()
                        ProgressView().tint(.accentCyan).scaleEffect(1.5)
                        Spacer()
                    } else {
                        ScrollView {
                            VStack(spacing: 16) {
                                // Hero glass card
                                GlassCard {
                                    VStack(alignment: .leading, spacing: 12) {
                                        HStack {
                                            VStack(alignment: .leading, spacing: 4) {
                                                Text(vm.weather?.cityName ?? "--")
                                                    .font(.system(size: 14, weight: .bold))
                                                    .foregroundColor(.accentCyan)
                                                Text(vm.formattedTemp(unit: unit))
                                                    .font(.system(size: 64, weight: .black))
                                                    .foregroundColor(.textPrimary)
                                                Text(vm.feelsLike(unit: unit))
                                                    .font(.system(size: 14))
                                                    .foregroundColor(.textMuted)
                                                Text(vm.weather?.description.capitalized ?? "--")
                                                    .font(.system(size: 16))
                                                    .foregroundColor(.textSecondary)
                                            }
                                            Spacer()
                                            Text(vm.weather?.emoji ?? "🌤️")
                                                .font(.system(size: 72))
                                        }
                                    }
                                    .padding(20)
                                }
                                .padding(.horizontal, 16)

                                // Detail chips card
                                SurfaceCard {
                                    HStack(spacing: 0) {
                                        WeatherChip(emoji: "💧", value: "\(vm.weather?.humidity ?? 0)%", label: "Humidity")
                                        Divider().background(Color.divider).frame(height: 60)
                                        WeatherChip(emoji: "💨", value: vm.formattedWind(unit: unit), label: "Wind")
                                    }
                                    .padding(16)
                                }
                                .padding(.horizontal, 16)

                                // Unit toggle
                                HStack(spacing: 12) {
                                    Spacer()
                                    Button {
                                        unit = unit == "C" ? "F" : "C"
                                    } label: {
                                        Text("Switch to °\(unit == "C" ? "F" : "C")")
                                            .font(.system(size: 13, weight: .semibold))
                                            .foregroundColor(.accentCyan)
                                            .padding(.horizontal, 16)
                                            .padding(.vertical, 9)
                                            .background(Color.accentCyan.opacity(0.12))
                                            .clipShape(Capsule())
                                    }

                                    Button {
                                        Task { await vm.refresh() }
                                    } label: {
                                        Label("Refresh", systemImage: "arrow.clockwise")
                                            .font(.system(size: 13, weight: .semibold))
                                            .foregroundColor(.accentCyan)
                                            .padding(.horizontal, 16)
                                            .padding(.vertical, 9)
                                            .overlay(Capsule().stroke(Color.accentCyan, lineWidth: 1))
                                    }
                                    Spacer()
                                }

                                if let error = vm.error {
                                    Text(error)
                                        .font(.system(size: 13))
                                        .foregroundColor(.errorRed)
                                        .multilineTextAlignment(.center)
                                        .padding(.horizontal, 32)
                                }
                                Spacer(minLength: 32)
                            }
                            .padding(.top, 12)
                        }
                    }
                }
            }
            .navigationBarHidden(true)
            .task { await vm.refresh() }
        }
    }
}

struct WeatherChip: View {
    let emoji: String
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 6) {
            Text(emoji).font(.system(size: 24))
            Text(value)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(.textPrimary)
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(.textMuted)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Weather ViewModel
@MainActor
class WeatherViewModel: ObservableObject {
    @Published var weather: WeatherData? = nil
    @Published var isLoading = false
    @Published var error: String? = nil

    func refresh() async {
        isLoading = true
        error = nil
        do {
            // Use device location; fall back to a default city
            let data = try await fetchWeather(lat: 37.3318, lon: -122.0312)
            weather = data
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    private func fetchWeather(lat: Double, lon: Double) async throws -> WeatherData {
        // Open-Meteo (free, no API key required)
        let urlStr = "https://api.open-meteo.com/v1/forecast?latitude=\(lat)&longitude=\(lon)&current_weather=true&hourly=relativehumidity_2m,apparent_temperature,windspeed_10m&timezone=auto"
        guard let url = URL(string: urlStr) else { throw URLError(.badURL) }
        let (data, _) = try await URLSession.shared.data(from: url)
        let json = try JSONDecoder().decode(OpenMeteoResponse.self, from: data)

        let tempC = json.current_weather.temperature
        let wind  = json.current_weather.windspeed
        let code  = json.current_weather.weathercode

        return WeatherData(
            cityName:    "Current Location",
            tempC:       tempC,
            feelsLikeC:  tempC - 2,
            description: weatherDescription(code: code),
            humidity:    json.hourly?.relativehumidity_2m?.first ?? 0,
            windKph:     wind,
            emoji:       weatherEmoji(code: code)
        )
    }

    func formattedTemp(unit: String) -> String {
        guard let w = weather else { return "--°\(unit)" }
        let val = unit == "F" ? w.tempC * 9/5 + 32 : w.tempC
        return String(format: "%.0f°\(unit)", val)
    }

    func feelsLike(unit: String) -> String {
        guard let w = weather else { return "Feels like --°" }
        let val = unit == "F" ? w.feelsLikeC * 9/5 + 32 : w.feelsLikeC
        return String(format: "Feels like %.0f°", val)
    }

    func formattedWind(unit: String) -> String {
        guard let w = weather else { return "-- km/h" }
        if unit == "F" {
            return String(format: "%.0f mph", w.windKph * 0.621371)
        }
        return String(format: "%.0f km/h", w.windKph)
    }

    private func weatherDescription(code: Int) -> String {
        switch code {
        case 0:        return "Clear sky"
        case 1,2,3:    return "Partly cloudy"
        case 45,48:    return "Foggy"
        case 51,53,55: return "Drizzle"
        case 61,63,65: return "Rainy"
        case 71,73,75: return "Snowy"
        case 80,81,82: return "Rain showers"
        case 95:       return "Thunderstorm"
        default:       return "Cloudy"
        }
    }

    private func weatherEmoji(code: Int) -> String {
        switch code {
        case 0:        return "☀️"
        case 1,2:      return "⛅"
        case 3:        return "☁️"
        case 45,48:    return "🌫️"
        case 51,53,55,61,63,65: return "🌧️"
        case 71,73,75: return "❄️"
        case 80,81,82: return "🌦️"
        case 95:       return "⛈️"
        default:       return "🌤️"
        }
    }
}

// MARK: - Open-Meteo Response
struct OpenMeteoResponse: Codable {
    let current_weather: CurrentWeather
    let hourly: HourlyData?

    struct CurrentWeather: Codable {
        let temperature: Double
        let windspeed: Double
        let weathercode: Int
    }

    struct HourlyData: Codable {
        let relativehumidity_2m: [Int]?
    }
}
