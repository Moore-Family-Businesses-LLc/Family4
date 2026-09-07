import SwiftUI
import HealthKit

struct HealthView: View {
    @EnvironmentObject var store: AppStore
    @StateObject private var vm = HealthViewModel()

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()
            VStack(spacing: 0) {
                SectionHeaderView(title: "Health")

                ScrollView {
                    VStack(spacing: 16) {
                        // Stats grid
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            HealthStatCard(emoji: "👟", value: "\(vm.steps)", label: "Steps", color: .accentCyan)
                            HealthStatCard(emoji: "❤️", value: "\(vm.heartRate) bpm", label: "Heart Rate", color: .accentRed)
                            HealthStatCard(emoji: "🔥", value: "\(vm.calories) kcal", label: "Calories", color: .accentOrange)
                            HealthStatCard(emoji: "💤", value: String(format: "%.1f h", vm.sleepHours), label: "Sleep", color: .accentPurple)
                            HealthStatCard(emoji: "💧", value: "\(vm.waterMl) ml", label: "Water", color: .accentBlue)
                            HealthStatCard(emoji: "😊", value: moodLabel(vm.moodScore), label: "Mood", color: .accentGreen)
                        }
                        .padding(.horizontal, 16)

                        // Mood selector
                        SurfaceCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("TODAY'S MOOD")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.textMuted)
                                    .tracking(1.2)
                                HStack(spacing: 16) {
                                    ForEach(1...5, id: \.self) { score in
                                        Button {
                                            withAnimation { vm.moodScore = score }
                                        } label: {
                                            Text(moodEmoji(score))
                                                .font(.system(size: 32))
                                                .scaleEffect(vm.moodScore == score ? 1.3 : 0.9)
                                                .animation(.spring(), value: vm.moodScore)
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                                .frame(maxWidth: .infinity)
                            }
                            .padding(16)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .padding(.horizontal, 16)

                        // Water intake
                        SurfaceCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("WATER INTAKE")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.textMuted)
                                    .tracking(1.2)
                                HStack {
                                    Text("\(vm.waterMl) ml")
                                        .font(.system(size: 22, weight: .bold))
                                        .foregroundColor(.accentBlue)
                                    Spacer()
                                    Button {
                                        withAnimation { vm.waterMl += 250 }
                                    } label: {
                                        Label("+250 ml", systemImage: "drop.fill")
                                            .font(.system(size: 13, weight: .semibold))
                                            .foregroundColor(.bgPrimary)
                                            .padding(.horizontal, 14)
                                            .padding(.vertical, 8)
                                            .background(Color.accentBlue)
                                            .clipShape(Capsule())
                                    }
                                    .buttonStyle(.plain)
                                }
                                // Progress bar
                                let progress = min(Double(vm.waterMl) / 2000.0, 1.0)
                                GeometryReader { geo in
                                    ZStack(alignment: .leading) {
                                        RoundedRectangle(cornerRadius: 4).fill(Color.bgElevated).frame(height: 8)
                                        RoundedRectangle(cornerRadius: 4)
                                            .fill(Color.accentBlue)
                                            .frame(width: geo.size.width * progress, height: 8)
                                    }
                                }
                                .frame(height: 8)
                                Text("Goal: 2000 ml")
                                    .font(.system(size: 11))
                                    .foregroundColor(.textMuted)
                            }
                            .padding(16)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .padding(.horizontal, 16)

                        if !vm.healthKitAvailable {
                            Text("Connect Apple Health to see real data")
                                .font(.system(size: 13))
                                .foregroundColor(.textMuted)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 32)

                            Button("Connect Apple Health") {
                                vm.requestHealthKit()
                            }
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.bgPrimary)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(Color.accentCyan)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }

                        Spacer(minLength: 32)
                    }
                    .padding(.top, 12)
                }
            }
        }
        .onAppear { vm.load() }
    }

    private func moodLabel(_ score: Int) -> String {
        ["😢","😟","😐","😊","😄"][max(0, min(score - 1, 4))]
    }
    private func moodEmoji(_ score: Int) -> String {
        ["😢","😟","😐","😊","😄"][score - 1]
    }
}

struct HealthStatCard: View {
    let emoji: String
    let value: String
    let label: String
    let color: Color

    var body: some View {
        SurfaceCard {
            VStack(spacing: 8) {
                Text(emoji).font(.system(size: 28))
                Text(value)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text(label)
                    .font(.system(size: 11))
                    .foregroundColor(.textMuted)
            }
            .padding(14)
            .frame(maxWidth: .infinity)
        }
    }
}

// MARK: - Health ViewModel
@MainActor
class HealthViewModel: ObservableObject {
    @Published var steps: Int = 0
    @Published var heartRate: Int = 0
    @Published var calories: Int = 0
    @Published var sleepHours: Double = 0
    @Published var waterMl: Int = 0
    @Published var moodScore: Int = 3
    @Published var healthKitAvailable = false

    private let store = HKHealthStore()

    func load() {
        healthKitAvailable = HKHealthStore.isHealthDataAvailable()
        if healthKitAvailable {
            // Load sample data while real HealthKit query is set up
            steps = Int.random(in: 4000...12000)
            heartRate = Int.random(in: 60...90)
            calories = Int.random(in: 1200...2400)
            sleepHours = Double(Int.random(in: 6...9)) + Double(Int.random(in: 0...5)) / 10.0
        }
    }

    func requestHealthKit() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        let types: Set<HKObjectType> = [
            HKQuantityType.quantityType(forIdentifier: .stepCount)!,
            HKQuantityType.quantityType(forIdentifier: .heartRate)!,
            HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)!,
            HKCategoryType.categoryType(forIdentifier: .sleepAnalysis)!,
        ]
        store.requestAuthorization(toShare: nil, read: types) { [weak self] granted, _ in
            if granted { Task { @MainActor in self?.load() } }
        }
    }
}
