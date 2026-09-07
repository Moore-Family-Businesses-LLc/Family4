import SwiftUI
import AVFoundation

struct WalkieTalkieView: View {
    @EnvironmentObject var store: AppStore
    @StateObject private var vm = WalkieTalkieViewModel()
    @AppStorage("walkieChannel") private var channel: Int = 1

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()
            VStack(spacing: 0) {
                SectionHeaderView(title: "Walkie-Talkie")

                ScrollView {
                    VStack(spacing: 20) {
                        // Channel card
                        GlassCard {
                            VStack(spacing: 16) {
                                Text("CHANNEL")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.textMuted)
                                    .tracking(1.2)
                                HStack(spacing: 20) {
                                    Button {
                                        if channel > 1 { channel -= 1 }
                                    } label: {
                                        Image(systemName: "minus.circle.fill")
                                            .font(.system(size: 32))
                                            .foregroundColor(.accentCyan)
                                    }
                                    Text("\(channel)")
                                        .font(.system(size: 48, weight: .black, design: .monospaced))
                                        .foregroundColor(.textPrimary)
                                        .frame(minWidth: 80)
                                    Button {
                                        if channel < 99 { channel += 1 }
                                    } label: {
                                        Image(systemName: "plus.circle.fill")
                                            .font(.system(size: 32))
                                            .foregroundColor(.accentCyan)
                                    }
                                }
                                HStack(spacing: 8) {
                                    Circle()
                                        .fill(vm.isTransmitting ? Color.accentRed : Color.onlineGreen)
                                        .frame(width: 10, height: 10)
                                    Text(vm.statusText)
                                        .font(.system(size: 13))
                                        .foregroundColor(.textSecondary)
                                }
                            }
                            .padding(24)
                            .frame(maxWidth: .infinity)
                        }
                        .padding(.horizontal, 16)

                        // PTT Button
                        VStack(spacing: 12) {
                            ZStack {
                                // Outer ring (animated when transmitting)
                                Circle()
                                    .stroke(
                                        vm.isTransmitting ? Color.accentCyan.opacity(0.4) : Color.clear,
                                        lineWidth: 2
                                    )
                                    .frame(width: 100, height: 100)
                                    .scaleEffect(vm.isTransmitting ? 1.2 : 1.0)
                                    .animation(.easeInOut(duration: 0.6).repeatForever(autoreverses: true),
                                               value: vm.isTransmitting)

                                Button {
                                    // No action — use gesture
                                } label: {
                                    Image(systemName: "mic.fill")
                                        .font(.system(size: 32))
                                        .foregroundColor(.bgPrimary)
                                        .frame(width: 80, height: 80)
                                        .background(vm.isTransmitting ? Color.accentPurple : Color.accentCyan)
                                        .clipShape(Circle())
                                        .shadow(color: (vm.isTransmitting ? Color.accentPurple : Color.accentCyan).opacity(0.5),
                                                radius: 12, x: 0, y: 4)
                                }
                                .buttonStyle(.plain)
                                .simultaneousGesture(
                                    DragGesture(minimumDistance: 0)
                                        .onChanged { _ in if !vm.isTransmitting { vm.startTransmitting() } }
                                        .onEnded { _ in vm.stopTransmitting() }
                                )
                                .scaleEffect(vm.isTransmitting ? 1.08 : 1.0)
                                .animation(.spring(response: 0.2), value: vm.isTransmitting)
                            }

                            Text("HOLD TO TALK")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.textMuted)
                                .tracking(2)
                        }

                        // Waveform / Last heard
                        SurfaceCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("LAST HEARD")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.textMuted)
                                    .tracking(1.2)
                                ForEach(vm.recentTransmissions, id: \.self) { entry in
                                    HStack(spacing: 8) {
                                        Circle().fill(Color.accentCyan).frame(width: 6, height: 6)
                                        Text(entry)
                                            .font(.system(size: 13))
                                            .foregroundColor(.textSecondary)
                                    }
                                }
                                if vm.recentTransmissions.isEmpty {
                                    Text("No recent transmissions")
                                        .font(.system(size: 13))
                                        .foregroundColor(.textMuted)
                                }
                            }
                            .padding(16)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .padding(.horizontal, 16)

                        Spacer(minLength: 32)
                    }
                    .padding(.top, 12)
                }
            }
        }
        .onAppear { vm.requestPermission() }
    }
}

// MARK: - Walkie ViewModel
@MainActor
class WalkieTalkieViewModel: ObservableObject {
    @Published var isTransmitting = false
    @Published var statusText = "Standby"
    @Published var recentTransmissions: [String] = []

    private var audioEngine = AVAudioEngine()
    private var playerNode = AVAudioPlayerNode()

    func requestPermission() {
        AVAudioApplication.requestRecordPermission { _ in }
    }

    func startTransmitting() {
        isTransmitting = true
        statusText = "Transmitting…"
        // In a real app: open UDP socket, stream mic audio to other family devices
        let entry = "\(Date().formatted(date: .omitted, time: .shortened)) — You"
        recentTransmissions.insert(entry, at: 0)
        if recentTransmissions.count > 5 { recentTransmissions = Array(recentTransmissions.prefix(5)) }
    }

    func stopTransmitting() {
        isTransmitting = false
        statusText = "Standby"
    }
}
